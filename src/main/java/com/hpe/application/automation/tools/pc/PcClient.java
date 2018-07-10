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


/*
*  Implements the main method of loadtest
*
* */

package com.hpe.application.automation.tools.pc;

import hudson.FilePath;

import java.beans.IntrospectionException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.*;
import java.util.*;

import hudson.console.HyperlinkNote;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;

import com.microfocus.adm.performancecenter.plugins.common.pcEntities.*;
import com.microfocus.adm.performancecenter.plugins.common.rest.PcRestProxy;
import com.hpe.application.automation.tools.model.PcModel;
import com.hpe.application.automation.tools.run.PcBuilder;

public class PcClient {

    private PcModel model;
    private PcRestProxy restProxy;
    private boolean loggedIn;
    private PrintStream logger;

    public PcClient(PcModel pcModel, PrintStream logger) {
        try {
            model = pcModel;

            if(model.getProxyOutURL(true) != null && !model.getProxyOutURL(true).isEmpty()){
                logger.println("Using proxy: " + model.getProxyOutURL(true));
            }
            restProxy = new PcRestProxy(model.isHTTPSProtocol(),model.getPcServerName(true), model.getAlmDomain(true), model.getAlmProject(true), model.getProxyOutURL(true),model.getProxyOutUser(true),model.getProxyOutPassword(true));
            this.logger = logger;
        }catch (PcException e){
            logger.println(e.getMessage());
        }

    }

    public <T extends PcRestProxy> PcClient(PcModel pcModel, PrintStream logger, T proxy) {
        model = pcModel;
        restProxy = proxy;
        this.logger = logger;
    }

    public boolean login() {
        try {
            String user = model.getAlmUserName(true);
            logger.println(String.format("Trying to login\n[PCServer='%s://%s', User='%s']",model.isHTTPSProtocol(), model.getPcServerName(true), user));
            loggedIn = restProxy.authenticate(user, model.getAlmPassword(true).toString());
        } catch (PcException e) {
            logger.println(e.getMessage());
          //  stackTraceToString(e);
        } catch (Exception e) {
            logger.println(e);
           // stackTraceToString(e);
        }
        logger.println(String.format("Login %s", loggedIn ? "succeeded" : "failed"));
        return loggedIn;
    }

//    public void stackTraceToString(Throwable e) {
//        StringBuilder sb = new StringBuilder();
//        logger.println("DEBUGMSG - STACKTRACE");
//        for (StackTraceElement element : e.getStackTrace()) {
//            logger.println("DEBUGMSG - " + element.toString());
//        }
//    }


    public boolean isLoggedIn() {

        return loggedIn;
    }

    public int startRun() throws NumberFormatException, ClientProtocolException, PcException, IOException {




        int testID = Integer.parseInt(model.getTestId(true));
        int testInstance = getCorrectTestInstanceID(testID);
        setCorrectTrendReportID();

        logger.println(String.format("\nExecuting Load Test: \n====================\nTest ID: %s \nTest Instance ID: %s \nTimeslot Duration: %s \nPost Run Action: %s \nUse VUDS: %s\n====================\n", Integer.parseInt(model.getTestId(true)), testInstance, model.getTimeslotDuration() ,model.getPostRunAction().getValue(),model.isVudsMode()));
//        logger.println("Sending run request:\n" + model.runParamsToString());
        PcRunResponse response = restProxy.startRun(testID,
                testInstance,
                model.getTimeslotDuration(),
                model.getPostRunAction().getValue(),
                model.isVudsMode());
        logger.println(String.format("\nRun started (TestID: %s, RunID: %s, TimeslotID: %s)\n",
                response.getTestID(), response.getID(), response.getTimeslotID()));
        return response.getID();
    }

