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
package com.microfocus.application.automation.tools.octane;

import com.microfocus.application.automation.tools.octane.events.EventsTest;
import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 * Purpose of this class is to provide Octane Server Mock for the tests of the Octane Plugin
 * The server will run on port that will be taken from configuration or default
 * The server will serve ALL of the tests, so each suite should carefully configure it to respond ot itself an not mess up with other suites
 */

public final class OctaneServerMock {
	private static final Logger logger = Logger.getLogger(EventsTest.class.getName());
	private static final OctaneServerMock INSTANCE = new OctaneServerMock();

	private final int DEFAULT_TESTING_SERVER_PORT = 9999;
	private int testingServerPort = DEFAULT_TESTING_SERVER_PORT;
	private boolean isRunning = false;

	private final List<TestSpecificHandler> testSpecificHandlers = new LinkedList<>();

	private OctaneServerMock() {
		logger.log(Level.INFO, "starting initialization...");
		String p = System.getProperty("testingServerPort");
		try {
			if (p != null) {
				testingServerPort = Integer.parseInt(p);
			}
		} catch (NumberFormatException nfe) {
			logger.log(Level.WARNING, "bad port number found in the system properties, default port will be used");
		}

		try {
			Server server = new Server(testingServerPort);
			server.setHandler(new OctaneServerMockHandler());
			server.start();
			isRunning = true;
			logger.log(Level.INFO, "SUCCESSFULLY started, listening on port " + testingServerPort);
		} catch (Throwable t) {
			logger.log(Level.SEVERE, "FAILED to start", t);
		}
	}

	public static OctaneServerMock getInstance() {
		return INSTANCE;
	}

	public boolean isRunning() {
		return isRunning;
	}

	public int getPort() {
		return testingServerPort;
	}

	public void addTestSpecificHandler(TestSpecificHandler testSpecificHandler) {
		if (testSpecificHandler == null) {
			throw new IllegalArgumentException("test specific handler for Octane Mock Server MUST NOT be null");
		}

		testSpecificHandlers.add(testSpecificHandler);
	}

	public void removeTestSpecificHandler(TestSpecificHandler testSpecificHandler) {
		testSpecificHandlers.remove(testSpecificHandler);
	}

	abstract public static class TestSpecificHandler extends AbstractHandler {
		abstract public boolean ownsUrlToProcess(String url);

		protected String getBodyAsString(Request request) throws IOException {
			StringBuilder body = new StringBuilder();
			byte[] buffer = new byte[1024];
			int len;

			GZIPInputStream gzip = new GZIPInputStream(Content.Source.asInputStream(request));
			while ((len = gzip.read(buffer, 0, 1024)) > 0) {
				body.append(new String(buffer, 0, len));
			}

			return body.toString();
		}
	}

	private final class OctaneServerMockHandler extends Handler.Abstract {

		@Override
		public boolean handle(Request request, Response response, Callback callback) throws Exception {
			logger.log(Level.INFO, "accepted request " + request.getMethod() + " " + Request.getPathInContext(request));
			for (TestSpecificHandler testSpecificHandler : testSpecificHandlers) {
				if (testSpecificHandler.ownsUrlToProcess(Request.getPathInContext(request))) {
					logger.log(Level.INFO, request.getMethod() + " " + Request.getPathInContext(request) + " picked up by " + testSpecificHandler);
					callback.succeeded();
					return testSpecificHandler.handle(request, response, callback);
				}
			}
			logger.log(Level.INFO, "none of test specific handlers matched for " + request.getMethod() + " " + Request.getPathInContext(request));
			if (request.getMethod().equals("POST") && Request.getPathInContext(request).equals("/authentication/sign_in")) {
				logger.log(Level.INFO, "found POST 'authentication/sign_in' request, will respond with default Mock handler");
				response.setStatus(HttpServletResponse.SC_OK);
				HttpCookie cookie = HttpCookie.build("LWSSO_COOKIE_KEY", "some_dummy_security_token").build();
				Response.addCookie(response,  cookie);
				callback.succeeded();
				return true;
			} else if (request.getMethod().equals("GET") && Request.getPathInContext(request).endsWith("tasks")) {
				return defaultGetTasksHandler(request, response, callback);
			} else if (request.getMethod().equals("GET") && Request.getPathInContext(request).startsWith("/internal-api/shared_spaces/") && Request.getPathInContext(request).endsWith("/workspaceId")) {
				return defaultGetWorkspaceFoLogsHandler(request, response, callback);
			} else if (request.getMethod().equals("GET") && Request.getPathInContext(request).endsWith("connectivity/status")) {
				response.setStatus(HttpServletResponse.SC_OK);
				response.write(true, BufferUtil.toBuffer("{\"octaneVersion\":\"15.1.20.9999\",\"supportedSdkVersion\":\"1.4.17\"}"), callback);
				callback.succeeded();
				return true;
			} else {
				logger.info("will respond with 200 and empty content");
				response.setStatus(HttpServletResponse.SC_OK);
				callback.succeeded();
				return true;
			}
		}

		private boolean defaultGetTasksHandler(Request request, Response response, Callback callback) {
			logger.log(Level.INFO, "found GET 'tasks' request, will respond with default Mock handler");
			try {
				Thread.sleep(10 * 1000);
			} catch (InterruptedException ie) {
				logger.log(Level.FINE, "interrupted while delaying default GET tasks response");
				callback.failed(ie);
			}
			response.setStatus(HttpServletResponse.SC_OK);
			return true;
		}

		private boolean defaultGetWorkspaceFoLogsHandler(Request request, Response response, Callback callback) {
			logger.log(Level.INFO, "found GET 'workspaceId' for build logs request, will respond with default Mock handler");
			response.setStatus(HttpServletResponse.SC_NO_CONTENT);
			callback.succeeded();
			return true;
		}
	}
}
