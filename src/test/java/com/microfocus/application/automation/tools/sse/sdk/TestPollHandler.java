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
import org.junit.Assert;
import org.junit.Test;

import com.microfocus.application.automation.tools.sse.common.ConsoleLogger;
import com.microfocus.application.automation.tools.sse.common.RestClient4Test;
import com.microfocus.application.automation.tools.sse.sdk.handler.PollHandler;
import com.microfocus.application.automation.tools.sse.sdk.handler.PollHandlerFactory;

@SuppressWarnings("squid:S2698")
public class TestPollHandler extends TestCase {
    
    @Test
    public void testBVSPoll() throws InterruptedException {
        
        Client client = new MockRestClientBVS(URL, DOMAIN, PROJECT, USER);
        boolean isOk;
        PollHandler pollHandler = new PollHandlerFactory().create(client, "BVS", "12", 0);
        isOk = pollHandler.poll(new ConsoleLogger());
        Assert.assertTrue(isOk);
    }
    
    private class MockRestClientBVS extends RestClient4Test {
        
        public MockRestClientBVS(String url, String domain, String project, String username) {
            
            super(url, domain, project, username);
        }
        
        @Override
        public Response httpGet(String url, String queryString, Map<String, String> headers, ResourceAccessLevel resourceAccessLevel) {
            
            Response ret = null;
            if (url.contains("procedure-runs/")) {
                ret = new Response(null, RUN_ENTITY_DATA_FORMAT, null, HttpURLConnection.HTTP_OK);
            } else if (url.contains("reservations/")) {
                ret = new Response(null, FINISHED_DATA, null, HttpURLConnection.HTTP_OK);
            } else if (url.contains("event-log-reads")) {
                ret = new Response(null, EVENT_LOG_DATA, null, HttpURLConnection.HTTP_OK);
            } else {
                Assert.fail();
            }
            
            return ret;
        }
    }
    
    @Test
    public void testPCPoll() throws InterruptedException {
        
        Client client = new MockRestClientPC(URL, DOMAIN, PROJECT, USER);
        boolean isOk;
        PollHandler pollHandler = new PollHandlerFactory().create(client, "PC", "12", 0);
        isOk = pollHandler.poll(new ConsoleLogger());
        Assert.assertTrue(isOk);
    }
    
    private class MockRestClientPC extends RestClient4Test {
        
        public MockRestClientPC(String url, String domain, String project, String username) {
            
            super(url, domain, project, username);
        }
        
        @Override
        public Response httpGet(String url, String queryString, Map<String, String> headers, ResourceAccessLevel resourceAccessLevel) {
            
            Response ret = null;
            if (url.contains("runs/")) {
                ret =
                        new Response(
                                null,
                                PC_RUN_ENTITY_DATA_FORMAT,
                                null,
                                HttpURLConnection.HTTP_OK);
            } else {
                Assert.fail();
            }
            
            return ret;
        }
    }
    
    @Test
    public void testPollBVSThrowsException() throws InterruptedException {
        
        Client client = new MockRestClientThrowsException(URL, DOMAIN, PROJECT, USER);
        boolean isOk;
        isOk = new PollHandlerFactory().create(client, "BVS", "12", 0).poll(new ConsoleLogger());
        Assert.assertFalse(isOk);
    }
    
    @Test
    public void testPollPCThrowsException() throws InterruptedException {
        
        Client client = new MockRestClientThrowsException(URL, DOMAIN, PROJECT, USER);
        boolean isOk;
        isOk = new PollHandlerFactory().create(client, "PC", "12", 0).poll(new ConsoleLogger());
        Assert.assertFalse(isOk);
    }
    
    private class MockRestClientThrowsException extends RestClient4Test {
        
        public MockRestClientThrowsException(
                String url,
                String domain,
                String project,
                String username) {
            
            super(url, domain, project, username);
        }
        
        @Override
        public Response httpGet(String url, String queryString, Map<String, String> headers, ResourceAccessLevel resourceAccessLevel) {
            
            throw new RuntimeException("MockRestClientThrowsException");
        }
    }
    
    @Test
    public void testPollBvsTwoPasses() throws InterruptedException {
        
        Client client = new MockRestClientBvsTwoPasses(URL, DOMAIN, PROJECT, USER);
        boolean isOk;
        isOk = new PollHandlerFactory().create(client, "BVS", "12", 0).poll(new ConsoleLogger());
        Assert.assertTrue(isOk);
    }
    
    private class MockRestClientBvsTwoPasses extends RestClient4Test {
        
        private int _calls = 0;
        
        public MockRestClientBvsTwoPasses(String url, String domain, String project, String username) {
            
            super(url, domain, project, username);
        }
        
        @Override
        public Response httpGet(String url, String queryString, Map<String, String> headers, ResourceAccessLevel resourceAccessLevel) {
            
            byte[] data = null;
            String expectedUrl =
                    String.format(
                            "%s/rest/domains/%s/projects/%s/reservations/%s",
                            URL,
                            DOMAIN,
                            PROJECT,
                            TIMESLOT_ID);
            if (url.contains("procedure-runs/")) {
                data = RUN_ENTITY_DATA_FORMAT;
            } else if (url.contains("reservations/")) {
                if (++_calls == 1) {
                    Assert.assertEquals(expectedUrl, url);
                    data = RUNNING_DATA;
                } else if (_calls == 2) {
                    data = FINISHED_DATA;
                }
            } else if (url.contains("event-log-reads")) {
                Assert.assertTrue(url.contains("event-log-reads"));
                data = EVENT_LOG_DATA;
            }
            Assert.assertTrue(_calls < 3);
            
            return new Response(null, data, null, HttpURLConnection.HTTP_OK);
        }
    }
    
    @Test
    public void testPollPCTwoPasses() throws InterruptedException {
        
        Client client = new MockRestClientPCTwoPasses(URL, DOMAIN, PROJECT, USER);
        boolean isOk;
        isOk = new PollHandlerFactory().create(client, "PC", "12", 0).poll(new ConsoleLogger());
        Assert.assertTrue(isOk);
    }
    
    private class MockRestClientPCTwoPasses extends RestClient4Test {
        
        private int _calls = 0;
        
        public MockRestClientPCTwoPasses(String url, String domain, String project, String username) {
            
            super(url, domain, project, username);
        }
        
        @Override
        public Response httpGet(String url, String queryString, Map<String, String> headers, ResourceAccessLevel resourceAccessLevel) {
            
            byte[] data = null;
            
            String expectedUrl =
                    String.format(
                            "%s/rest/domains/%s/projects/%s/runs/%s",
                            URL,
                            DOMAIN,
                            PROJECT,
                            "");
            if (url.contains("runs/")) {
                data = PC_RUN_ENTITY_DATA_FORMAT;
                if (++_calls == 1) {
                    Assert.assertEquals(expectedUrl, url);
                    data = PC_RUNNING_DATA;
                } else if (_calls < 4) {
                    data = PC_FINISHED_DATA;
                }
                Assert.assertTrue(_calls < 4);
                
            } else {
                Assert.fail();
            }
            Assert.assertTrue(_calls < 4);
            
            return new Response(null, data, null, HttpURLConnection.HTTP_OK);
        }
    }
    
}
