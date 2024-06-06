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

package com.microfocus.application.automation.tools.octane.tests.junit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.executor.impl.TestingToolType;
import com.hp.octane.integrations.dto.tests.Property;
import com.hp.octane.integrations.dto.tests.TestSuite;
import com.hp.octane.integrations.executor.converters.MfMBTConverter;
import com.hp.octane.integrations.uft.ufttestresults.UftTestResultsUtils;
import com.hp.octane.integrations.uft.ufttestresults.schema.UftResultIterationData;
import com.hp.octane.integrations.uft.ufttestresults.schema.UftResultStepData;
import com.hp.octane.integrations.uft.ufttestresults.schema.UftResultStepParameter;
import com.hp.octane.integrations.utils.SdkConstants;
import com.microfocus.application.automation.tools.JenkinsUtils;
import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import com.microfocus.application.automation.tools.octane.executor.UftConstants;
import com.microfocus.application.automation.tools.octane.tests.HPRunnerType;
import com.microfocus.application.automation.tools.octane.tests.junit.codeless.CodelessResult;
import com.microfocus.application.automation.tools.octane.tests.junit.codeless.CodelessResultParameter;
import com.microfocus.application.automation.tools.octane.tests.junit.codeless.CodelessResultUnit;
import com.microfocus.application.automation.tools.octane.tests.xml.AbstractXmlIterator;
import hudson.FilePath;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * JUnit result parser and enricher according to HPRunnerType
 */
public class JUnitXmlIterator extends AbstractXmlIterator<JUnitTestResult> {
	private static final Logger logger = SDKBasedLoggerProvider.getLogger(JUnitXmlIterator.class);
	private final FilePath workspace;
	private final long buildStarted;
	private final String buildId;
	private final String jobName;
	private final HPRunnerType hpRunnerType;
	private boolean stripPackageAndClass;
	private String moduleName;
	private String moduleNameFromFile;
	private String packageName;
	private String id;
	private String className;
	private String testName;
	private long testDuration;
	private TestResultStatus status;
    private String stackTraceStr;
    private String errorType;
    private String errorMsg;
    private String externalURL;
    private String uftResultFilePath;
    private String description;
    private List<ModuleDetection> moduleDetection;
    private String jenkinsRootUrl;
    private String sharedCheckOutDirectory;
    private Object additionalContext;
    private String filePath;
    public static final String SRL_REPORT_URL = "reportUrl";
    private Pattern testParserRegEx;
    private String externalRunId;
    private List<UftResultIterationData> uftResultData;
    private boolean octaneSupportsSteps;
    private TestingToolType testingToolType = TestingToolType.UFT;
    // for codeless use
    private long stepDuration;
    private boolean insideCaseElement = false; // used for codeless test to differentiate between test duration and step duration
    private JUnitTestResult currentJUnitTestResult;
    private List<UftResultStepData> currentIterationSteps;
    private Map<String, JUnitTestResult> testNameToJunitResultMap = new HashMap<>();
    private String stepName;
    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private Map<String, CodelessResult> testNameToCodelessResultMap = new HashMap<>();
    private Set<String> nodeNames;

    private final int ERROR_MESSAGE_MAX_SIZE = System.getProperty("octane.sdk.tests.error_message_max_size") != null ? Integer.parseInt(System.getProperty("octane.sdk.tests.error_message_max_size")) : 512*512;
    private final int ERROR_DETAILS_MAX_SIZE = System.getProperty("octane.sdk.tests.error_details_max_size") != null ? Integer.parseInt(System.getProperty("octane.sdk.tests.error_details_max_size")) : 512*512;


