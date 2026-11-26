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
 *  Copyright 2012-2026 Open Text.
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
package com.microfocus.application.automation.tools.pipelineSteps;

import org.apache.logging.log4j.LogManager;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlSelect;
import org.htmlunit.html.HtmlTextArea;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PcBuildStepTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @After
    public void closeLogFiles() {
        // Plugin log appenders otherwise keep JenkinsRule's temporary files locked on Windows.
        LogManager.shutdown();
    }

    @Test
    public void snippetGeneratorRendersFormAndGeneratesPcBuild() throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "pc");
        try (JenkinsRule.WebClient client = jenkins.createWebClient()) {
            HtmlPage page = client.goTo(job.getUrl() + "pipeline-syntax/");
            HtmlForm form = page.getFormByName("config");
            HtmlSelect steps = page.querySelector("select.dropdownList");
            steps.setSelectedAttribute(steps.getOptionByText("pcBuild: Performance Center Load Test"), true);
            jenkins.waitUntilNoActivity();
            client.waitForBackgroundJavaScript(10000);

            assertFalse(page.asNormalizedText().contains("This step has not yet defined any visual configuration."));
            form.getInputByName("_.pcServerName").setValue("performance.example");
            form.getInputByName("_.almDomain").setValue("DEFAULT");
            form.getInputByName("_.almProject").setValue("project");
            form.getInputByName("_.testId").setValue("42");
            assertEquals("0", form.getInputByName("_.timeslotDurationHours").getValue());
            assertEquals("30", form.getInputByName("_.timeslotDurationMinutes").getValue());
            assertEquals("AUTO", form.getSelectByName("_.autoTestInstanceID").getSelectedOptions().get(0).getValueAttribute());
            assertFalse(form.getSelectByName("_.postRunAction").getOptions().isEmpty());
            assertFalse(form.getSelectByName("_.credentialsId").getOptions().isEmpty());
            assertFalse(form.getSelectByName("_.credentialsProxyId").getOptions().isEmpty());

            page.getHtmlElementById("generatePipelineScript").click();
            client.waitForBackgroundJavaScript(10000);

            HtmlTextArea output = page.getHtmlElementById("prototypeText");
            String snippet = output.getText();
            assertTrue(snippet, snippet.startsWith("pcBuild "));
            assertTrue(snippet, snippet.contains("pcServerName: 'performance.example'"));
            assertTrue(snippet, snippet.contains("testId: '42'"));
            assertTrue(snippet, snippet.contains("autoTestInstanceID: 'AUTO'"));
        }
    }
}
