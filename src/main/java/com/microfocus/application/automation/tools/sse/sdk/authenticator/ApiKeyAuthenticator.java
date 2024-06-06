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
