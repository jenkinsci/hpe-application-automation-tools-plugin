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
import org.junit.Assert;
import org.junit.Test;

import com.hpe.application.automation.tools.sse.common.RestClient4Test;
import com.hpe.application.automation.tools.sse.sdk.handler.RunHandler;

/**
 * @author Effi Bar-She'an
 */
@SuppressWarnings("squid:S2699")
public class TestTestSetRunHandlerGetReportUrl extends TestCase {

    private static final String _dataFormat = "<SiteVersions>\n" +
            "<SiteVersion>12.50.39 (Patch 1)</SiteVersion>\n" +
            "<MajorVersion>%s</MajorVersion>\n" +
            "<MinorVersion>%s</MinorVersion>\n" +
            "<MinorMinorVersion>0</MinorMinorVersion>\n" +
            "<PatchLevel>1</PatchLevel>\n" +
            "<FormatVersion>1</FormatVersion>\n" +
            "</SiteVersions>";

    @Test
    public void testBuildNewUrl() {

        RunHandler runHandler = new RunHandlerFactory().create(new MockClient(URL, DOMAIN, PROJECT, USER, String.format(_dataFormat, "12", "5")), "TEST_SET", ENTITY_ID);
        runHandler.setRunId(RUN_ID);
        String url = runHandler.getReportUrl(createArgs());
        Assert.assertTrue(String.format("URL does not contains ui. URL: %s", url), url.contains("/ui/"));
        Assert.assertTrue(String.format("URL does not ends with Run ID: %s, URL: %s", RUN_ID, url) , url.endsWith(RUN_ID));
    }

    @Test
    public void testBuildOldUrl() {

        RunHandler runHandler = new RunHandlerFactory().create(new MockClient(URL, DOMAIN, PROJECT, USER, String.format(_dataFormat, "12", "0")), "TEST_SET", ENTITY_ID);
        runHandler.setRunId(RUN_ID);
        String url = runHandler.getReportUrl(createArgs());
        Assert.assertTrue(String.format("URL does not contains webui. URL: %s", url), url.contains("/webui/"));
        Assert.assertTrue(String.format("URL does not ends with Run ID: %s, URL: %s", RUN_ID, url) , url.endsWith(RUN_ID));
    }

    @Test
    public void testBuildHttpCallThrowsException() {

        RunHandler runHandler = new RunHandlerFactory().create(new MockClientThrowsException(URL, DOMAIN, PROJECT, USER), "TEST_SET", ENTITY_ID);
        runHandler.setRunId(RUN_ID);
        Assert.assertEquals("NA", runHandler.getReportUrl(createArgs()));
    }

    private class MockClient extends RestClient4Test {

        private final String _data;

        public MockClient(String url, String domain, String project, String username, String data) {

            super(url, domain, project, username);
            _data = data;
        }

        @Override
        public Response httpGet(
                String url,
                String queryString,
                Map<String, String> headers,
                ResourceAccessLevel resourceAccessLevel) {

            return new Response(null, _data.getBytes(), null, HttpURLConnection.HTTP_OK);
        }
    }

    private class MockClientThrowsException extends RestClient4Test {

        public MockClientThrowsException(String url, String domain, String project, String username) {

            super(url, domain, project, username);
        }

        @Override
        public Response httpGet(
                String url,
                String queryString,
                Map<String, String> headers,
                ResourceAccessLevel resourceAccessLevel) {

            throw new RuntimeException("catch me if you can :)");
        }
    }
}