/*
 * Certain versions of software accessible here may contain branding from Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 * This software was acquired by Micro Focus on September 1, 2017, and is now offered by OpenText.
 * Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright 2012-2024 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.pc;

import com.microfocus.application.automation.tools.run.PcBuilder;
import hudson.FilePath;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.microfocus.adm.performancecenter.plugins.common.rest.PcRestProxy;

import com.microfocus.adm.performancecenter.plugins.common.pcentities.*;

@SuppressWarnings({"squid:S2699","squid:S3658"})
public class TestPcClient {
         
    private static PcClient pcClient;
    public final String RESOURCES_DIR = getClass().getResource("").getPath();

    @BeforeClass
    public static void setUp() {
        try {
            PcRestProxy resetProxy = new MockPcRestProxy(PcTestBase.WEB_PROTOCOL, PcTestBase.PC_SERVER_NAME, PcTestBase.AUTHENTICATE_WITH_TOKEN, PcTestBase.ALM_DOMAIN,
                PcTestBase.ALM_PROJECT,PcTestBase.LOGGER);
            pcClient = new PcClient(PcTestBase.pcModel, System.out, resetProxy);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    @Test
    public void testStartRun(){
        System.out.println("Testing Start Run with PC client");
        try {
            Assert.assertTrue("Failed to start run with pcClient", pcClient.startRun() > 0);
        } catch (Exception e) {
            Assert.fail(e.toString());
        }       
    }

    @Test (timeout=5000)
    public void testWaitForRunCompletion(){
        
        System.out.println("Testing Wait for Run Completion with PC client");
        try {
            PcRunResponse response = pcClient.waitForRunCompletion(Integer.parseInt(PcTestBase.RUN_ID_WAIT), 200);
            Assert.assertEquals(response.getRunState(), RunState.FINISHED.value());
        } catch (InterruptedException e) {            
            Assert.fail("pcClient did not return from waitForRunCompletion (test run has timed out)");
        }catch (Exception e) {
            Assert.fail(e.toString());
        }       
    }
    
    @Test
    public void testPublishRunReport(){
        
        System.out.println("Testing Publish PC Run Report to Jenkins server with PC client");
        try {
            
            FilePath reportHtml = pcClient.publishRunReport(Integer.parseInt(PcTestBase.RUN_ID),
                String.format(PcBuilder.getRunReportStructure(), RESOURCES_DIR, PcBuilder.getArtifactsDirectoryName(), PcTestBase.RUN_ID));
            Assert.assertTrue("Failed to publish PC run report", reportHtml.exists());
            try {
                // Test cleanup
                reportHtml.getParent().getParent().getParent().deleteRecursive();
            } catch (Exception e) {
            }
        } catch (Exception e) {
            Assert.fail(e.toString());
        }       
    }    
    
    @Test
    public void testLogout() {        
        System.out.println("Testing Logout from PC server");
        Assert.assertTrue("Failed to logout with pcClient", pcClient.logout());
    }

    @Test
    public void testStopRun() {        
        System.out.println("Testing Stop Run with PC client");
        Assert.assertTrue("Failed to stop run with pcClient", pcClient.stopRun(Integer.parseInt(PcTestBase.RUN_ID)));
    }
    
    
}
  
