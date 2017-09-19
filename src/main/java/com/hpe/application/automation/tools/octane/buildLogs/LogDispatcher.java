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

package com.hpe.application.automation.tools.octane.buildLogs;

import com.google.common.primitives.Longs;
import com.google.inject.Inject;
import com.hp.mqm.client.MqmRestClient;
import com.hp.mqm.client.exception.RequestErrorException;
import com.hpe.application.automation.tools.octane.ResultQueue;
import com.hpe.application.automation.tools.octane.client.JenkinsInsightEventPublisher;
import com.hpe.application.automation.tools.octane.client.JenkinsMqmRestClientFactory;
import com.hpe.application.automation.tools.octane.client.JenkinsMqmRestClientFactoryImpl;
import com.hpe.application.automation.tools.octane.client.RetryModel;
import com.hpe.application.automation.tools.octane.configuration.ConfigurationService;
import com.hpe.application.automation.tools.octane.configuration.ServerConfiguration;
import com.hpe.application.automation.tools.octane.tests.AbstractSafeLoggingAsyncPeriodWork;
import com.hpe.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import hudson.Extension;
import hudson.console.PlainTextConsoleOutputStream;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.TimeUnit2;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Long.parseLong;

/**
 * Created by benmeior on 11/20/2016
 * Log dispatcher is responsible for dispatching logs messages to BDI server via Octane as its proxy
 */

@Extension
public class LogDispatcher extends AbstractSafeLoggingAsyncPeriodWork {
	private static final Logger logger = LogManager.getLogger(LogDispatcher.class);
	private static final ExecutorService logDispatcherExecutors = Executors.newFixedThreadPool(20, new NamedThreadFactory(LogDispatcher.class.getSimpleName()));

	private static final String OCTANE_LOG_FILE_NAME = "octane_log";
	private static final int MAX_RETRIES = 6;
	private static final long TIMEOUT = 20;

	private static final double BASE = 2;
	private static final double EXPONENT = 0;

	private RetryModel retryModel;
	private JenkinsMqmRestClientFactory clientFactory;
	private final ResultQueue logsQueue;

	public LogDispatcher() throws IOException {
		super("Octane log dispatcher");
		logsQueue = new LogsResultQueue(MAX_RETRIES);
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
		if (logsQueue.peekFirst() == null) {
			return;
		}

		MqmRestClient mqmRestClient = initMqmRestClient();
		if (mqmRestClient == null) {
			logger.warn("There are pending build logs, but MQM server location is not specified, build logs can't be submitted");
			logsQueue.remove();
			return;
		}

		ResultQueue.QueueItem item;

		while ((item = logsQueue.peekFirst()) != null) {

			if (retryModel.isQuietPeriod()) {
				logger.debug("There are pending logs, but we are in quiet period");
				return;
			}

			Run build = getBuildFromQueueItem(item);
			if (build == null) {
				logger.warn("Build and/or Project [" + item.getProjectName() + "#" + item.getBuildNumber() + "] no longer exists, pending build logs can't be submitted");
				logsQueue.remove();
				continue;
			}

			try {
				if (item.getWorkspace() == null) {
					//
					//  initial queue item flow - no workspaces, works with workspaces retrieval and loop ever each of them
					//
					List<String> workspaces = mqmRestClient.getJobWorkspaceId(ConfigurationService.getModel().getIdentity(), BuildHandlerUtils.getJobCiId(build));
					if (workspaces.isEmpty()) {
						logger.info(String.format("Job '%s' is not part of an Octane pipeline in any workspace, so its log will not be sent.", BuildHandlerUtils.getJobCiId(build)));
					} else {
						CountDownLatch latch = new CountDownLatch(workspaces.size());

						for (String workspaceId : workspaces) {
							logDispatcherExecutors.execute(new SendLogsExecutor(
									mqmRestClient,
									build,
									item,
									workspaceId,
									logsQueue,
									latch
							));
						}

						boolean completedResult = latch.await(TIMEOUT, TimeUnit.MINUTES);
						if (completedResult) {
							logger.error("timed out sending logs to - " + workspaces.size() + " workspaces.");
						}
					}
					logsQueue.remove();
				} else {
					//
					//  secondary queue item flow - workspace is known, we are in retry flow
					//
					transferBuildLogs(build, mqmRestClient, item);
				}
			} catch (Exception e) {
				logger.error("fatally failed to fetch relevant workspaces OR to send log for build " + item.getProjectName() + " #" + item.getBuildNumber() + " to workspace " + item.getWorkspace() + ", will not retry this one", e);
			}
		}
	}

