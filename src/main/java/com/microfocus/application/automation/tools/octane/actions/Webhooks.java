/*
 *
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * © Copyright 2012-2018 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors (“Micro Focus”) are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 *
 */

package com.microfocus.application.automation.tools.octane.actions;

import com.hp.octane.integrations.OctaneSDK;
import com.microfocus.application.automation.tools.model.SonarHelper;
import com.microfocus.application.automation.tools.model.WebhookExpectationAction;
import com.microfocus.application.automation.tools.octane.configuration.ConfigApi;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.maven.MavenModuleSet;
import hudson.model.*;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.io.*;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: gullery
 * Date: 8/10/14
 * Time: 12:47 PM
 * To change this template use File | Settings | File Templates.
 */

@Extension
public class Webhooks implements UnprotectedRootAction  {
	private static final Logger logger = LogManager.getLogger(Webhooks.class);
	// url details
	public static final String WEBHOOK_PATH =  "webhooks";
	public static final String NOTIFY_METHOD =  "/notify";

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

	public ConfigApi getConfiguration() {
		return new ConfigApi();
	}


	@RequirePOST
	public void doNotify(StaplerRequest req, StaplerResponse res) throws IOException { logger.info("Received POST from " + req.getRemoteHost());
		// legal user, handle request
		JSONObject inputNotification = (JSONObject) JSONValue.parse(req.getInputStream());
		Object properties = inputNotification.get("properties");
		// without build context, could not send octane relevant data
		if (!req.getHeader(PROJECT_KEY_HEADER).isEmpty() && properties != null && properties instanceof HashMap) {
			// get relevant parameters
			HashMap sonarAttachedProperties = ((HashMap) properties);
			// filter notifications from sonar projects, who haven't configured listener parameters
			if (sonarAttachedProperties.containsKey(BUILD_NUMBER_PARAM_NAME) && sonarAttachedProperties.containsKey(JOB_NAME_PARAM_NAME)) {
				String buildId = (String)(sonarAttachedProperties.get(BUILD_NUMBER_PARAM_NAME));
				String jobName = (String) sonarAttachedProperties.get(JOB_NAME_PARAM_NAME);
				// get sonar details from job configuration
				TopLevelItem jenkinsJob = Jenkins.getInstance().getItem(jobName);
				if (isValidJenkinsJob(jenkinsJob)) {
					AbstractProject jenkinsProject = ((AbstractProject) jenkinsJob);
					Integer buildNumber = Integer.valueOf(buildId, 10);
					if (isValidJenkinsBuildNumber(jenkinsProject, buildNumber)) {
						AbstractBuild build = getBuild(jenkinsProject, buildNumber);
						if (build != null && isBuildExpectingToGetWebhookCall(build) && !isBuildAlreadyGotWebhookCall(build)) {
							WebhookExpectationAction action = build.getAction(WebhookExpectationAction.class);
							ExtensionList<GlobalConfiguration> allConfigurations = GlobalConfiguration.all();
							GlobalConfiguration sonarConfiguration = allConfigurations.getDynamic(SonarHelper.SONAR_GLOBAL_CONFIG);
							if (sonarConfiguration != null) {
								String sonarToken = SonarHelper.getSonarInstallationTokenByUrl(build, sonarConfiguration, action.getServerUrl());
								HashMap project = (HashMap) inputNotification.get(PROJECT);
								String sonarProjectKey = (String) project.get(SONAR_PROJECT_KEY_NAME);

								// use SDK to fetch and push data
								OctaneSDK.getInstance().getSonarService().enqueueFetchAndPushSonarCoverageToOctane(jobName, buildId, sonarProjectKey, action.getServerUrl(), sonarToken);
								markBuildAsRecievedWebhookCall(build);
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
			}
		}
	}

	/**
	 * this method checks if build already got webhook call.
	 * we are only handling the first call, laters call for the same build
	 * will be rejected
	 * @param build
	 * @return
	 */
	private Boolean isBuildAlreadyGotWebhookCall(AbstractBuild build) {
		try {
			// build is promised to be exist at this point
			File rootDir = build.getRootDir();
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
	 * @param build
	 * @return true or false
	 */
	private Boolean isBuildExpectingToGetWebhookCall(AbstractBuild build) {
		WebhookExpectationAction action = build.getAction(WebhookExpectationAction.class);
		if (action != null && action.getExpectingToGetWebhookCall()) {
			return true;
		}
		return false;
	}


	// we may get notifications from sonar project of jobs without sonar configuration
	// or jobs of other CI servers, using this method, we ignore these notifications
	private boolean isValidJenkinsJob(TopLevelItem jenkinsJob) {
		return jenkinsJob != null && jenkinsJob instanceof AbstractProject;
	}

	// get build build number and jenkins job
	private AbstractBuild getBuild(AbstractProject jenkinsProject, int buildNumber) {
		if (jenkinsProject instanceof MavenModuleSet) {
			return ((MavenModuleSet) jenkinsProject).getBuildByNumber(buildNumber);
		} else if (jenkinsProject instanceof Project) {
			return (jenkinsProject).getBuildByNumber(buildNumber);
		}
		return null;
	}
	// we may get notifications from sonar project of jobs without sonar configuration
	// or jobs of other CI servers, using this method, we ignore these notifications
	private boolean isValidJenkinsBuildNumber(AbstractProject jenkinsProject, Integer buildNumber) {
		// in jenkins, all build ids are numbers
		try {
			return getBuild(jenkinsProject, buildNumber) != null;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * this method persist the fact a specific build got webhook call.
	 * @param build
	 * @throws IOException
	 */
	private void markBuildAsRecievedWebhookCall(AbstractBuild build) throws IOException {
		if (build == null) {
			return;
		}
		File buildBaseFolder = build.getRootDir();
		File isExpectingFile = new File(buildBaseFolder, IS_EXPECTING_FILE_NAME);
		FileOutputStream fos = new FileOutputStream(isExpectingFile);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(true);
	}
}
