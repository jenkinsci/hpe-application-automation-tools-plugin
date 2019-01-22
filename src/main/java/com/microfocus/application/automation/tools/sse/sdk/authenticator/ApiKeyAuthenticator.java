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

package com.microfocus.application.automation.tools.sse.sdk.authenticator;

import com.microfocus.adm.performancecenter.plugins.common.rest.RESTConstants;
import com.microfocus.application.automation.tools.sse.sdk.Client;
import com.microfocus.application.automation.tools.sse.sdk.Logger;
import com.microfocus.application.automation.tools.sse.sdk.ResourceAccessLevel;
import com.microfocus.application.automation.tools.sse.sdk.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * For ALM1260sso and 15sso, there's a different API to authenticated with api key.
 */
public class ApiKeyAuthenticator implements Authenticator {

    private static final String APIKEY_LOGIN_API = "rest/oauth2/login";
    private static final String CLIENT_TYPE = "ALM-CLIENT-TYPE";

    @Override
    public boolean login(Client client, String clientId, String secret, String clientType, Logger logger) {
        logger.log("Start login to ALM server with APIkey...");
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(CLIENT_TYPE, clientType);
        headers.put(RESTConstants.ACCEPT, "application/json");
        headers.put(RESTConstants.CONTENT_TYPE, "application/json");

        Response response =
                client.httpPost(
                        client.build(APIKEY_LOGIN_API),
                        String.format("{clientId:%s, secret:%s}", clientId, secret).getBytes(),
                        headers,
                        ResourceAccessLevel.PUBLIC);
        boolean result = response.isOk();
        logger.log(
                result ? String.format(
                                "Logged in successfully to ALM Server %s using %s",
                                client.getServerUrl(),
                                clientId)
                        : String.format(
                                "Login to ALM Server at %s failed. Status Code: %s",
                                client.getServerUrl(),
                                response.getStatusCode()));
        return result;
    }

    @Override
    public boolean logout(Client client, String username) {
        // No logout
        return true;
    }
}
