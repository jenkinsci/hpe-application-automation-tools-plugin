/*
 * Copyright (c) 2012 Hewlett-Packard Development Company, L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.hpe.application.automation.tools.sse.sdk;

import com.hpe.application.automation.tools.sse.common.ConsoleLogger;
import com.hpe.application.automation.tools.sse.common.RestClient4Test;
import com.hpe.application.automation.tools.sse.common.TestCase;
import com.hpe.application.automation.tools.sse.sdk.authenticator.RestAuthenticator;
import com.hpe.application.automation.tools.sse.sdk.authenticator.RestAuthenticatorSaas;
import org.junit.Assert;
import org.junit.Test;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestRestAuthenticatorSaas extends TestCase {
    
    @Test
    public void testLoginAlreadyAuthenticated() {
        Client client = new MockRestClientAlreadyAuthenticated(URL, DOMAIN, PROJECT, USER);
        boolean ok = new RestAuthenticatorSaas().login(client, "tester", "blabla", new ConsoleLogger());
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
        public Map getCookies() {
            Map headers = new HashMap<String, String>();
            headers.put(RestAuthenticatorSaas.LWSSO_COOKIE_KEY, "blabla");
            return headers;
        }
        @Override
        public Response httpGet(String url, String queryString, Map<String, String> headers, ResourceAccessLevel resourceAccessLevel) {

            return new Response(null, getExpectAuthInfo(), null, HttpURLConnection.HTTP_OK);
        }
    }
    
    @Test
    public void testLoginNotAuthenticated() {
        Client client = new MockRestClientNotAuthenticated(URL, DOMAIN, PROJECT, USER);
        boolean ok = new RestAuthenticatorSaas().login(client, "tester", "blabla", new ConsoleLogger());
        Assert.assertTrue(ok);
    }
    
    public class MockRestClientNotAuthenticated extends RestClient4Test {
        public MockRestClientNotAuthenticated(
                String url,
                String domain,
                String project,
                String username) {
            super(url, domain, project, username);
        }
        public Map getCookies() {
            Map headers = new HashMap<String, String>();
            return headers;
        }
        @Override
        public Response httpGet(String url, String queryString, Map<String, String> headers, ResourceAccessLevel resourceAccessLevel) {
            return new Response(null, getExpectAuthInfo(), null, HttpURLConnection.HTTP_OK);
        }
    }
    
    @Test
    public void testLoginFailedToLogin() {
        Client client = new MockRestClientFailedToLogin(URL, DOMAIN, PROJECT, USER);
        boolean ok = new RestAuthenticatorSaas().login(client, "tester", "blabla", new ConsoleLogger());
        Assert.assertFalse(ok);
    }
    
    public class MockRestClientFailedToLogin extends RestClient4Test {
        public MockRestClientFailedToLogin(
                String url,
                String domain,
                String project,
                String username) {
            super(url, domain, project, username);
        }
        
        @Override
        public Response httpGet(String url, String queryString, Map<String, String> headers, ResourceAccessLevel resourceAccessLevel) {
            Response ret =
                    new Response(
                            getAuthenticationHeaders(),
                            null,
                            null,
                            HttpURLConnection.HTTP_UNAUTHORIZED);
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
