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

package com.microfocus.application.automation.tools.octane.tests.detection;

import com.microfocus.application.automation.tools.octane.tests.CopyResourceSCM;
import com.microfocus.application.automation.tools.octane.tests.ExtensionUtil;
import com.microfocus.application.automation.tools.octane.tests.TestUtils;
import com.microfocus.application.automation.tools.octane.tests.junit.JUnitExtension;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.Maven;
import hudson.tasks.junit.JUnitResultArchiver;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.ToolInstallations;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileReader;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class ResultFieldsDetectionTest {

	@Rule
	public final JenkinsRule rule = new JenkinsRule();

	private static FreeStyleProject project;

	private static ResultFieldsDetectionService detectionService;

	private final JenkinsRule.WebClient jClient = rule.createWebClient();

	@Before
	public void setUp() throws Exception {

		project = rule.createFreeStyleProject("junit - job");
		JUnitExtension junitExtension = ExtensionUtil.getInstance(rule, JUnitExtension.class);
		detectionService = Mockito.mock(ResultFieldsDetectionService.class);
		junitExtension._setResultFieldsDetectionService(detectionService);

		Maven.MavenInstallation mavenInstallation = ToolInstallations.configureMaven3();
		//Maven.MavenInstallation mavenInstallation = new Maven.MavenInstallation("default-system-maven", System.getenv("MAVEN_HOME"), JenkinsRule.NO_PROPERTIES);

		//project.getBuildersList().add(new Maven(String.format("--settings \"%s\\conf\\settings.xml\" -U test",System.getenv("MAVEN_HOME")), mavenInstallation.getName(), null, null, "-Dmaven.test.failure.ignore=true"));
		project.getBuildersList().add(new Maven(String.format("--settings \"%s\\conf\\settings.xml\" -U test -Dmaven.repo.local=%s\\m2-temp",
				System.getenv("MAVEN_HOME"),System.getenv("TEMP")), mavenInstallation.getName(), null, null, "-Dmaven.test.failure.ignore=true"));

		project.setScm(new CopyResourceSCM("/helloWorldRoot"));

		TestUtils.createDummyConfiguration();
	}

	@Test
	public void testDetectionNotRun() throws Exception {
		//there is no test publisher set up in this project, detection will not run
		AbstractBuild build = TestUtils.runAndCheckBuild(project);
		verify(detectionService, Mockito.never()).getDetectedFields(build);
	}

	@Test
	public void testDetectionRunOnce() throws Exception {
		project.getPublishersList().add(new JUnitResultArchiver("**/target/surefire-reports/*.xml"));
		AbstractBuild build = TestUtils.runAndCheckBuild(project);
		verify(detectionService, Mockito.times(1)).getDetectedFields(build);
	}

	@Test
	public void testDetectedFieldsInXml() throws Exception {
		when(detectionService.getDetectedFields(any(AbstractBuild.class))).thenReturn(new ResultFields("HOLA", "CIAO", "SALUT"));
		project.getPublishersList().add(new JUnitResultArchiver("**/target/surefire-reports/*.xml"));
		AbstractBuild build = TestUtils.runAndCheckBuild(project);

		File mqmTestsXml = new File(build.getRootDir(), "mqmTests.xml");
		ResultFieldsXmlReader xmlReader = new ResultFieldsXmlReader(new FileReader(mqmTestsXml));
		ResultFields resultFields = xmlReader.readXml().getResultFields();

		Assert.assertNotNull(resultFields);
		Assert.assertEquals("HOLA", resultFields.getFramework());
		Assert.assertEquals("CIAO", resultFields.getTestingTool());
		Assert.assertEquals("SALUT", resultFields.getTestLevel());
	}

	@Test
	public void testNoDetectedFieldsInXml() throws Exception {
		when(detectionService.getDetectedFields(any(AbstractBuild.class))).thenReturn(null);
		project.getPublishersList().add(new JUnitResultArchiver("**/target/surefire-reports/*.xml"));
		AbstractBuild build = TestUtils.runAndCheckBuild(project);

		File mqmTestsXml = new File(build.getRootDir(), "mqmTests.xml");
		ResultFieldsXmlReader xmlReader = new ResultFieldsXmlReader(new FileReader(mqmTestsXml));
		ResultFields resultFields = xmlReader.readXml().getResultFields();
		;

		Assert.assertNull(resultFields.getFramework());
		Assert.assertNull(resultFields.getTestingTool());
		Assert.assertNull(resultFields.getTestLevel());
	}

	@Test
	public void testEmptyDetectedFieldsInXml() throws Exception {
		when(detectionService.getDetectedFields(any(AbstractBuild.class))).thenReturn(new ResultFields(null, null, null));
		project.getPublishersList().add(new JUnitResultArchiver("**/target/surefire-reports/*.xml"));
		AbstractBuild build = TestUtils.runAndCheckBuild(project);

		File mqmTestsXml = new File(build.getRootDir(), "mqmTests.xml");
		ResultFieldsXmlReader xmlReader = new ResultFieldsXmlReader(new FileReader(mqmTestsXml));
		ResultFields resultFields = xmlReader.readXml().getResultFields();

		Assert.assertNull(resultFields.getFramework());
		Assert.assertNull(resultFields.getTestingTool());
		Assert.assertNull(resultFields.getTestLevel());
	}

	/**
	 * We do not detect Junit yet.
	 */
	@Test
	public void testNotDetectableConfigurationInXml() throws Exception {
		project.getPublishersList().add(new JUnitResultArchiver("**/target/surefire-reports/*.xml"));
		AbstractBuild build = TestUtils.runAndCheckBuild(project);

		File mqmTestsXml = new File(build.getRootDir(), "mqmTests.xml");
		ResultFieldsXmlReader xmlReader = new ResultFieldsXmlReader(new FileReader(mqmTestsXml));
		ResultFields resultFields = xmlReader.readXml().getResultFields();

		Assert.assertNull(resultFields.getFramework());
		Assert.assertNull(resultFields.getTestingTool());
		Assert.assertNull(resultFields.getTestLevel());
	}
}
