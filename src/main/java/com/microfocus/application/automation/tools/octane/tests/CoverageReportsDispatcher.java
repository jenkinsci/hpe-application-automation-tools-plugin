/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.microfocus.application.automation.tools.octane.tests;

import com.google.common.primitives.Longs;
import com.google.inject.Inject;
import com.hp.mqm.client.MqmRestClient;
import com.hp.mqm.client.exception.RequestErrorException;
import com.microfocus.application.automation.tools.octane.ResultQueue;
import com.microfocus.application.automation.tools.octane.actions.coverage.CoverageService;
import com.microfocus.application.automation.tools.octane.client.JenkinsMqmRestClientFactory;
import com.microfocus.application.automation.tools.octane.client.JenkinsMqmRestClientFactoryImpl;
import com.microfocus.application.automation.tools.octane.client.RetryModel;
import com.microfocus.application.automation.tools.octane.configuration.ConfigurationService;
import com.microfocus.application.automation.tools.octane.configuration.ServerConfiguration;
import com.microfocus.application.automation.tools.octane.executor.CoverageReportsQueue;
import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import hudson.Extension;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.TimeUnit2;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * this class manages a queue of coverage report upload tasks
 */

@Extension
public class CoverageReportsDispatcher extends AbstractSafeLoggingAsyncPeriodWork {
	private static final Logger logger = LogManager.getLogger(CoverageReportsDispatcher.class);

	private static final int MAX_RETRIES = 6;

	private static final double BASE = 2;
	private static final double EXPONENT = 0;
	@Inject
	private RetryModel retryModel;
	private JenkinsMqmRestClientFactory clientFactory;
	private final ResultQueue reportsQueue;

	@Inject
	public CoverageReportsDispatcher() throws IOException {
		super("Octane coverage reports dispatcher");
		reportsQueue = new CoverageReportsQueue(MAX_RETRIES);
	}

	private long[] getQuietPeriodsInMinutes(double retries) {
		double exponent = EXPONENT;
		List<Long> quietPeriods = new ArrayList<>();
		while (exponent <= retries) {
			quietPeriods.add(TimeUnit2.MINUTES.toMillis((long) Math.pow(BASE, exponent)));
			exponent++;
		}
		return Longs.toArray(quietPeriods);
	}

	@Override
	protected void doExecute(TaskListener listener) {
		if (reportsQueue.peekFirst() == null) {
			return;
		}

		MqmRestClient mqmRestClient = initMqmRestClient();
		if (mqmRestClient == null) {
			logger.warn("There are pending coverage reports, but MQM server location is not specified, reports can't be submitted");
			reportsQueue.remove();
			return;
		}

		ResultQueue.QueueItem item;

		while ((item = reportsQueue.peekFirst()) != null) {

			if (retryModel.isQuietPeriod()) {
				logger.debug("There are pending coverage reports, but we are in quiet period");
				return;
			}

			Run build = getBuildFromQueueItem(item);
			if (build == null) {
				logger.warn("Build and/or Project [" + item.getProjectName() + "#" + item.getBuildNumber() + "] no longer exists, pending coverage reports can't be submitted");
				reportsQueue.remove();
				continue;
			}
			if (item.getType().equals(CoverageService.Jacoco.JACOCO_TYPE)) {
				transferCoverageReports(build, mqmRestClient, item, CoverageService.Jacoco.JACOCO_DEFAULT_FILE_NAME);
			} else if (item.getType().equals(CoverageService.Lcov.LCOV_TYPE)) {
				transferCoverageReports(build, mqmRestClient, item, CoverageService.Lcov.LCOV_DEFAULT_FILE_NAME);
			}
		}
	}

