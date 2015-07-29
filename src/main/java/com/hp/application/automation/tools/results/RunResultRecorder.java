// (c) Copyright 2012 Hewlett-Packard Development Company, L.P. 
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package com.hp.application.automation.tools.results;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.hp.application.automation.tools.common.Pair;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.model.*;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.TestResultAggregator;
import hudson.tasks.test.TestResultProjectAction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.tools.ant.DirectoryScanner;
import org.kohsuke.stapler.DataBoundConstructor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.hp.application.automation.tools.common.RuntimeUtils;
import com.hp.application.automation.tools.model.EnumDescription;
import com.hp.application.automation.tools.model.ResultsPublisherModel;
import com.hp.application.automation.tools.run.RunFromAlmBuilder;
import com.hp.application.automation.tools.run.RunFromFileBuilder;
import com.hp.application.automation.tools.run.SseBuilder;
import com.hp.application.automation.tools.run.PcBuilder;

/**
 * This class is adapted from {@link JunitResultArchiver}; Only the {@code perform()} method
 * slightly differs.
 *
 * @author Thomas Maurel
 */
public class RunResultRecorder extends Recorder implements Serializable, MatrixAggregatable {

    private static final long serialVersionUID = 1L;
    private final ResultsPublisherModel _resultsPublisherModel;

    @DataBoundConstructor
    public RunResultRecorder(boolean publishResults, String archiveTestResultsMode) {

        _resultsPublisherModel = new ResultsPublisherModel(archiveTestResultsMode);
    }

