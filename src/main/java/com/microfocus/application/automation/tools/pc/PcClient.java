/*
 * Certain versions of software accessible here may contain branding from Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 * This software was acquired by Micro Focus on September 1, 2017, and is now offered by OpenText.
 * Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright 2012-2024 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ___________________________________________________________________
 */


/*
 *  Implements the main method of loadtest
 *
 * */

package com.microfocus.application.automation.tools.pc;

import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.microfocus.adm.performancecenter.plugins.common.pcentities.*;
import com.microfocus.adm.performancecenter.plugins.common.rest.PcRestProxy;
import com.microfocus.application.automation.tools.pc.helper.DateFormatter;
import com.microfocus.application.automation.tools.run.PcBuilder;
import hudson.FilePath;
import hudson.console.HyperlinkNote;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;

import java.beans.IntrospectionException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PcClient {

    private PcModel model;
    private PcRestProxy restProxy;
    private boolean loggedIn;
    private PrintStream logger;
    private DateFormatter dateFormatter = new DateFormatter("");

    public PcClient(PcModel pcModel, PrintStream logger) {
        try {
            model = pcModel;
            String credentialsProxyId = model.getCredentialsProxyId(true);
            UsernamePasswordCredentials usernamePCPasswordCredentialsForProxy = PcBuilder.getCredentialsId(credentialsProxyId);
            String proxyOutUser = (usernamePCPasswordCredentialsForProxy == null || model.getProxyOutURL(true).isEmpty()) ? "" : usernamePCPasswordCredentialsForProxy.getUsername();
            String proxyOutPassword = (usernamePCPasswordCredentialsForProxy == null || model.getProxyOutURL(true).isEmpty()) ? "" : usernamePCPasswordCredentialsForProxy.getPassword().getPlainText();
            if (model.getProxyOutURL(true) != null && !model.getProxyOutURL(true).isEmpty()) {
                logger.println(String.format("%s - %s: %s", dateFormatter.getDate(), Messages.UsingProxy(), model.getProxyOutURL(true)));
                if (!proxyOutUser.isEmpty()) {
                    if (model.getCredentialsProxyId().startsWith("$"))
                        logger.println(String.format("%s - %s  %s.", dateFormatter.getDate(), Messages.UsingProxyCredentialsBuildParameters(), proxyOutUser));
                    else
                        logger.println(String.format("%s - %s %s.", dateFormatter.getDate(), Messages.UsingProxyCredentialsConfiguration(), proxyOutUser));
                }
            }
            restProxy = new PcRestProxy(model.isHTTPSProtocol(), model.getPcServerName(true), model.isAuthenticateWithToken(), model.getAlmDomain(true), model.getAlmProject(true), model.getProxyOutURL(true), proxyOutUser, proxyOutPassword);
            this.logger = logger;
        } catch (PcException e) {
            logger.println(String.format("%s - %s", dateFormatter.getDate(), e.getMessage()));
        }

    }

    public <T extends PcRestProxy> PcClient(PcModel pcModel, PrintStream logger, T proxy) {
        model = pcModel;
        restProxy = proxy;
        this.logger = logger;
    }

    public boolean login() {
        try {
            String credentialsId = model.getCredentialsId(true);
            UsernamePasswordCredentials usernamePCPasswordCredentials = PcBuilder.getCredentialsId(credentialsId);
            if (usernamePCPasswordCredentials != null) {
                if (model.getCredentialsId().startsWith("$"))
                    logger.println(String.format("%s - %s", dateFormatter.getDate(), Messages.UsingPCCredentialsBuildParameters()));
                else
                    logger.println(String.format("%s - %s", dateFormatter.getDate(), Messages.UsingPCCredentialsConfiguration()));
                logger.println(String.format("%s - %s\n[LRE Server='%s://%s/loadtest/%s', %s='%s']", dateFormatter.getDate(), Messages.TryingToLogin(), model.isHTTPSProtocol(), restProxy.GetPcServer(), restProxy.GetTenant(), model.isAuthenticateWithToken() ? "ClientIdKey" : "User", usernamePCPasswordCredentials.getUsername()));
                loggedIn = restProxy.authenticate(usernamePCPasswordCredentials.getUsername(), usernamePCPasswordCredentials.getPassword().getPlainText());
            } else {
                logger.println(String.format("%s - %s\n[LRE Server='%s://%s/loadtest/%s', %s='%s']", dateFormatter.getDate(), Messages.TryingToLogin(), model.isHTTPSProtocol(), restProxy.GetPcServer(), restProxy.GetTenant(), model.isAuthenticateWithToken() ? "ClientIdKey" : "User", PcBuilder.usernamePCPasswordCredentials.getUsername()));
                loggedIn = restProxy.authenticate(PcBuilder.usernamePCPasswordCredentials.getUsername(), PcBuilder.usernamePCPasswordCredentials.getPassword().getPlainText());
            }
        } catch (PcException e) {
            logger.println(String.format("%s - %s", dateFormatter.getDate(), e.getMessage()));
        } catch (Exception e) {
            logger.println(String.format("%s - %s", dateFormatter.getDate(), e));
        }
        logger.println(String.format("%s - %s", dateFormatter.getDate(), loggedIn ? Messages.LoginSucceeded() : Messages.LoginFailed()));
        return loggedIn;
    }

    public boolean isLoggedIn() {

        return loggedIn;
    }

    public int startRun() throws NumberFormatException, ClientProtocolException, PcException, IOException {


        int testID = Integer.parseInt(model.getTestId(true));
        int testInstance = getCorrectTestInstanceID(testID);
        setCorrectTrendReportID();

        logger.println(String.format("%s - \n" +
                        "%s \n" +
                        "====================\n" +
                        "%s: %s \n" +
                        "%s: %s \n" +
                        "%s: %s \n" +
                        "%s: %s \n" +
                        "%s: %s \n" +
                        "%s: %s \n" +
                        "%s: %s \n" +
                        "====================\n",
                dateFormatter.getDate(),
                Messages.ExecutingLoadTest(),
                Messages.Domain(), model.getAlmDomain(true),
                Messages.Project(), model.getAlmProject(true),
                Messages.TestID(), Integer.parseInt(model.getTestId(true)),
                Messages.TestInstanceID(), testInstance,
                Messages.TimeslotDuration(),
                new TimeslotDuration(model.getTimeslotDurationHours(true), model.getTimeslotDurationMinutes(true)),
                Messages.PostRunAction(), model.getPostRunAction().getValue(),
                Messages.UseVUDS(), model.isVudsMode()));
        PcRunResponse response = null;
        try {
            response = restProxy.startRun(testID,
                    testInstance,
                    new TimeslotDuration(model.getTimeslotDurationHours(true),model.getTimeslotDurationMinutes(true)),
                    model.getPostRunAction().getValue(),
                    model.isVudsMode(),
                    0);
            logger.println(String.format("%s - %s (TestID: %s, RunID: %s, TimeslotID: %s)", dateFormatter.getDate(), Messages.RunStarted(),
                    response.getTestID(), response.getID(), response.getTimeslotID()));
            return response.getID();
        } catch (NumberFormatException | ClientProtocolException | PcException ex) {
            Integer result = checkError1310(ex.getMessage());
            if (result != null) {
                return result;
            } else {
                logger.println(String.format("%s - %s. Error: %s", dateFormatter.getDate(), Messages.StartRunFailed(), ex.getMessage()));
            }
        } catch (IOException ex) {
            logger.println(String.format("%s - %s. IOException Error: %s", dateFormatter.getDate(), Messages.StartRunFailed(), ex.getMessage()));
        }
        if (!("RETRY".equals(model.getRetry()))) {
            return 0;
        } else {
            //counter
            int retryCount = 0;
            //values
            int retryDelay = Integer.parseInt(model.getRetryDelay());
            int retryOccurrences = Integer.parseInt(model.getRetryOccurrences());

            while (retryCount <= retryOccurrences) {
                retryCount++;
                try {
                    if (retryCount <= retryOccurrences) {
                        logger.println(String.format("%s - %s. %s (%s %s). %s: %s.",
                                dateFormatter.getDate(),
                                Messages.StartRunRetryFailed(),
                                Messages.AttemptingStartAgainSoon(),
                                retryDelay,
                                Messages.Minutes(),
                                Messages.AttemptsRemaining(),
                                retryOccurrences - retryCount + 1));
                        Thread.sleep(retryDelay * 60 * 1000);
                    }
                } catch (InterruptedException ex) {
                    logger.println(String.format("%s - wait failed", dateFormatter.getDate()));
                }

                try {
                    response = restProxy.startRun(testID,
                            testInstance,
                            new TimeslotDuration(model.getTimeslotDurationHours(true) ,model.getTimeslotDurationMinutes(true)),
                            model.getPostRunAction().getValue(),
                            model.isVudsMode(),
                            0);
                } catch (NumberFormatException | ClientProtocolException | PcException ex) {
                    Integer result = checkError1310(ex.getMessage());
                    if (result != null) {
                        return result;
                    } else {
                        logger.println(String.format("%s -%s. %s: %s",
                                dateFormatter.getDate(),
                                Messages.StartRunRetryFailed(),
                                Messages.Error(),
                                ex.getMessage()));
                    }
                } catch (IOException ex) {
                    logger.println(String.format("%s -%s. %s: %s",
                            dateFormatter.getDate(),
                            Messages.StartRunRetryFailed(),
                            Messages.Error(),
                            ex.getMessage()));
                }
                int ret = 0;
                if (response != null) {
                    try {
                        ret = response.getID();
                    } catch (Exception ex) {
                        logger.println(String.format("%s - %s. %s: %s",
                                dateFormatter.getDate(),
                                Messages.RetrievingIDFailed(),
                                Messages.Error(),
                                ex.getMessage()));
                    }
                }
                if (ret != 0) {
                    logger.println(String.format("%s - %s (TestID: %s, RunID: %s, TimeslotID: %s)\n",
                            dateFormatter.getDate(),
                            Messages.RunStarted(),
                            response.getTestID(),
                            response.getID(),
                            response.getTimeslotID()));
                    return ret;
                }
            }
        }
        return 0;
    }

    private Integer checkError1310(String msg) {
        Pattern p = Pattern.compile("executeRequest exception: Run was started with ID (\\d+), but.*Error code: 1310");
        Matcher m = p.matcher(msg);
        if (m.matches()) {
            logger.println(String.format("%s - %s. Recovered-error: %s", dateFormatter.getDate(), Messages.StartRunFailed(), msg));
            logger.println(String.format("%s - %s (TestID: %s, RunID: %s, TimeslotID: %s)", dateFormatter.getDate(), Messages.RunStarted(),
                    Integer.parseInt(model.getTestId(true)), m.group(1), "0"));
            return Integer.parseInt(m.group(1));
        } else {
            return null;
        }
    }

    private int getCorrectTestInstanceID(int testID) throws IOException, PcException {
        if ("AUTO".equals(model.getAutoTestInstanceID())) {
            try {
                logger.println(String.format("%s - %s.",
                        dateFormatter.getDate(),
                        Messages.SearchingTestInstance()));
                PcTestInstances pcTestInstances = null;
                try {
                    pcTestInstances = restProxy.getTestInstancesByTestId(testID);
                } catch (PcException ex) {
                    logger.println(String.format("%s - getTestInstancesByTestId %s. %s: %s",
                            dateFormatter.getDate(),
                            Messages.Failure(),
                            Messages.Error(),
                            ex.getMessage()));
                }

                int testInstanceID;
                if (pcTestInstances != null && pcTestInstances.getTestInstancesList() != null) {
                    PcTestInstance pcTestInstance = pcTestInstances.getTestInstancesList().get(pcTestInstances.getTestInstancesList().size() - 1);
                    testInstanceID = pcTestInstance.getInstanceId();
                    logger.println(String.format("%s - %s: %s",
                            dateFormatter.getDate(),
                            Messages.FoundTestInstanceID(),
                            testInstanceID));
                } else {
                    logger.println(String.format("%s - %s",
                            dateFormatter.getDate(),
                            Messages.NotFoundTestInstanceID()));
                    logger.println(String.format("%s - %s",
                            dateFormatter.getDate(),
                            Messages.SearchingAvailableTestSet()));
                    // Get a random TestSet
                    PcTestSets pcTestSets = restProxy.GetAllTestSets();
                    if (pcTestSets != null && pcTestSets.getPcTestSetsList() != null) {
                        PcTestSet pcTestSet = pcTestSets.getPcTestSetsList().get(pcTestSets.getPcTestSetsList().size() - 1);
                        int testSetID = pcTestSet.getTestSetID();
                        logger.println(String.format("%s - %s (testID: %s, TestSetID: %s)",
                                dateFormatter.getDate(),
                                Messages.CreatingNewTestInstance(),
                                testID,
                                testSetID));
                        testInstanceID = restProxy.createTestInstance(testID, testSetID);
                        logger.println(String.format("%s - %s: %s",
                                dateFormatter.getDate(),
                                Messages.TestInstanceCreatedSuccessfully(),
                                testInstanceID));
                    } else {
                        String msg = Messages.NoTestSetAvailable();
                        logger.println(String.format("%s - %s",
                                dateFormatter.getDate(),
                                msg));
                        throw new PcException(msg);
                    }
                }
                return testInstanceID;
            } catch (Exception e) {
                logger.println(String.format("%s - getCorrectTestInstanceID %s. %s: %s",
                        dateFormatter.getDate(),
                        Messages.Failure(),
                        Messages.Error(),
                        e.getMessage()));
                return Integer.parseInt(null);
            }
        }
        return Integer.parseInt(model.getTestInstanceId(true));
    }

    private void setCorrectTrendReportID() throws IOException, PcException {
        // If the user selected "Use trend report associated with the test" we want the report ID to be the one from the test
        String msg = Messages.NoTrendReportAssociated() + "\n" +
                Messages.PleaseTurnAutomaticTrendOn() + "\n" +
                Messages.PleaseTurnAutomaticTrendOnAlternative();
        if (("ASSOCIATED").equals(model.getAddRunToTrendReport()) && model.getPostRunAction() != PostRunAction.DO_NOTHING) {
            PcTest pcTest = restProxy.getTestData(Integer.parseInt(model.getTestId(true)));
            //if the trend report ID is parametrized
            if (!model.getTrendReportId().startsWith("$")) {
                if (pcTest.getTrendReportId() > -1)
                    model.setTrendReportId(String.valueOf(pcTest.getTrendReportId()));
                else {
                    throw new PcException(msg);
                }
            } else {
                try {
                    if (Integer.parseInt(model.getTrendReportId(true)) > -1)
                        model.setTrendReportId(String.valueOf(model.getTrendReportId(true)));
                    else {
                        throw new PcException(msg);
                    }
                } catch (Exception ex) {
                    throw new PcException(msg + System.getProperty("line.separator") + ex);
                }
            }
        }
    }

    public String getTestName() throws IOException, PcException {

        try {
            PcTest pcTest = restProxy.getTestData(Integer.parseInt(model.getTestId(true)));
            return pcTest.getTestName();
        } catch (IOException | PcException ex) {
            logger.println(String.format("%s - getTestData %s (testId : %s)", dateFormatter.getDate(), Messages.Failure(), model.getTestId(true)));
            throw ex;
        }
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
        RunState[] states = {RunState.BEFORE_COLLATING_RESULTS, RunState.BEFORE_CREATING_ANALYSIS_DATA};
        PcRunResponse response = null;
        RunState lastState = RunState.UNDEFINED;
        int threeStrikes = 3;
        do {
            try {

                if (threeStrikes < 3) {
                    logger.println(String.format("%s - Cannot get response from LRE about the state of the Run (ID=%s) %s time(s) consecutively",
                            dateFormatter.getDate(),
                            runId,
                            (3 - threeStrikes)));
                    if (threeStrikes == 0) {
                        logger.println(String.format("%s - %s: %s",
                                dateFormatter.getDate(),
                                Messages.StoppingMonitoringOnRun(),
                                runId));
                        break;
                    }
                    Thread.sleep(2000);
                    login();
                }
                response = restProxy.getRunData(runId);
                RunState currentState = RunState.get(response.getRunState());
                if (lastState.ordinal() < currentState.ordinal()) {
                    lastState = currentState;
                    logger.println(String.format("%s - RunID: %s - State = %s",
                            dateFormatter.getDate(),
                            runId,
                            currentState.value()));
                }

                // In case we are in state before collate or before analyze, we will wait 1 minute for the state to change otherwise we exit
                // because the user probably stopped the run from LRE or timeslot has reached the end.
                if (Arrays.asList(states).contains(currentState)) {
                    counter++;
                    Thread.sleep(1000);
                    if (counter > 60) {
                        logger.println(String.format("%s - Run ID: %s  - %s = %s",
                                dateFormatter.getDate(),
                                runId,
                                Messages.StoppedFromPC(),
                                currentState.value()));
                        break;
                    }
                } else {
                    counter = 0;
                    Thread.sleep(interval);
                }
                threeStrikes = 3;
            } catch (InterruptedException e) {
                throw e;
            } catch (PcException e) {
                threeStrikes--;
            }
        } while (lastState.ordinal() < completionState.ordinal());
        return response;
    }

    public FilePath publishRunReport(int runId, String reportDirectory) throws IOException, PcException, InterruptedException {
        PcRunResults runResultsList = restProxy.getRunResults(runId);
        if (runResultsList.getResultsList() != null) {
            for (PcRunResult result : runResultsList.getResultsList()) {
                if (result.getName().equals(PcBuilder.pcReportArchiveName)) {
                    File dir = new File(reportDirectory);
                    dir.mkdirs();
                    String reportArchiveFullPath = dir.getCanonicalPath() + IOUtils.DIR_SEPARATOR + PcBuilder.pcReportArchiveName;
                    logger.println(String.format("%s - %s", dateFormatter.getDate(), Messages.PublishingAnalysisReport()));
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
        logger.println(String.format("%s - %s", dateFormatter.getDate(), Messages.FailedToGetRunReport()));
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
            logger.println(String.format("%s - %s",
                    dateFormatter.getDate(),
                    e.getMessage()));
        } catch (Exception e) {
            logger.println(e);
        }
        logger.println(String.format("%s - %s",
                dateFormatter.getDate(),
                logoutSucceeded ? Messages.LogoutSucceeded() : Messages.LogoutFailed()));
        return logoutSucceeded;
    }

    public boolean stopRun(int runId) {
        boolean stopRunSucceeded = false;
        try {
            logger.println(String.format("%s - %s", dateFormatter.getDate(), Messages.StoppingRun()));
            stopRunSucceeded = restProxy.stopRun(runId, "stop");
        } catch (PcException e) {
            logger.println(String.format("%s - %s", dateFormatter.getDate(), e.getMessage()));
        } catch (Exception e) {
            logger.println(String.format("%s - %s", dateFormatter.getDate(), e));
        }
        logger.println(String.format("%s - %s", dateFormatter.getDate(), stopRunSucceeded ? Messages.StopRunSucceeded() : Messages.StopRunFailed()));
        return stopRunSucceeded;
    }

    public PcRunEventLog getRunEventLog(int runId) {
        try {
            return restProxy.getRunEventLog(runId);
        } catch (PcException e) {
            logger.println(String.format("%s - %s",
                    dateFormatter.getDate(),
                    e.getMessage()));
        } catch (Exception e) {
            logger.println(String.format("%s - %s",
                    dateFormatter.getDate(),
                    e.getMessage()));
        }
        return null;
    }

    public void addRunToTrendReport(int runId, String trendReportId) {

        TrendReportRequest trRequest = new TrendReportRequest(model.getAlmProject(true), runId, null);
        logger.println(String.format("%s - Adding run: %s to trend report: %s",
                dateFormatter.getDate(),
                runId,
                trendReportId));
        try {
            restProxy.updateTrendReport(trendReportId, trRequest);
            logger.println(String.format("%s - %s: %s %s: %s",
                    dateFormatter.getDate(),
                    Messages.PublishingRun(),
                    runId,
                    Messages.OnTrendReport(),
                    trendReportId));
        } catch (PcException e) {
            logger.println(String.format("%s - %s: %s",
                    dateFormatter.getDate(),
                    Messages.FailedToAddRunToTrendReport(),
                    e.getMessage()));
        } catch (IOException e) {
            logger.println(String.format("%s - %s: %s.",
                    dateFormatter.getDate(),
                    Messages.FailedToAddRunToTrendReport(),
                    Messages.ProblemConnectingToPCServer()));
        }
    }

    public void waitForRunToPublishOnTrendReport(int runId, String trendReportId) throws PcException, IOException, InterruptedException {

        ArrayList<PcTrendedRun> trendReportMetaDataResultsList;
        boolean publishEnded = false;
        int counterPublishStarted = 0;
        int counterPublishNotStarted = 0;
        boolean resultNotFound = true;

        do {
            trendReportMetaDataResultsList = restProxy.getTrendReportMetaData(trendReportId);

            if (trendReportMetaDataResultsList.isEmpty()) break;

            for (PcTrendedRun result : trendReportMetaDataResultsList) {
                resultNotFound = result.getRunID() != runId;
                if (resultNotFound) continue;

                if (result.getState().equals(PcBuilder.TRENDED) || result.getState().equals(PcBuilder.ERROR)) {
                    publishEnded = true;
                    logger.println(String.format("%s - Run: %s %s: %s",
                            dateFormatter.getDate(),
                            runId,
                            Messages.PublishingStatus(),
                            result.getState()));
                    break;
                } else {
                    Thread.sleep(5000);
                    counterPublishStarted++;
                    if (counterPublishStarted >= 360) { //waiting 30 minutes for timeout
                        String msg = String.format("%s: %s",
                                Messages.Error(),
                                Messages.PublishingEndTimeout());
                        throw new PcException(msg);
                    }
                }
            }
            if (!publishEnded && resultNotFound) {
                Thread.sleep(5000);
                counterPublishNotStarted++;
                if (counterPublishNotStarted >= 180) { //waiting 15 minutes for timeout
                    String msg = String.format("%s",
                            Messages.PublishingStartTimeout());
                    throw new PcException(msg);
                } else if (counterPublishNotStarted % 12 == 0) { //warning every minute until timeout
                    logger.println(String.format("%s - %s. %s: %s ... ",
                            dateFormatter.getDate(),
                            Messages.WaitingForTrendReportToStart(),
                            Messages.MinutesUntilTimeout(),
                            10 - (counterPublishNotStarted / 12)
                    ));
                }
            }
        } while (!publishEnded && counterPublishStarted < 120 && counterPublishNotStarted < 120);
    }

    public boolean downloadTrendReportAsPdf(String trendReportId, String directory) throws PcException {


        try {
            logger.println(String.format("%s - %s: %s %s",
                    dateFormatter.getDate(),
                    Messages.DownloadingTrendReport(),
                    trendReportId,
                    Messages.InPDFFormat()));
            InputStream in = restProxy.getTrendingPDF(trendReportId);
            File dir = new File(directory);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String filePath = directory + IOUtils.DIR_SEPARATOR + "trendReport" + trendReportId + ".pdf";
            Path destination = Paths.get(filePath);
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            logger.println(String.format("%s - %s: %s %s",
                    dateFormatter.getDate(),
                    Messages.TrendReport(),
                    trendReportId,
                    Messages.SuccessfullyDownloaded()));
        } catch (Exception e) {

            logger.println(String.format("%s - %s: %s",
                    dateFormatter.getDate(),
                    Messages.FailedToDownloadTrendReport(),
                    e.getMessage()));
            throw new PcException(e.getMessage());
        }

        return true;

    }

    public void publishTrendReport(String filePath, String trendReportId) {

        if (filePath == null) {
            return;
        }
        //     return String.format( HyperlinkNote.encodeTo(filePath, "View trend report " + trendReportId));
        logger.println(String.format("%s - %s",
                dateFormatter.getDate(),
                HyperlinkNote.encodeTo(filePath, Messages.ViewTrendReport() + " " + trendReportId)));

    }


    // This method will return a map with the following structure: <transaction_name:selected_measurement_value>
    // for example:
    // <Action_Transaction:0.001>
    // <Virtual transaction 2:0.51>
    // This function uses reflection since we know only at runtime which transactions data will be reposed from the rest request.
    public Map<String, String> getTrendReportByXML(String trendReportId, int runId, TrendReportTypes.DataType dataType, TrendReportTypes.PctType pctType, TrendReportTypes.Measurement measurement) throws IOException, PcException, IntrospectionException, NoSuchMethodException {

        Map<String, String> measurmentsMap = new LinkedHashMap<String, String>();
        measurmentsMap.put("RunId", "_" + runId + "_");
        measurmentsMap.put("Trend Measurement Type", measurement.toString() + "_" + pctType.toString());


        TrendReportTransactionDataRoot res = restProxy.getTrendReportByXML(trendReportId, runId);

//            java.lang.reflect.Method rootMethod =  res.getClass().getMethod("getTrendReport" + dataType.toString() + "DataRowsList");
//            ArrayList<Object> RowsListObj = (ArrayList<Object>) rootMethod.invoke(res);
//            RowsListObj.get(0);

        List<Object> RowsListObj = res.getTrendReportRoot();

        for (int i = 0; i < RowsListObj.size(); i++) {
            try {

                java.lang.reflect.Method rowListMethod = RowsListObj.get(i).getClass().getMethod("getTrendReport" + dataType.toString() + "DataRowList");

                for (Object DataRowObj : (ArrayList<Object>) rowListMethod.invoke(RowsListObj.get(i))) {
                    if (DataRowObj.getClass().getMethod("getPCT_TYPE").invoke(DataRowObj).equals(pctType.toString())) {
                        java.lang.reflect.Method method;
                        method = DataRowObj.getClass().getMethod("get" + measurement.toString());
                        measurmentsMap.put(DataRowObj.getClass().getMethod("getPCT_NAME").invoke(DataRowObj).toString(), method.invoke(DataRowObj) == null ? "" : method.invoke(DataRowObj).toString());
                    }
                }
            } catch (NoSuchMethodException e) {
                //  logger.println("No such method exception: " + e);
            } catch (Exception e) {
                // logger.println(String.format("%s - Error on getTrendReportByXML: %s ", dateFormatter.getDate(), e));
            }
        }


        //  logger.print(res);


        return measurmentsMap;


    }

}
