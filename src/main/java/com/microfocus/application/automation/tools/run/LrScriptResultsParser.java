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

package com.microfocus.application.automation.tools.run;

import hudson.FilePath;
import hudson.model.TaskListener;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by YafimK on 22/03/2017.
 */

/**
 * LR Script result xml paser - convers the results XML to JUNIT
 */
public class LrScriptResultsParser {


    /**
     * The constant LR_SCRIPT_RESULT_FILENAME.
     */
    public static final String LR_SCRIPT_RESULT_FILENAME = "Results.xml";
    public static final String LR_SCRIPT_PASSED_STATUS = "Passed";
    public static final String LR_SCRIPT_REPORT_PASSED_STATUS = "passed";
    public static final String LR_SCRIPT_REPORT_FAILED_STATUS = "failed";
    private TaskListener _logger;
    private String _scriptName;

    public LrScriptResultsParser(TaskListener listener) {
        this._logger = listener;
    }

    /**
     * Parse script result.
     *
     * @param scriptName the script name
     * @param workspace  the workspace
     * @throws InterruptedException the interrupted exception
     */
    public void parseScriptResult(String scriptName,
                                  FilePath workspace)
            throws InterruptedException {
        this._scriptName = scriptName;
        invoke(workspace);
    }

    /**
     * Invoke void.
     *
     * @param ws_filePath the ws file path
     * @return the void
     * @throws IOException          the io exception
     * @throws InterruptedException the interrupted exception
     */
    public void invoke(FilePath ws_filePath) throws InterruptedException {
        FilePath sourceFile = ws_filePath.child(this._scriptName).child(LR_SCRIPT_RESULT_FILENAME);
        FilePath targetFile = ws_filePath.child(this._scriptName).child("JunitResult.xml");
        parse(sourceFile, targetFile);
    }

    /**
     * Parse.
     *
     * @param scriptName the script name
     * @param outputFile the output file
     */
    public void parse(FilePath scriptName, FilePath outputFile) throws InterruptedException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        Document doc;
        Document newDoc;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(scriptName.read());

            newDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

            doc.getDocumentElement().normalize();
            NodeList actionNodes = doc.getElementsByTagName("Action");

            Element testSuits = newDoc.createElement("testsuites");
            parseScriptAction(newDoc, actionNodes, testSuits, scriptName.getParent().getBaseName());
            Element reportSummaryNode = (Element) doc.getElementsByTagName("Summary").item(actionNodes.getLength());
            testSuits.setAttribute(
                    LR_SCRIPT_REPORT_PASSED_STATUS, reportSummaryNode.getAttribute(LR_SCRIPT_REPORT_PASSED_STATUS));
            testSuits.setAttribute("failures", reportSummaryNode.getAttribute(LR_SCRIPT_REPORT_FAILED_STATUS));
            testSuits.setAttribute("name", scriptName.getParent().getBaseName());
            testSuits.setAttribute("xmlns:xsd", "http://www.w3.org/2001/XMLSchema");
            testSuits.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            int tests =
                    Integer.parseInt(reportSummaryNode.getAttribute(LR_SCRIPT_REPORT_FAILED_STATUS)) +
                            Integer.parseInt(reportSummaryNode
                                    .getAttribute(LR_SCRIPT_REPORT_PASSED_STATUS));
            testSuits.setAttribute("tests", String.valueOf(tests));
            newDoc.appendChild(testSuits);
            newDoc.setXmlVersion("1.0");

            TransformerFactory tFactory =
                    TransformerFactory.newInstance();
            Transformer transformer =
                    tFactory.newTransformer();

            DOMSource source = new DOMSource(newDoc);
            StreamResult result = new StreamResult(outputFile.write());

