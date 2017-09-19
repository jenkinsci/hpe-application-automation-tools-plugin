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

package com.hpe.application.automation.tools.octane.bridge;

import com.hp.mqm.client.MqmRestClient;
import com.hp.mqm.client.exception.AuthenticationException;
import com.hp.mqm.client.exception.ServerException;
import com.hp.mqm.client.exception.TemporarilyUnavailableException;
import com.hp.mqm.client.model.AbridgedTaskPluginInfo;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.spi.CIPluginServices;
import com.hp.octane.integrations.api.TasksProcessor;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.OctaneResultAbridged;
import com.hp.octane.integrations.dto.connectivity.OctaneTaskAbridged;
import com.hpe.application.automation.tools.octane.client.JenkinsMqmRestClientFactory;
import com.hpe.application.automation.tools.octane.configuration.ConfigurationService;
import com.hpe.application.automation.tools.octane.configuration.ServerConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.stapler.export.Exported;

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class BridgeClient {
	private static final Logger logger = LogManager.getLogger(BridgeClient.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();
	private static String serverInstanceId;
	private ExecutorService connectivityExecutors = Executors.newFixedThreadPool(5, new AbridgedConnectivityExecutorsFactory());
	private ExecutorService taskProcessingExecutors = Executors.newFixedThreadPool(30, new AbridgedTasksExecutorsFactory());
	volatile private boolean isConnected = false;
	volatile private boolean shuttingDown = false;

	private ServerConfiguration mqmConfig;
	private JenkinsMqmRestClientFactory restClientFactory;

	public BridgeClient(ServerConfiguration mqmConfig, JenkinsMqmRestClientFactory clientFactory, String serverIdentity) {
		this.serverInstanceId = serverIdentity;
		this.mqmConfig = new ServerConfiguration(mqmConfig.location, mqmConfig.sharedSpace, mqmConfig.username, mqmConfig.password, mqmConfig.impersonatedUser);
		restClientFactory = clientFactory;
		connect();
		logger.info("client initialized for '" + this.mqmConfig.location + "'; SP: " + this.mqmConfig.sharedSpace + "; access key: " + this.mqmConfig.username);
	}

	public void update(ServerConfiguration newConfig, String serverIdentity) {
		this.serverInstanceId = serverIdentity;
		mqmConfig = new ServerConfiguration(newConfig.location, newConfig.sharedSpace, newConfig.username, newConfig.password, newConfig.impersonatedUser);
		logger.info("client updated to '" + mqmConfig.location + "'; SP: " + mqmConfig.sharedSpace + "; access key: " + newConfig.username);
		restClientFactory.updateMqmRestClient(mqmConfig.location, mqmConfig.sharedSpace, mqmConfig.username, mqmConfig.password);
		connect();
	}

	private void connect() {
		if (!shuttingDown && !isConnected) {
			isConnected = true;
			connectivityExecutors.execute(new Runnable() {
				@Override
				public void run() {
					String tasksJSON;
					CIPluginServices pluginServices = OctaneSDK.getInstance().getPluginServices();
					try {
						MqmRestClient restClient = restClientFactory.obtain(mqmConfig.location, mqmConfig.sharedSpace, mqmConfig.username, mqmConfig.password);
						AbridgedTaskPluginInfo info = new AbridgedTaskPluginInfo()
								.setSelfIdentity(serverInstanceId)
								.setSelfType(pluginServices.getServerInfo().getType().value())
								.setSelfLocation(pluginServices.getServerInfo().getUrl())
								.setApiVersion(OctaneSDK.API_VERSION)
								.setSdkVersion(OctaneSDK.SDK_VERSION)
								.setPluginVersion(OctaneSDK.getInstance().getPluginServices().getPluginInfo().getVersion())
								.setOctaneUser(pluginServices.getOctaneConfiguration().getApiKey())
								.setCiServerUser(ConfigurationService.getModel().getImpersonatedUser());
						tasksJSON = restClient.getAbridgedTasks(info);
						isConnected = false;
						connect();
						if (tasksJSON != null && !tasksJSON.isEmpty()) {
							dispatchTasks(tasksJSON);
						}
					} catch (AuthenticationException ae) {
						isConnected = false;
						logger.error("connection to MQM Server temporary failed: authentication error", ae);
						try {
							Thread.sleep(20000);
						} catch (InterruptedException ie) {
							logger.info("interrupted while breathing on temporary exception, continue to re-connect...");
						}
						connect();
					} catch (TemporarilyUnavailableException tue) {
						isConnected = false;
						logger.error("connection to MQM Server temporary failed: resource not available", tue);
						try {
							Thread.sleep(20000);
						} catch (InterruptedException ie) {
							logger.info("interrupted while breathing on temporary exception, continue to re-connect...");
						}
						connect();
					} catch (ServerException se) {
						isConnected = false;
						logger.error("connection to MQM Server temporary failed: " + se.getMessage(), se);
						try {
							Thread.sleep(10000);
						} catch (InterruptedException ie) {
							logger.info("interrupted while breathing on temporary exception, continue to re-connect...");
						}
						connect();
					} catch (Exception e) {
						isConnected = false;
						logger.error("connection to MQM Server temporary failed: " + e.getMessage(), e);
						try {
							Thread.sleep(1000);
						} catch (InterruptedException ie) {
							logger.info("interrupted while breathing on temporary exception, continue to re-connect...");
						}
						connect();
					}
				}
			});
		} else if (shuttingDown) {
			logger.info("bridge client stopped");
		}
	}

	void dispose() {
		//  TODO: disconnect current connection once async connectivity is possible
		shuttingDown = true;
	}

	private void dispatchTasks(String tasksJSON) {
		try {
			OctaneTaskAbridged[] tasks = dtoFactory.dtoCollectionFromJson(tasksJSON, OctaneTaskAbridged[].class);

			logger.info("received " + tasks.length + " task(s)");
			for (final OctaneTaskAbridged task : tasks) {
				taskProcessingExecutors.execute(new Runnable() {
					@Override
					public void run() {
						try {
							TasksProcessor TasksProcessor = OctaneSDK.getInstance().getTasksProcessor();
							OctaneResultAbridged result = TasksProcessor.execute(task);
							MqmRestClient restClient = restClientFactory.obtain(
									mqmConfig.location,
									mqmConfig.sharedSpace,
									mqmConfig.username,
									mqmConfig.password);

							int submitStatus = restClient.putAbridgedResult(
								serverInstanceId,
								result.getId(),
								dtoFactory.dtoToJson(result));
							logger.info("result for task '" + result.getId() + "' submitted with status " + submitStatus);
						} catch (Exception e) {
							logger.error("failed to submit task '" + task.getId(), e);
						}
					}
				});
			}
		} catch (Exception e) {
			logger.error("failed to process tasks: " + e.getMessage(), e);
		}
	}

	@Exported(inline = true)
	public String getLocation() {
		return mqmConfig.location;
	}

	@Exported(inline = true)
	public String getSharedSpace() {
		return mqmConfig.sharedSpace;
	}

	@Exported(inline = true)
	public String getUsername() {
		return mqmConfig.username;
	}

	private static final class AbridgedConnectivityExecutorsFactory implements ThreadFactory {

		@Override
		public Thread newThread(@Nonnull Runnable runnable) {
			Thread result = new Thread(runnable);
			result.setName("AbridgedConnectivityThread-" + result.getId());
			result.setDaemon(true);
			return result;
		}
	}

	private static final class AbridgedTasksExecutorsFactory implements ThreadFactory {

		@Override
		public Thread newThread(@Nonnull Runnable runnable) {
			Thread result = new Thread(runnable);
			result.setName("AbridgedTasksExecutorsFactory-" + result.getId());
			result.setDaemon(true);
			return result;
		}
	}
}
