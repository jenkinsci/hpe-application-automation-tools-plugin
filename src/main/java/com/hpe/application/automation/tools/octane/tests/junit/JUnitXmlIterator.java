/*
 * © Copyright 2013 EntIT Software LLC
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright (c) 2018 Micro Focus Company, L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ___________________________________________________________________
 *
 */

package com.hpe.application.automation.tools.octane.tests.junit;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.tests.Property;
import com.hp.octane.integrations.dto.tests.TestSuite;
import com.hpe.application.automation.tools.octane.executor.OctaneConstants;
import com.hpe.application.automation.tools.octane.tests.HPRunnerType;
import com.hpe.application.automation.tools.octane.tests.xml.AbstractXmlIterator;
import hudson.FilePath;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JUnit result parser and enricher according to HPRunnerType
 */
public class JUnitXmlIterator extends AbstractXmlIterator<JUnitTestResult> {
	private static final Logger logger = LogManager.getLogger(JUnitXmlIterator.class);

	public static final String DASHBOARD_URL = "dashboardUrl";
	private final FilePath workspace;
	private final long buildStarted;
	private final String buildId;
	private final String jobName;
	private final HPRunnerType hpRunnerType;
	private boolean stripPackageAndClass;
	private String moduleName;
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
	private List<ModuleDetection> moduleDetection;
	private String jenkinsRootUrl;
	private String sharedCheckOutDirectory;
	private Object additionalContext;

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

	private String getStormRunnerURL(String path) {

		String srUrl = null;
		File srReport = new File(path);
		if (srReport.exists()) {
			TestSuite testSuite = DTOFactory.getInstance().dtoFromXmlFile(srReport, TestSuite.class);

			for (Property property : testSuite.getProperties()) {
				if (property.getPropertyName().equals(DASHBOARD_URL)) {
					srUrl = property.getPropertyValue();
					break;
				}
			}
		}
		return srUrl;
	}

	@Override
	protected void onEvent(XMLEvent event) throws XMLStreamException, IOException, InterruptedException {
		if (event instanceof StartElement) {
			StartElement element = (StartElement) event;
			String localName = element.getName().getLocalPart();
			if ("file".equals(localName)) {  // NON-NLS
				String path = readNextValue();
				for (ModuleDetection detection : moduleDetection) {
					moduleName = detection.getModule(new FilePath(new File(path)));
					if (moduleName != null) {
						break;
					}
				}
				if (hpRunnerType.equals(HPRunnerType.StormRunnerLoad)) {
					logger.error("HPE Runner: " + hpRunnerType);
					externalURL = getStormRunnerURL(path);
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
			} else if ("className".equals(localName)) { // NON-NLS
				String fqn = readNextValue();
				int p = fqn.lastIndexOf('.');
				className = fqn.substring(p + 1);
				if (p > 0) {
					packageName = fqn.substring(0, p);
				} else {
					packageName = "";
				}
			} else if ("testName".equals(localName)) { // NON-NLS
				testName = readNextValue();

                if (hpRunnerType.equals(HPRunnerType.UFT)) {
                    String myPackageName = packageName;
                    String myClassName = className;
                    String myTestName = testName;
                    packageName = "";
                    className = "";

					if (testName.startsWith(workspace.getRemote())) {
						// if workspace is prefix of the method name, cut it off
						// currently this handling is needed for UFT tests
						int testStartIndex = workspace.getRemote().length() + (sharedCheckOutDirectory == null ? 0 : (sharedCheckOutDirectory.length() + 1));
						String path = testName.substring(testStartIndex);
						path = path.replace(OctaneConstants.General.LINUX_PATH_SPLITTER, OctaneConstants.General.WINDOWS_PATH_SPLITTER);
						path = StringUtils.strip(path, OctaneConstants.General.WINDOWS_PATH_SPLITTER);

						//split path to package and and name fields
						if (path.contains(OctaneConstants.General.WINDOWS_PATH_SPLITTER)) {
							int testNameStartIndex = path.lastIndexOf(OctaneConstants.General.WINDOWS_PATH_SPLITTER);

							testName = path.substring(testNameStartIndex + 1);
							packageName = path.substring(0, testNameStartIndex);
						} else {
							testName = path;
						}
					}

					String cleanedTestName = cleanTestName(testName);
					boolean testReportCreated = true;
					if (additionalContext != null && additionalContext instanceof Set) {
						Set createdTests = (Set) additionalContext;
						testReportCreated = createdTests.contains(cleanedTestName);
					}

					workspace.createTextTempFile("build" + buildId + "." + cleanTestName(testName) + ".", "", "Created  " + testReportCreated);
					if (testReportCreated) {
						externalURL = jenkinsRootUrl + "job/" + jobName + "/" + buildId + "/artifact/UFTReport/" + cleanTestName(testName) + "/run_results.html";
					} else {
						//if UFT didn't created test results page - add reference to Jenkins test results page
						externalURL = jenkinsRootUrl + "job/" + jobName + "/" + buildId + "/testReport/" + myPackageName + "/" + jenkinsTestClassFormat(myClassName) + "/" + jenkinsTestNameFormat(myTestName) + "/";
					}
				} else if (hpRunnerType.equals(HPRunnerType.PerformanceCenter)) {
					externalURL = jenkinsRootUrl + "job/" + jobName + "/" + buildId + "/artifact/performanceTestsReports/pcRun/Report.html";
				} else if (hpRunnerType.equals(HPRunnerType.StormRunnerFunctional)) {
					if (StringUtils.isNotEmpty(id) && additionalContext != null && additionalContext instanceof Map) {
						Map<String, String> testId2Url = (Map) additionalContext;
						if (testId2Url.containsKey(id))
							externalURL = testId2Url.get(id);
					}
				} else if (hpRunnerType.equals(HPRunnerType.StormRunnerLoad)) {
                	//console contains link to report
					//link start with "View Report:"
					String VIEW_REPORT_PREFIX = "View Report: ";
					if (additionalContext != null && additionalContext instanceof Collection) {
						for (Object str : (Collection) additionalContext) {
							if (str != null && str instanceof String && ((String) str).startsWith(VIEW_REPORT_PREFIX)) {
								externalURL = str.toString().replace(VIEW_REPORT_PREFIX, "");
							}
						}
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

			}
		} else if (event instanceof EndElement) {
			EndElement element = (EndElement) event;
			String localName = element.getName().getLocalPart();

			if ("case".equals(localName)) { // NON-NLS
				TestError testError = new TestError(stackTraceStr, errorType, errorMsg);
				if (stripPackageAndClass) {
					//workaround only for UFT - we do not want packageName="All-Tests" and className="&lt;None>" as it comes from JUnit report
					addItem(new JUnitTestResult(moduleName, "", "", testName, status, duration, buildStarted, testError, externalURL));
				} else {
					addItem(new JUnitTestResult(moduleName, packageName, className, testName, status, duration, buildStarted, testError, externalURL));
				}
			}
		}
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