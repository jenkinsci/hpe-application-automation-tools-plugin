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

package com.hpe.application.automation.tools.run;

import com.hpe.application.automation.tools.AlmToolsUtils;
import com.hpe.application.automation.tools.EncryptionUtils;
import com.hpe.application.automation.tools.mc.JobConfigurationProxy;
import com.hpe.application.automation.tools.model.MCServerSettingsModel;
import com.hpe.application.automation.tools.model.ProxySettings;
import com.hpe.application.automation.tools.model.RunFromFileSystemModel;
import com.hpe.application.automation.tools.settings.MCServerSettingsBuilder;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.VariableResolver;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.minidev.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Objects;
import java.util.Properties;

/**
 * Describs a regular jenkins build step from UFT or LR
 */
public class RunFromFileBuilder extends Builder implements SimpleBuildStep {


	private String ResultFilename = "ApiResults.xml";
	private String ParamFileName = "ApiRun.txt";
	private final RunFromFileSystemModel runFromFileModel;
	private static final  String HP_TOOLS_LAUNCHER_EXE = "HpToolsLauncher.exe";
	private static final  String LRANALYSIS_LAUNCHER_EXE = "LRAnalysisLauncher.exe";

	/**
	 * Instantiates a new Run from file builder.
	 *
	 * @param fsTests the fs tests
	 */
	@DataBoundConstructor
	public RunFromFileBuilder(String fsTests) {

		runFromFileModel = new RunFromFileSystemModel(fsTests);
	}

	public void setFsTests(String fsTests){
		runFromFileModel.setFsTests(fsTests);
	}

	/**
	 * Instantiates a new Run from file builder.
	 *
	 * @param runFromFileModel the run from file model
	 */
	public RunFromFileBuilder(RunFromFileSystemModel runFromFileModel) {

		this.runFromFileModel = runFromFileModel;
	}

	/**
	 * @deprecated
	 * Instantiates a new Run from file builder.
	 *
	 * @param fsTests                   the fs tests
	 * @param fsTimeout                 the fs timeout
	 * @param controllerPollingInterval the controller polling interval
	 * @param perScenarioTimeOut        the per scenario time out
	 * @param ignoreErrorStrings        the ignore error strings
	 * @param displayController         the display controller
	 * @param mcServerName              the mc server name
	 * @param fsUserName                the fs user name
	 * @param fsPassword                the fs password
	 * @param fsDeviceId                the fs device id
	 * @param fsTargetLab               the fs target lab
	 * @param fsManufacturerAndModel    the fs manufacturer and model
	 * @param fsOs                      the fs os
	 * @param fsAutActions              the fs aut actions
	 * @param fsLaunchAppName           the fs launch app name
	 * @param fsDevicesMetrics          the fs devices metrics
	 * @param fsInstrumented            the fs instrumented
	 * @param fsExtraApps               the fs extra apps
	 * @param fsJobId                   the fs job id
	 * @param proxySettings             the proxy settings
	 * @param useSSL                    the use ssl
	 */
	@SuppressWarnings("squid:S00107")
	@Deprecated
	public RunFromFileBuilder(String fsTests, String fsTimeout, String fsUftRunMode, String controllerPollingInterval,
                              String perScenarioTimeOut, String ignoreErrorStrings, String displayController, String mcServerName, String fsUserName,
							  String fsPassword, String mcTenantId, String fsDeviceId, String fsTargetLab,
                              String fsManufacturerAndModel, String fsOs, String fsAutActions, String fsLaunchAppName, String fsDevicesMetrics, String fsInstrumented, String fsExtraApps, String fsJobId,
                              ProxySettings proxySettings, boolean useSSL) {

		runFromFileModel = new RunFromFileSystemModel(fsTests, fsTimeout, fsUftRunMode, controllerPollingInterval,
				perScenarioTimeOut, ignoreErrorStrings, displayController, mcServerName, fsUserName, fsPassword, mcTenantId, fsDeviceId, fsTargetLab, fsManufacturerAndModel, fsOs, fsAutActions, fsLaunchAppName,
				fsDevicesMetrics, fsInstrumented, fsExtraApps, fsJobId, proxySettings, useSSL);
	}

