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

import java.net.HttpURLConnection;
import java.util.Map;

import com.hpe.application.automation.tools.sse.common.TestCase;
import com.hpe.application.automation.tools.sse.sdk.handler.RunHandlerFactory;
import com.hpe.application.automation.tools.sse.sdk.handler.TestSetRunHandler;
import org.junit.Assert;
import org.junit.Test;

import com.hpe.application.automation.tools.sse.common.RestClient4Test;

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