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

package com.microfocus.application.automation.tools.octane.configuration;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.microfocus.application.automation.tools.octane.OctaneServerMock;
import com.microfocus.application.automation.tools.octane.tests.ExtensionUtil;
import com.microfocus.application.automation.tools.octane.Messages;
import hudson.util.FormValidation;
import hudson.util.Secret;
import org.eclipse.jetty.server.Request;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"squid:S2699", "squid:S3658", "squid:S2259", "squid:S1872", "squid:S2925", "squid:S109", "squid:S1607", "squid:S2701", "squid:S2698"})
public class ConfigurationServiceTest {
	private static final Logger logger = Logger.getLogger(ConfigurationServiceTest.class.getName());

	@ClassRule
	public static final JenkinsRule rule = new JenkinsRule();
	private final JenkinsRule.WebClient jClient = rule.createWebClient();

	private ConfigurationParser configurationParser;
	private Secret password;

	@Before
	public void init() throws Exception {
		logger.log(Level.FINE, "initializing configuration for test");

		configurationParser = ExtensionUtil.getInstance(rule, ConfigurationParser.class);
		password = Secret.fromString("password");

		HtmlPage configPage = jClient.goTo("configure");
		HtmlForm form = configPage.getFormByName("config");

		form.getInputByName("_.uiLocation").setValueAttribute("http://localhost:8008/ui/?p=1001/1002");
		form.getInputByName("_.username").setValueAttribute("username");
		form.getInputByName("_.password").setValueAttribute("password");
		rule.submit(form);
	}

	@Test
	public void testGetServerConfiguration() {
		ServerConfiguration configuration = ConfigurationService.getServerConfiguration();
		assertEquals("http://localhost:8008", configuration.location);
		assertEquals("1001", configuration.sharedSpace);
		assertEquals("username", configuration.username);
		assertEquals(password, configuration.password);
	}

	@Test
	public void testConfigurationRoundTrip() throws Exception {
		HtmlForm formIn = jClient.goTo("configure").getFormByName("config");
		rule.submit(formIn);
		HtmlForm formOut = jClient.goTo("configure").getFormByName("config");
		assertEquals(formIn.getInputByName("_.uiLocation").getValueAttribute(), formOut.getInputByName("_.uiLocation").getValueAttribute());
		assertEquals(formIn.getInputByName("_.username").getValueAttribute(), formOut.getInputByName("_.username").getValueAttribute());
		// NOTE: password is actually empty (bug or security feature?)
		assertEquals(formIn.getInputByName("_.password").getValueAttribute(), formOut.getInputByName("_.password").getValueAttribute());
	}

	@Test
	@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
	public void testCheckConfiguration() {
		//  prepare work with Octane Server Mock
		OctaneServerMock serverMock = OctaneServerMock.getInstance();
		assertTrue(serverMock.isRunning());

		ConfigurationTestHandler testHandler = new ConfigurationTestHandler();
		serverMock.addTestSpecificHandler(testHandler);

		// valid configuration
		testHandler.desiredStatus = HttpServletResponse.SC_OK;
		FormValidation validation = configurationParser.checkConfiguration("http://localhost:" + serverMock.getPort(), "1001", "username1", password);
		assertEquals(FormValidation.Kind.OK, validation.kind);
		assertTrue(validation.getMessage().contains("Connection successful"));

		// authentication failed
		testHandler.desiredStatus = HttpServletResponse.SC_UNAUTHORIZED;
		validation = configurationParser.checkConfiguration("http://localhost:" + serverMock.getPort(), "1001", "username1", password);
		assertEquals(FormValidation.Kind.ERROR, validation.kind);
		assertTrue(validation.getMessage().contains(Messages.AuthenticationFailure()));

		// authorization failed
		testHandler.desiredStatus = HttpServletResponse.SC_FORBIDDEN;
		validation = configurationParser.checkConfiguration("http://localhost:" + serverMock.getPort(), "1001", "username1", password);
		assertEquals(FormValidation.Kind.ERROR, validation.kind);
		assertTrue(validation.getMessage().contains(Messages.AuthorizationFailure()));

		// domain project does not exists
		testHandler.desiredStatus = HttpServletResponse.SC_NOT_FOUND;
		validation = configurationParser.checkConfiguration("http://localhost:" + serverMock.getPort(), "1001", "username1", password);
		assertEquals(FormValidation.Kind.ERROR, validation.kind);
		assertTrue(validation.getMessage().contains(Messages.ConnectionSharedSpaceInvalid()));

		serverMock.removeTestSpecificHandler(testHandler);
	}

	private static final class ConfigurationTestHandler extends OctaneServerMock.TestSpecificHandler {
		private int desiredStatus = HttpServletResponse.SC_OK;

		@Override
		public boolean ownsUrlToProcess(String url) {
			return "/authentication/sign_in".equals(url);
		}

		@Override
		public void handle(String s, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
			response.setStatus(desiredStatus);
		}
	}
}
