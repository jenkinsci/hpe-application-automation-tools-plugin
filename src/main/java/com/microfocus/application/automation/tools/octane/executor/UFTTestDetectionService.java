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

package com.microfocus.application.automation.tools.octane.executor;

import com.hp.octane.integrations.CIPluginServices;
import com.hp.octane.integrations.OctaneClient;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.executor.impl.TestingToolType;
import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameterFactory;
import com.hp.octane.integrations.uft.UftTestDiscoveryUtils;
import com.hp.octane.integrations.uft.items.*;
import com.hp.octane.integrations.utils.SdkConstants;
import com.hp.octane.integrations.utils.SdkStringUtils;
import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Service is responsible to detect changes according to SCM change and to put it to queue of UftTestDiscoveryDispatcher
 */
public class UFTTestDetectionService {
    private static final Logger logger = SDKBasedLoggerProvider.getLogger(UFTTestDetectionService.class);
    private static final String INITIAL_DETECTION_FILE = "INITIAL_DETECTION_FILE.txt";
    private static final String DETECTION_RESULT_FILE = "detection_result.json";

    public static UftTestDiscoveryResult startScanning(File rootDir, BuildListener buildListener, String configurationId, String workspaceId, String scmRepositoryId,
                                                       String testRunnerId, UFTTestDetectionCallable.ScmChangesWrapper scmChangesWrapper, boolean fullScan, TestingToolType testingToolType) {
        UftTestDiscoveryResult result = null;
        try {

            boolean myFullScan = fullScan || !initialDetectionFileExist(rootDir);
            if (myFullScan) {
                printToConsole(buildListener, "Executing full sync");
                // only full scan flow is supported in MBT
                result = UftTestDiscoveryUtils.doFullDiscovery(rootDir, testingToolType);
            } else {
                printToConsole(buildListener, "Executing changeSet sync. For full sync - define in job boolean parameter 'Full sync' with value 'true'.");
                result = doChangeSetDetection(scmChangesWrapper, rootDir, testingToolType, configurationId);
                removeTestDuplicatedForUpdateTests(result);
                removeFalsePositiveDataTables(result, result.getDeletedTests(), result.getDeletedScmResourceFiles());
                removeFalsePositiveDataTables(result, result.getNewTests(), result.getNewScmResourceFiles());
            }

            printResults(buildListener, result);

            if (result.isHasQuotedPaths()) {
                printToConsole(buildListener, "This run may not have discovered all updated tests. \n" +
                        "It seems that the changes in this build included filenames with Unicode characters, which Git did not list correctly.\n" +
                        "To make sure Git can properly list such file names, configure Git as follows : git config --global core.quotepath false\n" +
                        "To discover the updated tests that were missed in this run and send them to ALM Octane, run this job manually with the \"Full sync\" parameter selected.\n");
            }

            result.setScmRepositoryId(scmRepositoryId);
            result.setConfigurationId(configurationId);
            result.setWorkspaceId(workspaceId);
            result.setFullScan(fullScan);

            //we add test runner only for discovery jobs that were created for test runners
            if (testRunnerId != null) {
                result.setTestRunnerId(testRunnerId);
            }

            result.sortItems();
            createInitialDetectionFile(rootDir);

        } catch (Exception e) {
            logger.error("Fail in startScanning : " + e.getMessage(), e);
        }

        return result;
    }

    private static void printResults(BuildListener buildListener, UftTestDiscoveryResult result) {
        if (TestingToolType.UFT.equals(result.getTestingToolType())) {
            // print tables
            printByStatus(buildListener, result.getAllTests(), "Found %s tests with status %s");
            // print data tables
            printByStatus(buildListener, result.getAllScmResourceFiles(), "Found %s data tables with status %s");
        } else {
            // flatten action lists
            List<UftTestAction> actions = result.getAllTests().stream()
                    .map(AutomatedTest::getActions)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
            // print actions
            printByStatus(buildListener, actions, "Found %s actions with status %s");

            // flatten parameters
            List<UftTestParameter> parameters = actions.stream()
                    .filter(action -> !action.getParameters().isEmpty())
                    .map(action -> action.getParameters())
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
            // print parameters
            printByStatus(buildListener, parameters, "Found %s parameters with status %s");
        }
    }