    public JUnitXmlIterator(InputStream read, List<ModuleDetection> moduleDetection, FilePath workspace, String sharedCheckOutDirectory, String jobName, String buildId, long buildStarted, boolean stripPackageAndClass, HPRunnerType hpRunnerType, String jenkinsRootUrl, Object additionalContext, Pattern testParserRegEx, boolean octaneSupportsSteps,Set<String> nodeNames) throws XMLStreamException {
		super(read);
		this.stripPackageAndClass = stripPackageAndClass;
		this.moduleDetection = moduleDetection;
		this.workspace = workspace;
		this.sharedCheckOutDirectory = sharedCheckOutDirectory;
		this.buildId = buildId;
		this.jobName = jobName;
		this.buildStarted = buildStarted;
		this.hpRunnerType = hpRunnerType;
		this.jenkinsRootUrl = jenkinsRootUrl;
		this.additionalContext = additionalContext;
		this.testParserRegEx = testParserRegEx;
		this.octaneSupportsSteps = octaneSupportsSteps;
		this.nodeNames = nodeNames;
	}

	private static long parseTime(String timeString) {
		String time = timeString.replace(",", "");
		try {
			float seconds = Float.parseFloat(time);
			return (long) (seconds * 1000);
		} catch (NumberFormatException e) {
			try {
				return new DecimalFormat().parse(time).longValue();
			} catch (ParseException ex) {
				logger.debug("Unable to parse test duration: " + timeString);
			}
		}
		return 0;
	}

	@Override
	protected void onEvent(XMLEvent event) throws XMLStreamException, IOException, InterruptedException {
        if (testingToolType.equals(TestingToolType.CODELESS)) {
            handleCodelessTest(event);
        } else {
            handleJUnitTest(event);
        }
    }

