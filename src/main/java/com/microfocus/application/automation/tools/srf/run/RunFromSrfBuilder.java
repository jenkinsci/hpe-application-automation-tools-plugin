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

package com.microfocus.application.automation.tools.srf.run;
import com.microfocus.application.automation.tools.srf.model.*;
import com.microfocus.application.automation.tools.srf.settings.SrfServerSettingsBuilder;
import com.microfocus.application.automation.tools.srf.results.SrfResultFileWriter;
import com.microfocus.application.automation.tools.srf.utilities.SrfClient;
import com.microfocus.application.automation.tools.srf.utilities.SrfTrustManager;
import com.microfocus.application.automation.tools.srf.utilities.SseEventListener;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import groovy.transform.Synchronized;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.auth.AuthenticationException;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.SseFeature;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Dispatcher;
import org.kohsuke.stapler.QueryParameter;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.net.ssl.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

/**
 * Created by shepshel on 20/07/2016.
 */


public class RunFromSrfBuilder extends Builder implements Serializable, Observer {
    private static final long serialVersionUID = 3;
    private transient PrintStream logger;
    private boolean _https;
    private AbstractBuild<?, ?> build;
    private  String srfTestId;
    private  String srfBuildNumber;
    private  String srfTagNames;
    private  String srfReleaseNumber;
    private String srfTunnelName;
    private boolean srfCloseTunnel;
    private List<SrfTestParamsModel> srfTestParameters;
    private JSONArray jobIds;
    private SseEventListener sseEventListener;
    private HashSet<String> runningCount;
    private transient EventSource eventSrc;
    private String _ftaasServerAddress;
    private String _app;
    private String _tenant;
    private String _secret;
    private boolean _secretApplied;
    private transient HttpURLConnection _con;
    private static SrfTrustManager _trustMgr = new SrfTrustManager();
    private static SSLSocketFactory _factory;
    private String _token;
    private CompletableFuture<Boolean> srfExecutionFuture;
    private SrfClient srfClient;
    private static final Logger systemLogger = Logger.getLogger(RunFromSrfBuilder.class.getName());

    @DataBoundConstructor
    public RunFromSrfBuilder( String srfTestId,
                              String srfTagNames,
                              String srfReleaseNumber,
                              String srfBuildNumber,
                              String srfTunnelName,
                              boolean srfCloseTunnel,
                              List<SrfTestParamsModel> srfTestParameters ) {

        this.srfTestId = srfTestId;
        this.srfTagNames = srfTagNames;
        this.srfTestParameters = srfTestParameters;
        this.srfBuildNumber = srfBuildNumber;
        this.srfReleaseNumber = srfReleaseNumber;
        this.srfCloseTunnel = srfCloseTunnel;
        this.srfTunnelName = srfTunnelName;
    }

    class TestRunData implements Serializable
    {

        private String id;                   // "932c6c3e-939e-4b17-a04f-1a2951481758",
        private String name;                 // "Test-Test-Run",
        private String Start;                // "2016-07-25T08:27:59.318Z",
        private String duration;
        private String status;               // "status" : "success",
        private String testId;              // "246fa1a7-7ed2-4203-a4e9-7ce5fbf4f800",

        public TestRunData(JSONObject obj)
        {
            try {
                id = (String) obj.get("id");
                status = (String) obj.get("status");
                if (id == null) {
                    id = (String) obj.get("message");
                    status = "failed";
                }
                name = (String) obj.get("name");
                duration = obj.get("durationMs").toString();
            }
            catch (Exception e)
            {

            }
        }


        public void merge(TestRunData newData)
        {
            if (newData.name != null )  this.name = newData.name;
            if (newData.Start != null )  this.Start = newData.Start;
            if (newData.duration != null )  this.duration = newData.duration;
            if (newData.status != null )  this.status = newData.status;
            if (newData.testId != null )  this.testId = newData.testId;
            if (newData.duration != null )  this.duration = newData.duration;
        }
    }


    static class OpenThread extends Thread {
        private final EventSource eventSource;

        public OpenThread(EventSource eventSource) {
            this.eventSource = eventSource;
        }

        @Override
        public void run() {
            eventSource.open();
        }
    }
    static EventSource openAsynch(WebTarget target, String auth) {
        target.request(MediaType.APPLICATION_JSON_TYPE).header("Authorization", auth);
        EventSource eventSource = new EventSource(target, false);
        HttpsURLConnection.setDefaultSSLSocketFactory(RunFromSrfBuilder._factory);
        new OpenThread(eventSource).start();
        return eventSource;
    }

