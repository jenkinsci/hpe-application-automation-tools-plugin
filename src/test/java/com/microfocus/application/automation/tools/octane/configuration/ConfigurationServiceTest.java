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
package com.microfocus.application.automation.tools.octane.configuration;

import com.microfocus.application.automation.tools.model.OctaneServerSettingsModel;
import com.microfocus.application.automation.tools.octane.Messages;
import com.microfocus.application.automation.tools.octane.OctanePluginTestBase;
import com.microfocus.application.automation.tools.octane.OctaneServerMock;
import com.microfocus.application.automation.tools.octane.tests.ExtensionUtil;
import hudson.util.FormValidation;
import hudson.util.Secret;
import org.eclipse.jetty.server.Request;
import org.htmlunit.html.HtmlCheckBoxInput;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlElementUtil;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

@SuppressWarnings({"squid:S2699", "squid:S3658", "squid:S2259", "squid:S1872", "squid:S2925", "squid:S109", "squid:S1607", "squid:S2701", "squid:S2698"})
@Ignore("temporary ignore till sonar issue is fixed")
public class ConfigurationServiceTest extends OctanePluginTestBase {
	private static final Logger logger = Logger.getLogger(ConfigurationServiceTest.class.getName());

	private Secret password;

	@Before
	public void initBefore() {
		logger.log(Level.FINE, "initializing configuration for test");
		password = Secret.fromString("password");
	}

	@Test
	public void testGetServerConfiguration() {
		OctaneServerSettingsModel configuration = ConfigurationService.getAllSettings().get(0);
//		assertEquals("http://localhost:8008", configuration.getLocation());
		assertEquals(OctanePluginTestBase.ssp, configuration.getSharedSpace());
		assertEquals("username", configuration.getUsername());
		assertEquals(password, configuration.getPassword());
	}

	@Test
	public void testConfigManipulation() throws Exception {
		HtmlPage configPage = client.goTo("configure");
		HtmlForm updateConfigForm = configPage.getFormByName("config");


		//update identity for existing server config
		((HtmlCheckBoxInput) updateConfigForm.getElementsByAttribute("input", "name", "showIdentity").get(0)).setChecked(true);
		String newInstanceId = UUID.randomUUID().toString();
		OctaneServerSettingsModel oldModel = ConfigurationService.getSettings(instanceId);
		String oldIdentity = oldModel.getIdentity();
		String oldModelInternalId = oldModel.getInternalId();
		((HtmlInput) findInputText(updateConfigForm, "_.identity", instanceId)).setDefaultValue(newInstanceId);

		rule.submit(updateConfigForm);
		OctaneServerSettingsModel newModel = ConfigurationService.getSettings(newInstanceId);
		String newIdentity = newModel.getIdentity();
		String newModelInternalId = newModel.getInternalId();

		assertEquals(newModelInternalId, oldModelInternalId);
		assertNotEquals(oldIdentity, newIdentity);
		assertEquals(newInstanceId, newIdentity);

		//add new configuration
		configPage = client.goTo("configure");
		HtmlForm addConfigForm = configPage.getFormByName("config");
		HtmlElement addButton = findButton(addConfigForm, "Add ALM Octane server");
		configPage = (HtmlPage) HtmlElementUtil.click(addButton);
		addConfigForm = configPage.getFormByName("config");


		ssp = UUID.randomUUID().toString();
		((HtmlInput) findInputText(addConfigForm, "_.uiLocation", null)).setValueAttribute("http://localhost:8008/ui/?p=" + ssp + "/1002");
		((HtmlInput) findInputText(addConfigForm, "_.username", null)).setValueAttribute("username");
		((HtmlInput) findInputText(addConfigForm, "_.password", null)).setValueAttribute("password");

		rule.submit(addConfigForm);
		assertEquals(2, ConfigurationService.getAllSettings().size());

		//remove configuration
		configPage = client.goTo("configure");
		HtmlForm deleteConfigForm = configPage.getFormByName("config");
		HtmlElement deleteButton = findButton(deleteConfigForm, "Delete ALM Octane server");
		configPage = (HtmlPage) HtmlElementUtil.click(deleteButton);
		deleteConfigForm = configPage.getFormByName("config");

		rule.submit(deleteConfigForm);
		assertEquals(1, ConfigurationService.getAllSettings().size());
	}


	@Test
	public void testConfigurationRoundTrip() throws Exception {
		HtmlForm formIn = client.goTo("configure").getFormByName("config");
		HtmlForm formOut = client.goTo("configure").getFormByName("config");
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
		FormValidation validation = ConfigurationValidator.checkConfigurationAndWrapWithFormValidation("http://localhost:" + serverMock.getPort(), "1001", "username1", password);
		assertEquals(FormValidation.Kind.OK, validation.kind);
		assertTrue(validation.getMessage().contains("Connection successful"));

		// authentication failed
		testHandler.desiredStatus = HttpServletResponse.SC_UNAUTHORIZED;
		validation = ConfigurationValidator.checkConfigurationAndWrapWithFormValidation("http://localhost:" + serverMock.getPort(), "1001", "username1", password);
		assertEquals(FormValidation.Kind.ERROR, validation.kind);
		assertTrue(validation.getMessage().contains(Messages.AuthenticationFailure()));

		// authorization failed
		testHandler.desiredStatus = HttpServletResponse.SC_FORBIDDEN;
		validation = ConfigurationValidator.checkConfigurationAndWrapWithFormValidation("http://localhost:" + serverMock.getPort(), "1001", "username1", password);
		assertEquals(FormValidation.Kind.ERROR, validation.kind);
		assertTrue(validation.getMessage().contains(Messages.AuthorizationFailure()));

		// domain project does not exists
		testHandler.desiredStatus = HttpServletResponse.SC_NOT_FOUND;
		validation = ConfigurationValidator.checkConfigurationAndWrapWithFormValidation("http://localhost:" + serverMock.getPort(), "1001", "username1", password);
		assertEquals(FormValidation.Kind.ERROR, validation.kind);
		assertTrue(validation.getMessage().contains(Messages.ConnectionSharedSpaceInvalid()));

		serverMock.removeTestSpecificHandler(testHandler);
	}

	private HtmlElement findButton(HtmlElement form, String buttonText) {
		List<HtmlElement> list = new LinkedList<>();
		for (HtmlElement htmlElement : form.getElementsByTagName("button")) {
			if (buttonText.equals(htmlElement.getFirstChild().asNormalizedText())) {
				list.add(htmlElement);
			}
		}
		return list.size() > 0 ? list.get(0) : null;
	}

	private HtmlElement findInputText(HtmlElement form, String inputName, String value) {
		for (HtmlElement htmlElement : ((HtmlForm) form).getInputsByName(inputName))
			if (value == null || value.isEmpty()) {
				if (htmlElement.getAttribute("value").isEmpty()) {
					return htmlElement;
				}
			} else {
				if (value.equals(htmlElement.getAttribute("value"))) {
					return htmlElement;
				}
			}
		return null;
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
