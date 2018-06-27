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
