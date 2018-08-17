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

package com.microfocus.application.automation.tools.octane.tests.xml;

import com.microfocus.application.automation.tools.octane.configuration.ConfigurationService;
import com.microfocus.application.automation.tools.octane.tests.TestResultContainer;
import com.microfocus.application.automation.tools.octane.tests.build.BuildDescriptor;
import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import com.microfocus.application.automation.tools.octane.tests.detection.ResultFields;
import com.microfocus.application.automation.tools.octane.tests.testResult.TestResult;
import hudson.FilePath;
import hudson.model.Run;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * Save results to mqmTests.xml in XML format
 */
@SuppressWarnings("all")
public class TestResultXmlWriter {

	private FilePath targetPath;
	private BuildDescriptor buildDescriptor;

	private XMLStreamWriter writer;
	private OutputStream outputStream;

	public TestResultXmlWriter(FilePath targetPath, BuildDescriptor buildDescriptor) {
		this.targetPath = targetPath;
		this.buildDescriptor = buildDescriptor;
	}

	public TestResultXmlWriter(FilePath targetPath, Run build) {
		this.targetPath = targetPath;
		this.buildDescriptor = BuildHandlerUtils.getBuildType(build);
	}

	public void writeResults(TestResultContainer testResultContainer) throws InterruptedException, XMLStreamException, IOException {
		if (testResultContainer != null) {
			ResultFields resultFields = testResultContainer.getResultFields();
			initialize(resultFields);

			Iterator<TestResult> testResults = testResultContainer.getIterator();
			while (testResults.hasNext()) {
				TestResult testResult = testResults.next();
				testResult.writeXmlElement(writer);
			}
		}
	}

	public void close() throws XMLStreamException {
		if (outputStream != null) {
			writer.writeEndElement(); // test_runs
			writer.writeEndElement(); // test_result
			writer.writeEndDocument();
			writer.close();
			IOUtils.closeQuietly(outputStream);
		}
	}

	private void initialize(ResultFields resultFields) throws IOException, InterruptedException, XMLStreamException {
		if (outputStream == null) {
			outputStream = targetPath.write();
			writer = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream);
			writer.writeStartDocument();

			writer.writeStartElement("test_result");
			writer.writeStartElement("build");
			writer.writeAttribute("server_id", ConfigurationService.getModel().getIdentity());
			writer.writeAttribute("job_id", buildDescriptor.getJobId());
			writer.writeAttribute("job_name", buildDescriptor.getJobName());
			writer.writeAttribute("build_id", buildDescriptor.getBuildId());
			writer.writeAttribute("build_name", buildDescriptor.getBuildName());
			if (!StringUtils.isEmpty(buildDescriptor.getSubType())) {
				writer.writeAttribute("sub_type", buildDescriptor.getSubType());
			}
			writer.writeEndElement(); // build
			writeFields(resultFields);
			writer.writeStartElement("test_runs");
		}
	}

	private void writeFields(ResultFields resultFields) throws XMLStreamException {
		if (resultFields != null) {
			writer.writeStartElement("test_fields");
			writeField("Framework", resultFields.getFramework());
			writeField("Test_Level", resultFields.getTestLevel());
			writeField("Testing_Tool_Type", resultFields.getTestingTool());
			writeField("Test_Type", resultFields.getTestType());
			writer.writeEndElement();
		}
	}

	private void writeField(String type, String value) throws XMLStreamException {
		if (StringUtils.isNotEmpty(value)) {
			writer.writeStartElement("test_field");
			writer.writeAttribute("type", type);
			writer.writeAttribute("value", value);
			writer.writeEndElement();
		}
	}
}