    private void handleJUnitTest(XMLEvent event) throws XMLStreamException, IOException, InterruptedException {
        if (event instanceof StartElement) {
            StartElement element = (StartElement) event;
            String localName = element.getName().getLocalPart();
            if ("file".equals(localName)) {  // NON-NLS
                filePath = peekNextValue();
                if(checkIsCodelessTestResult(filePath)) {
                    testingToolType = TestingToolType.CODELESS;
                    handleCodelessTest(event);
                } else {
                    filePath = readNextValue();
                    testingToolType = TestingToolType.UFT;
                    for (ModuleDetection detection : moduleDetection) {
                        moduleNameFromFile = moduleName = detection.getModule(new FilePath(new File(filePath)));
                        if (moduleName != null) {
                            break;
                        }
                    }
                }
            } else if ("id".equals(localName)) {
                id = readNextValue();
            } else if ("case".equals(localName)) { // NON-NLS
                resetTestData();
            } else if ("className".equals(localName)) { // NON-NLS
                String fqn = readNextValue();
                int moduleIndex = fqn.indexOf("::");
                if (moduleIndex > 0) {
                    moduleName = fqn.substring(0, moduleIndex);
                    fqn = fqn.substring(moduleIndex + 2);
                }

                int p = fqn.lastIndexOf('.');
                className = fqn.substring(p + 1);
                if (p > 0) {
                    packageName = fqn.substring(0, p);
                } else {
                    packageName = "";
                }
            } else if ("stdout".equals(localName)) {
                String stdoutValue = readNextValue();
                if (stdoutValue != null) {
                    if ((hpRunnerType.equals(HPRunnerType.UFT) || hpRunnerType.equals(HPRunnerType.UFT_MBT)) && stdoutValue.contains("Test result: Warning")) {
                        errorMsg = "Test ended with 'Warning' status.";
                        parseUftErrorMessages();
                    }

                    externalURL = extractValueFromStdout(stdoutValue, "__octane_external_url_start__", "__octane_external_url_end__", externalURL);
                    description = extractValueFromStdout(stdoutValue, "__octane_description_start__", "__octane_description_end__", description);
                }
            } else if ("testName".equals(localName)) { // NON-NLS
                testName = readNextValue();
                if (testName != null && testName.endsWith("()")) {//clear ending () for gradle tests
                    testName = testName.substring(0, testName.length() - 2);
                }

                if (hpRunnerType.equals(HPRunnerType.UFT) || hpRunnerType.equals(HPRunnerType.UFT_MBT)) {
                    if (testName != null && testName.contains("..")) { //resolve existence of ../ - for example c://a/../b => c://b
                        testName = new File(FilenameUtils.separatorsToSystem(testName)).getCanonicalPath();
                    }

                    String myPackageName = packageName;
                    String myClassName = className;
                    String myTestName = testName;
                    packageName = "";
                    className = "";

                    // if workspace is prefix of the method name, cut it off
                    // currently this handling is needed for UFT tests
                    int uftTextIndexStart = getUftTestIndexStart(workspace, sharedCheckOutDirectory, testName);
                    if (uftTextIndexStart != -1) {
                        String path = testName.substring(uftTextIndexStart).replace(SdkConstants.FileSystem.LINUX_PATH_SPLITTER, SdkConstants.FileSystem.WINDOWS_PATH_SPLITTER);;
                        boolean isMBT = path.startsWith(MfMBTConverter.MBT_PARENT_SUB_DIR);
                        if(isMBT){//remove MBT prefix
                            //mbt test located in two level folder : ___mbt/_order
                            path = path.substring(MfMBTConverter.MBT_PARENT_SUB_DIR.length() + 1);//remove ___mbt
                            path = path.substring(path.indexOf(SdkConstants.FileSystem.WINDOWS_PATH_SPLITTER));//remove order part
                        }

                        path = StringUtils.strip(path, SdkConstants.FileSystem.WINDOWS_PATH_SPLITTER);

                        //split path to package and name fields
                        if (path.contains(SdkConstants.FileSystem.WINDOWS_PATH_SPLITTER)) {
                            int testNameStartIndex = path.lastIndexOf(SdkConstants.FileSystem.WINDOWS_PATH_SPLITTER);

                            testName = path.substring(testNameStartIndex + 1);
                            packageName = path.substring(0, testNameStartIndex);
                        } else {
                            testName = path;
                            if (isMBT) {
                                testName = MfMBTConverter.decodeTestNameIfRequired(testName);
                            }
                        }
                    }

                    String cleanedTestName = cleanTestName(testName);
                    String nodeName = "";
                    if(!nodeNames.isEmpty()){
                        nodeName = nodeNames.stream().findFirst().get();
                    }
                    boolean testReportCreated = true;
                    if (additionalContext != null && additionalContext instanceof List) {
                        //test folders are appear in the following format GUITest1[1], while [1] number of test. It possible that tests with the same name executed in the same job
                        //by adding [1] or [2] we can differentiate between different instances.
                        //We assume that test folders are sorted so in this section, once we found the test folder, we remove it from collection , in order to find the second instance in next iteration
                        List<String> createdTests = (List<String>) additionalContext;
                        String searchFor = cleanedTestName + "[";
                        Optional<String> optional = createdTests.stream().filter(str -> str.contains(searchFor)).findFirst();
                        if (optional.isPresent()) {
                            String nodeTestString = optional.get();
                            if(nodeTestString.contains("/")){
                                String node = nodeTestString.split("/")[0];
                                if (nodeNames.contains(node)) {
                                    nodeName = node;
                                }

                                cleanedTestName = nodeTestString.split("/")[1];
                                createdTests.remove(nodeTestString);
                            }

                        }
                        testReportCreated = optional.isPresent();
                    }

                    if (testReportCreated) {
                        final String basePath = ((List<String>) additionalContext).get(0);
                        String nodeNameSubFolder = StringUtils.isNotEmpty(nodeName) ? nodeName +"/" : "";
                        uftResultFilePath = Paths.get(basePath, "archive", "UFTReport", nodeNameSubFolder, cleanedTestName, "/Result/run_results.xml").toFile().getCanonicalPath();
                        externalURL = jenkinsRootUrl + "job/" + jobName + "/" + buildId + "/artifact/UFTReport/" + nodeNameSubFolder + cleanedTestName + "/Result/run_results.html";
                    } else {
                        //if UFT didn't created test results page - add reference to Jenkins test results page
                        externalURL = jenkinsRootUrl + "job/" + jobName + "/" + buildId + "/testReport/" + myPackageName + "/" + jenkinsTestClassFormat(myClassName) + "/" + jenkinsTestNameFormat(myTestName) + "/";
                    }
                } else if (hpRunnerType.equals(HPRunnerType.PerformanceCenter)) {
                    externalURL = jenkinsRootUrl + "job/" + jobName + "/" + buildId + "/artifact/performanceTestsReports/pcRun/Report.html";
                } else if (hpRunnerType.equals(HPRunnerType.StormRunnerLoad)) {
                    externalURL = tryGetStormRunnerReportURLFromJunitFile(filePath);
                    if (StringUtils.isEmpty(externalURL) && additionalContext != null && additionalContext instanceof Collection) {
                        externalURL = tryGetStormRunnerReportURLFromLog((Collection) additionalContext);
                    }
                }
            } else if ("duration".equals(localName)) { // NON-NLS
                testDuration = parseTime(readNextValue());
            } else if ("skipped".equals(localName)) { // NON-NLS
                if ("true".equals(readNextValue())) { // NON-NLS
                    status = TestResultStatus.SKIPPED;
                }
            } else if ("failedSince".equals(localName)) { // NON-NLS
                if (!"0".equals(readNextValue()) && !TestResultStatus.SKIPPED.equals(status)) {
                    status = TestResultStatus.FAILED;
                }
            } else if ("errorStackTrace".equals(localName)) { // NON-NLS
                status = TestResultStatus.FAILED;
                stackTraceStr = "";
                if (peek() instanceof Characters) {
                    stackTraceStr = readNextValue();
                    int index = stackTraceStr.indexOf("at ");
                    if (index >= 0) {
                        errorType = stackTraceStr.substring(0, index);
                    }
                }
            } else if ("errorDetails".equals(localName)) { // NON-NLS
                status = TestResultStatus.FAILED;
                errorMsg = readNextValue();
                int index = stackTraceStr.indexOf(':');
                if (index >= 0) {
                    errorType = stackTraceStr.substring(0, index);
                }
                if ((hpRunnerType.equals(HPRunnerType.UFT)|| hpRunnerType.equals(HPRunnerType.UFT_MBT)) && StringUtils.isNotEmpty(errorMsg)) {
                    parseUftErrorMessages();
                }
            }
        } else if (event instanceof EndElement) {
            EndElement element = (EndElement) event;
            String localName = element.getName().getLocalPart();

            if ("case".equals(localName)) { // NON-NLS
                errorMsg = StringUtils.length(errorMsg) > ERROR_MESSAGE_MAX_SIZE ? StringUtils.abbreviate(errorMsg,ERROR_MESSAGE_MAX_SIZE) : errorMsg;
                stackTraceStr = StringUtils.length(stackTraceStr) > ERROR_DETAILS_MAX_SIZE ? StringUtils.abbreviate(errorMsg,ERROR_DETAILS_MAX_SIZE) : stackTraceStr;
                TestError testError = new TestError(stackTraceStr, errorType, errorMsg);

                if(this.testParserRegEx != null){
                    splitTestNameByPattern();
                }
                if (hpRunnerType.equals(HPRunnerType.UFT_MBT) && StringUtils.isNotEmpty(uftResultFilePath)) {
                    try {
                        uftResultData = UftTestResultsUtils.getMBTData(new File(uftResultFilePath));
                    } catch (Exception e) {
                        logger.error("Failed to get MBT Data which includes steps results", e);
                    }
                }
                if (stripPackageAndClass) {
                    //workaround only for UFT - we do not want packageName="All-Tests" and className="&lt;None>" as it comes from JUnit report
                    addItem(new JUnitTestResult(moduleName, "", "", testName, status, testDuration, buildStarted, testError, externalURL, description, hpRunnerType,this.externalRunId, uftResultData, octaneSupportsSteps));
                } else {
                    addItem(new JUnitTestResult(moduleName, packageName, className, testName, status, testDuration, buildStarted, testError, externalURL, description, hpRunnerType,this.externalRunId, uftResultData, octaneSupportsSteps));
                }
            } else if ("suites".equals(localName)) {
                finalizeCodelessTests();
            }
        }
    }

