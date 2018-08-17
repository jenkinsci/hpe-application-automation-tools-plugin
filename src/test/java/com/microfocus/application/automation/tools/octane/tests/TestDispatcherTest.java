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
import com.hp.octane.integrations.api.RestService;
import com.microfocus.application.automation.tools.model.OctaneServerSettingsModel;
import com.microfocus.application.automation.tools.octane.OctaneServerMock;
import com.microfocus.application.automation.tools.octane.client.RetryModel;
import com.microfocus.application.automation.tools.octane.configuration.ConfigurationService;
import hudson.FilePath;
import hudson.matrix.Axis;
import hudson.matrix.AxisList;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.Maven;
import hudson.tasks.junit.JUnitResultArchiver;
import hudson.util.Secret;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.ToolInstallations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"squid:S2699", "squid:S3658", "squid:S2259", "squid:S1872", "squid:S2925", "squid:S109", "squid:S1607", "squid:S2698"})
public class TestDispatcherTest {
	private static final Logger logger = Logger.getLogger(TestDispatcherTest.class.getName());

	private static int octaneServerMockPort;
	private static String sharedSpaceId = TestDispatcherTest.class.getSimpleName();
	private static TestApiPreflightHandler testApiPreflightHandler = new TestApiPreflightHandler();
	private static TestApiPushTestsResultHandler testApiPushTestsResultHandler = new TestApiPushTestsResultHandler();
	private static AbstractProject project;
	private static TestDispatcher testDispatcher;
	private static TestQueue queue;

	@ClassRule
	public static final JenkinsRule rule = new JenkinsRule();

	static {
		System.setProperty("Octane.TestDispatcher.Period", "1000");
	}

	@BeforeClass
	public static void initClass() throws Exception {

		//  prepare project
		Maven.MavenInstallation mavenInstallation = ToolInstallations.configureMaven3();
		project = rule.createFreeStyleProject("TestDispatcher");
		((FreeStyleProject) project).getBuildersList().add(new Maven(String.format("--settings \"%s\\conf\\settings.xml\" install -Dmaven.repo.local=\"%s\\m2-temp\"",
				System.getenv("MAVEN_HOME"), System.getenv("TEMP")), mavenInstallation.getName(), null, null, "-Dmaven.test.failure.ignore=true"));
		((FreeStyleProject) project).getPublishersList().add(new JUnitResultArchiver("**/target/surefire-reports/*.xml"));
		project.setScm(new CopyResourceSCM("/helloWorldRoot"));

		//  prepare Octane Server Mock
		OctaneServerMock octaneServerMock = OctaneServerMock.getInstance();
		octaneServerMockPort = octaneServerMock.getPort();
		octaneServerMock.addTestSpecificHandler(testApiPreflightHandler);
		octaneServerMock.addTestSpecificHandler(testApiPushTestsResultHandler);

		//  configure plugin for the server
		OctaneServerSettingsModel model = new OctaneServerSettingsModel(
				"http://127.0.0.1:" + octaneServerMockPort + "/ui?p=" + sharedSpaceId,
				"username",
				Secret.fromString("password"),
				"");
		ConfigurationService.configurePlugin(model);

		testDispatcher = ExtensionUtil.getInstance(rule, TestDispatcher.class);
	}

	@AfterClass
	public static void cleanup() {
		OctaneServerMock octaneServerMock = OctaneServerMock.getInstance();
		octaneServerMock.removeTestSpecificHandler(testApiPreflightHandler);
		octaneServerMock.removeTestSpecificHandler(testApiPushTestsResultHandler);
	}

	@Before
	public void prepareForTest() {
		RetryModel retryModel = new RetryModel();
		testDispatcher._setRetryModel(retryModel);

		queue = new TestQueue();
		testDispatcher._setTestResultQueue(queue);
	}

