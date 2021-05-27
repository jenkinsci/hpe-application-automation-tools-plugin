/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2021 Micro Focus or one of its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.octane.tests.junit;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.tests.Property;
import com.hp.octane.integrations.dto.tests.TestSuite;
import com.hp.octane.integrations.executor.converters.MfUftConverter;
import com.hp.octane.integrations.uft.ufttestresults.UftTestResultsUtils;
import com.hp.octane.integrations.utils.SdkConstants;
import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import com.microfocus.application.automation.tools.octane.tests.HPRunnerType;
import com.microfocus.application.automation.tools.octane.tests.xml.AbstractXmlIterator;
import hudson.FilePath;
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
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
	private long duration;
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

	public JUnitXmlIterator(InputStream read, List<ModuleDetection> moduleDetection, FilePath workspace, String sharedCheckOutDirectory, String jobName, String buildId, long buildStarted, boolean stripPackageAndClass, HPRunnerType hpRunnerType, String jenkinsRootUrl, Object additionalContext) throws XMLStreamException {
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
		if (event instanceof StartElement) {
			StartElement element = (StartElement) event;
			String localName = element.getName().getLocalPart();
			if ("file".equals(localName)) {  // NON-NLS
				filePath = readNextValue();
				for (ModuleDetection detection : moduleDetection) {
					moduleNameFromFile = moduleName = detection.getModule(new FilePath(new File(filePath)));
					if (moduleName != null) {
						break;
					}
				}
			} else if ("id".equals(localName)) {
				id = readNextValue();
			} else if ("case".equals(localName)) { // NON-NLS
				packageName = "";
				className = "";
				testName = "";
				duration = 0;
				status = TestResultStatus.PASSED;
				stackTraceStr = "";
				errorType = "";
				errorMsg = "";
				externalURL = "";
				description = "";
				uftResultFilePath = "";
				moduleName = moduleNameFromFile;
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
					if (hpRunnerType.equals(HPRunnerType.UFT) && stdoutValue.contains("Test result: Warning")) {
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

                if (hpRunnerType.equals(HPRunnerType.UFT)) {
					if (testName != null && testName.contains("..")) { //resolve existence of ../ - for example c://a/../b => c://b
						testName = new File(testName).getCanonicalPath();
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
						String path = testName.substring(uftTextIndexStart);
						if(path.startsWith(MfUftConverter.MBT_PARENT_SUB_DIR)){//remove MBT prefix
							path = path.substring(MfUftConverter.MBT_PARENT_SUB_DIR.length());
						}
						path = path.replace(SdkConstants.FileSystem.LINUX_PATH_SPLITTER, SdkConstants.FileSystem.WINDOWS_PATH_SPLITTER);
						path = StringUtils.strip(path, SdkConstants.FileSystem.WINDOWS_PATH_SPLITTER);

						//split path to package and name fields
						if (path.contains(SdkConstants.FileSystem.WINDOWS_PATH_SPLITTER)) {
							int testNameStartIndex = path.lastIndexOf(SdkConstants.FileSystem.WINDOWS_PATH_SPLITTER);

							testName = path.substring(testNameStartIndex + 1);
							packageName = path.substring(0, testNameStartIndex);
						} else {
							testName = path;
						}
					}

					String cleanedTestName = cleanTestName(testName);
					boolean testReportCreated = true;
					if (additionalContext != null && additionalContext instanceof List) {
						//test folders are appear in the following format GUITest1[1], while [1] number of test. It possible that tests with the same name executed in the same job
						//by adding [1] or [2] we can differentiate between different instances.
						//We assume that test folders are sorted so in this section, once we found the test folder, we remove it from collection , in order to find the second instance in next iteration
						List<String> createdTests = (List<String>) additionalContext;
						String searchFor = cleanedTestName + "[";
						Optional<String> optional = createdTests.stream().filter(str -> str.startsWith(searchFor)).findFirst();
						if (optional.isPresent()) {
							cleanedTestName = optional.get();
							createdTests.remove(cleanedTestName);
						}
						testReportCreated = optional.isPresent();
					}

					//workspace.createTextTempFile("build" + buildId + "." + cleanTestName(testName) + ".", "", "Created  " + testReportCreated);
					if (testReportCreated) {
						uftResultFilePath = ((List<String>) additionalContext).get(0) +"\\archive\\UFTReport\\" + cleanedTestName + "\\run_results.xml";
						externalURL = jenkinsRootUrl + "job/" + jobName + "/" + buildId + "/artifact/UFTReport/" + cleanedTestName + "/run_results.html";
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
				duration = parseTime(readNextValue());
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
				if (hpRunnerType.equals(HPRunnerType.UFT) && StringUtils.isNotEmpty(errorMsg)) {
					parseUftErrorMessages();
				}
			}
		} else if (event instanceof EndElement) {
			EndElement element = (EndElement) event;
			String localName = element.getName().getLocalPart();

			if ("case".equals(localName)) { // NON-NLS
				TestError testError = new TestError(stackTraceStr, errorType, errorMsg);
				if (stripPackageAndClass) {
					//workaround only for UFT - we do not want packageName="All-Tests" and className="&lt;None>" as it comes from JUnit report
					addItem(new JUnitTestResult(moduleName, "", "", testName, status, duration, buildStarted, testError, externalURL, description));
				} else {
					addItem(new JUnitTestResult(moduleName, packageName, className, testName, status, duration, buildStarted, testError, externalURL, description));
				}
			}
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
					}
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
						Paths.get(workspace.getRemote(), sharedCheckOutDirectory).toFile().getCanonicalPath();
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
