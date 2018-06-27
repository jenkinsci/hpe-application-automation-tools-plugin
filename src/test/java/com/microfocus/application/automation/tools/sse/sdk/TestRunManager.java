/*
 * © Copyright 2013 EntIT Software LLC
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright (c) 2018 Micro Focus Company, L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ___________________________________________________________________
 *
 */

package com.microfocus.application.automation.tools.sse.sdk;

import com.microfocus.application.automation.tools.model.SseModel;
import com.microfocus.application.automation.tools.sse.common.TestCase;
import org.junit.Assert;
import org.junit.Test;

import com.microfocus.application.automation.tools.model.CdaDetails;
import com.microfocus.application.automation.tools.rest.RestClient;
import com.microfocus.application.automation.tools.sse.ArgsFactory;
import com.microfocus.application.automation.tools.sse.result.model.junit.JUnitTestCaseStatus;
import com.microfocus.application.automation.tools.sse.result.model.junit.Testcase;
import com.microfocus.application.automation.tools.sse.result.model.junit.Testsuites;

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