    private static void printByStatus(BuildListener buildListener, List<? extends SupportsOctaneStatus> entities, String messageTemplate) {
        Map<OctaneStatus, Integer> testStatusMap = computeStatusMap(entities);
        for (Map.Entry<OctaneStatus, Integer> entry : testStatusMap.entrySet()) {
            printToConsole(buildListener, String.format(messageTemplate, entry.getValue(), entry.getKey()));
        }
    }

    private static Map<OctaneStatus, Integer> computeStatusMap(List<? extends SupportsOctaneStatus> entities) {
        Map<OctaneStatus, Integer> statusMap = new HashMap<>();
        for (SupportsOctaneStatus item : entities) {
            if (!statusMap.containsKey(item.getOctaneStatus())) {
                statusMap.put(item.getOctaneStatus(), 0);
            }
            statusMap.put(item.getOctaneStatus(), statusMap.get(item.getOctaneStatus()) + 1);
        }
        return statusMap;
    }

    /**
     * Deleted data table might be part of deleted test. During discovery its very hard to know.
     * Here we pass through all deleted data tables, if we found data table parent is test folder - we know that the delete was part of test delete
     *
     * @param tests
     * @param scmResourceFiles
     */
    private static void removeFalsePositiveDataTables(UftTestDiscoveryResult result, List<AutomatedTest> tests, List<ScmResourceFile> scmResourceFiles) {
        if (!scmResourceFiles.isEmpty() && !tests.isEmpty()) {

            List<ScmResourceFile> falsePositive = new ArrayList<>();
            for (ScmResourceFile item : scmResourceFiles) {
                int parentSplitterIndex = item.getRelativePath().lastIndexOf(SdkConstants.FileSystem.WINDOWS_PATH_SPLITTER);
                if (parentSplitterIndex != -1) {
                    String parentName = item.getRelativePath().substring(0, parentSplitterIndex);
                    for (AutomatedTest test : tests) {
                        String testPath = SdkStringUtils.isEmpty(test.getPackage()) ? test.getName() : test.getPackage() + SdkConstants.FileSystem.WINDOWS_PATH_SPLITTER + test.getName();
                        if (parentName.contains(testPath)) {
                            falsePositive.add(item);
                            break;
                        }
                    }
                }
            }

            result.getAllScmResourceFiles().removeAll(falsePositive);
        }
    }

    private static void removeTestDuplicatedForUpdateTests(UftTestDiscoveryResult result) {
        Set<String> keys = new HashSet<>();
        List<AutomatedTest> testsToRemove = new ArrayList<>();
        for (AutomatedTest test : result.getUpdatedTests()) {
            String key = test.getPackage() + "_" + test.getName();
            if (keys.contains(key)) {
                testsToRemove.add(test);
            }
            keys.add(key);

        }
        result.getAllTests().removeAll(testsToRemove);
    }

    public static void printToConsole(BuildListener buildListener, String msg) {
        if (buildListener != null) {
            buildListener.getLogger().println("UFTTestDetectionService : " + msg);
        }
    }

