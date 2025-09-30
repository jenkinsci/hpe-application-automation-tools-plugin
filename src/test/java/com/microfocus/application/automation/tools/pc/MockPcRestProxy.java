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
package com.microfocus.application.automation.tools.pc;

import com.microfocus.adm.performancecenter.plugins.common.pcentities.PcException;
import com.microfocus.adm.performancecenter.plugins.common.pcentities.RunState;
import com.microfocus.adm.performancecenter.plugins.common.rest.PcRestProxy;
import com.microfocus.application.automation.tools.run.PcBuilder;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.microfocus.adm.performancecenter.plugins.common.pcentities.RunState.*;
import static com.microfocus.application.automation.tools.pc.PcTestBase.STOP_MODE;

public class MockPcRestProxy extends PcRestProxy {

    private static Iterator<RunState> runState = initializeRunStateIterator();

    public MockPcRestProxy(String webProtocol, String pcServerName, boolean authenticateWithToken, String almDomain, String almProject,PrintStream logger) throws PcException {
        super(webProtocol, pcServerName, authenticateWithToken, almDomain, almProject,null,null,null);
    }

    @Override
    protected CloseableHttpResponse executeRawRequest(HttpRequestBase request) throws PcException, IOException {
        String url = request.getURI().toString();

        // Publish run report endpoint (fake ZIP)
        if (url.contains("/Runs/") && url.contains("/Results/") && url.endsWith("/data")) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                zos.putNextEntry(new ZipEntry(PcBuilder.pcReportFileName));
                zos.write("<html><body>Fake report</body></html>".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
            BasicHttpResponse basic = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1), 200, "OK");
            basic.setEntity(new ByteArrayEntity(baos.toByteArray()));
            return new CloseableHttpResponseAdapter(basic);
        }

        // Test instances endpoint
        else if (url.contains("/testinstances")) {
            String fakeXml = "<TestInstances><TestInstance id=\"123\"/></TestInstances>";
            BasicHttpResponse basic = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1), 200, "OK");
            basic.setEntity(new StringEntity(fakeXml, StandardCharsets.UTF_8));
            return new CloseableHttpResponseAdapter(basic);
        }

        // Fallback for other endpoints
        else {
            BasicHttpResponse basic = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1), 404, "Not Found");
            return new CloseableHttpResponseAdapter(basic);
        }
    }


    @Override
    @SuppressWarnings("unused")
    protected String executeRequest(HttpRequestBase request) throws PcException, ClientProtocolException, IOException {
        HttpResponse response = null;
        String requestUrl = request.getURI().toString();

        // --- LOGIN / LOGOUT / STOP RUN ---
        if (requestUrl.equals(String.format(AUTHENTICATION_LOGIN_URL, PcTestBase.WEB_PROTOCOL, PcTestBase.PC_SERVER_NAME))
            || requestUrl.equals(String.format(AUTHENTICATION_LOGOUT_URL, PcTestBase.WEB_PROTOCOL, PcTestBase.PC_SERVER_NAME))
            // FIXED: Stop Run is handled here
            || requestUrl.equals(String.format(getBaseURL() + "/%s/%s/%s", RUNS_RESOURCE_NAME, PcTestBase.RUN_ID, STOP_MODE))) {
            response = getOkResponse();
            // Optionally, return some dummy content that executeRequest() can convert to string
            response.setEntity(new StringEntity("<StopRunResponse>OK</StopRunResponse>", StandardCharsets.UTF_8));
        }

        // --- RUNS endpoint ---
        else if (requestUrl.equals(String.format(getBaseURL() + "/%s", RUNS_RESOURCE_NAME))
                 || requestUrl.equals(String.format(getBaseURL() + "/%s/%s", RUNS_RESOURCE_NAME, PcTestBase.RUN_ID))) {
            response = getOkResponse();
            response.setEntity(new StringEntity(PcTestBase.runResponseEntity));
        }

        // --- TESTS endpoint ---
        else if (requestUrl.equals(String.format(getBaseURL() + "/%s", TESTS_RESOURCE_NAME))
                 || requestUrl.equals(String.format(getBaseURL() + "/%s/%s", TESTS_RESOURCE_NAME, PcTestBase.TEST_ID))) {
            response = getOkResponse();
            response.setEntity(new StringEntity(PcTestBase.testResponseEntity));
        }

        // --- RUNS WAIT endpoint ---
        else if (requestUrl.equals(String.format(getBaseURL() + "/%s/%s", RUNS_RESOURCE_NAME, PcTestBase.RUN_ID_WAIT))) {
            response = getOkResponse();
            response.setEntity(new StringEntity(PcTestBase.runResponseEntity.replace("*", runState.next().value())));
            if (!runState.hasNext())
                runState = initializeRunStateIterator();
        }

        // --- RUN RESULTS endpoint ---
        else if (requestUrl.equals(String.format(getBaseURL() + "/%s/%s/%s", RUNS_RESOURCE_NAME, PcTestBase.RUN_ID, RESULTS_RESOURCE_NAME))) {
            response = getOkResponse();
            response.setEntity(new StringEntity(PcTestBase.runResultsEntity));
        }

        // --- RUN RESULTS DATA endpoint (ZIP for publishRunReport) ---
        else if (requestUrl.equals(String.format(getBaseURL() + "/%s/%s/%s/%s/data", RUNS_RESOURCE_NAME, PcTestBase.RUN_ID, RESULTS_RESOURCE_NAME, PcTestBase.REPORT_ID))) {
            response = getOkResponse();
            response.setEntity(new FileEntity(
                    new File(getClass().getResource(PcBuilder.pcReportArchiveName).getPath()), ContentType.DEFAULT_BINARY));
        }

        // --- fallback ---
        if (response == null)
            throw new PcException(String.format("%s %s is not recognized by PC Rest Proxy", request.getMethod(), requestUrl));

        return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
    }


    private HttpResponse getOkResponse(){

        return new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK");
    }

    private static Iterator<RunState> initializeRunStateIterator(){

        return Arrays.asList(INITIALIZING,RUNNING,COLLATING_RESULTS,CREATING_ANALYSIS_DATA,FINISHED).iterator();
    }
}