	/**
	 * Gets param file name.
	 *
	 * @return the param file name
	 */
	public String getParamFileName() {
		return ParamFileName;
	}

	/**
	 * Sets controller polling interval.
	 *
	 * @param controllerPollingInterval the controller polling interval
	 */
	@DataBoundSetter
	public void setControllerPollingInterval(String controllerPollingInterval) {
		runFromFileModel.setControllerPollingInterval(controllerPollingInterval);
	}

	/**
	 * Sets per scenario time out.
	 *
	 * @param perScenarioTimeOut the per scenario time out
	 */
	@DataBoundSetter
	public void setPerScenarioTimeOut(String perScenarioTimeOut) {
		runFromFileModel.setPerScenarioTimeOut(perScenarioTimeOut);
	}

	/**
	 * Sets ignore error strings.
	 *
	 * @param ignoreErrorStrings the ignore error strings
	 */
	@DataBoundSetter
	public void setIgnoreErrorStrings(String ignoreErrorStrings) {
		runFromFileModel.setIgnoreErrorStrings(ignoreErrorStrings);
	}

	/**
	 * Sets display controller.
	 *
	 * @param displayController the display controller
	 */
	@DataBoundSetter
	public void setDisplayController(String displayController) {
		runFromFileModel.setDisplayController(displayController);
	}

	/**
	 * Sets fs timeout.
	 *
	 * @param fsTimeout the fs timeout
	 */
	@DataBoundSetter
	public void setFsTimeout(String fsTimeout) {
		runFromFileModel.setFsTimeout(fsTimeout);
	}

	/**
	 * Sets fs runMode.
	 *
	 * @param fsUftRunMode the fs runMode
	 */
	@DataBoundSetter
	public void setFsUftRunMode(String fsUftRunMode) {
		runFromFileModel.setFsUftRunMode(fsUftRunMode);
	}

	/**
	 * Sets mc server name.
	 *
	 * @param mcServerName the mc server name
	 */
	@DataBoundSetter
	public void setMcServerName(String mcServerName) {
		runFromFileModel.setMcServerName(mcServerName);
	}

    /**
     * Sets mc server name.
     *
     * @param useSSL the mc server name
     */
    @DataBoundSetter
    public void setuseSSL(boolean useSSL) {
        runFromFileModel.setUseSSL(useSSL);
    }


    /**
	 * Sets fs user name.
	 *
	 * @param fsUserName the fs user name
	 */
	@DataBoundSetter
	public void setFsUserName(String fsUserName) {
		runFromFileModel.setFsUserName(fsUserName);
	}

	/**
	 * Sets fs password.
	 *
	 * @param fsPassword the fs password
	 */
	@DataBoundSetter
	public void setFsPassword(String fsPassword) {
		runFromFileModel.setFsPassword(fsPassword);
	}


	@DataBoundSetter
	public void setMcTenantId(String mcTenantId) {
		runFromFileModel.setMcTenantId(mcTenantId);
	}



	/**
	 * Sets fs device id.
	 *
	 * @param fsDeviceId the fs device id
	 */
	@DataBoundSetter
	public void setFsDeviceId(String fsDeviceId) {
		runFromFileModel.setFsDeviceId(fsDeviceId);
	}

	/**
	 * Sets fs os.
	 *
	 * @param fsOs the fs os
	 */
	@DataBoundSetter
	public void setFsOs(String fsOs) {
		runFromFileModel.setFsOs(fsOs);
	}

	/**
	 * Sets fs manufacturer and model.
	 *
	 * @param fsManufacturerAndModel the fs manufacturer and model
	 */
	@DataBoundSetter
	public void setFsManufacturerAndModel(String fsManufacturerAndModel) {
		runFromFileModel.setFsManufacturerAndModel(fsManufacturerAndModel);
	}

	/**
	 * Sets fs target lab.
	 *
	 * @param fsTargetLab the fs target lab
	 */
	@DataBoundSetter
	public void setFsTargetLab(String fsTargetLab) {
		runFromFileModel.setFsTargetLab(fsTargetLab);
	}

