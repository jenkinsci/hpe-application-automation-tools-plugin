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

import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.HttpMethod;
import org.htmlunit.Page;
import org.htmlunit.WebRequest;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import com.microfocus.application.automation.tools.model.OctaneServerSettingsModel;
import com.microfocus.application.automation.tools.octane.OctanePluginTestBase;
import hudson.util.Secret;
import net.jcip.annotations.NotThreadSafe;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.UUID;

@SuppressWarnings({"squid:S2699", "squid:S3658", "squid:S2259", "squid:S1872", "squid:S2925", "squid:S109", "squid:S1607", "squid:S2701", "squid:S2698"})
@NotThreadSafe
@Ignore("temporary ignore till sonar issue is fixed")
public class ConfigApiTest extends OctanePluginTestBase {

	@Before
	public void initTest() throws Exception {
		HtmlPage configPage = client.goTo("configure");
		HtmlForm form = configPage.getFormByName("config");
		ssp = UUID.randomUUID().toString();
		form.getInputByName("_.uiLocation").setValueAttribute("http://localhost:8008/ui/?p=" + ssp + "/1002");
		form.getInputByName("_.username").setValueAttribute("username");
		form.getInputByName("_.password").setValueAttribute("password");
		rule.submit(form);
	}


	@Test
	public void testRead() throws Exception {
		Page page = client.goTo("nga/configuration/read", "application/json");
		String configsAsString = page.getWebResponse().getContentAsString();
		JSONArray configs = JSONObject.fromObject(configsAsString).getJSONArray("configurations");
		for (int i = 0; i < configs.size(); i++) {
			JSONObject config = configs.getJSONObject(i);
			Assert.assertEquals("http://localhost:8008", config.getString("location"));
			Assert.assertEquals(ssp, config.getString("sharedSpace"));
			Assert.assertEquals("username", config.getString("username"));
			String instanceId = config.getString("serverIdentity");
			Assert.assertTrue(instanceId != null && !instanceId.isEmpty());
		}
	}

	@Test
	public void testSave() throws Exception {
		// basic scenario: location, shared space and credentials
		JSONObject config = new JSONObject();
		config.put("location", "http://localhost:8088");
		String sharedSP = UUID.randomUUID().toString();
		config.put("sharedSpace", sharedSP);
		config.put("username", "username1");
		config.put("password", "password1");
		WebRequest req = new WebRequest(client.createCrumbedUrl("nga/configuration/save"), HttpMethod.POST);
		req.setEncodingType(null);
		req.setRequestBody(config.toString());
		Page page = client.getPage(req);
		config = JSONObject.fromObject(page.getWebResponse().getContentAsString());
		checkConfig(config, "http://localhost:8088", sharedSP, "username1", Secret.fromString("password1"));

		String instanceId = config.getString("serverIdentity");
		Assert.assertTrue(instanceId != null && !instanceId.isEmpty());

		// location, shared space, no credentials
		config = new JSONObject();
		config.put("location", "http://localhost:8888");
		String sharedSP1 = UUID.randomUUID().toString();
		config.put("sharedSpace", sharedSP1);
		req.setRequestBody(config.toString());
		page = client.getPage(req);
		config = JSONObject.fromObject(page.getWebResponse().getContentAsString());
		checkConfig(config, "http://localhost:8888", sharedSP1, "", Secret.fromString(""));
//		Assert.assertEquals(instanceId, config.getString("serverIdentity"));

		// location, shared space and username without password
		config = new JSONObject();
		config.put("location", "http://localhost:8882");
		String sharedSP2 = UUID.randomUUID().toString();
		config.put("sharedSpace", sharedSP2);
		config.put("username", "username3");
		config.put("password", "password3");
		req.setRequestBody(config.toString());
		page = client.getPage(req);
		config = JSONObject.fromObject(page.getWebResponse().getContentAsString());
		checkConfig(config, "http://localhost:8882", sharedSP2, "username3", Secret.fromString("password3"));
//		Assert.assertEquals(instanceId, config.getString("serverIdentity"));

		// uiLocation and identity
		config = new JSONObject();
		String sharedSP3 = UUID.randomUUID().toString();
		config.put("uiLocation", "http://localhost:8881/ui?p=" + sharedSP3 + "/1002");
//		config.put("serverIdentity", "2d2fa955-1d13-4d8c-947f-ab11c72bf850");
		req.setRequestBody(config.toString());
		page = client.getPage(req);
		config = JSONObject.fromObject(page.getWebResponse().getContentAsString());
		checkConfig(config, "http://localhost:8881", sharedSP3, "", Secret.fromString(""));
//		Assert.assertEquals("2d2fa955-1d13-4d8c-947f-ab11c72bf850", config.getString("serverIdentity"));

		// requires POST
		req.setHttpMethod(HttpMethod.GET);
		try {
			client.getPage(req);
			Assert.fail("Only POST should be allowed");
		} catch (FailingHttpStatusCodeException ex) {
			// expected
		}
	}

	private void checkConfig(JSONObject config, String location, String sharedSpace, String username, Secret password) {
		// check values returned
		Assert.assertEquals(location, config.getString("location"));
		Assert.assertEquals(sharedSpace, config.getString("sharedSpace"));
		Assert.assertEquals(username, config.getString("username"));
		// check values stored
		OctaneServerSettingsModel serverConfiguration = ConfigurationService.getSettings(config.getString("serverIdentity"));
		Assert.assertEquals(location, serverConfiguration.getLocation());
		Assert.assertEquals(sharedSpace, serverConfiguration.getSharedSpace());
		Assert.assertEquals(username, serverConfiguration.getUsername());
		Assert.assertEquals(password, serverConfiguration.getPassword());
	}
}