	private void transferCoverageReports(Run build, MqmRestClient mqmRestClient, ResultQueue.QueueItem item, String coverageReportFileSuffix) {
		long index = 0;
		File coverageFile = getCoverageFile(build, index, coverageReportFileSuffix);
		try {
			// iterate all coverage reports in build folder
			while (coverageFile.exists()) {
				// send each report as IS to octane using rest client
				boolean status = mqmRestClient.postCoverageReports(
						ConfigurationService.getModel().getIdentity(),
						BuildHandlerUtils.getJobCiId(build),
						BuildHandlerUtils.getBuildCiId(build),
						new FileInputStream(coverageFile),
						coverageFile.length(), item.getType());
				if (status) {
					logger.info("Successfully sent coverage report " + coverageFile.getName() + " for job " + item.getProjectName() + " with build #" + item.getBuildNumber());
				} else {
					logger.error("failed to send coverage report " + coverageFile.getName() + " for job " + item.getProjectName() + " with build #" + item.getBuildNumber());
					reAttemptTask(item.getProjectName(), item.getBuildNumber(), item.getType());
					return;
				}
				// get next file
				index++;
				coverageFile = getCoverageFile(build, index, coverageReportFileSuffix);
			}
			reportsQueue.remove();
		} catch (RequestErrorException ree) {
			logger.error("failed to send coverage reports (of type " + item.getType() + ") for job " + item.getProjectName() + " #" + item.getBuildNumber(), ree);
			reAttemptTask(item.getProjectName(), item.getBuildNumber(), item.getType());
		} catch (Exception e) {
			logger.error("fatally failed to send coverage reports (of type " + item.getType() + ") for build " + item.getProjectName() + " #" + item.getBuildNumber() + ", will not retry this one", e);
			retryModel.success();
			reportsQueue.remove();
		}
	}

	private File getCoverageFile(Run build, long index, String coverageReportFileSuffix) {
		String coverageReportFilePath = build.getRootDir() + File.separator + CoverageService.getCoverageReportFileName((int) index, coverageReportFileSuffix);
		return new File(coverageReportFilePath);
	}

	private void reAttemptTask(String projectName, int buildNumber, String itemReportType) {
		if (!reportsQueue.failed()) { // add task to queue and return true if max attempts not reached, else return false
			logger.warn("maximum number of attempts reached (" + MAX_RETRIES + "), " +
					"operation will not be re-attempted for build " + projectName + " #" + buildNumber + " of type " + itemReportType);
			retryModel.success();
		} else {
			logger.info("There are pending logs, but we are in quiet period");
			retryModel.failure();
		}
	}

	private MqmRestClient initMqmRestClient() {
		MqmRestClient result = null;
		ServerConfiguration configuration = ConfigurationService.getServerConfiguration();
		if (configuration.isValid()) {
			result = clientFactory.obtain(
					configuration.location,
					configuration.sharedSpace,
					configuration.username,
					configuration.password);
		}
		return result;
	}

	private Run getBuildFromQueueItem(ResultQueue.QueueItem item) {
		Run result = null;
		Jenkins jenkins = Jenkins.getInstance();
		if (jenkins == null) {
			throw new IllegalStateException("failed to obtain Jenkins' instance");
		}
		Job project = (Job) jenkins.getItemByFullName(item.getProjectName());
		if (project != null) {
			result = project.getBuildByNumber(item.getBuildNumber());
		}
		return result;
	}

	@Override
	public long getRecurrencePeriod() {
		String value = System.getProperty("Octane.LogDispatcher.Period"); // let's us config the recurrence period. default is 10 seconds.
		if (!StringUtils.isEmpty(value)) {
			return Long.valueOf(value);
		}
		return TimeUnit2.SECONDS.toMillis(10);
	}

	public void enqueueTask(String projectName, int buildNumber, String fileType) {
		reportsQueue.add(projectName, fileType, buildNumber);
	}

	@Inject
	public void setEventPublisher() {
		this.retryModel = new RetryModel(getQuietPeriodsInMinutes(MAX_RETRIES));
	}

	@Inject
	public void setMqmRestClientFactory(JenkinsMqmRestClientFactoryImpl clientFactory) {
		this.clientFactory = clientFactory;
	}

	private static final class NamedThreadFactory implements ThreadFactory {

		private AtomicInteger threadNumber = new AtomicInteger(1);
		private final String namePrefix;

		private NamedThreadFactory(String namePrefix) {
			this.namePrefix = namePrefix;
		}

		public Thread newThread(Runnable runnable) {
			Thread result = new Thread(runnable, this.namePrefix + " thread-" + threadNumber.getAndIncrement());
			result.setDaemon(true);
			return result;
		}
	}
}
