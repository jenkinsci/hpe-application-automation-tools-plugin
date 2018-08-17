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

package com.microfocus.application.automation.tools.run;

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
