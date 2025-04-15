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
/*
 * Create the PCModel and the PCClient and allows the connection between the job and PC
 * */
package com.microfocus.application.automation.tools.run;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;
import com.microfocus.adm.performancecenter.plugins.common.pcentities.*;
import com.microfocus.application.automation.tools.pc.PcClient;
import com.microfocus.application.automation.tools.pc.PcModel;
import com.microfocus.application.automation.tools.pc.helper.DateFormatter;
import com.microfocus.application.automation.tools.sse.result.model.junit.Error;
import com.microfocus.application.automation.tools.sse.result.model.junit.Failure;
import com.microfocus.application.automation.tools.sse.result.model.junit.*;
import com.thoughtworks.xstream.XStream;
import hudson.*;
import hudson.console.HyperlinkNote;
import hudson.model.Queue;
import hudson.model.*;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.LogTaskListener;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.beans.IntrospectionException;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;

import static com.microfocus.adm.performancecenter.plugins.common.pcentities.RunState.FINISHED;
import static com.microfocus.adm.performancecenter.plugins.common.pcentities.RunState.RUN_FAILURE;

public class PcBuilder extends Builder implements SimpleBuildStep {

    public static final String artifactsResourceName = "artifact";
    public static final String runReportStructure = "%s/%s/performanceTestsReports/pcRun";
    public static final String trendReportStructure = "%s/%s/performanceTestsReports/TrendReports";
    public static final String pcReportArchiveName = "Reports.zip";
    public static final String pcReportFileName = "Report.html";
    public static final String TRENDED = "Trended";
    public static final String PENDING = "Pending";
    public static final String PUBLISHING = "Publishing";
    public static final String ERROR = "Error";
    private static final String artifactsDirectoryName = "archive";
    private static final String RUNID_BUILD_VARIABLE = "PC_RUN_ID";
    public static UsernamePasswordCredentials usernamePCPasswordCredentials;
    public static UsernamePasswordCredentials usernamePCPasswordCredentialsForProxy;
    private transient static Run<?, ?> _run;
    private static PrintStream logger;
    private final String timeslotDurationHours;
    private final String timeslotDurationMinutes;
    private final boolean statusBySLA;
    private PcModel pcModel;
    private String serverAndPort;
    private String pcServerName;
    private String credentialsId;
    private String almDomain;
    private String almProject;
    private String testId;
    private String testInstanceId;
    private String autoTestInstanceID;
    private PostRunAction postRunAction;
    private boolean vudsMode;
    private String description;
    private String addRunToTrendReport;
    private String trendReportId;
    private boolean HTTPSProtocol;
    private String proxyOutURL;
    private String credentialsProxyId;
    private String retry;
    private String retryDelay;
    private String retryOccurrences;
    private boolean authenticateWithToken;
    private int runId;
    private String testName;
    private FilePath pcReportFile;
    private String junitResultsFileName;
    private File WorkspacePath;
    private FilePath Workspace;

    @DataBoundConstructor
    public PcBuilder(
            String serverAndPort,
            String pcServerName,
            String credentialsId,
            String almDomain,
            String almProject,
            String testId,
            String testInstanceId,
            String autoTestInstanceID,
            String timeslotDurationHours,
            String timeslotDurationMinutes,
            PostRunAction postRunAction,
            boolean vudsMode,
            boolean statusBySLA,
            String description,
            String addRunToTrendReport,
            String trendReportId,
            boolean HTTPSProtocol,
            String proxyOutURL,
            String credentialsProxyId,
            String retry,
            String retryDelay,
            String retryOccurrences,
            boolean authenticateWithToken) {

        this.serverAndPort = serverAndPort;
        this.pcServerName = pcServerName;
        this.credentialsId = credentialsId;
        this.almDomain = almDomain;
        this.almProject = almProject;
        this.testId = testId;
        this.testInstanceId = testInstanceId;
        this.autoTestInstanceID = autoTestInstanceID;
        this.timeslotDurationHours = timeslotDurationHours;
        this.timeslotDurationMinutes = timeslotDurationMinutes;
        this.postRunAction = postRunAction;
        this.vudsMode = vudsMode;
        this.statusBySLA = statusBySLA;
        this.description = description;
        this.addRunToTrendReport = addRunToTrendReport;
        this.trendReportId = trendReportId;
        this.HTTPSProtocol = HTTPSProtocol;
        this.proxyOutURL = proxyOutURL;
        this.credentialsProxyId = credentialsProxyId;
        this.retry = (retry == null || retry.isEmpty()) ? "NO_RETRY" : retry;
        this.retryDelay = ("NO_RETRY".equals(this.retry)) ? "0" : (retryDelay == null || retryDelay.isEmpty()) ? "5" : retryDelay;
        this.retryOccurrences = ("NO_RETRY".equals(this.retry)) ? "0" : (retryOccurrences == null || retryOccurrences.isEmpty()) ? "3" : retryOccurrences;
        this.authenticateWithToken = authenticateWithToken;
    }

    public static UsernamePasswordCredentials getCredentialsId(String credentialsId) {
        if (credentialsId != null && _run != null)
            return getCredentialsById(credentialsId, _run, logger);
        return null;
    }

    public static UsernamePasswordCredentials getCredentialsProxyId(String credentialsProxyId) {
        if (credentialsProxyId != null && _run != null)
            return getCredentialsById(credentialsProxyId, _run, logger);
        return null;
    }

    private static UsernamePasswordCredentials getCredentialsById(String credentialsId, Run<?, ?> run, PrintStream logger) {
        if (StringUtils.isBlank(credentialsId))
            return null;

        UsernamePasswordCredentials usernamePCPasswordCredentials = CredentialsProvider.findCredentialById(credentialsId,
                StandardUsernamePasswordCredentials.class,
                run,
                URIRequirementBuilder.create().build());

        if (usernamePCPasswordCredentials == null) {
            logger.println(String.format("%s : %s",
                    Messages.CannotFindCredentials(),
                    credentialsId));
        }

        return usernamePCPasswordCredentials;
    }