	private void transferBuildLogs(Run build, MqmRestClient mqmRestClient, ResultQueue.QueueItem item) {
		try {
			OctaneLog octaneLog = getOctaneLogFile(build);
			boolean status = mqmRestClient.postLogs(
					parseLong(item.getWorkspace()),
					ConfigurationService.getModel().getIdentity(),
					build.getParent().getName(),
					String.valueOf(build.getNumber()),
					octaneLog.getLogStream(),
					octaneLog.getFileLength());
			if (status) {
				logger.info("Successfully sent logs of " + item.getProjectName() + " #" + item.getBuildNumber() + " to workspace " + item.getWorkspace());
				logsQueue.remove();
			} else {
				logger.error("failed to send log for build " + item.getProjectName() + " #" + item.getBuildNumber() + " to workspace " + item.getWorkspace());
				reAttempt(item.getProjectName(), item.getBuildNumber());
			}
		} catch (RequestErrorException ree) {
			logger.error("failed to send log for build " + item.getProjectName() + " #" + item.getBuildNumber() + " to workspace " + item.getWorkspace(), ree);
			reAttempt(item.getProjectName(), item.getBuildNumber());
		} catch (Exception e) {
			logger.error("fatally failed to send log for build " + item.getProjectName() + " #" + item.getBuildNumber() + " to workspace " + item.getWorkspace() + ", will not retry this one", e);
			retryModel.success();
			logsQueue.remove();
		}
	}

	private void reAttempt(String projectName, int buildNumber) {
		if (!logsQueue.failed()) {
			logger.warn("maximum number of attempts reached, operation will not be re-attempted for build "+ projectName + " #" + buildNumber);
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

	private OctaneLog getOctaneLogFile(Run build) throws IOException {
		String octaneLogFilePath = build.getLogFile().getParent() + File.separator + OCTANE_LOG_FILE_NAME;
		File logFile = new File(octaneLogFilePath);
		if (!logFile.exists()) {
			try (FileOutputStream fileOutputStream = new FileOutputStream(logFile);
			     InputStream logStream = build.getLogInputStream();
			     PlainTextConsoleOutputStream out = new PlainTextConsoleOutputStream(fileOutputStream)) {
				IOUtils.copy(logStream, out);
				out.flush();
			}
		}
		return new OctaneLog(logFile);
	}

	private Run getBuildFromQueueItem(ResultQueue.QueueItem item) {
		Run result = null;
		Job project = (Job) Jenkins.getInstance().getItemByFullName(item.getProjectName());
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

	public void enqueueLog(String projectName, int buildNumber) {
		logsQueue.add(projectName, buildNumber, null);
	}

	@Inject
	public void setEventPublisher(JenkinsInsightEventPublisher eventPublisher) {
		this.retryModel = new RetryModel(eventPublisher, getQuietPeriodsInMinutes(MAX_RETRIES));
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

	private final class SendLogsExecutor implements Runnable {
		private final MqmRestClient mqmRestClient;
		private final Run build;
		private final ResultQueue.QueueItem item;
		private final String workspaceId;
		private final ResultQueue logsQueue;
		private final CountDownLatch latch;

		private SendLogsExecutor(
				MqmRestClient mqmRestClient,
				Run build,
				ResultQueue.QueueItem item,
				String workspaceId,
				ResultQueue logsQueue,
				CountDownLatch latch) {
			this.mqmRestClient = mqmRestClient;
			this.build = build;
			this.item = item;
			this.workspaceId = workspaceId;
			this.logsQueue = logsQueue;
			this.latch = latch;
		}

		@Override
		public void run() {
			try {
				OctaneLog octaneLog = getOctaneLogFile(build);
				boolean status = mqmRestClient.postLogs(
						parseLong(workspaceId),
						ConfigurationService.getModel().getIdentity(),
						BuildHandlerUtils.getJobCiId(build),
						String.valueOf(build.getNumber()),
						octaneLog.getLogStream(),
						octaneLog.getFileLength());
				if (status) {
					logger.info("Successfully sent logs of " + item.getProjectName() + " #" + item.getBuildNumber() + " to workspace " + workspaceId);
				} else {
					logger.debug("failed to send log for build " + item.getProjectName() + " #" + item.getBuildNumber() + " to workspace " + workspaceId);
					logsQueue.add(item.getProjectName(), item.getBuildNumber(), workspaceId);
				}
			} catch (RequestErrorException ree) {
				logger.debug("failed to send log for build " + item.getProjectName() + " #" + item.getBuildNumber() + " to workspace " + workspaceId, ree);
				logsQueue.add(item.getProjectName(), item.getBuildNumber(), workspaceId);
			} catch (Exception e) {
				logger.error("fatally failed to send log for build " + item.getProjectName() + " #" + item.getBuildNumber() + " to workspace " + workspaceId + ", will not retry this one", e);
			}
			latch.countDown();
		}
	}
}
