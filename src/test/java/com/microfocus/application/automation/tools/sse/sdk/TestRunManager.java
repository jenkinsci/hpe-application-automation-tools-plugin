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

import com.microfocus.application.automation.tools.common.SSEException;
import com.microfocus.application.automation.tools.model.SseModel;
import com.microfocus.application.automation.tools.sse.common.TestCase;
import com.microfocus.application.automation.tools.sse.common.XPathUtils;
import com.microfocus.application.automation.tools.sse.sdk.request.GetTestInstancesRequest;
import org.junit.Assert;
import org.junit.Test;

import com.microfocus.application.automation.tools.model.CdaDetails;
import com.microfocus.application.automation.tools.rest.RestClient;
import com.microfocus.application.automation.tools.sse.ArgsFactory;
import com.microfocus.application.automation.tools.sse.result.model.junit.JUnitTestCaseStatus;
import com.microfocus.application.automation.tools.sse.result.model.junit.Testcase;
import com.microfocus.application.automation.tools.sse.result.model.junit.Testsuites;

import java.util.Collections;

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
        Assert.assertTrue(verifyEmpty(testsuites, connection, args) || verifyTestsuitesFunctional(testsuites, "bvs"));
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
        Assert.assertTrue(verifyEmpty(testsuites, connection, args) || verifyTestsuitesFunctional(testsuites, "bvs"));
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
        Assert.assertTrue(verifyEmpty(testsuites, connection, args) || verifyTestsuitesFunctional(testsuites, "bvs"));
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
        Assert.assertTrue(verifyEmpty(testsuites, connection, args) || verifyTestsuitesFunctional(testsuites, "testset"));
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
        Assert.assertTrue(verifyEmpty(testsuites, connection, args) || verifyTestsuitesPC(testsuites, "testset"));
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

        Assert.assertTrue(verifyEmpty(testsuites, connection, args));
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
        
        Assert.assertNotNull(testsuites);
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
        
        Assert.assertTrue(thrown || verifyEmpty(new Testsuites(), connection, args));
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

    private boolean verifyEmpty(Testsuites testsuites, RestClient connection, Args args) {
        boolean ret = false;

        Response res = new GetTestInstancesRequest(connection, args.getEntityId()).execute();

        ret = !res.isOk() || res.getData() == null || !XPathUtils.hasResults(res.toString());

        return ret && testsuites.getTestsuite().isEmpty();
    }

}
