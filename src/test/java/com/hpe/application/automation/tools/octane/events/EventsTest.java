/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.hpe.application.automation.tools.octane.events;

import com.hpe.application.automation.tools.model.OctaneServerSettingsModel;
import com.hpe.application.automation.tools.octane.tests.ExtensionUtil;
import com.hp.octane.integrations.dto.events.CIEventType;
import com.hpe.application.automation.tools.octane.configuration.ConfigurationService;
import com.hpe.application.automation.tools.octane.configuration.ServerConfiguration;
import hudson.model.FreeStyleProject;
import hudson.util.Secret;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: gullery
 * Date: 13/01/15
 * Time: 22:05
 * To change this template use File | Settings | File Templates.
 */
@SuppressWarnings({"squid:S2699","squid:S3658","squid:S2259","squid:S1872","squid:S2925","squid:S109","squid:S1607","squid:S2701","squid:S2698"})
public class EventsTest {
	private static final Logger logger = Logger.getLogger(EventsTest.class.getName());

	static final private String projectName = "root-job-events-case";
	static final private int DEFAULT_TESTING_SERVER_PORT = 9999;
	static final private String sharedSpaceId = "1007";
	static final private String username = "some";
	static final private String password = "pass";

	static private Server server;
	static private int testingServerPort = DEFAULT_TESTING_SERVER_PORT;
	static private EventsHandler eventsHandler;

	@ClassRule
	public static final JenkinsRule rule = new JenkinsRule();

	@BeforeClass
	static public void beforeClass() throws Exception {
		String p = System.getProperty("testingServerPort");
		try {
			if (p != null) {
				testingServerPort = Integer.parseInt(p);
			}
		} catch (NumberFormatException nfe) {
			logger.info("EVENTS TEST: bad port number format, default port will be used: " + testingServerPort);
		}
		logger.info("EVENTS TEST: port chosen for mock Octane server: " + testingServerPort);

		eventsHandler = new EventsHandler();
		server = new Server(testingServerPort);
		server.setHandler(eventsHandler);
		server.start();
		logger.info("EVENTS TEST: mock Octane server started on local port " + testingServerPort);
	}

	@AfterClass
	static public void afterClass() throws Exception {
		logger.info("EVENTS TEST: stopping and destroying mock Octane server");
		server.stop();
		server.destroy();
	}

	@Test
	public void testEventsA() throws Exception {
		configurePlugin();

		EventsService eventsService = ExtensionUtil.getInstance(rule, EventsService.class);
		assertNotNull(eventsService.getClient());
		assertEquals("http://127.0.0.1:" + testingServerPort, eventsService.getClient().getLocation());
		assertEquals(sharedSpaceId, eventsService.getClient().getSharedSpace());
		logger.info("EVENTS TEST: event client configuration is: " +
				eventsService.getClient().getLocation() + " - " +
				eventsService.getClient().getSharedSpace() + " - " +
				eventsService.getClient().getUsername());

		FreeStyleProject p = rule.createFreeStyleProject(projectName);

		assertEquals(0, p.getBuilds().toArray().length);
		p.scheduleBuild2(0);
		while (p.getLastBuild() == null || p.getLastBuild().isBuilding()) {
			Thread.sleep(1000);
		}
		assertEquals(1, p.getBuilds().toArray().length);
		Thread.sleep(5000);

		List<CIEventType> eventsOrder = new ArrayList<>(Arrays.asList(CIEventType.STARTED, CIEventType.FINISHED));
		List<JSONObject> eventsLists = eventsHandler.getResults();
		JSONObject tmp;
		JSONArray events;
		logger.info(eventsLists.toString());
		System.out.print(eventsLists.toString());
		logger.info("EVENTS TEST: server mock received " + eventsLists.size() + " list/s of events");
		for (JSONObject l : eventsLists) {
			assertEquals(2, l.length());

			assertFalse(l.isNull("server"));
			tmp = l.getJSONObject("server");
			assertTrue(rule.getInstance().getRootUrl().startsWith(tmp.getString("url")));
			assertEquals("jenkins", tmp.getString("type"));
			assertEquals(ConfigurationService.getModel().getIdentity(), tmp.getString("instanceId"));

			assertFalse(l.isNull("events"));
			events = l.getJSONArray("events");
			for (int i = 0; i < events.length(); i++) {
				tmp = events.getJSONObject(i);
				if (tmp.getString("project").equals(projectName)) {
					assertEquals(eventsOrder.get(0), CIEventType.fromValue(tmp.getString("eventType")));
					eventsOrder.remove(0);
				}
			}
		}
		assertEquals(0, eventsOrder.size());
	}

	private static final class EventsHandler extends AbstractHandler {
		private final List<JSONObject> eventsLists = new ArrayList<>();

		@Override
		public void handle(String s, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
			logger.info("EVENTS TEST: server mock requested: " + baseRequest.getMethod() + " " + baseRequest.getPathInfo());

			String body = "";
			byte[] buffer;
			int len;
			if (request.getPathInfo().equals("/authentication/sign_in")) {
				response.addCookie(new Cookie("LWSSO_COOKIE_KEY", "some_dummy_security_token"));
				response.setStatus(HttpServletResponse.SC_OK);
			} else if (request.getPathInfo().endsWith("/tasks")) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ie) {
					//  do nothing here
				} finally {
					response.setStatus(HttpServletResponse.SC_OK);
				}
			} else if (request.getPathInfo().equals("/internal-api/shared_spaces/" + sharedSpaceId + "/analytics/ci/events")) {
				buffer = new byte[1024];

				GZIPInputStream gzis = new GZIPInputStream( request.getInputStream() );
				while ((len = gzis.read(buffer, 0, 1024)) > 0) {
					body += new String(buffer, 0, len);
				}
				try {
					eventsLists.add(new JSONObject(body));
				} catch (JSONException e) {
					logger.warning("EVENTS TEST: response wasn't JSON compatible");
				}
				logger.info("EVENTS TEST: server mock events list length " + eventsLists.size());
				response.setStatus(HttpServletResponse.SC_OK);
			}
			baseRequest.setHandled(true);
		}

		List<JSONObject> getResults() {
			return eventsLists;
		}
	}

	private void configurePlugin() throws Exception {
		OctaneServerSettingsModel
                model = new OctaneServerSettingsModel("http://127.0.0.1:" + testingServerPort + "/ui?p=" + sharedSpaceId,
				username,
				Secret.fromString(password),
				"");
		ConfigurationService.configurePlugin(model);

		ServerConfiguration serverConfiguration = ConfigurationService.getServerConfiguration();
		assertNotNull(serverConfiguration);
		assertEquals("http://127.0.0.1:" + testingServerPort, serverConfiguration.location);
		assertEquals(sharedSpaceId, serverConfiguration.sharedSpace);
		logger.info("EVENTS TEST: plugin configured with the following server configuration: " + serverConfiguration);
	}
}