    private int getCorrectTestInstanceID(int testID) throws IOException, PcException {
        if("AUTO".equals(model.getAutoTestInstanceID())){
            try {


            logger.println("Searching for available Test Instance");
            PcTestInstances pcTestInstances = restProxy.getTestInstancesByTestId(testID);
            int testInstanceID = 0;
            if (pcTestInstances != null && pcTestInstances.getTestInstancesList() != null){
                PcTestInstance pcTestInstance = pcTestInstances.getTestInstancesList().get(pcTestInstances.getTestInstancesList().size()-1);
                testInstanceID = pcTestInstance.getInstanceId();
                logger.println("Found testInstanceId: " + testInstanceID);
            }else{
                logger.println("Could not find available TestInstanceID, Creating Test Instance.");
                logger.println("Searching for available TestSet");
                // Get a random TestSet
                PcTestSets pcTestSets = restProxy.GetAllTestSets();
                if (pcTestSets !=null && pcTestSets.getPcTestSetsList() !=null){
                    PcTestSet pcTestSet = pcTestSets.getPcTestSetsList().get(pcTestSets.getPcTestSetsList().size()-1);
                    int testSetID = pcTestSet.getTestSetID();
                    logger.println(String.format("Creating Test Instance with testID: %s and TestSetID: %s", testID,testSetID));
                    testInstanceID = restProxy.createTestInstance(testID,testSetID);
                    logger.println(String.format("Test Instance with ID : %s has been created successfully.", testInstanceID));
                }else{
                    String msg = "No TestSetID available in project, please create a testset from Performance Center UI";
                    logger.println(msg);
                    throw new PcException(msg);
                }
            }
            return testInstanceID;
            } catch (Exception e){
                logger.println(String.format("getCorrectTestInstanceID failed, reason: %s",e));
                return Integer.parseInt(null);
            }
        }
        return Integer.parseInt(model.getTestInstanceId(true));
    }

    private void setCorrectTrendReportID() throws IOException, PcException {
        // If the user selected "Use trend report associated with the test" we want the report ID to be the one from the test
        String msg = "No trend report ID is associated with the test.\n" +
                "Please turn Automatic Trending on for the test through Performance Center UI.\n" +
                "Alternatively you can check 'Add run to trend report with ID' on Jenkins job configuration.";
        if (("ASSOCIATED").equals(model.getAddRunToTrendReport()) && model.getPostRunAction() != PostRunAction.DO_NOTHING) {
            PcTest pcTest = restProxy.getTestData(Integer.parseInt(model.getTestId(true)));
            //if the trend report ID is parametrized
            if(!model.getTrendReportId().startsWith("$")) {
                if (pcTest.getTrendReportId() > -1)
                    model.setTrendReportId(String.valueOf(pcTest.getTrendReportId()));
                else {
                    throw new PcException(msg);
                }
            }
            else {
                try {
                    if (Integer.parseInt(model.getTrendReportId(true)) > -1)
                        model.setTrendReportId(String.valueOf(model.getTrendReportId(true)));
                    else {
                        throw new PcException(msg);
                    }
                }
                catch (Exception ex) {
                    throw new PcException(msg + System.getProperty("line.separator") + ex);
                }
            }
        }
    }

    public String getTestName()  throws IOException, PcException{
        PcTest pcTest = restProxy.getTestData(Integer.parseInt(model.getTestId(true)));
        return pcTest.getTestName();
    }

    public PcRunResponse waitForRunCompletion(int runId) throws InterruptedException, ClientProtocolException, PcException, IOException {

        return waitForRunCompletion(runId, 5000);
    }

    public PcRunResponse waitForRunCompletion(int runId, int interval) throws InterruptedException, ClientProtocolException, PcException, IOException {
        RunState state = RunState.UNDEFINED;
        switch (model.getPostRunAction()) {
            case DO_NOTHING:
                state = RunState.BEFORE_COLLATING_RESULTS;
                break;
            case COLLATE:
                state = RunState.BEFORE_CREATING_ANALYSIS_DATA;
                break;
            case COLLATE_AND_ANALYZE:
                state = RunState.FINISHED;
                break;
        }
        return waitForRunState(runId, state, interval);
    }


