/*
 * © Copyright 2013 EntIT Software LLC
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright (c) 2018 Micro Focus Company, L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
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
