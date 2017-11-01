/*
 * MIT License
 *
 * Copyright (c) 2016 Hewlett-Packard Development Company, L.P.
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
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

/*
* Implements the REST API methods for executing the loadtest
* */
package com.hpe.application.automation.tools.pc;

import com.hpe.application.automation.tools.common.PcException;
import com.hpe.application.automation.tools.model.TimeslotDuration;
import com.hpe.application.automation.tools.rest.RESTConstants;
import com.hpe.application.automation.tools.sse.sdk.Base64Encoder;
import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.conn.SchemeRegistryFactory;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.*;

import java.io.PrintStream;


import static org.apache.commons.httpclient.HttpStatus.*;

public class PcRestProxy {

    protected static final String        BASE_PC_API_URL                = "%s://%s/LoadTest/rest";
    protected static final String        BASE_PC_API_AUTHENTICATION_URL = BASE_PC_API_URL + "/authentication-point";
    protected static final String        AUTHENTICATION_LOGIN_URL       = BASE_PC_API_AUTHENTICATION_URL + "/authenticate";
    protected static final String        AUTHENTICATION_LOGOUT_URL      = BASE_PC_API_AUTHENTICATION_URL + "/logout";
    protected static final String        PC_API_RESOURCES_TEMPLATE      = BASE_PC_API_URL + "/domains/%s/projects/%s";
    protected static final String        RUNS_RESOURCE_NAME             = "Runs";
    protected static final String        TESTS_RESOURCE_NAME            = "tests";
    protected static final String        TEST_INSTANCES_NAME            = "testinstances";
    protected static final String        TEST_SETS_NAME                 = "testsets";
    protected static final String        RESULTS_RESOURCE_NAME          = "Results";
    protected static final String        EVENTLOG_RESOURCE_NAME         = "EventLog";
    protected static final String        TREND_REPORT_RESOURCE_NAME     = "TrendReports";
    protected static final String        TREND_REPORT_RESOURCE_SUFFIX     = "data";
    protected static final String        CONTENT_TYPE_XML               = "application/xml";
    public static final String                  PC_API_XMLNS                   = "http://www.hp.com/PC/REST/API";
	
    protected static final List<Integer> validStatusCodes = Arrays.asList(SC_OK, SC_CREATED, SC_ACCEPTED, SC_NO_CONTENT);
	
	private String baseURL;
    private String pcServer;
	private String domain;
	private String project;
	private String webProtocol;
	private String proxyScheme;
	private String proxyHostName;
	private int proxyPort;
    private String proxyUser;
    private String proxyPassword;

	private DefaultHttpClient client;
    private HttpContext context;
    private CookieStore cookieStore;
  //  private PrintStream logger;

