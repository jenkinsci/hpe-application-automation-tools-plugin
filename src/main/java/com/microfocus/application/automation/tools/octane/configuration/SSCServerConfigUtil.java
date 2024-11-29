/*
 *  Certain versions of software accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 *  This software was acquired by Micro Focus on September 1, 2017, and is now
 *  offered by OpenText.
 *  Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical
 *  in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the
 *  property of their respective owners.
 *  OpenText is a trademark of Open Text.
 *  __________________________________________________________________
 *  MIT License
 *
 *  Copyright 2012-2025 Open Text.
 *
 *  The only warranties for products and services of Open Text and
 *  its affiliates and licensors ("Open Text") are as may be set forth
 *  in the express warranty statements accompanying such products and services.
 *  Nothing herein should be construed as constituting an additional warranty.
 *  Open Text shall not be liable for technical or editorial errors or
 *  omissions contained herein. The information contained herein is subject
 *  to change without notice.
 *
 *  Except as specifically indicated otherwise, this document contains
 *  confidential information and a valid license is required for possession,
 *  use or copying. If this work is provided to the U.S. Government,
 *  consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 *  Computer Software Documentation, and Technical Data for Commercial Items are
 *  licensed to the U.S. Government under vendor's standard commercial license.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ___________________________________________________________________
 */
package com.microfocus.application.automation.tools.octane.configuration;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Descriptor;
import hudson.tasks.Publisher;
import jenkins.model.Jenkins;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/*
    Utility to help retrieving the configuration of the SSC Server URL and SSC project/version pair
 */

public class SSCServerConfigUtil {
	private static final Logger logger = SDKBasedLoggerProvider.getLogger(SSCServerConfigUtil.class);
	private static final String PUBLISHER_NEW_NAME = "com.fortify.plugin.jenkins.FortifyPlugin";
	private static final String PUBLISHER_OLD_VERSION = "com.fortify.plugin.jenkins.FPRPublisher";

	private static final String FORTIFY_UPLOAD_ACTION_NAME = "com.fortify.plugin.jenkins.FortifyUploadBuildAction";
	private static final String FORTIFY_UPLOAD_PROJECT_ACTIONS_METHOD = "getProjectActions";
	private static final String FORTIFY_UPLOAD_APP_NAME_METHOD = "getAppName";
	private static final String FORTIFY_UPLOAD_APP_VERSION_METHOD = "getAppVersion";

	public static String getSSCServer() {
		Descriptor sscDescriptor = getSSCDescriptor();
		return sscDescriptor != null ? getFieldValue(sscDescriptor, "url") : null;
	}

	/**
	 * extracts and returns SSC project name and version if found on the AbstractProject configuration of the specified build
	 *
	 * @param build AbstractBuild
	 * @return valid SSC project name and version pair; otherwise NULL
	 */
	public static SSCProjectVersionPair getProjectConfigurationFromBuild(AbstractBuild build) {
		return build != null ? getProjectVersion(build.getProject()) : null;
	}

	public static SSCProjectVersionPair getProjectConfigurationFromWorkflowRun(WorkflowRun run) {

		SSCProjectVersionPair projectVersionPair = null;
		List<Action> workflowActions = run != null ? (List<Action>) run.getAllActions() : new ArrayList<>();

		for (Action action: workflowActions) {
			if (action.getClass().getName().equals(FORTIFY_UPLOAD_ACTION_NAME)) {
				try {
					List<Action> projectActions = (List<Action>) ReflectionUtils.invokeMethodByName(action, FORTIFY_UPLOAD_PROJECT_ACTIONS_METHOD, null);
					Action projectMethods = projectActions != null && projectActions.size() > 0 ? projectActions.get(0) : null;

					if (projectMethods != null) {
						String projName = (String) ReflectionUtils.invokeMethodByName(projectMethods, FORTIFY_UPLOAD_APP_NAME_METHOD, null);
						String version = (String) ReflectionUtils.invokeMethodByName(projectMethods, FORTIFY_UPLOAD_APP_VERSION_METHOD, null);

						projectVersionPair = new SSCProjectVersionPair(projName, version);
					}
				} catch(Exception e) {
					logger.error("Failed getProjectConfigurationFromWorkflowRun", e);
				}
			}
		}

		return projectVersionPair;
	}

