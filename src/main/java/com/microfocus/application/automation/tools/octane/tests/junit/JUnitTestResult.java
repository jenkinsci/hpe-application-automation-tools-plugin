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

import com.hp.octane.integrations.testresults.XmlWritableTestResult;
import com.hp.octane.integrations.utils.SdkStringUtils;
import com.microfocus.application.automation.tools.octane.tests.HPRunnerType;
import com.microfocus.application.automation.tools.octane.tests.detection.MFToolsDetectionExtension;
import org.apache.commons.lang.StringUtils;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.Serializable;

/**
 * Test Run XML writer to mqmTests.xml
 */
final public class JUnitTestResult implements Serializable, XmlWritableTestResult {

    private final static int DEFAULT_STRING_SIZE = 255;
    private final String moduleName;
    private final String packageName;
    private final String className;
    private final String testName;
    private final String description;
    private final TestResultStatus result;
    private final long duration;
    private final long started;
    private final TestError testError;
    private final String externalReportUrl;
    private final HPRunnerType runnerType;


    public JUnitTestResult(String moduleName, String packageName, String className, String testName, TestResultStatus result, long duration, long started, TestError testError, String externalReportUrl, String description, HPRunnerType runnerType) {
        this.moduleName = restrictSize(moduleName, DEFAULT_STRING_SIZE);
        this.packageName = restrictSize(packageName, DEFAULT_STRING_SIZE);
        this.className = restrictSize(className, DEFAULT_STRING_SIZE);
        if (StringUtils.isEmpty(testName)) {
            this.testName = "[noName]";
        } else {
            this.testName = restrictSize(testName, DEFAULT_STRING_SIZE);
        }
        this.result = result;
        this.duration = duration;
        this.started = started;
        this.testError = testError;
        this.externalReportUrl = externalReportUrl;
        this.description = description;
        this.runnerType = runnerType;
    }

    private String restrictSize(String value, int size) {
        String result = value;
        if (value != null && value.length() > size) {
            result = value.substring(0, size);
        }
        return result;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public String getTestName() {
        return testName;
    }

    public TestResultStatus getResult() {
        return result;
    }

    public long getDuration() {
        return duration;
    }

    public long getStarted() {
        return started;
    }

    public TestError getTestError() {
        return testError;
    }

    public String getExternalReportUrl() {return externalReportUrl;}

    @Override
    public void writeXmlElement(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("test_run");
        writer.writeAttribute("module", moduleName);
        writer.writeAttribute("package", packageName);
        writer.writeAttribute("class", className);
        writer.writeAttribute("name", testName);
        writer.writeAttribute("duration", String.valueOf(duration));
        writer.writeAttribute("status", result.toPrettyName());
        writer.writeAttribute("started", String.valueOf(started));
        if(externalReportUrl != null && !externalReportUrl.isEmpty()) {
            writer.writeAttribute("external_report_url", externalReportUrl);
        }
        if (HPRunnerType.UFT_MBT.equals(runnerType)) {
            writer.writeAttribute("run_type", MFToolsDetectionExtension.UFT_MBT);
        }
        if (result.equals(TestResultStatus.FAILED) && testError != null) {
            writer.writeStartElement("error");
            writer.writeAttribute("type", String.valueOf(testError.getErrorType()));
            writer.writeAttribute("message", String.valueOf(testError.getErrorMsg()));
            writer.writeCharacters(testError.getStackTraceStr());
            writer.writeEndElement();
        } else if (testError != null && !SdkStringUtils.isEmpty(testError.getErrorMsg())) {//warning case
            writer.writeStartElement("error");
            writer.writeAttribute("message", String.valueOf(testError.getErrorMsg()));
            writer.writeEndElement();
        }

        if (description != null && !description.isEmpty()) {
            writer.writeStartElement("description");
            writer.writeCharacters(description);
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }
}
