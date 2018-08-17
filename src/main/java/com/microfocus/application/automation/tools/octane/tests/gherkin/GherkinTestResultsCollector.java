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

package com.microfocus.application.automation.tools.octane.tests.gherkin;

import com.microfocus.application.automation.tools.octane.actions.cucumber.CucumberResultsService;
import com.microfocus.application.automation.tools.octane.tests.junit.TestResultStatus;
import com.microfocus.application.automation.tools.octane.tests.testResult.TestResult;
import hudson.FilePath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by franksha on 20/03/2016.
 */
public class GherkinTestResultsCollector {

    public static List<TestResult> collectGherkinTestsResults(File buildDir) throws ParserConfigurationException, IOException, InterruptedException, SAXException, TransformerException {
        List<TestResult> result = new ArrayList<>();

        //Retrieve the cucumber results xml
        int i = 0;
        FilePath gherkinTestResultsFilePath = new FilePath(buildDir).child(CucumberResultsService.getGherkinResultFileName(i));

        while (gherkinTestResultsFilePath.exists()) {
            String gherkinTestResultsPath = buildDir.getAbsolutePath() + File.separator + CucumberResultsService.getGherkinResultFileName(i);

            //parse the xml
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(gherkinTestResultsPath);
            doc.getDocumentElement().normalize();

            validateXMLVersion(doc);

            //Go over the features
            NodeList featureNodes = doc.getElementsByTagName("feature");
            for (int f = 0; f < featureNodes.getLength(); f++) {
                Element featureElement = (Element) featureNodes.item(f);
                FeatureInfo featureInfo = new FeatureInfo(featureElement);
                result.add(new GherkinTestResult(featureInfo.getName(), featureElement, featureInfo.getDuration(), featureInfo.getStatus()));
            }

            i++;
            gherkinTestResultsFilePath = new FilePath(buildDir).child(CucumberResultsService.getGherkinResultFileName(i));
        } //end while

        return result;
    }

    private static class FeatureInfo {
        private String name;
        private List<String> scenarioNames = new ArrayList<>();
        private TestResultStatus status = TestResultStatus.PASSED;
        private boolean statusDetermined = false;
        private long duration = 0;

        public FeatureInfo(Element featureElement) {
            name = featureElement.getAttribute("name");
            NodeList backgroundNodes = featureElement.getElementsByTagName("background");
            Element backgroundElement = backgroundNodes.getLength() > 0 ? (Element)backgroundNodes.item(0) : null;
            NodeList backgroundSteps = backgroundElement != null ? backgroundElement.getElementsByTagName("step") : null;

            //Go over the scenarios
            NodeList scenarioNodes = featureElement.getElementsByTagName("scenario");
            for (int s = 0; s < scenarioNodes.getLength(); s++) {
                Element scenarioElement = (Element) scenarioNodes.item(s);
                ScenarioInfo scenarioInfo = new ScenarioInfo(scenarioElement, backgroundSteps);
                String scenarioName = scenarioInfo.getName();
                scenarioNames.add(scenarioName);

                duration += scenarioInfo.getDuration();
                if (!statusDetermined && TestResultStatus.SKIPPED.equals(scenarioInfo.getStatus())) {
                    status = TestResultStatus.SKIPPED;
                    statusDetermined = true;
                } else if (!statusDetermined && TestResultStatus.FAILED.equals(scenarioInfo.getStatus())) {
                    status = TestResultStatus.FAILED;
                    statusDetermined = true;
                }
            }
        }

        public String getName() {
            return name;
        }

        public List<String> getScenarioNames() {
            return scenarioNames;
        }

        public TestResultStatus getStatus() {
            return status;
        }

        public long getDuration() {
            return duration;
        }

        private class ScenarioInfo {
            private List<String> stepNames = new ArrayList<String>();
            private long duration = 0;
            private TestResultStatus status = TestResultStatus.PASSED;
            private boolean statusDetermined = false;
            private String name;

