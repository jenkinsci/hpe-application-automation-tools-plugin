package com.hpe.application.automation.tools.run;
import com.hpe.application.automation.tools.model.SrfServerSettingsModel;
import com.hpe.application.automation.tools.model.SrfTestParamsModel;
import com.hpe.application.automation.tools.settings.SrfServerSettingsBuilder;
import groovy.transform.Synchronized;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.auth.AuthenticationException;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.media.sse.EventListener;
import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Dispatcher;
import org.kohsuke.stapler.QueryParameter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.net.ssl.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Created by shepshel on 20/07/2016.
 */


    public class RunFromSrfBuilder extends Builder implements java.io.Serializable {
        static final long serialVersionUID = 3;
        class TestRunData implements java.io.Serializable
    {
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

        String id;                   // "932c6c3e-939e-4b17-a04f-1a2951481758",
        String name;                 // "Test-Test-Run",
        String Start;                // "2016-07-25T08:27:59.318Z",
        String duration;
        String status;               // "status" : "success",
        String testId;              // "246fa1a7-7ed2-4203-a4e9-7ce5fbf4f800",
        int         execCount;
        String [] tags;
        String user;
            JSONObject _context;
//            "scriptStatus" : {},
//            "environmentCount" : 0,
 //                   "scriptCount" : 0
 //       },
 //           "scriptRuns" : []
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
        private java.util.Hashtable<String, TestRunData> _testRunData;
        private JSONArray tests ;
        public transient Object srfTestArg = null;
        @DataBoundConstructor
        public RunFromSrfBuilder( String srfTestId,
                                  String srfTagNames,
                                  String srfReleaseNumber,
                                  String srfBuildNumber,
                                  String srfTunnelName,
                                  boolean srfCloseTunnel,
                                  List<SrfTestParamsModel> srfTestParameters ){

            this.srfTestId = srfTestId;
            this.srfTagNames = srfTagNames;
            this.srfTestParameters = srfTestParameters;
            this.srfBuildNumber = srfBuildNumber;
            this.srfReleaseNumber = srfReleaseNumber;
            this.srfCloseTunnel = srfCloseTunnel;
            this.srfTunnelName = srfTunnelName;
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
    private boolean _runStatus;
    private int runningCount;
    private transient EventSource eventSrc;
    private ArrayList<String> _testRunEnds;
    private int _timeout;
    private String _ftaasServerAddress;
    private String _app;
    private String _tenant;
    private String _secret;
    private boolean _success;
    private boolean _secretApplied;
    private transient HttpURLConnection _con;
    private static SrfTrustManager _trustMgr = new SrfTrustManager();
    static SSLSocketFactory _factory;
    public static JSONObject GetSrfConnectionData(AbstractBuild<?, ?> build, PrintStream logger) {
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
            path = path.concat("/com.hpe.application.automation.tools.settings.SrfServerSettingsBuilder.xml");
            File file = new File(path);
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(file);
            // This also shows how you can consult the global configuration of the builder
            JSONObject connectionData = new JSONObject();


            String app = document.getElementsByTagName("srfAppName").item(0).getTextContent();
            String tenant = app.substring(1, app.indexOf('_'));
            String secret = document.getElementsByTagName("srfSecretName").item(0).getTextContent();
            String server = document.getElementsByTagName("srfServerName").item(0).getTextContent();
            boolean https = true;
            if (server.startsWith("https://") == false) {
                if (server.startsWith("http://") == false) {
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
                    server = server.concat(":8080");
            }
            String srfProxy = "";
            String srfTunnel = "";
            try {
                srfProxy = document.getElementsByTagName("srfProxyName").item(0).getTextContent();
                srfTunnel = document.getElementsByTagName("srfTunnelPath").item(0).getTextContent();
            }
            catch (Exception e){

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
        catch (SAXException e){
            logger.print(e.getMessage());
        }
        catch (IOException e){
            logger.print(e.getMessage());
        }
        return null;
    }

    private JSONArray GetTestResults(JSONArray tests) throws IOException {
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
    private String ApplyJobParams(String val){
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
    private HttpURLConnection Connect(String path, String method){
        try {
                String reqUrl = _ftaasServerAddress.concat(path);
                if(_token == null) {
                    _token = LoginToSrf();
                }
                reqUrl = reqUrl.concat("?access-token=");
                reqUrl = reqUrl.concat(_token).concat("&TENANTID="+_tenant);

                URL srvUrl = new URL(reqUrl);
                HttpURLConnection con = (HttpURLConnection) srvUrl.openConnection();
                if(_https)
                    ((HttpsURLConnection)con).setSSLSocketFactory(_factory);
                con.setRequestMethod(method);
                con.setRequestProperty("Content-Type", "application/json");
                if(_https)
                    ((HttpsURLConnection)con).setSSLSocketFactory(_factory);
                return con;
            }
            catch (IOException e){
                return null;
            }
    }
    private void FillExecutionReqBody() throws IOException{
        _con.setDoOutput(true);
        JSONObject data = new JSONObject();
        JSONObject testParams = new JSONObject();
        JSONObject ciParameters = new JSONObject();
        if (srfTestId != null && srfTestId.length() > 0) {
            data.put("yac", ApplyJobParams(srfTestId));
        } else {
            String[] tagNames = ApplyJobParams(srfTagNames).split(",");
            data.put("tags", tagNames);
        }
        if (srfTunnelName != null && srfTunnelName.length() > 0) {

            testParams.put("tunnelName", srfTunnelName);
        }
        if(data.size() == 0){
            throw new IOException("Wrong filter");
        }

        testParams.put("filter", data);
        Properties ciProps = new Properties();
        Properties props = new Properties();
        String buildNumber = ApplyJobParams(srfBuildNumber);
        String releaseNumber = ApplyJobParams(srfReleaseNumber);
        if (buildNumber != null && buildNumber.length() > 0) {
            data.put("build", buildNumber);
        }
        if (releaseNumber != null && releaseNumber.length() > 0)
            data.put("release", releaseNumber);

        this.logger.print(String.format("Required build & release: %1s %2s\n\r", buildNumber, releaseNumber));
        HashMap<String, String> paramObj = new HashMap<String, String>();
        int cnt = 0;

        if ((srfTestParameters != null) && !srfTestParameters.isEmpty()) {
            cnt = srfTestParameters.size();
            if (cnt > 0)
                logger.print("Parameters: \n\r");
            for (int i = 0; i < cnt; i++) {
                String name = srfTestParameters.get(i).getName();
                String val = ApplyJobParams(srfTestParameters.get(i).getValue());
                paramObj.put(name, val);
                logger.print(String.format("%1s : %2s\n\r", name, val));
            }
        }

        if (cnt > 0)
            data.put("params", paramObj);
        //add request header

        //     con.setRequestProperty("session-context", context);
        try {
            OutputStream out = _con.getOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(out);
            writer.write(data.toString());
            writer.flush();
            out.flush();
            out.close();
        } catch (java.net.ProtocolException e) {
            logger.print(e.getMessage());
            logger.print("\n\r");
        }
    }
    private JSONArray GetTestsSet() throws MalformedURLException, AuthenticationException, IOException {

       StringBuffer response = new StringBuffer();
       JSONArray ar = new JSONArray();
        _con = Connect("/rest/jobmanager/v1/execution/jobs", "POST");
        try {
            FillExecutionReqBody();
        }
        catch (IOException e) {
            logger.print(e.getMessage());
            logger.print("\n\r");
            throw e;
        }
            _timeout = 20000;

            int responseCode = _con.getResponseCode();
            BufferedReader br;
            if(responseCode == 401 && _secretApplied){
               throw new AuthenticationException("Login required\n\r");
           }
            if (responseCode == 200) {
                br = new BufferedReader(new InputStreamReader((_con.getInputStream())));
            }
            else {
                br = new BufferedReader(new InputStreamReader((_con.getErrorStream())));
            }

            String inputLine;
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
                logger.println(inputLine);
            }

           if (responseCode != 200) {
                try{
                    JSONArray tests = JSONObject.fromObject(response.toString()).getJSONArray("jobs");
                            JSONArray.fromObject(response.toString());
                    int len = tests.size();
                    for (int i= 0; i < len; i++) {
                        JSONObject jo = tests.getJSONObject(i);
                        if(jo.containsKey("testId") && ( jo.size() == 1)) {
                            ar.add(jo);
                        }
                        else {
                            logger.print("\n\r");
                            logger.print( jo.toString());
                            logger.print("\n\r");
                        }
                    }
                }
                catch(Exception e) {

                }
                if(ar.size() == 0) {
                    logger.print("\n\r");
                    logger.print(response);
                    logger.print("\n\r");
                    String msg = response.toString();
                    throw new IOException(msg);
                }
            }
            else
               ar = JSONObject.fromObject(response.toString()).getJSONArray("jobs");
            JSONArray testAr = new JSONArray();
            int cnt = ar.size();
            for (int k = 0; k < cnt; k++ ){
                testAr.add(ar.getJSONObject(k).getString("jobId"));
            }
            return testAr;
        }


    private String AddAuthentication(HttpsURLConnection con){
        String auth = _app +":"+_secret;
        byte[] auth64 = Base64.encodeBase64(auth.getBytes());
        String data = "Basic " + new String(auth64);
        if(con != null)
            con.addRequestProperty("Authorization", data);
        return data;
    }
private String LoginToSrf() throws MalformedURLException, IOException{
    String authorizationsAddress = _ftaasServerAddress.concat("/rest/security/authorizations/access-tokens");
        //    .concat("/?TENANTID="+_tenant);
        Writer writer = null;
    // login //
    JSONObject login = new JSONObject();
    login.put("loginName",_app);
    login.put("password",_secret);
    OutputStream out;
    URL loginUrl = new URL(authorizationsAddress);
    URLConnection loginCon;
    if(_ftaasServerAddress.startsWith("http://")) {
        loginCon = (HttpURLConnection)loginUrl.openConnection();
        loginCon.setDoOutput(true);
        loginCon.setDoInput(true);
        ((HttpURLConnection) loginCon).setRequestMethod("POST");
        loginCon.setRequestProperty("Content-Type",     "application/json");
        out = loginCon.getOutputStream();
    }
    else {
        loginCon =  loginUrl.openConnection();
        loginCon.setDoOutput(true);
        loginCon.setDoInput(true);
        ((HttpsURLConnection) loginCon).setRequestMethod("POST");
        loginCon.setRequestProperty("Content-Type", "application/json");
        ((HttpsURLConnection) loginCon).setSSLSocketFactory(_factory);
         out = loginCon.getOutputStream();
    }


        writer = new OutputStreamWriter(out);
        writer.write(login.toString());
        writer.flush();
        out.flush();
        out.close();
        int responseCode = ((HttpURLConnection) loginCon).getResponseCode();
    BufferedReader br = new BufferedReader(new InputStreamReader((loginCon.getInputStream())));
    StringBuffer response = new StringBuffer();
    String line;
    while ((line = br.readLine()) != null) {
        response.append(line);
    }
    String tmp = response.toString();
    int n = tmp.length();

    return tmp.substring(1, n-1);
    }
    private void InitSrfEventListener(){
        if(_token != null)
            return;
        try {
               _token = LoginToSrf();
        }
        catch (IOException e){
            if(_token == null){
                logger.println("");
                logger.println(e.getMessage());
                return;
            }
        }
        String urlSSe = _ftaasServerAddress.concat("/rest/test-manager/events")
                .concat("?level=session&types=test-run-started,test-run-ended,test-run-count,script-step-updated,script-step-created,script-run-started,script-run-ended");
        urlSSe = urlSSe.concat("&access-token=").concat(_token);
//        urlSSe = urlSSe.concat("&TENANTID="+_tenant);
        final String delim = "\r\n#########################################################################\r\n";
        ClientBuilder sslBuilder = ClientBuilder.newBuilder();
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            _trustMgr = new SrfTrustManager();
            sslContext.init(null, new SrfTrustManager[]{_trustMgr}, null);
            SSLContext.setDefault(sslContext);
        }
        catch (NoSuchAlgorithmException e1){return;}
        catch (KeyManagementException e2){return;}
        sslBuilder.register(SSLContext.class);
        Client client = sslBuilder.register(SseFeature.class).build();
        client.register(sslContext);
        try {
            client.getSslContext().init(null, new SrfTrustManager[]{_trustMgr}, null);
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        WebTarget target = client.target(urlSSe);
        int responseCode;

        eventSrc = openAsynch(target, AddAuthentication(null));
        eventSrc.register(new EventListener() {
            @Override
            public void onEvent(InboundEvent inboundEvent) {
                _timeout = 20000;
   //             if(tests == null)
 //                   return;
                String eventName = inboundEvent.getName();
    //            logger.print(String.format("***********%1s**********\r\n", eventName));
                String data = inboundEvent.readData();
                String str;
                if(data != null && data.length()> 0)
                    try {

                        JSONObject obj = JSONObject.fromObject(data);
                        if (eventName.compareTo("test-run-count") == 0) {

                            return;
                        }
                        if (eventName.compareTo("test-run-started") == 0) {
                            logger.print(delim);
                            obj.discard("runningCount");
                            JSONObject o1 = JSONObject.fromObject(obj.get("testRun"));
                            str = String.format("\r\n%1s %2s Status:%3s\r\n",
                                    o1.get("name"),
                                    eventName,
                                    o1.get("status"));
                            logger.print(str);
                            _testRunEnds.add(o1.get("id").toString());

                        }
                        if ((eventName.compareTo("test-run-ended") == 0) /*|| (eventName.compareTo("test-run-started") == 0)*/) {
                            int testsCnt=tests.size();
                            boolean skip = true;
                            String id = obj.getJSONObject("testRun").getString("id");
                            for(int i = 0; i < testsCnt; i++){
                                if(id.compareTo(tests.getString(i)) == 0){
                                    skip = false;
                                    break;
                                }
                            }
                            if(skip)
                                return;
                            logger.print(delim);
                            obj.discard("runningCount");
                            JSONObject o1 = JSONObject.fromObject(obj.get("testRun"));
                            o1.discard("id");
                            o1.discard("tags");
                            o1.discard("user");
                            o1.discard("additionalData");
                            obj.discard("testRun");

                            JSONObject o2 = JSONObject.fromObject(o1.get("test"));
                            o1.discard("test");
                            obj.put("testRun", o1);
                            obj.put("environments", o2.get("environments"));
                            obj.put("scripts", o2.get("scripts"));

                            str = String.format("\r\n%1s %2s Status:%3s\r\n",
                                    o1.get("name"),
                                    eventName,
                                    o1.get("status")
                            );
                            logger.print(str);
                            runningCount--;
                        }
                        if (eventName.contains ("script-step-")) {
                            String status = obj.getString("status");
                            if(status.compareTo("running") == 0)
                                return;
                            logger.print(delim);
                            str = String.format("\r\n%1s Status: %2s\r\n",
                                    eventName,
                                    obj.get("status")
                            );
                            logger.print(str);
                            obj.discard("id");
                            obj.discard("scriptRun");
                            obj.discard("snapshot");

                        }
                        if(eventName.contains("script-run-")){
                            logger.print(delim);
                        }
                        logger.print(delim);
                        logger.print(obj.toString(2));
                        logger.print("\r\n");
                    }
                    catch (Exception e){
                        logger.print(e.getMessage());
                    };
                //                                     if(runningCount == 0)
                //                              eventSrc.close();

            }

        });
    }

    String _token;
        @Override
        public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, BuildListener _listener)
                throws InterruptedException, IOException {
            _testRunEnds = new ArrayList<String>();
            _success = true;
            this.logger = _listener.getLogger();
            Dispatcher.TRACE = true;
            Dispatcher.TRACE_PER_REQUEST=true;
            ClassLoader cl = ClassLoader.getSystemClassLoader();
            _token=null;
            this.build = build;
//////////////////////////////////////////////////////////////////////////////////////////////////////////
            JSONObject conData = GetSrfConnectionData(build, logger);
            if(conData == null)
                return false;
            _app = conData.getString("app");
            _secret = conData.getString("secret");
            _ftaasServerAddress = conData.getString("server");
            _https = conData.getBoolean("https");
            _tenant = conData.getString("tenant");
            String srfProxy =  conData.getString("proxy");

            try{
                SSLContext sslContext = SSLContext.getInstance("TLS");
                _trustMgr = new SrfTrustManager();
                sslContext.init(null,new SrfTrustManager[]{_trustMgr }, null);
                SSLContext.setDefault(sslContext);
                _factory = sslContext.getSocketFactory();
            }
            catch (NoSuchAlgorithmException e){
                logger.print(e.getMessage());
                logger.print("\n\r");
            } catch (KeyManagementException e){
                logger.print(e.getMessage());
                logger.print("\n\r");
            };

            if((srfProxy != null) && (srfProxy.length() != 0)) {
                String[] res = srfProxy.split(":", 2);
                Properties systemProperties = System.getProperties();
                String proxy = res[0];
                systemProperties.setProperty("https.proxyHost", proxy);
                if(res.length == 2) {
                    String port = res[1];
                    systemProperties.setProperty("https.proxyPort", port);
                }
            }
//////////////////////////////////////////////////////////////////////
            tests = null;
            InitSrfEventListener();
            _secretApplied = false;
            try {
                while (true) {
                    try {
                        tests = GetTestsSet();
                        if(tests.size()>0 && eventSrc==null)
                            InitSrfEventListener();
                        break;
                    } catch (AuthenticationException e) {
                        InitSrfEventListener();
                        if(_token == null)
                            _token = LoginToSrf();
                        _secretApplied = true;
                    } catch (Exception e) {
                 //       if(!_secretApplied)
                 //           continue;
                        if (eventSrc != null)
                            eventSrc.close();
                        eventSrc = null;
                        return false;
                    }

                }
                runningCount = tests.size();
                while (runningCount > 0) {
                    dowait(1000);
                    _timeout = _timeout - 1000;
                    //         if(_timeout <= 0)
                    //             runningCount --;
                }
            }
            finally {
                if (eventSrc != null) {
                    eventSrc.close();
                    eventSrc = null;
                }
                if (_con != null){
                    _con.disconnect();
                    _con = null;
                    if(srfCloseTunnel){
                        if(CreateTunnelBuilder.Tunnels != null){
                            for (Process p:CreateTunnelBuilder.Tunnels){
                                p.destroy();
                            }
                            CreateTunnelBuilder.Tunnels.clear();
                        }
                    }
                }

            }
            JSONArray testRes = GetTestResults(tests);
            int sz = testRes.size();
            for(int i = 0; i <sz; i++){
                JSONObject jo = testRes.getJSONObject(i);
                jo.put("tenantid", _tenant);
            }
            FileOutputStream fs = null;
            try {
                String name = String.format("%1d/report.json", build.number);
                String path = build.getRootDir().getPath().concat("/report.json");
                File f = new File(path);
                f.createNewFile();
                fs = new FileOutputStream(f);
                fs.write(testRes.toString().getBytes());
            }
            catch (Exception e) {
                logger.print(e.getMessage());
            }
            finally {
                if(fs != null)
                    fs.close();
            }
            String   xmlReport = "";
            try {
                xmlReport = Convert2Xml(testRes);
            }
            catch (ParserConfigurationException e){

            }
            fs = null;
            try {
                String name = String.format("report%1d.xml", build.number); //build.getWorkspace().getParent().child("builds").child(name)
                String htmlName = String.format("%1s/Reports/index.html",build.getWorkspace());//, build.number);
                File f = new File(build.getWorkspace().child(name).toString());
                f.createNewFile();

                fs = new FileOutputStream(f);
                fs.write(xmlReport.getBytes());

                if(_con != null)
                    _con.disconnect();
            }
            catch (Exception e){
                logger.print(e.getMessage());
                if(eventSrc != null)
                    eventSrc.close();
                eventSrc = null;
            }
            finally {
                if(fs != null)
                    fs.close();
            }
            return _success;
        }
    private synchronized void dowait(long time) throws InterruptedException {
        try {
            wait(time);
        } catch (InterruptedException e) {
            throw e;
        }
    }

    String Convert2Xml(JSONArray report) throws ParserConfigurationException{
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.newDocument();
        Element root = doc.createElement("testsuites");
        try {


            root.setAttribute("tenant", _tenant);
            int testsCnt = report.size();
 //          root.setAttribute("tests", String.format("%1d", testsCnt));
            int errorsTestSute = 0;
            int failuresTestSute = 0;
            int timeTestSute = 0;
            for (int i = 0; i < testsCnt; i++) {
                Element testSuite = doc.createElement("testsuite");
                JSONObject test = (JSONObject)(report.get(i));
                String status = test.getString("status");
                if(status == null)
                    status = "errored";
                if(status.compareTo("failed") == 0)
                    _success = false;
                else if(status.compareTo("errored") == 0)
                    _success = false;
                else if(status.compareTo("cancelled") == 0)
                    _success = false;

                String timestamp = test.getString("start");
                String testDuration = test.getString("durationMs");
                if(testDuration == null || testDuration.length() == 0)
                    testDuration = "0";
                int duration_i = Integer .parseInt(testDuration, 10)/1000;
                testSuite.setAttribute("time", String.format("%1d.0",duration_i ));
                testSuite.setAttribute("yac", test.getString("yac"));
                JSONArray scriptRuns = (JSONArray) (test.get("scriptRuns"));

                test = test.getJSONObject("test");
                String name = test.getString("name");
                JSONObject additionalData = test.getJSONObject("additionalData");

                int scriptCnt = scriptRuns.size();
                testSuite.setAttribute("name", name);
                testSuite.setAttribute("timestamp", timestamp);
                testSuite.setAttribute("tests", String.format("%1d", scriptCnt));
                root.appendChild(testSuite);
                for (int j = 0; j < scriptCnt; j++) {
                    Element testCase = doc.createElement("testcase");
                    JSONObject scriptRun = scriptRuns.getJSONObject(j);
                    JSONObject assetInfo = scriptRun.getJSONObject("assetInfo");
                    String scriptName = assetInfo.getString("name");
                    String scriptStatus = scriptRun.getString("status");
                    if("success".compareTo(scriptStatus) != 0){
                        Element failure = doc.createElement("failure");
                        testCase.appendChild(failure);
                    }
                    Element script = doc.createElement("system-out");
                    try {
                        JSONArray steps = scriptRun.getJSONArray("scriptSteps");
                        int nSteps = steps.size();
                        StringBuilder allSteps = new StringBuilder();
                        for (int k = 0; k < nSteps; k++) {
                          allSteps.append(String.format("<p>%1d  %1s</p>",k+1, steps.getJSONObject(k).getString("description")));
                        }
                        script.setTextContent(allSteps.toString());
                    }
                    catch (Exception e){}
                    testCase.appendChild(script);
              //     testCase.setAttribute("classname", scriptName);
                    testCase.setAttribute("name", scriptName);
                    testCase.setAttribute("yac", scriptRun.getString("yac"));
                    String duration =scriptRun.getString("durationMs");
                    if(duration == null)
                        duration = testDuration;
                    duration_i = Integer.parseInt(duration, 10)/1000;
                    testCase.setAttribute("time", String.format("%1d.0",duration_i ));
                    status = scriptRun.getString("status");
                    JSONObject env = scriptRun.getJSONObject("environment");
                    JSONObject os = env.getJSONObject("os");
                    JSONObject browser = env.getJSONObject("browser");
                    String envString = String.format("%1s %1s %1s %1s", os.getString("name"), os.getString("version"), browser.getString("name"), browser.getString("version"));
                    Element envXml = doc.createElement("properties");
                    Element prop = doc.createElement("property");
                    prop.setAttribute("Environment", envString) ;
                    envXml.appendChild(prop);
                    testCase.appendChild(envXml);
                    if((status != null) && (status.compareTo("success") != 0)){
                        if("failed".compareTo(status) == 0) {
                            failuresTestSute++;
                        }
                        else
                            errorsTestSute ++;
                        if(scriptRun.containsKey("errors")) {
                            JSONArray errorsAr = new JSONArray();
                            Object errors = "";
                            try {
                                errorsAr = scriptRun.getJSONArray("errors");
                            }
                            catch (Exception e){
                                try {
                                    errors = scriptRun.getJSONObject("errors");
                                    errorsAr.add(errors);
                                }
                                catch (Exception e1) {
                                    JSONObject jErr = new JSONObject();
                                    jErr.put("error", errors.toString());
                                    errorsAr.add(jErr);
                                }
                            }

                            int errCnt = errorsAr.size();
                            for (int k = 0; k < errCnt; k++) {
                                Element error = doc.createElement("error");
                                if(errorsAr.get(k) == JSONNull.getInstance())
                                    continue;
                                error.setAttribute("message", ((JSONObject)(errorsAr.get(k))).getString("message"));
                                testCase.appendChild(error);
                            }
                        }

                    }
                    testSuite.appendChild(testCase);
                    testSuite.setAttribute("errors", String.format("%1d", errorsTestSute));
                    testSuite.setAttribute("failures", String.format("%1d", failuresTestSute));
                    errorsTestSute = 0;
                    failuresTestSute = 0;
                }
                root.appendChild(testSuite);
            }
        }
        catch (Exception e){
            logger.println(e.getMessage());
            e.printStackTrace();
        }
        doc.appendChild(root);
        String xml = getStringFromDocument(doc);
        return xml;
    }
    String getStringFromDocument(Document doc)
    {
        try
        {
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);
            return writer.toString();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            return null;
        }

    }

     static   class SrfTrustManager extends X509ExtendedTrustManager implements X509TrustManager {

            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s, SSLEngine engine) throws CertificateException {

            }
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s, Socket socket) throws CertificateException {

            }
            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s, SSLEngine engine) throws CertificateException {
            }
            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s, Socket socket) throws CertificateException {
            }
            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            }
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }

        @Extension
        // This indicates to Jenkins that this is an implementation of an extension
        // point.
        public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
            private String srfTestId;
            private String srfTagNames;
            private Object srfTestArgs;
            private String srfTunnelName;
            private boolean srfCloseTunnel;
                public DescriptorImpl() {
                    load();
                }

            @Override
            public boolean isApplicable(Class<? extends AbstractProject> aClass) {
                    return true;
                }

            @Override
            public String getDisplayName() {
                return "Execute tests by SRF";
            }

            public boolean hasSrfServers() {
                return Hudson.getInstance().getDescriptorByType(
                        SrfServerSettingsBuilder.SrfDescriptorImpl.class).hasSrfServers();
            }

            public SrfServerSettingsModel[] getSrfServers() {
                return Hudson.getInstance().getDescriptorByType(
                        SrfServerSettingsBuilder.SrfDescriptorImpl.class).getInstallations();
            }

            public FormValidation doCheckSrfUserName(@QueryParameter String value) {
                if (StringUtils.isBlank(value)) {
                    return FormValidation.error("User name must be set");
                }

                return FormValidation.ok();
            }

            public FormValidation doCheckSrfTimeout(@QueryParameter String value) {

                if (StringUtils.isEmpty(value)) {
                    return FormValidation.ok();
                }

                String val1 = value.trim();

                if (val1.length() > 0 && val1.charAt(0) == '-')
                    val1 = val1.substring(1);

                if (!StringUtils.isNumeric(val1) && val1 != "") {
                    return FormValidation.error("Timeout name must be a number");
                }
                return FormValidation.ok();
            }

            public FormValidation doCheckSrfPassword(@QueryParameter String value) {
                // if (StringUtils.isBlank(value)) {
                // return FormValidation.error("Password must be set");
                // }

                return FormValidation.ok();
            }

            public FormValidation doCheckSrfDomain(@QueryParameter String value) {
                if (StringUtils.isBlank(value)) {
                    return FormValidation.error("Domain must be set");
                }

                return FormValidation.ok();
            }

            public FormValidation doCheckSrfProject(@QueryParameter String value) {
                if (StringUtils.isBlank(value)) {
                    return FormValidation.error("Project must be set");
                }

                return FormValidation.ok();
            }

            public FormValidation doCheckSrfTestSets(@QueryParameter String value) {
                if (StringUtils.isBlank(value)) {
                    return FormValidation.error("Testsets are missing");
                }

                return FormValidation.ok();
            }

        }

}
