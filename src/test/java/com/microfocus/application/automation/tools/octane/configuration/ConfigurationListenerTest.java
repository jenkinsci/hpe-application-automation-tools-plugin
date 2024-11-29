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
import com.microfocus.application.automation.tools.octane.OctanePluginTestBase;
import com.microfocus.application.automation.tools.octane.tests.ExtensionUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.TestExtension;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationListenerTest extends OctanePluginTestBase {

	private TestConfigurationListener listener;

	@Before
	public void initialize() {
		listener = ExtensionUtil.getInstance(rule, TestConfigurationListener.class);
		Assert.assertEquals("Listener count doesn't match 1", 1, listener.getCount());

		List<OctaneServerSettingsModel> confs = listener.getConfigurationsChange();
		Assert.assertNotNull("Configuration is null", confs);
		Assert.assertEquals("Config size doesn't match 2", 2, confs.size());
		Assert.assertEquals("location doesn't match localhost:8008", "http://localhost:8008", confs.get(0).getLocation());
		Assert.assertEquals("username doesn't match username", "username", confs.get(0).getUsername());
		Assert.assertNull(confs.get(1));
	}

	@Test
	public void testConfigurationListener() throws Exception {
		OctaneServerSettingsModel oldModel = ConfigurationService.getAllSettings().get(0);
		OctaneServerSettingsModel newModel = new OctaneServerSettingsModel(oldModel.getUiLocation(), oldModel.getUsername(), oldModel.getPassword(), "");
		newModel.setIdentity(oldModel.getIdentity());
		newModel.setLocation(oldModel.getUiLocation());
		newModel.setInternalId(oldModel.getInternalId());
		// not increased on re-submit
		ConfigurationService.configurePlugin(newModel);
		Assert.assertEquals("Listener count doesn't match 1", 1, listener.getCount());

		oldModel = ConfigurationService.getAllSettings().get(0);
		newModel = new OctaneServerSettingsModel(oldModel.getUiLocation(),"username2", oldModel.getPassword(), "");
		newModel.setIdentity(oldModel.getIdentity());
		newModel.setInternalId(oldModel.getInternalId());
		// increased when configuration changes
		ConfigurationService.configurePlugin(newModel);
		Assert.assertEquals("Listener count doesn't match 2", 2, listener.getCount());

		List<OctaneServerSettingsModel> confs = listener.getConfigurationsChange();
		Assert.assertNotNull("Configuration is null", confs);
		Assert.assertEquals("Config count doesn't match 2", 2, confs.size());
		Assert.assertEquals("http://localhost:8008", confs.get(0).getLocation());
		Assert.assertEquals("username2", confs.get(0).getUsername());
		Assert.assertEquals("http://localhost:8008", confs.get(1).getLocation());
		Assert.assertEquals("username", confs.get(1).getUsername());
	}

	@TestExtension
	public static class TestConfigurationListener implements ConfigurationListener {

		private int count;
		private List<OctaneServerSettingsModel> newAndOld;

		@Override
		public void onChanged(OctaneServerSettingsModel newConf, OctaneServerSettingsModel oldConf) {
			++count;
			newAndOld = new ArrayList<>();
			newAndOld.add(newConf);
			newAndOld.add(oldConf);
		}

		public int getCount() {
			return count;
		}

		public List<OctaneServerSettingsModel> getConfigurationsChange() {
			return newAndOld;
		}
	}
}
