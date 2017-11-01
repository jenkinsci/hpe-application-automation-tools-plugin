/*
 * MIT License
 *
 * Copyright (c) 2016 Hewlett-Packard Development Company, L.P.
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
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
 */


/*
* Create the PCModel and the PCClient and allows the connection between the job and PC
* */
package com.hpe.application.automation.tools.run;

import com.hpe.application.automation.tools.common.PcException;
import com.hpe.application.automation.tools.model.PcModel;
import com.hpe.application.automation.tools.model.PostRunAction;
import com.hpe.application.automation.tools.model.TimeslotDuration;
import com.hpe.application.automation.tools.pc.*;
import com.hpe.application.automation.tools.pc.RunState;
import com.hpe.application.automation.tools.sse.result.model.junit.Error;
import com.hpe.application.automation.tools.sse.result.model.junit.Failure;
import com.hpe.application.automation.tools.sse.result.model.junit.*;
import com.hpe.application.automation.tools.pc.PcClient;
import com.hpe.application.automation.tools.pc.PcRunEventLog;
import com.hpe.application.automation.tools.pc.PcRunEventLogRecord;
import com.hpe.application.automation.tools.pc.PcRunResponse;
import com.hpe.application.automation.tools.sse.result.model.junit.JUnitTestCaseStatus;
import com.hpe.application.automation.tools.sse.result.model.junit.Testcase;
import com.hpe.application.automation.tools.sse.result.model.junit.Testsuite;
import com.hpe.application.automation.tools.sse.result.model.junit.Testsuites;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.PluginWrapper;
import hudson.console.HyperlinkNote;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.beans.IntrospectionException;
import java.io.*;
import java.lang.reflect.Method;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.hpe.application.automation.tools.pc.RunState.FINISHED;
import static com.hpe.application.automation.tools.pc.RunState.RUN_FAILURE;

public class PcBuilder extends Builder implements SimpleBuildStep{
    
    private static final String artifactsDirectoryName = "archive";
    public static final String artifactsResourceName = "artifact";
    public static final String runReportStructure = "%s/%s/performanceTestsReports/pcRun";
    public static final String trendReportStructure = "%s/%s/performanceTestsReports/TrendReports";
    public static final String pcReportArchiveName = "Reports.zip";
    public static final String pcReportFileName = "Report.html";
    private static final String RUNID_BUILD_VARIABLE = "HP_RUN_ID";

    public static final String    TRENDED         = "Trended";
    public static final String    PENDING         = "Pending";
    public static final String    PUBLISHING      = "Publishing";
    public static final String    ERROR           = "Error";
    
    private final PcModel pcModel;


    private final String almPassword;
    private final String timeslotDurationHours;
    private final String timeslotDurationMinutes;
    private final boolean statusBySLA;
    private int runId;
    private String testName;
    private FilePath pcReportFile;
    private String junitResultsFileName;
    private PrintStream logger;
    private File WorkspacePath;
    
    @DataBoundConstructor
    public PcBuilder(
            String serverAndPort,
            String pcServerName,
            String almUserName,
            String almPassword,
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
            String proxyOutUser,
            String proxyOutPassword) {
        this.almUserName = almUserName;
        this.almPassword = almPassword;
        this.timeslotDurationHours = timeslotDurationHours;
        this.timeslotDurationMinutes = timeslotDurationMinutes;
        this.statusBySLA = statusBySLA;

        pcModel =
                new PcModel(
                        serverAndPort.trim(),
                        pcServerName.trim(),
                        almUserName.trim(),
                        almPassword,
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
                        proxyOutUser,
                        proxyOutPassword);
    }
    
    @Override
    public DescriptorImpl getDescriptor() {
        
        return (DescriptorImpl) super.getDescriptor();
    }
    
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        WorkspacePath =  new File(build.getWorkspace().toURI());
        perform(build, build.getWorkspace(), launcher, listener);

