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
package com.microfocus.application.automation.tools.run;

import com.hp.octane.integrations.executor.TestsToRunConverter;
import com.microfocus.application.automation.tools.JenkinsUtils;
import com.microfocus.application.automation.tools.AlmToolsUtils;
import com.microfocus.application.automation.tools.EncryptionUtils;
import com.microfocus.application.automation.tools.Messages;
import com.microfocus.application.automation.tools.lr.model.ScriptRTSSetModel;
import com.microfocus.application.automation.tools.lr.model.SummaryDataLogModel;
import com.microfocus.application.automation.tools.mc.Constants;
import com.microfocus.application.automation.tools.mc.JobConfigurationProxy;
import com.microfocus.application.automation.tools.model.*;
import com.microfocus.application.automation.tools.settings.MCServerSettingsGlobalConfiguration;
import com.microfocus.application.automation.tools.uft.model.SpecifyParametersModel;
import com.microfocus.application.automation.tools.uft.model.UftRunAsUser;
import com.microfocus.application.automation.tools.uft.model.UftSettingsModel;
import com.microfocus.application.automation.tools.uft.utils.UftToolUtils;
import hudson.*;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.Secret;
import hudson.util.VariableResolver;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.minidev.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Describes a regular jenkins build step from UFT or LR
 */
public class RunFromFileBuilder extends Builder implements SimpleBuildStep {

    private static final String LRANALYSIS_LAUNCHER_EXE = "LRAnalysisLauncher.exe";

    public static final String HP_TOOLS_LAUNCHER_EXE = "HpToolsLauncher.exe";
    public static final String HP_TOOLS_LAUNCHER_EXE_CFG = "HpToolsLauncher.exe.config";

    private RunFromFileSystemModel runFromFileModel;

    private FileSystemTestSetModel fileSystemTestSetModel;
    private SpecifyParametersModel specifyParametersModel;
    private boolean isParallelRunnerEnabled;
    private boolean areParametersEnabled;
    private SummaryDataLogModel summaryDataLogModel;

    private ScriptRTSSetModel scriptRTSSetModel;

    private UftSettingsModel uftSettingsModel;

    private Map<Long, String> resultFileNames;

    /**
     * Instantiates a new Run from file builder.
     *
     * @param fsTests the fs tests
     */
    @DataBoundConstructor
    public RunFromFileBuilder(String fsTests,
                              boolean isParallelRunnerEnabled,
                              boolean areParametersEnabled,
                              SpecifyParametersModel specifyParametersModel,
                              FileSystemTestSetModel fileSystemTestSetModel,
                              SummaryDataLogModel summaryDataLogModel,
                              ScriptRTSSetModel scriptRTSSetModel,
                              UftSettingsModel uftSettingsModel) {
        this.runFromFileModel = new RunFromFileSystemModel(fsTests);
        this.specifyParametersModel = specifyParametersModel;
        this.fileSystemTestSetModel = fileSystemTestSetModel;
        this.isParallelRunnerEnabled = isParallelRunnerEnabled;
        this.areParametersEnabled = areParametersEnabled;
        this.summaryDataLogModel = summaryDataLogModel;
        this.scriptRTSSetModel = scriptRTSSetModel;
        this.uftSettingsModel = uftSettingsModel;
        if (uftSettingsModel != null) {
            uftSettingsModel.setFsTestPath(getFsTests());
        }
        resultFileNames = new HashMap<Long, String>();
    }

