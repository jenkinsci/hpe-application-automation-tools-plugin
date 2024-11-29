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
package com.microfocus.application.automation.tools.octane.model;

import com.microfocus.application.automation.tools.model.OctaneServerSettingsModel;
import com.microfocus.application.automation.tools.octane.configuration.ConfigurationValidator;
import com.microfocus.application.automation.tools.octane.exceptions.AggregatedMessagesException;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class WorkspaceJenkinsUserTest {


    @Test
    public void testParseWorkspace2ImpersonatedUserConf() {
        String payload = "1001:user1" + System.lineSeparator() + //legal line
                "#1002:user2" + System.lineSeparator() + //comment line
                "1003:user1003:user1004" + System.lineSeparator() + //Expected error : Workspace configuration is not valid, valid format is 'Workspace ID:jenkins user': 1003:user1003:user1004
                "user1004:1004" + System.lineSeparator() + //Expected error : Workspace configuration is not valid, workspace ID must be numeric: user1004:1004
                "abc:abc" + System.lineSeparator() + //Expected error : Workspace configuration is not valid, workspace ID must be numeric: abc:abc
                "1005:" + System.lineSeparator() + //Expected error : Workspace configuration is not valid, user value is empty: 1005:
                "1001:user1a" + System.lineSeparator() + //Expected error : Duplicated workspace configuration: 1001:user1a

                "1010:user10" + System.lineSeparator(); //legal line

        //iteration 1 - ignore errors
        Map<Long, String> output = OctaneServerSettingsModel.parseWorkspace2ImpersonatedUserConf(payload, true);
        Assert.assertEquals(3, output.size());
        Assert.assertTrue(output.containsKey(1001L));
        Assert.assertEquals("user1", output.get(1001L));

        Assert.assertTrue(output.containsKey(1010L));
        Assert.assertEquals("user10", output.get(1010L));

        //iteration 1 - get errors
        try {
            OctaneServerSettingsModel.parseWorkspace2ImpersonatedUserConf(payload, false);
            Assert.fail("Exception expected");
        } catch (Exception e) {
            if (e instanceof AggregatedMessagesException) {
                List<String> messages = ((AggregatedMessagesException) e).getMessages();
                Assert.assertEquals(4, messages.size());
                //Assert.assertTrue(messages.contains("Workspace configuration is not valid, valid format is 'Workspace ID:jenkins user': 1003:user1003:user1004"));
                Assert.assertTrue(messages.contains("Workspace configuration is not valid, workspace ID must be numeric: user1004:1004"));
                Assert.assertTrue(messages.contains("Workspace configuration is not valid, workspace ID must be numeric: abc:abc"));
                Assert.assertTrue(messages.contains("Workspace configuration is not valid, user value is empty: 1005:"));
                Assert.assertTrue(messages.contains("Duplicated workspace configuration: 1001:user1a"));
            } else {
                Assert.fail("Wrong type of exception : " + e.getClass().getName());
            }
        }
    }
}
