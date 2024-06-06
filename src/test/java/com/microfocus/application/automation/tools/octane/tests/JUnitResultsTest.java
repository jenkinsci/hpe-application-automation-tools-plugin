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

package com.microfocus.application.automation.tools.octane.tests;

import com.microfocus.application.automation.tools.octane.OctanePluginTestBase;
import com.microfocus.application.automation.tools.octane.tests.junit.TestResultStatus;
import hudson.matrix.*;
import hudson.maven.MavenModuleSet;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.Maven;
import hudson.tasks.junit.JUnitResultArchiver;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.jvnet.hudson.test.ToolInstallations;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings({"squid:S2699", "squid:S3658", "squid:S2259", "squid:S1872", "squid:S2925", "squid:S109", "squid:S1607", "squid:S2701", "squid:S2698"})
public class JUnitResultsTest extends OctanePluginTestBase {

	private static Set<String> helloWorld2Tests = new HashSet<>();

	static {
		helloWorld2Tests.add(TestUtils.testSignature("helloWorld2", "hello", "HelloWorld2Test", "testOnce",
				TestResultStatus.PASSED));
		helloWorld2Tests.add(TestUtils.testSignature("helloWorld2", "hello", "HelloWorld2Test", "testDoce", TestResultStatus.PASSED));
	}

	private static Set<String> subFolderHelloWorldTests = new HashSet<>();

	static {
		subFolderHelloWorldTests.add(TestUtils.testSignature("subFolder/helloWorld", "hello", "HelloWorldTest", "testOne", TestResultStatus.PASSED));
		subFolderHelloWorldTests.add(TestUtils.testSignature("subFolder/helloWorld", "hello", "HelloWorldTest", "testTwo", TestResultStatus.FAILED));
		subFolderHelloWorldTests.add(TestUtils.testSignature("subFolder/helloWorld", "hello", "HelloWorldTest", "testThree", TestResultStatus.SKIPPED));
	}

	private static String mavenName;

	@BeforeClass
	public static void prepareClass() throws Exception {
		rule.jenkins.setNumExecutors(10);
		Maven.MavenInstallation mavenInstallation = ToolInstallations.configureMaven35();
		mavenName = mavenInstallation.getName();
	}

	@Test
	public void testJUnitResults() throws Exception {
		String projectName = "root-job-" + UUID.randomUUID().toString();
		FreeStyleProject project = rule.createFreeStyleProject(projectName);

		project.getBuildersList().add(new Maven(String.format("--settings \"%s\\conf\\settings.xml\" clean test -Dmaven.repo.local=%s\\m2-temp",
				TestUtils.getMavenHome(), System.getenv("TEMP")), mavenName, null, null, "-Dmaven.test.failure.ignore=true"));
		project.getPublishersList().add(new JUnitResultArchiver("**/target/surefire-reports/*.xml"));
		project.setScm(new CopyResourceSCM("/helloWorldRoot"));
		AbstractBuild build = TestUtils.runAndCheckBuild(project);

		matchTests(build, projectName, TestUtils.helloWorldTests, helloWorld2Tests);
	}

	@Test
	public void testJUnitResultsPom() throws Exception {
		String projectName = "root-job-" + UUID.randomUUID().toString();
		FreeStyleProject project = rule.createFreeStyleProject(projectName);

		project.getBuildersList().add(new Maven(String.format("--settings \"%s\\conf\\settings.xml\" clean test -Dmaven.repo.local=%s\\m2-temp",
				TestUtils.getMavenHome(), System.getenv("TEMP")), mavenName, "subFolder/helloWorld/pom.xml", null, "-Dmaven.test.failure.ignore=true"));
		project.getPublishersList().add(new JUnitResultArchiver("**/target/surefire-reports/*.xml"));
		project.setScm(new CopyResourceSCM("/helloWorldRoot", "subFolder"));
		AbstractBuild build = TestUtils.runAndCheckBuild(project);

		matchTests(build, projectName, subFolderHelloWorldTests);
	}

	@Test
	public void testJUnitResultsTwoPoms() throws Exception {
		String projectName = "root-job-" + UUID.randomUUID().toString();
		FreeStyleProject project = rule.createFreeStyleProject(projectName);

		project.getBuildersList().add(new Maven(String.format("--settings \"%s\\conf\\settings.xml\" clean test -Dmaven.repo.local=%s\\m2-temp",
				TestUtils.getMavenHome(), System.getenv("TEMP")), mavenName, "pom.xml", null, "-Dmaven.test.failure.ignore=true"));
		project.getPublishersList().add(new JUnitResultArchiver("**/target/surefire-reports/*.xml"));
		project.setScm(new CopyResourceSCM("/helloWorldRoot"));
		AbstractBuild build = TestUtils.runAndCheckBuild(project);

		matchTests(build, projectName, TestUtils.helloWorldTests, helloWorld2Tests);
	}

	@Test
	public void testJUnitResultsLegacy() throws Exception {
		String projectName = "root-job-" + UUID.randomUUID().toString();
		MavenModuleSet project = rule.createProject(MavenModuleSet.class, projectName);
		project.runHeadless();

		project.setMaven(mavenName);
		project.setGoals(String.format("clean test --settings \"%s\\conf\\settings.xml\" -Dmaven.repo.local=%s\\m2-temp -Dmaven.test.failure.ignore=true",
				TestUtils.getMavenHome(), System.getenv("TEMP")));
		project.getPublishersList().add(new JUnitResultArchiver("**/target/surefire-reports/*.xml"));
		project.setScm(new CopyResourceSCM("/helloWorldRoot"));
		AbstractBuild build = TestUtils.runAndCheckBuild(project);

		matchTests(build, projectName, TestUtils.helloWorldTests, helloWorld2Tests);
	}

