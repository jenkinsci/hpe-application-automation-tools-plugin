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

package com.microfocus.application.automation.tools.octane.tests;

import com.microfocus.application.automation.tools.model.OctaneServerSettingsModel;
import com.microfocus.application.automation.tools.octane.configuration.ConfigurationService;
import com.microfocus.application.automation.tools.octane.tests.junit.JUnitTestResult;
import com.microfocus.application.automation.tools.octane.tests.junit.TestResultStatus;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.util.Secret;
import org.junit.Assert;

import java.util.HashSet;
import java.util.Set;

public class TestUtils {

	static Set<String> helloWorldTests = new HashSet<>();

	static {
		helloWorldTests.add(testSignature("helloWorld", "hello", "HelloWorldTest", "testOne", TestResultStatus.PASSED));
		helloWorldTests.add(testSignature("helloWorld", "hello", "HelloWorldTest", "testTwo", TestResultStatus.FAILED));
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
}
