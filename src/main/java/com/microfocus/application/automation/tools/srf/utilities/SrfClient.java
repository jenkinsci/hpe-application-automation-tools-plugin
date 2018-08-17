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

package com.microfocus.application.automation.tools.srf.utilities;

import com.microfocus.application.automation.tools.srf.model.SrfException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.HttpHost;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.maven.wagon.authorization.AuthorizationException;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.logging.Logger;

public class SrfClient {

    CloseableHttpClient httpclient;
    private String srfServerAddress;
    private SSLSocketFactory sslSocketFactory;
    private HttpHost proxyHost;
    private String accessToken;
    private String workspaceId;
    private String tenantId;
    private static final Logger systemLogger = Logger.getLogger(SrfClient.class.getName());

    public SrfClient(String srfServerAddress, SSLSocketFactory sslSocketFactory, URL proxyUrl) {
        this.srfServerAddress = srfServerAddress;
        httpclient = HttpClients.createDefault();
        this.sslSocketFactory = sslSocketFactory;
        this.proxyHost = proxyUrl != null ? new HttpHost(proxyUrl.getHost(), proxyUrl.getPort()) : null;

        if (proxyHost != null) {
            Properties systemProperties = System.getProperties();
            systemProperties.setProperty("https.proxyHost", proxyHost.getHostName());
            systemProperties.setProperty("http.proxyHost", proxyHost.getHostName());
            systemProperties.setProperty("https.proxyPort", String.valueOf(proxyHost.getPort()));
            systemProperties.setProperty("http.proxyPort", String.valueOf(proxyHost.getPort()));
        }
    }

    public SrfClient(String srfServerAddress, String tenantId, SSLSocketFactory sslSocketFactory, URL proxyUrl) {
        this(srfServerAddress, sslSocketFactory, proxyUrl);
        this.tenantId = tenantId;
    }

    /**
     * SRF login with client's ID and secret, generated from SRF admin page
     * @param clientId
     * @param clientSecret
     * @throws AuthorizationException
     * @throws IOException
     * @throws SrfException
     */
    public void login(String clientId, String clientSecret) throws AuthorizationException, IOException, SrfException {
        systemLogger.info(String.format("Logging with client's id: %s  into %s", clientId, srfServerAddress));
        String authorizationsAddress = srfServerAddress.concat("/rest/security/public/v2/authorizations/access-tokens");

        JSONObject loginBody = new JSONObject();
        loginBody.put("loginName", clientId);
        loginBody.put("password", clientSecret);

        String response = sendPostRequest(new URL(authorizationsAddress), loginBody);
        JSONObject accessKeys = JSONObject.fromObject(response);
        accessToken = accessKeys.getString("accessToken");
        workspaceId = accessKeys.getString("workspaceId");
        // tenantId = accessKeys.getString("tenantId"); wait till 1.61 is out

        if (accessToken == null || accessToken.isEmpty() || workspaceId == null || workspaceId.isEmpty()) {
            throw new SrfException(String.format("Received invalid access keys: access token %s ,workspace id %s", accessToken, workspaceId));
        }
        systemLogger.info(String.format("Successfully logged in to %s", srfServerAddress));

    }

    /**
     * Execute SRF tests via test id or tags
     * @param requestBody
     * @return Jobs array
     * @throws AuthorizationException
     * @throws IOException
     * @throws SrfException
     */
    public JSONArray executeTestsSet(JSONObject requestBody) throws AuthorizationException, IOException, SrfException {
        systemLogger.fine(String.format("executing %s", srfServerAddress));
        String executionAddress = getAuthenticatedSrfApiAddress("/rest/jobmanager/v1/workspaces/%s/execution/jobs");

        String response = sendPostRequest(new URL(executionAddress), requestBody);
        return JSONObject.fromObject(response).getJSONArray("jobs");
    }

