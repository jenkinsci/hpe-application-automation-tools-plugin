/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2019 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors ("Micro Focus") are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.octane.actions;

import com.hp.octane.integrations.OctaneClient;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.services.vulnerabilities.ToolType;
import com.microfocus.application.automation.tools.octane.ImpersonationUtil;
import com.microfocus.application.automation.tools.model.OctaneServerSettingsModel;
import com.microfocus.application.automation.tools.octane.configuration.ConfigurationService;
import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import com.microfocus.application.automation.tools.octane.model.SonarHelper;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.*;
import hudson.security.ACLContext;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.logging.log4j.Logger;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: gullery
 * Date: 8/10/14
 * Time: 12:47 PM
 * To change this template use File | Settings | File Templates.
 */

@Extension
public class Webhooks implements UnprotectedRootAction {
	private static final Logger logger = SDKBasedLoggerProvider.getLogger(Webhooks.class);
	// url details
	public static final String WEBHOOK_PATH = "webhooks";
	public static final String NOTIFY_METHOD = "/notify";

	private String PROJECT_KEY_KEY = "PROJECT_KEY";
	private String SONAR_URL_KEY = "SONAR_URL";
	private String SONAR_TOKEN_KEY = "SONAR_TOKEN";
	private String REMOTE_TAG_KEY = "REMOTE_TAG";

	// json parameter names
	private final String PROJECT = "project";
	private final String SONAR_PROJECT_KEY_NAME = "key";
	private final String IS_EXPECTING_FILE_NAME = "is_expecting.txt";
	private final String JOB_NAME_PARAM_NAME = "sonar.analysis.jobName";
	private final String BUILD_NUMBER_PARAM_NAME = "sonar.analysis.buildNumber";
	private static final String PROJECT_KEY_HEADER = "X-SonarQube-Project";

	public String getIconFileName() {
		return null;
	}

	public String getDisplayName() {
		return null;
	}

	public String getUrlName() {
		return WEBHOOK_PATH;
	}

	@RequirePOST
	public void doNotify(StaplerRequest req, StaplerResponse res) throws IOException {
		logger.info("Received POST from " + req.getRemoteHost());
		// legal user, handle request
		JSONObject inputNotification = (JSONObject) JSONValue.parse(req.getInputStream());
		Object properties = inputNotification.get("properties");
		// without build context, could not send octane relevant data
		if (!req.getHeader(PROJECT_KEY_HEADER).isEmpty() && properties instanceof Map) {
			// get relevant parameters
			Map sonarAttachedProperties = (Map) properties;
			// filter notifications from sonar projects, who haven't configured listener parameters
			if (sonarAttachedProperties.containsKey(BUILD_NUMBER_PARAM_NAME) && sonarAttachedProperties.containsKey(JOB_NAME_PARAM_NAME)) {
				String buildId = (String) (sonarAttachedProperties.get(BUILD_NUMBER_PARAM_NAME));
				String jobName = (String) sonarAttachedProperties.get(JOB_NAME_PARAM_NAME);
				Run run = null;
				for (OctaneClient octaneClient : OctaneSDK.getClients()) {
					ACLContext aclContext = null;
					try {
						String octaneInstanceId = octaneClient.getInstanceId();
						aclContext = ImpersonationUtil.startImpersonation(octaneInstanceId);
						Item topLevelItem = Jenkins.get().getItemByFullName(jobName);
						if (isValidJenkinsJob(topLevelItem)) {
							Job jenkinsJob = ((Job) topLevelItem);
							Integer buildNumber = Integer.valueOf(buildId, 10);
							if (isValidJenkinsBuildNumber(jenkinsJob, buildNumber)) {
								run = getRun(jenkinsJob, buildNumber);
								if (run != null && isRunExpectingToGetWebhookCall(run) && !isRunAlreadyGotWebhookCall(run)) {
									WebhookAction action = run.getAction(WebhookAction.class);
									ExtensionList<GlobalConfiguration> allConfigurations = GlobalConfiguration.all();
									GlobalConfiguration sonarConfiguration = allConfigurations.getDynamic(SonarHelper.SONAR_GLOBAL_CONFIG);
									if (sonarConfiguration != null) {
										String sonarToken = SonarHelper.getSonarInstallationTokenByUrl(sonarConfiguration, action.getServerUrl(), run);
										HashMap project = (HashMap) inputNotification.get(PROJECT);
										String sonarProjectKey = (String) project.get(SONAR_PROJECT_KEY_NAME);

										if (action.getDataTypeSet().contains(SonarHelper.DataType.COVERAGE)) {
											// use SDK to fetch and push data
											octaneClient.getSonarService().enqueueFetchAndPushSonarCoverage(jobName, buildId, sonarProjectKey, action.getServerUrl(), sonarToken);
										}
										if (action.getDataTypeSet().contains(SonarHelper.DataType.VULNERABILITIES)) {
											Map<String, String> additionalProperties = new HashMap<>();
											additionalProperties.put(PROJECT_KEY_KEY, sonarProjectKey);
											additionalProperties.put(SONAR_URL_KEY, action.getServerUrl());
											additionalProperties.put(SONAR_TOKEN_KEY, sonarToken);
											additionalProperties.put(REMOTE_TAG_KEY, sonarProjectKey);
											OctaneServerSettingsModel settings = ConfigurationService.getSettings(octaneInstanceId);
											octaneClient.getVulnerabilitiesService().enqueueRetrieveAndPushVulnerabilities(jobName, buildId, ToolType.SONAR, run.getStartTimeInMillis(), settings.getMaxTimeoutHours(), additionalProperties);

										}
										res.setStatus(HttpStatus.SC_OK); // sonar should get positive feedback for webhook
									}
								} else {
									logger.warn("Got request from sonarqube webhook listener for build ," + buildId + " which is not expecting to get sonarqube data");
									res.setStatus(HttpStatus.SC_EXPECTATION_FAILED);
								}
							} else {
								logger.warn("Got request from sonarqube webhook listener, but build " + buildId + " context could not be resolved");
								res.setStatus(HttpStatus.SC_NOT_ACCEPTABLE);
							}
						}
					} catch (Exception e) {
						logger.error("exception occurred while trying to enqueue fetchAndPush task to octane, for build: " + buildId, e);
					} finally {
						if (aclContext != null) {
							ImpersonationUtil.stopImpersonation(aclContext);
						}
					}
				}
				if (run != null) {
					markBuildAsRecievedWebhookCall(run);
				}
			}
		}
	}

