/**
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
 using HpToolsLauncher.Utils;
using System.Globalization;
using System.IO;
using System.Xml;
using System.Xml.Serialization;

namespace HpToolsLauncher
{
    public class JunitXmlBuilder : IXmlBuilder
    {
        private string _xmlName = "APIResults.xml";

        public string XmlName
        {
            get { return _xmlName; }
            set { _xmlName = value; }
        }

        public const string ClassName = "HPToolsFileSystemRunner";
        public const string RootName = "uftRunnerRoot";
        private const string ALL_TESTS_FORMAT = "All-Tests.{0}";
        private const string DOT = ".";
        private const string UNDERSCORE = "_";
        private const string PASS = "pass";
        private const string FAIL = "fail";
        private const string ERROR = "error";
        private const string WARNING = "warning";

        private readonly XmlSerializer _serializer = new XmlSerializer(typeof(testsuites));
        private readonly testsuites _testSuites = new testsuites();

        public JunitXmlBuilder()
        {
            _testSuites.name = RootName;
        }

        public testsuites TestSuites
        {
            get { return _testSuites; }
        }

        /// <summary>
        /// converts all data from the test results in to the Junit xml format and writes the xml file to disk.
        /// </summary>
        /// <param name="results"></param>
        public void CreateXmlFromRunResults(TestSuiteRunResults results)
        {
            testsuite uftts = new testsuite
            {
                errors = results.NumErrors,
                tests = results.NumTests,
                failures = results.NumFailures,
                name = results.SuiteName,
                package = ClassName
            };
            foreach (TestRunResults testRes in results.TestRuns)
            {
                if (testRes.TestType == TestType.LoadRunner)
                {
                    testsuite lrts = CreateXmlFromLRRunResults(testRes);
                    _testSuites.AddTestsuite(lrts);
                }
                else
                {
                    testcase ufttc = CovertUFTRunResultsToTestcase(testRes);
                    uftts.AddTestCase(ufttc);
                }
            }
            if (uftts.testcase.Length > 0)
            {
                _testSuites.AddTestsuite(uftts);
            }

            if (File.Exists(XmlName))
            {
                File.Delete(XmlName);
            }

            using (Stream s = File.OpenWrite(XmlName))
            {
                _serializer.Serialize(s, _testSuites);
            }
        }

        /// <summary>
        /// Create or update the xml report. This function is called in a loop after each test execution in order to get the report built progressively
        /// If the job is aborted by user we still can provide the (partial) report with completed tests results.
        /// </summary>
        /// <param name="ts">reference to testsuite object, existing or going to be added to _testSuites collection</param>
        /// <param name="testRes">test run results to be converted</param>
        /// <param name="addToTestSuites">flag to indicate if the first param testsuite must be added to the collection</param>
        public void CreateOrUpdatePartialXmlReport(testsuite ts, TestRunResults testRes, bool addToTestSuites)
        {
            testcase tc = CovertUFTRunResultsToTestcase(testRes);
            ts.AddTestCase(tc);
            if (addToTestSuites)
            {
                _testSuites.AddTestsuite(ts);
            }

            // NOTE: if the file already exists it will be overwritten / replaced, the entire _testSuites will be serialized every time
            using (Stream s = File.Open(_xmlName, FileMode.Create, FileAccess.Write, FileShare.Read))
            {
                _serializer.Serialize(s, _testSuites);
            }
        }

        private testsuite CreateXmlFromLRRunResults(TestRunResults testRes)
        {
            testsuite lrts = new testsuite();
            int totalTests = 0, totalFailures = 0, totalErrors = 0;

            string resultFileFullPath = testRes.ReportLocation + "\\SLA.xml";
            if (File.Exists(resultFileFullPath))
            {
                try
                {
                    XmlDocument xdoc = new XmlDocument();
                    xdoc.Load(resultFileFullPath);

                    foreach (XmlNode childNode in xdoc.DocumentElement.ChildNodes)
                    {
                        if (childNode.Attributes != null && childNode.Attributes["FullName"] != null)
                        {
                            testRes.TestGroup = testRes.TestPath;
                            testcase lrtc = CovertUFTRunResultsToTestcase(testRes);
                            lrtc.name = childNode.Attributes["FullName"].Value;
                            if (childNode.InnerText.ToLowerInvariant().Contains("failed"))
                            {
                                lrtc.status = "fail";
                                totalFailures++;
                            }
                            else if (childNode.InnerText.ToLowerInvariant().Contains("passed"))
                            {
                                lrtc.status = "pass";
                                lrtc.error = new error[] { };
                            }
                            totalErrors += lrtc.error.Length;
                            lrts.AddTestCase(lrtc);
                            totalTests++;
                        }
                    }
                }
                catch (XmlException)
                {

                }
            }

            lrts.name = testRes.TestPath;
            lrts.tests = totalTests;
            lrts.errors = totalErrors;
            lrts.failures = totalFailures;
            lrts.time = testRes.Runtime.TotalSeconds.ToString(CultureInfo.InvariantCulture);
            return lrts;
        }

        private testcase CovertUFTRunResultsToTestcase(TestRunResults testRes)
        {
            testcase tc = new testcase
            {
                systemout = testRes.ConsoleOut,
                systemerr = testRes.ConsoleErr,
                report = testRes.ReportLocation,
                classname = string.Format(ALL_TESTS_FORMAT, testRes.TestGroup == null ? string.Empty : testRes.TestGroup.Replace(DOT, UNDERSCORE)),
                name = testRes.TestPath,
                type = testRes.TestType.ToString(),
                time = testRes.Runtime.TotalSeconds.ToString(CultureInfo.InvariantCulture)
            };

            if (!string.IsNullOrWhiteSpace(testRes.FailureDesc))
                tc.AddFailure(new failure { message = testRes.FailureDesc });

            switch (testRes.TestState)
            {
                case TestState.Passed:
                    tc.status = PASS;
                    break;
                case TestState.Failed:
                    tc.status = FAIL;
                    break;
                case TestState.Error:
                    tc.status = ERROR;
                    break;
                case TestState.Warning:
                    tc.status = WARNING;
                    break;
                default:
                    tc.status = PASS;
                    break;
            }
            if (!string.IsNullOrWhiteSpace(testRes.ErrorDesc))
                tc.AddError(new error { message = testRes.ErrorDesc });
            return tc;
        }
    }
}
