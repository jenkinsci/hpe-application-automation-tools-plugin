package com.hpe.application.automation.tools.run;

import hudson.FilePath;
import hudson.tasks.junit.TestResult;
import org.junit.Test;
import org.junit.Before; 
import org.junit.After;

import java.io.File;

/** 
* LrScriptResultsParser Tester. 
* 
* @author <Authors name> 
* @since <pre>��� 22, 2017</pre> 
* @version 1.0 
*/ 
public class LrScriptResultsParserTest { 

@Before
public void before() throws Exception { 
} 

@After
public void after() throws Exception { 
} 

/** 
* 
* Method: parse(String scriptName) 
* 
*/ 
@Test
public void testParse() throws Exception {
    final String pathname = "C:\\Jenkins\\jobs\\TestRunLRScriptReg\\builds\\13\\WebHttpHtml1\\Results.xml";
    final String junitOutput = "C:\\Jenkins\\jobs\\TestRunLRScriptReg\\builds\\31\\WebHttpHtml1\\JunitResult.xml";

    FilePath resultFile = new FilePath(new File(pathname));
    FilePath junitFile = new FilePath(new File(junitOutput));

//    LrScriptResultsParser lrscriptresultparser = new LrScriptResultsParser();
//    lrscriptresultparser.parse(resultFile, junitFile);

    TestResult testResult = new TestResult();
    testResult.parse(new File(junitOutput));
    testResult.tally();


} 


} 