	/**
	 * this method checks if run already got webhook call.
	 * we are only handling the first call, laters call for the same run
	 * will be rejected
	 *
	 * @param run run
	 * @return result
	 */
	private Boolean isRunAlreadyGotWebhookCall(Run run) {
		try {
			// run is promised to be exist at this point
			File rootDir = run.getRootDir();
			File isExpectingFile = new File(rootDir, IS_EXPECTING_FILE_NAME);
			FileInputStream fis = new FileInputStream(isExpectingFile);
			ObjectInputStream ois = new ObjectInputStream(fis);
			return (Boolean) ois.readObject();
		} catch (Exception e) {
			return Boolean.FALSE;
		}
	}

	/**
	 * use build action to decide whether we need to get a webhook call from sonarqube
	 *
	 * @param run build
	 * @return true or false
	 */
	private Boolean isRunExpectingToGetWebhookCall(Run run) {
		WebhookAction action = run.getAction(WebhookAction.class);
		return action != null && action.getExpectingToGetWebhookCall();
	}

	// we may get notifications from sonar project of jobs without sonar configuration
	// or jobs of other CI servers, using this method, we ignore these notifications
	private boolean isValidJenkinsJob(Item jenkinsJob) {
		return jenkinsJob instanceof Job;
	}

	// get run by build number and jenkins job
	private Run getRun(Job jenkinsJob, int buildNumber) {
		return jenkinsJob.getBuildByNumber(buildNumber);
	}

	// we may get notifications from sonar project of jobs without sonar configuration
	// or jobs of other CI servers, using this method, we ignore these notifications
	private boolean isValidJenkinsBuildNumber(Job jenkinsJob, Integer buildNumber) {
		// in jenkins, all build ids are numbers
		try {
			return getRun(jenkinsJob, buildNumber) != null;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * this method persist the fact a specific run got webhook call.
	 *
	 * @param run run
	 * @throws IOException exception
	 */
	private void markBuildAsRecievedWebhookCall(Run run) throws IOException {
		if (run == null) {
			return;
		}
		File buildBaseFolder = run.getRootDir();
		File isExpectingFile = new File(buildBaseFolder, IS_EXPECTING_FILE_NAME);
		FileOutputStream fos = new FileOutputStream(isExpectingFile);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(true);
	}
}