    private PcRunResponse waitForRunState(int runId, RunState completionState, int interval) throws InterruptedException,
            ClientProtocolException, PcException, IOException {

        int counter = 0;
        RunState[] states = {RunState.BEFORE_COLLATING_RESULTS,RunState.BEFORE_CREATING_ANALYSIS_DATA};
        PcRunResponse response = null;
        RunState lastState = RunState.UNDEFINED;
        do {
            response = restProxy.getRunData(runId);
            RunState currentState = RunState.get(response.getRunState());
            if (lastState.ordinal() < currentState.ordinal()) {
                lastState = currentState;
                logger.println(String.format("RunID: %s - State = %s", runId, currentState.value()));
            }

            // In case we are in state before collate or before analyze, we will wait 1 minute for the state to change otherwise we exit
            // because the user probably stopped the run from PC or timeslot has reached the end.
            if (Arrays.asList(states).contains(currentState)){
                counter++;
                Thread.sleep(1000);
                if(counter > 60 ){
                    logger.println(String.format("RunID: %s  - Stopped from Performance Center side with state = %s", runId, currentState.value()));
                    break;
                }
            }else{
                counter = 0;
                Thread.sleep(interval);
            }
        } while (lastState.ordinal() < completionState.ordinal());
        return response;
    }

    public FilePath publishRunReport(int runId, String reportDirectory) throws IOException, PcException, InterruptedException {
        PcRunResults runResultsList = restProxy.getRunResults(runId);
        if (runResultsList.getResultsList() != null){
            for (PcRunResult result : runResultsList.getResultsList()) {
                if (result.getName().equals(PcBuilder.pcReportArchiveName)) {
                    File dir = new File(reportDirectory);
                    dir.mkdirs();
                    String reportArchiveFullPath = dir.getCanonicalPath() + IOUtils.DIR_SEPARATOR + PcBuilder.pcReportArchiveName;
                    logger.println("Publishing analysis report");
                    restProxy.GetRunResultData(runId, result.getID(), reportArchiveFullPath);
                    FilePath fp = new FilePath(new File(reportArchiveFullPath));
                    fp.unzip(fp.getParent());
                    fp.delete();
                    FilePath reportFile = fp.sibling(PcBuilder.pcReportFileName);
                    if (reportFile.exists())
                        return reportFile;
                }
            }
        }
        logger.println("Failed to get run report");
        return null;
    }

    public boolean logout() {
        if (!loggedIn)
            return true;

        boolean logoutSucceeded = false;
        try {
            logoutSucceeded = restProxy.logout();
            loggedIn = !logoutSucceeded;
        } catch (PcException e) {
            logger.println(e.getMessage());
        } catch (Exception e) {
            logger.println(e);
        }
        logger.println(String.format("Logout %s", logoutSucceeded ? "succeeded" : "failed"));
        return logoutSucceeded;
    }

    public boolean stopRun(int runId) {
        boolean stopRunSucceeded = false;
        try {
            logger.println("Stopping run");
            stopRunSucceeded = restProxy.stopRun(runId, "stop");
        } catch (PcException e) {
            logger.println(e.getMessage());
        } catch (Exception e) {
            logger.println(e);
        }
        logger.println(String.format("Stop run %s", stopRunSucceeded ? "succeeded" : "failed"));
        return stopRunSucceeded;
    }

    public PcRunEventLog getRunEventLog(int runId){
        try {
            return restProxy.getRunEventLog(runId);
        } catch (PcException e) {
            logger.println(e.getMessage());
        } catch (Exception e) {
            logger.println(e);
        }
        return null;
    }

    public void addRunToTrendReport(int runId, String trendReportId)
    {

        TrendReportRequest trRequest = new TrendReportRequest(model.getAlmProject(true), runId, null);
        logger.println("Adding run: " + runId + " to trend report: " + trendReportId);
        try {
            restProxy.updateTrendReport(trendReportId, trRequest);
            logger.println("Publishing run: " + runId + " on trend report: " + trendReportId);
        }
        catch (PcException e) {
            logger.println("Failed to add run to trend report: " + e.getMessage());
        }
        catch (IOException e) {
            logger.println("Failed to add run to trend report: Problem connecting to PC Server");
        }
    }

