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

package com.microfocus.application.automation.tools.octane.tests.xml;

import com.microfocus.application.automation.tools.octane.tests.TestResultContainer;
import com.microfocus.application.automation.tools.octane.tests.TestResultIterable;
import com.microfocus.application.automation.tools.octane.tests.junit.JUnitTestResult;
import com.microfocus.application.automation.tools.octane.tests.TestResultIterator;
import com.microfocus.application.automation.tools.octane.tests.TestUtils;
import com.microfocus.application.automation.tools.octane.tests.detection.ResultFields;
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"squid:S2698","squid:S2699"})
public class TestResultXmlWriterTest {

	@ClassRule
	public static final JenkinsRule jenkins = new JenkinsRule();

	private TestResultContainer container;

	@Before
	public void initialize() throws IOException {
		List<TestResult> testResults = new ArrayList<>();
		testResults.add(new JUnitTestResult("module", "package", "class", "testName", TestResultStatus.PASSED, 1l, 2l, null, null));
		container = new TestResultContainer(testResults.iterator(), new ResultFields());
	}

	@Test
	public void testNonEmptySubType() throws Exception {
		MatrixProject matrixProject = jenkins.createProject(MatrixProject.class, "matrix-project");

		matrixProject.setAxes(new AxisList(new Axis("OS", "Linux")));
		MatrixBuild build = (MatrixBuild) TestUtils.runAndCheckBuild(matrixProject);
		Assert.assertEquals(1, build.getExactRuns().size());
		assertBuildType(build.getExactRuns().get(0), "matrix-project", "OS=Linux");
	}

	@Test
	public void testEmptySubType() throws Exception {
		FreeStyleProject project = jenkins.createFreeStyleProject("freestyle-project");
		FreeStyleBuild build = (FreeStyleBuild) TestUtils.runAndCheckBuild(project);
		assertBuildType(build, "freestyle-project", null);
	}

	private void assertBuildType(AbstractBuild build, String buildType, String subType) throws IOException, XMLStreamException, InterruptedException {
		FilePath testXml = new FilePath(build.getWorkspace(), "test.xml");
		TestResultXmlWriter xmlWriter = new TestResultXmlWriter(testXml, build);
		xmlWriter.writeResults(container);
		xmlWriter.close();

		TestResultIterator iterator = new TestResultIterable(new File(testXml.getRemote())).iterator();
		Assert.assertEquals(buildType, iterator.getJobId());
		Assert.assertEquals(subType, iterator.getSubType());
	}
}
