package com.hpe.application.automation.tools.results;

import com.hpe.application.automation.tools.common.RuntimeUtils;
import com.hpe.application.automation.tools.model.EnumDescription;
import com.hpe.application.automation.tools.model.ResultsPublisherModel;
import com.hpe.application.automation.tools.run.RunFromSrfBuilder;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.model.*;
import hudson.remoting.VirtualChannel;
import hudson.tasks.*;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.TestObject;
import hudson.tasks.test.TestResultProjectAction;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.tools.ant.DirectoryScanner;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.MetaClassLoader;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

/**
 * This class is adapted from {@link JunitResultArchiver}; Only the {@code perform()} method
 * slightly differs.
 *
 * @author Thomas Maurel
 */
public class SrfResultsReport extends Recorder implements Serializable {

    private static final long serialVersionUID = 1L;
    public  static Hashtable<String, SrfTestResultAction> myHash = new Hashtable<String, SrfTestResultAction>();

    @DataBoundConstructor
    public SrfResultsReport() {
        if(myHash == null)
            myHash = new Hashtable<String, SrfTestResultAction>();

    }

    public class SrfTestResultAction extends TestResultAction {
        private AbstractBuild<?, ?> _build;
        private JSONArray _buildInfo;
        private int _idx;
        private PrintStream _logger;
        private TestObject _target;
        private TestResult _result;
      public String getId(){
          return _target.getId();
      }
    @Override
        public  TestResult getResult(){
            Properties props = System.getProperties();
            props.setProperty("stapler.trace", "true");
            props.setProperty("stapler.resourcePath", "");
            URL[] urls = {this.getClass().getProtectionDomain().getCodeSource().getLocation()};
            try {
              MetaClassLoader.debugLoader = new MetaClassLoader(new SrfClassLoader(urls, null));
            }catch (Exception e){}
            _result =super.getResult();
            return _result;
        };
        public Object getWrappedTarget(){
            return _target;
        }
        private void getBuildInfo(CaseResult p){
            String data = null;
            BufferedReader reader = null;
            try {
                String path = p.getRun().getRootDir().getPath().concat("/report.json");
                reader = new BufferedReader(new FileReader(path));
                String line = null;
                StringBuffer buf = new StringBuffer();
                while ( (line = reader.readLine() ) != null){
                    buf.append(line);
                }
                data = buf.toString();
            }
            catch (Exception e) {
            }
            finally {
                try {
                    if(reader != null)
                        reader.close();
                } catch (IOException e) {

                }
            }
            _buildInfo = JSONArray.fromObject(data);
        }
        public SrfTestResultAction(AbstractBuild owner, TestResult result, BuildListener listener) {
           super(owner, result, listener);
            String data = null;
            if(listener != null)
             _logger = listener.getLogger();
            _result=result;
            BufferedReader reader = null;
              try {
                    String path = _build.getRootDir().getPath().concat("/report.json");
                    reader = new BufferedReader(new FileReader(path));
                    String line = null;
                    StringBuffer buf = new StringBuffer();
                    while ( (line = reader.readLine() ) != null){
                        buf.append(line);
                    }
                    data = buf.toString();
              }
            catch (Exception e) {
            }
            finally {
                  try {
                      if(reader != null)
                        reader.close();
                  } catch (IOException e) {

                  }
              }
            _buildInfo = JSONArray.fromObject(data);
        }
        public void startLoop(){
            _idx = 0;
        }
        public  String getDeepLink(Object p){
            String testName = "";
            int idx = ++_idx;
            testName = ((CaseResult) p).getClassName().toLowerCase();
            getBuildInfo((CaseResult)p);
            int cnt = _buildInfo.size();
            String srfUrl = "";
            for (int i = 0; i < cnt; i++) {
                JSONObject jTest = _buildInfo.getJSONObject(i);
                String name = jTest.getString("name").toLowerCase();
                if(name.compareTo(testName) != 0)
                    continue;

                JSONObject jo = (JSONObject)_buildInfo.get(0);
                String y1 = jo.getString("yac");
                JSONArray runs = jo.getJSONArray("scriptRuns");
                int len = runs.size();
                JSONObject run = runs.getJSONObject(len - idx );
                String y2 = run.getString("yac");
                srfUrl = String.format("%1s/results/%1s/details/compare?script-runs=%1s", getSrfServer(owner), y1, y2);
                String tenant = jTest.getString("tenantid");
                srfUrl = srfUrl.concat("&TENANTID=").concat(tenant);
            }
            _logger.println(srfUrl);
            return srfUrl;
        }
        public String getEnvString(Object p){
            String testName = "";
            int idx = _idx + 1;
            try{
                testName = p.getClass().getMethod("getClassName").invoke(p).toString().toLowerCase();
            }
            catch (NoSuchMethodException e){
                return "NoSuchMethodException";
            }
            catch (IllegalAccessException e){
                return "IllegalAccessException";
            }
            catch(InvocationTargetException e){
                return "InvocationTargetException";
            }
            catch (Exception e){
                return e.getMessage();
            }
            int cnt = _buildInfo.size();
            for (int i = 0; i < cnt; i++) {
                JSONObject jTest = _buildInfo.getJSONObject(i);
                String name = jTest.getString("name").toLowerCase();
                if(name.compareTo(testName) != 0)
                    continue;
                JSONArray scriptRuns = jTest.getJSONArray("scriptRuns");
                JSONObject scriptRun = scriptRuns.getJSONObject(idx - 1);
                JSONObject env = scriptRun.getJSONObject("environment");
                JSONObject os = env.getJSONObject("os");
                JSONObject browser = env.getJSONObject("browser");
                String envString = String.format("%1s %1s %1s %1s", os.getString("name"), os.getString("version"), browser.getString("name"), browser.getString("version"));
                return envString;
            }
            return _buildInfo.size() + "";
        }
        private void setTarget(TestObject t, Hashtable<String, SrfTestResultAction>  h){
            _target = t;
            myHash.putAll(h);
        }
    }