	/**
	 * Sets fs aut actions.
	 *
	 * @param fsAutActions the fs aut actions
	 */
	@DataBoundSetter
	public void setFsAutActions(String fsAutActions) {
		runFromFileModel.setFsAutActions(fsAutActions);
	}

	/**
	 * Sets fs launch app name.
	 *
	 * @param fsLaunchAppName the fs launch app name
	 */
	@DataBoundSetter
	public void setFsLaunchAppName(String fsLaunchAppName) {
		runFromFileModel.setFsLaunchAppName(fsLaunchAppName);
	}

	/**
	 * Sets fs instrumented.
	 *
	 * @param fsInstrumented the fs instrumented
	 */
	@DataBoundSetter
	public void setFsInstrumented(String fsInstrumented) {
		runFromFileModel.setFsInstrumented(fsInstrumented);
	}

	/**
	 * Sets fs devices metrics.
	 *
	 * @param fsDevicesMetrics the fs devices metrics
	 */
	@DataBoundSetter
	public void setFsDevicesMetrics(String fsDevicesMetrics) {
		runFromFileModel.setFsDevicesMetrics(fsDevicesMetrics);
	}

	/**
	 * Sets fs extra apps.
	 *
	 * @param fsExtraApps the fs extra apps
	 */
	@DataBoundSetter
	public void setFsExtraApps(String fsExtraApps) {
		runFromFileModel.setFsExtraApps(fsExtraApps);
	}

	/**
	 * Sets fs job id.
	 *
	 * @param fsJobId the fs job id
	 */
	@DataBoundSetter
	public void setFsJobId(String fsJobId) {
		runFromFileModel.setFsJobId(fsJobId);
	}

	/**
	 * Sets proxy settings.
	 *
	 * @param proxySettings the proxy settings
	 */
	@DataBoundSetter
	public void setProxySettings(ProxySettings proxySettings) {
		runFromFileModel.setProxySettings(proxySettings);
	}

