package com.ge.application.automation.steps;


import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import qc.rest.examples.infrastructure.Response;
import qc.rest.examples.infrastructure.RestConnector;
import sun.misc.BASE64Encoder;
/**
* This example shows how to login, logout, and authenticate to the server using REST. Note that this is a
 * rather "thin" layer over {@link RestConnector} because these operations are *almost* HTML
 * standards.
 * 
 * Slightly altered from sample code provided in ALM rest api documentation
 */
public class RestAuthenticationHandler {
	
    private RestConnector con;
    private PrintStream logger;
	
    public RestAuthenticationHandler(PrintStream logger) {
    	this.logger = logger;
        con = RestConnector.getInstance();
    }
    
    public RestConnector connect(String username, String password,
    		String host, /*String port,*/
    		String domain, String project) {
    	RestConnector con =
                RestConnector.getInstance().init(
                        new HashMap<String, String>(),
                        host,
                        domain,
                        project);
        RestAuthenticationHandler example = new RestAuthenticationHandler(logger);
        //Returns null if authenticated. If not authenticated, returns
        //a URL indicating where to login.
        //We are not logged in, so call returns a URL
        String authenticationPoint = null;
		try {
			authenticationPoint = example.isAuthenticated();
		} catch (Exception e) {
			e.printStackTrace();
		}
        //Now we login to previously returned URL.
        try {
			example.login(authenticationPoint, username, password);
		} catch (Exception e) {
			e.printStackTrace();
		}
        return con;
    }
    
    /**
     * @param username
     * @param password
     * @return true if authenticated at the end of this method.
     * @throws Exception
     *
     * convenience method used by other examples to do their login
     */
    public boolean login(String username, String password) throws Exception {
        String authenticationPoint = this.isAuthenticated();
        if (authenticationPoint != null) {
            return this.login(authenticationPoint, username, password);
        }
        return true;
    }
    /**
     * @param loginUrl to authenticate at
     * @param username
     * @param password
     * @return true on operation success, false otherwise
     * @throws Exception
     *
     * logging in to our system is standard http login (basic authentication), where one must store
     * the returned cookies for further use.
     */
    public boolean login(String loginUrl, String username, String password) throws Exception {
        //Create a string that looks like "Basic ((username:password)<as bytes>)<64encoded>"
        byte[] credBytes = (username + ":" + password).getBytes();
        String credEncodedString = "Basic " + new BASE64Encoder().encode(credBytes);
        Map<String, String> map = new HashMap<String, String>();
        map.put("Authorization", credEncodedString);
        Response response = con.httpGet(loginUrl, null, map);
        boolean ret = response.getStatusCode() == HttpURLConnection.HTTP_OK;
        return ret;
    }
    /**
     * @return true if logout successful
     * @throws Exception
     *             close session on server and clean session cookies on client
     */
    public boolean logout() throws Exception {
        //New that the get operation logs us out by setting authentication cookies to: LWSSO_COOKIE_KEY="" using server response header Set-Cookie
        Response response =
                con.httpGet(con.buildUrl("authentication-point/logout"), null, null);
        return (response.getStatusCode() == HttpURLConnection.HTTP_OK);
    }
    /**
     * @return null if authenticated.<br>
     *         a url to authenticate against if not authenticated.
     * @throws Exception
     */
    public String isAuthenticated() throws Exception {
        String isAuthenticateUrl = con.buildUrl("rest/is-authenticated");
        String ret;
        Response response = con.httpGet(isAuthenticateUrl, null, null);
        int responseCode = response.getStatusCode();
        //If already authenticated
        if (responseCode == HttpURLConnection.HTTP_OK) {
            ret = null;
        }
        //If not authenticated - get the address where to authenticate via WWW-Authenticate
        else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            Iterable<String> authenticationHeader =
                    response.getResponseHeaders().get("WWW-Authenticate");
            String newUrl = authenticationHeader.iterator().next().split("=")[1];
            newUrl = newUrl.replace("\"", "");
            newUrl += "/authenticate";
            ret = newUrl;
        }
        //Not OK and not unauthorized - means some kind of error, like 404, or 500
        else {
            throw response.getFailure();
        }
        return ret;
    }
}
