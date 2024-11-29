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
package com.microfocus.application.automation.tools.octane.actions.plugin;

import com.hp.octane.integrations.dto.general.CIJobsList;
import com.hp.octane.integrations.dto.general.CIProviderSummaryInfo;
import com.hp.octane.integrations.dto.general.CIServerTypes;
import com.hp.octane.integrations.dto.parameters.CIParameterType;
import com.hp.octane.integrations.dto.pipelines.PipelineNode;
import com.microfocus.application.automation.tools.octane.OctanePluginTestBase;
import com.microfocus.application.automation.tools.octane.actions.PluginActions;
import com.microfocus.application.automation.tools.octane.configuration.ConfigurationService;
import com.microfocus.application.automation.tools.octane.tests.TestUtils;
import hudson.model.*;
import jenkins.model.Jenkins;
import org.htmlunit.Page;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: gullery
 * Date: 07/01/15
 * Time: 22:09
 * To change this template use File | Settings | File Templates.
 */

@SuppressWarnings({"squid:S2699", "squid:S3658", "squid:S2259", "squid:S1872", "squid:S2925", "squid:S109", "squid:S1607", "squid:S2701", "squid:S3578", "squid:S2698"})
public class PluginActionsTest extends OctanePluginTestBase {

	@Test
	public void testPluginActionsMethods() {
		PluginActions pluginActions = new PluginActions();
		assertNull(pluginActions.getIconFileName());
		assertNull(pluginActions.getDisplayName());
		assertEquals("nga", pluginActions.getUrlName());
	}

	@Ignore("temp ignore")
	@Test
	public void testPluginActions_REST_Status() throws IOException, SAXException {
		Page page = client.goTo("nga/api/v1/status", "application/json");
		System.out.println(page.getWebResponse().getContentAsString());
		CIProviderSummaryInfo status = dtoFactory.dtoFromJson(page.getWebResponse().getContentAsString(), CIProviderSummaryInfo.class);

		assertNotNull(status);

		assertNotNull(status.getServer());
		assertEquals(CIServerTypes.JENKINS.value(), status.getServer().getType());
		assertEquals(Jenkins.VERSION, status.getServer().getVersion());
		assertEquals(rule.getInstance().getRootUrl(), status.getServer().getUrl() + "/");
		assertNotNull(status.getServer().getSendingTime());

		assertNotNull(status.getPlugin());
		assertEquals(ConfigurationService.getPluginVersion(), status.getPlugin().getVersion());
	}

	@Test
	public void testPluginActions_REST_Jobs_NoParams() throws IOException, SAXException {
		String projectName = "root-job-" + UUID.randomUUID().toString();

		String taskUrl = "nga/api/v1/jobs";
		CIJobsList response = TestUtils.sendTask(taskUrl, CIJobsList.class);

		assertNotNull(response);
		assertNotNull(response.getJobs());
		assertEquals(rule.getInstance().getTopLevelItemNames().size(), response.getJobs().length);

		rule.createFreeStyleProject(projectName);
		response = TestUtils.sendTask(taskUrl, CIJobsList.class);

		assertNotNull(response);
		assertNotNull(response.getJobs());
		for (PipelineNode ciJob : response.getJobs()) {
			if (projectName.equals(ciJob.getName())) {
				assertNotNull(ciJob.getParameters());
				assertEquals(0, ciJob.getParameters().size());
			}
		}
	}

	@Test
	public void testPluginActions_REST_Jobs_WithParams() throws IOException, SAXException {
		String projectName = "root-job-" + UUID.randomUUID().toString();
		FreeStyleProject fsp;

		String taskUrl = "nga/api/v1/jobs";
		CIJobsList response = TestUtils.sendTask(taskUrl, CIJobsList.class);

		assertNotNull(response);
		assertNotNull(response.getJobs());
		assertEquals(0, response.getJobs().length);

		fsp = rule.createFreeStyleProject(projectName);
		ParametersDefinitionProperty params = new ParametersDefinitionProperty(Arrays.asList(
				(ParameterDefinition) new BooleanParameterDefinition("ParamA", true, "bool"),
				(ParameterDefinition) new StringParameterDefinition("ParamB", "str", "string"),
				(ParameterDefinition) new FileParameterDefinition("ParamC", "file param")
		));
		fsp.addProperty(params);
		response = TestUtils.sendTask(taskUrl, CIJobsList.class);

		assertNotNull(response);
		assertNotNull(response.getJobs());
		assertEquals(1, response.getJobs().length);
		assertEquals(projectName, response.getJobs()[0].getName());
		assertNotNull(response.getJobs()[0].getParameters());
		assertEquals(3, response.getJobs()[0].getParameters().size());

		//  Test ParamA
		assertNotNull(response.getJobs()[0].getParameters().get(0));
		assertEquals("ParamA", response.getJobs()[0].getParameters().get(0).getName());
		assertEquals(CIParameterType.BOOLEAN, response.getJobs()[0].getParameters().get(0).getType());
		assertEquals("bool", response.getJobs()[0].getParameters().get(0).getDescription());
		assertEquals(true, response.getJobs()[0].getParameters().get(0).getDefaultValue());

		//  Test ParamB
		assertNotNull(response.getJobs()[0].getParameters().get(1));
		assertEquals("ParamB", response.getJobs()[0].getParameters().get(1).getName());
		assertEquals(CIParameterType.STRING, response.getJobs()[0].getParameters().get(1).getType());
		assertEquals("string", response.getJobs()[0].getParameters().get(1).getDescription());
		assertEquals("str", response.getJobs()[0].getParameters().get(1).getDefaultValue());

		//  Test ParamC
		assertNotNull(response.getJobs()[0].getParameters().get(2));
		assertEquals("ParamC", response.getJobs()[0].getParameters().get(2).getName());
		assertEquals(CIParameterType.FILE, response.getJobs()[0].getParameters().get(2).getType());
		assertEquals("file param", response.getJobs()[0].getParameters().get(2).getDescription());
		assertEquals("", response.getJobs()[0].getParameters().get(2).getDefaultValue());
	}
}
