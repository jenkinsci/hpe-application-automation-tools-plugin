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

package com.hpe.application.automation.tools.octane.events;

import com.hpe.application.automation.tools.model.OctaneServerSettingsModel;
import com.hp.octane.integrations.dto.events.CIEventType;
import com.hpe.application.automation.tools.octane.OctaneServerMock;
import com.hpe.application.automation.tools.octane.configuration.ConfigurationService;
import hudson.model.FreeStyleProject;
import hudson.util.Secret;
import org.eclipse.jetty.server.Request;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: gullery
 * Date: 13/01/15
 * Time: 22:05
 * To change this template use File | Settings | File Templates.
 */
@SuppressWarnings({"squid:S2699", "squid:S3658", "squid:S2259", "squid:S1872", "squid:S2925", "squid:S109", "squid:S1607", "squid:S2701", "squid:S2698"})
public class EventsTest {
	private static final Logger logger = Logger.getLogger(EventsTest.class.getName());

	private static final String projectName = "root-job-events-case";
	private static final String sharedSpaceId = "1007";
	private static final String username = "some";
	private static final String password = "pass";
	private static final EventsTestHandler eventsTestHandler = new EventsTestHandler();

	@ClassRule
	public static final JenkinsRule rule = new JenkinsRule();

	@BeforeClass
	public static void beforeClass() {
		//  ensure server mock is up
		OctaneServerMock serverMock = OctaneServerMock.getInstance();
		assertTrue(serverMock.isRunning());
		serverMock.addTestSpecificHandler(eventsTestHandler);

		//  configure plugin for the server
		OctaneServerSettingsModel model = new OctaneServerSettingsModel(
				"http://127.0.0.1:" + serverMock.getPort() + "/ui?p=" + sharedSpaceId,
				username,
				Secret.fromString(password),
				"");
		ConfigurationService.configurePlugin(model);
	}

	@Test
	public void testEventsA() throws Exception {
		FreeStyleProject p = rule.createFreeStyleProject(projectName);

		assertEquals(0, p.getBuilds().toArray().length);
		p.scheduleBuild2(0);
		while (p.getLastBuild() == null || p.getLastBuild().isBuilding()) {
			Thread.sleep(1000);
		}
		assertEquals(1, p.getBuilds().toArray().length);
		Thread.sleep(5000);

		List<CIEventType> eventsOrder = new ArrayList<>(Arrays.asList(CIEventType.STARTED, CIEventType.FINISHED));
		List<JSONObject> eventsLists = eventsTestHandler.eventsLists;
		JSONObject tmp;
		JSONArray events;
		logger.info(eventsLists.toString());
		logger.info("EVENTS TEST: server mock received " + eventsLists.size() + " list/s of events");
		for (JSONObject l : eventsLists) {
			assertEquals(2, l.length());

			assertFalse(l.isNull("server"));
			tmp = l.getJSONObject("server");
			assertTrue(rule.getInstance().getRootUrl() != null && rule.getInstance().getRootUrl().startsWith(tmp.getString("url")));
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

	private static final class EventsTestHandler extends OctaneServerMock.TestSpecificHandler {
		private final List<JSONObject> eventsLists = new LinkedList<>();

		@Override
		public boolean ownsUrlToProcess(String url) {
			return ("/internal-api/shared_spaces/" + sharedSpaceId + "/analytics/ci/events").equals(url);
		}

		@Override
		public void handle(String s, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
			String requestBody = getBodyAsString(baseRequest);

			try {
				eventsLists.add(new JSONObject(requestBody));
			} catch (JSONException e) {
				logger.warning("EVENTS TEST: response wasn't JSON compatible");
			}
			logger.info("EVENTS TEST: server mock events list length " + eventsLists.size());
			response.setStatus(HttpServletResponse.SC_OK);
		}
	}
}