    public String getSrfTestId() {
        return srfTestId;
    }
    public String getSrfTunnelName() {
        return srfTunnelName;
    }
    public boolean getSrfCloseTunnel() {
        return srfCloseTunnel;
    }
    public String getSrfBuildNumber(){
        return srfBuildNumber;
    }
    public String getSrfReleaseNumber(){
        return srfReleaseNumber;
    }
    public String getSrfTagNames() {
        return srfTagNames;
    }
    public List<SrfTestParamsModel> getSrfTestParameters() {return  srfTestParameters;  }

    public String getRunResultsFileName() {
        return String.format( "report%1d.xml", build.number);
    }
    @Synchronized
    @Override
    public RunFromSrfBuilder.DescriptorImpl getDescriptor() {
        return (RunFromSrfBuilder.DescriptorImpl) super.getDescriptor();
    }
    // TODO: REMOVE THIS AND USE ONLY CLIENT
    public static JSONObject getSrfConnectionData(AbstractBuild<?, ?> build, PrintStream logger) {
        try {
            CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            String path = build.getProject().getParent().getRootDir().toString();
            path = path.concat("/com.microfocus.application.automation.tools.srf.settings.SrfServerSettingsBuilder.xml");
            File file = new File(path);
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(file);
            // This also shows how you can consult the global configuration of the builder
            JSONObject connectionData = new JSONObject();

            String credentialsId = document.getElementsByTagName("credentialsId").item(0).getTextContent();
            UsernamePasswordCredentials credentials = CredentialsProvider.findCredentialById(credentialsId,
                    StandardUsernamePasswordCredentials.class,
                    build,
                    URIRequirementBuilder.create().build());

            String app = credentials.getUsername();
            String tenant = app.substring(1, app.indexOf('_'));
            String secret = credentials.getPassword().getPlainText();
            String server = document.getElementsByTagName("srfServerName").item(0).getTextContent();

            // Normalize SRF server URL string if needed
            if (server.substring(server.length() - 1).equals("/")) {
                server = server.substring(0, server.length() - 1);
            }

            boolean https = true;
            if (!server.startsWith("https://")) {
                if (!server.startsWith("http://")) {
                    String tmp = server;
                    server = "https://";
                    server = server.concat(tmp);
                } else
                    https = false;
            }
            URL urlTmp = new URL(server);
            if (urlTmp.getPort() == -1) {
                if (https)
                    server = server.concat(":443");
                else
                    server = server.concat(":80");
            }
            String srfProxy = "";
            String srfTunnel = "";
            try {
                srfProxy = document.getElementsByTagName("srfProxyName").item(0) != null ? document.getElementsByTagName("srfProxyName").item(0).getTextContent().trim() : null;
                srfTunnel = document.getElementsByTagName("srfTunnelPath").item(0) != null ? document.getElementsByTagName("srfTunnelPath").item(0).getTextContent() : null;
            }
            catch (Exception e){
               throw e;
            }
            connectionData.put("app", app);
            connectionData.put("tunnel", srfTunnel);
            connectionData.put("secret", secret);
            connectionData.put("server", server);
            connectionData.put("https", (https) ? "True" : "False");
            connectionData.put("proxy", srfProxy);
            connectionData.put("tenant", tenant);
            return connectionData;
        }
        catch (ParserConfigurationException e){
            logger.print(e.getMessage());
            logger.print("\n\r");
        }
        catch (SAXException | IOException e){
            logger.print(e.getMessage());
        }
        return null;
    }