	@Test
	public void testDispatcher() throws Exception {
		testApiPreflightHandler.respondWithNegative = false;
		testApiPreflightHandler.lastSessionHits = 0;
		testApiPushTestsResultHandler.respondWithErrorFailsNumber = 0;
		testApiPushTestsResultHandler.lastSessionHits = 0;
		testApiPushTestsResultHandler.testResults.clear();

		FreeStyleBuild build = executeBuild();
		testApiPushTestsResultHandler.resetAndWaitForNextDispatches(1, 10000);
		assertEquals(1, testApiPreflightHandler.lastSessionHits);
		assertEquals(testApiPushTestsResultHandler.testResults.get(0), IOUtils.toString(new FileInputStream(new File(build.getRootDir(), "mqmTests.xml"))));
		verifyAudit(false, build, true);
		testApiPushTestsResultHandler.testResults.clear();
		testApiPreflightHandler.lastSessionHits = 0;

		FreeStyleBuild build2 = executeBuild();
		testApiPushTestsResultHandler.resetAndWaitForNextDispatches(1, 10000);
		assertEquals(1, testApiPreflightHandler.lastSessionHits);
		assertEquals(testApiPushTestsResultHandler.testResults.get(0), IOUtils.toString(new FileInputStream(new File(build2.getRootDir(), "mqmTests.xml"))));
		verifyAudit(false, build2, true);
		assertEquals(0, queue.size());
		testApiPushTestsResultHandler.testResults.clear();
		testApiPreflightHandler.lastSessionHits = 0;
	}

	@Test
	public void testDispatcherBatch() throws Exception {
		FreeStyleBuild build1 = ((FreeStyleProject) project).scheduleBuild2(0).get();
		FreeStyleBuild build2 = ((FreeStyleProject) project).scheduleBuild2(0).get();
		FreeStyleBuild build3 = ((FreeStyleProject) project).scheduleBuild2(0).get();
		queue.add(Arrays.asList(build1, build2, build3));
		testApiPushTestsResultHandler.resetAndWaitForNextDispatches(3, 20000);

		assertEquals(3, testApiPreflightHandler.lastSessionHits);
		assertEquals(testApiPushTestsResultHandler.testResults.get(0), IOUtils.toString(new FileInputStream(new File(build1.getRootDir(), "mqmTests.xml"))));
		assertEquals(testApiPushTestsResultHandler.testResults.get(1), IOUtils.toString(new FileInputStream(new File(build2.getRootDir(), "mqmTests.xml"))));
		assertEquals(testApiPushTestsResultHandler.testResults.get(2), IOUtils.toString(new FileInputStream(new File(build3.getRootDir(), "mqmTests.xml"))));
		assertEquals(0, queue.size());

		verifyAudit(false, build1, true);
		verifyAudit(false, build2, true);
		verifyAudit(false, build3, true);

		testApiPreflightHandler.lastSessionHits = 0;
		testApiPushTestsResultHandler.testResults.clear();
	}

	@Test
	public void testDispatcherTemporaryFailureRetryTest() throws Exception {
		testApiPreflightHandler.lastSessionHits = 0;
		testApiPushTestsResultHandler.lastSessionHits = 0;
		testApiPushTestsResultHandler.respondWithErrorFailsNumber = 6;

		FreeStyleBuild build = executeBuild();
		testApiPushTestsResultHandler.resetAndWaitForNextDispatches(1, 5000);
		assertEquals(1, testApiPreflightHandler.lastSessionHits);
		assertEquals(1, testApiPushTestsResultHandler.lastSessionHits);
		verifyAudit(true, build, false);

		//  starting quite period of 3 seconds

		executeBuild();
		testApiPushTestsResultHandler.resetAndWaitForNextDispatches(1, 10000);

		//  entering quite period of 10 seconds

		executeBuild();

		executeBuild();

		//  entering quite period of 60 seconds

		assertEquals(3, testApiPreflightHandler.lastSessionHits);
		assertEquals(3, testApiPushTestsResultHandler.lastSessionHits);
		assertEquals(4, queue.size());

		testApiPreflightHandler.lastSessionHits = 0;
		testApiPushTestsResultHandler.lastSessionHits = 0;
		testApiPushTestsResultHandler.testResults.clear();
	}

	@Test
	public void testDispatcherBodyFailure() throws Exception {
		// body post fails for the first time, succeeds afterwards
		//
		testApiPreflightHandler.respondWithNegative = false;
		testApiPreflightHandler.lastSessionHits = 0;
		testApiPushTestsResultHandler.lastSessionHits = 0;
		testApiPushTestsResultHandler.respondWithErrorFailsNumber = 1;
		testApiPushTestsResultHandler.testResults.clear();

		FreeStyleBuild build = executeBuild();
		testApiPushTestsResultHandler.resetAndWaitForNextDispatches(2, 10000);
		assertEquals(2, testApiPreflightHandler.lastSessionHits);
		assertEquals(2, testApiPushTestsResultHandler.lastSessionHits);
		assertEquals(testApiPushTestsResultHandler.testResults.get(0), IOUtils.toString(new FileInputStream(new File(build.getRootDir(), "mqmTests.xml"))));
		verifyAudit(true, build, false, true);

		assertEquals(0, queue.size());
		assertEquals(0, queue.getDiscards());

		// body post fails for two consecutive times
		//
		testApiPreflightHandler.lastSessionHits = 0;
		testApiPushTestsResultHandler.lastSessionHits = 0;
		testApiPushTestsResultHandler.respondWithErrorFailsNumber = 2;
		testApiPushTestsResultHandler.testResults.clear();
		build = executeBuild();
		testApiPushTestsResultHandler.resetAndWaitForNextDispatches(2, 15000);
		assertEquals(2, testApiPreflightHandler.lastSessionHits);
		assertEquals(2, testApiPushTestsResultHandler.lastSessionHits);
		assertEquals(0, testApiPushTestsResultHandler.testResults.size());
		verifyAudit(true, build, false, false);

		assertEquals(1, queue.size());
	}

