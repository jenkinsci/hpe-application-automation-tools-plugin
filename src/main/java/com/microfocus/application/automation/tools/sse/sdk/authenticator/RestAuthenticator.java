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

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.microfocus.application.automation.tools.common.SSEException;
import com.microfocus.adm.performancecenter.plugins.common.rest.RESTConstants;
import com.microfocus.application.automation.tools.sse.sdk.Base64Encoder;
import com.microfocus.application.automation.tools.sse.sdk.Client;
import com.microfocus.application.automation.tools.sse.sdk.Logger;
import com.microfocus.application.automation.tools.sse.sdk.ResourceAccessLevel;
import com.microfocus.application.automation.tools.sse.sdk.Response;
import sun.rmi.runtime.Log;

/**
 * @author Effi Bar-She'an
 * @author Dani Schreiber
 */

public class RestAuthenticator implements Authenticator {

    public static final String IS_AUTHENTICATED = "rest/is-authenticated";
    public static final String AUTHENTICATE_HEADER = "WWW-Authenticate";
    public static final String AUTHENTICATION_INFO = "AuthenticationInfo";
    public static final String USER_NAME = "Username";
    public static final String QCAPP_NAME = "/qcbin";
    public static final String AUTHENTICATE_POINT = "authentication-point/authenticate";

    private String authenticationPoint;
    
    public boolean login(Client client, String username, String password, String clientType, Logger logger) {
        logger.log("Start login to ALM server...");
        if (isAuthenticated(client, logger)) {
            return true;
        }

        if (authenticationPoint != null) {
            logger.log("Got authenticate point:" + authenticationPoint);
            if (!isAuthenticatePointRight(authenticationPoint, client.getServerUrl(), logger)) {
                authenticationPoint = null;
            }
        }

        // Some customer always got wrong authenticate point because of an issue of ALM.
        // But they still can login with a right authenticate point.
        // So try to login with that anyway.
        if (authenticationPoint == null) {
            authenticationPoint = client.getServerUrl().endsWith("/") ?
                    client.getServerUrl() + AUTHENTICATE_POINT :
                    client.getServerUrl() + "/" + AUTHENTICATE_POINT;
            logger.log("Failed to get authenticate point from server. Try to login with: " + authenticationPoint);
        }

        boolean ret = authenticate(client, authenticationPoint, username, password, logger);
        if (ret) {
            ret = appendQCSessionCookies(client, clientType, logger);
        }
        return ret;
    }

    /**
     * Some ALM server generates wrong authenticate point and port.
     * @param authenticatePoint
     * @param serverUrl
     * @param logger
     * @return
     */
    private boolean isAuthenticatePointRight(String authenticatePoint, String serverUrl, Logger logger) {
        boolean result = false;
        // Check schema
        if (!serverUrl.substring(0,5).equalsIgnoreCase(authenticatePoint.substring(0,5))) {
            logger.log("Authenticate point schema is different with server schema. Please check with ALM site admin.");
        }
        else {
            // Check port
            String serverPort = serverUrl.substring(
                    serverUrl.indexOf(QCAPP_NAME) - 4,
                    serverUrl.indexOf(QCAPP_NAME));

            String authPointPort = authenticatePoint.substring(
                    authenticatePoint.indexOf(QCAPP_NAME) - 4,
                    authenticatePoint.indexOf(QCAPP_NAME));

            if (!serverPort.equalsIgnoreCase(authPointPort)) {
                logger.log("Authenticate point port is different with server port. Please check with ALM site admin.");
            }
            else {
                result = true;
            }
        }
        return result;
    }
    