    @Override
    public void update(Observable o, Object eventType) {
        SrfSseEventNotification srfSseEventNotification = (SrfSseEventNotification) eventType;
        switch (srfSseEventNotification.srfTestRunEvent) {
            case TEST_RUN_END:
                boolean removed = this.runningCount.remove(srfSseEventNotification.testRunId);
                if (!removed){
                    systemLogger.warning(String.format("Received TEST_RUN_END event for non existing run %s", srfSseEventNotification.testRunId));
                    return;
                }

                if (runningCount.size() > 0)
                    return;
                break;
            default:
                return;
        }

        JSONArray testRes;

        try {
            testRes = srfClient.getTestRuns(jobIds);

            int sz = testRes.size();
            for(int i = 0; i <sz; i++){
                JSONObject jo = testRes.getJSONObject(i);
                jo.put("tenantid", _tenant);
            }

            SrfResultFileWriter.writeJsonReport(build.getRootDir().getPath(), testRes.toString());
            SrfResultFileWriter.writeXmlReport(build, testRes, _tenant);
            SrfResultFileWriter.writeOctaneResultsUrlFile(testRes, build.getRootDir().getPath(), _tenant, _ftaasServerAddress);

            switch (build.getResult().toString()) {
                case "SUCCESS":
                    this.srfExecutionFuture.complete(true);
                    return;
                case "ABORTED":
                case "FAILURE":
                    this.srfExecutionFuture.complete(false);
                    return;
                default:
                    systemLogger.warning(String.format("Received undefined build result: %s", build.getResult().toString()));
                    this.srfExecutionFuture.complete(false);
                    break;
            }

        } catch (Exception e) {
            logger.print(e.getMessage());
            this.srfExecutionFuture.complete(false);
        } finally {
            cleanUp();
        }
    }

    private JSONArray getTestResults(JSONArray tests) throws IOException {
        String url = "";
        JSONArray res = new JSONArray();
        for (int i = 0; i < tests.size(); i++) {
            url = _ftaasServerAddress.concat("/rest/test-manager/test-runs");
            url = url.concat("?access-token=");
            url=url.concat(_token);
            url=url.concat("&");

            String runId = tests.get(i).toString();
            url = url.concat(String.format("id=%1s", runId));
            url = url.concat("&include=resource,script-runs,script-steps");

            URL srvUrl = new URL(url);
            HttpURLConnection con;
            con = (HttpURLConnection) srvUrl.openConnection();
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type",     "application/json");
            int rc = con.getResponseCode();
            if(rc == 500)
            {
                i--;
                continue;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader((con.getInputStream())));
            StringBuffer response = new StringBuffer();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            JSONArray tmp = JSONArray.fromObject(response.toString());
            res.addAll(tmp);
        }
        return res;
    }

