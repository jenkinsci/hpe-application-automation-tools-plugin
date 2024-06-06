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
import org.junit.Assert;
import org.junit.Test;

import com.microfocus.application.automation.tools.sse.common.RestClient4Test;
import com.microfocus.application.automation.tools.sse.sdk.handler.RunHandler;

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