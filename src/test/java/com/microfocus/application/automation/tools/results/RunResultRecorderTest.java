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

package com.microfocus.application.automation.tools.results;

import com.microfocus.application.automation.tools.results.projectparser.performance.JobLrScenarioResult;
import com.microfocus.application.automation.tools.results.projectparser.performance.LrJobResults;
import com.microfocus.application.automation.tools.model.ResultsPublisherModel;
import hudson.FilePath;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/** 
* RunResultRecorder Tester. 
* 
* @author <Authors name> 
* @since <pre>??? 17, 2016</pre> 
* @version 1.0
 */
@SuppressWarnings("squid:S2699")
public class RunResultRecorderTest { 

@Before
public void before() throws Exception { 
} 

@After
public void after() throws Exception { 
} 

/** 
* 
* Method: getDescriptor() 
* 
*/ 
@Test
public void testGetDescriptor() throws Exception { 
//TODO: Test goes here... 
} 

/** 
* 
* Method: perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) 
* 
*/ 
@Test
public void testPerform() throws Exception { 
//TODO: Test goes here... 
} 

/** 
* 
* Method: getProjectAction(AbstractProject<?, ?> project) 
* 
*/ 
@Test
public void testGetProjectAction() throws Exception { 
//TODO: Test goes here... 
} 

/** 
* 
* Method: getProjectActions(AbstractProject<?, ?> project) 
* 
*/ 
@Test
public void testGetProjectActions() throws Exception { 
//TODO: Test goes here... 
} 

/** 
* 
* Method: createAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) 
* 
*/ 
@Test
public void testCreateAggregator() throws Exception { 
//TODO: Test goes here... 
} 

/** 
* 
* Method: getRequiredMonitorService() 
* 
*/ 
@Test
public void testGetRequiredMonitorService() throws Exception { 
//TODO: Test goes here... 
} 

/** 
* 
* Method: getResultsPublisherModel() 
* 
*/ 
@Test
public void testGetResultsPublisherModel() throws Exception { 
//TODO: Test goes here... 
} 

/** 
* 
* Method: getDisplayName() 
* 
*/ 
@Test
public void testGetDisplayName() throws Exception { 
//TODO: Test goes here... 
} 

/** 
* 
* Method: isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) 
* 
*/ 
@Test
public void testIsApplicable() throws Exception { 
//TODO: Test goes here... 
} 

/** 
* 
* Method: getReportArchiveModes() 
* 
*/ 
@Test
public void testGetReportArchiveModes() throws Exception { 
//TODO: Test goes here... 
} 

/** 
* 
* Method: accept(File file) 
* 
*/ 
@Test
public void testAccept() throws Exception { 
//TODO: Test goes here... 
} 

/** 
* 
* Method: sort(List<ExtensionComponent<Descriptor<Publisher>>> r) 
* 
*/ 
@Test
public void testSort() throws Exception { 
//TODO: Test goes here... 
} 

/** 
* 
* Method: compare(ExtensionComponent<Descriptor<Publisher>> lhs, ExtensionComponent<Descriptor<Publisher>> rhs) 
* 
*/ 
@Test
public void testCompare() throws Exception { 
//TODO: Test goes here... 
} 

/** 
* 
* Method: addNotifier(Descriptor<Publisher> d) 
* 
*/ 
@Test
public void testAddNotifier() throws Exception { 
//TODO: Test goes here... 
} 

/** 
* 
* Method: addRecorder(Descriptor<Publisher> d) 
* 
*/ 
@Test
public void testAddRecorder() throws Exception { 
//TODO: Test goes here... 
} 

/** 
* 
* Method: add(Descriptor<Publisher> d) 
* 
*/ 
@Test
public void testAddD() throws Exception { 
//TODO: Test goes here... 
} 

/** 
* 
* Method: add(int index, Descriptor<Publisher> d) 
* 
*/ 
@Test
public void testAddForIndexD() throws Exception { 
//TODO: Test goes here... 
} 

/** 
* 
* Method: get(int index) 
* 
*/ 
@Test
public void testGet() throws Exception { 
//TODO: Test goes here... 
} 

/** 
* 
* Method: size() 
* 
*/ 
@Test
public void testSize() throws Exception { 
//TODO: Test goes here... 
} 

/** 
* 
* Method: iterator() 
* 
*/ 
@Test
public void testIterator() throws Exception { 
//TODO: Test goes here... 
} 

/** 
* 
* Method: remove(Object o) 
* 
*/ 
@Test
public void testRemove() throws Exception { 
//TODO: Test goes here... 
} 


/** 
* 
* Method: writeReportMetaData2XML(List<ReportMetaData> htmlReportsInfo, String xmlFile) 
* 
*/ 
@Test
public void testWriteReportMetaData2XML() throws Exception { 
//TODO: Test goes here... 
/* 
try { 
   Method method = RunResultRecorder.getClass().getMethod("writeReportMetaData2XML", List<ReportMetaData>.class, String.class); 
   method.setAccessible(true); 
   method.invoke(<Object>, <Parameters>); 
} catch(NoSuchMethodException e) { 
} catch(IllegalAccessException e) { 
} catch(InvocationTargetException e) { 
} 
*/ 
} 

/** 
* 
* Method: collectAndPrepareHtmlReports(AbstractBuild build, BuildListener listener, List<ReportMetaData> htmlReportsInfo) 
* 
*/ 
@Test
public void testCollectAndPrepareHtmlReports() throws Exception { 
//TODO: Test goes here... 
/* 
try { 
   Method method = RunResultRecorder.getClass().getMethod("collectAndPrepareHtmlReports", AbstractBuild.class, BuildListener.class, List<ReportMetaData>.class); 
   method.setAccessible(true); 
   method.invoke(<Object>, <Parameters>); 
} catch(NoSuchMethodException e) { 
} catch(IllegalAccessException e) { 
} catch(InvocationTargetException e) { 
} 
*/ 
} 

/** 
* 
* Method: archiveTestsReport(AbstractBuild<?, ?> build, BuildListener listener, List<String> resultFiles, TestResult testResult) 
* 
*/ 
@Test
public void testArchiveTestsReport() throws Exception { 
//TODO: Test goes here... 
/* 
try { 
   Method method = RunResultRecorder.getClass().getMethod("archiveTestsReport", AbstractBuild<?,.class, BuildListener.class, List<String>.class, TestResult.class); 
   method.setAccessible(true); 
   method.invoke(<Object>, <Parameters>); 
} catch(NoSuchMethodException e) { 
} catch(IllegalAccessException e) { 
} catch(InvocationTargetException e) { 
} 
*/ 
} 

/** 
* 
* Method: copyRunReport(FilePath reportFolder, String testFolderPath, File buildDir, String scenerioName) 
* 
*/ 
@Test
public void testCopyRunReport() throws Exception { 
//TODO: Test goes here... 
/* 
try { 
   Method method = RunResultRecorder.getClass().getMethod("copyRunReport", FilePath.class, String.class, File.class, String.class); 
   method.setAccessible(true); 
   method.invoke(<Object>, <Parameters>); 
} catch(NoSuchMethodException e) { 
} catch(IllegalAccessException e) { 
} catch(InvocationTargetException e) { 
} 
*/ 
} 

/** 
* 
* Method: buildJobDataset(AbstractBuild<?, ?> build, BuildListener listener) 
* 
*/ 
@Test
public void testBuildJobDataset() throws Exception { 
//TODO: Test goes here... 
/* 
try { 
   Method method = RunResultRecorder.getClass().getMethod("buildJobDataset", AbstractBuild<?,.class, BuildListener.class); 
   method.setAccessible(true); 
   method.invoke(<Object>, <Parameters>); 
} catch(NoSuchMethodException e) { 
} catch(IllegalAccessException e) { 
} catch(InvocationTargetException e) { 
} 
*/ 
}


@Test
public void testCreatingJobDataSet()
{
    try {
    RunResultRecorder runResultRecorder = new RunResultRecorder(ResultsPublisherModel.CreateHtmlReportResults.getValue());
    Method parseScenarioResults = runResultRecorder.getClass().getDeclaredMethod("parseScenarioResults", FilePath.class);
    parseScenarioResults.setAccessible(true);

    ArrayList<FilePath> runReportList = new ArrayList<FilePath>(0);
    runReportList.add(new FilePath(new File(getClass().getResource("RunReport.xml").getPath())));
    runReportList.add(new FilePath(new File(getClass().getResource("RunReport_sc5.xml").getPath())));
        LrJobResults jobResults = new LrJobResults();

    // read each RunReport.xml
    for (FilePath reportFilePath : runReportList) {
        JobLrScenarioResult result = null;
        JobLrScenarioResult jobLrScenarioResult = (JobLrScenarioResult) parseScenarioResults.invoke(runResultRecorder,
                reportFilePath);
        jobResults.addScenario(jobLrScenarioResult);
    }
    System.out.println("");
    } catch(NoSuchMethodException e) {
        e.printStackTrace();
    } catch(IllegalAccessException e) {
        e.printStackTrace();

    } catch(InvocationTargetException e) {
        e.printStackTrace();
    }


}

/** 
* 
* Method: getJobLrScenarioResult(FilePath slaFilePath) 
* 
*/ 
@Test
public void testParseScenarioResults() throws Exception {

	RunResultRecorder runResultRecorder = new RunResultRecorder(ResultsPublisherModel.CreateHtmlReportResults.getValue());
	FilePath runReportPath = new FilePath(new File(getClass().getResource("RunReport.xml").getPath()));
    JobLrScenarioResult result = null;
    try {
	   	Method method = runResultRecorder.getClass().getDeclaredMethod("parseScenarioResults", FilePath.class);
		method.setAccessible(true);
        result = (JobLrScenarioResult) method.invoke(runResultRecorder, runReportPath);
    } catch(NoSuchMethodException e) {
		e.printStackTrace();
	} catch(IllegalAccessException e) {
		e.printStackTrace();

	} catch(InvocationTargetException e) {
		e.printStackTrace();

	}

    assertNotNull("RunResult parser result is empty", result);
    assertEquals(Integer.valueOf(344), result.vUserSum.get("Passed"));
    assertEquals(Integer.valueOf(364), result.vUserSum.get("Stopped"));
    assertEquals(Integer.valueOf(292), result.vUserSum.get("Failed"));
    assertEquals(Integer.valueOf(0), result.vUserSum.get("Error"));
    assertEquals(Integer.valueOf(1000), result.vUserSum.get("Count"));


} 

/** 
* 
* Method: processScenarioStats(JobLrScenarioResult jobLrScenarioResult, Document doc) 
* 
*/ 
@Test
public void testProcessScenarioStats() throws Exception { 
//TODO: Test goes here... 
/* 
try { 
   Method method = RunResultRecorder.getClass().getMethod("processScenarioStats", JobLrScenarioResult.class, Document.class); 
   method.setAccessible(true); 
   method.invoke(<Object>, <Parameters>); 
} catch(NoSuchMethodException e) { 
} catch(IllegalAccessException e) { 
} catch(InvocationTargetException e) { 
} 
*/ 
} 

/** 
* 
* Method: processSLA(JobLrScenarioResult jobLrScenarioResult, Document doc) 
* 
*/ 
@Test
public void testProcessSLA() throws Exception { 
//TODO: Test goes here... 
/* 
try { 
   Method method = RunResultRecorder.getClass().getMethod("processSLA", JobLrScenarioResult.class, Document.class); 
   method.setAccessible(true); 
   method.invoke(<Object>, <Parameters>); 
} catch(NoSuchMethodException e) { 
} catch(IllegalAccessException e) { 
} catch(InvocationTargetException e) { 
} 
*/ 
} 

/** 
* 
* Method: processSlaRule(JobLrScenarioResult jobLrScenarioResult, Element slaRuleElement, LrTest.SLA_GOAL slaGoal) 
* 
*/ 
@Test
public void testProcessSlaRule() throws Exception { 
//TODO: Test goes here... 
/* 
try { 
   Method method = RunResultRecorder.getClass().getMethod("processSlaRule", JobLrScenarioResult.class, Element.class, LrTest.SLA_GOAL.class); 
   method.setAccessible(true); 
   method.invoke(<Object>, <Parameters>); 
} catch(NoSuchMethodException e) { 
} catch(IllegalAccessException e) { 
} catch(InvocationTargetException e) { 
} 
*/ 
} 

/** 
* 
* Method: addTimeRanges(TimeRangeResult transactionTimeRange, Element slaRuleElement) 
* 
*/ 
@Test
public void testAddTimeRanges() throws Exception { 
//TODO: Test goes here... 
/* 
try { 
   Method method = RunResultRecorder.getClass().getMethod("addTimeRanges", TimeRangeResult.class, Element.class); 
   method.setAccessible(true); 
   method.invoke(<Object>, <Parameters>); 
} catch(NoSuchMethodException e) { 
} catch(IllegalAccessException e) { 
} catch(InvocationTargetException e) { 
} 
*/ 
} 

/** 
* 
* Method: archiveFolder(FilePath reportFolder, String testStatus, FilePath archivedFile, BuildListener listener) 
* 
*/ 
@Test
public void testArchiveFolder() throws Exception { 
//TODO: Test goes here... 
/* 
try { 
   Method method = RunResultRecorder.getClass().getMethod("archiveFolder", FilePath.class, String.class, FilePath.class, BuildListener.class); 
   method.setAccessible(true); 
   method.invoke(<Object>, <Parameters>); 
} catch(NoSuchMethodException e) { 
} catch(IllegalAccessException e) { 
} catch(InvocationTargetException e) { 
} 
*/ 
} 

/** 
* 
* Method: createHtmlReport(FilePath reportFolder, String testFolderPath, File artifactsDir, List<String> reportNames, TestResult testResult) 
* 
*/ 
@Test
public void testCreateHtmlReport() throws Exception { 
//TODO: Test goes here... 
/* 
try { 
   Method method = RunResultRecorder.getClass().getMethod("createHtmlReport", FilePath.class, String.class, File.class, List<String>.class, TestResult.class); 
   method.setAccessible(true); 
   method.invoke(<Object>, <Parameters>); 
} catch(NoSuchMethodException e) { 
} catch(IllegalAccessException e) { 
} catch(InvocationTargetException e) { 
} 
*/ 
} 

/** 
* 
* Method: createTransactionSummary(FilePath reportFolder, String testFolderPath, File artifactsDir, List<String> reportNames, TestResult testResult) 
* 
*/ 
@Test
public void testCreateTransactionSummary() throws Exception { 
//TODO: Test goes here... 
/* 
try { 
   Method method = RunResultRecorder.getClass().getMethod("createTransactionSummary", FilePath.class, String.class, File.class, List<String>.class, TestResult.class); 
   method.setAccessible(true); 
   method.invoke(<Object>, <Parameters>); 
} catch(NoSuchMethodException e) { 
} catch(IllegalAccessException e) { 
} catch(InvocationTargetException e) { 
} 
*/ 
} 

/** 
* 
* Method: outputReportFiles(List<String> reportNames, File reportDirectory, TestResult testResult, boolean tranSummary) 
* 
*/ 
@Test
public void testOutputReportFiles() throws Exception { 
//TODO: Test goes here... 
/* 
try { 
   Method method = RunResultRecorder.getClass().getMethod("outputReportFiles", List<String>.class, File.class, TestResult.class, boolean.class); 
   method.setAccessible(true); 
   method.invoke(<Object>, <Parameters>); 
} catch(NoSuchMethodException e) { 
} catch(IllegalAccessException e) { 
} catch(InvocationTargetException e) { 
} 
*/ 
} 

/** 
* 
* Method: write2XML(Document document, String filename) 
* 
*/ 
@Test
public void testWrite2XML() throws Exception { 
//TODO: Test goes here... 
/* 
try { 
   Method method = RunResultRecorder.getClass().getMethod("write2XML", Document.class, String.class); 
   method.setAccessible(true); 
   method.invoke(<Object>, <Parameters>); 
} catch(NoSuchMethodException e) { 
} catch(IllegalAccessException e) { 
} catch(InvocationTargetException e) { 
} 
*/ 
} 

/** 
* 
* Method: getUniqueZipFileNameInFolder(ArrayList<String> names, String fileName) 
* 
*/ 
@Test
public void testGetUniqueZipFileNameInFolder() throws Exception { 
//TODO: Test goes here... 
/* 
try { 
   Method method = RunResultRecorder.getClass().getMethod("getUniqueZipFileNameInFolder", ArrayList<String>.class, String.class); 
   method.setAccessible(true); 
   method.invoke(<Object>, <Parameters>); 
} catch(NoSuchMethodException e) { 
} catch(IllegalAccessException e) { 
} catch(InvocationTargetException e) { 
} 
*/ 
} 

/** 
* 
* Method: classify(Descriptor<Publisher> d) 
* 
*/ 
@Test
public void testClassify() throws Exception { 
//TODO: Test goes here... 
/* 
try { 
   Method method = RunResultRecorder.getClass().getMethod("classify", Descriptor<Publisher>.class); 
   method.setAccessible(true); 
   method.invoke(<Object>, <Parameters>); 
} catch(NoSuchMethodException e) { 
} catch(IllegalAccessException e) { 
} catch(InvocationTargetException e) { 
} 
*/ 
} 

} 
