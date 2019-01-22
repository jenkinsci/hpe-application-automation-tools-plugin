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
                logger.log(String.format(
                        "Failed login to ALM Server URL: %s. Exception: %s",
                        url.endsWith("/") ? url : String.format("%s/", url),
                        e.getMessage()));
            }
        }
        return result;
    }
}
