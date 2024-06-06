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

package com.microfocus.application.automation.tools.sse.sdk;

import java.net.HttpURLConnection;
import java.util.Map;

import com.microfocus.application.automation.tools.sse.common.TestCase;
import com.microfocus.application.automation.tools.sse.sdk.handler.RunHandlerFactory;
import com.microfocus.application.automation.tools.sse.sdk.handler.TestSetRunHandler;
import org.junit.Assert;
import org.junit.Test;

import com.microfocus.application.automation.tools.sse.common.RestClient4Test;

/**
 * @author Effi Bar-She'an
 */
@SuppressWarnings("squid:S2699")
public class TestTestSetRunHandler extends TestCase {

    @Test
    public void testStart() {

        Client client = new MockRestStartClient(URL, DOMAIN, PROJECT, USER);
        Response response =
                new RunHandlerFactory().create(client, "TEST_SET", ENTITY_ID).start(
                        DURATION,
                        POST_RUN_ACTION,
                        ENVIRONMENT_CONFIGURATION_ID,
                        null);
        Assert.assertTrue(response.isOk());
    }

    private class MockRestStartClient extends RestClient4Test {

        public MockRestStartClient(String url, String domain, String project, String username) {

            super(url, domain, project, username);
        }

        @Override
        public Response httpPost(String url, byte[] data, Map<String, String> headers, ResourceAccessLevel resourceAccessLevel) {

            return new Response(null, null, null, HttpURLConnection.HTTP_OK);
        }
    }

    @Test
    public void testStop() {

        Client client = new MockRestStopClient(URL, DOMAIN, PROJECT, USER);
        Response response = new TestSetRunHandler(client, "23").stop();
        Assert.assertTrue(response.isOk());
    }

    private class MockRestStopClient extends RestClient4Test {

        public MockRestStopClient(String url, String domain, String project, String username) {

            super(url, domain, project, username);
        }

        @Override
        public Response httpPost(String url, byte[] data, Map<String, String> headers, ResourceAccessLevel resourceAccessLevel) {

            return new Response(null, null, null, HttpURLConnection.HTTP_OK);
        }
    }
}