    // parse codeless test parts. the codeless junit result is different than the uft. so, its processing is different:
    // 1) in codeless, for each test,
    // a separate result file is generated. each "suite" section in the file matches an iteration. each "case" section matches
    // a unit in an iteration. so, in order to process a test run, we need to process all its iterations one by one and add
    // the test at the close of the "suites" element. after each iteration is processed, we need to update the test duration
    // and status if there is a change
    // 2) the test's name is taken from the file name
    private void handleCodelessTest(XMLEvent event) throws XMLStreamException, IOException, InterruptedException {
        if (event instanceof StartElement) {
            StartElement element = (StartElement) event;
            String localName = element.getName().getLocalPart();
            if ("file".equals(localName)) {  // NON-NLS
                filePath = peekNextValue();
                if(!checkIsCodelessTestResult(filePath)) {
                    testingToolType = TestingToolType.UFT;
                    handleJUnitTest(event);
                } else { // start of a new iteration
                    filePath = readNextValue();
                    testingToolType = TestingToolType.CODELESS;
                    String fileName = filePath.substring(filePath.lastIndexOf("\\") + 1);
                    testName = fileName.substring(0, fileName.lastIndexOf("-Report"));
                    readCodelessTestJsonResult(testName, filePath);
                    currentIterationSteps = new ArrayList<>();
                    currentJUnitTestResult = testNameToJunitResultMap.get(testName);
                }
            } else if ("suite".equals(localName)) { // start of iteration
                resetTestData();
            } else if ("case".equals(localName)) { // start of step
                resetCaseData();
                insideCaseElement = true;
            } else if ("duration".equals(localName)) { // NON-NLS
                if (insideCaseElement) {
                    stepDuration = parseTime(readNextValue());
                } else {
                    testDuration = parseTime(readNextValue());
                }
            } else if ("testName".equals(localName)) { //
                stepName = readNextValue();
            } else if ("skipped".equals(localName)) { // NON-NLS
                if ("true".equals(readNextValue())) { // NON-NLS
                    status = TestResultStatus.SKIPPED;
                }
            } else if ("failedSince".equals(localName)) { // NON-NLS
                if (!"0".equals(readNextValue()) && !TestResultStatus.SKIPPED.equals(status)) {
                    status = TestResultStatus.FAILED;
                }
            } else if ("errorStackTrace".equals(localName)) { // NON-NLS
                status = TestResultStatus.FAILED;
                stackTraceStr = "";
                if (peek() instanceof Characters) {
                    stackTraceStr = readNextValue();
                    int index = stackTraceStr.indexOf("at ");
                    if (index >= 0) {
                        errorType = stackTraceStr.substring(0, index);
                    }
                }
            } else if ("errorDetails".equals(localName)) { // NON-NLS
                status = TestResultStatus.FAILED;
                errorMsg = readNextValue();
                int index = stackTraceStr.indexOf(':');
                if (index >= 0) {
                    errorType = stackTraceStr.substring(0, index);
                }
                if ((hpRunnerType.equals(HPRunnerType.UFT)|| hpRunnerType.equals(HPRunnerType.UFT_MBT)) && StringUtils.isNotEmpty(errorMsg)) {
                    parseUftErrorMessages();
                }
            }
        } else if (event instanceof EndElement) {
            EndElement element = (EndElement) event;
            String localName = element.getName().getLocalPart();

            if ("case".equals(localName)) { // end step
                errorMsg = StringUtils.length(errorMsg) > ERROR_MESSAGE_MAX_SIZE ? StringUtils.abbreviate(errorMsg,ERROR_MESSAGE_MAX_SIZE) : errorMsg;
                UftResultStepData stepData = new UftResultStepData(Collections.singletonList(stepName), "", status.toPrettyName(), errorMsg, stepDuration);
                currentIterationSteps.add(stepData);

                insideCaseElement = false;
            } else if ("suite".equals(localName)) { // end of iteration, add the junit result
                UftResultIterationData iterationData = new UftResultIterationData(currentIterationSteps, testDuration);
                TestResultStatus iterationStatus = TestResultStatus.fromPrettyName(calculateIterationStatus(iterationData));
                if(currentJUnitTestResult == null) {
                    List<UftResultIterationData> iterations = new ArrayList<>();
                    iterations.add(iterationData);
                    TestError testError = null;
                    if(iterationStatus.equals(TestResultStatus.FAILED)) {
                        testError = new TestError("", "", findFirstError(iterationData));
                    }
                    // strip the test counter from the test name. but leave it in the map as is since test name is not unique
                    String actualTestName = testName.substring(testName.indexOf("_") + 1);
                    currentJUnitTestResult = new JUnitTestResult("", "", "", actualTestName, iterationStatus, testDuration, buildStarted, testError,  "", "", hpRunnerType, this.externalRunId, iterations, octaneSupportsSteps);
                    testNameToJunitResultMap.put(testName, currentJUnitTestResult);
                } else { // new iteration to an existing test result
                    currentJUnitTestResult.setDuration(currentJUnitTestResult.getDuration() + testDuration);
                    currentJUnitTestResult.getUftResultData().add(iterationData);
                    if (iterationStatus.equals(TestResultStatus.FAILED) && !currentJUnitTestResult.getResult().equals(TestResultStatus.FAILED)) {
                        currentJUnitTestResult.setResult(TestResultStatus.FAILED);
                        TestError testError = new TestError(stackTraceStr, errorType, findFirstError(iterationData));
                        currentJUnitTestResult.setTestError(testError);
                    }
                }
                testingToolType = TestingToolType.UFT;
            } else if ("suites".equals(localName)) {
                finalizeCodelessTests();
            }
        }
    }