    private String applyJobParams(String val){
        if ((val.length() > 2) && val.startsWith("${") && val.endsWith("}")) {
            String varName = val.substring(2, val.length() - 1);
            val = build.getBuildVariables().get(varName);
            if(val == null){
                try {
                    val = build.getEnvironment().get(varName);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        else if ((val.length() > 2) && val.startsWith("%") && val.endsWith("%")) {
            String varName = val.substring(1, val.length() - 1);
            try {
                val = build.getEnvironment().get(varName);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return val;
    }


    private JSONObject createExecutionReqBody() throws IOException, SrfException {

        JSONObject data = new JSONObject();
        JSONObject testParams = new JSONObject();

        if (srfTestId != null && !srfTestId.isEmpty()) {
            String[] testIds = normalizeParam(applyJobParams(srfTestId));
            data.put("testYac", testIds);
        } else if (srfTagNames != null && !srfTagNames.isEmpty()) {
            String[] tagNames = normalizeParam(srfTagNames);
            data.put("tags", tagNames);
        } else
            throw new SrfException("Both test id and test tags are empty");

        if (srfTunnelName != null && srfTunnelName.length() > 0) {
            data.put("tunnelName", srfTunnelName);
        }

        if(data.size() == 0){
            throw new IOException("Wrong filter");
        }

        testParams.put("filter", data);
        String buildNumber = applyJobParams(srfBuildNumber);
        String releaseNumber = applyJobParams(srfReleaseNumber);
        if (buildNumber != null && buildNumber.length() > 0) {
            data.put("build", buildNumber);
        }
        if (releaseNumber != null && releaseNumber.length() > 0)
            data.put("release", releaseNumber);

        this.logger.print(String.format("Required build & release: %1s %2s\n\r", buildNumber, releaseNumber));
        HashMap<String, String> paramObj = new HashMap<String, String>();
        int cnt = 0;

        if (srfTestParameters != null && !srfTestParameters.isEmpty()) {
            cnt = srfTestParameters.size();
            if (cnt > 0)
                logger.print("Parameters: \n\r");
            for (int i = 0; i < cnt; i++) {
                String name = srfTestParameters.get(i).getName();
                String val = applyJobParams(srfTestParameters.get(i).getValue());
                paramObj.put(name, val);
                logger.print(String.format("%1s : %2s\n\r", name, val));
            }
        }

        if (cnt > 0)
            data.put("params", paramObj);

        return data;
    }

    private String[] normalizeParam(String paramToNormalize) {
        String[] params = paramToNormalize.split(",");
        for (int i = 0; i < params.length; i++) {
            // Normalize param
            String param = params[i];
            params[i] = param.trim();
        }
        return params;
    }

    private JSONArray executeTestsSet() throws IOException, SrfException, AuthorizationException {
        JSONObject requestBody = createExecutionReqBody();
        JSONArray jobs = srfClient.executeTestsSet(requestBody);
        if (jobs == null || jobs.size() == 0)
            throw new SrfException(String.format("No tests found for %s", this.srfTestId != null && !this.srfTestId.equals("") ? "test id: " + this.srfTestId : "test tags: " + this.srfTagNames));
        return getJobIds(jobs);
    }

    private JSONArray getJobIds(JSONArray jobs) {
        JSONArray jobIds = new JSONArray();
        int cnt = jobs.size();
        for (int k = 0; k < cnt; k++ ){
            JSONObject job = jobs.getJSONObject(k);
            try {
                if (job.has("error")) {
                    String errorClassName = job.get("error").getClass().getSimpleName();
                    switch (errorClassName) {
                        case "JSONObject":
                            JSONObject jobExecutionError = job.getJSONObject("error");
                            handleJobError(jobIds, jobExecutionError);
                            break;
                        case "JSONArray":
                            JSONArray jobExecutionErrors = job.getJSONArray("error");
                            for (Object jobError : jobExecutionErrors) {
                                JSONObject error = (JSONObject) jobError;
                                handleJobError(jobIds, error);
                            }
                            break;
                        default:
                            throw new SrfException(String.format("Received unexpected error class type, expected 'JSONObject' or 'JSONArray' but received %s", errorClassName));
                    }
                } else {
                    jobIds.add(job.getString("jobId"));
                    runningCount.add(job.getString("testRunId"));
                }
            } catch (Exception e) {
                systemLogger.severe(e.getLocalizedMessage());
            }
        }
        return jobIds;
    }

    private void handleJobError(JSONArray jobIds, JSONObject jobExecutionError) {
        JSONObject errorParameters = jobExecutionError.getJSONObject("parameters");
        String testRunId = errorParameters.getString("testRunId");
        // Make sure we won't add the same run in case job has multiple errors
        if (!runningCount.contains(testRunId)) {
            jobIds.add(errorParameters.getString("jobId"));
            runningCount.add(testRunId);
        }
    }

    private String addAuthentication(HttpsURLConnection con){
        String auth = _app +":"+_secret;
        byte[] auth64 = Base64.encodeBase64(auth.getBytes());
        String data = "Basic " + new String(auth64);
        if(con != null)
            con.addRequestProperty("Authorization", data);
        return data;
    }

    private void initSrfEventListener() throws IOException, IllegalArgumentException, SrfException, AuthorizationException {
        // !!! Important Notice !!!
        // by using 'level=session' in the sse request we're ensuring that we'll get only events related
        // to this run since we're creating a new session (token) for each run
        String urlSSe = _ftaasServerAddress.concat("/rest/test-manager/events")
                .concat("?level=session&types=test-run-started,test-run-ended,test-run-count,script-step-updated,script-step-created,script-run-started,script-run-ended");
        urlSSe = urlSSe.concat("&access-token=").concat(_token);

        ClientBuilder sslBuilder = ClientBuilder.newBuilder();
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            _trustMgr = new SrfTrustManager();
            sslContext.init(null, new SrfTrustManager[]{_trustMgr}, null);
            SSLContext.setDefault(sslContext);
        } catch (NoSuchAlgorithmException | KeyManagementException e1) {return;}

        sslBuilder.register(SSLContext.class);
        Client client = sslBuilder.register(SseFeature.class).build();
        client.register(sslContext);
        try {
            client.getSslContext().init(null, new SrfTrustManager[]{_trustMgr}, null);
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        WebTarget target = client.target(urlSSe);

        eventSrc = openAsynch(target, addAuthentication(null));
        if (eventSrc == null){
            throw new SrfException("Failed to initiate open event source for SRF SSE");
        }

        eventSrc.register(this.sseEventListener);
    }

    @Override
    public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, BuildListener _listener)
            throws InterruptedException, IOException {

        this.logger = _listener.getLogger();
        Dispatcher.TRACE = true;
        Dispatcher.TRACE_PER_REQUEST=true;

        this._token=null; // Important in order to get only this run events
        this.build = build;
        this.sseEventListener = new SseEventListener(this.logger);
        this.sseEventListener.addObserver(this);
        this.srfExecutionFuture = new CompletableFuture<>();
        this.runningCount = new HashSet<>();

        JSONObject conData = getSrfConnectionData(build, logger);
        if(conData == null)
            return false;

        _app = conData.getString("app");
        _secret = conData.getString("secret");
        _ftaasServerAddress = conData.getString("server");
        _https = conData.getBoolean("https");
        _tenant = conData.getString("tenant");
        String srfProxy =  conData.getString("proxy");

        URL proxy = null;
        if((srfProxy != null) && (srfProxy.length() != 0)) {
            proxy = new URL(srfProxy);
            String proxyHost = proxy.getHost();
            String proxyPort = String.format("%d",proxy.getPort());
            Properties systemProperties = System.getProperties();
            systemProperties.setProperty("https.proxyHost", proxyHost);
            systemProperties.setProperty("http.proxyHost", proxyHost);
            systemProperties.setProperty("https.proxyPort", proxyPort);
            systemProperties.setProperty("http.proxyPort", proxyPort);
        }

        try{
            SSLContext sslContext = SSLContext.getInstance("TLS");
            _trustMgr = new SrfTrustManager();
            sslContext.init(null,new SrfTrustManager[]{_trustMgr }, null);
            SSLContext.setDefault(sslContext);
            _factory = sslContext.getSocketFactory();
            this.srfClient = new SrfClient(_ftaasServerAddress, _tenant, _factory, proxy);
        }
        catch (NoSuchAlgorithmException | KeyManagementException e){
            logger.print(e.getMessage());
            logger.print("\n\r");
        }

        jobIds = null;
        try {
            srfClient.login(_app, _secret);
            this._token = srfClient.getAccessToken();

            initSrfEventListener();
            jobIds = executeTestsSet();

        } catch (UnknownHostException | ConnectException | SSLHandshakeException | IllegalArgumentException | AuthorizationException | AuthenticationException e) {
            cleanUp();
            logger.println(String.format("ERROR: Failed logging into SRF server: %s %s", this._ftaasServerAddress, e));
            return false;
        } catch (IOException | SrfException e) {
            cleanUp();
            logger.println(String.format("ERROR: Failed executing test, %s", e));
            return false;
        }

        try {
            boolean buildResult = this.srfExecutionFuture.get();
            return buildResult;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            build.setResult(Result.ABORTED);
            // TODO: Optimization instead of testrunid set maintain testrunid map with job info, in order to avoid already finished job cancellation
            if (!jobIds.isEmpty()) {
                for (int i = 0; i < jobIds.size(); i++) {
                    String jobId = jobIds.get(i).toString();
                    try {
                        srfClient.cancelJob(jobId);
                    } catch (SrfException e1) {
                        e1.printStackTrace();
                    }
                }
            }

            return false;
        } finally {
            cleanUp();
        }
    }

    private void cleanUp() {
        if (eventSrc != null) {
            eventSrc.close();
            eventSrc = null;
        }

        if (_con != null){
            _con.disconnect();
            _con = null;
        }

        if(srfCloseTunnel && CreateTunnelBuilder.Tunnels != null){
            for (Process p:CreateTunnelBuilder.Tunnels){
                p.destroy();
            }
            CreateTunnelBuilder.Tunnels.clear();
        }
    }


    @Extension
    // This indicates to Jenkins that this is an implementation of an extension
    // point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Execute SRF tests";
        }

        public SrfServerSettingsBuilder.SrfDescriptorImpl getSrfServerSettingsBuilderDescriptor() throws SrfException {
            return Jenkins.getInstance().getDescriptorByType(
                        SrfServerSettingsBuilder.SrfDescriptorImpl.class);
        }

        public FormValidation doCheckSrfTimeout(@QueryParameter String value) {

            if (StringUtils.isEmpty(value)) {
                return FormValidation.ok();
            }

            String val1 = value.trim();

            if (val1.length() > 0 && val1.charAt(0) == '-')
                val1 = val1.substring(1);

            if (!StringUtils.isNumeric(val1) && !val1.isEmpty()) {
                return FormValidation.error("Timeout name must be a number");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckSrfTagNames(@QueryParameter String value) {
             if (StringUtils.isBlank(value))
                return FormValidation.ok();

            return FormValidation.ok();
        }

    }

}
