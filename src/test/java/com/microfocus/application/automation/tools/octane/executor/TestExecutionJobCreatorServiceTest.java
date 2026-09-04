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
package com.microfocus.application.automation.tools.octane.executor;

import com.microfocus.application.automation.tools.model.LoggedJenkinsRule;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import org.junit.Rule;
import org.junit.Test;

import java.lang.reflect.Method;

import static com.microfocus.application.automation.tools.octane.executor.UftConstants.AUTONOMOUS_TESTER_LABEL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that Autonomous Tester jobs are restricted to nodes with the dedicated Autonomous Tester
 * label, whatever its casing.
 */
public class TestExecutionJobCreatorServiceTest {

    @Rule
    public LoggedJenkinsRule jenkins = new LoggedJenkinsRule();

    @Test
    public void addAutonomousTesterAssignedNode_matchesOnlyNodesWithTheLabel() throws Exception {
        FreeStyleProject proj = jenkins.createFreeStyleProject("aute-job");
        invokeAddAutonomousTesterAssignedNode(proj);

        assertEquals(AUTONOMOUS_TESTER_LABEL, proj.getAssignedLabelString());

        assertTrue(matches(proj, jenkins.createSlave("at-node-1", "AUTONOMOUS_TESTER", null)));
        assertTrue(matches(proj, jenkins.createSlave("at-node-2", "AT", null)));
        assertFalse(matches(proj, jenkins.createSlave("uft-node", "UFT", null)));

    }

    private static boolean matches(FreeStyleProject proj, Node node) {
        return proj.getAssignedLabel().matches(node);
    }

    private static void invokeAddAutonomousTesterAssignedNode(FreeStyleProject proj) throws Exception {
        Method method = TestExecutionJobCreatorService.class
                .getDeclaredMethod("addAutonomousTesterAssignedNode", FreeStyleProject.class);
        method.setAccessible(true);
        method.invoke(null, proj);
    }
}
