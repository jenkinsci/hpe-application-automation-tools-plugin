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

import com.hpe.application.automation.tools.model.SseModel;
import com.hpe.application.automation.tools.sse.common.TestCase;
import org.junit.Assert;
import org.junit.Test;

import com.hpe.application.automation.tools.model.CdaDetails;
import com.hpe.application.automation.tools.rest.RestClient;
import com.hpe.application.automation.tools.sse.ArgsFactory;
import com.hpe.application.automation.tools.sse.result.model.junit.JUnitTestCaseStatus;
import com.hpe.application.automation.tools.sse.result.model.junit.Testcase;
import com.hpe.application.automation.tools.sse.result.model.junit.Testsuites;

@SuppressWarnings({"squid:S2698","squid:S2699"})
public class TestRunManager extends TestCase {
    
    @Test
    public void testEndToEndBVS() throws InterruptedException {
        
        SseModel model = createBvsModel();
        model.setAlmServerUrl(URL);
        Args args = new ArgsFactory().create(model);
        RestClient connection =
                new MockRestClientFunctional(
                        args.getUrl(),
                        args.getDomain(),
                        args.getProject(),
                        args.getUsername());
        Testsuites testsuites = new RunManager().execute(connection, args, new ConsoleLogger());
        
        Assert.assertNotNull(testsuites);
        Assert.assertTrue(verifyTestsuitesFunctional(testsuites, "bvs"));
    }
    
    @Test
    public void testEndToEndBVSWithCDA() throws InterruptedException {
        
        SseModel model = createBvsModelWithCDA();
        model.setAlmServerUrl(URL);
        Args args = new ArgsFactory().create(model);
        RestClient connection =
                new MockRestClientFunctional(
                        args.getUrl(),
                        args.getDomain(),
                        args.getProject(),
                        args.getUsername());
        Testsuites testsuites = new RunManager().execute(connection, args, new ConsoleLogger());
        Assert.assertNotNull(testsuites);
        Assert.assertTrue(verifyTestsuitesFunctional(testsuites, "bvs"));
    }
    
    @Test
    public void testEndToEndBVSWithEmptyCDA() throws InterruptedException {
        
        SseModel model = createBvsModelWithEmptyCDA();
        model.setAlmServerUrl(URL);
        Args args = new ArgsFactory().create(model);
        RestClient connection =
                new MockRestClientFunctional(
                        args.getUrl(),
                        args.getDomain(),
                        args.getProject(),
                        args.getUsername());
        Testsuites testsuites = new RunManager().execute(connection, args, new ConsoleLogger());
        Assert.assertNotNull(testsuites);
        Assert.assertTrue(verifyTestsuitesFunctional(testsuites, "bvs"));
    }
    
    @Test
    public void testEndToEndTestSet() throws InterruptedException {
        
        SseModel model = createTestSetModel();
        model.setAlmServerUrl(URL);
        Args args = new ArgsFactory().create(model);
        RestClient connection =
                new MockRestClientFunctional(
                        args.getUrl(),
                        args.getDomain(),
                        args.getProject(),
                        args.getUsername());
        Testsuites testsuites = new RunManager().execute(connection, args, new ConsoleLogger());
        
        Assert.assertNotNull(testsuites);
        Assert.assertTrue(verifyTestsuitesFunctional(testsuites, "testset"));
    }
    
    @Test
    public void testEndToEndPC() throws InterruptedException {
        
        SseModel model = createPCModel();
        model.setAlmServerUrl(URL);
        Args args = new ArgsFactory().create(model);
        RestClient connection =
                new MockRestClientPC(
                        args.getUrl(),
                        args.getDomain(),
                        args.getProject(),
                        args.getUsername());
        Testsuites testsuites = new RunManager().execute(connection, args, new ConsoleLogger());
        
        Assert.assertNotNull(testsuites);
        Assert.assertTrue(verifyTestsuitesPC(testsuites, "testset"));
    }
    
    @Test
    public void testBadRunEntity() throws InterruptedException {
        
        SseModel model = createBvsModel();
        model.setAlmServerUrl(URL);
        Args args = new ArgsFactory().create(model);
        RestClient connection =
                new MockRestClientBadRunEntity(
                        args.getUrl(),
                        args.getDomain(),
                        args.getProject(),
                        args.getUsername());
        RunManager runManager = new RunManager();
        Testsuites testsuites = runManager.execute(connection, args, new ConsoleLogger());
        
        Assert.assertNull(testsuites);
        
    }
    
