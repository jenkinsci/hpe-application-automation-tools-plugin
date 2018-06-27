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

package com.microfocus.application.automation.tools.results.parser;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.microfocus.application.automation.tools.results.parser.antjunit.AntJUnitReportParserImpl;
import com.microfocus.application.automation.tools.results.parser.jenkinsjunit.JenkinsJUnitReportParserImpl;
import com.microfocus.application.automation.tools.results.parser.mavensurefire.MavenSureFireReportParserImpl;
import com.microfocus.application.automation.tools.results.parser.nunit.NUnitReportParserImpl;
import com.microfocus.application.automation.tools.results.parser.testngxml.TestNGXmlReportParserImpl;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTestSet;

public class ReportParserManager {
	
	private static ReportParserManager instance = new ReportParserManager();
	
	List<ReportParser> parserList = new ArrayList<ReportParser> ();
	
	private ReportParserManager() {
		parserList.add(new JenkinsJUnitReportParserImpl());
		parserList.add(new MavenSureFireReportParserImpl());
		parserList.add(new TestNGXmlReportParserImpl());
		parserList.add(new NUnitReportParserImpl());
		parserList.add(new AntJUnitReportParserImpl());
	}
	
	public static ReportParserManager getInstance() {
		return instance;
	}
	
	public List<AlmTestSet> parseTestSets(String reportFilePath, String testingFramework, String testingTool) {
		List<AlmTestSet> testsets = null;

		for(ReportParser reportParser : parserList) {
			try {
				InputStream in = new FileInputStream(reportFilePath);

				testsets = reportParser.parseTestSets(in, testingFramework, testingTool);
				break;
			} catch (Exception e) {
				
				e.printStackTrace();
				
			}
		}

		return testsets;
	}


}