    public void cancelJob(String jobId) throws SrfException {
        systemLogger.fine(String.format("Cancelling job id: %s", jobId));
        String jobCancelAddress = getAuthenticatedSrfApiAddress("/rest/jobmanager/v1/workspaces/%s/execution/jobs/{0}", new String[]{jobId});

        try {
            String response = sendRequest(new URL(jobCancelAddress), HttpMethod.DELETE);
            systemLogger.fine(response);
        } catch (SrfException | AuthorizationException | IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Retrieve SRF test runs
     * @param jobIds
     * @return TestRuns array
     * @throws AuthorizationException
     * @throws IOException
     * @throws SrfException
     */
    public JSONArray getTestRuns(JSONArray jobIds) throws AuthorizationException, IOException, SrfException {
        JSONArray testRuns = new JSONArray();
        for (int i = 0; i < jobIds.size(); i++) {
            JSONArray testRun = getTestRun((String) jobIds.get(i));
            testRuns.addAll(testRun);
        }

        return testRuns;
    }

    /**
     * Retrieve SRF test run
     * @param jobId
     * @return Test run
     * @throws IOException
     * @throws AuthorizationException
     * @throws SrfException
     */
    public JSONArray getTestRun(String jobId) throws IOException, AuthorizationException, SrfException {
        String testRunAddress = getAuthenticatedSrfApiAddress("/rest/test-manager/workspaces/%s/test-runs")
                .concat(String.format("&id=%s&include=resource,script-runs,script-steps", jobId));

        String response = sendRequest(new URL(testRunAddress), HttpMethod.GET);
        return JSONArray.fromObject(response);
    }

    public String getAccessToken() {
        return accessToken;
    }

    private String sendRequest(URL url, HttpMethod method) throws IOException, SrfException, AuthorizationException {
        URLConnection connection = url.openConnection();

        if (url.getProtocol().startsWith("https")) {
            ((HttpsURLConnection) connection).setRequestMethod(method.text);
            ((HttpsURLConnection) connection).setSSLSocketFactory(sslSocketFactory);
        } else {
            ((HttpURLConnection) connection).setRequestMethod(method.text);
        }

        // set the connection timeout to 5 and the read timeout to 20 seconds
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(20000);

        int statusCode = ((HttpURLConnection) connection).getResponseCode();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader((connection.getInputStream())));

        StringBuilder response = new StringBuilder();
        String line = "";
        while ((line = bufferedReader.readLine()) != null) {
            response.append(line);
        }

        if (statusCode >= 400) {
            HttpCodeErrorClassifier.throwError(statusCode, response.toString());
        }

        return response.toString();
    }

    private String sendPostRequest(URL url, JSONObject body) throws IOException, SrfException, AuthorizationException  {
        OutputStreamWriter writer = null;
        OutputStream out = null;
        BufferedReader bufferedReader = null;
        URLConnection connection = url.openConnection();
        StringBuilder response;

        try {
            if (url.getProtocol().startsWith("https")) {
                ((HttpsURLConnection) connection).setRequestMethod("POST");
                ((HttpsURLConnection) connection).setSSLSocketFactory(sslSocketFactory);
            } else {
                ((HttpURLConnection) connection).setRequestMethod("POST");
            }

            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            // set the connection timeout to 5 and the read timeout to 20 seconds
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(20000);

            out = connection.getOutputStream();
            writer = new OutputStreamWriter(out);
            writer.write(body.toString());
            writer.flush();
            out.flush();

            int statusCode = ((HttpURLConnection) connection).getResponseCode();

            InputStream inputStream = statusCode >= 400 ? ((HttpURLConnection)connection).getErrorStream() : ((HttpURLConnection)connection).getInputStream();
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            response = new StringBuilder();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                response.append(line);
            }

            if (statusCode >= 400) {
                HttpCodeErrorClassifier.throwError(statusCode, response.toString());
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (out != null) out.close();
            if (writer != null) writer.close();
            if (bufferedReader != null) bufferedReader.close();
        }

        return response.toString();
    }

    private String getAuthenticatedSrfApiAddress(String path) throws SrfException {
        if (tenantId == null || tenantId.isEmpty()) {
           throw new SrfException("Tenant id is null or empty");
        }
        return srfServerAddress
                .concat(String.format(path, workspaceId))
                .concat(String.format("?access-token=%s&TENANTID=%s", accessToken, tenantId));
    }

    private String getAuthenticatedSrfApiAddress(String path, String[] pathParams) throws SrfException {
        String parametizesUrl = MessageFormat.format(path, pathParams);
        return getAuthenticatedSrfApiAddress(parametizesUrl);
    }

    private enum HttpMethod {

        POST("POST"),
        GET("GET"),
        DELETE("DELETE");

        private String text;
        private HttpMethod(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }

    }

}
