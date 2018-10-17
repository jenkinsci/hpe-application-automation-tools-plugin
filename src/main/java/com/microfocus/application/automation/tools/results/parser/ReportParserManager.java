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

package com.microfocus.application.automation.tools.results.parser;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.microfocus.application.automation.tools.results.parser.antjunit.AntJUnitReportParserImpl;
import com.microfocus.application.automation.tools.results.parser.jenkinsjunit.JenkinsJUnitReportParserImpl;
import com.microfocus.application.automation.tools.results.parser.mavensurefire.MavenSureFireReportParserImpl;
import com.microfocus.application.automation.tools.results.parser.nunit.NUnitReportParserImpl;
import com.microfocus.application.automation.tools.results.parser.nunit3.NUnit3ReportParserImpl;
import com.microfocus.application.automation.tools.results.parser.testngxml.TestNGXmlReportParserImpl;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTestSet;
import com.microfocus.application.automation.tools.sse.sdk.Logger;
import hudson.FilePath;

public class ReportParserManager {
	
	private static ReportParserManager instance = new ReportParserManager();

	private List<ReportParser> parserList;
	private FilePath workspace;
	private Logger logger;

	private ReportParserManager() {}

	public static ReportParserManager getInstance(FilePath workspace, Logger logger) {
		if (instance.workspace == null) {
			instance.workspace = workspace;
		}
		if (instance.logger == null) {
			instance.logger = logger;
		}
		return instance;
	}

	public List<AlmTestSet> parseTestSets(String reportFilePath, String testingFramework, String testingTool) {
		init();
		List<AlmTestSet> testsets = null;
		for (ReportParser reportParser : parserList) {
			try {
				InputStream in = new FileInputStream(reportFilePath);
				testsets = reportParser.parseTestSets(in, testingFramework, testingTool);
				break;
			} catch (Exception e) {
				logger.log("Failed to parse file with: " + reportParser.getClass().getName());
			}
		}
		return testsets;
	}

	private void init() {
		if (parserList == null) {
			parserList = new ArrayList<ReportParser>();
		}

		if (parserList.isEmpty()) {
			parserList.add(new JenkinsJUnitReportParserImpl());
			parserList.add(new MavenSureFireReportParserImpl());
			parserList.add(new TestNGXmlReportParserImpl());
			parserList.add(new NUnit3ReportParserImpl(workspace));
			parserList.add(new NUnitReportParserImpl());
			parserList.add(new AntJUnitReportParserImpl());
		}
	}
}