    private void finalizeCodelessTests() {
        if (!testNameToJunitResultMap.isEmpty()) {
            // add parameters to all the units
            addParametersToUnitsResponse();
            testNameToJunitResultMap.values().forEach(this::addItem);
        }
    }

    private void resetTestData() {
        packageName = "";
        className = "";
        testName = "";
        testDuration = 0;
        status = TestResultStatus.PASSED;
        stackTraceStr = "";
        errorType = "";
        errorMsg = "";
        externalURL = "";
        description = "";
        uftResultFilePath = "";
        moduleName = moduleNameFromFile;
        uftResultData = null;
    }

    private void resetCaseData() {
        stepDuration = 0;
        status = TestResultStatus.PASSED;
        stackTraceStr = "";
        errorType = "";
        errorMsg = "";
        insideCaseElement = false;
        stepName = "";
    }

    private String calculateIterationStatus(UftResultIterationData iterationData) {
        Set<String> stepStatuses = iterationData.getSteps().stream().map(UftResultStepData::getResult).collect(Collectors.toSet());
        if(stepStatuses.contains(TestResultStatus.FAILED.toPrettyName())) {
            return TestResultStatus.FAILED.toPrettyName();
        } else if (stepStatuses.contains(TestResultStatus.PASSED.toPrettyName())) {
            return TestResultStatus.PASSED.toPrettyName();
        } else {
            return TestResultStatus.SKIPPED.toPrettyName();
        }
    }

