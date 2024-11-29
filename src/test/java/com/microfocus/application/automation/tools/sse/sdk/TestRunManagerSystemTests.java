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
package com.microfocus.application.automation.tools.sse.sdk;

import com.microfocus.application.automation.tools.model.CdaDetails;
import com.microfocus.application.automation.tools.model.SseModel;
import com.microfocus.application.automation.tools.sse.ArgsFactory;
import com.microfocus.application.automation.tools.sse.result.model.junit.Testcase;
import com.microfocus.application.automation.tools.sse.result.model.junit.Testsuites;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.microfocus.application.automation.tools.rest.RestClient;
import com.microfocus.application.automation.tools.sse.result.model.junit.JUnitTestCaseStatus;

/**
 * 
 * @author Amir Zahavi
 * 
 */
@Ignore
@SuppressWarnings("squid:S2699")
public class TestRunManagerSystemTests {
    
    private static final String SERVER_NAME = "zahavia1.emea.hpqcorp.net";
    private static final int PORT = 8080;
    private static final String PROJECT = "Project1";
    private static int TEST_SET_ID = 2;
    private static int BVS_ID = 1084;
    
    @Test
    public void testEndToEndTestSet() throws InterruptedException {
        
        SseModel model = createModel(SseModel.TEST_SET, SERVER_NAME, PROJECT, PORT, TEST_SET_ID);
        Args args = new ArgsFactory().create(model);
        RestClient connection =
                new RestClient(
                        args.getUrl(),
                        args.getDomain(),
                        args.getProject(),
                        args.getUsername());
        Testsuites testsuites = new RunManager().execute(connection, args, new ConsoleLogger());
        
        assertTestsuitesPassed(testsuites);
    }
    
    @Test
    public void testEndToEndBVS() throws InterruptedException {
        
        SseModel model = createModel(SseModel.BVS, SERVER_NAME, PROJECT, PORT, BVS_ID);
        Args args = new ArgsFactory().create(model);
        RestClient connection =
                new RestClient(
                        args.getUrl(),
                        args.getDomain(),
                        args.getProject(),
                        args.getUsername());
        Testsuites testsuites = new RunManager().execute(connection, args, new ConsoleLogger());
        
        Assert.assertNotNull(testsuites);
        assertTestsuitesPassed(testsuites);
    }
    
    private void assertTestsuitesPassed(Testsuites testsuites) {
        
        Testcase testcase = testsuites.getTestsuite().get(0).getTestcase().get(0);
        Assert.assertNotNull(testcase);
        Assert.assertTrue(
                "Test did not run successfully",
                testcase.getStatus().equals(JUnitTestCaseStatus.PASS));
    }
    
    /**
     * 
     * @param entityType
     *            "BVS" or "TEST_SET"
     */
    private SseModel createModel(
            String entityType,
            String serverName,
            String projectName,
            int port,
            int testSetID) {
        
        final String userName = "sa";
        String description = "";
        final String password = "";
        String domain = "DEFAULT";
        String timeslotDuration = "30";
        String postRunAction = "Collate";
        String environmentConfigurationId = "";
        CdaDetails cdaDetails = null;
        SseModel ret =
                new MockSseModel(
                        serverName,
                        userName,
                        password,
                        domain,
                        projectName,
                        entityType,
                        String.valueOf(testSetID),
                        timeslotDuration,
                        description,
                        postRunAction,
                        environmentConfigurationId,
                        cdaDetails,
                        null);
        ret.setAlmServerUrl(String.format("http://%s:%d/qcbin", serverName, port));
        
        return ret;
    }
    
}
