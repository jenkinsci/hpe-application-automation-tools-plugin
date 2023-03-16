/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2023 Micro Focus or one of its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * ___________________________________________________________________
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
                if (testRes.TestType == TestType.LoadRunner.ToString())
                {
                    testsuite lrts = CreateXmlFromLRRunResults(testRes);
                    _testSuites.AddTestsuite(lrts);
                }
                else
                {
                    testcase ufttc = ConvertUFTRunResultsToTestcase(testRes);
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
            testcase tc = ConvertUFTRunResultsToTestcase(testRes);
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
                            testcase lrtc = ConvertUFTRunResultsToTestcase(testRes);
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

        private testcase ConvertUFTRunResultsToTestcase(TestRunResults testRes)
        {
            testcase tc = new testcase
            {
                systemout = testRes.ConsoleOut,
                systemerr = testRes.ConsoleErr,
                report = testRes.ReportLocation,
                classname = string.Format(ALL_TESTS_FORMAT, testRes.TestGroup == null ? string.Empty : testRes.TestGroup.Replace(DOT, UNDERSCORE)),
                name = testRes.TestPath,
                type = testRes.TestType,
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
