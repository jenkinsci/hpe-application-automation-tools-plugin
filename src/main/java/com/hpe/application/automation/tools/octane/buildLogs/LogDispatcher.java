/*
 * © Copyright 2013 EntIT Software LLC
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright (c) 2018 Micro Focus Company, L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ___________________________________________________________________
 *
 */

package com.hpe.application.automation.tools.octane.buildLogs;

import com.google.common.primitives.Longs;
import com.google.inject.Inject;
import com.hp.mqm.client.MqmRestClient;
import com.hp.mqm.client.exception.RequestErrorException;
import com.hpe.application.automation.tools.octane.ResultQueue;
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
			logger.warn("there are pending build logs, but MQM server location is not specified, build logs can't be submitted");
			logsQueue.remove();
			return;
		}

		ResultQueue.QueueItem item;

		while ((item = logsQueue.peekFirst()) != null) {
			if (retryModel.isQuietPeriod()) {
				logger.info("there are pending logs, but we are in quiet period");
				return;
			}

			Run build = getBuildFromQueueItem(item);
			if (build == null) {
				logger.warn("build and/or project [" + item.getProjectName() + " #" + item.getBuildNumber() + "] no longer exists, pending build logs won't be submitted");
				logsQueue.remove();
				continue;
			}

			String jobCiId = BuildHandlerUtils.getJobCiId(build);
			try {
				if (item.getWorkspace() == null) {
					//
					//  initial queue item flow - no workspaces, works with workspaces retrieval and loop ever each of them
					//
					logger.info("retrieving all workspaces that logs of [" + jobCiId + "] are relevant to...");
					List<String> workspaces = mqmRestClient.getJobWorkspaceId(ConfigurationService.getModel().getIdentity(), BuildHandlerUtils.getJobCiId(build));
					if (workspaces.isEmpty()) {
						logger.info("[" + jobCiId + "] is not part of any Octane pipeline in any workspace, log won't be sent");
					} else {
						logger.info("logs of [" + jobCiId + "] found to be relevant to " + workspaces.size() + " workspace/s");
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
						if (!completedResult) {
							logger.error("timed out sending logs to " + workspaces.size() + " workspace/s");
						}
					}
					logsQueue.remove();
				} else {
					//
					//  secondary queue item flow - workspace is known, we are in retry flow
					//
					logger.info("");
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
					BuildHandlerUtils.getJobCiId(build),
					BuildHandlerUtils.getBuildCiId(build),
					octaneLog.getLogStream(),
					octaneLog.getFileLength());
			if (status) {
				logger.info("successfully sent log of [" + item.getProjectName() + " #" + item.getBuildNumber() + "] to workspace " + item.getWorkspace());
				logsQueue.remove();
			} else {
				logger.error("failed to send log of [" + item.getProjectName() + " #" + item.getBuildNumber() + "] to workspace " + item.getWorkspace());
				reAttempt(item.getProjectName(), item.getBuildNumber());
			}
		} catch (RequestErrorException ree) {
			logger.error("failed to send log of [" + item.getProjectName() + " #" + item.getBuildNumber() + "] to workspace " + item.getWorkspace(), ree);
			reAttempt(item.getProjectName(), item.getBuildNumber());
		} catch (Exception e) {
			logger.error("fatally failed to send log of [" + item.getProjectName() + " #" + item.getBuildNumber() + "] to workspace " + item.getWorkspace() + ", will not retry this one", e);
			retryModel.success();
			logsQueue.remove();
		}
	}

	private void reAttempt(String projectName, int buildNumber) {
		if (!logsQueue.failed()) {
			logger.warn("maximum number of attempts reached, operation will not be re-attempted for build " + projectName + " #" + buildNumber);
			retryModel.success();
		} else {
			logger.info("there are pending logs, but we are in quiet period");
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
		String value = System.getProperty("Octane.LogDispatcher.Period");
		if (!StringUtils.isEmpty(value)) {
			return Long.valueOf(value);
		}
		return TimeUnit2.SECONDS.toMillis(10);
	}

	public void enqueueLog(String projectName, int buildNumber) {
		logsQueue.add(projectName, buildNumber, null);
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
						BuildHandlerUtils.getBuildCiId(build),
						octaneLog.getLogStream(),
						octaneLog.getFileLength());
				if (status) {
					logger.info("successfully sent log of [" + item.getProjectName() + " #" + item.getBuildNumber() + "] to workspace " + workspaceId);
				} else {
					logger.debug("failed to send log of [" + item.getProjectName() + " #" + item.getBuildNumber() + "] to workspace " + workspaceId);
					logsQueue.add(item.getProjectName(), item.getBuildNumber(), workspaceId);
				}
			} catch (RequestErrorException ree) {
				logger.debug("failed to send log of [" + item.getProjectName() + " #" + item.getBuildNumber() + "] to workspace " + workspaceId, ree);
				logsQueue.add(item.getProjectName(), item.getBuildNumber(), workspaceId);
			} catch (Exception e) {
				logger.error("fatally failed to send log of [" + item.getProjectName() + " #" + item.getBuildNumber() + "] to workspace " + workspaceId + ", will not retry this one", e);
			}
			latch.countDown();
		}
	}
}
