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
package com.microfocus.application.automation.tools.uft.utils;

import com.microfocus.application.automation.tools.uft.model.UftRunAsUser;
import com.microfocus.application.automation.tools.results.projectparser.performance.XmlParserUtil;
import com.microfocus.application.automation.tools.uft.model.RerunSettingsModel;
import hudson.FilePath;
import hudson.model.*;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

import static com.microfocus.application.automation.tools.uft.utils.Constants.*;

public class UftToolUtils {

    private static final Logger logger = Logger.getLogger(UftToolUtils.class.getName());
    private static final String ACTION_TAG = "Action";
    private static final String ACTIONS_XML_TAG = "Actions.xml";

    private UftToolUtils() {
    }

    /**
     * Update rerun settings list
     *
     * @param fsTestPath          the build tests path
     * @param rerunSettingsModels the rerun settings models to update
     * @return
     */
    public static List<RerunSettingsModel> updateRerunSettings(String nodeName, String fsTestPath, List<RerunSettingsModel> rerunSettingsModels) {
        List<String> buildTests = getBuildTests(nodeName, fsTestPath);

        if(buildTests != null && !buildTests.isEmpty()) {
            List<String> testPaths = getTests(buildTests, rerunSettingsModels);
            for (String testPath : testPaths) {
                if (!listContainsTest(rerunSettingsModels, testPath)) {
                    rerunSettingsModels.add(new RerunSettingsModel(testPath, false, 0, ""));
                }
            }
        }

        return rerunSettingsModels;
    }

    public static boolean isMtbxContent(String testContent) {
        return testContent != null && testContent.toLowerCase().contains("<mtbx>");
    }
    public static boolean isMtbxFile(String testContent) {
        return testContent != null && testContent.toLowerCase().endsWith(".mtbx");
    }


    /**
     * Retrieves the build tests
     *
     * @return an mtbx file with tests, a single test or a list of tests from test folder
     */
    public static List<String> getBuildTests(String nodeName, String fsTestPath) {
        if (fsTestPath == null)  return new ArrayList<>();
        List<String> buildTests;
        Node node = Jenkins.get().getNode(nodeName);
        String rawTestString = fsTestPath.replace("\\", "/").trim();

        if (Jenkins.get().getNodes().isEmpty() || (node == null)) {//run tests on master
            buildTests = getTests(rawTestString);
        } else {//run tests on selected node
            buildTests = getTestsFromNode(nodeName, rawTestString);
        }

        return buildTests;
    }

    public static List<String> getTests(String rawTestString) {
        List<String> buildTests = new ArrayList<>();
        if (isMtbxContent(rawTestString)) {//mtbx content in the test path
            buildTests = extractTestPathsFromMtbxContent(rawTestString);
        } else if (isMtbxFile(rawTestString)) {//mtbx file in the test path
            try {
                String fileContent = new String(Files.readAllBytes(Paths.get(rawTestString)));
                return getTests(fileContent);
            } catch (IOException e) {
                logger.info(String.format("Failed to get tests from mtbx file %s : %s", rawTestString, e.getMessage()));
            }
        } else if (rawTestString != null) {
            List<String> tests = Arrays.asList(rawTestString.split("\\r?\\n"));

            String paramFilteredTestPath = filterParamFromPath(rawTestString);

            File testFolder = new File(paramFilteredTestPath);

            if (tests.size() == 1 && (testFolder.isDirectory())) {//single test, folder or mtbx file
                if(testFolder.exists()){
                    buildTests = listFilesForFolder(new File(paramFilteredTestPath));
                }
            } else {//list of tests/folders
                for (String test : tests) {
                    File testFile = new File(filterParamFromPath(test).trim());
                    if(testFile.exists()) {
                        buildTests = getBuildTests(testFile);
                    }
                }
            }
        }

        return buildTests;
    }

    private static String filterParamFromPath(String testPath) {
        int firstIndexOfParam = testPath.indexOf(" \"");
        return firstIndexOfParam == -1 ? testPath : testPath.substring(0, firstIndexOfParam);
    }

