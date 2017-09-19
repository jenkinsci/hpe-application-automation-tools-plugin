/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.hpe.application.automation.tools.sse.sdk;

import com.hpe.application.automation.tools.model.CdaDetails;
import com.hpe.application.automation.tools.model.SseModel;
import com.hpe.application.automation.tools.sse.ArgsFactory;
import com.hpe.application.automation.tools.sse.result.model.junit.Testcase;
import com.hpe.application.automation.tools.sse.result.model.junit.Testsuites;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.hpe.application.automation.tools.rest.RestClient;
import com.hpe.application.automation.tools.sse.result.model.junit.JUnitTestCaseStatus;

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