    private String findFirstError(UftResultIterationData iterationData) {
        Optional<UftResultStepData> failedStepOptional = iterationData.getSteps().stream().filter(uftResultStepData -> uftResultStepData.getResult().equals(TestResultStatus.FAILED.toPrettyName())).findFirst();
        if(failedStepOptional.isPresent()) {
            return failedStepOptional.get().getMessage();
        }
        return "";
    }

    // codeless test result file path is in the form of c:\Jenkins\workspace\MBT-test-runner-1001-8xepv\codeless_17\1_Codeless Model_00002.json-Report.xml
    private boolean checkIsCodelessTestResult(String resultFilePath) {
        String codelessFolderName = String.format(UftConstants.CODELESS_FOLDER_TEMPLATE, buildId);
        return resultFilePath.contains(SdkConstants.FileSystem.WINDOWS_PATH_SPLITTER + codelessFolderName + SdkConstants.FileSystem.WINDOWS_PATH_SPLITTER);
    }

    private void readCodelessTestJsonResult(String testName, String testFilePath) {
        if(testNameToCodelessResultMap.get(testName) != null) { // codeless json result was already read for a previous iteration
            return;
        }
        String jsonFileName = testFilePath.replace("xml", "json");
        FilePath jsonFilePath = workspace.child(jsonFileName);
        try {
            if(jsonFilePath.exists()) {
                String jsonResult = jsonFilePath.readToString();
                CodelessResult codelessResult = mapper.readValue(jsonResult, CodelessResult.class);
                testNameToCodelessResultMap.put(testName, codelessResult);
            }
        } catch (IOException | InterruptedException e) {
            logger.warn("Failed to read codeless json result file {}", jsonFileName, e);
        }
    }

