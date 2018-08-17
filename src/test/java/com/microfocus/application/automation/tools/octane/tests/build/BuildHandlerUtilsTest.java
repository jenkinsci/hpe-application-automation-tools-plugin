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

package com.microfocus.application.automation.tools.octane.tests.build;

import com.microfocus.application.automation.tools.octane.tests.CopyResourceSCM;
import com.microfocus.application.automation.tools.octane.tests.TestUtils;
import hudson.matrix.*;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.Maven;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.ToolInstallations;

import java.util.HashMap;

@SuppressWarnings({"squid:S2699", "squid:S3658", "squid:S2259", "squid:S1872", "squid:S2925", "squid:S109", "squid:S1607", "squid:S2701"})
public class BuildHandlerUtilsTest {

	@ClassRule
	public static final JenkinsRule jenkins = new JenkinsRule();

	@Test
	public void testMatrixBuildType() throws Exception {
		MatrixProject matrixProject = jenkins.createProject(MatrixProject.class, "matrix-project");
		matrixProject.setAxes(new AxisList(new Axis("OS", "Linux", "Windows")));
		MatrixBuild build = (MatrixBuild) TestUtils.runAndCheckBuild(matrixProject);

		BuildDescriptor descriptor = BuildHandlerUtils.getBuildType(build);
		Assert.assertEquals("matrix-project", descriptor.getJobId());
		Assert.assertEquals("", descriptor.getSubType());

		Assert.assertEquals("matrix-project", BuildHandlerUtils.getProjectFullName(build));

		HashMap<String, String> expectedType = new HashMap<>();
		expectedType.put("OS=Linux", "matrix-project/OS=Linux");
		expectedType.put("OS=Windows", "matrix-project/OS=Windows");

		for (MatrixRun run : build.getExactRuns()) {
			descriptor = BuildHandlerUtils.getBuildType(run);
			Assert.assertEquals("matrix-project", descriptor.getJobId());
			String fullName = expectedType.remove(descriptor.getSubType());
			Assert.assertEquals(fullName, BuildHandlerUtils.getProjectFullName(run));
		}
		Assert.assertTrue(expectedType.isEmpty());
	}

	@Test
	public void testMavenBuildType() throws Exception {
		MavenModuleSet project = jenkins.createProject(MavenModuleSet.class, "maven-project");
		project.runHeadless();

		Maven.MavenInstallation mavenInstallation = ToolInstallations.configureMaven3();

		project.setMaven(mavenInstallation.getName());
		project.setGoals(String.format("clean test --settings \"%s\\conf\\settings.xml\" -Dmaven.repo.local=\"%s\\m2-temp\" -Dmaven.test.failure.ignore=true", System.getenv("MAVEN_HOME"), System.getenv("TEMP")));
		project.setScm(new CopyResourceSCM("/helloWorldRoot"));
		MavenModuleSetBuild build = (MavenModuleSetBuild) TestUtils.runAndCheckBuild(project);

		BuildDescriptor descriptor = BuildHandlerUtils.getBuildType(build);
		Assert.assertEquals("maven-project", descriptor.getJobId());
		Assert.assertEquals("", descriptor.getSubType());

		Assert.assertEquals("maven-project", BuildHandlerUtils.getProjectFullName(build));
	}

	@Test
	public void testFallbackBuildType() throws Exception {
		FreeStyleProject project = jenkins.createFreeStyleProject("freestyle-project");
		FreeStyleBuild build = (FreeStyleBuild) TestUtils.runAndCheckBuild(project);
		BuildDescriptor descriptor = BuildHandlerUtils.getBuildType(build);
		Assert.assertEquals("freestyle-project", descriptor.getJobId());
		Assert.assertEquals("", descriptor.getSubType());
		Assert.assertEquals("freestyle-project", BuildHandlerUtils.getProjectFullName(build));
	}
}