    private String getSrfServer(AbstractBuild<?, ?> build){
        String ftaasServerAddress = "";
        try {
            String path = build.getProject().getParent().getRootDir().toString();
            path = path.concat("/com.hpe.application.automation.tools.settings.SrfServerSettingsBuilder.xml");
            File file = new File(path);
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(file);
            ftaasServerAddress = document.getElementsByTagName("srfServerName").item(0).getTextContent();
            if( !ftaasServerAddress.startsWith("http://") && !ftaasServerAddress.startsWith("https://"))
            {
                String tmp = ftaasServerAddress;
                ftaasServerAddress="https://";
                ftaasServerAddress = ftaasServerAddress.concat(tmp);
            }
            if(ftaasServerAddress.startsWith("http://") && !ftaasServerAddress.substring(6).contains(":") ){
                    ftaasServerAddress = ftaasServerAddress.concat(":8080");
            }
            else if(ftaasServerAddress.startsWith("https://")) {
                if (ftaasServerAddress.substring(7).contains(":") == false)
                    ftaasServerAddress = ftaasServerAddress.concat(":443");
            }
            String srfProxy =  document.getElementsByTagName("srfProxyName").item(0).getTextContent();
            if((srfProxy != null) && (srfProxy.length() != 0)) {
                String[] res = srfProxy.split(":", 2);
                Properties systemProperties = System.getProperties();
                String proxy = res[0];
                systemProperties.setProperty("http.proxyHost", proxy);
                if(res.length == 2) {
                    String port = res[1];
                    systemProperties.setProperty("http.proxyPort", port);
                }
            }
        }
        catch (ParserConfigurationException e){
        }
        catch (SAXException e){
        }
        catch (IOException e){
        }
        return ftaasServerAddress;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        final List<String> mergedResultNames = new ArrayList<String>();

//        TestNameTransformer.all().add(new TestNameTransformer()  );
        SrfTestResultAction action;
        Project<?, ?> project = RuntimeUtils.cast(build.getProject());
        List<Builder> builders = project.getBuilders();
        int cnt = builders.size();
        for (int i = 0; i <cnt ; i++) {
            if(builders.get(i) instanceof RunFromSrfBuilder) {
                mergedResultNames.add(String.format("report%1d.xml", build.number));
            }

        }



        // Has any QualityCenter builder been set up?
        if (mergedResultNames.isEmpty()) {
            listener.getLogger().println("RunResultRecorder: no results xml File provided");
            return true;
        }

        TestResult result = null;
        try {
            final long buildTime = build.getTimestamp().getTimeInMillis();
            final long nowMaster = System.currentTimeMillis();

            result = build.getWorkspace().act(new FileCallable<TestResult>() {

                @Override
                public void checkRoles( RoleChecker roleChecker ) throws SecurityException {

                }

                private static final long serialVersionUID = 1L;

                @Override
                public TestResult invoke(File ws, VirtualChannel channel) throws IOException {
                    final long nowSlave = System.currentTimeMillis();
                    List<String> files = new ArrayList<String>();
                    DirectoryScanner ds = new DirectoryScanner();
                    ds.setBasedir(ws);

                    // Transform the report file names list to a
                    // File
                    // Array,
                    // and add it to the DirectoryScanner includes
                    // set
                    for (String name : mergedResultNames) {
                        File file = new File(ws, name);
                        if (file.exists()) {
                            files.add(file.getName());
                        }
                    }

                    Object[] objectArray = new String[files.size()];
                    files.toArray(objectArray);
                    ds.setIncludes((String[]) objectArray);
                    ds.scan();
                    if (ds.getIncludedFilesCount() == 0) {
                        // no test result. Most likely a
                        // configuration
                        // error or
                        // fatal problem
                        throw new AbortException("Report not found");
                    }

                    return new TestResult(buildTime + (nowSlave - nowMaster), ds, true);
                }

  /*              @Override
                public void checkRoles(RoleChecker arg0) throws SecurityException {
                    // TODO Auto-generated method stub

                } */
            });
            String str = result.getUrl();
            action = new SrfTestResultAction(build, result, listener);
            if (result.getPassCount() == 0 && result.getFailCount() == 0) {
                throw new AbortException("Result is empty");
            }
        } catch (AbortException e) {
            if (build.getResult() == Result.FAILURE) {
                // most likely a build failed before it gets to the test
                // phase.
                // don't report confusing error message.
                return true;
            }

            listener.getLogger().println(e.getMessage());
            build.setResult(Result.FAILURE);
            return true;
        } catch (IOException e) {
            e.printStackTrace(listener.error("Failed to archive testing tool reports"));
            build.setResult(Result.FAILURE);
            return true;
        }

        build.getActions().add(action);

      /*  try {
            archiveTestsReport(build, listener, fileSystemResultNames, result);
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        File artifactsDir = build.getArtifactsDir();
        if (artifactsDir.exists()) {
            File reportDirectory = new File(artifactsDir.getParent(), PERFORMANCE_REPORT_FOLDER);
            if (reportDirectory.exists()) {
                File htmlIndexFile = new File(reportDirectory, INDEX_HTML_NAME);
                if (htmlIndexFile.exists())
                    build.getActions().add(new PerformanceReportAction(build));
            }

            File summaryDirectory = new File(artifactsDir.getParent(), TRANSACTION_SUMMARY_FOLDER);
            if (summaryDirectory.exists()) {
                File htmlIndexFile = new File(summaryDirectory, INDEX_HTML_NAME);
                if (htmlIndexFile.exists())
                    build.getActions().add(new TransactionSummaryAction(build));
            }
        }*/


        // get the artifacts directory where we will upload the zipped report
        // folder

        // read each result.xml
        /*
         * The structure of the result file is: <testsuites> <testsuite>
         * <testcase.........report="path-to-report"/>
         * <testcase.........report="path-to-report"/>
         * <testcase.........report="path-to-report"/>
         * <testcase.........report="path-to-report"/> </testsuite>
         * </testsuites>
         */













                
        return true;
    }