    private void addParametersToUnitsResponse() {
        testNameToJunitResultMap.entrySet().forEach(entry -> {
            String testName = entry.getKey();
            JUnitTestResult testResult = entry.getValue();
            CodelessResult codelessResult = testNameToCodelessResultMap.get(testName);
            if(codelessResult != null) {
                List<UftResultIterationData> resultIterations = testResult.getUftResultData();
                List<List<CodelessResultUnit>> codelessIterations = codelessResult.getIterations();
                // assume that lists have the same size and iterations are equally ordered
                if(resultIterations.size() == codelessIterations.size()) {
                    for(int i = 0; i < resultIterations.size(); i++) {
                        List<UftResultStepData> steps = resultIterations.get(i).getSteps();
                        List<CodelessResultUnit> currentCodelessUnits = codelessIterations.get(i);
                        // assume that the iterations have the same units size and units are equally ordered
                        if(steps.size() == currentCodelessUnits.size()) {
                            for(int k = 0; k < steps.size(); k++) {
                                UftResultStepData currentStep = steps.get(k);
                                CodelessResultUnit currentUnit = currentCodelessUnits.get(k);
                                mergeParameters(currentStep, currentUnit);
                            }
                        } else {
                            logger.warn("Codeless response for {} iteration {} is inconsistent- # of units does not match", testName, i);
                        }
                    }
                } else {
                    logger.warn("Codeless response for {} is inconsistent- # of iterations does not match", testName);
                }
            }
        });
    }

    private void mergeParameters(UftResultStepData stepData, CodelessResultUnit codelessResultUnit) {
        if(CollectionUtils.isEmpty(codelessResultUnit.getParameters())) {
            return;
        }

        codelessResultUnit.getParameters().forEach(parameter -> {
            UftResultStepParameter uftResultStepParameter = convertParameter(parameter);
            if(parameter.getType().equals("input")) {
                stepData.getInputParameters().add(uftResultStepParameter);
            } else {
                stepData.getOutputParameters().add(uftResultStepParameter);
            }
        });
    }

    private UftResultStepParameter convertParameter(CodelessResultParameter parameter) {
        return new UftResultStepParameter(parameter.getName(), parameter.getValue(), "");
    }

    private void splitTestNameByPattern(){
		Matcher matcher = testParserRegEx.matcher(this.testName);
		if (matcher.find()) {
			this.externalRunId = this.testName.substring(matcher.start());
			this.testName = this.testName.substring(0, matcher.start());
		}
	}


