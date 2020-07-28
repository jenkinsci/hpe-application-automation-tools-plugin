/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2019 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors ("Micro Focus") are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.uft.utils;

import com.microfocus.application.automation.tools.results.projectparser.performance.XmlParserUtil;
import com.microfocus.application.automation.tools.uft.model.RerunSettingsModel;
import hudson.FilePath;
import hudson.model.Node;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ivy.util.FileUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

public class UftToolUtils {

    private static final Logger logger = Logger.getLogger(UftToolUtils.class.getName());
    private static final String ACTION_TAG = "Action";

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
        List<String> buildTests = UftToolUtils.getBuildTests(nodeName, fsTestPath);

        if(buildTests != null && !buildTests.isEmpty()) {
            List<String> testPaths = UftToolUtils.getTests(buildTests, rerunSettingsModels);
            for (String testPath : testPaths) {
                if (!UftToolUtils.listContainsTest(rerunSettingsModels, testPath)) {
                    rerunSettingsModels.add(new RerunSettingsModel(testPath, false, 0, ""));
                }
            }
        }

        return rerunSettingsModels;
    }

    /**
     * Retrieves the build tests
     *
     * @return an mtbx file with tests, a single test or a list of tests from test folder
     */
    public static List<String> getBuildTests(String nodeName, String fsTestPath) {
        if (fsTestPath == null)  return new ArrayList<>();
        List<String> buildTests = new ArrayList<>();
        Node node = Jenkins.get().getNode(nodeName);
        String directoryPath = fsTestPath.replace("\\", "/").trim();

        if (Jenkins.get().getNodes().isEmpty() || (node == null)) {//run tests on master
            if(directoryPath.contains("<Mtbx>")){//mtbx content in the test path
                try {
                    buildTests = parseMtbxContent(directoryPath);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                }
            } else {
                List<String> tests = Arrays.asList(directoryPath.split("\\r?\\n"));

                if (tests.size() == 1 && (new File(directoryPath).isDirectory())) {//single test, folder or mtbx file
                    buildTests = listFilesForFolder(new File(directoryPath));
                } else {//list of tests/folders
                    for (String test : tests) {
                        File testFile = new File(test.trim());
                        buildTests = getBuildTests(testFile);
                    }
                }
            }
        } else {//run tests on selected node
            buildTests = getTestsFromNode(nodeName, directoryPath);
        }

        return buildTests;
    }

    public static List<String> parseMtbxContent(String mtbxContent) throws IOException, SAXException, ParserConfigurationException {
        List<String> tests = new ArrayList<>();

        File tempFile = new File("TempFile.txt");
        BufferedWriter output = null;
        try {
            output = new BufferedWriter(new FileWriter(tempFile));
            output.write(mtbxContent);
        }catch(IOException ex){
            System.out.println("Error writing the file : " + ex.getMessage());
        }finally {
            try {
                if (output != null) {
                    output.close();
                }
            }catch (IOException ex){
                System.out.println("Error closing the buffer writer" + ex.getMessage());
            }
        }


        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(tempFile);
        document.getDocumentElement().normalize();
        Element root = document.getDocumentElement();
        NodeList childNodes = root.getChildNodes();
        for (int x = 0; x < childNodes.getLength(); x++) {
            org.w3c.dom.Node data = childNodes.item(x);
            if (data.getNodeName().equalsIgnoreCase("Test")) {
                tests.add(XmlParserUtil.getNodeAttr("path", data));
            }
        }

        tempFile.delete();

        return tests;
    }

    public static List<String> getTestsFromNode(String nodeName, String path) {
        Node node = Jenkins.get().getNode(nodeName);
        FilePath filePath = new FilePath(node.getChannel(), path);
        UftMasterToSlave uftMasterToSlave = new UftMasterToSlave();
        List<String> tests = new ArrayList<>();
        try {
            tests = filePath.act(uftMasterToSlave);//invoke listFilesForFolder
        } catch (IOException e) {
            logger.info(String.format("File path not found %s", e.getMessage()));
        } catch (InterruptedException e) {
            logger.info(String.format("Remote operation failed %s", e.getMessage()));
        }

        return tests;
    }

    /**
     * Retrieves the mtbx path, a test path or the list of tests inside a folder
     *
     * @param folder the test path setup in the configuration (can be the an mtbx file, a single test or a folder containing other tests)
     * @return a list of tests
     */
    public static List<String> listFilesForFolder(final File folder) {
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
    public static List<String> getBuildTests(final File folder){
        List<String> buildTests = new ArrayList<>();
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                if(!fileEntry.getName().contains(ACTION_TAG)){
                    buildTests.add(fileEntry.getPath().trim()); continue;
                }
                buildTests.add(folder.getPath().trim());//single test
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
    public static Boolean listContainsTest(List<RerunSettingsModel> rerunSettingModels, String test) {
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
    public static List<String> getTests(List<String> buildTests, List<RerunSettingsModel> rerunSettingModels) {
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

}