	@Override
	public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener)

			throws InterruptedException, IOException {

		// get the mc server settings
		MCServerSettingsModel mcServerSettingsModel = getMCServerSettingsModel();

		EnvVars env = null;
		try {
			env = build.getEnvironment(listener);

		} catch (IOException | InterruptedException e) {
			listener.error("Failed loading build environment " + e);
		}



		// this is an unproper replacment to the build.getVariableResolver since workflow run won't support the
		// getBuildEnviroment() as written here:
		// https://github.com/jenkinsci/pipeline-plugin/blob/893e3484a25289c59567c6724f7ce19e3d23c6ee/DEVGUIDE.md#variable-substitutions

		JSONObject jobDetails = null;
		String mcServerUrl = "";
		// now merge them into one list
		Properties mergedProperties = new Properties();
		if (mcServerSettingsModel != null) {
			mcServerUrl = mcServerSettingsModel.getProperties().getProperty("MobileHostAddress");
			if (runFromFileModel.getProxySettings() == null) {
				jobDetails = runFromFileModel.getJobDetails(mcServerUrl, null, null, null);
			} else {
				jobDetails = runFromFileModel.getJobDetails(mcServerUrl, runFromFileModel.getProxySettings().getFsProxyAddress(), runFromFileModel.getProxySettings().getFsProxyUserName(),
						runFromFileModel.getProxySettings().getFsProxyPassword());
			}
			mergedProperties.setProperty("mobileinfo", jobDetails != null ? jobDetails.toJSONString() : "");
			mergedProperties.setProperty("MobileHostAddress", mcServerUrl);
		}

		if (runFromFileModel != null && StringUtils.isNotBlank(runFromFileModel.getFsPassword())) {
			try {
				String encPassword = EncryptionUtils.Encrypt(runFromFileModel.getFsPassword(), EncryptionUtils.getSecretKey());
				mergedProperties.put("MobilePassword", encPassword);
			} catch (Exception e) {
				build.setResult(Result.FAILURE);
				listener.fatalError("problem in mobile center password encryption" + e);
			}
		}

		if(env == null)
		{
			listener.fatalError("Enviroment not set");
			throw new IOException("Env Null - something went wrong with fetching jenkins build environment");
		}
		if(build instanceof AbstractBuild)
		{
			VariableResolver<String> varResolver = ((AbstractBuild) build).getBuildVariableResolver();
			mergedProperties.putAll(runFromFileModel.getProperties(env, varResolver));
		}
		else
		{
			mergedProperties.putAll(runFromFileModel.getProperties(env));
		}


		int idx = 0;
		for (Iterator<String> iterator = env.keySet().iterator(); iterator.hasNext(); ) {
			String key = iterator.next();
			idx++;
			mergedProperties.put("JenkinsEnv" + idx, key + ";" + env.get(key));
		}

		Date now = new Date();
		Format formatter = new SimpleDateFormat("ddMMyyyyHHmmssSSS");
		String time = formatter.format(now);

		// get a unique filename for the params file
		ParamFileName = "props" + time + ".txt";
		ResultFilename = "Results" + time + ".xml";

		mergedProperties.put("runType", AlmRunTypes.RunType.FileSystem.toString());
		mergedProperties.put("resultsFilename", ResultFilename);

		//handling mtbx file content :
		// If we have mtbx content - it is located in Test1 property and there is no other test properties (like Test2 etc)
		// We save mtbx content in workspace and replace content of Test1 by reference to saved file
		String firstTestKey = "Test1";
		String firstTestContent = mergedProperties.getProperty(firstTestKey, "");
		if (RunFromFileSystemModel.isMtbxContent(firstTestContent)) {
			try {
				String mtbxFilePath = createMtbxFileInWs(workspace, firstTestContent, time);
				mergedProperties.setProperty(firstTestKey, mtbxFilePath);
			} catch (IOException | InterruptedException e) {
				build.setResult(Result.FAILURE);
				listener.error("Failed to save MTBX file : " + e.getMessage());
			}
		}

		// get properties serialized into a stream
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		try {
			mergedProperties.store(stream, "");
		} catch (IOException e) {
			listener.error("Storing run variable failed: " + e);
			build.setResult(Result.FAILURE);
		}
		String propsSerialization = stream.toString();
		FilePath CmdLineExe;
		try (InputStream propsStream = IOUtils.toInputStream(propsSerialization)) {

            // Get the URL to the Script used to run the test, which is bundled
			// in the plugin
			@SuppressWarnings("squid:S2259")
			URL cmdExeUrl = Jenkins.getInstance().pluginManager.uberClassLoader.getResource(HP_TOOLS_LAUNCHER_EXE);
			if (cmdExeUrl == null) {
				listener.fatalError(HP_TOOLS_LAUNCHER_EXE + " not found in resources");
				return;
			}

			@SuppressWarnings("squid:S2259")
			URL cmdExe2Url = Jenkins.getInstance().pluginManager.uberClassLoader.getResource(LRANALYSIS_LAUNCHER_EXE);
			if (cmdExe2Url == null) {
				listener.fatalError(LRANALYSIS_LAUNCHER_EXE + "not found in resources");
				return;
			}

			FilePath propsFileName = workspace.child(ParamFileName);
			CmdLineExe = workspace.child(HP_TOOLS_LAUNCHER_EXE);
			FilePath CmdLineExe2 = workspace.child(LRANALYSIS_LAUNCHER_EXE);

			try {
				// create a file for the properties file, and save the properties
				propsFileName.copyFrom(propsStream);

				// Copy the script to the project workspace
				CmdLineExe.copyFrom(cmdExeUrl);

				CmdLineExe2.copyFrom(cmdExe2Url);

			} catch (IOException | InterruptedException e) {
				build.setResult(Result.FAILURE);
				listener.error("Copying executable files to executing node " + e);
			}
		}

		try {
			// Run the HpToolsLauncher.exe
			AlmToolsUtils.runOnBuildEnv(build, launcher, listener, CmdLineExe, ParamFileName);
			// Has the report been successfully generated?
		} catch (IOException ioe) {
			Util.displayIOException(ioe, listener);
			build.setResult(Result.FAILURE);
			listener.error("Failed running HpToolsLauncher " + ioe);
			return;
		} catch (InterruptedException e) {
			build.setResult(Result.ABORTED);
			PrintStream out = listener.getLogger();
			listener.error("Failed running HpToolsLauncher - build aborted " + e);

			try {
				AlmToolsUtils.runHpToolsAborterOnBuildEnv(build, launcher, listener, ParamFileName, workspace);
			} catch (IOException e1) {
				Util.displayIOException(e1, listener);
				build.setResult(Result.FAILURE);
				return;
			} catch (InterruptedException e1) {
				listener.error("Failed running HpToolsAborter " + e1);
			}
			out.println("Operation Was aborted by user.");
		}
	}

	private static String createMtbxFileInWs(FilePath workspace, String mtbxContent, String timeString) throws IOException, InterruptedException {
		String fileName = "test_suite_" + timeString + ".mtbx";

		FilePath remoteFile = workspace.child(fileName);

		String mtbxContentUpdated = mtbxContent.replace("${WORKSPACE}", workspace.getRemote());
		InputStream in = IOUtils.toInputStream(mtbxContentUpdated, "UTF-8");
		remoteFile.copyFrom(in);

		return remoteFile.getRemote();
	}

	/**
	 * Gets mc server settings model.
	 *
	 * @return the mc server settings model
	 */
	public MCServerSettingsModel getMCServerSettingsModel() {
		for (MCServerSettingsModel mcServer : getDescriptor().getMcServers()) {
			if (this.runFromFileModel != null
					&& runFromFileModel.getMcServerName() != null
					&& mcServer.getMcServerName() != null
					&& runFromFileModel.getMcServerName().equals(mcServer.getMcServerName())) {
				return mcServer;
			}
		}
		return null;
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	/**
	 * Gets run from file model.
	 *
	 * @return the run from file model
	 */
	public RunFromFileSystemModel getRunFromFileModel() {
		return runFromFileModel;
	}

	/**
	 * Gets run results file name.
	 *
	 * @return the run results file name
	 */
	public String getRunResultsFileName() {
		return ResultFilename;
	}

	/**
	 * The type Descriptor.
	 */
	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		/**
		 * The Instance.
		 */
		JobConfigurationProxy instance = JobConfigurationProxy.getInstance();

		/**
		 * Instantiates a new Descriptor.
		 */
		public DescriptorImpl() {
			load();
		}


		@Override
		public boolean isApplicable(
				@SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {
			return true;
		}

		/**
		 * Gets job id.
		 * If there is already a job created by jenkins plugin, and exists then return this job id,
		 * otherwise, create a new temp job and return the new job id.
		 *
		 * @param mcUrl         the mc url
		 * @param mcUserName    the mc user name
		 * @param mcPassword    the mc password
		 * @param proxyAddress  the proxy address
		 * @param proxyUserName the proxy user name
		 * @param proxyPassword the proxy password
		 * @param previousJobId the previous job id
		 * @return the job id
		 */
		@JavaScriptMethod
		public String getJobId(String mcUrl, String mcUserName, String mcPassword, String mcTenantId, String proxyAddress, String proxyUserName, String proxyPassword, String previousJobId) {
			if(null != previousJobId && !previousJobId.isEmpty()){
                JSONObject jobJSON = instance.getJobById(mcUrl, mcUserName, mcPassword, mcTenantId, proxyAddress, proxyUserName, proxyPassword, previousJobId);
                if(jobJSON != null && previousJobId.equals(jobJSON.getAsString("id"))){
                    return previousJobId;
                }else {
                    return instance.createTempJob(mcUrl, mcUserName, mcPassword, mcTenantId, proxyAddress, proxyUserName, proxyPassword);
                }
			}
			return instance.createTempJob(mcUrl, mcUserName, mcPassword, mcTenantId, proxyAddress, proxyUserName, proxyPassword);
		}

		/**
		 * Populate app and device json object.
		 *
		 * @param mcUrl         the mc url
		 * @param mcUserName    the mc user name
		 * @param mcPassword    the mc password
		 * @param proxyAddress  the proxy address
		 * @param proxyUserName the proxy user name
		 * @param proxyPassword the proxy password
		 * @param jobId         the job id
		 * @return the json object
		 */
		@JavaScriptMethod
		public JSONObject populateAppAndDevice(String mcUrl, String mcUserName, String mcPassword, String mcTenantId, String proxyAddress, String proxyUserName, String proxyPassword, String jobId) {
			return instance.getJobJSONData(mcUrl, mcUserName, mcPassword, mcTenantId, proxyAddress, proxyUserName, proxyPassword, jobId);
		}

		/**
		 * Gets mc server url.
		 *
		 * @param serverName the server name
		 * @return the mc server url
		 */
		@SuppressWarnings("squid:S2259")
		@JavaScriptMethod
		public String getMcServerUrl(String serverName) {
			String serverUrl = "";
			MCServerSettingsModel[] servers = Jenkins.getInstance().getDescriptorByType(
					MCServerSettingsBuilder.MCDescriptorImpl.class).getInstallations();
			for (MCServerSettingsModel mcServer : servers) {
				if (mcServer.getMcServerName().equals(serverName)) {
					serverUrl = mcServer.getMcServerUrl();
				}
			}
			return serverUrl;
		}

		@Override
		public String getDisplayName() {
			return "Execute HPE tests from file system";
		}

		/**
		 * Do check fs tests form validation.
		 *
		 * @param value the value
		 * @return the form validation
		 */
		@SuppressWarnings("squid:S1172")
		public FormValidation doCheckFsTests(@QueryParameter String value) {
			return FormValidation.ok();
		}

		/**
		 * Do check ignore error strings form validation.
		 *
		 * @param value the value
		 * @return the form validation
		 */
		@SuppressWarnings("squid:S1172")
		public FormValidation doCheckIgnoreErrorStrings(@QueryParameter String value) {

			return FormValidation.ok();
		}

		/**
		 * Do check fs timeout form validation.
		 *
		 * @param value the value
		 * @return the form validation
		 */
		public FormValidation doCheckFsTimeout(@QueryParameter String value) {
			if (StringUtils.isEmpty(value)) {
				return FormValidation.ok();
			}

			String val1 = value.trim();
			if (val1.length() > 0 && val1.charAt(0) == '-')
				val1 = val1.substring(1);

			if (!StringUtils.isNumeric(val1) && !Objects.equals(val1, "")) {
				return FormValidation.error("Timeout name must be a number");
			}
			return FormValidation.ok();
		}

		/**
		 * Has mc servers boolean.
		 *
		 * @return the boolean
		 */
		@SuppressWarnings("squid:S2259")
		public boolean hasMCServers() {
			return Jenkins.getInstance().getDescriptorByType(
					MCServerSettingsBuilder.MCDescriptorImpl.class).hasMCServers();
		}

		/**
		 * Get mc servers mc server settings model [ ].
		 *
		 * @return the mc server settings model [ ]
		 */
		@SuppressWarnings("squid:S2259")

		public MCServerSettingsModel[] getMcServers() {
			return Jenkins.getInstance().getDescriptorByType(
					MCServerSettingsBuilder.MCDescriptorImpl.class).getInstallations();
		}

		/**
		 * Do check controller polling interval form validation.
		 *
		 * @param value the value
		 * @return the form validation
		 */
		public FormValidation doCheckControllerPollingInterval(@QueryParameter String value) {
			if (StringUtils.isEmpty(value)) {
				return FormValidation.ok();
			}

			if (!StringUtils.isNumeric(value)) {
				return FormValidation.error("Controller Polling Interval must be a number");
			}

			return FormValidation.ok();
		}

		/**
		 * Do check per scenario time out form validation.
		 *
		 * @param value the value
		 * @return the form validation
		 */
		public FormValidation doCheckPerScenarioTimeOut(@QueryParameter String value) {
			if (StringUtils.isEmpty(value)) {
				return FormValidation.ok();
			}

			if (!StringUtils.isNumeric(value)) {
				return FormValidation.error("Per Scenario Timeout must be a number");
			}

			return FormValidation.ok();
		}

	}
}