    public static String getArtifactsDirectoryName() {

        return artifactsDirectoryName;
    }

    public static String getArtifactsResourceName() {

        return artifactsResourceName;
    }

    public static String getRunReportStructure() {

        return runReportStructure;
    }

    public static String getPcReportArchiveName() {

        return pcReportArchiveName;
    }

    public static String getPcreportFileName() {

        return pcReportFileName;
    }

    public static String getPluginVersion() {
        Plugin plugin = getJenkinsInstance().getPlugin(Messages.ArtifactId());
        return plugin.getWrapper().getVersion();
    }

    private static Jenkins getJenkinsInstance() {
        Jenkins result = Jenkins.getInstance();
        if (result == null) {
            throw new IllegalStateException(Messages.FailedToObtainInstance());
        }
        return result;
    }

    @Override
    public DescriptorImpl getDescriptor() {

        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        if (build.getWorkspace() != null)
            WorkspacePath = new File(build.getWorkspace().toURI());
        else
            WorkspacePath = null;
        if ((getPcModel() != null) && (build != null) && (build instanceof AbstractBuild))
            setPcModelBuildParameters(build, listener);
        if (build.getWorkspace() != null)
            perform(build, build.getWorkspace(), launcher, listener);
        else
            return false;
        return true;
    }

    private void setPcModelBuildParameters(AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException {
        Map<String, String> mapParamsAndEnvars = new HashMap<String, String>();
        Map<String, String> buildParameters = build.getBuildVariables();
        mapParamsAndEnvars.putAll(buildParameters);
        if (listener != null) {
            Map<String, String> buildEnvars = build.getEnvironment(listener);
            mapParamsAndEnvars.putAll(buildEnvars);
        } else {
            Map<String, String> buildEnvars = build.getEnvironment(new LogTaskListener(null, Level.INFO));
            mapParamsAndEnvars.putAll(buildEnvars);
        }
        String buildParametersAndEnvars = mapParamsAndEnvars.toString();
        if (!buildParameters.isEmpty())
            getPcModel().setBuildParameters(buildParametersAndEnvars);
    }

    public File getWorkspacePath() {
        return WorkspacePath;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(String newCredentialsId) {
        credentialsId = newCredentialsId;
        pcModel = null;
        getPcModel();
    }

    public String getCredentialsProxyId() {
        return credentialsProxyId;
    }

    public void setCredentialsProxyId(String newCredentialsProxyId) {
        credentialsProxyId = newCredentialsProxyId;
        pcModel = null;
        getPcModel();
    }

    //pcModel is intialized here.
    public PcModel getPcModel() {
        if (pcModel == null) {
            pcModel =
                    new PcModel(
                            serverAndPort.trim(),
                            pcServerName.trim(),
                            credentialsId,
                            almDomain.trim(),
                            almProject.trim(),
                            testId.trim(),
                            autoTestInstanceID,
                            testInstanceId.trim(),
                            timeslotDurationHours.trim(),
                            timeslotDurationMinutes.trim(),
                            postRunAction,
                            vudsMode,
                            description,
                            addRunToTrendReport,
                            trendReportId,
                            HTTPSProtocol,
                            proxyOutURL,
                            credentialsProxyId,
                            retry,
                            retryDelay,
                            retryOccurrences,
                            authenticateWithToken);
        }
        return pcModel;
    }

    public String getRunResultsFileName() {

        return junitResultsFileName;
    }

    private void setBuildParameters(AbstractBuild<?, ?> build) {
        try {
            if (build != null && build.getBuildVariables() != null)
                getPcModel().setBuildParameters(build.getBuildVariables().toString());
        } catch (Exception ex) {
            logger.println(String.format("%s - %s: %s",
                    DateFormatter.getDateTime(),
                    Messages.BuildParameterNotConsidered(),
                    ex.getMessage()));
        }
    }

    private String getVersion() {
        String completeVersion = getPluginVersion();
        if (completeVersion != null) {
            String[] partsOfCompleteVersion = completeVersion.split(" [(]");
            return partsOfCompleteVersion[0];
        }
        return "unknown";
    }

    private Testsuites execute(PcClient pcClient, Run<?, ?> build)
            throws InterruptedException, NullPointerException {
        _run = build;
        try {
            String version = getVersion();
            if (!(version == null || version.equals("unknown")))
                logger.println(String.format("%s - %s '%s'",
                        DateFormatter.getDateTime(),
                        Messages.PluginVersionIs(),
                        version));
            if ((getPcModel() != null) && (build != null) && (build instanceof AbstractBuild))
                setPcModelBuildParameters((AbstractBuild) build, null);
            if (!StringUtils.isBlank(getPcModel().getDescription()))
                logger.println(String.format("%s - %s: %s",
                        DateFormatter.getDateTime(),
                        Messages.TestDescription(),
                        getPcModel().getDescription()));
            if (!beforeRun(pcClient))
                return null;

            return run(pcClient, build);

        } catch (InterruptedException e) {
            build.setResult(Result.ABORTED);
            pcClient.stopRun(runId);
            throw e;
        } catch (NullPointerException e) {
            logger.println(String.format("%s - %s: %s",
                    DateFormatter.getDateTime(),
                    Messages.Error(),
                    e.getMessage()));
        } catch (Exception e) {
            logger.println(String.format("%s - %s",
                    DateFormatter.getDateTime(),
                    e.getMessage()));
        } finally {
            pcClient.logout();
        }
        return null;
    }

    private Testsuites run(PcClient pcClient, Run<?, ?> build)
            throws InterruptedException, ClientProtocolException,
            IOException, PcException {
        if ((getPcModel() != null) && (build != null) && (build instanceof AbstractBuild))
            setPcModelBuildParameters((AbstractBuild) build, null);
        PcRunResponse response = null;
        String errorMessage = "";
        String eventLogString = "";
        boolean trendReportReady = false;
        try {
            runId = pcClient.startRun();
            if (runId == 0)
                return null;
        } catch (NumberFormatException | ClientProtocolException | PcException ex) {
            logger.println(String.format("%s - %s. %s: %s",
                    DateFormatter.getDateTime(),
                    Messages.StartRunFailed(),
                    Messages.Error(),
                    ex.getMessage()));
            throw ex;
        } catch (IOException ex) {
            logger.println(String.format("%s - %s. %s: %s",
                    DateFormatter.getDateTime(),
                    Messages.StartRunFailed(),
                    Messages.Error(),
                    ex.getMessage()));
            throw ex;
        }

        //getTestName failure should not fail test execution.
        try {
            testName = pcClient.getTestName();
            if (testName == null) {
                testName = String.format("TestId_%s", getPcModel().getTestId());
                logger.println(String.format("%s - getTestName failed. Using '%s' as testname.",
                        DateFormatter.getDateTime(),
                        testName));
            } else
                logger.println(String.format("%s - %s %s",
                        DateFormatter.getDateTime(),
                        Messages.TestNameIs(),
                        testName));
        } catch (PcException | IOException ex) {
            testName = String.format("TestId_%s", getPcModel().getTestId());
            logger.println(String.format("%s - getTestName failed. Using '%s' as testname. Error: %s \n",
                    DateFormatter.getDateTime(),
                    testName,
                    ex.getMessage()));
        }

        try {
            List<ParameterValue> parameters = new ArrayList<>();
            parameters.add(new StringParameterValue(RUNID_BUILD_VARIABLE, "" + runId));
            // This allows a user to access the runId from within Jenkins using a build variable.
            build.addAction(new AdditionalParametersAction(parameters));
            logger.print(String.format("%s - %s: %s = %s \n",
                    DateFormatter.getDateTime(),
                    Messages.SetEnvironmentVariable(),
                    RUNID_BUILD_VARIABLE,
                    runId));
            response = pcClient.waitForRunCompletion(runId);

            if (response != null && RunState.get(response.getRunState()) == FINISHED && getPcModel().getPostRunAction() != PostRunAction.DO_NOTHING) {
                pcReportFile = pcClient.publishRunReport(runId, getReportDirectory(build));

                // Adding the trend report section if ID has been set or if the Associated Trend report is selected.
                if (((("USE_ID").equals(getPcModel().getAddRunToTrendReport()) && getPcModel().getTrendReportId(true) != null) || ("ASSOCIATED").equals(getPcModel().getAddRunToTrendReport())) && RunState.get(response.getRunState()) != RUN_FAILURE) {
                    Thread.sleep(5000);
                    pcClient.addRunToTrendReport(this.runId, getPcModel().getTrendReportId(true));
                    pcClient.waitForRunToPublishOnTrendReport(this.runId, getPcModel().getTrendReportId(true));
                    pcClient.downloadTrendReportAsPdf(getPcModel().getTrendReportId(true), getTrendReportsDirectory(build));
                    trendReportReady = true;
                }

            } else if (response != null && RunState.get(response.getRunState()).ordinal() > FINISHED.ordinal()) {
                PcRunEventLog eventLog = pcClient.getRunEventLog(runId);
                eventLogString = buildEventLogString(eventLog);
            }

        } catch (PcException e) {
            logger.println(String.format("%s - Error: %s",
                    DateFormatter.getDateTime(),
                    e.getMessage()));
        }

        Testsuites ret = new Testsuites();
        parsePcRunResponse(ret, response, build, errorMessage, eventLogString);
        try {
            parsePcTrendResponse(ret, build, pcClient, trendReportReady, getPcModel().getTrendReportId(true), runId);
        } catch (IntrospectionException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        return ret;
    }

    private String buildEventLogString(PcRunEventLog eventLog) {

        String logFormat = "%-5s | %-7s | %-19s | %s\n";
        StringBuilder eventLogStr = new StringBuilder("Event Log:\n\n" + String.format(logFormat, "ID", "TYPE", "TIME", "DESCRIPTION"));
        for (PcRunEventLogRecord record : eventLog.getRecordsList()) {
            eventLogStr.append(String.format(logFormat, record.getID(), record.getType(), record.getTime(), record.getDescription()));
        }
        return eventLogStr.toString();
    }

    private boolean beforeRun(PcClient pcClient) {
        return validatePcForm() && pcClient.login();
    }

    private String getReportDirectory(Run<?, ?> build) {
        return String.format(
                runReportStructure,
                build.getRootDir().getPath(),
                artifactsDirectoryName);
    }

    private String getTrendReportsDirectory(Run<?, ?> build) {
        return String.format(
                trendReportStructure,
                build.getRootDir().getPath(),
                artifactsDirectoryName);
    }


    @Override
    @Deprecated
    public boolean perform(Build<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return super.perform(build, launcher, listener);
    }

    private boolean validatePcForm() {

        logger.println(String.format("%s - %s",
                DateFormatter.getDateTime(),
                Messages.ValidatingParametersBeforeRun()));
        String prefix = "doCheck";
        boolean ret = true;
        Method[] methods = getDescriptor().getClass().getMethods();
        Method[] modelMethods = getPcModel().getClass().getMethods();
        for (Method method : methods) {
            String name = method.getName();
            if (name.startsWith(prefix)) {
                name = name.replace(prefix, "").toLowerCase();
                for (Method modelMethod : modelMethods) {
                    String modelMethodName = modelMethod.getName();
                    if (modelMethodName.toLowerCase().equals("get" + name) && modelMethod.getParameterTypes().length == 0) {
                        try {
                            Object obj = FormValidation.ok();
                            if (!("testinstanceid".equals(name) && "AUTO".equals(getPcModel().getAutoTestInstanceID()))
                                    && !(("retrydelay".equals(name) && "NO_RETRY".equals(getPcModel().getRetry())) || getPcModel().getRetry().isEmpty())
                                    && !(("retryoccurrences".equals(name) && "NO_RETRY".equals(getPcModel().getRetry())) || getPcModel().getRetry().isEmpty())
                            ) {
                                if ("doCheckCredentialsId".equals(method.getName()) && "credentialsid".equals(name) && "getCredentialsId".equals(modelMethodName)
                                        || "doCheckCredentialsProxyId".equals(method.getName()) && "credentialsproxyid".equals(name) && "getCredentialsProxyId".equals(modelMethodName)
                                )
                                    obj = method.invoke(getDescriptor(), null, null, modelMethod.invoke(getPcModel()));
                                else
                                    obj = method.invoke(getDescriptor(), modelMethod.invoke(getPcModel()));
                            }
                            if (!obj.equals(FormValidation.ok())) {
                                logger.println(obj);
                                ret = false;
                            }
                            break;
                        } catch (Exception e) {
                            logger.println(String.format("%s - Validation error: method.getName() = '%s', name = '%s', modelMethodName = '%s', exception = '%s'.",
                                    DateFormatter.getDateTime(),
                                    method.getName(),
                                    name,
                                    modelMethodName,
                                    e.getMessage()));
                        }
                    }
                }
            }
        }

        boolean isTrendReportIdValid = validateTrendReportIdIsNumeric(getPcModel().getTrendReportId(true), ("USE_ID").equals(getPcModel().getAddRunToTrendReport()));

        ret &= isTrendReportIdValid;
        return ret;

    }


    private boolean validateTrendReportIdIsNumeric(String trendReportId, boolean addRunToTrendReport) {

        FormValidation res = FormValidation.ok();
        if (addRunToTrendReport) {
            if (trendReportId.isEmpty()) {
                res = FormValidation.error(String.format("%s: %s.",
                        Messages.ParameterIsMissing(),
                        Messages.TrendReportIDIsMissing()));
            } else {

                try {

                    Integer.parseInt(trendReportId);
                } catch (NumberFormatException e) {

                    res = FormValidation.error(Messages.IllegalParameter());
                }

            }
        }

        logger.println(String.format("%s - %s",
                DateFormatter.getDateTime(),
                res.toString().replace(": <div/>", "")));

        return res.equals(FormValidation.ok());
    }

    private Testsuites parsePcRunResponse(Testsuites ret,
                                          PcRunResponse runResponse,
                                          Run<?, ?> build,
                                          String errorMessage, String eventLogString) throws IOException, InterruptedException {

        RunState runState = RunState.get(runResponse.getRunState());


        List<Testsuite> testSuites = ret.getTestsuite();
        Testsuite testSuite = new Testsuite();
        Testcase testCase = new Testcase();
        //testCase.setClassname("Performance Tests.Test ID: " + runResponse.getTestID());
        testCase.setClassname("Performance Test.Load Test");
        testCase.setName(testName + "(ID:" + runResponse.getTestID() + ")");
        testCase.setTime(String.valueOf(runResponse.getDuration() * 60));
        if (pcReportFile != null && pcReportFile.exists() && runState == FINISHED) {
            testCase.getSystemOut().add(getOutputForReportLinks(build));
        }
        updateTestStatus(testCase, runResponse, errorMessage, eventLogString);
        testSuite.getTestcase().add(testCase);
        testSuite.setName("Performance Test ID: " + runResponse.getTestID() + ", Run ID: " + runResponse.getID());
        testSuites.add(testSuite);
        return ret;
    }

    private Testsuites parsePcTrendResponse(Testsuites ret, Run<?, ?> build, PcClient pcClient, boolean trendReportReady, String TrendReportID, int runID) throws PcException, IntrospectionException, IOException, InterruptedException, NoSuchMethodException {


        if (trendReportReady) {
            String reportUrlTemp = trendReportStructure.replaceFirst("%s/", "") + "/trendReport%s.pdf";
            String reportUrl = String.format(reportUrlTemp, artifactsResourceName, getPcModel().getTrendReportId(true));
            pcClient.publishTrendReport(reportUrl, getPcModel().getTrendReportId(true));

            // Updating all CSV files for plot plugin
            // this helps to show the transaction of each result
            if (isPluginActive("Plot plugin")) {
                logger.println(String.format("%s %s.",
                        DateFormatter.getDateTime(),
                        Messages.UpdatingCsvFilesForTrendingCharts()));
                updateCSVFilesForPlot(pcClient, runID);
                String plotUrlPath = "/job/" + build.getParent().getName() + "/plot";
                logger.println(String.format("%s - %s",
                        DateFormatter.getDateTime(),
                        HyperlinkNote.encodeTo(plotUrlPath, Messages.TrendingCharts())));
            } else {
                logger.println(String.format("%s - %s %s (%s).",
                        DateFormatter.getDateTime(),
                        Messages.YouCanViewTrendCharts(),
                        HyperlinkNote.encodeTo("https://admhelp.microfocus.com/lre/en/2023-2023-r1/online_help/Content/PC/Continuous-Integration-Jenkins.htm#mt-item-4", Messages.Documentation()),
                        Messages.PerformanceCenter1255AndLater()));
            }
        }
        return ret;
    }

    private boolean isPluginActive(String pluginDisplayName) {
        List<PluginWrapper> allPlugin = Jenkins.get().pluginManager.getPlugins();
        for (PluginWrapper pw :
                allPlugin) {

            if (pw.getDisplayName().toLowerCase().equals(pluginDisplayName.toLowerCase())) {
                return pw.isActive();
            }
        }
        return false;
    }

    private void updateCSVFilesForPlot(PcClient pcClient, int runId) throws IOException, PcException, IntrospectionException, NoSuchMethodException {

        TriTrendReportTypes triTrendReportTypes[] = {
                // Transaction - TRT
                new TriTrendReportTypes(TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_MINIMUM),
                new TriTrendReportTypes(TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_MAXIMUM),
                new TriTrendReportTypes(TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_AVERAGE),
                new TriTrendReportTypes(TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_MEDIAN),
                new TriTrendReportTypes(TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_STDDEVIATION),
                new TriTrendReportTypes(TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_COUNT1),
                new TriTrendReportTypes(TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_PERCENTILE_90),
                new TriTrendReportTypes(TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_PERCENTILE_95),
                // Transaction - TPS
                new TriTrendReportTypes(TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TPS, TrendReportTypes.Measurement.PCT_MINIMUM),
                new TriTrendReportTypes(TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TPS, TrendReportTypes.Measurement.PCT_MAXIMUM),
                new TriTrendReportTypes(TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TPS, TrendReportTypes.Measurement.PCT_AVERAGE),
                new TriTrendReportTypes(TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TPS, TrendReportTypes.Measurement.PCT_MEDIAN),
                new TriTrendReportTypes(TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TPS, TrendReportTypes.Measurement.PCT_SUM1),
                // Transaction - TRS
                new TriTrendReportTypes(TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRS, TrendReportTypes.Measurement.PCT_COUNT1),
                // Monitors - UDP
                new TriTrendReportTypes(TrendReportTypes.DataType.Monitors, TrendReportTypes.PctType.UDP, TrendReportTypes.Measurement.PCT_MINIMUM),
                new TriTrendReportTypes(TrendReportTypes.DataType.Monitors, TrendReportTypes.PctType.UDP, TrendReportTypes.Measurement.PCT_MAXIMUM),
                new TriTrendReportTypes(TrendReportTypes.DataType.Monitors, TrendReportTypes.PctType.UDP, TrendReportTypes.Measurement.PCT_AVERAGE),
                new TriTrendReportTypes(TrendReportTypes.DataType.Monitors, TrendReportTypes.PctType.UDP, TrendReportTypes.Measurement.PCT_MEDIAN),
                new TriTrendReportTypes(TrendReportTypes.DataType.Monitors, TrendReportTypes.PctType.UDP, TrendReportTypes.Measurement.PCT_STDDEVIATION),
                new TriTrendReportTypes(TrendReportTypes.DataType.Monitors, TrendReportTypes.PctType.UDP, TrendReportTypes.Measurement.PCT_COUNT1),
                new TriTrendReportTypes(TrendReportTypes.DataType.Monitors, TrendReportTypes.PctType.UDP, TrendReportTypes.Measurement.PCT_SUM1),
                // Regular - VU
                new TriTrendReportTypes(TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.VU, TrendReportTypes.Measurement.PCT_MAXIMUM),
                new TriTrendReportTypes(TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.VU, TrendReportTypes.Measurement.PCT_AVERAGE),
                // Regular - WEB
                new TriTrendReportTypes(TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.WEB, TrendReportTypes.Measurement.PCT_MINIMUM),
                new TriTrendReportTypes(TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.WEB, TrendReportTypes.Measurement.PCT_MAXIMUM),
                new TriTrendReportTypes(TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.WEB, TrendReportTypes.Measurement.PCT_AVERAGE),
                new TriTrendReportTypes(TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.WEB, TrendReportTypes.Measurement.PCT_MEDIAN),
                new TriTrendReportTypes(TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.WEB, TrendReportTypes.Measurement.PCT_SUM1)
        };

        for (TriTrendReportTypes triTrendReportType : triTrendReportTypes
        ) {
            saveFileToWorkspacePath(pcClient, getPcModel().getTrendReportId(true), runId, triTrendReportType.getDataType(), triTrendReportType.getPctType(), triTrendReportType.getMeasurement());
        }

    }

    private boolean saveFileToWorkspacePath(PcClient pcClient, String trendReportID, int runId, TrendReportTypes.DataType dataType, TrendReportTypes.PctType pctType, TrendReportTypes.Measurement measurement) throws IOException, PcException, IntrospectionException, NoSuchMethodException {
        String fileName = measurement.toString().toLowerCase() + "_" + pctType.toString().toLowerCase() + ".csv";
        Map<String, String> measurementMap = pcClient.getTrendReportByXML(trendReportID, runId, dataType, pctType, measurement);
        try {
            FilePath filePath = new FilePath(Workspace.getChannel(), getWorkspacePath().getPath() + "/" + fileName);
            String filepathContent = "";
            for (String key : measurementMap.keySet()) {
                filepathContent += key + ",";
            }
            filepathContent += "\r\n";
            for (String value : measurementMap.values()) {
                filepathContent += value + ",";
            }
            filePath.write(filepathContent, null);
            return true;
        } catch (InterruptedException e) {
            if (getWorkspacePath().getPath() != null)
                logger.println(String.format("%s - %s: %s %s: %s. %s: %s",
                        DateFormatter.getDateTime(),
                        Messages.ErrorSavingFile(),
                        fileName,
                        Messages.ToWorkspacePath(),
                        getWorkspacePath().getPath(),
                        Messages.Error(),
                        e.getMessage()));
            else
                logger.println(String.format("%s - %s: %s. %s. %s: %s",
                        DateFormatter.getDateTime(),
                        Messages.ErrorSavingFile(),
                        fileName,
                        Messages.WorkspacePathIsUnavailable(),
                        Messages.Error(),
                        e.getMessage()));
            return false;
        }
    }

    private void updateTestStatus(Testcase testCase, PcRunResponse response, String errorMessage, String eventLog) {
        RunState runState = RunState.get(response.getRunState());
        if (runState == RUN_FAILURE) {
            setError(testCase,
                    String.format("%s. %s",
                            runState,
                            errorMessage),
                    eventLog);
        } else if (statusBySLA && runState == FINISHED && !(response.getRunSLAStatus().equalsIgnoreCase("passed"))) {
            setFailure(testCase, Messages.RunMeasurementsNotReachSLACriteria() + ": "
                    + response.getRunSLAStatus(), eventLog);
        } else if (runState.hasFailure()) {
            setFailure(testCase,
                    String.format("%s. %s",
                            runState,
                            errorMessage),
                    eventLog);
        } else if (errorMessage != null && !errorMessage.isEmpty()) {
            setFailure(testCase,
                    String.format("%s. %s",
                            runState,
                            errorMessage),
                    eventLog);
        } else {
            testCase.setStatus(JUnitTestCaseStatus.PASS);
        }
    }

    private void setError(Testcase testCase, String message, String eventLog) {
        Error error = new Error();
        error.setMessage(message);
        if (!(eventLog == null || eventLog.isEmpty()))
            testCase.getSystemErr().add(eventLog);
        testCase.getError().add(error);
        testCase.setStatus(JUnitTestCaseStatus.ERROR);
        logger.println(String.format("%s - %s %s",
                DateFormatter.getDateTime(),
                message,
                eventLog));
    }

    private void setFailure(Testcase testCase, String message, String eventLog) {
        Failure failure = new Failure();
        failure.setMessage(message);
        if (!(eventLog == null || eventLog.isEmpty()))
            testCase.getSystemErr().add(eventLog);
        testCase.getFailure().add(failure);
        testCase.setStatus(JUnitTestCaseStatus.FAILURE);
        logger.println(String.format("%s - %s: %s %s",
                DateFormatter.getDateTime(),
                Messages.Failure(),
                message,
                eventLog));
    }

    private String getOutputForReportLinks(Run<?, ?> build) {
        String urlPattern = getArtifactsUrlPattern(build);
        String viewUrl = String.format(urlPattern + "/%s", pcReportFileName);
        String downloadUrl = String.format(urlPattern + "/%s", "*zip*/pcRun");
        logger.println(String.format("%s - %s", DateFormatter.getDateTime(), HyperlinkNote.encodeTo(viewUrl, Messages.ViewAnalysisReportOfRun() + " " + runId)));

        return String.format("%s: %s" +
                        "\n\n%s:\n%s" +
                        "\n\n%s:\n%s",
                Messages.LoadTestRunID(), runId,
                Messages.ViewAnalysisReport(), getPcModel().getserverAndPort() + "/" + build.getUrl() + viewUrl,
                Messages.DownloadReport(), getPcModel().getserverAndPort() + "/" + build.getUrl() + downloadUrl);
    }

    private String getArtifactsUrlPattern(Run<?, ?> build) {

        String runReportUrlTemp = runReportStructure.replaceFirst("%s/", "");
        return String.format(
                runReportUrlTemp,
                artifactsResourceName);
    }

    private void provideStepResultStatus(Result resultStatus, Run<?, ?> build) {
        String runIdStr =
                (runId > 0) ? String.format(" (RunID: %s)", String.valueOf(runId)) : "";
        logger.println(String.format("%s - %s%s: %s\n- - -",
                DateFormatter.getDateTime(),
                Messages.ResultStatus(),
                runIdStr,
                resultStatus.toString()));
        build.setResult(resultStatus);

    }

    private Result createRunResults(FilePath filePath, Testsuites testsuites) {
        Result ret = Result.SUCCESS;
        try {
            if (testsuites != null) {
                StringWriter writer = new StringWriter();
                XStream xstream = new XStream();
                xstream.autodetectAnnotations(true);
                xstream.toXML(testsuites, writer);
                filePath.write(writer.toString(), null);
                if (containsErrorsOrFailures(testsuites.getTestsuite())) {
                    ret = Result.FAILURE;
                }
            } else {
                logger.println(String.format("%s - %s", DateFormatter.getDateTime(), Messages.EmptyResults()));
                ret = Result.FAILURE;
            }

        } catch (Exception cause) {
            logger.print(String.format(
                    "%s - %s. %s: %s",
                    DateFormatter.getDateTime(),
                    Messages.FailedToCreateRunResults(),
                    Messages.Exception(),
                    cause.getMessage()));
            ret = Result.FAILURE;
        }
        return ret;
    }

    private boolean containsErrorsOrFailures(List<Testsuite> testsuites) {
        boolean ret = false;
        for (Testsuite testsuite : testsuites) {
            for (Testcase testcase : testsuite.getTestcase()) {
                String status = testcase.getStatus();
                if (status.equals(JUnitTestCaseStatus.ERROR)
                        || status.equals(JUnitTestCaseStatus.FAILURE)) {
                    ret = true;
                    break;
                }
            }
        }
        return ret;
    }

    private String getJunitResultsFileName() {
        // Use LocalDateTime to get the current date and time
        LocalDateTime now = LocalDateTime.now();

        // Define the format pattern to match the original method's output
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyyHHmmssSSS");

        // Format the current date and time
        String time = now.format(formatter);

        // Generate the unique file name using the formatted time string
        junitResultsFileName = String.format("Results%s.xml", time);

        return junitResultsFileName;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace, @Nonnull Launcher launcher,
                        @Nonnull TaskListener listener) throws InterruptedException, IOException {
        Workspace = workspace;
        WorkspacePath = new File(workspace.toURI());
        Result resultStatus = Result.FAILURE;
        //trendReportReady = false;
        logger = listener.getLogger();
        if (credentialsId != null)
            usernamePCPasswordCredentials = getCredentialsById(credentialsId, build, logger);
        if (credentialsProxyId != null && !credentialsProxyId.isEmpty())
            usernamePCPasswordCredentialsForProxy = getCredentialsById(credentialsProxyId, build, logger);
        PcClient pcClient = new PcClient(getPcModel(), logger);
        Testsuites testsuites = execute(pcClient, build);

//        // Create Trend Report
//        if(trendReportReady){
//            String reportUrlTemp = trendReportStructure.replaceFirst("%s/", "") + "/trendReport%s.pdf";
//            String reportUrl = String.format(reportUrlTemp, artifactsResourceName, getPcModel().getTrendReportId(true));
//            pcClient.publishTrendReport(reportUrl, getPcModel().getTrendReportId(true));
//        }
//        // End Create Trend Report

        FilePath resultsFilePath = workspace.child(getJunitResultsFileName());
        resultStatus = createRunResults(resultsFilePath, testsuites);
        provideStepResultStatus(resultStatus, build);


        //add info for execution in pipeline mode
        ParametersAction parameterAction = build.getAction(ParametersAction.class);
        List<ParameterValue> newParams = (parameterAction != null) ? new ArrayList<>(parameterAction.getAllParameters()) : new ArrayList<>();
        newParams.add(new StringParameterValue("buildStepName", "PcBuilder"));
        newParams.add(new StringParameterValue("resultsFilename", this.getRunResultsFileName()));

        if (parameterAction instanceof AdditionalParametersAction) {
            build.addOrReplaceAction(new AdditionalParametersAction(newParams));
        } else {
            build.addOrReplaceAction(new ParametersAction(newParams));
        }


        if (!Result.SUCCESS.equals(resultStatus) && !Result.FAILURE.equals(resultStatus)) {
            return;
        }
//        //Only do this if build worked (Not unstable or aborted - which might mean there is no report
//        JUnitResultArchiver jUnitResultArchiver = new JUnitResultArchiver(this.getRunResultsFileName());
//        jUnitResultArchiver.setKeepLongStdio(true);
//        jUnitResultArchiver.perform(build, workspace, launcher, listener);

    }

    public String getServerAndPort() {
        return getPcModel().getserverAndPort();
    }

    public String getPcServerName() {
        return getPcModel().getPcServerName();
    }

    public String getAlmProject() {
        return getPcModel().getAlmProject();
    }

    public String getTestId() {
        return getPcModel().getTestId();
    }

    public String getAlmDomain() {
        return getPcModel().getAlmDomain();
    }

    public String getTimeslotDurationHours() {
        return getPcModel().getTimeslotDurationHours();
    }

    public String getTimeslotDurationMinutes() {
        return getPcModel().getTimeslotDurationMinutes();
    }

    public PostRunAction getPostRunAction() {
        return getPcModel().getPostRunAction();
    }

    public String getTrendReportId() {
        return getPcModel().getTrendReportId(true);
    }

    public String getAutoTestInstanceID() {
        return getPcModel().getAutoTestInstanceID();
    }

    public String getTestInstanceId() {
        return getPcModel().getTestInstanceId();
    }

    public String getAddRunToTrendReport() {
        return getPcModel().getAddRunToTrendReport();
    }

    public boolean isVudsMode() {
        return getPcModel().isVudsMode();
    }

    public boolean isAuthenticateWithToken() {
        return getPcModel().isAuthenticateWithToken();
    }

    public String getRetry() {
        return getPcModel().getRetry();
    }

    public String getRetryOccurrences() {
        return getPcModel().getRetryOccurrences();
    }

    public String getRetryDelay() {
        return getPcModel().getRetryDelay();
    }

    public String getDescription() {
        return getPcModel().getDescription();
    }

    public boolean isHTTPSProtocol() {
        return getPcModel().httpsProtocol();
    }

    public boolean isStatusBySLA() {
        return statusBySLA;
    }

    public String getProxyOutURL() {
        return getPcModel().getProxyOutURL();
    }

    // This indicates to Jenkins that this is an implementation of an extension
    // point
    @Extension
    @Symbol("pcBuild")
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public DescriptorImpl() {

            load();
        }

        @Override
        public boolean isApplicable(
                @SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {

            return true;
        }

        @Override
        public String getDisplayName() {

            return Messages.DisplayName();
        }

        public FormValidation doCheckPcServerName(@QueryParameter String value) {

            return validateString(value, "Server");
        }

        public FormValidation doCheckAlmDomain(@QueryParameter String value) {

            return validateString(value, "Domain");
        }

        public FormValidation doCheckAlmProject(@QueryParameter String value) {

            return validateString(value, "Project");
        }

        public FormValidation doCheckTestId(@QueryParameter String value) {

            return validateHigherThanInt(value, "Test ID", 0, true);
        }

        public FormValidation doCheckRetryDelay(@QueryParameter String value) {

            return validateHigherThanInt(value, "Delay between attempts (in minutes)", 0, true);
        }

        public FormValidation doCheckRetryOccurrences(@QueryParameter String value) {

            return validateHigherThanInt(value, "Number of attempts", 0, true);
        }

        // if autoTestInstanceID is selected we don't need to check the validation of the test instance
//        public static FormValidation CheckOnlyAutoTestInstanceId(String autoTestInstanceID){
//            if(autoTestInstanceID.equals("AUTO"))
//                return FormValidation.ok();
//            else
//                return FormValidation.error("Error ");
//        }


        public FormValidation doCheckTestInstanceId(@QueryParameter String value) {
            return validateHigherThanInt(value, "Test Instance ID", 0, true);
        }


        public FormValidation doCheckTimeslotDuration(@QueryParameter TimeslotDuration value) {

            return validateHigherThanInt(
                    String.valueOf(value.toMinutes()),
                    "Timeslot Duration (in minutes)",
                    30,
                    false);
        }

        public FormValidation doCheckTimeslotId(@QueryParameter String value) {

            return validateHigherThanInt(value, "Timeslot ID", 0, true);
        }

        public FormValidation doCheckCredentialsId(@AncestorInPath Item project,
                                                   @QueryParameter String pcUrl,
                                                   @QueryParameter String value) {
            return checkCredentialsId(project, pcUrl, value);
        }

        public FormValidation doCheckCredentialsProxyId(@AncestorInPath Item project,
                                                        @QueryParameter String pcUrl,
                                                        @QueryParameter String value) {
            return checkCredentialsId(project, pcUrl, value);
        }

        public FormValidation checkCredentialsId(@AncestorInPath Item project,
                                                 @QueryParameter String pcUrl,
                                                 @QueryParameter String credentialIdValue) {
            if (project == null || !project.hasPermission(Item.EXTENDED_READ)) {
                return FormValidation.ok();
            }

            String credentialIdValueStr = Util.fixEmptyAndTrim(credentialIdValue);
            if (credentialIdValueStr == null) {
                return FormValidation.ok();
            }

            String pcUrlStr = Util.fixEmptyAndTrim(pcUrl);
            if (pcUrlStr == null)
            // not set, can't check
            {
                return FormValidation.ok();
            }

            if (pcUrlStr.indexOf('$') >= 0)
            // set by variable, can't check
            {
                return FormValidation.ok();
            }

            for (ListBoxModel.Option o : CredentialsProvider.listCredentials(
                    StandardUsernamePasswordCredentials.class,
                    project,
                    project instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task) project) : ACL.SYSTEM,
                    URIRequirementBuilder.create().build(),
                    new IdMatcher(credentialIdValueStr))) {

                if (StringUtils.equals(credentialIdValueStr, o.value)) {
                    return FormValidation.ok();
                }
            }
            // no credentials available, can't check
            return FormValidation.warning(String.format("%s s",
                    Messages.CannotFindAnyCredentials(),
                    credentialIdValueStr));
        }


        /**
         * @param limitIncluded if true, value must be higher than limit. if false, value must be equal to or
         *                      higher than limit.
         */
        private FormValidation validateHigherThanInt(
                String value,
                String field,
                int limit,
                boolean limitIncluded) {
            FormValidation ret = FormValidation.ok();
            value = value.trim();
            if (StringUtils.isBlank(value)) {
                ret = FormValidation.error(" " + Messages.MustBeSet());
            } else {
                try {
                    //regular expression: parameter (with brackets or not)
                    if (value.matches("^\\$\\{[\\w-. ]*}$|^\\$[\\w-.]*$"))
                        return ret;
                        //regular expression: number
                    else if (value.matches("[0-9]*$|")) {
                        if (limitIncluded && Integer.parseInt(value) <= limit)
                            ret = FormValidation.error(" " + Messages.MustBeHigherThan() + " " + limit);
                        else if (Integer.parseInt(value) < limit)
                            ret = FormValidation.error(" " + Messages.MustBeAtLeast() + " " + limit);
                    } else
                        ret = FormValidation.error(" " + Messages.MustBeAWholeNumberOrAParameter() + ", " + Messages.ForExample() + ": 23, $TESTID or ${TEST_ID}.");
                } catch (Exception e) {
                    ret = FormValidation.error(" " + Messages.MustBeAWholeNumberOrAParameter() + " (" + Messages.ForExample() + ": $TESTID or ${TestID})");
                }
            }

            return ret;

        }

        private FormValidation validateString(String value, String field) {
            FormValidation ret = FormValidation.ok();
            if (StringUtils.isBlank(value.trim())) {
                ret = FormValidation.error(field + " " + Messages.MustBeSet());
            }

            return ret;
        }


        public List<PostRunAction> getPostRunActions() {

            return PcModel.getPostRunActions();
        }


        /**
         * To fill in the credentials drop down list which's field is 'credentialsId'.
         * This method's name works with tag <c:select/>.
         */
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item project,
                                                     @QueryParameter String credentialsId) {

            if (project == null || !project.hasPermission(Item.CONFIGURE)) {
                return new StandardUsernameListBoxModel().includeCurrentValue(credentialsId);
            }
            return new StandardUsernameListBoxModel()
                    .includeEmptyValue()
                    .includeAs(
                            project instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task) project) : ACL.SYSTEM,
                            project,
                            StandardUsernamePasswordCredentials.class,
                            URIRequirementBuilder.create().build())
                    .includeCurrentValue(credentialsId);
        }

        /**
         * To fill in the credentials drop down list which's field is 'credentialsProxyId'.
         * This method's name works with tag <c:select/>.
         */
        public ListBoxModel doFillCredentialsProxyIdItems(@AncestorInPath Item project,
                                                          @QueryParameter String credentialsId) {

            return doFillCredentialsIdItems(project, credentialsId);
        }

    }

    private class TriTrendReportTypes {

        private TrendReportTypes.DataType dataType;
        private TrendReportTypes.PctType pctType;
        private TrendReportTypes.Measurement measurement;

        TriTrendReportTypes(TrendReportTypes.DataType dataType, TrendReportTypes.PctType pctType, TrendReportTypes.Measurement measurement) {
            this.dataType = dataType;
            this.pctType = pctType;
            this.measurement = measurement;
        }

        public TrendReportTypes.DataType getDataType() {
            return dataType;
        }

        public TrendReportTypes.PctType getPctType() {
            return pctType;
        }

        public TrendReportTypes.Measurement getMeasurement() {
            return measurement;
        }
    }
}
