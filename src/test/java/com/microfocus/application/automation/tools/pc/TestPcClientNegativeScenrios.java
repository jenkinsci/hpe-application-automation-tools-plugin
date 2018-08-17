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

package com.microfocus.application.automation.tools.pc;

import hudson.FilePath;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.microfocus.application.automation.tools.run.PcBuilder;

import com.microfocus.adm.performancecenter.plugins.common.rest.PcRestProxy;
import com.microfocus.adm.performancecenter.plugins.common.pcEntities.*;

@SuppressWarnings({"squid:S2699","squid:S3658"})
public class TestPcClientNegativeScenrios {

    private static PcClient  pcClient;
    public final String      RESOURCES_DIR = getClass().getResource("").getPath();

    @Rule
    public ExpectedException exception     = ExpectedException.none();

    @BeforeClass
    public static void setUp() {
        System.out.println("Starting Performance Center client negative testing scenarios:");
        try {
            PcRestProxy resetProxy = new MockPcRestProxyBadResponses(PcTestBase.WEB_PROTOCOL,PcTestBase.PC_SERVER_NAME, PcTestBase.ALM_DOMAIN,
                PcTestBase.ALM_PROJECT,PcTestBase.LOGGER);
            pcClient = new PcClient(PcTestBase.pcModel, new PrintStream(new OutputStream() {
                
                @Override
                public void write(int b) throws IOException {}
                
            }), resetProxy);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    @AfterClass
    public static void tearDown() {
        System.out.println("End of Performance Center client negative testing scenarios");
    }

    @Test
    public void testLoginWithWrongCredentials() {

        System.out.println("Testing Login to PC server with wrong credentials");
        Assert.assertFalse("Login to PC server with wrong creadentials should have failed", pcClient.login());
    }

    @Test(timeout = 5000)
    public void testHandleRunFailureWhileWaitingForRunCompletion() {

        System.out.println("Testing Wait For Run Completion with PC client while run fails");
        try {
            PcRunResponse response = pcClient.waitForRunCompletion(Integer.parseInt(PcTestBase.RUN_ID_WAIT), 200);
            Assert.assertEquals(response.getRunState(), RunState.RUN_FAILURE.value());
        } catch (InterruptedException e) {
            Assert.fail("pcClient did not return from waitForRunCompletion (test run has timed out)");
        } catch (Exception e) {
            Assert.fail(e.toString());
        }
    }

    @Test
    public void testPublishRunReportWithEmptyResults() {

        System.out.println("Testing Publish PC Run Report to while run results are empty");
        try {

            FilePath reportHtml = pcClient.publishRunReport(Integer.parseInt(PcTestBase.RUN_ID),
                    String.format(PcBuilder.getRunReportStructure(), RESOURCES_DIR, PcBuilder.getArtifactsDirectoryName(),PcTestBase.RUN_ID));
            Assert.assertNull("pcClient.publishRunReport should have returned null due to empty run results",
                reportHtml);
        } catch (Exception e) {
            Assert.fail("pcClient.publishRunReport threw an exception (should have returned null due to empty run results): "
                        + e.toString());
        }
    }

    @Test
    public void testStopNonExistingRun() {
        
        System.out.println("Testing stopping a non-exising run with PC client");
        Assert.assertFalse("Stopping a non-existing run should have failed", pcClient.stopRun(Integer.parseInt(PcTestBase.RUN_ID)));
    }

}
