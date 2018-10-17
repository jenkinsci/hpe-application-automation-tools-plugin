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

package com.microfocus.application.automation.tools.octane.executor;

import com.hp.octane.integrations.uft.UftTestDiscoveryUtils;
import com.hp.octane.integrations.uft.items.*;
import com.hp.octane.integrations.util.SdkConstants;
import com.hp.octane.integrations.util.SdkStringUtils;
import com.microfocus.application.automation.tools.octane.executor.scmmanager.ScmPluginFactory;
import com.microfocus.application.automation.tools.octane.executor.scmmanager.ScmPluginHandler;
import hudson.ExtensionList;
import hudson.FilePath;
import hudson.model.*;
import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;
import jenkins.model.Jenkins;
import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * Service is responsible to detect changes according to SCM change and to put it to queue of UftTestDiscoveryDispatcher
 */
public class UFTTestDetectionService {
    private static final Logger logger = LogManager.getLogger(UFTTestDetectionService.class);
    private static final String INITIAL_DETECTION_FILE = "INITIAL_DETECTION_FILE.txt";
    private static final String DETECTION_RESULT_FILE = "detection_result.xml";

    public static UftTestDiscoveryResult startScanning(AbstractBuild<?, ?> build, String workspaceId, String scmRepositoryId, BuildListener buildListener) {
        ChangeLogSet<? extends ChangeLogSet.Entry> changeSet = build.getChangeSet();
        Object[] changeSetItems = changeSet.getItems();
        UftTestDiscoveryResult result = null;

        try {

            boolean fullScan = build.getId().equals("1") || !initialDetectionFileExist(build.getWorkspace()) || isFullScan((build));
            if (fullScan) {
                printToConsole(buildListener, "Executing full sync");
                result = UftTestDiscoveryUtils.doFullDiscovery(new File(build.getWorkspace().getRemote()));
            } else {
                printToConsole(buildListener, "Executing changeSet sync");
                result = doChangeSetDetection(changeSetItems, new File(build.getWorkspace().getRemote()));
                removeTestDuplicatedForUpdateTests(result);
                removeFalsePositiveDataTables(result, result.getDeletedTests(), result.getDeletedScmResourceFiles());
                removeFalsePositiveDataTables(result, result.getNewTests(), result.getNewScmResourceFiles());
            }

            Map<OctaneStatus, Integer> testStatusMap = computeStatusMap(result.getAllTests());
            for (Map.Entry<OctaneStatus, Integer> entry : testStatusMap.entrySet()) {
                printToConsole(buildListener, String.format("Found %s tests with status %s", entry.getValue(), entry.getKey()));
            }

            Map<OctaneStatus, Integer> resourceFilesStatusMap = computeStatusMap(result.getAllScmResourceFiles());
            for (Map.Entry<OctaneStatus, Integer> entry : resourceFilesStatusMap.entrySet()) {
                printToConsole(buildListener, String.format("Found %s data tables with status %s", entry.getValue(), entry.getKey()));
            }

            if (!result.getDeletedFolders().isEmpty()) {
                printToConsole(buildListener, String.format("Found %s deleted folders", result.getDeletedFolders().size()));

                //This situation is relevant for SVN only.
                //Deleting folder - SCM event doesn't supply information about deleted items in deleted folder - only top-level directory.
                //In this case need to do for each deleted folder - need to check with Octane what tests and data tables were under this folder.
                //so for each deleted folder - need to do at least 2 requests. In this situation - decided to activate full sync as it already tested scenario.
                //Full sync wil be triggered with delay of 60 secs to give the dispatcher possibility to sync other found changes

                //triggering full sync
                printToConsole(buildListener, "To sync deleted items - full sync required. Triggering job with full sync parameter.");

                FreeStyleProject proj = (FreeStyleProject) build.getParent();
                List<ParameterValue> newParameters = new ArrayList<>();
                for (ParameterValue param : build.getAction(ParametersAction.class).getParameters()) {
                    ParameterValue paramForSet;
                    if (param.getName().equals(UftConstants.FULL_SCAN_PARAMETER_NAME)) {
                        paramForSet = new BooleanParameterValue(UftConstants.FULL_SCAN_PARAMETER_NAME, true);
                    } else {
                        paramForSet = param;
                    }
                    newParameters.add(paramForSet);
                }

                ParametersAction parameters = new ParametersAction(newParameters);
                CauseAction causeAction = new CauseAction(new FullSyncRequiredCause(build.getId()));
                proj.scheduleBuild2(60, parameters, causeAction);
            }

            if (result.isHasQuotedPaths()) {
                printToConsole(buildListener, "This run may not have discovered all updated tests. \n" +
                        "It seems that the changes in this build included filenames with Unicode characters, which Git did not list correctly.\n" +
                        "To make sure Git can properly list such file names, configure Git as follows : git config --global core.quotepath false\n" +
                        "To discover the updated tests that were missed in this run and send them to ALM Octane, run this job manually with the \"Full sync\" parameter selected.\n");
            }

            result.setScmRepositoryId(scmRepositoryId);
            result.setWorkspaceId(workspaceId);
            result.setFullScan(fullScan);
            result.sortItems();
            publishDetectionResults(getReportXmlFile(build), buildListener, result);

            if (result.hasChanges()) {
                UftTestDiscoveryDispatcher dispatcher = getExtension(UftTestDiscoveryDispatcher.class);
                dispatcher.enqueueResult(build.getProject().getName(), build.getNumber());
            }
            createInitialDetectionFile(build.getWorkspace());

        } catch (Exception e) {
            logger.error("Fail in startScanning : " + e.getMessage());
        }

        return result;
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

    private static boolean isFullScan(AbstractBuild<?, ?> build) {
        ParametersAction parameters = build.getAction(ParametersAction.class);
        if (parameters != null) {
            ParameterValue parameterValue = parameters.getParameter(UftConstants.FULL_SCAN_PARAMETER_NAME);
            if (parameterValue != null) {
                return (Boolean) parameterValue.getValue();
            }
        }
        return false;
    }


    private static <T> T getExtension(Class<T> clazz) {
        ExtensionList<T> items = Jenkins.getInstance().getExtensionList(clazz);
        return items.get(0);
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

    private static void printToConsole(BuildListener buildListener, String msg) {
        if (buildListener != null) {
            buildListener.getLogger().println("UFTTestDetectionService : " + msg);
        }
    }

    private static UftTestDiscoveryResult doChangeSetDetection(Object[] changeSetItems, File workspace) {
        UftTestDiscoveryResult result = new UftTestDiscoveryResult();
        if (changeSetItems.length == 0) {
            return result;
        }

        for (int i = 0; i < changeSetItems.length; i++) {
            ChangeLogSet.Entry changeSet = (ChangeLogSet.Entry) changeSetItems[i];
            for (ChangeLogSet.AffectedFile affectedFileChange : changeSet.getAffectedFiles()) {
                if (affectedFileChange.getPath().startsWith("\"")) {
                    result.setHasQuotedPaths(true);
                }
                boolean isDir = isDir(affectedFileChange);
                String affectedFileFullPath = workspace + File.separator + affectedFileChange.getPath();
                if (!isDir) {
                    if (UftTestDiscoveryUtils.isTestMainFilePath(affectedFileChange.getPath())) {
                        File testFolder = UftTestDiscoveryUtils.getTestFolderForTestMainFile(affectedFileFullPath);
                        File affectedFile = new File(affectedFileFullPath);
                        boolean fileExist = affectedFile.exists();
                        UftTestType uftTestType = UftTestDiscoveryUtils.getUftTestType(affectedFileChange.getPath());

                        AutomatedTest test = UftTestDiscoveryUtils.createAutomatedTest(workspace, testFolder, uftTestType);
                        addChangeSetSrcAndDst(test, affectedFileChange);

                        if (EditType.ADD.equals(affectedFileChange.getEditType())) {
                            if (fileExist) {
                                result.getAllTests().add(test);
                            }
                        } else if (EditType.DELETE.equals(affectedFileChange.getEditType())) {
                            if (!fileExist) {
                                test.setOctaneStatus(OctaneStatus.DELETED);
                                test.setExecutable(false);
                                result.getAllTests().add(test);
                            }
                        } else if (EditType.EDIT.equals(affectedFileChange.getEditType())) {
                            if (fileExist) {
                                test.setOctaneStatus(OctaneStatus.MODIFIED);
                                result.getAllTests().add(test);
                            }
                        }
                    } else if (UftTestDiscoveryUtils.isUftDataTableFile(affectedFileChange.getPath())) {
                        File affectedFile = new File(affectedFileFullPath);
                        ScmResourceFile resourceFile = UftTestDiscoveryUtils.createDataTable(workspace, affectedFile);
                        addChangeSetSrcAndDst(resourceFile, affectedFileChange);
                        if (EditType.ADD.equals(affectedFileChange.getEditType())) {
                            UftTestType testType = UftTestDiscoveryUtils.isUftTestFolder(affectedFile.getParentFile().listFiles());
                            if (testType.isNone()) {
                                if (affectedFile.exists()) {
                                    result.getAllScmResourceFiles().add(resourceFile);
                                }
                            }
                        } else if (EditType.DELETE.equals(affectedFileChange.getEditType())) {
                            if (!affectedFile.exists()) {
                                resourceFile.setOctaneStatus(OctaneStatus.DELETED);
                                result.getAllScmResourceFiles().add(resourceFile);
                            }
                        }
                    }
                } else //isDir
                {
                    if (EditType.DELETE.equals(affectedFileChange.getEditType())) {

                        FilePath filePath = new FilePath(new File(affectedFileChange.getPath()));
                        String deletedFolder = filePath.getRemote().replace(SdkConstants.FileSystem.LINUX_PATH_SPLITTER, SdkConstants.FileSystem.WINDOWS_PATH_SPLITTER);
                        result.getDeletedFolders().add(deletedFolder);
                    }
                }
            }
        }

        return result;
    }

    private static boolean isDir(ChangeLogSet.AffectedFile path) {
        //ONLY for SVN plugin : Check if path is directory
        if (path.getClass().getName().equals("hudson.scm.SubversionChangeLogSet$Path")) {
            try {
                String value = (String) FieldUtils.readDeclaredField(path, "kind", true);
                return "dir".equals(value);
            } catch (Exception e) {
                //treat it as false
            }
        }
        return false;
    }

    private static void addChangeSetSrcAndDst(SupportsMoveDetection entity, ChangeLogSet.AffectedFile affectedFile) {
        if (affectedFile != null) {
            ScmPluginHandler handler = ScmPluginFactory.getScmHandlerByChangePathClass(affectedFile.getClass().getName());
            if (handler != null) {
                entity.setChangeSetSrc(handler.getChangeSetSrc(affectedFile));
                entity.setChangeSetDst(handler.getChangeSetDst(affectedFile));
            }
        }
    }

    private static boolean initialDetectionFileExist(FilePath workspace) {
        try {
            File rootFile = new File(workspace.toURI());
            File file = new File(rootFile, INITIAL_DETECTION_FILE);
            return file.exists();

        } catch (Exception e) {
            return false;
        }
    }

    private static void createInitialDetectionFile(FilePath workspace) {
        try {
            File rootFile = new File(workspace.toURI());
            File file = new File(rootFile, INITIAL_DETECTION_FILE);
            logger.info("Initial detection file path : " + file.getPath());
            file.createNewFile();
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to createInitialDetectionFile : " + e.getMessage());
        }
    }

    /**
     * Serialize detectionResult to file in XML format
     *
     * @param fileToWriteTo
     * @param taskListenerLog
     * @param detectionResult
     */
    public static void publishDetectionResults(File fileToWriteTo, TaskListener taskListenerLog, UftTestDiscoveryResult detectionResult) {

        try {
            detectionResult.writeToFile(fileToWriteTo);
        } catch (JAXBException e) {
            String msg = "Failed to persist detection results because of JAXBException : " + e.getMessage();
            if (taskListenerLog != null) {
                taskListenerLog.error(msg);
            }
            logger.error(msg);
        }
    }

    public static UftTestDiscoveryResult readDetectionResults(Run run) {

        File file = getReportXmlFile(run);

        try {
            return UftTestDiscoveryResult.readFromFile(file);
        } catch (JAXBException | FileNotFoundException e) {
            return null;
        }
    }

    public static File getReportXmlFile(Run run) {
        return new File(run.getRootDir(), DETECTION_RESULT_FILE);
    }
}