    /**
     * @param loginUrl
     *            to authenticate at
     * @return true on operation success, false otherwise Basic authentication (must store returned
     *         cookies for further use)
     */
    private boolean authenticate(Client client, String loginUrl, String username, String password, Logger logger) {
        // create a string that looks like:
        // "Basic ((username:password)<as bytes>)<64encoded>"
        byte[] credBytes = (username + ":" + password).getBytes();
        String credEncodedString = "Basic " + Base64Encoder.encode(credBytes);
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(RESTConstants.AUTHORIZATION, credEncodedString);
        Response response = client.httpGet(loginUrl, null, headers, ResourceAccessLevel.PUBLIC);

        boolean ret = response.isOk();
        if (ret) {
            logger.log(String.format(
                    "Logged in successfully to ALM Server %s using %s",
                    client.getServerUrl(),
                    username));
        } else {
            logger.log(String.format(
                    "Login to ALM Server at %s failed. Status Code: %s",
                    client.getServerUrl(),
                    response.getStatusCode()));
        }
        return ret;
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
                client.httpGet(
                        client.build("authentication-point/logout"),
                        null,
                        null,
                        ResourceAccessLevel.PUBLIC);
        return response.isOk();
    }

    /**
     * Verify is the client is already authenticated. If not, try get the authenticate point.
     * @param client client
     * @param logger logger
     * @return null or authenticate point
     */
    private boolean isAuthenticated(Client client, Logger logger) {
        boolean result = false;
        Response response =
                client.httpGet(
                        client.build(IS_AUTHENTICATED),
                        null,
                        null,
                        ResourceAccessLevel.PUBLIC);
        
        if (checkAuthResponse(response, client.getUsername(), logger)) {
            logger.log(String.format(
                    "Already logged in to ALM Server %s using %s",
                    client.getServerUrl(),
                    client.getUsername()));
            result = true;
        }
        else {
            // Try to get authenticate point regardless the response status.
            authenticationPoint = getAuthenticatePoint(response);
            if (authenticationPoint == null) {
                // If can't get authenticate point, then output the message.
                logger.log("Can't get authenticate header.");
                if(response.getStatusCode() != HttpURLConnection.HTTP_OK) {
                    logger.log(String.format("Failed to get authenticate header. Exception: %s", response.getFailure()));
                }
            } else {
                authenticationPoint = authenticationPoint.replace("\"", "");
                authenticationPoint += "/authenticate";
            }
        }
        return result;
    }
    
    private boolean checkAuthResponse(Response response, String authUser, Logger logger) {
        boolean ret = false;
        if (response.getStatusCode() == HttpURLConnection.HTTP_OK){
            if (response.getData() != null
                    && new String(response.getData()).contains(AUTHENTICATION_INFO)
                    && new String(response.getData()).contains(USER_NAME)
                    && new String(response.getData()).contains(authUser)){
                ret = true;
            }
            else {
                logger.log("Failed to check authenticate response header.");
            }
        }
        else if (response.getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            logger.log(String.format("User %s unauthorized.", authUser));
        }
        else {
            logger.log(String.format("Failed to check authenticate status. Exception: %s", response.getFailure()));
        }
        return ret;
    }

    /**
     * Try get authenticate point from response.
     * @param response response
     * @return null or authenticate point
     */
    private String getAuthenticatePoint(Response response) {
        Map<String, List<String>> headers = response.getHeaders();
        if (headers == null || headers.size() == 0) {
            return null;
        }
        if (headers.get(AUTHENTICATE_HEADER) == null || headers.get(AUTHENTICATE_HEADER).isEmpty()) {
            return null;
        }
        String authenticateHeader = headers.get(AUTHENTICATE_HEADER).get(0);
        String[] authenticateHeaderArray = authenticateHeader.split("=");
        if (authenticateHeaderArray.length == 1) {
            return null;
        }
        return authenticateHeaderArray[1];
    }

    private boolean appendQCSessionCookies(Client client, String clientType, Logger logger) {
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
        boolean ret = response.isOk();
        if (!ret) {
            logger.log(String.format("Cannot append QCSession cookies. Exception: %s", response.getFailure()));
        } else {
            logger.log("Session created.");
        }
        return ret;
    }

    private byte[] generateClientTypeData(String clientType) {
        String data = String.format("<session-parameters><client-type>%s</client-type></session-parameters>", clientType);
        return data.getBytes();
    }
}
