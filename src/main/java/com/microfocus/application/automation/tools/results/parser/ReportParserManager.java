/*
 *  Certain versions of software accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 *  This software was acquired by Micro Focus on September 1, 2017, and is now
 *  offered by OpenText.
 *  Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical
 *  in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the
 *  property of their respective owners.
 *  OpenText is a trademark of Open Text.
 *  __________________________________________________________________
 *  MIT License
 *
 *  Copyright 2012-2025 Open Text.
 *
 *  The only warranties for products and services of Open Text and
 *  its affiliates and licensors ("Open Text") are as may be set forth
 *  in the express warranty statements accompanying such products and services.
 *  Nothing herein should be construed as constituting an additional warranty.
 *  Open Text shall not be liable for technical or editorial errors or
 *  omissions contained herein. The information contained herein is subject
 *  to change without notice.
 *
 *  Except as specifically indicated otherwise, this document contains
 *  confidential information and a valid license is required for possession,
 *  use or copying. If this work is provided to the U.S. Government,
 *  consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 *  Computer Software Documentation, and Technical Data for Commercial Items are
 *  licensed to the U.S. Government under vendor's standard commercial license.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ___________________________________________________________________
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