	@Test
	public void testDispatchMatrixBuild() throws Exception {
		AbstractProject tmp = project;

		//  prepare Matrix project
		Maven.MavenInstallation mavenInstallation = ToolInstallations.configureMaven3();
		project = rule.createProject(MatrixProject.class, "TestDispatcherMatrix");
		((MatrixProject) project).setAxes(new AxisList(new Axis("osType", "Linux", "Windows")));
		((MatrixProject) project).getBuildersList().add(new Maven(String.format("--settings \"%s\\conf\\settings.xml\" install -Dmaven.repo.local=\"%s\\m2-temp\"",
				System.getenv("MAVEN_HOME"), System.getenv("TEMP")), mavenInstallation.getName(), null, null, "-Dmaven.test.failure.ignore=true"));
		((MatrixProject) project).getPublishersList().add(new JUnitResultArchiver("**/target/surefire-reports/*.xml"));
		project.setScm(new CopyResourceSCM("/helloWorldRoot"));

		testApiPreflightHandler.respondWithNegative = false;
		testApiPreflightHandler.lastSessionHits = 0;
		testApiPushTestsResultHandler.respondWithErrorFailsNumber = 0;
		testApiPushTestsResultHandler.lastSessionHits = 0;
		testApiPushTestsResultHandler.testResults.clear();

		MatrixBuild matrixBuild = ((MatrixProject) project).scheduleBuild2(0).get();
		for (MatrixRun run : matrixBuild.getExactRuns()) {
			queue.add("TestDispatcherMatrix/" + run.getParent().getName(), run.getNumber());
		}
		testApiPushTestsResultHandler.resetAndWaitForNextDispatches(2, 10000);
		for (int i = 0; i < matrixBuild.getExactRuns().size(); i++) {
			MatrixRun run = matrixBuild.getExactRuns().get(i);
			assertEquals(2, testApiPreflightHandler.lastSessionHits);
			assertEquals(testApiPushTestsResultHandler.testResults.get(i), IOUtils.toString(new FileInputStream(new File(run.getRootDir(), "mqmTests.xml"))));
			verifyAudit(false, run, true);
		}

		assertEquals(0, queue.size());

		project = tmp;
	}

	@Test
	public void testDispatcherTemporarilyUnavailable() throws Exception {
		testApiPreflightHandler.respondWithNegative = false;
		testApiPreflightHandler.lastSessionHits = 0;
		testApiPushTestsResultHandler.respondWithErrorFailsNumber = 0;
		testApiPushTestsResultHandler.lastSessionHits = 0;
		testApiPushTestsResultHandler.testResults.clear();

		RetryModel retryModel = new RetryModel(2, 2, 2, 2, 2, 2, 2, 2, 2);
		testDispatcher._setRetryModel(retryModel);

		//  one successful push
		FreeStyleBuild build1 = executeBuild();
		testApiPushTestsResultHandler.resetAndWaitForNextDispatches(1, 10000);

		//  session of failures
		testApiPushTestsResultHandler.respondWithErrorFailsNumber = 5;
		FreeStyleBuild build2 = executeBuild();
		testApiPushTestsResultHandler.resetAndWaitForNextDispatches(6, 50000);

		assertEquals(7, testApiPreflightHandler.lastSessionHits);
		assertEquals(7, testApiPushTestsResultHandler.lastSessionHits);

		assertEquals(testApiPushTestsResultHandler.testResults.get(0), IOUtils.toString(new FileInputStream(new File(build1.getRootDir(), "mqmTests.xml"))));
		verifyAudit(false, build1, true);
		assertEquals(testApiPushTestsResultHandler.testResults.get(1), IOUtils.toString(new FileInputStream(new File(build2.getRootDir(), "mqmTests.xml"))));
		verifyAudit(true, build2, false, false, false, false, false, true);

		assertEquals(0, queue.size());
	}

