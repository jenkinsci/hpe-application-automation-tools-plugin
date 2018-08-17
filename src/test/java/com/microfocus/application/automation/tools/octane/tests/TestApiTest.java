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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gargoylesoftware.htmlunit.Page;
import com.hp.octane.integrations.api.RestService;
import com.microfocus.application.automation.tools.model.OctaneServerSettingsModel;
import com.microfocus.application.automation.tools.octane.OctaneServerMock;
import com.microfocus.application.automation.tools.octane.client.RetryModel;
import com.microfocus.application.automation.tools.octane.configuration.ConfigurationService;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.Maven;
import hudson.tasks.junit.JUnitResultArchiver;
import hudson.util.Secret;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.eclipse.jetty.server.Request;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.ToolInstallations;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"squid:S2699", "squid:S3658", "squid:S2259", "squid:S1872", "squid:S2925", "squid:S109", "squid:S1607", "squid:S2698"})
public class TestApiTest {

	private static AbstractBuild build;
	private static JenkinsRule.WebClient client;
	private static int octaneServerMockPort;
	private static TestApiPreflightHandler testApiPreflightHandler = new TestApiPreflightHandler();
	private static TestApiPushTestsResultHandler testApiPushTestsResultHandler = new TestApiPushTestsResultHandler();
	private static TestApiPushTestsLogHandler testApiPushTestsLogHandler = new TestApiPushTestsLogHandler();
	private static String sharedSpaceId = TestApiTest.class.getSimpleName();
	private static String testsJobName = "test-api-test";
	private static Long pushTestResultId = 10001L;

	@ClassRule
	final public static JenkinsRule rule = new JenkinsRule();

	@BeforeClass
	public static void init() throws Exception {

		//  prepare client for verifications
		client = rule.createWebClient();

		//  prepare dispatcher mocking logic
		TestDispatcher testDispatcher = ExtensionUtil.getInstance(rule, TestDispatcher.class);
		TestQueue queue = new TestQueue();
		testDispatcher._setTestResultQueue(queue);
		TestListener testListener = ExtensionUtil.getInstance(rule, TestListener.class);
		testListener._setTestResultQueue(queue);
		queue.waitForTicks(1); // needed to avoid occasional interaction with the client we just overrode (race condition)

		RetryModel retryModel = new RetryModel();
		testDispatcher._setRetryModel(retryModel);

		//  prepare Octane Server Mock
		OctaneServerMock octaneServerMock = OctaneServerMock.getInstance();
		octaneServerMockPort = octaneServerMock.getPort();
		octaneServerMock.addTestSpecificHandler(testApiPreflightHandler);
		octaneServerMock.addTestSpecificHandler(testApiPushTestsResultHandler);
		octaneServerMock.addTestSpecificHandler(testApiPushTestsLogHandler);

		//  configure plugin for the server
		OctaneServerSettingsModel model = new OctaneServerSettingsModel(
				"http://127.0.0.1:" + octaneServerMockPort + "/ui?p=" + sharedSpaceId,
				"username",
				Secret.fromString("password"),
				"");
		ConfigurationService.configurePlugin(model);

		//  run the actual pipeline producing the tests
		FreeStyleProject project = rule.createFreeStyleProject(testsJobName);
		Maven.MavenInstallation mavenInstallation = ToolInstallations.configureMaven3();
		project.getBuildersList().add(new Maven(String.format("--settings \"%s\\conf\\settings.xml\" test -Dmaven.repo.local=\"%s\\m2-temp\"",
				System.getenv("MAVEN_HOME"), System.getenv("TEMP")), mavenInstallation.getName(), "helloWorld/pom.xml", null, "-Dmaven.test.failure.ignore=true"));
		project.getPublishersList().add(new JUnitResultArchiver("**/target/surefire-reports/*.xml"));
		project.setScm(new CopyResourceSCM("/helloWorldRoot"));
		build = TestUtils.runAndCheckBuild(project);

		// make sure dispatcher logic was executed
		queue.waitForTicks(3);
	}