    public void waitForRunToPublishOnTrendReport(int runId, String trendReportId) throws PcException,IOException,InterruptedException{

        ArrayList<PcTrendedRun> trendReportMetaDataResultsList;
        boolean publishEnded = false;
        int counter = 0;

        do {
            trendReportMetaDataResultsList = restProxy.getTrendReportMetaData(trendReportId);

            if (trendReportMetaDataResultsList.isEmpty())  break;

            for (PcTrendedRun result : trendReportMetaDataResultsList) {

                if (result.getRunID() != runId) continue;

                if (result.getState().equals(PcBuilder.TRENDED) || result.getState().equals(PcBuilder.ERROR)){
                    publishEnded = true;
                    logger.println("Run: " + runId + " publishing status: "+ result.getState());
                    break;
                }else{
                    Thread.sleep(5000);
                    counter++;
                    if(counter >= 120){
                        String msg = "Error: Publishing didn't ended after 10 minutes, aborting...";
                        throw new PcException(msg);
                    }
                }
             }

        }while (!publishEnded && counter < 120);
    }

    public boolean downloadTrendReportAsPdf(String trendReportId, String directory) throws PcException {


        try {
            logger.println("Downloading trend report: " + trendReportId + " in PDF format");
            InputStream in = restProxy.getTrendingPDF(trendReportId);
            File dir = new File(directory);
            if(!dir.exists()){
                dir.mkdirs();
            }
            String filePath = directory + IOUtils.DIR_SEPARATOR + "trendReport" + trendReportId + ".pdf";
            Path destination = Paths.get(filePath);
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            logger.println("Trend report: " + trendReportId + " was successfully downloaded");
        }
        catch (Exception e) {

            logger.println("Failed to download trend report: " + e.getMessage());
            throw new PcException(e.getMessage());
        }

        return true;

    }

    public void publishTrendReport(String filePath, String trendReportId){

        if (filePath == null){return;}
   //     return String.format( HyperlinkNote.encodeTo(filePath, "View trend report " + trendReportId));
        logger.println( HyperlinkNote.encodeTo(filePath, "View trend report " + trendReportId));

    }


    // This method will return a map with the following structure: <transaction_name:selected_measurement_value>
    // for example:
    // <Action_Transaction:0.001>
    // <Virtual transaction 2:0.51>
    // This function uses reflection since we know only at runtime which transactions data will be reposed from the rest request.
    public Map<String, String>  getTrendReportByXML(String trendReportId, int runId, TrendReportTypes.DataType dataType, TrendReportTypes.PctType pctType,TrendReportTypes.Measurement measurement) throws IOException, PcException, IntrospectionException, NoSuchMethodException {

        Map<String, String> measurmentsMap = new LinkedHashMap<String, String>();
        measurmentsMap.put("RunId","_" + runId + "_");
        measurmentsMap.put("Trend Measurement Type",measurement.toString() + "_" + pctType.toString());



            TrendReportTransactionDataRoot res = restProxy.getTrendReportByXML(trendReportId, runId);

//            java.lang.reflect.Method rootMethod =  res.getClass().getMethod("getTrendReport" + dataType.toString() + "DataRowsList");
//            ArrayList<Object> RowsListObj = (ArrayList<Object>) rootMethod.invoke(res);
//            RowsListObj.get(0);

            List<Object> RowsListObj = res.getTrendReportRoot();

            for (int i=0; i< RowsListObj.size();i++){
                try {

                    java.lang.reflect.Method rowListMethod = RowsListObj.get(i).getClass().getMethod("getTrendReport" + dataType.toString() + "DataRowList");

                for ( Object DataRowObj : (ArrayList<Object>)rowListMethod.invoke(RowsListObj.get(i)))
                {
                    if (DataRowObj.getClass().getMethod("getPCT_TYPE").invoke(DataRowObj).equals(pctType.toString()))
                    {
                        java.lang.reflect.Method method;
                        method = DataRowObj.getClass().getMethod("get" + measurement.toString());
                        measurmentsMap.put(DataRowObj.getClass().getMethod("getPCT_NAME").invoke(DataRowObj).toString(),method.invoke(DataRowObj)==null?"":method.invoke(DataRowObj).toString());
                    }
                }
                }catch (NoSuchMethodException e){
                  //  logger.println("No such method exception: " + e);
                }
                catch (Exception e){
                    logger.println("Error on getTrendReportByXML: " + e);
                }
            }




          //  logger.print(res);


        return measurmentsMap;


    }

}