    @Override
    public DescriptorImpl getDescriptor() {

        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {

        TestResultAction action;
        Project<?, ?> project = RuntimeUtils.cast(build.getProject());
        List<Builder> builders = project.getBuilders();

        final List<String> almResultNames = new ArrayList<String>();
        final List<String> fileSystemResultNames = new ArrayList<String>();
        final List<String> mergedResultNames = new ArrayList<String>();
        final List<String> almSSEResultNames = new ArrayList<String>();
        final List<String> pcResultNames = new ArrayList<String>();

        // Get the TestSet report files names of the current build
        for (Builder builder : builders) {
            if (builder instanceof RunFromAlmBuilder) {
                almResultNames.add(((RunFromAlmBuilder) builder).getRunResultsFileName());
            } else if (builder instanceof RunFromFileBuilder) {
                fileSystemResultNames.add(((RunFromFileBuilder) builder).getRunResultsFileName());
            } else if (builder instanceof SseBuilder) {
                String resultsFileName = ((SseBuilder) builder).getRunResultsFileName();
                if (resultsFileName != null)
                    almSSEResultNames.add(resultsFileName);
            } else if (builder instanceof PcBuilder) {
                String resultsFileName = ((PcBuilder) builder).getRunResultsFileName();
                if (resultsFileName != null)
                    pcResultNames.add(resultsFileName);
            }
        }

        mergedResultNames.addAll(almResultNames);
        mergedResultNames.addAll(fileSystemResultNames);
        mergedResultNames.addAll(almSSEResultNames);
        mergedResultNames.addAll(pcResultNames);

        // Has any QualityCenter builder been set up?
        if (mergedResultNames.isEmpty()) {
            listener.getLogger().println("RunResultRecorder: no results xml File provided");
            return true;
        }

        try {
            final long buildTime = build.getTimestamp().getTimeInMillis();
            final long nowMaster = System.currentTimeMillis();

            TestResult result = build.getWorkspace().act(new FileCallable<TestResult>() {

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
                    TestResult res = new TestResult();
                    return new TestResult(buildTime + (nowSlave - nowMaster), ds, true);
                }
            });

            action = new TestResultAction(build, result, listener);
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

        try {
            archiveTestsReport(build, listener, fileSystemResultNames);
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return true;
    }

    private Boolean collectAndPrepareHtmlReports(AbstractBuild build, BuildListener listener, List<ReportMetaData> htmlReportsInfo) throws IOException, InterruptedException {
        //Project<?, ?> project = RuntimeUtils.cast(build.getProject());
        //File reportDir = new File(build.getRootDir(), "UFTReport");
        File reportDir = new File(build.getArtifactsDir(), "UFTReport");

        FilePath rootTarget = new FilePath(reportDir);

        for (ReportMetaData htmlReportInfo : htmlReportsInfo) {

            //make sure it's a html report
            if(!htmlReportInfo.getIsHtmlReport())
            {
                continue;
            }
            String htmlReportDir = htmlReportInfo.getFolderPath(); //C:\UFTTest\GuiTest1\Report

            listener.getLogger().println("collectAndPrepareHtmlReports, collecting:" + htmlReportDir);

            //copy to the subdirs of master
            FilePath source = new FilePath(build.getWorkspace(), htmlReportDir);
            String testFullName = htmlReportInfo.getDisPlayName();  //like "C:\UFTTest\GuiTest1"
            File testFileFullName = new File(testFullName);
            String testName = testFileFullName.getName();  //like GuiTest1
            String dest = testName;
            FilePath targetPath = new FilePath(rootTarget, dest);  //target path is something like "C:\Program Files (x86)\Jenkins\jobs\testAction\builds\35\archive\UFTReport\GuiTest1"
            listener.getLogger().println("copying html report, source: " + source.getRemote() + " target: " + targetPath.getRemote());
            source.copyRecursiveTo(targetPath);

            //just test some url value
//            String buildurl = build.getUrl();  //like "job/testAction/46/"
//            listener.getLogger().println("build url is: " + buildurl);
//
//            String rootUrl = Hudson.getInstance().getRootUrl();  //http://localhost:8080/
//            listener.getLogger().println("root url is: " + rootUrl);
            //end -test some url value

            //fill in the urlName of this report. we need a network path not a FS path
            String resourceUrl = htmlReportInfo.getResourceURL();
            String urlName = resourceUrl + "/run_results.html"; //like artifact/UFTReport/GuiTest1/run_results.html

            listener.getLogger().println("set the report urlName to " + urlName);
            htmlReportInfo.setUrlName(urlName);

        }

        return true;
    }


    private void archiveTestsReport(
            AbstractBuild<?, ?> build,
            BuildListener listener,
            List<String> resultFiles) throws ParserConfigurationException, SAXException,
            IOException, InterruptedException {

        if ((resultFiles == null) || (resultFiles.size() == 0)) {
            return;
        }

        ArrayList<String> zipFileNames = new ArrayList<String>();
        ArrayList<FilePath> reportFolders = new ArrayList<FilePath>();

        listener.getLogger().println(
                "Report archiving mode is set to: "
                        + _resultsPublisherModel.getArchiveTestResultsMode());

        // if we dont want to archive any results
        /*if (resultsPublisherModel.getArchiveTestResultsMode().equals(
                ResultsPublisherModel.dontArchiveResults.getValue())) {
        	
        	deleteReportsFolder(reportFolders,listener);
        	return;
        }*/

        FilePath projectWS = build.getWorkspace();

        // get the artifacts directory where we will upload the zipped report
        // folder
        File artifactsDir = build.getArtifactsDir();
        artifactsDir.mkdirs();

        // read each result.xml
        /*
         * The structure of the result file is: <testsuites> <testsuite>
         * <testcase.........report="path-to-report"/>
         * <testcase.........report="path-to-report"/>
         * <testcase.........report="path-to-report"/>
         * <testcase.........report="path-to-report"/> </testsuite>
         * </testsuites>
         */

        for (String resultsFilePath : resultFiles) {
            FilePath resultsFile = projectWS.child(resultsFilePath);
            List<Pair<String, String>> htmlReportFoldersToCollect = new ArrayList<Pair<String, String>>();

            List<ReportMetaData> ReportInfoToCollect = new ArrayList<ReportMetaData>();

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            Document doc = dBuilder.parse(resultsFile.read());
            doc.getDocumentElement().normalize();

            Node testSuiteNode = doc.getElementsByTagName("testsuite").item(0);
            NodeList testCasesNodes = ((Element) testSuiteNode).getElementsByTagName("testcase");

            boolean reportIsHtml = false;
            
            for (int i = 0; i < testCasesNodes.getLength(); i++) {

                Node nNode = testCasesNodes.item(i);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {

                    Element eElement = (Element) nNode;

                    if (!eElement.hasAttribute("report")) {
                        continue;
                    }


                    String reportFolderPath = eElement.getAttribute("report"); //e.g. "C:\UFTTest\GuiTest1\Report"
                    String testFolderPath = eElement.getAttribute("name"); //e.g. "C:\UFTTest\GuiTest1"
                    String testStatus = eElement.getAttribute("status");  //e.g. "pass"


                    Node nodeSystemInfo = eElement.getElementsByTagName("system-out").item(0);
                    String sysinfo = nodeSystemInfo.getFirstChild().getNodeValue();
                    String testDateTime = sysinfo.substring(0,19); //like "21/07/2015 11:52:50";

                    FilePath reportFolder = new FilePath(projectWS.getChannel(), reportFolderPath);

                    reportFolders.add(reportFolder);  //no use at present.

                    String archiveTestResultMode =
                            _resultsPublisherModel.getArchiveTestResultsMode();
                    boolean archiveTestResult = false;
                    
                    //check for the new html report
                    FilePath htmlReport = new FilePath(reportFolder, "run_results.html");
                    if (htmlReport.exists()) {
                        reportIsHtml = true;
                        String htmlReportDir = reportFolder.getRemote();

                        //old way
                        //htmlReportFoldersToCollect.add(new Pair<String, String>(htmlReportDir, testFolderPath));

                        ReportMetaData reportMetaData = new ReportMetaData();
                        reportMetaData.setFolderPath(htmlReportDir);
                        reportMetaData.setDisPlayName(testFolderPath);
                        reportMetaData.setIsHtmlReport(true);
                        reportMetaData.setDateTime(testDateTime);
                        reportMetaData.setStatus(testStatus);

                        File testFileFullName = new File(testFolderPath);
                        String testName = testFileFullName.getName();
                        String resourceUrl = "artifact/UFTReport/" + testName;
                        reportMetaData.setResourceURL(resourceUrl);
                        //don't know reportMetaData's URL path yet, we will generate it later.
                        ReportInfoToCollect.add(reportMetaData);

                        listener.getLogger().println("add html report info to ReportInfoToCollect: " + "[date]" + testDateTime);
                    }

                    if (archiveTestResultMode.equals(ResultsPublisherModel.alwaysArchiveResults.getValue())) {
                        archiveTestResult = true;
                    } else if (archiveTestResultMode.equals(ResultsPublisherModel.ArchiveFailedTestsResults.getValue())) {
                        if (testStatus.equals("fail")) {
                            archiveTestResult = true;
                        } else if (archiveTestResultMode.equals(ResultsPublisherModel.dontArchiveResults.getValue())) {
                            archiveTestResult = false;
                        }
                    }
                    
                    
                    if (archiveTestResult && !reportIsHtml) {

                        if (reportFolder.exists()) {

                            FilePath testFolder =
                                    new FilePath(projectWS.getChannel(), testFolderPath);

                            String zipFileName =
                                    getUniqueZipFileNameInFolder(zipFileNames, testFolder.getName());
                            zipFileNames.add(zipFileName);

                            listener.getLogger().println(
                                    "Zipping report folder: " + reportFolderPath);

                            ByteArrayOutputStream outstr = new ByteArrayOutputStream();
                            reportFolder.zip(outstr);
                            
                            /*
                             * I did't use copyRecursiveTo or copyFrom due to
                             * bug in
                             * jekins:https://issues.jenkins-ci.org/browse
                             * /JENKINS-9189 //(which is cleaimed to have been
                             * fixed, but not. So I zip the folder to stream and
                             * copy it to the master.
                             */

                            ByteArrayInputStream instr =
                                    new ByteArrayInputStream(outstr.toByteArray());

                            FilePath archivedFile =
                                    new FilePath(new FilePath(artifactsDir), zipFileName);
                            archivedFile.copyFrom(instr);

                            outstr.close();
                            instr.close();
                            
                            //add to Report list
                            ReportMetaData reportMetaData = new ReportMetaData();
                            reportMetaData.setIsHtmlReport(false);
                            //reportMetaData.setFolderPath(htmlReportDir); //no need for RRV
                            reportMetaData.setDisPlayName(testFolderPath);
                            String zipFileUrlName = "artifact/" + zipFileName;
                            reportMetaData.setUrlName(zipFileUrlName);    //for RRV, the file url and resource url are the same.
                            reportMetaData.setResourceURL(zipFileUrlName);
                            reportMetaData.setDateTime(testDateTime);
                            reportMetaData.setStatus(testStatus);
                            ReportInfoToCollect.add(reportMetaData);

                        } else {
                            listener.getLogger().println(
                                    "No report folder was found in: " + reportFolderPath);
                        }
                    }
                }
            }

            if (reportIsHtml && !ReportInfoToCollect.isEmpty()){

                listener.getLogger().println("begin to collectAndPrepareHtmlReports");
                collectAndPrepareHtmlReports(build, listener, ReportInfoToCollect);
            }


            listener.getLogger().println("Adding a report action to the current build.");
            try {
                //just test
//                    Date dt = new Date();
//                    SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                    String dateTime = format1.format(dt);

                //HtmlBuildReportAction reportAction = new HtmlBuildReportAction(build, listener, dateTime);

                HtmlBuildReportAction reportAction = new HtmlBuildReportAction(build, listener, ReportInfoToCollect);
                //HtmlBuildReportAction reportAction = new HtmlBuildReportAction(build, listener);
//                final long buildTime = build.getTimestamp().getTimeInMillis();
//                DirectoryScanner ds = new DirectoryScanner();
//
//                List<String> files = new ArrayList<String>();
//                files.add(reportFile);
//
//                Object[] objectArray = new String[files.size()];
//                files.toArray(objectArray);
//                ds.setIncludes((String[]) objectArray);
//                ds.scan();
//                TestResult result = new TestResult(buildTime, ds, true);
//
//                TestResultAction reportAction = new TestResultAction(build, result, listener);
                build.getActions().add(reportAction);

            } catch (Exception ex) {
                listener.getLogger().println("a problem adding action: " + ex.toString());
            }

        }
    }


    /*
     * if we have a directory with file name "file.zip" we will return
     * "file_1.zip";
     */
    private String getUniqueZipFileNameInFolder(ArrayList<String> names, String fileName)
            throws IOException, InterruptedException {

        String result = fileName + "_Report.zip";

        int index = 0;

        while (names.indexOf(result) > -1) {
            result = fileName + "_" + (++index) + "_Report.zip";
        }

        return result;
    }

    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {

        return new TestResultProjectAction(project);
    }

    @Override
    public MatrixAggregator createAggregator(
            MatrixBuild build,
            Launcher launcher,
            BuildListener listener) {

        return new TestResultAggregator(build, launcher, listener);
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {

        return BuildStepMonitor.BUILD;
    }

    public ResultsPublisherModel getResultsPublisherModel() {

        return _resultsPublisherModel;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {

            load();
        }

        @Override
        public String getDisplayName() {

            return "Publish HP tests result";
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
}