	private static SSCProjectVersionPair getProjectVersion(AbstractProject project) {
		for (Object publisher : project.getPublishersList()) {
			if (publisher instanceof Publisher &&
					isSSCPublisher(publisher.getClass().getName())) {
				return getProjectNameByReflection(publisher);
			}
		}
		return null;
	}

	private static boolean isSSCPublisher(String publisherName) {
		return PUBLISHER_NEW_NAME.equals(publisherName) ||
				PUBLISHER_OLD_VERSION.equals(publisherName);
	}

	private static SSCProjectVersionPair getProjectNameByReflection(Object fprPublisher) {
		String projectName = getFieldValue(fprPublisher, "projectName");
		String projectVersion = getFieldValue(fprPublisher, "projectVersion");
		if (projectName != null && !projectName.isEmpty() && projectVersion != null && !projectVersion.isEmpty()) {
			return new SSCProjectVersionPair(projectName, projectVersion);
		}
		logger.warn("Version seems to be 18.20.1071 or higher");
		//18.20.1071 version.
		Object uploadSSC = getFieldValueAsObj(fprPublisher, "uploadSSC");
		if (uploadSSC == null) {
			Object analysisRunType = getFieldValueAsObj(fprPublisher, "analysisRunType");
			if (analysisRunType == null) {
				logger.warn("uploadSSC section was not found");
			} else {
				uploadSSC = getFieldValueAsObj(analysisRunType, "uploadSSC");
				if (uploadSSC == null) {
					logger.warn("uploadSSC section was not found");
				} else {
					projectName = getFieldValue(uploadSSC, "appName");
					projectVersion = getFieldValue(uploadSSC, "appVersion");
					if (projectName != null && !projectName.isEmpty() && projectVersion != null && !projectVersion.isEmpty()) {
						return new SSCProjectVersionPair(projectName, projectVersion);
					}
				}

				logger.warn("uploadSSC section was not found");
			}
		} else {
			logger.warn("uploadSSC was found ");
			projectName = getFieldValue(uploadSSC, "projectName");
			projectVersion = getFieldValue(uploadSSC, "projectVersion");
			logger.warn("projectName" + projectName + " , ProjectVersion" + projectVersion);
		}
		if (projectName != null && !projectName.isEmpty() && projectVersion != null && !projectVersion.isEmpty()) {
			return new SSCProjectVersionPair(projectName, projectVersion);
		}
		return null;
	}

	private static String getFieldValue(Object someObject, String fieldName) {
		for (Field field : someObject.getClass().getDeclaredFields()) {
			field.setAccessible(true);
			if (field.getName().equals(fieldName)) {
				Object value = null;
				try {
					value = field.get(someObject);
				} catch (IllegalAccessException e) {
					logger.error("Failed to getFieldValue", e);
				}
				if (value != null) {
					return value.toString();
				}
			}
		}
		return null;
	}

	private static Object getFieldValueAsObj(Object someObject, String fieldName) {
		for (Field field : someObject.getClass().getDeclaredFields()) {
			field.setAccessible(true);
			if (field.getName().equals(fieldName)) {
				try {
					return field.get(someObject);
				} catch (IllegalAccessException e) {
					logger.error("Failed to getFieldValue", e);
				}
			}
		}
		return null;
	}

	private static Descriptor getSSCDescriptor() {
		Descriptor publisher = Jenkins.get().getDescriptorByName(PUBLISHER_OLD_VERSION);
		if (publisher == null) {
			//18.20 version and above.
			logger.debug("didn't find Old SSC FPRPublisher");
			Descriptor plugin = Jenkins.get().getDescriptorByName(PUBLISHER_NEW_NAME);
			if (plugin == null) {
				logger.debug("didn't find Fortify Plugin of 18.20 version and above");
			}
			return plugin;
		}
		return publisher;
	}

	public static final class SSCProjectVersionPair {
		public final String project;
		public final String version;

		public SSCProjectVersionPair(String project, String version) {
			this.project = project;
			this.version = version;
		}
	}
}