    private static UftTestDiscoveryResult doChangeSetDetection(UFTTestDetectionCallable.ScmChangesWrapper scmChangesWrapper, File workspace, TestingToolType testingToolType,String configurationId) {
        UftTestDiscoveryResult result = new UftTestDiscoveryResult();
        result.setTestingToolType(testingToolType);
        scmChangesWrapper.getAffectedFiles().sort(Comparator.comparing(UFTTestDetectionCallable.ScmChangeAffectedFileWrapper::getPath));
        List<UFTTestDetectionCallable.ScmChangeAffectedFileWrapper> dataTableAffectFiles = new LinkedList<>();
        for (UFTTestDetectionCallable.ScmChangeAffectedFileWrapper affectedFileWrapper : scmChangesWrapper.getAffectedFiles()) {
            if (affectedFileWrapper.getPath().startsWith("\"")) {
                result.setHasQuotedPaths(true);
            }
            String affectedFileFullPath = workspace + File.separator + affectedFileWrapper.getPath();
            if (!affectedFileWrapper.isSvnDirType()) {
                if (UftTestDiscoveryUtils.isTestMainFilePath(affectedFileWrapper.getPath())) {
                    handleUftTestChanges(workspace, testingToolType, result, affectedFileWrapper, affectedFileFullPath);
                } else if (TestingToolType.UFT.equals(testingToolType) && UftTestDiscoveryUtils.isUftDataTableFile(affectedFileWrapper.getPath())) {
                    handleUftDataTableChanges(workspace, result, affectedFileWrapper, affectedFileFullPath, dataTableAffectFiles);
                } else if (TestingToolType.MBT.equals(testingToolType) && UftTestDiscoveryUtils.isUftActionFile(affectedFileWrapper.getPath())) {
                    handleUftActionChanges(workspace, result, affectedFileWrapper, affectedFileFullPath);
                }
            } else if (UFTTestDetectionCallable.ScmChangeEditTypeWrapper.DELETE.equals(affectedFileWrapper.getEditType())) { //isDir
                FilePath filePath = new FilePath(new File(affectedFileWrapper.getPath()));
                String deletedFolder = filePath.getRemote().replace(SdkConstants.FileSystem.LINUX_PATH_SPLITTER, SdkConstants.FileSystem.WINDOWS_PATH_SPLITTER);
                result.getDeletedFolders().add(deletedFolder);
            }
        }
        OctaneClient octaneClient = OctaneSDK.getClientByInstanceId(configurationId);
        if (ConfigurationParameterFactory.isUftTestsDeepRenameCheckEnabled(octaneClient.getConfigurationService().getConfiguration())) {
            createDataTableHashCodeToTestPath(dataTableAffectFiles, result);
        }
        return result;
    }

    private static void createDataTableHashCodeToTestPath( List<UFTTestDetectionCallable.ScmChangeAffectedFileWrapper> dataTableAffectFiles, UftTestDiscoveryResult result) {
        Map<String,List<String>> combineDataTableHashCodeToTests = new HashMap<>();
        for (AutomatedTest test : result.getAllTests()) {
            String testPath = test.getPackage() + "\\" + test.getName();
            String finalTestPath = testPath.replace("\\", "/");


            String allDataTableEffected = dataTableAffectFiles.stream().filter(dataTableAffectFile ->dataTableAffectFile.getPath().indexOf(finalTestPath) == 0)
                    .map(dataTableAffectFile -> dataTableAffectFile.getPath().substring(finalTestPath.length() + 1) + ":" + dataTableAffectFile.getGitDst())
                    .collect(Collectors.joining("-"));

            combineDataTableHashCodeToTests.computeIfAbsent(convertToHashCode(allDataTableEffected).toString(),k-> new LinkedList<>()).add(testPath);
        }
        result.setCombineDataTableHashCodeToTestPathListMap(combineDataTableHashCodeToTests);
    }



    private static StringBuilder convertToHashCode(String key) {
        StringBuilder sb = new StringBuilder();
        StringBuilder returnString = new StringBuilder();
        returnString.append(key);

        try {
            byte[] keyByteUTF8 = key.getBytes(StandardCharsets.UTF_8);
            MessageDigest md = MessageDigest.getInstance("SHA-1");

            md.update(keyByteUTF8, 0, keyByteUTF8.length);
            byte[] mdbytes = md.digest();

            //convert the byte to hex format
            for (int i = 0; i < mdbytes.length; i++) {
                sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
            }

        } catch (Exception e) {
            logger.error("failed to calculate hash code: "+ e.getMessage());
        }
        if (sb.length() > 0)
            return sb;
        else
            return returnString;
    }


    private static void handleUftTestChanges(File workspace,
                                             TestingToolType testingToolType,
                                             UftTestDiscoveryResult result,
                                             UFTTestDetectionCallable.ScmChangeAffectedFileWrapper affectedFileWrapper,
                                             String affectedFileFullPath) {

        File testFolder = UftTestDiscoveryUtils.getTestFolderForTestMainFile(affectedFileFullPath);
        File affectedFile = new File(affectedFileFullPath);
        boolean fileExist = affectedFile.exists();
        UftTestType uftTestType = UftTestDiscoveryUtils.getUftTestType(affectedFileWrapper.getPath());

        AutomatedTest test = UftTestDiscoveryUtils.createAutomatedTest(workspace, testFolder, uftTestType, testingToolType);
        test.setChangeSetSrc(affectedFileWrapper.getGitSrc());
        test.setChangeSetDst(affectedFileWrapper.getGitDst());

        if (UFTTestDetectionCallable.ScmChangeEditTypeWrapper.ADD.equals(affectedFileWrapper.getEditType())) {
            if (fileExist) { // uft and mbt behave the same
                result.getAllTests().add(test);
            }
        } else if (UFTTestDetectionCallable.ScmChangeEditTypeWrapper.DELETE.equals(affectedFileWrapper.getEditType())) {
            if (!fileExist) {
                test.setOctaneStatus(OctaneStatus.DELETED);
                test.setExecutable(false);
                result.getAllTests().add(test);
            }
        } else if (UFTTestDetectionCallable.ScmChangeEditTypeWrapper.EDIT.equals(affectedFileWrapper.getEditType())) {
            if (fileExist) {
                test.setOctaneStatus(OctaneStatus.MODIFIED);
                result.getAllTests().add(test);
            }
        }
    }