	private void parseUftErrorMessages() {
		try {
			if (StringUtils.isNotEmpty(uftResultFilePath)) {
				String msg = UftTestResultsUtils.getAggregatedErrorMessage(UftTestResultsUtils.getErrorData(new File(uftResultFilePath)));
				if (msg.length() >= 255) {
					msg = msg.substring(0, 250) +" ...";
				}
				if (StringUtils.isNotEmpty(msg)) {
					errorMsg = msg;
				}
			}
		} catch (Exception e) {
			logger.error("Failed to parseUftErrorMessages" + e.getMessage());
		}
	}

	private static String tryGetStormRunnerReportURLFromLog(Collection logLines) {
		//console contains link to report
		//link start with "View report:"
		String VIEW_REPORT_PREFIX = "view report at:";
		for (Object str : logLines) {
			if (str instanceof String && ((String) str).toLowerCase().startsWith(VIEW_REPORT_PREFIX)) {
				return  ((String) str).substring(VIEW_REPORT_PREFIX.length()).trim();
			}
		}
		return "";
	}

	private static String tryGetStormRunnerReportURLFromJunitFile(String path) {
		try {
			String srUrl = null;
			File srReport = new File(path);
			if (srReport.exists()) {
				TestSuite testSuite = DTOFactory.getInstance().dtoFromXmlFile(srReport, TestSuite.class);
				for (Property property : testSuite.getProperties()) {
					if (property.getPropertyName().equals(SRL_REPORT_URL)) {
						srUrl = property.getPropertyValue();
						break;
					};
				}
			}
			return srUrl;
		} catch (Exception e) {
			logger.debug("Failed to getStormRunnerURL: " + e.getMessage());
			return "";
		}
	}
	private String extractValueFromStdout(String stdoutValue, String startString, String endString, String defaultValue) {
		String result = defaultValue;
		int startIndex = stdoutValue.indexOf(startString);
		if (startIndex > 0) {
			int endIndex = stdoutValue.indexOf(endString, startIndex);
			if (endIndex > 0) {
				result = stdoutValue.substring(startIndex + startString.length(), endIndex).trim();
			}
		}
		return result;
	}

	private int getUftTestIndexStart(FilePath workspace, String sharedCheckOutDirectory, String testName) {
		int returnIndex = -1;
		try {
			if (sharedCheckOutDirectory == null) {
				sharedCheckOutDirectory = "";
			}
			String pathToTest;
			if (StringUtils.isEmpty(sharedCheckOutDirectory)) {
				pathToTest = workspace.getRemote();
			} else {
				pathToTest = Paths.get(sharedCheckOutDirectory).isAbsolute() ?
						sharedCheckOutDirectory :
						Paths.get(FilenameUtils.separatorsToSystem(workspace.getRemote()),
								FilenameUtils.separatorsToSystem(sharedCheckOutDirectory))
							 .toFile().getCanonicalPath();
			}


			if (testName.toLowerCase().startsWith(pathToTest.toLowerCase())) {
				returnIndex = pathToTest.length() + 1;
			}
		} catch (Exception e) {
			logger.error(String.format("Failed to getUftTestIndexStart for testName '%s' and sharedCheckOutDirectory '%s' : %s", testName, sharedCheckOutDirectory, e.getMessage()), e);
		}
		return returnIndex;
	}

	private String cleanTestName(String testName) {
		// subfolder\testname
		if (testName.contains("\\")) {
			return testName.substring(testName.lastIndexOf('\\') + 1);
		}
		if (testName.contains("/")) {
			return testName.substring(testName.lastIndexOf('/') + 1);
		}
		return testName;
	}

	private String jenkinsTestNameFormat(String testName) {
		if (StringUtils.isEmpty(testName)) {
			return testName;
		}
		return testName.trim().replaceAll("[-:\\ ,()/\\[\\]]", "_").replace('#', '_').replace('\\', '_').replace('.', '_');
	}

	private String jenkinsTestClassFormat(String className) {
		if (StringUtils.isEmpty(className)) {
			return className;
		}
		return className.trim().replaceAll("[:/<>]", "_").replace("\\", "_").replace(" ", "%20");
	}
}
