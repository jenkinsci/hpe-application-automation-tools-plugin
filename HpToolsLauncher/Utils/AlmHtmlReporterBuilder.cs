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
using System;
using System.Collections.Generic;
using System.Text;
using HpToolsLauncher.Properties;

namespace HpToolsLauncher.Utils.Alm
{
    // DTOs (Data Transfer Objects) to hold the report data and build the HTML report from a template
    public class TestRunResult
    {
        public string TestName { get; set; }
        public string Status { get; set; }
        public string Message { get; set; }
        public string AlmLink { get; set; }
    }

    public class TestSetResult
    {
        public string TestSetName { get; set; }
        public string TestSetPath { get; set; }
        public DateTime FinishedAt { get; set; }
        public List<TestRunResult> Tests { get; set; }
        public string CumulativeSummary { get; set; }
        public TestSetResult()
        {
            Tests = new List<TestRunResult>();
        }
    }

    public class ReportContext
    {
        public string ServerUrl { get; set; }
        public string Domain { get; set; }
        public string Project { get; set; }
        public string User { get; set; }
        public List<TestSetResult> TestSets { get; set; }
        public ReportContext()
        {
            TestSets = new List<TestSetResult>();
        }
    }

    public class AlmHtmlReportBuilder
    {
        private readonly string _template = Resources.AlmHtmlReport;

        public string BuildReport(ReportContext context)
        {
            StringBuilder containers = new StringBuilder();

            foreach (TestSetResult set in context.TestSets)
            {
                string containerHtml = BuildTestSetContainer(context, set);
                containers.AppendLine(containerHtml);
            }
            return _template.Replace("{{REPORT_CONTAINER}}", containers.ToString());
        }

        private string BuildTestSetContainer(ReportContext context, TestSetResult testSetResultsDTO)
        {
            string containerTemplate = string.Format(@"
                <div class=""report-container"">
                    <div class=""report-header"">
                        <h2>Execution Report</h2>
                        <p>Test set {0} finished at {1}</p>
                    </div>
                    <div class=""summary-block"">
                        <table class=""summary-table"" width=""100%"" style=""table-layout:fixed;"">
                            <tr>
                                <td class=""label"">Server:</td>
                                <td class=""value"">{2}</td>
                                <td class=""label"">Domain:</td>
                                <td class=""value"">{3}</td>
                            </tr>
                            <tr>
                                <td class=""label"">Project:</td>
                                <td class=""value"">{4}</td>
                                <td class=""label"">User:</td>
                                <td class=""value"">{5}</td>
                            </tr>
                            <tr>
                                <td class=""label"">Test Set:</td>
                                <td class=""value"">{0}</td>
                                <td class=""label"">ALM Path:</td>
                                <td class=""value"">{6}</td>
                            </tr>
                        </table>
                    </div>
                    <div class=""section-title"">Test Results</div>
                    <div style=""padding:0 36px 0px 0px;"">
                        <table class=""results-table"" width=""100%"" style=""table-layout:fixed;"">
                            <thead>
                                <tr>
                                    <th style=""width:18%;"">Test</th>
                                    <th style=""width:12%;"">Status</th>
                                    <th style=""width:35%;"">Message</th>
                                    <th style=""width:35%;"">ALM Link</th>
                                </tr>
                            </thead>
                            <tbody>
                                [[TEST_ROWS]]
                            </tbody>
                        </table>
                    </div>
                    <div class=""footer-note"">
                        [[CUMULATIVE_SUMMARY]]
                    </div>
                </div>
                <br/><br/>",
                testSetResultsDTO.TestSetName,
                testSetResultsDTO.FinishedAt.ToString("dd/MM/yyyy HH:mm:ss"),
                context.ServerUrl,
                context.Domain,
                context.Project,
                context.User,
                testSetResultsDTO.TestSetPath
            );

            string rows = BuildTestRows(testSetResultsDTO.Tests);

            return containerTemplate
                .Replace("{{REPORT_DATE}}", testSetResultsDTO.FinishedAt.ToString("dd/MM/yyyy HH:mm:ss"))
                .Replace("{{TEST_SET_NAME}}", testSetResultsDTO.TestSetName)
                .Replace("{{SERVER_URL}}", context.ServerUrl)
                .Replace("{{DOMAIN}}", context.Domain)
                .Replace("{{PROJECT}}", context.Project)
                .Replace("{{USER}}", context.User)
                .Replace("{{ALM_PATH}}", testSetResultsDTO.TestSetPath)
                .Replace("[[TEST_ROWS]]", rows)
                .Replace("[[CUMULATIVE_SUMMARY]]", testSetResultsDTO.CumulativeSummary ?? "");
        }

        private string BuildTestRows(List<TestRunResult> tests)
        {
            StringBuilder sb = new StringBuilder();

            foreach (TestRunResult test in tests)
            {
                string statusClass;
                switch (test.Status)
                {
                    case "Passed":
                        statusClass = "status-passed";
                        break;
                    case "Failed":
                        statusClass = "status-failed";
                        break;
                    case "Warning":
                        statusClass = "status-warning";
                        break;
                    default:
                        statusClass = "";
                        break;
                }
                sb.AppendLine(string.Format(@"
                <tr>
                    <td>{0}</td>
                    <td class=""{1}"">{2}</td>
                    <td>{3}</td>
                    <td><a href=""{4}"">Open in ALM</a></td>
                </tr>",
                test.TestName,
                statusClass,
                test.Status,
                test.Message,
                test.AlmLink));
            }

            return sb.ToString();
        }
    }
}