    private static void handleUftDataTableChanges(File workspace,
                                                  UftTestDiscoveryResult result,
                                                  UFTTestDetectionCallable.ScmChangeAffectedFileWrapper affectedFileWrapper,
                                                  String affectedFileFullPath,
                                                  List<UFTTestDetectionCallable.ScmChangeAffectedFileWrapper> dataTableAffectFiles) {
        dataTableAffectFiles.add(affectedFileWrapper);
        File affectedFile = new File(affectedFileFullPath);
        ScmResourceFile resourceFile = UftTestDiscoveryUtils.createDataTable(workspace, affectedFile);
        resourceFile.setChangeSetSrc(affectedFileWrapper.getGitSrc());
        resourceFile.setChangeSetDst(affectedFileWrapper.getGitDst());

        if (UFTTestDetectionCallable.ScmChangeEditTypeWrapper.ADD.equals(affectedFileWrapper.getEditType())) {
            UftTestType testType = UftTestDiscoveryUtils.isUftTestFolder(affectedFile.getParentFile().listFiles());
            if (testType.isNone()) {
                if (affectedFile.exists()) {
                    result.getAllScmResourceFiles().add(resourceFile);
                }
            }
        } else if (UFTTestDetectionCallable.ScmChangeEditTypeWrapper.DELETE.equals(affectedFileWrapper.getEditType())) {
            if (!affectedFile.exists()) {
                resourceFile.setOctaneStatus(OctaneStatus.DELETED);
                result.getAllScmResourceFiles().add(resourceFile);
            }
        }
    }

    private static void handleUftActionChanges(File workspace, UftTestDiscoveryResult result, UFTTestDetectionCallable.ScmChangeAffectedFileWrapper affectedFileWrapper, String affectedFileFullPath) {
        // currently do nothing. to be implemented
    }

    private static boolean initialDetectionFileExist(File rootFile) {
        try {
            File file = new File(rootFile, INITIAL_DETECTION_FILE);
            return file.exists();

        } catch (Exception e) {
            return false;
        }
    }

    private static void createInitialDetectionFile(File rootFile) {
        try {
            File file = new File(rootFile, INITIAL_DETECTION_FILE);
            logger.info("Initial detection file path : " + file.getPath());
            file.createNewFile();
        } catch (IOException e) {
            logger.error("Failed to createInitialDetectionFile : " + e.getMessage());
        }
    }

    /**
     * Serialize detectionResult to file in XML format
     *
     * @param run
     * @param taskListenerLog
     * @param detectionResult
     */
    public static void publishDetectionResults(Run run, TaskListener taskListenerLog, UftTestDiscoveryResult detectionResult) {

        File file = getDetectionResultFile(run);
        try {
            detectionResult.writeToFile(file);
        } catch (Exception e) {
            String msg = "Failed to persist detection results : " + e.getMessage();
            if (taskListenerLog != null) {
                taskListenerLog.error(msg);
            }
            logger.error(msg);
        }
    }

    public static UftTestDiscoveryResult readDetectionResults(Run run) {
        File file = getDetectionResultFile(run);

        try {
            return UftTestDiscoveryResult.readFromFile(file);
        } catch (IOException e) {
            logger.error("Failed to read detection results : " + e.getMessage());
            return null;
        }
    }

    public static File getDetectionResultFile(Run run) {
        return new File(run.getRootDir(), DETECTION_RESULT_FILE);
    }

}