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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hpe.application.automation.tools.sse.sdk.authenticator.RestAuthenticator;
import com.hpe.application.automation.tools.sse.common.TestCase;
import org.junit.Assert;
import org.junit.Test;

import com.hpe.application.automation.tools.sse.common.ConsoleLogger;
import com.hpe.application.automation.tools.sse.common.RestClient4Test;

@SuppressWarnings("squid:S2698")
public class TestRestAuthenticator extends TestCase {
    
    @Test
    public void testLoginAlreadyAuthenticated() {
        
        Client client = new MockRestClientAlreadyAuthenticated(URL, DOMAIN, PROJECT, USER);
        boolean ok = new RestAuthenticator().login(client, "tester", "blabla", new ConsoleLogger());
        Assert.assertTrue(ok);
    }
    
    public class MockRestClientAlreadyAuthenticated extends RestClient4Test {
        
        public MockRestClientAlreadyAuthenticated(
                String url,
                String domain,
                String project,
                String username) {
            
            super(url, domain, project, username);
        }
        
        @Override
        public Response httpGet(String url, String queryString, Map<String, String> headers, ResourceAccessLevel resourceAccessLevel) {
            
            return new Response(null, getExpectAuthInfo(), null, HttpURLConnection.HTTP_OK);
        }
    }
    
    @Test
    public void testLoginNotAuthenticated() {
        
        Client client = new MockRestClientNotAuthenticated(URL, DOMAIN, PROJECT, USER);
        boolean ok = new RestAuthenticator().login(client, "tester", "blabla", new ConsoleLogger());
        Assert.assertTrue(ok);
    }
    
    public class MockRestClientNotAuthenticated extends RestClient4Test {
        
        private int _time = 0;
        private final String _isAuthenticatedUrl;
        private final String _authenticationUrl;
        
        private MockRestClientNotAuthenticated(
                String url,
                String domain,
                String project,
                String username) {
            
            super(url, domain, project, username);
            _isAuthenticatedUrl = build(RestAuthenticator.IS_AUTHENTICATED);
            _authenticationUrl = build("authentication-point/authenticate");
        }
        
        @Override
        public Response httpGet(String url, String queryString, Map<String, String> headers, ResourceAccessLevel resourceAccessLevel) {
            
            Response ret = null;
            if (++_time == 1) {
                Assert.assertEquals(_isAuthenticatedUrl, url);
                ret =
                        new Response(
                                getAuthenticationHeaders(),
                                null,
                                null,
                                HttpURLConnection.HTTP_UNAUTHORIZED);
            } else if (_time == 2) {
                Assert.assertEquals(_authenticationUrl, url);
                ret = new Response(null, null, null, HttpURLConnection.HTTP_OK);
            }
            Assert.assertTrue("More than 2 calls to httpGet", _time < 3);
            
            return ret;
        }
        
        private Map<String, List<String>> getAuthenticationHeaders() {
            
            Map<String, List<String>> ret = new HashMap<String, List<String>>(1);
            List<String> values = new ArrayList<String>();
            values.add(String.format("LWSSO realm=\"%s\"", build("authentication-point")));
            ret.put(RestAuthenticator.AUTHENTICATE_HEADER, values);
            
            return ret;
        }
    }
    
    @Test
    public void testLoginFailedToLogin() {
        
        Client client = new MockRestClientFailedToLogin(URL, DOMAIN, PROJECT, USER);
        boolean ok = new RestAuthenticator().login(client, "tester", "blabla", new ConsoleLogger());
        Assert.assertFalse(ok);
    }
    
    public class MockRestClientFailedToLogin extends RestClient4Test {
        
        private int _time = 0;
        private final String _isAuthenticatedUrl;
        private final String _authenticationUrl;
        
        private MockRestClientFailedToLogin(
                String url,
                String domain,
                String project,
                String username) {
            
            super(url, domain, project, username);
            _isAuthenticatedUrl = build(RestAuthenticator.IS_AUTHENTICATED);
            _authenticationUrl = build("authentication-point/authenticate");
        }
        
        @Override
        public Response httpGet(String url, String queryString, Map<String, String> headers, ResourceAccessLevel resourceAccessLevel) {
            
            Response ret = null;
            if (++_time == 1) {
                Assert.assertEquals(_isAuthenticatedUrl, url);
                ret =
                        new Response(
                                getAuthenticationHeaders(),
                                null,
                                null,
                                HttpURLConnection.HTTP_UNAUTHORIZED);
            } else if (_time == 2) {
                Assert.assertEquals(_authenticationUrl, url);
                ret = new Response(null, null, null, HttpURLConnection.HTTP_UNAUTHORIZED);
            }
            Assert.assertTrue("More than 2 calls to httpGet", _time < 3);
            
            return ret;
        }
        
        private Map<String, List<String>> getAuthenticationHeaders() {
            
            Map<String, List<String>> ret = new HashMap<String, List<String>>(1);
            List<String> values = new ArrayList<String>();
            values.add(String.format("LWSSO realm=\"%s\"", build("authentication-point")));
            ret.put(RestAuthenticator.AUTHENTICATE_HEADER, values);
            
            return ret;
        }
    }
}