    private void write2XML(Document document,String filename)
    {
        PrintWriter pw = null;
        try {
            document.normalize();

            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            DOMSource source = new DOMSource(document);
            pw = new PrintWriter(new FileOutputStream(filename));
            StreamResult result = new StreamResult(pw);
            transformer.transform(source, result);
        } catch (Exception e) {
            ;
        }
        finally {
            if(pw != null)
                pw.close();
        }

    }

    /*
     * if we have a directory with file name "file.zip" we will return
     * "file_1.zip";
     */





    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {

        return new TestResultProjectAction(project);
    }



    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    public ResultsPublisherModel getResultsPublisherModel() {

        return new ResultsPublisherModel("");
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public static  DescriptorImpl _inst;
        public DescriptorImpl() {
            String s=this.getDescriptorUrl();
            load();
            _inst = this;
        }

        @Override
        public String getDisplayName() {

            return "Publish HPE SRF Test Results";
        }

        @Override
        public boolean isApplicable(
                @SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {

            return true;
        }

        public List<EnumDescription> getReportArchiveModes() {

            return ResultsPublisherModel.archiveModes;
        }
    }

        //"Act*;Icons;Resources;CountersMonitorResults.txt;*.xls;GeneralInfo.ini;InstallNewReport.html;Results.qtp;Results.xml";

                }


class SrfClassLoader extends URLClassLoader{
    URLClassLoader _parent;
    public SrfClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        _parent = (URLClassLoader)parent;
                    }
    @Override
    public URL findResource(final String name){
        try {
             URL url = super.findResource(name);
            return url;
        }catch(Exception e){
            return null;
        }

    }

}