    public static List<String> extractTestPathsFromMtbxContent(String mtbxContent) {
        List<String> tests = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(mtbxContent.getBytes()));
            document.getDocumentElement().normalize();
            Element root = document.getDocumentElement();
            NodeList childNodes = root.getChildNodes();
            for (int x = 0; x < childNodes.getLength(); x++) {
                org.w3c.dom.Node data = childNodes.item(x);
                if (data.getNodeName().equalsIgnoreCase("Test")) {
                    tests.add(XmlParserUtil.getNodeAttr("path", data));
                }
            }
        } catch (IOException | SAXException | ParserConfigurationException e) {
            logger.warning("Failed to extractTestPathsFromMtbxContent : " + e.getMessage());
        }

        return tests;
    }

    private static List<String> getTestsFromNode(String nodeName, String path) {
        Node node = Jenkins.get().getNode(nodeName);
        FilePath filePath = new FilePath(node.getChannel(), path);
        UftMasterToSlave uftMasterToSlave = new UftMasterToSlave(path);
        List<String> tests = new ArrayList<>();
        try {
            tests = filePath.act(uftMasterToSlave);//
        } catch (IOException e) {
            logger.info(String.format("File path not found %s", e.getMessage()));
        } catch (InterruptedException e) {
            logger.info(String.format("Remote operation failed %s", e.getMessage()));
        }

        return tests;
    }

    public static void deleteReportFoldersFromNode(String nodeName, String testPath, TaskListener listener) {
        FilePath filePath = getFilePath(nodeName, testPath);
        try {
            List<FilePath> entries = filePath.list();
            boolean isDeleted = false;
            for (FilePath entry : entries) {
                try {
                    if (entry.getName().startsWith("Report")) {
                        entry.deleteRecursive();
                        listener.getLogger().println(String.format("Folder %s is deleted", entry));
                        isDeleted = true;
                    }
                } catch (Exception e) {
                    listener.error(String.format("Failed to delete folder %s : %s", entry.getName(), e.getMessage()));
                }
                try {
                    if (entry.getName().startsWith("StRes")) {
                        entry.deleteRecursive();
                        listener.getLogger().println(String.format("Folder %s is deleted", entry));
                    }
                } catch (Exception e) {
                    listener.error(String.format("Failed to delete folder %s : %s", entry.getName(), e.getMessage()));
                }
            }

            if (!isDeleted) {
                listener.getLogger().println(String.format("No report folder was deleted"));
            }
        } catch (IOException | InterruptedException e) {
            listener.error("Failure in clearing report folders for " + testPath + " : " + e.getMessage());
        }
    }

    public static FilePath getFilePath(String nodeName, String testPath){
        Node node = Jenkins.get().getNode(nodeName);
        FilePath filePath;
        if (Jenkins.get().getNodes().isEmpty() || (node == null)) {//tests are running on master
            filePath = new FilePath(new File(testPath));
        } else {//tests are running on node
            filePath = new FilePath(node.getChannel(), testPath);
        }

        return filePath;
    }

    /**
     * Retrieves the mtbx path, a test path or the list of tests inside a folder
     *
     * @param folder the test path setup in the configuration (can be the an mtbx file, a single test or a folder containing other tests)
     * @return a list of tests
     */
    private static List<String> listFilesForFolder(final File folder) {
        List<String> buildTests = new ArrayList<>();

        if (!folder.isDirectory() && folder.getName().contains("mtbx")) {
            buildTests.add(folder.getPath().trim());
            return buildTests;
        }

        if(folder.isDirectory() && !folder.getName().contains("mtbx") && folder.getName().contains(ACTION_TAG)){//single test
                buildTests.add(folder.getPath().trim());
        }

        buildTests = getBuildTests(folder);

        return buildTests;
    }

    /**
     * Get the list of build tests
     * @param folder
     * @return either a single test or a set of tests
     */
    private static List<String> getBuildTests(final File folder){
        List<String> buildTests = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        for (final File fileEntry : files) {
            if (fileEntry.isDirectory()) {
                if(!fileEntry.getName().contains(ACTION_TAG)){
                    buildTests.add(fileEntry.getPath().trim());
                    continue;
                }
                buildTests.add(folder.getPath().trim());//single test
                break;
            } else if (fileEntry.isFile() && fileEntry.getName().endsWith(ACTIONS_XML_TAG)) {
                buildTests.add(folder.getPath().trim()); // it is an api test, which contains Actions.xml, which contains all the test Actions
                break;
            }
        }

        return buildTests;
    }

    /**
     * Checks if a list of tests contains another test
     *
     * @param rerunSettingModels the list of tests
     * @param test               the verified test
     * @return true if the list already contains the test, false otherwise
     */
    private static Boolean listContainsTest(List<RerunSettingsModel> rerunSettingModels, String test) {
        for (RerunSettingsModel settings : rerunSettingModels) {
            if (settings.getTest().trim().equals(test.trim())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Updates the list of current tests based on the updated list of build tests
     *
     * @param buildTests         the list of build tests setup in the configuration
     * @param rerunSettingModels the list of current tests
     * @return the updated list of tests to rerun
     */
    private static List<String> getTests(List<String> buildTests, List<RerunSettingsModel> rerunSettingModels) {
        List<String> rerunTests = new ArrayList<>();
        if (buildTests == null || rerunSettingModels == null) {
            return rerunTests;
        }

        for (RerunSettingsModel rerun : rerunSettingModels) {
            rerunTests.add(rerun.getTest().trim());
        }

        for (String test : buildTests) {
            if (!rerunTests.contains(test)) {
                rerunTests.add(test.trim());
            }
        }

        for (Iterator<RerunSettingsModel> it = rerunSettingModels.iterator(); it.hasNext(); ) {
            RerunSettingsModel rerunSettingsModel1 = it.next();
            if (!buildTests.contains(rerunSettingsModel1.getTest().trim())) {
                rerunTests.remove(rerunSettingsModel1.getTest());
                it.remove();
            }
        }

        return rerunTests;
    }

    public static FormValidation doCheckNumberOfReruns(final String value) {

        String errorMessage = "You must enter a positive integer number.";

        try {
            int number = Integer.parseInt(value);

            if (StringUtils.isBlank(value.trim()) || number < 0) {
                return FormValidation.error(errorMessage);
            }
        } catch (NumberFormatException e) {
            return FormValidation.error(errorMessage);
        }

        return FormValidation.ok();
    }

    public static List<String> getNodesList() {
        List<Node> nodeList = Jenkins.get().getNodes();
        List<String> nodes = new ArrayList<>();
        nodes.add("master");
        for (Node node : nodeList) {
            nodes.add(node.getDisplayName());
        }

        return nodes;
    }
    public static boolean isPrintTestParams(@Nonnull Run<?, ?> build, @Nonnull TaskListener listener) {
        ParametersAction parameterAction = build.getAction(ParametersAction.class);
        String msg = "NOTE : The test parameters and their values are printed by default in both Console Output and Results###.xml. You can disable this behavior by defining a job-level parameter UFT_PRINT_TEST_PARAMS as boolean and set it to false.";
        boolean isUftPrintTestParams = true;
        if (parameterAction == null) {
            listener.getLogger().println(msg);
        } else {
            ParameterValue uftPrintTestParams = parameterAction.getParameter(UFT_PRINT_TEST_PARAMS);
            if (uftPrintTestParams == null) {
                listener.getLogger().println(msg);
            } else {
                isUftPrintTestParams = (boolean) uftPrintTestParams.getValue();
                listener.getLogger().println(String.format(KEY_VALUE_FORMAT, UFT_PRINT_TEST_PARAMS, isUftPrintTestParams ? "Yes" : "No")) ;
            }
        }
        return isUftPrintTestParams;
    }

    public static UftRunAsUser getRunAsUser(@Nonnull Run<?, ?> build, @Nonnull TaskListener listener) throws IllegalArgumentException {
        ParametersAction paramAction = build.getAction(ParametersAction.class);
        UftRunAsUser uftRunAsUser = null;
        if (paramAction != null) {
            ParameterValue paramValuePair = paramAction.getParameter(UFT_RUN_AS_USER_NAME);
            if (paramValuePair != null) {
                String username = (String) paramValuePair.getValue();
                if (StringUtils.isNotBlank(username)) {
                    listener.getLogger().println(String.format(KEY_VALUE_FORMAT, UFT_RUN_AS_USER_NAME, username));
                    paramValuePair = paramAction.getParameter(UFT_RUN_AS_USER_ENCODED_PWD);
                    if (paramValuePair == null) {
                        uftRunAsUser = getRunAsUserWithPassword(paramAction, username);
                    } else {
                        Secret encodedPwd = (Secret) paramValuePair.getValue();
                        if (encodedPwd == null || StringUtils.isBlank(encodedPwd.getPlainText())) {
                            uftRunAsUser = getRunAsUserWithPassword(paramAction, username);
                        } else {
                            paramValuePair = paramAction.getParameter(UFT_RUN_AS_USER_PWD);
                            if (paramValuePair != null) {
                                Secret pwd = (Secret) paramValuePair.getValue();
                                if (pwd != null && StringUtils.isNotBlank(pwd.getPlainText())) {
                                    throw new IllegalArgumentException(String.format("Please provide either %s or %s, but not both.", UFT_RUN_AS_USER_PWD, UFT_RUN_AS_USER_ENCODED_PWD));
                                }
                            }
                            uftRunAsUser = new UftRunAsUser(username, encodedPwd.getPlainText());
                        }
                    }
                }
            }
        }
        return uftRunAsUser;
    }

    private static UftRunAsUser getRunAsUserWithPassword(ParametersAction paramAction, String username) throws IllegalArgumentException {
        Secret pwd = getRunAsUserPassword(paramAction);
        if (pwd == null || StringUtils.isBlank(pwd.getPlainText())) {
            throw new IllegalArgumentException(String.format("Either %s or %s is required.", UFT_RUN_AS_USER_PWD, UFT_RUN_AS_USER_ENCODED_PWD));
        }
        return new UftRunAsUser(username, pwd);
    }
    private static Secret getRunAsUserPassword(ParametersAction paramAction) {
        Secret pwd = null;
        if (paramAction != null) {
            ParameterValue paramValuePair = paramAction.getParameter(UFT_RUN_AS_USER_PWD);
            if (paramValuePair != null) {
                pwd = (Secret) paramValuePair.getValue();
            }
        }
        return pwd;
    }
}