        return true;
    }

    public File getWorkspacePath(){
        return WorkspacePath;
    }
    
    public PcModel getPcModel() {
        
        return pcModel;
    }
    
    public String getRunResultsFileName() {
        
        return junitResultsFileName;
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

    private Testsuites execute(PcClient pcClient, Run<?, ?> build)
            throws InterruptedException,NullPointerException {
        try {
            if (!StringUtils.isBlank(pcModel.getDescription()))
                logger.println("- - -\nTest description: " + pcModel.getDescription());
            if (!beforeRun(pcClient))
                return null;

            return run(pcClient, build);

        } catch (InterruptedException e) {
            build.setResult(Result.ABORTED);
            pcClient.stopRun(runId);
            throw e;
        } catch (NullPointerException e) {
            logger.println("Error: Run could not start!");
        } catch (Exception e) {
            logger.println(e);
        } finally {
            pcClient.logout();
        }
        return null;
    }


    private Testsuites run(PcClient pcClient, Run<?, ?> build)
            throws InterruptedException, ClientProtocolException,
            IOException, PcException {
        
        PcRunResponse response = null;
        String errorMessage = "";
        String eventLogString = "";
        boolean trendReportReady = false;
        try {
            runId = pcClient.startRun();
            testName = pcClient.getTestName();

            List<ParameterValue> parameters = new ArrayList<>();
            parameters.add(new StringParameterValue(RUNID_BUILD_VARIABLE, "" + runId));
            // This allows a user to access the runId from within Jenkins using a build variable.
            build.addAction(new AdditionalParametersAction(parameters));
            logger.print("Set " + RUNID_BUILD_VARIABLE + " Env Variable to " + runId + "\n");

            response = pcClient.waitForRunCompletion(runId);


            if (response != null && RunState.get(response.getRunState()) == FINISHED) {
                pcReportFile = pcClient.publishRunReport(runId, getReportDirectory(build));

                // Adding the trend report section if ID has been set
                if(("USE_ID").equals(pcModel.getAddRunToTrendReport()) && pcModel.getTrendReportId() != null && RunState.get(response.getRunState()) != RUN_FAILURE){
                    pcClient.addRunToTrendReport(this.runId, pcModel.getTrendReportId());
                    pcClient.waitForRunToPublishOnTrendReport(this.runId, pcModel.getTrendReportId());
                    pcClient.downloadTrendReportAsPdf(pcModel.getTrendReportId(), getTrendReportsDirectory(build));
                    trendReportReady = true;
                }

                // Adding the trend report if the Associated Trend report is selected.
                if(("ASSOCIATED").equals(pcModel.getAddRunToTrendReport()) && RunState.get(response.getRunState()) != RUN_FAILURE){
                    pcClient.addRunToTrendReport(this.runId, pcModel.getTrendReportId());
                    pcClient.waitForRunToPublishOnTrendReport(this.runId, pcModel.getTrendReportId());
                    pcClient.downloadTrendReportAsPdf(pcModel.getTrendReportId(), getTrendReportsDirectory(build));
                    trendReportReady = true;
                }

            } else if (response != null && RunState.get(response.getRunState()).ordinal() > FINISHED.ordinal()) {
                PcRunEventLog eventLog = pcClient.getRunEventLog(runId);
                eventLogString = buildEventLogString(eventLog);
            }


        } catch (PcException e) {
            errorMessage = e.getMessage();
            logger.println("Error: " + errorMessage);
        }

        Testsuites ret = new Testsuites();
        parsePcRunResponse(ret,response, build, errorMessage, eventLogString);
        try {
            parsePcTrendResponse(ret,build,pcClient,trendReportReady,pcModel.getTrendReportId(),runId);
        } catch (IntrospectionException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        return ret;
    }
    
    private String buildEventLogString(PcRunEventLog eventLog) {
        
        String logFormat = "%-5s | %-7s | %-19s | %s\n";
        StringBuilder eventLogStr = new StringBuilder("Event Log:\n\n" + String.format(logFormat, "ID", "TYPE", "TIME","DESCRIPTION"));
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
        
        logger.println("Validating parameters before run");
        String prefix = "doCheck";
        boolean ret = true;
        Method[] methods = getDescriptor().getClass().getMethods();
        Method[] modelMethods = pcModel.getClass().getMethods();
        for (Method method : methods) {
            String name = method.getName();
            if (name.startsWith(prefix)) {
                name = name.replace(prefix, "").toLowerCase();
                for (Method modelMethod : modelMethods) {
                    String modelMethodName = modelMethod.getName();
                    if (modelMethodName.toLowerCase().equals("get" + name)) {
                        try {
                            Object obj = FormValidation.ok();
                            if (!(name.equals("testinstanceid")&& pcModel.getAutoTestInstanceID().equals("AUTO"))){
                                obj = method.invoke(getDescriptor(), modelMethod.invoke(getPcModel()));
                            }
                            if (!obj.equals(FormValidation.ok())) {
                                logger.println(obj);
                                ret = false;
                            }
                            break;
                        } catch (Exception e) {
                            logger.println(e);
                        }
                    }
                }
            }
        }

        boolean isTrendReportIdValid = validateTrendReportIdIsNumeric(getPcModel().getTrendReportId(),("USE_ID").equals(getPcModel().getAddRunToTrendReport()));

        ret &= isTrendReportIdValid;
        return ret;
        
    }



    private boolean validateTrendReportIdIsNumeric(String trendReportId, boolean addRunToTrendReport){

        FormValidation res = FormValidation.ok();
        if(addRunToTrendReport){
            if(trendReportId.isEmpty()){
                res = FormValidation.error("Parameter Is Missing: trend report ID is missing");
            }
            else{

                try{

                    Integer.parseInt(trendReportId);
                }
                catch(NumberFormatException e) {

                    res = FormValidation.error("Illegal Parameter: trend report ID is not a number");
                }

            }
        }

        logger.println(res.toString().replace(": <div/>",""));

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

    private Testsuites parsePcTrendResponse(Testsuites ret,Run<?, ?> build,PcClient pcClient,boolean trendReportReady,String TrendReportID, int runID) throws PcException,IntrospectionException,IOException, InterruptedException ,NoSuchMethodException{


        if(trendReportReady){
            String reportUrlTemp = trendReportStructure.replaceFirst("%s/", "") + "/trendReport%s.pdf";
            String reportUrl = String.format(reportUrlTemp, artifactsResourceName, pcModel.getTrendReportId());
            pcClient.publishTrendReport(reportUrl, pcModel.getTrendReportId());

            // Updating all CSV files for plot plugin
            // this helps to show the transaction of each result
            if (isPluginActive("Plot plugin")) {
                logger.println("Updating csv files for Trending Charts.");
                updateCSVFilesForPlot(pcClient, runID);
                String plotUrlPath = "/job/" + build.getParent().getName() + "/plot";
                logger.println(HyperlinkNote.encodeTo(plotUrlPath, "Trending Charts")); // + HyperlinkNote.encodeTo("https://wiki.jenkins-ci.org/display/JENKINS/HP+Application+Automation+Tools#HPApplicationAutomationTools-RunningPerformanceTestsusingHPPerformanceCenter","More Info"));
            }else{
                logger.println("You can view Trending Charts directly from Jenkins using Plot Plugin, see more details on the " + HyperlinkNote.encodeTo("https://wiki.jenkins.io/display/JENKINS/HPE+Application+Automation+Tools#HPEApplicationAutomationTools-RunningPerformanceTestsusingHPEPerformanceCenter","documentation") + "(Performance Center 12.55 and Later).");
            }
        }
        return ret;
    }

    private boolean isPluginActive(String pluginDisplayName){
        List<PluginWrapper> allPlugin = Jenkins.getInstance().pluginManager.getPlugins();
        for (PluginWrapper pw :
                allPlugin) {

            if (pw.getDisplayName().toLowerCase().equals(pluginDisplayName.toLowerCase())) {
                return pw.isActive();
            }
        }
        return false;
    }

    private void updateCSVFilesForPlot(PcClient pcClient, int runId) throws IOException, PcException, IntrospectionException, NoSuchMethodException {

        //Map<String, String> measurementMap =pcClient.getTrendReportByXML(pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_AVERAGE);

        // Transaction - TRT
        saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_MINIMUM);
        saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_MAXIMUM);
        saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_AVERAGE);
        saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_MEDIAN);
        saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_STDDEVIATION);
        saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_COUNT1);
        //saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_SUM1);
        saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_PERCENTILE_90);

        // Transaction - TPS
        saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TPS, TrendReportTypes.Measurement.PCT_MINIMUM);
        saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TPS, TrendReportTypes.Measurement.PCT_MAXIMUM);
        saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TPS, TrendReportTypes.Measurement.PCT_AVERAGE);
        saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TPS, TrendReportTypes.Measurement.PCT_MEDIAN);
        //saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TPS, TrendReportTypes.Measurement.PCT_STDDEVIATION);
        saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TPS, TrendReportTypes.Measurement.PCT_SUM1);

        // Transaction - TRS
        //saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRS, TrendReportTypes.Measurement.PCT_MINIMUM);
        //saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRS, TrendReportTypes.Measurement.PCT_MAXIMUM);
        //saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRS, TrendReportTypes.Measurement.PCT_AVERAGE);
        //saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRS, TrendReportTypes.Measurement.PCT_MEDIAN);
        //saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRS, TrendReportTypes.Measurement.PCT_STDDEVIATION);
        saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRS, TrendReportTypes.Measurement.PCT_COUNT1);


        // Monitors - UDP
        saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Monitors, TrendReportTypes.PctType.UDP, TrendReportTypes.Measurement.PCT_MINIMUM);
        saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Monitors, TrendReportTypes.PctType.UDP, TrendReportTypes.Measurement.PCT_MAXIMUM);
        saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Monitors, TrendReportTypes.PctType.UDP, TrendReportTypes.Measurement.PCT_AVERAGE);
        saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Monitors, TrendReportTypes.PctType.UDP, TrendReportTypes.Measurement.PCT_MEDIAN);
        saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Monitors, TrendReportTypes.PctType.UDP, TrendReportTypes.Measurement.PCT_STDDEVIATION);
        saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Monitors, TrendReportTypes.PctType.UDP, TrendReportTypes.Measurement.PCT_COUNT1);
        saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Monitors, TrendReportTypes.PctType.UDP, TrendReportTypes.Measurement.PCT_SUM1);

        // Regular - VU
        //saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.VU, TrendReportTypes.Measurement.PCT_MINIMUM);
        saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.VU, TrendReportTypes.Measurement.PCT_MAXIMUM);
        saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.VU, TrendReportTypes.Measurement.PCT_AVERAGE);
        //saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.VU, TrendReportTypes.Measurement.PCT_MEDIAN);
        //saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.VU, TrendReportTypes.Measurement.PCT_STDDEVIATION);


        // Regular - WEB
        saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.WEB, TrendReportTypes.Measurement.PCT_MINIMUM);
        saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.WEB, TrendReportTypes.Measurement.PCT_MAXIMUM);
        saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.WEB, TrendReportTypes.Measurement.PCT_AVERAGE);
        saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.WEB, TrendReportTypes.Measurement.PCT_MEDIAN);
        //saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.WEB, TrendReportTypes.Measurement.PCT_STDDEVIATION);
        saveFileToWorkspacePath(pcClient,pcModel.getTrendReportId(),runId,TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.WEB, TrendReportTypes.Measurement.PCT_SUM1);



      //  logger.print(build.getRootDir().getPath());
    }


    private boolean saveFileToWorkspacePath(PcClient pcClient, String trendReportID, int runId,TrendReportTypes.DataType dataType, TrendReportTypes.PctType pctType, TrendReportTypes.Measurement measurement)throws IOException, PcException, IntrospectionException, NoSuchMethodException{
        String fileName = measurement.toString().toLowerCase()  + "_" +  pctType.toString().toLowerCase() + ".csv";
        File file = new File(getWorkspacePath().getPath() + "/" + fileName);
        try{
            Map<String, String> measurementMap = pcClient.getTrendReportByXML(trendReportID,runId,dataType, pctType, measurement);
            if (!file.exists()){
                file.createNewFile();
            }
            PrintWriter writer = new PrintWriter(file);
            for (String key : measurementMap.keySet()) {
                writer.print(key + ",");
            }
            writer.print("\r\n");
            for (String value : measurementMap.values()) {
                writer.print(value + ",");
            }
            writer.close();
       //     logger.println(fileName + " Created.");
            return true;
        } catch (IOException e) {
            logger.println("Error saving file: "+ fileName + " with Error: " + e.getMessage());
            return false;
        }

    }


    private void updateTestStatus(Testcase testCase, PcRunResponse response, String errorMessage, String eventLog) {
        RunState runState = RunState.get(response.getRunState());
        if (runState == RUN_FAILURE) {
            setError(testCase, String.format("%s. %s", runState, errorMessage), eventLog);
        } else if (statusBySLA && runState == FINISHED && !(response.getRunSLAStatus().equalsIgnoreCase("passed"))) {
            setFailure(testCase, "Run measurements did not reach SLA criteria. Run SLA Status: "
                                 + response.getRunSLAStatus(), eventLog);
        } else if (runState.hasFailure()) {          
            setFailure(testCase, String.format("%s. %s", runState, errorMessage), eventLog);
        } else if(errorMessage != null && !errorMessage.isEmpty()){
            setFailure(testCase, String.format("%s. %s", runState, errorMessage), eventLog);
        }
        else{
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
        logger.println(String.format("%s %s", message ,eventLog));
    }
    
    private void setFailure(Testcase testCase, String message, String eventLog) {
        Failure failure = new Failure();
        failure.setMessage(message);
        if (!(eventLog == null || eventLog.isEmpty()))
            testCase.getSystemErr().add(eventLog);
        testCase.getFailure().add(failure);
        testCase.setStatus(JUnitTestCaseStatus.FAILURE);
        logger.println(String.format("Failure: %s %s", message ,eventLog));
    }
    
    private String getOutputForReportLinks(Run<?, ?> build) {
        String urlPattern = getArtifactsUrlPattern(build);
        String viewUrl = String.format(urlPattern + "/%s", pcReportFileName);
        String downloadUrl = String.format(urlPattern + "/%s", "*zip*/pcRun");
        logger.println(HyperlinkNote.encodeTo(viewUrl, "View analysis report of run " + runId));

        return String.format("Load Test Run ID: %s\n\nView analysis report:\n%s\n\nDownload Report:\n%s", runId, pcModel.getserverAndPort() +  "/" +  build.getUrl() + viewUrl, pcModel.getserverAndPort() + "/" + build.getUrl() + downloadUrl);
    }
    
    private String getArtifactsUrlPattern(Run<?, ?> build) {

        String runReportUrlTemp = runReportStructure.replaceFirst("%s/", "");
        return String.format(
                runReportUrlTemp,
                artifactsResourceName);
    }
    
    private void provideStepResultStatus(Result resultStatus, Run<?, ?> build) {
        String runIdStr =
                (runId > 0) ? String.format(" (PC RunID: %s)", String.valueOf(runId)) : "";
        logger.println(String.format(
                "Result Status%s: %s\n- - -",
                runIdStr,
                resultStatus.toString()));
        build.setResult(resultStatus);
        
    }
    
    private Result createRunResults(FilePath filePath, Testsuites testsuites) {
        Result ret = Result.SUCCESS;
        try {
            if (testsuites != null) {
                StringWriter writer = new StringWriter();
                JAXBContext context = JAXBContext.newInstance(Testsuites.class);
                Marshaller marshaller = context.createMarshaller();
                marshaller.marshal(testsuites, writer);
                filePath.write(writer.toString(), null);
                if (containsErrorsOrFailures(testsuites.getTestsuite())) {
                    ret = Result.UNSTABLE;
                }
            } else {
                logger.println("Empty Results");
                ret = Result.UNSTABLE;
            }
            
        } catch (Exception cause) {
            logger.print(String.format(
                    "Failed to create run results, Exception: %s",
                    cause.getMessage()));
            ret = Result.UNSTABLE;
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
        Format formatter = new SimpleDateFormat("ddMMyyyyHHmmssSSS");
        String time = formatter.format(new Date());
        junitResultsFileName = String.format("Results%s.xml", time);
        return junitResultsFileName;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace, @Nonnull Launcher launcher,
                        @Nonnull TaskListener listener) throws InterruptedException, IOException {
        Result resultStatus = Result.FAILURE;
        //trendReportReady = false;
        logger = listener.getLogger();
        PcClient pcClient = new PcClient(pcModel, logger);
        Testsuites testsuites = execute(pcClient, build);

//        // Create Trend Report
//        if(trendReportReady){
//            String reportUrlTemp = trendReportStructure.replaceFirst("%s/", "") + "/trendReport%s.pdf";
//            String reportUrl = String.format(reportUrlTemp, artifactsResourceName, pcModel.getTrendReportId());
//            pcClient.publishTrendReport(reportUrl, pcModel.getTrendReportId());
//        }
//        // End Create Trend Report

        FilePath resultsFilePath = workspace.child(getJunitResultsFileName());
        resultStatus = createRunResults(resultsFilePath, testsuites);
        provideStepResultStatus(resultStatus, build);

        if (!Result.SUCCESS.equals(resultStatus) && !Result.FAILURE.equals(resultStatus)) {
            return;
        }
//        //Only do this if build worked (Not unstable or aborted - which might mean there is no report
//        JUnitResultArchiver jUnitResultArchiver = new JUnitResultArchiver(this.getRunResultsFileName());
//        jUnitResultArchiver.setKeepLongStdio(true);
//        jUnitResultArchiver.perform(build, workspace, launcher, listener);

    }

    public String getServerAndPort()
    {
        return getPcModel().getserverAndPort();
    }
    public String getPcServerName()
    {
        return getPcModel().getPcServerName();
    }

    public String getAlmProject()
    {
        return getPcModel().getAlmProject();
    }
    public String getTestId()
    {
        return getPcModel().getTestId();
    }
    public String getAlmDomain()
    {
        return getPcModel().getAlmDomain();
    }
    public String getTimeslotDurationHours()
    {
        return timeslotDurationHours;
    }
    public String getTimeslotDurationMinutes()
    {
        return timeslotDurationMinutes;
    }
    public PostRunAction getPostRunAction()
    {
        return getPcModel().getPostRunAction();
    }

    public String getTrendReportId()
    {
        return getPcModel().getTrendReportId();
    }

    public String getAutoTestInstanceID()
    {
        return getPcModel().getAutoTestInstanceID();
    }
    public String getTestInstanceId()
    {
        return getPcModel().getTestInstanceId();
    }


    public String getAddRunToTrendReport()
    {
        return getPcModel().getAddRunToTrendReport();
    }


    public boolean isVudsMode()
    {
        return getPcModel().isVudsMode();
    }

    public String getDescription()
    {
        return getPcModel().getDescription();
    }
    public String getAlmUserName() {
        return almUserName;
    }

    private final String almUserName;

    public String getAlmPassword() {
        return almPassword;
    }

    public boolean isHTTPSProtocol()
    {
        return getPcModel().httpsProtocol();
    }

    public boolean isStatusBySLA() {
        return statusBySLA;
    }

    public String getProxyOutURL(){ return getPcModel().getProxyOutURL();}
    public String getProxyOutUser(){ return getPcModel().getProxyOutUser();}
    public String getProxyOutPassword(){ return getPcModel().getProxyOutPassword();}

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
            
            return "Execute performance test using Performance Center";
        }
        
        public FormValidation doCheckPcServerName(@QueryParameter String value) {
            
           return validateString(value, "PC Server");
        }
        
        public FormValidation doCheckAlmUserName(@QueryParameter String value) {
            
            return validateString(value, "User name");
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


        // if autoTestInstanceID is selected we don't need to check the validation of the test instance
//        public static FormValidation CheckOnlyAutoTestInstanceId(String autoTestInstanceID){
//            if(autoTestInstanceID.equals("AUTO"))
//                return FormValidation.ok();
//            else
//                return FormValidation.error("Error ");
//        }



        public FormValidation doCheckTestInstanceId(@QueryParameter String value){
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


        /**
         * @param limitIncluded
         *            if true, value must be higher than limit. if false, value must be equal to or
         *            higher than limit.
         */
        private FormValidation validateHigherThanInt(
                String value,
                String field,
                int limit,
                boolean limitIncluded) {
            FormValidation ret = FormValidation.ok();
            value = value.trim();
            String messagePrefix = field + " must be ";
            if (StringUtils.isBlank(value)) {
                ret = FormValidation.error(messagePrefix + "set");
            } else {
                try {
                    if (limitIncluded && Integer.parseInt(value) <= limit)
                        ret = FormValidation.error(messagePrefix + "higher than " + limit);
                    else if (Integer.parseInt(value) < limit)
                        ret = FormValidation.error(messagePrefix + "at least " + limit);
                } catch (Exception e) {
                    ret = FormValidation.error(messagePrefix + "a whole number");
                }
            }
            
            return ret;
            
        }
        
        private FormValidation validateString(String value, String field) {
            FormValidation ret = FormValidation.ok();
            if (StringUtils.isBlank(value.trim())) {
                ret = FormValidation.error(field + " must be set");
            }
            
            return ret;
        }

        
        public List<PostRunAction> getPostRunActions() {
            
            return PcModel.getPostRunActions();
        }

        
    }
    
}
