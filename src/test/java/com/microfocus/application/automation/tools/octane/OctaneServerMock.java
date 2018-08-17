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

package com.microfocus.application.automation.tools.octane;

import com.microfocus.application.automation.tools.octane.events.EventsTest;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

			GZIPInputStream gzip = new GZIPInputStream(request.getInputStream());
			while ((len = gzip.read(buffer, 0, 1024)) > 0) {
				body.append(new String(buffer, 0, len));
			}

			return body.toString();
		}
	}

	private final class OctaneServerMockHandler extends AbstractHandler {

		@Override
		public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse response) throws IOException, ServletException {
			logger.log(Level.INFO, "accepted request " + request.getMethod() + " " + request.getPathInfo());
			for (TestSpecificHandler testSpecificHandler : testSpecificHandlers) {
				if (testSpecificHandler.ownsUrlToProcess(request.getPathInfo())) {
					logger.log(Level.INFO, request.getMethod() + " " + request.getPathInfo() + " picked up by " + testSpecificHandler);
					testSpecificHandler.handle(s, request, httpServletRequest, response);
					request.setHandled(true);
					break;
				}
			}
			if (!request.isHandled()) {
				logger.log(Level.INFO, "none of test specific handlers matched for " + request.getMethod() + " " + request.getPathInfo());
				if (request.getMethod().equals("POST") && request.getPathInfo().equals("/authentication/sign_in")) {
					logger.log(Level.INFO, "found POST 'authentication/sign_in' request, will respond with default Mock handler");
					response.setStatus(HttpServletResponse.SC_OK);
					response.addCookie(new Cookie("LWSSO_COOKIE_KEY", "some_dummy_security_token"));
					request.setHandled(true);
				} else if (request.getMethod().equals("GET") && request.getPathInfo().endsWith("tasks")) {
					defaultGetTasksHandler(request, response);
				} else if (request.getMethod().equals("GET") && request.getPathInfo().startsWith("/internal-api/shared_spaces/") && request.getPathInfo().endsWith("/workspaceId")) {
					defaultGetWorkspaceFoLogsHandler(request, response);
				} else {
					logger.info("will respond with 200 and empty content");
					response.setStatus(HttpServletResponse.SC_OK);
					request.setHandled(true);
				}
			}
		}

		private void defaultGetTasksHandler(Request request, HttpServletResponse response) {
			logger.log(Level.INFO, "found GET 'tasks' request, will respond with default Mock handler");
			try {
				Thread.sleep(10 * 1000);
			} catch (InterruptedException ie) {
				logger.log(Level.FINE, "interrupted while delaying default GET tasks response");
			}
			response.setStatus(HttpServletResponse.SC_OK);
			request.setHandled(true);
		}

		private void defaultGetWorkspaceFoLogsHandler(Request request, HttpServletResponse response) {
			logger.log(Level.INFO, "found GET 'workspaceId' for build logs request, will respond with default Mock handler");
			response.setStatus(HttpServletResponse.SC_NO_CONTENT);
			request.setHandled(true);
		}
	}
}
