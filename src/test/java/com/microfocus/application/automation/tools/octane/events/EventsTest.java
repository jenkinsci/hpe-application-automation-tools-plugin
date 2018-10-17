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

package com.microfocus.application.automation.tools.octane.events;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.events.CIEvent;
import com.hp.octane.integrations.dto.events.CIEventsList;
import com.microfocus.application.automation.tools.model.OctaneServerSettingsModel;
import com.hp.octane.integrations.dto.events.CIEventType;
import com.microfocus.application.automation.tools.octane.OctaneServerMock;
import com.microfocus.application.automation.tools.octane.configuration.ConfigurationService;
import hudson.model.FreeStyleProject;
import hudson.util.Secret;
import org.eclipse.jetty.server.Request;
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
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

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
		CIEventsList eventsLists = eventsTestHandler.eventsLists;
		logger.info(eventsLists.toString());
		logger.info("EVENTS TEST: server mock received " + eventsLists.getEvents().size() + " list/s of events");

		assertNotNull(eventsLists.getServer());
		assertTrue(rule.getInstance().getRootUrl() != null && rule.getInstance().getRootUrl().startsWith(eventsLists.getServer().getUrl()));
		assertEquals("jenkins", eventsLists.getServer().getType());
		assertEquals(ConfigurationService.getModel().getIdentity(), eventsLists.getServer().getInstanceId());

		assertNotNull(eventsLists.getEvents());
		assertFalse(eventsLists.getEvents().isEmpty());
		for (CIEvent event : eventsLists.getEvents()) {
			System.out.println(event.getEventType() + "-" + event.getProject() + "-" + event.getBuildCiId());
			if (projectName.equals(event.getProject())) {
				assertEquals(eventsOrder.get(0), event.getEventType());
				eventsOrder.remove(0);
			}
		}
		assertEquals(0, eventsOrder.size());
	}

	private static final class EventsTestHandler extends OctaneServerMock.TestSpecificHandler {
		private final CIEventsList eventsLists = dtoFactory.newDTO(CIEventsList.class)
				.setEvents(new LinkedList<>());

		@Override
		public boolean ownsUrlToProcess(String url) {
			return ("/internal-api/shared_spaces/" + sharedSpaceId + "/analytics/ci/events").equals(url);
		}

		@Override
		public void handle(String s, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
			String requestBody = getBodyAsString(baseRequest);
			CIEventsList tmp = dtoFactory.dtoFromJson(requestBody, CIEventsList.class);
			eventsLists.setServer(tmp.getServer());
			eventsLists.getEvents().addAll(tmp.getEvents());
			logger.info("EVENTS TEST: server mock events list length " + eventsLists.getEvents().size());
			response.setStatus(HttpServletResponse.SC_OK);
		}
	}
}