    /**
     * Instantiates a new Run from file builder.
     *
     * @param fsTests the fs tests
     */
    public RunFromFileBuilder(String fsTests) {
        runFromFileModel = new RunFromFileSystemModel(fsTests);
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
     * @param fsTests                   the fs tests
     * @param fsTimeout                 the fs timeout
     * @param controllerPollingInterval the controller polling interval
     * @param perScenarioTimeOut        the per scenario time out
     * @param ignoreErrorStrings        the ignore error strings
     * @param analysisTemplate          the analysis template
     * @param displayController         the display controller
     * @param mcServerName              the mc server name
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
     * @deprecated Instantiates a new Run from file builder.
     */
    @SuppressWarnings("squid:S00107")
    @Deprecated
    public RunFromFileBuilder(String fsTests, String fsTimeout, String fsUftRunMode, String controllerPollingInterval,
                              String perScenarioTimeOut, String ignoreErrorStrings, String displayController,
                              String analysisTemplate, String mcServerName, AuthModel authModel, String fsDeviceId, String fsTargetLab, String fsManufacturerAndModel,
                              String fsOs, String fsAutActions, String fsLaunchAppName, String fsDevicesMetrics,
                              String fsInstrumented, String fsExtraApps, String fsJobId, ProxySettings proxySettings,
                              boolean useSSL, boolean isParallelRunnerEnabled, String fsReportPath, CloudBrowserModel cloudBrowserModel) {
        this.isParallelRunnerEnabled = isParallelRunnerEnabled;
        runFromFileModel = new RunFromFileSystemModel(fsTests, fsTimeout, fsUftRunMode, controllerPollingInterval,
                perScenarioTimeOut, ignoreErrorStrings, displayController, analysisTemplate, mcServerName,
                authModel, fsDeviceId, fsTargetLab, fsManufacturerAndModel, fsOs,
                fsAutActions, fsLaunchAppName, fsDevicesMetrics, fsInstrumented, fsExtraApps, fsJobId,
                proxySettings, useSSL, fsReportPath, cloudBrowserModel);
    }

    /**
     * Replace the fsTests given as mtbx with the actual mtbx file.
     *
     * @param workspace the current workspace
     * @param props     the properties
     * @param content   the mtbx content
     * @param key       the test key
     * @param time      current time string
     * @param index     the index for the prefix
     * @throws Exception
     */
    private static void replaceTestWithMtbxFile(FilePath workspace, Properties props, String content, String key,
                                                String time, int index) throws Exception {
        if (UftToolUtils.isMtbxContent(content)) {
            try {
                String prefx = index > 0 ? index + "_" : "";
                String mtbxFilePath = prefx + createMtbxFileInWs(workspace, content, time);
                props.setProperty(key, mtbxFilePath);
            } catch (IOException | InterruptedException e) {
                throw new Exception(e);
            }
        }
    }

    /**
     * Replace the fsTests given as mtbx with the actual mtbx file.
     *
     * @param workspace the current workspace
     * @param props     the properties
     * @param content   the mtbx content
     * @param key       the test key
     * @param time      current time string
     * @throws Exception
     */
    private static void replaceTestWithMtbxFile(FilePath workspace, Properties props, String content, String key,
                                                String time) throws Exception {
        replaceTestWithMtbxFile(workspace, props, content, key, time, 0);
    }

    /**
     * Creates an .mtbx file with the provided mtbx content.
     *
     * @param workspace   jenkins workspace
     * @param mtbxContent the motbx content
     * @param timeString  current time represented as a String
     * @return the remote file path
     * @throws IOException
     * @throws InterruptedException
     */
    private static String createMtbxFileInWs(FilePath workspace, String mtbxContent, String timeString)
            throws IOException, InterruptedException {
        String fileName = "test_suite_" + timeString + ".mtbx";

        FilePath remoteFile = workspace.child(fileName);

        String mtbxContentUpdated = mtbxContent.replace("${WORKSPACE}", workspace.getRemote());
        if (mtbxContent.contains("${workspace}")) {
            mtbxContentUpdated = mtbxContent.replace("${workspace}", workspace.getRemote());
        }
        try (InputStream in = IOUtils.toInputStream(mtbxContentUpdated, StandardCharsets.UTF_8)) {
            remoteFile.copyFrom(in);
        }
        return remoteFile.getRemote();
    }

    public FileSystemTestSetModel getFileSystemTestSetModel() {
        return fileSystemTestSetModel;
    }

    /**
     * Gets the parallel runner flag.
     *
     * @return the current parallel runner flag state(enabled/disabled)
     */
    public boolean getIsParallelRunnerEnabled() {
        return isParallelRunnerEnabled;
    }

    /**
     * Sets the parallel runner flag
     *
     * @param isParallelRunnerEnabled the parallel runner flag
     */
    @DataBoundSetter
    private void setIsParallelRunnerEnabled(boolean isParallelRunnerEnabled) {
        this.isParallelRunnerEnabled = isParallelRunnerEnabled;
    }

    public String getAnalysisTemplate() {
        return runFromFileModel.getAnalysisTemplate();
    }

    /**
     * Sets analysis template.
     *
     * @param analysisTemplate the analysis template
     */
    @DataBoundSetter
    public void setAnalysisTemplate(String analysisTemplate) {
        runFromFileModel.setAnalysisTemplate(analysisTemplate);
    }

    public SummaryDataLogModel getSummaryDataLogModel() {
        return summaryDataLogModel;
    }

    public void setSummaryDataLogModel(SummaryDataLogModel summaryDataLogModel) {
        this.summaryDataLogModel = summaryDataLogModel;
    }

    public ScriptRTSSetModel getScriptRTSSetModel() {
        return scriptRTSSetModel;
    }

    public void setScriptRTSSetModel(ScriptRTSSetModel scriptRTSSetModel) {
        this.scriptRTSSetModel = scriptRTSSetModel;
    }

    public UftSettingsModel getUftSettingsModel() {
        return uftSettingsModel;
    }

    @DataBoundSetter
    public void setUftSettingsModel(UftSettingsModel uftSettingsModel) {
        this.uftSettingsModel = uftSettingsModel;
    }

    public String getFsTimeout() {
        return runFromFileModel.getFsTimeout();
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

    public String getFsTests() {
        return runFromFileModel.getFsTests();
    }

    public void setFsTests(String fsTests) {
        runFromFileModel.setFsTests(fsTests);
    }

    public String getControllerPollingInterval() {
        return runFromFileModel.getControllerPollingInterval();
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

    public String getPerScenarioTimeOut() {
        return runFromFileModel.getPerScenarioTimeOut();
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

    public String getDisplayController() {
        return runFromFileModel.getDisplayController();
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

    public String getFsAutActions() {
        return runFromFileModel.getFsAutActions();
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

    public String getFsDeviceId() {
        return runFromFileModel.getFsDeviceId();
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

    public String getFsDevicesMetrics() {
        return runFromFileModel.getFsDevicesMetrics();
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

    public String getFsExtraApps() {
        return runFromFileModel.getFsExtraApps();
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

    public String getFsOs() {
        return runFromFileModel.getFsOs();
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

    public String getFsInstrumented() {
        return runFromFileModel.getFsInstrumented();
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

    public String getFsJobId() {
        return runFromFileModel.getFsJobId();
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

    public String getFsUftRunMode() {
        return runFromFileModel.getFsUftRunMode();
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

    public String getIgnoreErrorStrings() {
        return runFromFileModel.getIgnoreErrorStrings();
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

    public String getMcServerName() {
        return runFromFileModel.getMcServerName();
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

    public String getFsManufacturerAndModel() {
        return runFromFileModel.getFsManufacturerAndModel();
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

    public String getFsLaunchAppName() {
        return runFromFileModel.getFsLaunchAppName();
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

    public ProxySettings getProxySettings() {
        return runFromFileModel.getProxySettings();
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

    public String getFsTargetLab() {
        return runFromFileModel.getFsTargetLab();
    }

    public AuthModel getAuthModel() {
        return runFromFileModel.getAuthModel();
    }

    @DataBoundSetter
    public void setAuthModel(AuthModel authModel) {
        runFromFileModel.setAuthModel(authModel);
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

    public boolean getUseSSL() {
        return runFromFileModel.isUseSSL();
    }

    /**
     * Get the fs report path.
     *
     * @return the filesystem report path
     */
    public String getFsReportPath() {
        return runFromFileModel.getFsReportPath();
    }

    /**
     * Sets the report path
     *
     * @param fsReportPath the report path
     */
    @DataBoundSetter
    public void setFsReportPath(String fsReportPath) {
        runFromFileModel.setFsReportPath(fsReportPath);
    }

    public CloudBrowserModel getCloudBrowserModel() {
        return runFromFileModel.getCloudBrowserModel();
    }
    @DataBoundSetter
    public void setCloudBrowserModel(CloudBrowserModel cloudBrowserModel) {
        runFromFileModel.setCloudBrowserModel(cloudBrowserModel);
    }
    public String getOutEncoding() { return runFromFileModel.getOutEncoding(); }

    @DataBoundSetter
    public void setOutEncoding(String encoding) { runFromFileModel.setOutEncoding(encoding); }

    /**
     * Sets mc server name.
     *
     * @param useSSL the mc server name
     */
    @DataBoundSetter
    public void setUseSSL(boolean useSSL) {
        runFromFileModel.setUseSSL(useSSL);
    }

    public Map<Long, String> getResultFileNames() {
        return resultFileNames;
    }

    @DataBoundSetter
    public void setResultFileNames(Map<Long, String> results) {
        resultFileNames = results;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace, @Nonnull Launcher launcher,
                        @Nonnull TaskListener listener)
            throws IOException {
        PrintStream out = listener.getLogger();

        UftOctaneUtils.setUFTRunnerTypeAsParameter(build, listener);
        // get the mc server settings
        MCServerSettingsModel mcServerSettingsModel = getMCServerSettingsModel();

        EnvVars env = null;
        try {
            env = build.getEnvironment(listener);
        } catch (IOException | InterruptedException e) {
            listener.error("Failed loading build environment: " + e.getMessage());
        }

        Node currNode = JenkinsUtils.getCurrentNode(workspace);
        if (currNode == null) {
            listener.error("Failed to get current executor node.");
            return;
        }

        // in case of mbt, since mbt can support uft and codeless at the same time, run only if there are uft tests
        ParametersAction parameterAction = build.getAction(ParametersAction.class);
        ParameterValue octaneFrameworkParam = parameterAction != null ? parameterAction.getParameter("octaneTestRunnerFramework") : null;
        if (octaneFrameworkParam != null && octaneFrameworkParam.getValue().equals("MBT")) {
            String testsToRunConverted = env == null ? null : env.get(TestsToRunConverter.DEFAULT_TESTS_TO_RUN_CONVERTED_PARAMETER);
            if (StringUtils.isEmpty(testsToRunConverted)) {
                out.println(RunFromFileBuilder.class.getSimpleName() + " : No UFT tests were found");
                return;
            }
        }

        // this is an unproper replacement to the build.getVariableResolver since workflow run won't support the
        // getBuildEnvironment() as written here:
        // https://github.com/jenkinsci/pipeline-plugin/blob/893e3484a25289c59567c6724f7ce19e3d23c6ee/DEVGUIDE
        // .md#variable-substitutions

        JSONObject jobDetails;
        String mcServerUrl;
        // now merge them into one list
        Properties mergedProps = new Properties();
        if (mcServerSettingsModel != null) {
            mcServerUrl = mcServerSettingsModel.getProperties().getProperty("MobileHostAddress");
            jobDetails = runFromFileModel.getJobDetails(mcServerUrl);

            mergedProps.setProperty("mobileinfo", jobDetails != null ? jobDetails.toJSONString() : "");
            mergedProps.setProperty("MobileHostAddress", mcServerUrl);

            CloudBrowserModel cbm = getCloudBrowserModel();
            if (cbm != null) {
                String cb = String.format("\"url=%s;os=%s;type=%s;version=%s;region=%s\"", cbm.getUrl(), cbm.getOs(), cbm.getType(), cbm.getVersion(), cbm.getRegion());
                mergedProps.setProperty("cloudBrowser", cb);
            }
        }

        // check whether Mobile authentication info is given or not
        String plainTextPwd = runFromFileModel.getMcPassword() == null ? null : Secret.fromString(runFromFileModel.getMcPassword()).getPlainText();
        String plainTextToken = runFromFileModel.getMcExecToken() == null ? null : Secret.fromString(runFromFileModel.getMcExecToken()).getPlainText();
        if (StringUtils.isNotBlank(plainTextPwd)) {
            try {
                String encPassword = EncryptionUtils.encrypt(plainTextPwd, currNode);
                mergedProps.put("MobilePassword", encPassword);
            } catch (Exception e) {
                build.setResult(Result.FAILURE);
                listener.fatalError("Problem in Digital Lab password encryption: " + e.getMessage() + ".");
                return;
            }
        } else if (StringUtils.isNotBlank(plainTextToken)) {
            try {
                String encToken = EncryptionUtils.encrypt(plainTextToken, currNode);
                mergedProps.put("MobileExecToken", encToken);
            } catch (Exception e) {
                build.setResult(Result.FAILURE);
                listener.fatalError("Problem in Digital Lab execution token encryption: " + e.getMessage() + ".");
                return;
            }
        }

        if (env == null) {
            listener.fatalError("Environment not set");
            throw new IOException("Env Null - something went wrong with fetching jenkins build environment");
        }

        if (build instanceof AbstractBuild) {
            VariableResolver<String> varResolver = ((AbstractBuild) build).getBuildVariableResolver();
        }

        mergedProps.putAll(Objects.requireNonNull(runFromFileModel).getProperties(env, currNode));

        if (areParametersEnabled) {
            try {
                specifyParametersModel.addProperties(mergedProps, "Test", currNode);
            } catch (Exception e) {
                listener.error("Error occurred while parsing parameter input, reverting back to empty array.");
            }
        }
        boolean isPrintTestParams = UftToolUtils.isPrintTestParams(build, listener);
        mergedProps.put("printTestParams", isPrintTestParams ? "1" : "0");

        UftRunAsUser uftRunAsUser;
        try {
            uftRunAsUser = UftToolUtils.getRunAsUser(build, listener);
            if (uftRunAsUser != null) {
                mergedProps.put("uftRunAsUserName", uftRunAsUser.getUsername());
                if (StringUtils.isNotBlank(uftRunAsUser.getEncodedPassword())) {
                    mergedProps.put("uftRunAsUserEncodedPassword", uftRunAsUser.getEncodedPasswordAsEncrypted(currNode));
                } else if (uftRunAsUser.getPassword() != null) {
                    mergedProps.put("uftRunAsUserPassword", uftRunAsUser.getPasswordAsEncrypted(currNode));
                }
            }
        } catch(IllegalArgumentException | EncryptionUtils.EncryptionException e) {
            build.setResult(Result.FAILURE);
            listener.fatalError(String.format("Build parameters check failed: %s.", e.getMessage()));
            return;
        }
        int idx = 0;
        for (Iterator<String> iterator = env.keySet().iterator(); iterator.hasNext(); ) {
            String key = iterator.next();
            idx++;
            mergedProps.put("JenkinsEnv" + idx, key + ";" + env.get(key));
        }

        Date now = new Date();
        Format formatter = new SimpleDateFormat("ddMMyyyyHHmmssSSS");
        String time = formatter.format(now);

        // get a unique filename for the params file
        String propsFileName = String.format("props%s.txt", time);
        String resFileName = String.format("Results%s_%d.xml", time, build.getNumber());

        long threadId = Thread.currentThread().getId();
        if (resultFileNames == null) {
            resultFileNames = new HashMap<Long, String>();
        }
        resultFileNames.put(threadId, resFileName);

        mergedProps.put("runType", AlmRunTypes.RunType.FileSystem.toString());

        if (summaryDataLogModel != null) {
            summaryDataLogModel.addToProps(mergedProps);
        }

        if (scriptRTSSetModel != null) {
            scriptRTSSetModel.addScriptsToProps(mergedProps, env);
        }

        mergedProps.put("resultsFilename", resFileName);

        // parallel runner is enabled
        if (isParallelRunnerEnabled) {
            // add the parallel runner properties
            fileSystemTestSetModel.addTestSetProperties(mergedProps, env);

            // we need to replace each mtbx test with mtbx file path
            for (int index = 1; index < this.fileSystemTestSetModel.getFileSystemTestSet().size(); index++) {
                String key = "Test" + index;
                String content = mergedProps.getProperty(key + index, "");
                try {
                    replaceTestWithMtbxFile(workspace, mergedProps, content, key, time, index);
                } catch (Exception e) {
                    build.setResult(Result.FAILURE);
                    listener.error("Failed to save MTBX file : " + e.getMessage());
                }
            }
        } else {
            // handling mtbx file content :
            // If we have mtbx content - it is located in Test1 property and there is no other test properties (like
            // Test2 etc)
            // We save mtbx content in workspace and replace content of Test1 by reference to saved file
            // this only applies to the normal file system flow
            String firstTestKey = "Test1";
            String firstTestContent = mergedProps.getProperty(firstTestKey, "");
            try {
                replaceTestWithMtbxFile(workspace, mergedProps, firstTestContent, firstTestKey, time);
            } catch (Exception e) {
                build.setResult(Result.FAILURE);
                listener.error("Failed to save MTBX file : " + e.getMessage());
            }
        }

        if (uftSettingsModel != null) {
            uftSettingsModel.addToProperties(mergedProps);
        }

        // cleanup report folders before running the build
        String selectedNode = env.get("NODE_NAME");
        if (selectedNode == null) {//if slave is given in the pipeline and not as part of build step
            try {
                selectedNode = launcher.getComputer().getName();
            } catch (Exception e) {
                listener.error("Failed to get selected node for UFT execution : " + e.getMessage());
            }
        }

        // clean cleanuptests' report folders
        int index = 1;
        while (mergedProps.getProperty("CleanupTest" + index) != null) {
            String testPath = mergedProps.getProperty("CleanupTest" + index);
            List<String> cleanupTests = UftToolUtils.getBuildTests(selectedNode, testPath);
            for (String test : cleanupTests) {
                UftToolUtils.deleteReportFoldersFromNode(selectedNode, test, listener);
            }

            index++;
        }

        // clean actual tests' report folders
        index = 1;
        while (mergedProps.getProperty("Test" + index) != null) {
            String testPath = mergedProps.getProperty(("Test" + index));
            List<String> buildTests = UftToolUtils.getBuildTests(selectedNode, testPath);
            for (String test : buildTests) {
                UftToolUtils.deleteReportFoldersFromNode(selectedNode, test, listener);
            }
            index++;
        }

        mergedProps.setProperty("numOfTests", String.valueOf(index - 1));

        // get properties serialized into a stream
        String strProps;
        try {
            strProps = AlmToolsUtils.getPropsAsString(mergedProps);
        } catch (IOException e) {
            build.setResult(Result.FAILURE);
            listener.error("Failed to store properties on agent machine: " + e);
            return;
        }
        
        // Get the URL to the Script used to run the test, which is bundled
        // in the plugin
        @SuppressWarnings("squid:S2259")
        URL cmdExeUrl = Jenkins.get().pluginManager.uberClassLoader.getResource(HP_TOOLS_LAUNCHER_EXE);
        if (cmdExeUrl == null) {
            listener.fatalError(HP_TOOLS_LAUNCHER_EXE + " not found in resources");
            return;
        }

        @SuppressWarnings("squid:S2259")
        URL cmdExeCfgUrl = Jenkins.get().pluginManager.uberClassLoader.getResource(HP_TOOLS_LAUNCHER_EXE_CFG);
        if (cmdExeCfgUrl == null) {
            listener.fatalError(HP_TOOLS_LAUNCHER_EXE_CFG + " not found in resources");
            return;
        }

        @SuppressWarnings("squid:S2259")
        URL cmdExe2Url = Jenkins.get().pluginManager.uberClassLoader.getResource(LRANALYSIS_LAUNCHER_EXE);
        if (cmdExe2Url == null) {
            listener.fatalError(LRANALYSIS_LAUNCHER_EXE + "not found in resources");
            return;
        }

        FilePath fileProps = workspace.child(propsFileName);
        FilePath cmdLineExe = workspace.child(HP_TOOLS_LAUNCHER_EXE);
        FilePath cmdLineExeCfg = workspace.child(HP_TOOLS_LAUNCHER_EXE_CFG);
        FilePath cmdLineExe2 = workspace.child(LRANALYSIS_LAUNCHER_EXE);

        try {
            // create a file for the properties file, and save the properties
            if (!AlmToolsUtils.tryCreatePropsFile(listener, strProps, fileProps)) {
                build.setResult(Result.FAILURE);
                return;
            }
            // Copy the script to the project workspace
            cmdLineExe.copyFrom(cmdExeUrl);
            cmdLineExeCfg.copyFrom(cmdExeCfgUrl);
            cmdLineExe2.copyFrom(cmdExe2Url);
        } catch (IOException | InterruptedException e) {
            build.setResult(Result.FAILURE);
            listener.error("Failed to copy props file or UFT tools to agent machine. " + e);
        }

        try {
            // Run the HpToolsLauncher.exe
            AlmToolsUtils.runOnBuildEnv(build, launcher, listener, cmdLineExe, propsFileName, currNode, runFromFileModel.getOutEncoding());
            // Has the report been successfully generated?
        } catch (IOException ioe) {
            Util.displayIOException(ioe, listener);
            build.setResult(Result.FAILURE);
            listener.error("Failed running HpToolsLauncher " + ioe.getMessage());
        } catch (InterruptedException e) {
            build.setResult(Result.ABORTED);
            listener.error("Failed running HpToolsLauncher - build aborted " + StringUtils.defaultString(e.getMessage()));
            try {
                AlmToolsUtils.runHpToolsAborterOnBuildEnv(build, launcher, listener, propsFileName, workspace);
            } catch (IOException e1) {
                Util.displayIOException(e1, listener);
                build.setResult(Result.FAILURE);
            } catch (InterruptedException e1) {
                listener.error("Failed running HpToolsAborter " + e1.getMessage());
            }
        }
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
        synchronized (this) {
            long threadId = Thread.currentThread().getId();
            String fileName = resultFileNames.get(threadId);
            return fileName;
        }
    }

    public boolean isAreParametersEnabled() {
        return areParametersEnabled;
    }

    public void setAreParametersEnabled(boolean areParametersEnabled) {
        this.areParametersEnabled = areParametersEnabled;
    }

    public SpecifyParametersModel getSpecifyParametersModel() {
        return specifyParametersModel;
    }

    /**
     * The type Descriptor.
     */
    @Symbol("runFromFSBuilder")
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
        public Map<String, String> getJobId(String mcUrl, String mcUserName, String mcPassword, String mcTenantId, String accessKey, String authType,
                               boolean useProxyAuth, String proxyAddress, String proxyUserName, String proxyPassword, String previousJobId) {
            AuthModel authModel = new AuthModel(mcUserName, mcPassword, mcTenantId, accessKey, authType);
            ProxySettings proxy = new ProxySettings(useProxyAuth, proxyAddress, proxyUserName, proxyPassword);
            Map<String, String> map = new HashMap<>();
            String jobIdKey = "jobId";

            try {
                JSONObject loginJson = instance.loginToMC(mcUrl, authModel, proxy);
                Map<String, String> initHeaders = instance.initHeaders(authModel, loginJson);
                if (initHeaders != null) {
                    map = initHeaders;
                    boolean serverOnSaaS = instance.isServerOnSaaS(initHeaders, mcUrl, proxy);
                    map.put("isSaaS", String.valueOf(serverOnSaaS));
                    if (!StringUtils.isEmpty(accessKey)) {
                            AtomicReference<String> tenantIdValue = new AtomicReference<>("");
                            Arrays.stream(accessKey.split(";")).forEach(str -> {
                                if (str.toLowerCase().contains("tenant")) {
                                    tenantIdValue.set(str.substring(8));
                                }
                            });

                            if (tenantIdValue.get() != null) {
                                map.put(Constants.TENANT_ID_COOKIE, tenantIdValue.get());
                            }
                        }
                    }

                String jobId;
                if (null != previousJobId && !previousJobId.isEmpty()) {
                    JSONObject jobJSON = instance.getJobByIdWithHeaders(mcUrl, proxy, previousJobId, initHeaders);
                    if (jobJSON != null && previousJobId.equals(jobJSON.getAsString("id"))) {
                        jobId = previousJobId;
                    } else {
                        jobId = instance.createTempJobWithHeaders(mcUrl, proxy, initHeaders);
                    }
                } else {
                    jobId = instance.createTempJobWithHeaders(mcUrl, proxy, initHeaders);
                }

                map.put(jobIdKey, jobId);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return map;
        }

        /**
         * Populate app and device json object.
         *
         * @param mcUrl the mc url
         * @param jobId the job id
         * @return the json object
         */
        @JavaScriptMethod
        public JSONObject populateAppAndDevice(String mcUrl, String mcUserName, String mcPassword, String mcTenantId, String accessKey, String authType,
                                               boolean useProxyAuth, String proxyAddress, String proxyUserName, String proxyPassword,
                                               String jobId) {
            AuthModel authModel = new AuthModel(mcUserName, mcPassword, mcTenantId, accessKey, authType);
            ProxySettings proxy = new ProxySettings(useProxyAuth, proxyAddress, proxyUserName, proxyPassword);
            return instance.getJobJSONData(mcUrl, authModel, proxy, jobId);
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
            MCServerSettingsModel[] servers = MCServerSettingsGlobalConfiguration.getInstance().getInstallations();
            for (MCServerSettingsModel mcServer : servers) {
                if (mcServer.getMcServerName().equals(serverName)) {
                    serverUrl = mcServer.getMcServerUrl().trim();
                    break;
                }
            }
            return serverUrl;
        }

        @JavaScriptMethod
        public JSONObject getBrowserLab(String serverName, String accessKey, boolean useProxyAuth, String proxyAddr, String proxyUserName, String proxyPassword) {
            String serverUrl = getMcServerUrl(serverName);
            if (StringUtils.isNotBlank(serverUrl)) {
                serverUrl = StringUtils.stripEnd(serverUrl, "/");
                ProxySettings proxy = new ProxySettings(useProxyAuth, proxyAddr, proxyUserName, proxyPassword);
                return instance.getBrowserLab(serverUrl, accessKey, proxy);
            }
            return null;
        }

        @Override
        public String getDisplayName() {
            return Messages.RunFromFileBuilderStepName(Messages.CompanyName());
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

            String sanitizedValue = value.trim();
            if (sanitizedValue.length() > 0 && sanitizedValue.charAt(0) == '-') {
                sanitizedValue = sanitizedValue.substring(1);
            }

            if (!isParameterizedValue(sanitizedValue) && !StringUtils.isNumeric(sanitizedValue)) {
                return FormValidation.error("Timeout must be a parameter or a number, e.g.: 23, $Timeout or " +
                        "${Timeout}.");
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
            return MCServerSettingsGlobalConfiguration.getInstance().hasMCServers();
        }

        /**
         * Get mc servers mc server settings model [ ].
         *
         * @return the mc server settings model [ ]
         */
        @SuppressWarnings("squid:S2259")

        public MCServerSettingsModel[] getMcServers() {
            MCServerSettingsModel emptySrv = new MCServerSettingsModel("", "");
            MCServerSettingsModel[] servers = MCServerSettingsGlobalConfiguration.getInstance().getInstallations();
            if (servers == null) {
                servers = new MCServerSettingsModel[0];
            }
            int nbOfServers = servers.length;
            MCServerSettingsModel[] all = new MCServerSettingsModel[nbOfServers + 1];
            all[0] = emptySrv;
            for (int i = 0; i < servers.length; i++) {
                all[i + 1] = servers[i];
            }
            return all;
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

            if (!isParameterizedValue(value) && !StringUtils.isNumeric(value)) {
                return FormValidation.error("Per Scenario Timeout must be a parameter or a number, e.g.: 23, " +
                        "$ScenarioDuration or ${ScenarioDuration}.");
            }

            return FormValidation.ok();
        }

        /**
         * Check if the value is parameterized.
         *
         * @param value the value
         * @return boolean
         */
        public boolean isParameterizedValue(String value) {
            //Parameter (with or without brackets)
            return value.matches("^\\$\\{[\\w-. ]*}$|^\\$[\\w-.]*$");
        }

        public List<EnumDescription> getFsUftRunModes() {
            return RunFromFileSystemModel.fsUftRunModes;
        }

        public List<EnumDescription> getFsTestTypes() {
            return UftSettingsModel.fsTestTypes;
        }

        public List<String> getNodes() {
            return UftToolUtils.getNodesList();
        }

        public List<String> getEncodings() { return RunFromFileSystemModel.encodings; }

        public boolean getHasConfigurePermission() { return JenkinsUtils.hasItemConfigurePermission(); }
    }

}