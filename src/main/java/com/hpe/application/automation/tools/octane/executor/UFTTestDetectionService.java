/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.hpe.application.automation.tools.octane.executor;

import com.hpe.application.automation.tools.octane.actions.UFTTestUtil;
import com.hpe.application.automation.tools.octane.actions.UftTestType;
import com.hpe.application.automation.tools.octane.actions.dto.AutomatedTest;
import com.hpe.application.automation.tools.octane.actions.dto.ScmResourceFile;
import hudson.ExtensionList;
import hudson.FilePath;
import hudson.model.*;
import hudson.plugins.git.GitChangeSet;
import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Service is responsible to detect changes according to SCM change and to put it to queue of UftTestDiscoveryDispatcher
 */
public class UFTTestDetectionService {
    private static final Logger logger = LogManager.getLogger(UFTTestDetectionService.class);
    private static final String INITIAL_DETECTION_FILE = "INITIAL_DETECTION_FILE.txt";
    private static final String DETECTION_RESULT_FILE = "detection_result.xml";
    private static final String STFileExtention = ".st";//api test
    private static final String QTPFileExtention = ".tsp";//gui test
    private static final String XLSXExtention = ".xlsx";//excel file
    private static final String XLSExtention = ".xls";//excel file
    private static final String windowsPathSplitter = "\\";
    private static final String linuxPathSplitter = "/";


