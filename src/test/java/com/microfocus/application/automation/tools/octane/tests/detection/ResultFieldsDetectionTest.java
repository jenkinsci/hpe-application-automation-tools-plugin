/*
 *  Certain versions of software accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 *  This software was acquired by Micro Focus on September 1, 2017, and is now
 *  offered by OpenText.
 *  Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical
 *  in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the
 *  property of their respective owners.
 *  OpenText is a trademark of Open Text.
 *  __________________________________________________________________
 *  MIT License
 *
 *  Copyright 2012-2025 Open Text.
 *
 *  The only warranties for products and services of Open Text and
 *  its affiliates and licensors ("Open Text") are as may be set forth
 *  in the express warranty statements accompanying such products and services.
 *  Nothing herein should be construed as constituting an additional warranty.
 *  Open Text shall not be liable for technical or editorial errors or
 *  omissions contained herein. The information contained herein is subject
 *  to change without notice.
 *
 *  Except as specifically indicated otherwise, this document contains
 *  confidential information and a valid license is required for possession,
 *  use or copying. If this work is provided to the U.S. Government,
 *  consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 *  Computer Software Documentation, and Technical Data for Commercial Items are
 *  licensed to the U.S. Government under vendor's standard commercial license.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ___________________________________________________________________
 */
package com.microfocus.application.automation.tools.octane.tests.detection;

import com.microfocus.application.automation.tools.octane.OctanePluginTestBase;
import com.microfocus.application.automation.tools.octane.tests.CopyResourceSCM;
import com.microfocus.application.automation.tools.octane.tests.ExtensionUtil;
import com.microfocus.application.automation.tools.octane.tests.TestUtils;
import com.microfocus.application.automation.tools.octane.tests.junit.JUnitExtension;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.Maven;
import hudson.tasks.junit.JUnitResultArchiver;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.ToolInstallations;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileReader;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ResultFieldsDetectionTest  extends OctanePluginTestBase {

	private static FreeStyleProject project;

	private static ResultFieldsDetectionService detectionService;

	@Before
	public void setUp() throws Exception {

		project = rule.createFreeStyleProject("junit - job-" + UUID.randomUUID().toString());
		JUnitExtension junitExtension = ExtensionUtil.getInstance(rule, JUnitExtension.class);
		detectionService = Mockito.mock(ResultFieldsDetectionService.class);
		junitExtension._setResultFieldsDetectionService(detectionService);

		Maven.MavenInstallation mavenInstallation = ToolInstallations.configureMaven35();
		//Maven.MavenInstallation mavenInstallation = new Maven.MavenInstallation("default-system-maven", TestUtils.getMavenHome(), JenkinsRule.NO_PROPERTIES);

		//project.getBuildersList().add(new Maven(String.format("--settings \"%s\\conf\\settings.xml\" -U test",TestUtils.getMavenHome()), mavenInstallation.getName(), null, null, "-Dmaven.test.failure.ignore=true"));
		project.getBuildersList().add(new Maven(String.format("--settings \"%s\\conf\\settings.xml\" -U test -Dmaven.repo.local=%s\\m2-temp",
				TestUtils.getMavenHome(),System.getenv("TEMP")), mavenInstallation.getName(), null, null, "-Dmaven.test.failure.ignore=true"));

		project.setScm(new CopyResourceSCM("/helloWorldRoot"));
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
