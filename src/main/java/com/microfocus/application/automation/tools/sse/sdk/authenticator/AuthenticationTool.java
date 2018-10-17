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

import com.microfocus.adm.performancecenter.plugins.common.rest.RESTConstants;
import com.microfocus.application.automation.tools.common.SSEException;
import com.microfocus.application.automation.tools.sse.sdk.Client;
import com.microfocus.application.automation.tools.sse.sdk.Logger;
import com.microfocus.application.automation.tools.sse.sdk.ResourceAccessLevel;
import com.microfocus.application.automation.tools.sse.sdk.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unify the rest authentication process here from separated part, ALMRestTool and RunManager.
 * Any authentication change will only need to change here.
 * Created by llu2 on 4/5/2017.
 */
public final class AuthenticationTool {

    private static List<Authenticator> authenticators;

    private AuthenticationTool() {
        // Add the private constructor to hide the implicit public one.
    }

    static {
        authenticators = new ArrayList<>();
        authenticators.add(new RestAuthenticator());
    }

    /**
     * Try authenticate use a list of authenticators and then create session.
     */
    public static boolean authenticate(Client client, String username, String password, String url, String clientType, Logger logger) {
        if (login(client, username, password, url, logger)) {
            appendQCSessionCookies(client, clientType, logger);
            return true;
        }
        return false;
    }

    private static boolean login(Client client, String username, String password, String url, Logger logger) {
        boolean result = false;
        for(Authenticator authenticator : authenticators) {
            try {
                result = authenticator.login(client, username, password, logger);
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

    private static void appendQCSessionCookies(Client client, String clientType, Logger logger) {
        logger.log("Creating session...");

        Map<String, String> headers = new HashMap<String, String>();
        headers.put(RESTConstants.CONTENT_TYPE, RESTConstants.APP_XML);
        headers.put(RESTConstants.ACCEPT, RESTConstants.APP_XML);

        // issue a post request so that cookies relevant to the QC Session will be added to the RestClient
        Response response =
                client.httpPost(
                        client.build("rest/site-session"),
                        generateClientTypeData(clientType),
                        headers,
                        ResourceAccessLevel.PUBLIC);
        if (!response.isOk()) {
            throw new SSEException("Cannot append QCSession cookies", response.getFailure());
        } else {
            logger.log("Session created.");
        }
    }

    private static byte[] generateClientTypeData(String clientType) {
        String data = String.format("<session-parameters><client-type>%s</client-type></session-parameters>", clientType);
        return data.getBytes();
    }
}
