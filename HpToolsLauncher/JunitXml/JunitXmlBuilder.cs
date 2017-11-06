// (c) Copyright 2012 Hewlett-Packard Development Company, L.P. 
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

using System.IO;
using System.Xml.Serialization;
using System.Xml;

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
        //public const string ClassName = "uftRunner";
        public const string ClassName = "HPToolsFileSystemRunner";
        public const string RootName = "uftRunnerRoot";

        XmlSerializer _serializer = new XmlSerializer(typeof(testsuites));

        testsuites _testSuites = new testsuites();


        public JunitXmlBuilder()
        {
            _testSuites.name = RootName;
        }

        /// <summary>
        /// converts all data from the test resutls in to the Junit xml format and writes the xml file to disk.
        /// </summary>
        /// <param name="results"></param>
        public void CreateXmlFromRunResults(TestSuiteRunResults results)
        {
            _testSuites = new testsuites();

            testsuite uftts = new testsuite
            {
                errors = results.NumErrors.ToString(),
                tests = results.NumTests.ToString(),
                failures = results.NumFailures.ToString(),
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
                    testcase ufttc = CreateXmlFromUFTRunResults(testRes);
                    uftts.AddTestCase(ufttc);
                }
            }
            if(uftts.testcase.Length > 0)
                _testSuites.AddTestsuite(uftts);

            if (File.Exists(XmlName))
                File.Delete(XmlName);

            using (Stream s = File.OpenWrite(XmlName))
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
                            testcase lrtc = CreateXmlFromUFTRunResults(testRes);
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
                catch (System.Xml.XmlException)
                {

                }
            }

            lrts.name = testRes.TestPath;
            lrts.tests = totalTests.ToString();
            lrts.errors = totalErrors.ToString();
            lrts.failures = totalFailures.ToString();
            lrts.time = testRes.Runtime.TotalSeconds.ToString();
            return lrts;
        }

        private testcase CreateXmlFromUFTRunResults(TestRunResults testRes)
        {
            string baseClassName = "GUI-Tests";
            string testName = testRes.TestName;

            if(string.IsNullOrEmpty(testName) && !string.IsNullOrEmpty(testRes.ReportLocation))
            {
                testName = Directory.GetParent(testRes.ReportLocation).Name;
            }

            testcase tc = new testcase
            {
                name = testName,
                systemout = testRes.ConsoleOut,
                systemerr = testRes.ConsoleErr,
                report = testRes.ReportLocation,
                classname = baseClassName + "." + ((testRes.TestGroup == null) ? "" : testRes.TestGroup.Replace(".", "_")),
                type = testRes.TestType,
                time = testRes.Runtime.TotalSeconds.ToString()
            };

            if (!string.IsNullOrWhiteSpace(testRes.FailureDesc))
                tc.AddFailure(new failure { message = testRes.FailureDesc });

            switch (testRes.TestState)
            {
                case TestState.Passed:
                    tc.status = "pass";
                    break;
                case TestState.Failed:
                    tc.status = "fail";
                    break;
                case TestState.Error:
                    tc.status = "error";
                    break;
                case TestState.Warning:
                    tc.status = "warning";
                    break;
                default:
                    tc.status = "pass";
                    break;
            }
            if (!string.IsNullOrWhiteSpace(testRes.ErrorDesc))
                tc.AddError(new error { message = testRes.ErrorDesc });
            return tc;
        }




    }
}