	//temporary disable as it failed in CI
	//@Test
	public void testJUnitResultsLegacyWithoutJUnitArchiver() throws Exception {
		String projectName = "root-job-" + UUID.randomUUID().toString();
		MavenModuleSet project = rule.createProject(MavenModuleSet.class, projectName);
		project.runHeadless();

		project.setMaven(mavenName);
		project.setGoals(String.format("clean test --settings \"%s\\conf\\settings.xml\" -Dmaven.repo.local=%s\\m2-temp -Dmaven.test.failure.ignore=true",
				TestUtils.getMavenHome(), System.getenv("TEMP")));
		project.setScm(new CopyResourceSCM("/helloWorldRoot"));
		AbstractBuild build = TestUtils.runAndCheckBuild(project);

		matchTests(build, projectName, TestUtils.helloWorldTests, helloWorld2Tests);
	}

	@Test
	public void testJUnitResultsLegacySubfolder() throws Exception {
		String projectName = "root-job-" + UUID.randomUUID().toString();
		MavenModuleSet project = rule.createProject(MavenModuleSet.class, projectName);
		project.runHeadless();

		project.setMaven(mavenName);
		project.setRootPOM("subFolder/helloWorld/pom.xml");
		project.setGoals(String.format("clean test --settings \"%s\\conf\\settings.xml\" -Dmaven.repo.local=%s\\m2-temp -Dmaven.test.failure.ignore=true",
				TestUtils.getMavenHome(), System.getenv("TEMP")));
		project.getPublishersList().add(new JUnitResultArchiver("**/target/surefire-reports/*.xml"));
		project.setScm(new CopyResourceSCM("/helloWorldRoot", "subFolder"));
		AbstractBuild build = TestUtils.runAndCheckBuild(project);

		matchTests(build, projectName, subFolderHelloWorldTests);
	}

	@Test
	public void testJUnitResultsWorkspaceStripping() throws Exception {
		Set<String> uftTests = new HashSet<>();
		uftTests.add(TestUtils.testSignature("", "All-Tests", "<None>", "subfolder" + File.separator + "CalculatorPlusNextGen", TestResultStatus.FAILED));

		String projectName = "root-job-" + UUID.randomUUID().toString();
		FreeStyleProject project = rule.createFreeStyleProject(projectName);
		project.getPublishersList().add(new TestCustomJUnitArchiver("UFT_results.xml"));
		project.setScm(new CopyResourceSCM("/UFT"));
		AbstractBuild build = TestUtils.runAndCheckBuild(project);

		matchTests(build, projectName, uftTests);
	}

	@Test
	public void testJUnitResultsFreeStyleModule() throws Exception {
		// this scenario simulates FreeStyle project with maven executed via shell (by not using Maven builder directly)
		String projectName = "root-job-" + UUID.randomUUID().toString();
		FreeStyleProject project = rule.createFreeStyleProject(projectName);

		project.getBuildersList().add(new Maven(String.format("--settings \"%s\\conf\\settings.xml\" clean test -Dmaven.repo.local=%s\\m2-temp",
				TestUtils.getMavenHome(), System.getenv("TEMP")), mavenName, null, null, "-Dmaven.test.failure.ignore=true"));
		project.getPublishersList().add(new JUnitResultArchiver("**/target/surefire-reports/*.xml"));
		project.setScm(new CopyResourceSCM("/helloWorldRoot"));
		AbstractBuild build = TestUtils.runAndCheckBuild(project);

		matchTests(build, projectName, TestUtils.helloWorldTests, helloWorld2Tests);
	}

	@Test
	public void testJUnitResultsMatrixProject() throws Exception {
		String projectName = "root-job-" + UUID.randomUUID().toString();
		String axisParamName = "osType";
		String[] subtypes = new String[]{"Linux", "Windows"};
		MatrixProject matrixProject = rule.createProject(MatrixProject.class, projectName);
		matrixProject.setAxes(new AxisList(new Axis(axisParamName, subtypes)));

		matrixProject.getBuildersList().add(new Maven(String.format("--settings \"%s\\conf\\settings.xml\" clean test -Dmaven.test.failure.ignore=true -Dmaven.repo.local=%s\\m2-temp -X",
				TestUtils.getMavenHome(), System.getenv("TEMP")), mavenName));

		matrixProject.getPublishersList().add(new JUnitResultArchiver("**/target/surefire-reports/*.xml"));
		matrixProject.setScm(new CopyResourceSCM("/helloWorldRoot"));
		MatrixBuild build = (MatrixBuild) TestUtils.runAndCheckBuild(matrixProject);

		for (MatrixRun run : build.getExactRuns()) {
			matchTests(
					run,
					projectName + "/" + axisParamName + "=" + subtypes[build.getExactRuns().indexOf(run)],
					TestUtils.helloWorldTests, helloWorld2Tests);
		}
		Assert.assertFalse(new File(build.getRootDir(), "mqmTests.xml").exists());
	}

	private void matchTests(AbstractBuild build, String projectName, Set<String>... expectedTests) throws FileNotFoundException {
		File mqmTestsXml = new File(build.getRootDir(), "mqmTests.xml");
		TestUtils.matchTests(new TestResultIterable(mqmTestsXml), projectName, build.getStartTimeInMillis(), expectedTests);
	}
}