	private FreeStyleBuild executeBuild() throws ExecutionException, InterruptedException {
		FreeStyleBuild build = ((FreeStyleProject) project).scheduleBuild2(0).get();
		queue.add(build.getProject().getName(), build.getNumber());
		return build;
	}

	private void verifyAudit(boolean unavailableIfFailed, AbstractBuild build, boolean... statuses) throws IOException, InterruptedException {
		FilePath auditFile = new FilePath(new File(build.getRootDir(), TestDispatcher.TEST_AUDIT_FILE));
		JSONArray audits;
		if (statuses.length > 0) {
			assertTrue(auditFile.exists());
			InputStream is = auditFile.read();
			audits = JSONArray.fromObject(IOUtils.toString(is, "UTF-8"));
			IOUtils.closeQuietly(is);
		} else {
			assertFalse(auditFile.exists());
			audits = new JSONArray();
		}
		assertEquals(statuses.length, audits.size());
		for (int i = 0; i < statuses.length; i++) {
			JSONObject audit = audits.getJSONObject(i);
			assertEquals("http://127.0.0.1:" + octaneServerMockPort, audit.getString("location"));
			assertEquals(sharedSpaceId, audit.getString("sharedSpace"));
			assertEquals(statuses[i], audit.getBoolean("pushed"));
			if (statuses[i]) {
				assertEquals(1L, audit.getLong("id"));
			}
			if (!statuses[i] && unavailableIfFailed) {
				assertTrue(audit.getBoolean("temporarilyUnavailable"));
			} else {
				assertFalse(audit.containsKey("temporarilyUnavailable"));
			}
			assertNotNull(audit.getString("date"));
		}
	}

	private static abstract class TDTHandlersBase extends OctaneServerMock.TestSpecificHandler {
		protected volatile int dispatchesCounterForWait;

		protected void resetAndWaitForNextDispatches(int numberOfDispatches, int maxMillisToWait) {
			dispatchesCounterForWait = 0;
			long startTime = System.currentTimeMillis();
			while (dispatchesCounterForWait < numberOfDispatches && maxMillisToWait > System.currentTimeMillis() - startTime) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException ie) {
					logger.log(Level.WARNING, "interrupted during a wait for dispatches");
				}
			}
			assertEquals("verifying that reached the desired number of dispatches", numberOfDispatches, dispatchesCounterForWait);
			logger.log(Level.INFO, numberOfDispatches + " dispatch round/s reached in ~" + (System.currentTimeMillis() - startTime) + "ms");
		}
	}

	private static final class TestApiPreflightHandler extends TDTHandlersBase {
		private int lastSessionHits = 0;
		private boolean respondWithNegative = false;

		@Override
		public boolean ownsUrlToProcess(String url) {
			return url.endsWith("/jobs/" + project.getName() + "/tests-result-preflight");
		}

		@Override
		public void handle(String s, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
			if (baseRequest.getPathInfo().endsWith("/jobs/" + project.getName() + "/tests-result-preflight")) {
				if (respondWithNegative) {
					response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				} else {
					response.setStatus(HttpServletResponse.SC_OK);
					response.getWriter().write(String.valueOf(true));
				}
				lastSessionHits++;
			}
			synchronized (this) {
				dispatchesCounterForWait++;
			}
		}

	}

	private static final class TestApiPushTestsResultHandler extends TDTHandlersBase {
		private List<String> testResults = new LinkedList<>();
		private int lastSessionHits = 0;
		private int respondWithErrorFailsNumber = 0;

		@Override
		public boolean ownsUrlToProcess(String url) {
			return (RestService.SHARED_SPACE_INTERNAL_API_PATH_PART + sharedSpaceId + RestService.ANALYTICS_CI_PATH_PART + "test-results").equals(url);
		}

		@Override
		public void handle(String s, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
			if (respondWithErrorFailsNumber == 0) {
				testResults.add(getBodyAsString(baseRequest));
				TestDispatcher.TestsPushResponseDTO body = new TestDispatcher.TestsPushResponseDTO();
				body.id = String.valueOf(1);
				body.status = "queued";
				response.setStatus(HttpServletResponse.SC_ACCEPTED);
				response.getWriter().write(new ObjectMapper().writeValueAsString(body));

			} else {
				response.setStatus(503);
				respondWithErrorFailsNumber--;
			}
			lastSessionHits++;
			synchronized (this) {
				dispatchesCounterForWait++;
			}
		}
	}
}
