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

import com.microfocus.application.automation.tools.sse.sdk.Client;
import com.microfocus.application.automation.tools.sse.sdk.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Unify the rest authentication process here from separated part, ALMRestTool and RunManager.
 * Any authentication change will only need to change here.
 * Created by llu2 on 4/5/2017.
 */
public class AuthenticationTool {

    private List<Authenticator> authenticators;
    private static AuthenticationTool instance;

    private AuthenticationTool() {
        authenticators = new ArrayList<>();
        authenticators.add(new RestAuthenticator());
        authenticators.add(new ApiKeyAuthenticator());
    }

    public static synchronized AuthenticationTool getInstance() {
        if (instance == null) {
            instance = new AuthenticationTool();
        }
        return instance;
    }

    /**
     * Try authenticate use a list of authenticators and then create session.
     */
    public boolean authenticate(Client client, String username, String password, String url, String clientType, Logger logger) {
        boolean result = false;
        for(Authenticator authenticator : authenticators) {
            try {
                result = authenticator.login(client, username, password, clientType, logger);
                if (result) {
                    break;
                }
            } catch (Exception e) {
                logger.error(String.format(
                        "Failed login to ALM Server URL: (%s). Exception: %s",
                        url.endsWith("/") ? url : String.format("%s/", url),
                        e.getMessage()));
            }
        }
        return result;
    }
}
