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
package com.microfocus.application.automation.tools.octane.tests;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOBase;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.OctaneResultAbridged;
import com.hp.octane.integrations.dto.connectivity.OctaneTaskAbridged;
import com.microfocus.application.automation.tools.model.OctaneServerSettingsModel;
import com.microfocus.application.automation.tools.octane.configuration.ConfigurationService;
import com.microfocus.application.automation.tools.octane.tests.junit.JUnitTestResult;
import com.microfocus.application.automation.tools.octane.tests.junit.TestResultStatus;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.util.Secret;
import org.junit.Assert;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TestUtils {

	static Set<String> helloWorldTests = new HashSet<>();

	static {
		helloWorldTests.add(testSignature("helloWorld", "hello", "HelloWorldTest", "testOne", TestResultStatus.PASSED));
		helloWorldTests.add(testSignature("helloWorld", "hello", "HelloWorldTest", "testTwo", TestResultStatus.PASSED));
		helloWorldTests.add(testSignature("helloWorld", "hello", "HelloWorldTest", "testThree", TestResultStatus.SKIPPED));
	}

	public static void createDummyConfiguration(){
		ConfigurationService.configurePlugin(new OctaneServerSettingsModel(
				"http://localhost:8008/ui/?p=1001/1002","username",
				Secret.fromString("password"),null));
	}

	public static AbstractBuild runAndCheckBuild(AbstractProject project) throws Exception {
		AbstractBuild build = (AbstractBuild) project.scheduleBuild2(0).get();
		if (!build.getResult().isBetterOrEqualTo(Result.UNSTABLE)) { // avoid expensive build.getLog() until condition is met
			Assert.fail("Build status: " + build.getResult() + ", log follows:\n" + build.getLog());
		}
		return build;
	}

	private static String testSignature(JUnitTestResult testResult) {
		return testSignature(testResult.getModuleName(), testResult.getPackageName(), testResult.getClassName(),
				testResult.getTestName(), testResult.getResult());
	}

	static String testSignature(String moduleName, String packageName, String className, String testName, TestResultStatus status) {
		return moduleName + "#" + packageName + "#" + className + "#" + testName + "#" + status.toPrettyName() + "#";
	}

	static void matchTests(TestResultIterable testResultIterable, String buildType, long started, Set<String>... expectedTests) {
		Set<String> copy = new HashSet<>();
		for (Set<String> expected : expectedTests) {
			copy.addAll(expected);
		}
		TestResultIterator it = testResultIterable.iterator();
		Assert.assertEquals(buildType, it.getJobId());
		while (it.hasNext()) {
			JUnitTestResult testResult = it.next();
			String testSignature = TestUtils.testSignature(testResult);
			Assert.assertTrue("Not found: " + testSignature + " in " + copy, copy.remove(testSignature));
			Assert.assertEquals("Start time differs", started, testResult.getStarted());
		}
		Assert.assertTrue("More tests expected: " + copy.toString(), copy.isEmpty());
	}

	static public String getMavenHome() {
		String result = System.getenv("MAVEN_HOME") != null && !System.getenv("MAVEN_HOME").isEmpty() ?
				System.getenv("MAVEN_HOME") :
				System.getenv("M2_HOME");
		if (result == null || result.isEmpty()) {
			throw new IllegalStateException("nor MAVEN_HOME neither M2_HOME is defined, won't run");
		}
		return result;
	}

	public static <T extends DTOBase> T sendTask(String url, Class<T> targetType) {
		DTOFactory dtoFactory = DTOFactory.getInstance();
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		OctaneTaskAbridged task = dtoFactory.newDTO(OctaneTaskAbridged.class)
				.setMethod(com.hp.octane.integrations.dto.connectivity.HttpMethod.GET)
				.setUrl(url)
				.setHeaders(headers);
		OctaneResultAbridged taskResult = OctaneSDK.getClients().get(0).getTasksProcessor().execute(task);

		T result = null;
		if(targetType!=null){
			result = dtoFactory.dtoFromJson(taskResult.getBody(), targetType);
		}

		return result;
	}
}