            public ScenarioInfo(Element scenarioElement, NodeList backgroundSteps) {
                name = getScenarioName(scenarioElement);

                List<Element> stepElements = getStepElements(backgroundSteps, scenarioElement);
                for (Element stepElement : stepElements) {
                    addStep(stepElement);
                }

                scenarioElement.setAttribute("status", status.toPrettyName());

                //for surefire report
                stepNames.add(name);
                stepNames.add("Scenario: " + name);
            }

            public List<String> getStepNames() {
                return stepNames;
            }

            public long getDuration() {
                return duration;
            }

            public TestResultStatus getStatus() {
                return status;
            }

            public String getName() {
                return name;
            }

            private void addStep(Element stepElement) {
                String stepName = stepElement.getAttribute("name");
                stepNames.add(stepName);

                String durationStr = stepElement.getAttribute("duration");
                long stepDuration = durationStr != "" ? Long.parseLong(durationStr) : 0;
                duration += stepDuration;

                String stepStatus = stepElement.getAttribute("status");
                if (!statusDetermined && ("pending".equals(stepStatus) || "skipped".equals(stepStatus))) {
                    status = TestResultStatus.SKIPPED;
                    statusDetermined = true;
                } else if (!statusDetermined && "failed".equals(stepStatus)) {
                    status = TestResultStatus.FAILED;
                    statusDetermined = true;
                }
            }

            private List<Element> getStepElements(NodeList backgroundSteps, Element scenarioElement) {
                List<Element> stepElements = new ArrayList<Element>();
                if(backgroundSteps != null) {
                    for (int bs = 0; bs < backgroundSteps.getLength(); bs++) {
                        Element stepElement = (Element) backgroundSteps.item(bs);
                        stepElements.add(stepElement);
                    }
                }
                NodeList stepNodes = scenarioElement.getElementsByTagName("step");
                for (int sn = 0; sn < stepNodes.getLength(); sn++) {
                    Element stepElement = (Element) stepNodes.item(sn);
                    stepElements.add(stepElement);
                }

                return stepElements;
            }

            private String getScenarioName(Element scenarioElement) {
                String scenarioName = scenarioElement.getAttribute("name");
                if (scenarioElement.hasAttribute("outlineIndex")) {
                    String outlineIndexStr = scenarioElement.getAttribute("outlineIndex");
                    if (outlineIndexStr != null && !outlineIndexStr.isEmpty()) {
                        Integer outlineIndex = Integer.valueOf(scenarioElement.getAttribute("outlineIndex"));
                        if (outlineIndex > 1) {
                            //we add the index only from 2 and upwards seeing as that is the naming convention in junit xml.
                            String delimiter = " ";
                            if (!scenarioName.contains(" ")) {
                                //we need to use the same logic as used in the junit report
                                delimiter = "_";
                            }
                            scenarioName = scenarioName + delimiter + scenarioElement.getAttribute("outlineIndex");
                        }
                    }

                }
                return scenarioName;
            }
        }
    }

    private static void validateXMLVersion(Document doc) {
        String XML_VERSION = "1";
        NodeList featuresNodes = doc.getElementsByTagName("features");
        if(featuresNodes.getLength() > 0) {
            String versionAttr = ((Element) featuresNodes.item(0)).getAttribute("version");
            if (versionAttr == null || versionAttr.isEmpty() || versionAttr.compareTo(XML_VERSION) != 0) {
                throw new IllegalArgumentException("\n********************************************************\n" +
                    "Incompatible xml version received from the Octane formatter.\n" +
                    "expected version = " + XML_VERSION + " actual version = " + versionAttr + ".\n" +
                    "You may need to update the octane formatter version to the correct version in order to work with this jenkins plugin\n" +
                    "********************************************************");
            }
        } else {
            throw new IllegalArgumentException("The file does not contain Octane Gherkin results. Configuration error?");
        }
    }
}