    public static UFTTestDetectionResult startScanning(AbstractBuild<?, ?> build, String workspaceId, String scmRepositoryId, BuildListener buildListener) {
        ChangeLogSet<? extends ChangeLogSet.Entry> changeSet = build.getChangeSet();
        Object[] changeSetItems = changeSet.getItems();
        UFTTestDetectionResult result = null;

        try {

            boolean fullScan = build.getId().equals("1") || !initialDetectionFileExist(build.getWorkspace()) || isFullScan((build));
            if (fullScan) {
                printToConsole(buildListener, "Executing full sync");
                result = doInitialDetection(build.getWorkspace());
            } else {
                printToConsole(buildListener, "Executing changeSet sync");
                result = doChangeSetDetection(changeSetItems, build.getWorkspace());
                removeTestDuplicated(result.getUpdatedTests());
                removeFalsePositiveDeletedDataTables(result.getDeletedTests(), result.getDeletedScmResourceFiles());
            }

            if (!result.getNewTests().isEmpty()) {
                printToConsole(buildListener, String.format("Found %s new tests", result.getNewTests().size()));
            }
            if (!result.getUpdatedTests().isEmpty()) {
                printToConsole(buildListener, String.format("Found %s updated tests", result.getUpdatedTests().size()));
            }
            if (!result.getDeletedTests().isEmpty()) {
                printToConsole(buildListener, String.format("Found %s deleted tests", result.getDeletedTests().size()));
            }
            if (!result.getNewScmResourceFiles().isEmpty()) {
                printToConsole(buildListener, String.format("Found %s new data tables", result.getNewScmResourceFiles().size()));
            }
            if (!result.getDeletedScmResourceFiles().isEmpty()) {
                printToConsole(buildListener, String.format("Found %s deleted data tables", result.getDeletedScmResourceFiles().size()));
            }

            result.setScmRepositoryId(scmRepositoryId);
            result.setWorkspaceId(workspaceId);
            result.setFullScan(fullScan);
            sortTests(result.getNewTests());
            sortTests(result.getUpdatedTests());
            sortTests(result.getDeletedTests());
            sortDataTables(result.getNewScmResourceFiles());
            sortDataTables(result.getDeletedScmResourceFiles());
            publishDetectionResults(getReportXmlFile(build), buildListener, result);

            if (result.hasChanges()) {
                UftTestDiscoveryDispatcher dispatcher = getExtension(UftTestDiscoveryDispatcher.class);
                dispatcher.enqueueResult(build.getProject().getName(), build.getNumber());
            }
            createInitialDetectionFile(build.getWorkspace());

        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Deleted data table might be part of deleted test. During discovery its very hard to know.
     * Here we pass through all deleted data tables, if we found data table parent is test folder - we know that the delete was part of test delete
     *
     * @param deletedTests
     * @param deletedScmResourceFiles
     */
    private static void removeFalsePositiveDeletedDataTables(List<AutomatedTest> deletedTests, List<ScmResourceFile> deletedScmResourceFiles) {
        if (!deletedScmResourceFiles.isEmpty() && !deletedTests.isEmpty()) {

            List<ScmResourceFile> falsePositive = new ArrayList<>();
            for (ScmResourceFile item : deletedScmResourceFiles) {
                int parentSplitterIndex = item.getRelativePath().lastIndexOf(windowsPathSplitter);
                if (parentSplitterIndex != -1) {
                    String parentName = item.getRelativePath().substring(0, parentSplitterIndex);
                    for (AutomatedTest test : deletedTests) {
                        String testPath = StringUtils.isEmpty(test.getPackage()) ? test.getName() : test.getPackage() + windowsPathSplitter + test.getName();
                        if (testPath.equals(parentName)) {
                            falsePositive.add(item);
                            break;
                        }
                    }
                }
            }
            deletedScmResourceFiles.removeAll(falsePositive);
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

    private static void sortTests(List<AutomatedTest> newTests) {
        Collections.sort(newTests, new Comparator<AutomatedTest>() {
            @Override
            public int compare(AutomatedTest o1, AutomatedTest o2) {
                int comparePackage = o1.getPackage().compareTo(o2.getPackage());
                if (comparePackage == 0) {
                    return o1.getName().compareTo(o2.getName());
                } else {
                    return comparePackage;
                }
            }
        });
    }

    private static void sortDataTables(List<ScmResourceFile> dataTables) {
        Collections.sort(dataTables, new Comparator<ScmResourceFile>() {
            @Override
            public int compare(ScmResourceFile o1, ScmResourceFile o2) {
                return o1.getRelativePath().compareTo(o2.getRelativePath());
            }
        });
    }

    private static <T> T getExtension(Class<T> clazz) {
        ExtensionList<T> items = Jenkins.getInstance().getExtensionList(clazz);
        return items.get(0);
    }

    private static void removeTestDuplicated(List<AutomatedTest> tests) {
        Set<String> keys = new HashSet<>();
        List<AutomatedTest> testsToRemove = new ArrayList<>();
        for (AutomatedTest test : tests) {
            String key = test.getPackage() + "_" + test.getName();
            if (keys.contains(key)) {
                testsToRemove.add(test);
            }
            keys.add(key);

        }
        tests.removeAll(testsToRemove);
    }

    private static void printToConsole(BuildListener buildListener, String msg) {
        if (buildListener != null) {
            buildListener.getLogger().println("UFTTestDetectionService : " + msg);
        }
    }

    private static UFTTestDetectionResult doChangeSetDetection(Object[] changeSetItems, FilePath workspace) throws IOException, InterruptedException {
        UFTTestDetectionResult result = new UFTTestDetectionResult();
        if (changeSetItems.length == 0) {
            return result;
        }

        boolean isGitChanges = changeSetItems[0] instanceof GitChangeSet;
        if (!isGitChanges) {
            return result;
        }

        for (int i = 0; i < changeSetItems.length; i++) {
            GitChangeSet changeSet = (GitChangeSet) changeSetItems[i];
            for (GitChangeSet.Path path : changeSet.getPaths()) {
                String fileFullPath = workspace + File.separator + path.getPath();
                if (isTestMainFilePath(path.getPath())) {

                    if (EditType.ADD.equals(path.getEditType())) {
                        if (isFileExist(fileFullPath)) {
                            FilePath testFolder = getTestFolderForTestMainFile(fileFullPath);
                            scanFileSystemRecursively(workspace, testFolder, result.getNewTests(), result.getNewScmResourceFiles());
                        }
                    } else if (EditType.DELETE.equals(path.getEditType())) {
                        if (!isFileExist(fileFullPath)) {
                            FilePath testFolder = getTestFolderForTestMainFile(fileFullPath);
                            AutomatedTest test = createAutomatedTest(workspace, testFolder, null, false);
                            result.getDeletedTests().add(test);
                        }
                    } else if (EditType.EDIT.equals(path.getEditType())) {
                        if (isFileExist(fileFullPath)) {
                            FilePath testFolder = getTestFolderForTestMainFile(fileFullPath);
                            scanFileSystemRecursively(workspace, testFolder, result.getUpdatedTests(), result.getUpdatedScmResourceFiles());
                        }
                    }
                } else if (isUftDataTableFile(path.getPath())) {
                    FilePath filePath = new FilePath(new File(fileFullPath));
                    if (EditType.ADD.equals(path.getEditType())) {
                        UftTestType testType = isUftTestFolder(filePath.getParent().list());
                        if (testType.isNone()) {
                            if (filePath.exists()) {
                                ScmResourceFile resourceFile = createDataTable(workspace, filePath);
                                result.getNewScmResourceFiles().add(resourceFile);
                            }
                        }
                    } else if (EditType.DELETE.equals(path.getEditType())) {
                        if (!filePath.exists()) {
                            ScmResourceFile resourceFile = createDataTable(workspace, filePath);
                            result.getDeletedScmResourceFiles().add(resourceFile);
                        }
                    }
                }
            }
        }

        return result;
    }

    private static AutomatedTest createAutomatedTest(FilePath root, FilePath dirPath, UftTestType testType, boolean executable) {
        AutomatedTest test = new AutomatedTest();
        test.setName(dirPath.getName());

        String relativePath = getRelativePath(root, dirPath);
        String packageName = relativePath.length() != dirPath.getName().length() ? relativePath.substring(0, relativePath.length() - dirPath.getName().length() - 1) : "";
        test.setPackage(packageName);
        test.setExecutable(executable);

        if (testType != null && !testType.isNone()) {
            test.setUftTestType(testType);
        }

        String description = UFTTestUtil.getTestDescription(dirPath);
        test.setDescription(description);

        return test;
    }

    private static String getRelativePath(FilePath root, FilePath path) {
        String testPath = path.getRemote();
        String rootPath = root.getRemote();
        String relativePath = testPath.replace(rootPath, "");
        relativePath = StringUtils.strip(relativePath, windowsPathSplitter + linuxPathSplitter);
        //we want all paths will be in windows style, because tests are run in windows, therefore we replace all linux splitters (/) by windows one (\)
        //http://stackoverflow.com/questions/23869613/how-to-replace-one-or-more-in-string-with-just
        relativePath = relativePath.replaceAll(linuxPathSplitter, windowsPathSplitter + windowsPathSplitter);//str.replaceAll("/", "\\\\");
        return relativePath;
    }

    private static boolean isFileExist(String path) {
        File file = new File(path);
        return file.exists();
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
            file.createNewFile();
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to createInitialDetectionFile : " + e.getMessage());
        }
    }

    /*private static void removeInitialDetectionFlag(FilePath workspace) {
        try {
            File rootFile = new File(workspace.toURI());
            File file = new File(rootFile, INITIAL_DETECTION_FILE);
            file.delete();
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to removeInitialDetectionFlag");
        }
    }*/

    private static UFTTestDetectionResult doInitialDetection(FilePath workspace) throws IOException, InterruptedException {
        UFTTestDetectionResult result = new UFTTestDetectionResult();
        scanFileSystemRecursively(workspace, workspace, result.getNewTests(), result.getNewScmResourceFiles());
        return result;
    }

    private static void scanFileSystemRecursively(FilePath root, FilePath dirPath, List<AutomatedTest> foundTests, List<ScmResourceFile> foundResources) throws IOException, InterruptedException {
        List<FilePath> paths = dirPath.isDirectory() ? dirPath.list() : Arrays.asList(dirPath);

        //if it test folder - create new test, else drill down to subFolders
        UftTestType testType = isUftTestFolder(paths);
        if (!testType.isNone()) {
            AutomatedTest test = createAutomatedTest(root, dirPath, testType, true);
            foundTests.add(test);

        } else {
            for (FilePath path : paths) {
                if (path.isDirectory()) {
                    scanFileSystemRecursively(root, path, foundTests, foundResources);
                } else if (isUftDataTableFile(path.getName())) {
                    ScmResourceFile dataTable = createDataTable(root, path);
                    foundResources.add(dataTable);
                }
            }
        }
    }

    private static ScmResourceFile createDataTable(FilePath root, FilePath path) {
        ScmResourceFile resourceFile = new ScmResourceFile();
        resourceFile.setName(path.getName());
        resourceFile.setRelativePath(getRelativePath(root, path));
        return resourceFile;

    }

    private static boolean isUftDataTableFile(String path) {
        return path.endsWith(XLSXExtention) || path.endsWith(XLSExtention);
    }

    private static UftTestType isUftTestFolder(List<FilePath> paths) {
        for (FilePath path : paths) {
            if (path.getName().endsWith(STFileExtention)) {
                return UftTestType.API;
            }
            if (path.getName().endsWith(QTPFileExtention)) {
                return UftTestType.GUI;
            }
        }

        return UftTestType.None;
    }

    private static boolean isTestMainFilePath(String path) {
        String lowerPath = path.toLowerCase();
        if (lowerPath.endsWith(STFileExtention)) {
            return true;
        } else if (lowerPath.endsWith(QTPFileExtention)) {
            return true;
        }

        return false;
    }

    private static FilePath getTestFolderForTestMainFile(String path) {
        if (isTestMainFilePath(path)) {
            File file = new File(path);
            File parent = file.getParentFile();
            return new FilePath(parent);
        }
        return null;
    }

    /**
     * Serialize detectionResult to file in XML format
     *
     * @param fileToWriteTo
     * @param taskListenerLog
     * @param detectionResult
     */
    public static void publishDetectionResults(File fileToWriteTo, TaskListener taskListenerLog, UFTTestDetectionResult detectionResult) {

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(UFTTestDetectionResult.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(detectionResult, fileToWriteTo);

        } catch (JAXBException e) {
            if (taskListenerLog != null) {
                taskListenerLog.error("Failed to persist detection results: " + e.getMessage());
            }
            logger.error("Failed to persist detection results: " + e.getMessage());
        }
    }

    public static UFTTestDetectionResult readDetectionResults(Run run) {

        File file = getReportXmlFile(run);
        try {
            JAXBContext context = JAXBContext.newInstance(UFTTestDetectionResult.class);
            Unmarshaller m = context.createUnmarshaller();
            return (UFTTestDetectionResult) m.unmarshal(new FileReader(file));
        } catch (JAXBException | FileNotFoundException e) {
            return null;
        }
    }

    private static File getReportXmlFile(Run run) {
        return new File(run.getRootDir(), DETECTION_RESULT_FILE);
    }
}