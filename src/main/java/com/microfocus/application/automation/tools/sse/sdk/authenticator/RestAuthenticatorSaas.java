/*
 * © Copyright 2013 EntIT Software LLC
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright (c) 2018 Micro Focus Company, L.P.
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
 * ___________________________________________________________________
 *
 */

package com.microfocus.application.automation.tools.sse.sdk.authenticator;

import com.microfocus.adm.performancecenter.plugins.common.rest.RESTConstants;
import com.microfocus.application.automation.tools.sse.sdk.Base64Encoder;
import com.microfocus.application.automation.tools.sse.sdk.Client;
import com.microfocus.application.automation.tools.sse.sdk.Logger;
import com.microfocus.application.automation.tools.sse.sdk.ResourceAccessLevel;
import com.microfocus.application.automation.tools.sse.sdk.Response;

import java.util.HashMap;
import java.util.Map;

/***
 * Rest Authenticator via SaaS
 * Created by Roy Lu on 2/10/2017.
 */

public class RestAuthenticatorSaas implements Authenticator {
    public static final String AUTHENTICATION_POINT = "authentication-point/authenticate";
    public static final String AUTHENTICATION_LOGOUT = "authentication-point/logout";
    public static final String LWSSO_COOKIE_KEY = "LWSSO_COOKIE_KEY";
    
    public boolean login(Client client, String username, String password, Logger logger) {
        logger.log("Start login to ALM server through SaaS.");
        boolean ret = true;
        if (!isAuthenticated(client)) {
            Response response = login(client, AUTHENTICATION_POINT, username, password);
            if (response.isOk()) {
                logLoggedInSuccessfully(username, client.getServerUrl(), logger);
            } else {
                logger.log(String.format(
                        "Login to ALM Server at %s failed. Status Code: %s",
                        client.getServerUrl(),
                        response.getStatusCode()));
                ret = false;
            }
        } else {
            logger.log("Already authenticated.");
        }
        return ret;
    }

    /**
     * Actually login
     * @param client
     * @param loginUrl
     * @param username
     * @param password
     * @return
     */
    private Response login(Client client, String loginUrl, String username, String password) {
        // create a string that looks like:
        // "Basic ((username:password)<as bytes>)<64encoded>"
        byte[] credBytes = (username + ":" + password).getBytes();
        String credEncodedString = "Basic " + Base64Encoder.encode(credBytes);
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(RESTConstants.AUTHORIZATION, credEncodedString);
        return client.httpGet(client.build(loginUrl), null, headers, ResourceAccessLevel.PUBLIC);
    }
    
    /**
     * @return true if logout successful
     * @throws Exception
     *             close session on server and clean session cookies on client
     */
    public boolean logout(Client client, String username) {
        // note the get operation logs us out by setting authentication cookies to:
        // LWSSO_COOKIE_KEY="" via server response header Set-Cookie
        Response response =
                client.httpGet(client.build(AUTHENTICATION_LOGOUT),
                        null,
                        null,
                        ResourceAccessLevel.PUBLIC);
        return response.isOk();
    }

    /**
     * Check the client is already authenticated.
     */
    private boolean isAuthenticated(Client client) {
        return client.getCookies().keySet().contains(LWSSO_COOKIE_KEY);
    }

    private void logLoggedInSuccessfully(String username, String loginServerUrl, Logger logger) {
        logger.log(String.format(
            "Logged in successfully to ALM Server %s using %s",
            loginServerUrl,
            username));
    }
}