            transformer.transform(source, result);
        } catch (SAXException e) {
            log("XML reader error");
            log(e);
        } catch (ParserConfigurationException e) {
            log("XML parser error");
            log(e);
        } catch (IOException e) {
            log("IO error");
            log(e);
        } catch (TransformerConfigurationException tce) {
            log("* Transformer Factory error");
            log(" " + tce.getMessage());

            Throwable x = tce;
            if (tce.getException() != null) {
                x = tce.getException();
            }
            log(x);
        } catch (TransformerException te) {
            log("* Transformation error");
            log(" " + te.getMessage());

            Throwable x = te;
            if (te.getException() != null) {
                x = te.getException();
            }
            log(x);
        }
    }

    private void log(Object msg) {
        _logger.error(msg.toString());
    }

    private static void parseScriptAction(Document newDoc, NodeList actionNodes, Element rootnode, String scriptName) {
        for (int i = 0; i < actionNodes.getLength(); i++) {

            Element action = (Element) actionNodes.item(i);


            NodeList actionProps = action.getElementsByTagName("AName");
            Element actionName = (Element) actionProps.item(0);

            Element testSuite = newDoc.createElement("testsuite");
            final String suiteName = getCharacterDataFromElement(actionName);
            testSuite.setAttribute("name", suiteName);

            NodeList stepNodes = action.getElementsByTagName("Step");
            parseScriptActionStep(newDoc, testSuite, stepNodes, scriptName + "." + suiteName);

            Element suiteSummaryNode = (Element) action.getElementsByTagName("Summary").item(0);
            testSuite.setAttribute(
                    LR_SCRIPT_REPORT_PASSED_STATUS, suiteSummaryNode.getAttribute(LR_SCRIPT_REPORT_PASSED_STATUS));
            testSuite.setAttribute("failures", suiteSummaryNode.getAttribute(LR_SCRIPT_REPORT_FAILED_STATUS));
            int tests =
                    Integer.parseInt(suiteSummaryNode.getAttribute(LR_SCRIPT_REPORT_FAILED_STATUS)) +
                            Integer.parseInt(suiteSummaryNode.getAttribute(LR_SCRIPT_REPORT_PASSED_STATUS));
            testSuite.setAttribute("package", scriptName);

            testSuite.setAttribute("tests", String.valueOf(tests));
            if (tests > 0) {
                rootnode.appendChild(testSuite);
            }
        }
    }

    private static void parseScriptActionStep(Document newDoc, Element testSuite, NodeList stepNodes,
                                              String className) {
        for (int j = 0; j < stepNodes.getLength(); j++) {
            Element step = (Element) stepNodes.item(j);
            Element objNode = (Element) step.getElementsByTagName("Obj").item(0);
            Element testCase = newDoc.createElement("testcase");
            testCase.setAttribute("name", getStepDataFromElement(objNode));
            Element nodeArgs = (Element) step.getElementsByTagName("NodeArgs").item(0);
            String stepStatus = nodeArgs.getAttribute("status");
            if (stepStatus.equals(LR_SCRIPT_PASSED_STATUS)) {
                stepStatus = "pass";
            } else {
                stepStatus = "fail";
                Element failureMessage = newDoc.createElement("failure");
                failureMessage.setAttribute("message", "");

                testCase.appendChild(failureMessage);
            }
            testCase.setAttribute("status", stepStatus);
            testCase.setAttribute("classname", className);

            testSuite.appendChild(testCase);
        }
    }

    private static String getStepDataFromElement(Element e) {
        String stepName = getCharacterDataFromElement(e);
        stepName = stepName.replace("Url: ", "");

        return stepName;
    }

    private static String getCharacterDataFromElement(Element e) {
        Node child = e.getFirstChild();
        if (child instanceof CharacterData) {
            CharacterData cd = (CharacterData) child;
            return cd.getData();
        }
        return "";
    }

    /**
     * Parse.
     *
     * @param scriptName the script name
     * @param outputFile the output file
     * @throws FileNotFoundException the file not found exception
     */
    public void parse(File scriptName, File outputFile) throws InterruptedException {
        parse(new FilePath(scriptName.getAbsoluteFile()), new FilePath(outputFile.getAbsoluteFile()));
    }

    /**
     * Sets script name.
     *
     * @param _scriptName the script name
     */
    public void setScriptName(String _scriptName) {
        this._scriptName = _scriptName;
    }
}
