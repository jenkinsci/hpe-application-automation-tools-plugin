/*
 *
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * © Copyright 2012-2018 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors (“Micro Focus”) are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 *
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