    @Test
    public void testLoginFailed() throws InterruptedException {
        
        SseModel model = createBvsModel();
        model.setAlmServerUrl(URL);
        Args args = new ArgsFactory().create(model);
        RestClient connection =
                new MockRestClientFailedLogin(
                        args.getUrl(),
                        args.getDomain(),
                        args.getProject(),
                        args.getUsername());
        Testsuites testsuites = new RunManager().execute(connection, args, new ConsoleLogger());
        
        Assert.assertNull(testsuites);
    }
    
    @Test
    public void testBadDomain() throws InterruptedException {
        
        SseModel model = createBvsModel();
        model.setAlmServerUrl(URL);
        Args args = new ArgsFactory().create(model);
        RestClient connection =
                new MockRestClientFailedLogin(
                        args.getUrl(),
                        args.getDomain(),
                        args.getProject(),
                        args.getUsername());
        Testsuites testsuites = new RunManager().execute(connection, args, new ConsoleLogger());
        
        Assert.assertNull(testsuites);
    }
    
    @Test
    public void testBadRunResponse() {
        
        SseModel model = createTestSetModel();
        model.setAlmServerUrl(URL);
        Args args = new ArgsFactory().create(model);
        RestClient connection =
                new MockRestClientBadRunResponse(
                        args.getUrl(),
                        args.getDomain(),
                        args.getProject(),
                        args.getUsername());
        boolean thrown = false;
        try {
            new RunManager().execute(connection, args, new ConsoleLogger());
        } catch (Throwable cause) {
            thrown = true;
        }
        
        Assert.assertTrue(thrown);
    }
    
    private MockSseModel createBvsModel() {
        
        return new MockSseModel(
                SERVERNAME,
                USER,
                PASS,
                DOMAIN,
                PROJECT,
                "BVS",
                "1041",
                DURATION,
                DESCRIPTION,
                POST_RUN_ACTION,
                ENVIRONMENT_CONFIGURATION_ID,
                null,
                null);
    }
    
    private MockSseModel createBvsModelWithCDA() {
        
        return new MockSseModel(
                SERVERNAME,
                USER,
                PASS,
                DOMAIN,
                PROJECT,
                "BVS",
                "1041",
                DURATION,
                DESCRIPTION,
                POST_RUN_ACTION,
                ENVIRONMENT_CONFIGURATION_ID,
                CDA_DETAILS,
                null);
    }
    
    private MockSseModel createBvsModelWithEmptyCDA() {
        
        return new MockSseModel(
                SERVERNAME,
                USER,
                PASS,
                DOMAIN,
                PROJECT,
                "BVS",
                "1041",
                DURATION,
                DESCRIPTION,
                POST_RUN_ACTION,
                ENVIRONMENT_CONFIGURATION_ID,
                new CdaDetails("", "", ""),
                null);
    }
    
    private MockSseModel createTestSetModel() {
        
        return new MockSseModel(
                SERVERNAME,
                USER,
                PASS,
                DOMAIN,
                PROJECT,
                "TEST_SET",
                "1041",
                DURATION,
                DESCRIPTION,
                POST_RUN_ACTION,
                ENVIRONMENT_CONFIGURATION_ID,
                null,
                null);
    }
    
    private MockSseModel createPCModel() {
        
        return new MockSseModel(
                SERVERNAME,
                USER,
                PASS,
                DOMAIN,
                PROJECT,
                "PC",
                "1041",
                DURATION,
                DESCRIPTION,
                POST_RUN_ACTION,
                ENVIRONMENT_CONFIGURATION_ID,
                null,
                null);
    }
    
    private boolean verifyTestsuitesFunctional(Testsuites testsuites, String runType) {
        
        boolean ret = true;
        Testcase testcase = testsuites.getTestsuite().get(0).getTestcase().get(0);
        ret =
                (testcase.getStatus().equals(JUnitTestCaseStatus.PASS))
                        && (testcase.getName().equals("vapi1"))
                        && (testcase.getClassname().equals(String.format("%s1 (id:1041).testset1", runType))) && (testcase.getTime().equals("0.0"));
        
        return ret;
    }
    
    private boolean verifyTestsuitesPC(Testsuites testsuites, String runType) {
        
        boolean ret = true;
        Testcase testcase = testsuites.getTestsuite().get(0).getTestcase().get(0);
        ret =
                (testcase.getStatus().equals("error"))
                        && (testcase.getName().equals("Unnamed test"))
                        && (testcase.getClassname().equals("PC Test ID: 5, Run ID: 42, Test Set ID: 1.(Unnamed test set)"))
                        && (testcase.getTime().equals("0.0"));
        
        return ret;
    }
}
