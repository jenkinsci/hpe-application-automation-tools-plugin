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

package com.microfocus.application.automation.tools.octane.tests.xml;

import com.microfocus.application.automation.tools.model.OctaneServerSettingsModel;
import com.microfocus.application.automation.tools.octane.OctaneServerMock;
import com.microfocus.application.automation.tools.octane.OctanePluginTestBase;
import com.microfocus.application.automation.tools.octane.configuration.ConfigurationService;
import com.microfocus.application.automation.tools.octane.tests.TestResultContainer;
import com.microfocus.application.automation.tools.octane.tests.TestResultIterable;
import com.microfocus.application.automation.tools.octane.tests.TestResultIterator;
import com.microfocus.application.automation.tools.octane.tests.TestUtils;
import com.microfocus.application.automation.tools.octane.tests.detection.ResultFields;
import com.microfocus.application.automation.tools.octane.tests.junit.JUnitTestResult;
import com.microfocus.application.automation.tools.octane.tests.junit.TestResultStatus;
import com.microfocus.application.automation.tools.octane.tests.testResult.TestResult;
import hudson.FilePath;
import hudson.matrix.Axis;
import hudson.matrix.AxisList;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.util.Secret;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/***
 * Tests on TestResultXmlWriter
 */
@SuppressWarnings({"squid:S2698", "squid:S2699"})
public class TestResultXmlWriterTest extends OctanePluginTestBase {
	private static TestResultContainer container;

	@BeforeClass
	public static void initialize() {
		List<TestResult> testResults = new ArrayList<>();
		testResults.add(new JUnitTestResult("module", "package", "class", "testName", TestResultStatus.PASSED, 1, 2, null, null, null));
		container = new TestResultContainer(testResults.iterator(), new ResultFields());
		OctaneServerMock serverMock = OctaneServerMock.getInstance();
		OctaneServerSettingsModel model = new OctaneServerSettingsModel(
				"http://127.0.0.1:" + serverMock.getPort() + "/ui?p=1001",
				"username",
				Secret.fromString("password"),
				"");
		ConfigurationService.configurePlugin(model);
	}

	@Test
	public void testFreestyleProject() throws Exception {
		FreeStyleProject project = rule.createFreeStyleProject("freestyle-project");
		FreeStyleBuild build = (FreeStyleBuild) TestUtils.runAndCheckBuild(project);
		assertBuildType(build, "freestyle-project", null);
	}

	@Test
	public void testMatrixProject() throws Exception {
		MatrixProject matrixProject = rule.createProject(MatrixProject.class, "matrix-project");
		matrixProject.setAxes(new AxisList(new Axis("OS", "Linux")));
		MatrixBuild build = (MatrixBuild) TestUtils.runAndCheckBuild(matrixProject);
		Assert.assertEquals(1, build.getExactRuns().size());
		assertBuildType(build.getExactRuns().get(0), "matrix-project", "OS=Linux");
	}

	private void assertBuildType(AbstractBuild build, String jobName, String matrixExtendedName) throws IOException, XMLStreamException, InterruptedException {
		Assert.assertNotNull(build);
		Assert.assertNotNull(build.getWorkspace());
		FilePath testXml = new FilePath(build.getWorkspace(), "test.xml");
		TestResultXmlWriter xmlWriter = new TestResultXmlWriter(testXml, build);
		xmlWriter.writeResults(container);
		xmlWriter.close();

		TestResultIterator iterator = new TestResultIterable(new File(testXml.getRemote())).iterator();
		Assert.assertEquals(jobName + (matrixExtendedName == null ? "" : "/" + matrixExtendedName), iterator.getJobId());
		Assert.assertEquals(matrixExtendedName, iterator.getSubType());
	}
}