    public PcRestProxy(String webProtocolName, String pcServerName, String almDomain, String almProject,PrintStream mainLogger, String proxyOutURL, String proxyUser, String proxyPassword) throws PcException {

//        logger = mainLogger;
    	pcServer = pcServerName;
    	domain = almDomain;
    	project = almProject;
    	webProtocol = webProtocolName;
    	baseURL = String.format(PC_API_RESOURCES_TEMPLATE, webProtocol,pcServer, domain, project);

    	PoolingClientConnectionManager cxMgr = new PoolingClientConnectionManager(SchemeRegistryFactory.createDefault());
    	cxMgr.setMaxTotal(100);
    	cxMgr.setDefaultMaxPerRoute(20);

    	
    	client = new DefaultHttpClient(cxMgr);
        if (proxyOutURL != null && !proxyOutURL.isEmpty()) {
            // Setting proxy
            // we should get the full proxy URL from the user: http(s)://<server>:<port>
            // PAC (proxy auto-config) or Automatic configuration script is not supported (for example our proxy: http://autocache.hpecorp.net/)
            getProxyDataFromURL(proxyOutURL);
            this.proxyUser = proxyUser;
            this.proxyPassword = proxyPassword;
            HttpHost proxy = new HttpHost(proxyHostName, proxyPort, proxyScheme);

            if (proxyUser != null && !proxyUser.isEmpty()) {
                Credentials credentials = new UsernamePasswordCredentials(proxyUser, proxyPassword);
                AuthScope authScope = new AuthScope(proxyHostName, proxyPort);
                CredentialsProvider credsProvider = new BasicCredentialsProvider();
                client.getCredentialsProvider().setCredentials(authScope, credentials);
            }
            client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);

        }
    	context = new BasicHttpContext();
    	cookieStore = new BasicCookieStore();
    	context.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
	}

        private void getProxyDataFromURL(String proxyURL) throws PcException{

            try {
                String mainStr = "";
                if (proxyURL != null && !proxyURL.isEmpty()){
                    String[] urlSplit = proxyURL.split("://");

                    proxyScheme = urlSplit[0];
                        mainStr = urlSplit[1];
                    if (mainStr.contains(":")){
                        proxyHostName = mainStr.split(":")[0];
                        proxyPort = Integer.parseInt(mainStr.split(":")[1]);
                    }else{
                        proxyHostName = mainStr;
                        proxyPort = 80;
                    }



                }
            } catch (Exception ex) {
                throw new PcException("Error: Validating Proxy URL: " + ex + " Please add a proxy URL in this pattern: http(s)://<host>:<port> or leave blank");
            }



        }
    
    public boolean authenticate(String userName, String password) throws PcException, ClientProtocolException, IOException {
        String userNameAndPassword = userName + ":" + password;
        String encodedCredentials = Base64Encoder.encode(userNameAndPassword.getBytes());
        HttpGet authRequest = new HttpGet(String.format(AUTHENTICATION_LOGIN_URL,webProtocol, pcServer));
        authRequest.addHeader("Authorization", String.format("Basic %s", encodedCredentials));
        executeRequest(authRequest);
        return true;
    }

    public PcRunResponse startRun(int testId, int testInstaceId, TimeslotDuration timeslotDuration,
            String postRunAction, boolean vudsMode) throws PcException, ClientProtocolException, IOException {
     //   logger.println("Starting run");
        HttpPost startRunRequest = new HttpPost(String.format(baseURL + "/%s", RUNS_RESOURCE_NAME));
        startRunRequest.addHeader(RESTConstants.CONTENT_TYPE, CONTENT_TYPE_XML);
        PcRunRequest runRequestData = new PcRunRequest(testId, testInstaceId, 0, timeslotDuration, postRunAction, vudsMode);
        startRunRequest.setEntity(new StringEntity(runRequestData.objectToXML(), ContentType.APPLICATION_XML));
        HttpResponse response = executeRequest(startRunRequest);
        String startRunResponse = IOUtils.toString(response.getEntity().getContent());
        return PcRunResponse.xmlToObject(startRunResponse);
    }


    public int createTestInstance(int testId, int testSetId) throws PcException, ClientProtocolException, IOException {
        HttpPost createTestInstanceRequest = new HttpPost(String.format(baseURL + "/%s", TEST_INSTANCES_NAME));
        TestInstanceCreateRequest testInstanceCreateRequest = new TestInstanceCreateRequest(testId,testSetId);
        createTestInstanceRequest.setEntity(new StringEntity(testInstanceCreateRequest.objectToXML(), ContentType.APPLICATION_XML));
        createTestInstanceRequest.addHeader(RESTConstants.CONTENT_TYPE, CONTENT_TYPE_XML);
        HttpResponse response = executeRequest(createTestInstanceRequest);
        String responseXml = IOUtils.toString(response.getEntity().getContent());
        int testInstanceID = 0;
        try {
            testInstanceID = testInstanceCreateRequest.getTestInstanceIDFromResponse(responseXml,"TestInstanceID");
        } catch (SAXException|ParserConfigurationException e) {
            throw new PcException("createTestInstance exception: " + e);
        }
        return testInstanceID;
    }

    public PcTestSets GetAllTestSets()throws IOException,PcException{
        String getTestSetsUrl = String.format(baseURL + "/%s", TEST_SETS_NAME);
        HttpGet getTestSetsRequest = new HttpGet(getTestSetsUrl);
        HttpResponse response = executeRequest(getTestSetsRequest);
        String testSets = IOUtils.toString(response.getEntity().getContent());
        return PcTestSets.xmlToObject(testSets);
    }

    public PcTestInstances getTestInstancesByTestId(int testId)throws PcException,IOException{
        String uri = String.format(baseURL + "/%s?%s=%s", TEST_INSTANCES_NAME,"query",URLEncoder.encode("{test-id[" + testId + "]}","UTF-8"));
        HttpGet getFirtstTestInstanceByTestID = new HttpGet(uri);
        HttpResponse response = executeRequest(getFirtstTestInstanceByTestID);
        String testInstances = IOUtils.toString(response.getEntity().getContent());
        return PcTestInstances.xmlToObject(testInstances);

    }

    public boolean stopRun(int runId, String stopMode) throws PcException, ClientProtocolException, IOException {
        String stopUrl = String.format(baseURL + "/%s/%s/%s", RUNS_RESOURCE_NAME, runId, stopMode);
        HttpPost stopRunRequest = new HttpPost(stopUrl);
        ReleaseTimeslot releaseTimesloteRequest = new ReleaseTimeslot(true, "Do Not Collate");
        stopRunRequest.addHeader(RESTConstants.CONTENT_TYPE, CONTENT_TYPE_XML);
        stopRunRequest.setEntity(new StringEntity (releaseTimesloteRequest.objectToXML(),ContentType.APPLICATION_XML)); 
        executeRequest(stopRunRequest);
        return true;
    }

    public PcRunResponse getRunData(int runId) throws PcException, ClientProtocolException, IOException {
        HttpGet getRunDataRequest = new HttpGet(String.format(baseURL + "/%s/%s", RUNS_RESOURCE_NAME, runId));
        HttpResponse response = executeRequest(getRunDataRequest);
        String runData = IOUtils.toString(response.getEntity().getContent());
        return PcRunResponse.xmlToObject(runData);
    }

    public PcTest getTestData(int testId) throws IOException, PcException {
        HttpGet getTestDataRequest = new HttpGet(String.format(baseURL + "/%s/%s",TESTS_RESOURCE_NAME,testId ));
        HttpResponse response = executeRequest(getTestDataRequest);
        String testData = IOUtils.toString(response.getEntity().getContent());
        return PcTestData.xmlToObject(testData);
    }

    public PcRunResults getRunResults(int runId) throws PcException, ClientProtocolException, IOException {
        String getRunResultsUrl = String
            .format(baseURL + "/%s/%s/%s", RUNS_RESOURCE_NAME, runId, RESULTS_RESOURCE_NAME);
        HttpGet getRunResultsRequest = new HttpGet(getRunResultsUrl);
        HttpResponse response = executeRequest(getRunResultsRequest);
        String runResults = IOUtils.toString(response.getEntity().getContent());
        return PcRunResults.xmlToObject(runResults);
    }

    public boolean GetRunResultData(int runId, int resultId, String localFilePath) throws PcException, ClientProtocolException, IOException {
        String getRunResultDataUrl = String.format(baseURL + "/%s/%s/%s/%s/data", RUNS_RESOURCE_NAME, runId,
                RESULTS_RESOURCE_NAME, resultId);
        HttpGet getRunResultRequest = new HttpGet(getRunResultDataUrl);
        HttpResponse response = executeRequest(getRunResultRequest);
        OutputStream out = new FileOutputStream(localFilePath);
        InputStream in = response.getEntity().getContent();
        IOUtils.copy(in, out);
        IOUtils.closeQuietly(in);
        IOUtils.closeQuietly(out);
        return true;
    }


    public TrendReportTransactionDataRoot getTrendReportByXML (String trendReportId, int runId) throws PcException, ClientProtocolException, IOException {
        String getTrendReportByXMLUrl = String.format(baseURL + "/%s/%s/%s", TREND_REPORT_RESOURCE_NAME, trendReportId,runId);
        HttpGet getTrendReportByXMLRequest = new HttpGet(getTrendReportByXMLUrl);
        HttpResponse response = executeRequest(getTrendReportByXMLRequest);
        String trendReportByXML = IOUtils.toString(response.getEntity().getContent(), CharEncoding.UTF_8);
        return TrendReportTransactionDataRoot.xmlToObject(trendReportByXML);
    }


    public boolean updateTrendReport(String trendReportId, TrendReportRequest trendReportRequest) throws PcException, IOException {

        String updateTrendReportUrl = String.format(baseURL + "/%s/%s", TREND_REPORT_RESOURCE_NAME, trendReportId);
        HttpPost updateTrendReportRequest = new HttpPost(updateTrendReportUrl);
        updateTrendReportRequest.addHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_XML);
        updateTrendReportRequest.setEntity(new StringEntity(trendReportRequest.objectToXML(), ContentType.APPLICATION_XML));
        executeRequest(updateTrendReportRequest);
        return true;
    }



    public InputStream getTrendingPDF(String trendReportId) throws IOException, PcException {

        String getTrendReportUrl = String.format(baseURL + "/%s/%s/%s", TREND_REPORT_RESOURCE_NAME, trendReportId,TREND_REPORT_RESOURCE_SUFFIX);
        HttpGet getTrendReportRequest = new HttpGet(getTrendReportUrl);
        executeRequest(getTrendReportRequest);

        HttpResponse response = executeRequest(getTrendReportRequest);
        InputStream in = response.getEntity().getContent();

        return in;

    }

    public ArrayList<PcTrendedRun> getTrendReportMetaData (String trendReportId) throws PcException, ClientProtocolException, IOException {
        String getTrendReportMetaDataUrl = String.format(baseURL + "/%s/%s", TREND_REPORT_RESOURCE_NAME, trendReportId);
        HttpGet getTrendReportMetaDataRequest = new HttpGet(getTrendReportMetaDataUrl);
        HttpResponse response = executeRequest(getTrendReportMetaDataRequest);
        String trendReportMetaData = IOUtils.toString(response.getEntity().getContent());
        return PcTrendReportMetaData.xmlToObject(trendReportMetaData);
    }
    
    public PcRunEventLog getRunEventLog(int runId) throws PcException, ClientProtocolException, IOException {
        String getRunEventLogUrl = String
            .format(baseURL + "/%s/%s/%s", RUNS_RESOURCE_NAME, runId, EVENTLOG_RESOURCE_NAME);
        HttpGet getRunEventLogRequest = new HttpGet(getRunEventLogUrl);
        HttpResponse response = executeRequest(getRunEventLogRequest);
        String runEventLog = IOUtils.toString(response.getEntity().getContent());
        return PcRunEventLog.xmlToObject(runEventLog);
    }
    
    public boolean logout() throws PcException, ClientProtocolException, IOException {
        HttpGet logoutRequest = new HttpGet(String.format(AUTHENTICATION_LOGOUT_URL, webProtocol,pcServer));
        executeRequest(logoutRequest);
        return true;
    }

    protected HttpResponse executeRequest(HttpRequestBase request) throws PcException, IOException {

        HttpResponse response = client.execute(request,context);
			if (!isOk(response)){
				String message;
				try {
					String content = IOUtils.toString(response.getEntity().getContent());
//                    logger.println("DEBUGMSG - response content: " + content);
					PcErrorResponse exception = PcErrorResponse.xmlToObject(content);
					message  = String.format("%s Error code: %s", exception.ExceptionMessage, exception.ErrorCode);
                } catch (Exception ex) {
					message = response.getStatusLine().toString();
				}
				throw new PcException("executeRequest exception: " + message);
			}  		
    		return response;           
    }

	public static boolean isOk (HttpResponse response) {
	    return validStatusCodes.contains(response.getStatusLine().getStatusCode());
	}
	
    protected String getBaseURL() {
        return baseURL;
    }

}
