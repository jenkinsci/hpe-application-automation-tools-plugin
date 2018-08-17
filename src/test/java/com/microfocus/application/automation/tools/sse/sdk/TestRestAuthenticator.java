/*
 *
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * © Copyright 2012-2018 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors (“Micro Focus”) are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 *
 */

package com.microfocus.application.automation.tools.sse.sdk;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.microfocus.application.automation.tools.sse.sdk.authenticator.RestAuthenticator;
import com.microfocus.application.automation.tools.sse.common.TestCase;
import org.junit.Assert;
import org.junit.Test;

import com.microfocus.application.automation.tools.sse.common.ConsoleLogger;
import com.microfocus.application.automation.tools.sse.common.RestClient4Test;

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