	@AfterClass
	public static void cleanup() {
		OctaneServerMock octaneServerMock = OctaneServerMock.getInstance();
		octaneServerMock.removeTestSpecificHandler(testApiPreflightHandler);
		octaneServerMock.removeTestSpecificHandler(testApiPushTestsResultHandler);
		octaneServerMock.removeTestSpecificHandler(testApiPushTestsLogHandler);
	}

	@Test
	public void testXml() throws Exception {
		Page testResults = client.goTo("job/" + testsJobName + "/" + build.getNumber() + "/nga/tests/xml", "application/xml");
		String testResultsBody = testResults.getWebResponse().getContentAsString();
		assertEquals(testResultsBody, testApiPushTestsResultHandler.testResults.get(0));
		TestUtils.matchTests(new TestResultIterable(new StringReader(testResultsBody)), testsJobName, build.getStartTimeInMillis(), TestUtils.helloWorldTests);
	}

	@Test
	public void testAudit() throws Exception {
		Page auditLog = client.goTo("job/" + testsJobName + "/" + build.getNumber() + "/nga/tests/audit", "application/json");
		JSONArray audits = JSONArray.fromObject(auditLog.getWebResponse().getContentAsString());
		assertEquals(1, audits.size());
		JSONObject audit = audits.getJSONObject(0);
		assertEquals((long) pushTestResultId, audit.getLong("id"));
		assertTrue(audit.getBoolean("pushed"));
		assertEquals("http://127.0.0.1:" + octaneServerMockPort, audit.getString("location"));
		assertEquals(sharedSpaceId, audit.getString("sharedSpace"));
		assertNotNull(audit.getString("date"));
	}

	@Test
	public void testLog() throws IOException, SAXException {
		Page publishLog = client.goTo("job/" + testsJobName + "/" + build.getNumber() + "/nga/tests/log", "text/plain");
		assertEquals("This is the log", publishLog.getWebResponse().getContentAsString());
	}

	private static final class TestApiPreflightHandler extends OctaneServerMock.TestSpecificHandler {

		@Override
		public boolean ownsUrlToProcess(String url) {
			return url.endsWith("/jobs/" + testsJobName + "/tests-result-preflight");
		}

		@Override
		public void handle(String s, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
			if (baseRequest.getPathInfo().endsWith("/jobs/" + testsJobName + "/tests-result-preflight")) {
				response.setStatus(HttpServletResponse.SC_OK);
				response.getWriter().write(String.valueOf(true));
			}
		}
	}

	private static final class TestApiPushTestsResultHandler extends OctaneServerMock.TestSpecificHandler {
		private List<String> testResults = new LinkedList<>();

		@Override
		public boolean ownsUrlToProcess(String url) {
			return (RestService.SHARED_SPACE_INTERNAL_API_PATH_PART + sharedSpaceId + RestService.ANALYTICS_CI_PATH_PART + "test-results").equals(url);
		}

		@Override
		public void handle(String s, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
			testResults.add(getBodyAsString(baseRequest));
			TestDispatcher.TestsPushResponseDTO body = new TestDispatcher.TestsPushResponseDTO();
			body.id = String.valueOf(pushTestResultId);
			body.status = "queued";
			response.setStatus(HttpServletResponse.SC_ACCEPTED);
			response.getWriter().write(new ObjectMapper().writeValueAsString(body));
		}
	}

	private static final class TestApiPushTestsLogHandler extends OctaneServerMock.TestSpecificHandler {

		@Override
		public boolean ownsUrlToProcess(String url) {
			return (RestService.SHARED_SPACE_INTERNAL_API_PATH_PART + sharedSpaceId + RestService.ANALYTICS_CI_PATH_PART + "test-results/" + pushTestResultId + "/log").equals(url);
		}

		@Override
		public void handle(String s, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().write("This is the log");
		}
	}
}
