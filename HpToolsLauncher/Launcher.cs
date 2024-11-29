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
using HpToolsLauncher.Properties;
using HpToolsLauncher.RTS;
using HpToolsLauncher.TestRunners;
using HpToolsLauncher.Utils;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;

namespace HpToolsLauncher
{
    public enum CiName
    {
        Hudson,
        Jenkins,
        TFS,
        CCNET
    }

    public class Launcher
    {
        private IAssetRunner _runner;
        private IXmlBuilder _xmlBuilder;
        private bool _ciRun = false;
        private readonly JavaProperties _ciParams = new JavaProperties();
        private TestStorageType _runType;
        private static ExitCodeEnum _exitCode = ExitCodeEnum.Passed;
        private const string _dateFormat = "dd'/'MM'/'yyyy HH':'mm':'ss";
        private string _encoding;
        private const string PASSWORD = "Password";
        private const string RERUN_ALL_TESTS = "Rerun the entire set of tests";
        private const string RERUN_SPECIFIC_TESTS = "Rerun specific tests in the build";
        private const string RERUN_FAILED_TESTS = "Rerun only failed tests";
        private const string ONE = "1";
        private const string CLEANUP_TEST = "CleanupTest";

        public const string ClassName = "HPToolsFileSystemRunner";

        public static string DateFormat
        {
            get { return _dateFormat; }
        }

        /// <summary>
        /// if running an alm job theses strings are mandatory:
        /// </summary>
        private readonly string[] requiredParamsForQcRun = { "almServerUrl",
                                 "almUsername",
                                 "almPassword",
                                 "almDomain",
                                 "almProject",
                                 "almRunMode",
                                 "almTimeout",
                                 "almRunHost"};
        private readonly char[] _colon_semicolon = ",;".ToCharArray();

        /// <summary>
        /// a place to save the unique timestamp which shows up in properties/results/abort file names
        /// this timestamp per job run.
        /// </summary>
        public static string UniqueTimeStamp { get; set; }

        /// <summary>
        /// saves the exit code in case we want to run all tests but fail at the end since a file wasn't found
        /// </summary>
        public static ExitCodeEnum ExitCode
        {
            get { return _exitCode; }
            set { _exitCode = value; }
        }

        public enum ExitCodeEnum
        {
            Passed = 0,
            Failed = -1,
            Unstable = -2,
            Aborted = -3
        }


        /// <summary>
        /// constructor
        /// </summary>
        /// <param name="paramFileName"></param>
        /// <param name="runType"></param>
        public Launcher(string paramFileName, TestStorageType runType, string encoding = "UTF-8")
        {
            _runType = runType;
            if (paramFileName != null)
                _ciParams.Load(paramFileName);

            _encoding = encoding;
        }

        /// <summary>
        /// writes to console using the ConsolWriter class
        /// </summary>
        /// <param name="message"></param>
        private static void WriteToConsole(string message)
        {
            ConsoleWriter.WriteLine(message);
        }

        public void SafelyCancel()
        {
            if (_runner != null)
            {
                _runner.SafelyCancel();
            }
        }

        /// <summary>
        /// analyzes and runs the tests given in the param file.
        /// </summary>
        public void Run()
        {
            _ciRun = true;
            if (_runType == TestStorageType.Unknown)
                Enum.TryParse(_ciParams["runType"], true, out _runType);
            if (_runType == TestStorageType.Unknown)
            {
                WriteToConsole(Resources.LauncherNoRuntype);
                return;
            }

            if (!_ciParams.ContainsKey("resultsFilename"))
            {
                WriteToConsole(Resources.LauncherNoResFilenameFound);
                return;
            }
            string resultsFilename = _ciParams["resultsFilename"];

            UniqueTimeStamp = _ciParams.GetOrDefault("uniqueTimeStamp", resultsFilename.ToLower().Replace("results", string.Empty).Replace(".xml", string.Empty));

            //run the entire set of test once
            //create the runner according to type
            _runner = CreateRunner(true);

            //runner instantiation failed (no tests to run or other problem)
            if (_runner == null)
            {
                ConsoleWriter.WriteLine("empty runner;");
                Environment.Exit((int)ExitCodeEnum.Failed);
                return;
            }

            TestSuiteRunResults results = _runner.Run();

            string onCheckFailedTests = _ciParams.GetOrDefault("onCheckFailedTest");
            bool rerunTestsOnFailure = !string.IsNullOrEmpty(onCheckFailedTests) && Convert.ToBoolean(onCheckFailedTests.ToLower());
            if (_runType != TestStorageType.MBT)
            {
                RunSummary(resultsFilename, results);
            }

            if (_runType == TestStorageType.FileSystem)
            {
                //the "On failure" option is selected and the run build contains failed tests
                // we need to check if there were any failed tests
                bool thereAreFailedTests = _exitCode == ExitCodeEnum.Failed || results.NumFailures > 0;
                if (rerunTestsOnFailure && thereAreFailedTests)
                {
                    ConsoleWriter.WriteLine("There are failed tests.");

                    string fsTestType = _ciParams.GetOrDefault("testType");

                    //rerun the selected tests (either the entire set, just the selected tests or only the failed tests)
                    List<TestRunResults> runResults = results.TestRuns;
                    List<TestInfo> reruntests = new List<TestInfo>();
                    int index = 0;
                    foreach (var item in runResults)
                    {
                        if ((fsTestType == RERUN_ALL_TESTS) ||
                            (fsTestType == RERUN_FAILED_TESTS && (item.TestState == TestState.Failed || item.TestState == TestState.Error)))
                        {
                            index++;
                            reruntests.Add(new TestInfo(string.Format("FailedTest{0}", index), item.TestInfo));
                        }
                    }
                    if (fsTestType == RERUN_SPECIFIC_TESTS)
                    {
                        var specificTests = GetValidTests("FailedTest", Resources.LauncherNoFailedTestsFound, Resources.LauncherNoValidFailedTests, fsTestType);
                        reruntests = FileSystemTestsRunner.GetListOfTestInfo(specificTests);
                    }

                    //create the runner according to type
                    _runner = CreateRunner(false, reruntests, (RunnerBase)_runner);

                    //runner instantiation failed (no tests to run or other problem)
                    if (_runner == null)
                    {
                        Environment.Exit((int)ExitCodeEnum.Failed);
                        return;
                    }
                    TestSuiteRunResults rerunResults = _runner.Run();

                    RunSummary(resultsFilename, results, rerunResults);
                }
            }
            Environment.Exit((int)_exitCode);
        }

        /// <summary>
        /// creates the correct runner according to the given type
        /// </summary>
        /// <param name="isFirstRun"></param>
        private IAssetRunner CreateRunner(bool isFirstRun, List<TestInfo> reruntests = null, RunnerBase initialRunnerBase = null)
        {
            IAssetRunner runner = null;

            switch (_runType)
            {
                case TestStorageType.AlmLabManagement:

                case TestStorageType.Alm:
                { 
                    //check that all required parameters exist
                    foreach (string param1 in requiredParamsForQcRun)
                    {
                        if (!_ciParams.ContainsKey(param1))
                        {
                            ConsoleWriter.WriteLine(string.Format(Resources.LauncherParamRequired, param1));
                            return null;
                        }
                    }

                    //parse params that need parsing
                    double dblQcTimeout;
                    if (!double.TryParse(_ciParams["almTimeout"], out dblQcTimeout))
                    {
                        ConsoleWriter.WriteLine(Resources.LauncherTimeoutNotNumeric);
                        dblQcTimeout = int.MaxValue;
                    }

                    ConsoleWriter.WriteLine(string.Format(Resources.LuancherDisplayTimout, dblQcTimeout));

                    QcRunMode enmQcRunMode;
                    if (!Enum.TryParse(_ciParams["almRunMode"], true, out enmQcRunMode))
                    {
                        ConsoleWriter.WriteLine(Resources.LauncherIncorrectRunmode);
                        enmQcRunMode = QcRunMode.RUN_LOCAL;
                    }
                    ConsoleWriter.WriteLine(string.Format(Resources.LauncherDisplayRunmode, enmQcRunMode.ToString()));

                    //go over test sets in the parameters, and collect them
                    List<string> sets = GetParamsWithPrefix("TestSet", true);

                    if (sets.Count == 0)
                    {
                        ConsoleWriter.WriteLine(Resources.LauncherNoTests);
                        return null;
                    }

                    List<TestParameter> @params = GetValidParams();

                    //check if filterTests flag is selected; if yes apply filters on the list
                    bool isFilterSelected;
                    string filter = _ciParams.GetOrDefault("FilterTests");

                    isFilterSelected = !string.IsNullOrEmpty(filter) && Convert.ToBoolean(filter.ToLower());

                    string filterByName = _ciParams.GetOrDefault("FilterByName");

                    string statuses = _ciParams.GetOrDefault("FilterByStatus");

                    List<string> filterByStatuses = new List<string>();

                    if (statuses != string.Empty)
                    {
                        if (statuses.Contains(","))
                        {
                            filterByStatuses = statuses.Split(',').ToList();
                        }
                        else
                        {
                            filterByStatuses.Add(statuses);
                        }
                    }

                    bool isSSOEnabled = _ciParams.ContainsKey("SSOEnabled") ? Convert.ToBoolean(_ciParams["SSOEnabled"]) : false;
                    string clientID = _ciParams.GetOrDefault("almClientID");
                    string apiKey = _ciParams.ContainsKey("almApiKeySecret") ? Encrypter.Decrypt(_ciParams["almApiKeySecret"]) : string.Empty;
                    string almRunHost = _ciParams.GetOrDefault("almRunHost");

                    //create an Alm runner
                    runner = new AlmTestSetsRunner(_ciParams["almServerUrl"],
                                     _ciParams["almUsername"],
                                     Encrypter.Decrypt(_ciParams["almPassword"]),
                                     _ciParams["almDomain"],
                                     _ciParams["almProject"],
                                     dblQcTimeout,
                                     enmQcRunMode,
                                     almRunHost,
                                     sets,
                                     @params,
                                     isFilterSelected,
                                     filterByName,
                                     filterByStatuses,
                                     isFirstRun,
                                     _runType,
                                     isSSOEnabled,
                                     clientID, apiKey);
                    break;
                }
                case TestStorageType.FileSystem:
                { 
                    bool displayController = _ciParams.GetOrDefault("displayController") == ONE;
                    string analysisTemplate = _ciParams.GetOrDefault("analysisTemplate");

                    bool printInputParams = _ciParams.GetOrDefault("printTestParams", ONE) == ONE;
                    IEnumerable<string> jenkinsEnvVarsWithCommas = GetParamsWithPrefix("JenkinsEnv");
                    Dictionary<string, string> jenkinsEnvVars = new Dictionary<string, string>();
                    foreach (string var in jenkinsEnvVarsWithCommas)
                    {
                        string[] nameVal = var.Split(_colon_semicolon);
                        jenkinsEnvVars.Add(nameVal[0], nameVal[1]);
                    }

                    //add build tests and cleanup tests in correct order
                    List<TestData> validTests = new List<TestData>();
                    List<TestInfo> cleanupAndRerunTests = new List<TestInfo>();

                    if (isFirstRun)
                    {
                        ConsoleWriter.WriteLine("Run build tests");
                        List<TestData> validBuildTests = GetValidTests("Test", Resources.LauncherNoTestsFound, Resources.LauncherNoValidTests, string.Empty);
                        if (validBuildTests.Count == 0)
                        {
                            Environment.Exit((int)ExitCodeEnum.Failed);
                        }

                        //run only the build tests
                        foreach (var item in validBuildTests)
                        {
                            validTests.Add(item);
                        }
                    }
                    else
                    { //add also cleanup tests
                        string fsTestType = _ciParams.GetOrDefault("testType");
                        List<TestData> validCleanupTests = GetValidTests(CLEANUP_TEST, Resources.LauncherNoCleanupTestsFound, Resources.LauncherNoValidCleanupTests, fsTestType);
                        List<string> reruns = GetParamsWithPrefix("Reruns");
                        List<int> numberOfReruns = new List<int>();
                        foreach (var item in reruns)
                        {
                            numberOfReruns.Add(int.Parse(item));
                        }

                        bool noRerunsSet = CheckListOfRerunValues(numberOfReruns);

                        if (noRerunsSet)
                        {
                            ConsoleWriter.WriteLine("In order to rerun the tests the number of reruns should be greater than zero.");
                        }
                        else
                        {
                            switch (fsTestType)
                            {
                                case RERUN_ALL_TESTS: ConsoleWriter.WriteLine("The entire test set will run again."); break;
                                case RERUN_SPECIFIC_TESTS: ConsoleWriter.WriteLine("Only the selected tests will run again."); break;
                                case RERUN_FAILED_TESTS: ConsoleWriter.WriteLine("Only the failed tests will run again."); break;
                            }

                            for (int i = 0; i < numberOfReruns.Count; i++)
                            {
                                var currentRerun = numberOfReruns[i];

                                if (fsTestType == RERUN_ALL_TESTS || fsTestType == RERUN_FAILED_TESTS)
                                {
                                    while (currentRerun > 0)
                                    {
                                        if (validCleanupTests.Count > 0)
                                        {
                                            var cleanupTest = FileSystemTestsRunner.GetFirstTestInfo(validCleanupTests[i], jenkinsEnvVars);
                                            if (cleanupTest != null)
                                                cleanupAndRerunTests.Add(cleanupTest);
                                        }

                                        if (reruntests.Count > 0)
                                        {
                                            cleanupAndRerunTests.AddRange(reruntests);
                                        }
                                        else
                                        {
                                            Console.WriteLine(fsTestType == RERUN_ALL_TESTS ? "There are no tests to rerun." : "There are no failed tests to rerun.");
                                            break;
                                        }

                                        currentRerun--;
                                    }
                                }
                                else if (fsTestType == RERUN_SPECIFIC_TESTS)
                                {
                                    while (currentRerun > 0)
                                    {
                                        if (validCleanupTests.Count > 0)
                                        {
                                            var cleanupTest = FileSystemTestsRunner.GetFirstTestInfo(validCleanupTests[i], jenkinsEnvVars);
                                            if (cleanupTest != null)
                                                cleanupAndRerunTests.Add(cleanupTest);
                                        }
                                        if (reruntests != null && reruntests.Count > i)
                                            cleanupAndRerunTests.Add(reruntests[i]);
                                        else
                                        {
                                            Console.WriteLine(string.Format("There is no specific test with index = {0}", i + 1));
                                            break;
                                        }

                                        currentRerun--;
                                    }
                                }
                            }
                        }
                    }

                    //parse the timeout into a TimeSpan
                    TimeSpan timeout = TimeSpan.MaxValue;
                    if (_ciParams.ContainsKey("fsTimeout"))
                    {
                        string strTimeoutInSeconds = _ciParams["fsTimeout"];
                        if (strTimeoutInSeconds.Trim() != "-1")
                        {
                            int intTimeoutInSeconds;
                            int.TryParse(strTimeoutInSeconds, out intTimeoutInSeconds);
                            timeout = TimeSpan.FromSeconds(intTimeoutInSeconds);
                        }
                    }
                    ConsoleWriter.WriteLine("Launcher timeout is " + timeout.ToString(@"dd\:\:hh\:mm\:ss"));

                    //LR specific values:
                    //default values are set by JAVA code, in com.hpe.application.automation.tools.model.RunFromFileSystemModel.java

                    int pollingInterval = 30;
                    if (_ciParams.ContainsKey("controllerPollingInterval"))
                        pollingInterval = int.Parse(_ciParams["controllerPollingInterval"]);
                    ConsoleWriter.WriteLine("Controller Polling Interval: " + pollingInterval + " seconds");

                    TimeSpan perScenarioTimeOutMinutes = TimeSpan.MaxValue;
                    if (_ciParams.ContainsKey("PerScenarioTimeOut"))
                    {
                        string strTimeoutInMinutes = _ciParams["PerScenarioTimeOut"];
                        int intTimoutInMinutes;
                        if (strTimeoutInMinutes.Trim() != "-1" && int.TryParse(strTimeoutInMinutes, out intTimoutInMinutes))
                            perScenarioTimeOutMinutes = TimeSpan.FromMinutes(intTimoutInMinutes);
                    }
                    ConsoleWriter.WriteLine("PerScenarioTimeout: " + perScenarioTimeOutMinutes.ToString(@"dd\:\:hh\:mm\:ss") + " minutes");

                    char[] delimiter = { '\n' };
                    List<string> ignoreErrorStrings = new List<string>();
                    if (_ciParams.ContainsKey("ignoreErrorStrings"))
                    {
                        ignoreErrorStrings.AddRange(Array.ConvertAll(_ciParams["ignoreErrorStrings"].Split(delimiter, StringSplitOptions.RemoveEmptyEntries), ignoreError => ignoreError.Trim()));
                    }

                    //If a file path was provided and it doesn't exist stop the analysis launcher
                    if (!string.IsNullOrWhiteSpace(analysisTemplate) && !Helper.FileExists(analysisTemplate))
                    {
                        return null;
                    }

                    //--MC connection info
                    McConnectionInfo mcConnectionInfo = null;
                    try
                    {
                        mcConnectionInfo = new McConnectionInfo(_ciParams);
                    }
                    catch(NoMcConnectionException)
                    {
                        // no action, the Test will use the default UFT One settings
                    }
                    catch (Exception ex)
                    {
                        ConsoleWriter.WriteErrLine(ex.Message);
                        Environment.Exit((int)ExitCodeEnum.Failed);
                    }

                    // other mobile info
                    string mobileinfo = string.Empty;
                    if (_ciParams.ContainsKey("mobileinfo"))
                    {
                        mobileinfo = _ciParams["mobileinfo"];
                    }
                    CloudBrowser cloudBrowser = null;
                    string strCloudBrowser = _ciParams.GetOrDefault("cloudBrowser").Trim();
                    if (!strCloudBrowser.IsNullOrEmpty())
                    {
                         CloudBrowser.TryParse(strCloudBrowser, out cloudBrowser);
                    }

                    var parallelRunnerEnvironments = new Dictionary<string, List<string>>();

                    // retrieve the parallel runner environment for each test
                    if (_ciParams.ContainsKey("parallelRunnerMode"))
                    {
                        foreach (var test in validTests)
                        {
                            string envKey = "Parallel" + test.Id + "Env";
                            List<string> testEnvironments = GetParamsWithPrefix(envKey);

                            // add the environments for all the valid tests
                            parallelRunnerEnvironments.Add(test.Id, testEnvironments);
                        }
                    }

                    // users can provide a custom report path
                    string reportPath = null;
                    if (_ciParams.ContainsKey("fsReportPath"))
                    {
                        if (Directory.Exists(_ciParams["fsReportPath"]))
                        {   //path is not parameterized
                            reportPath = _ciParams["fsReportPath"];
                        }
                        else
                        {   //path is parameterized
                            string fsReportPath = _ciParams["fsReportPath"];

                            //get parameter name
                            fsReportPath = fsReportPath.Trim(new char[] { ' ', '$', '{', '}' });

                            //get parameter value
                            fsReportPath = fsReportPath.Trim(new char[] { ' ', '\t' });
                            try
                            {
                                reportPath = jenkinsEnvVars[fsReportPath];
                            }
                            catch (KeyNotFoundException)
                            {
                                Console.WriteLine("============================================================================");
                                Console.WriteLine("The provided results folder path {0} does not exist.", fsReportPath);
                                Console.WriteLine("============================================================================");
                                Environment.Exit((int)ExitCodeEnum.Failed);
                            }
                        }
                    }

                    RunAsUser uftRunAsUser = null;
                    string username = _ciParams.GetOrDefault("uftRunAsUserName");
                    if (!string.IsNullOrEmpty(username))
                    {
                        string encryptedAndEncodedPwd = _ciParams.GetOrDefault("uftRunAsUserEncodedPassword");
                        string encryptedPwd = _ciParams.GetOrDefault("uftRunAsUserPassword");
                        if (!string.IsNullOrEmpty(encryptedAndEncodedPwd))
                        {
                            string encodedPwd = Encrypter.Decrypt(encryptedAndEncodedPwd);
                            uftRunAsUser = new RunAsUser(username, encodedPwd);
                        }
                        else if (!string.IsNullOrEmpty(encryptedPwd))
                        {
                            string plainTextPwd = Encrypter.Decrypt(encryptedPwd);
                            uftRunAsUser = new RunAsUser(username, plainTextPwd.ToSecureString());
                        }
                    }

                    SummaryDataLogger summaryDataLogger = GetSummaryDataLogger();
                    List<ScriptRTSModel> scriptRTSSet = GetScriptRtsSet();
                    string resultsFilename = _ciParams["resultsFilename"];
                    string uftRunMode = _ciParams.GetOrDefault("fsUftRunMode", "Fast");
                    if (validTests.Count > 0)
                    {
                        runner = new FileSystemTestsRunner(validTests, GetValidParams(), printInputParams, timeout, uftRunMode, pollingInterval, perScenarioTimeOutMinutes, ignoreErrorStrings, jenkinsEnvVars, new DigitalLab(mcConnectionInfo, mobileinfo, cloudBrowser), parallelRunnerEnvironments, displayController, analysisTemplate, summaryDataLogger, scriptRTSSet, reportPath, resultsFilename, _encoding, uftRunAsUser);
                    }
                    else if (cleanupAndRerunTests.Count > 0)
                    {
                        runner = new FileSystemTestsRunner(cleanupAndRerunTests, printInputParams, timeout, uftRunMode, pollingInterval, perScenarioTimeOutMinutes, ignoreErrorStrings, jenkinsEnvVars, new DigitalLab(mcConnectionInfo, mobileinfo, cloudBrowser), parallelRunnerEnvironments, displayController, analysisTemplate, summaryDataLogger, scriptRTSSet, reportPath, resultsFilename, _encoding, uftRunAsUser);
                    }
                    else
                    {
                        ConsoleWriter.WriteLine(Resources.FsRunnerNoValidTests);
                        Environment.Exit((int)ExitCodeEnum.Failed);
                    }

                    break;
                }
                case TestStorageType.MBT:
                    string parentFolder = _ciParams["parentFolder"];
                    string repoFolder = _ciParams["repoFolder"];

                    int counter = 1;
                    string testProp = "test" + counter;
                    List<MBTTest> tests = new List<MBTTest>();
                    while (_ciParams.ContainsKey(testProp))
                    {
                        MBTTest test = new MBTTest();
                        tests.Add(test);

                        test.Name = _ciParams[testProp];
                        test.Script = _ciParams.GetOrDefault("script" + counter);
                        test.UnitIds = _ciParams.GetOrDefault("unitIds" + counter);
                        test.UnderlyingTests = new List<string>(_ciParams.GetOrDefault("underlyingTests" + counter).Split(';'));
                        test.PackageName = _ciParams.GetOrDefault("package" + counter);
                        test.DatableParams = _ciParams.GetOrDefault("datableParams" + counter);

                        test.PackageName = _ciParams.GetOrDefault("package" + counter, "");
                        test.DatableParams = _ciParams.GetOrDefault("datableParams" + counter, "");
                        testProp = "test" + (++counter);
                    }

                    runner = new MBTRunner(parentFolder, repoFolder, tests);
                    break;
                default:
                    runner = null;
                    break;
            }
            if (runner != null && !isFirstRun)
            {
                RunnerBase rbase = (RunnerBase)runner;
                rbase.XmlBuilder =  initialRunnerBase.XmlBuilder; // reuse the populated initialXmlBuilder because it contains testcases already created, in order to speed up the report building
                rbase.IndexOfRptDirsByTestPath = initialRunnerBase.IndexOfRptDirsByTestPath;
            }
            return runner;
        }

        private List<string> GetParamsWithPrefix(string prefix, bool skipEmptyEntries = false)
        {
            int idx = 1;
            List<string> parameters = new List<string>();
            while (_ciParams.ContainsKey(prefix + idx))
            {
                string set = _ciParams[prefix + idx];
                if (set.StartsWith("Root\\"))
                    set = set.Substring(5);
                set = set.TrimEnd(" \\".ToCharArray());
                if (!(skipEmptyEntries && string.IsNullOrWhiteSpace(set)))
                {
                    parameters.Add(set);
                }
                ++idx;
            }
            return parameters;
        }

        private Dictionary<string, string> GetKeyValuesWithPrefix(string prefix)
        {
            int idx = 1;

            Dictionary<string, string> dict = new Dictionary<string, string>();

            while (_ciParams.ContainsKey(prefix + idx))
            {
                string set = _ciParams[prefix + idx];
                if (set.StartsWith("Root\\"))
                    set = set.Substring(5);
                set = set.TrimEnd(" \\".ToCharArray());
                string key = prefix + idx;
                dict[key] = set;
                ++idx;
            }

            return dict;
        }

        /// <summary>
        /// used by the run fuction to run the tests
        /// </summary>
        /// <param name="resultsFile"></param>
        ///
        private void RunSummary(string resultsFile, TestSuiteRunResults results, TestSuiteRunResults rerunResults = null)
        {
            try
            {
                if (results == null)
                {
                    Environment.Exit((int)ExitCodeEnum.Failed);
                    return;
                }

                if (_runType != TestStorageType.FileSystem) // for FileSystem the report is already generated inside FileSystemTestsRunner.Run()
                {
                    if (_ciRun)
                    {
                        _xmlBuilder = new JunitXmlBuilder();
                        _xmlBuilder.XmlName = resultsFile;
                    }

                    _xmlBuilder.CreateXmlFromRunResults(results);
                }
                var allTestRuns = new List<TestRunResults>(results.TestRuns);

                if (allTestRuns.Count == 0)
                {
                    ConsoleWriter.WriteLine(Resources.GeneralDoubleSeperator);
                    ConsoleWriter.WriteLine("No tests were run");
                    _exitCode = ExitCodeEnum.Failed;
                    Environment.Exit((int)_exitCode);
                }

                bool is4Rerun = rerunResults != null && rerunResults.TestRuns.Count > 0;

                int failures, successes, errors, warnings;
                if (is4Rerun)
                {
                    UpdateExitCode(rerunResults, out successes, out failures, out errors, out warnings);
                    failures += results.NumFailures;
                    successes += allTestRuns.Count(t => t.TestState == TestState.Passed);
                    errors += results.NumErrors;
                    warnings += results.NumWarnings;
                    allTestRuns.AddRange(rerunResults.TestRuns);
                }
                else
                {
                    UpdateExitCode(results, out successes, out failures, out errors, out warnings);
                }

                //this is the total run summary
                ConsoleWriter.ActiveTestRun = null;
                string runStatus = string.Empty;

                switch (_exitCode)
                {
                    case ExitCodeEnum.Passed:
                        runStatus = "Job succeeded";
                        break;
                    case ExitCodeEnum.Unstable:
						{
                            if (failures > 0 && warnings > 0)
                            {
                                runStatus = "Job unstable (Passed with failed tests and generated warnings)";
                            }
                            else if (failures > 0)
                            {
                                runStatus = "Job unstable (Passed with failed tests)";
                            }
                            else if (warnings > 0)
                            {
                                runStatus = "Job unstable (Generated warnings)";
                            }

                            break;
                        }
                    case ExitCodeEnum.Aborted:
                        runStatus = "Job failed due to being Aborted";
                        break;
                    case ExitCodeEnum.Failed:
                        runStatus = "Job failed";
                        break;
                    default:
                        runStatus = "Error: Job status is Undefined";
                        break;
                }

                ConsoleWriter.WriteLine(Resources.LauncherDoubleSeparator);
                ConsoleWriter.WriteLine(string.Format(Resources.LauncherDisplayStatistics, runStatus, allTestRuns.Count, successes, failures, errors, warnings));

                int testIndex = 1;
                if (!_runner.RunWasCancelled)
                {
                    allTestRuns.ForEach(tr => { ConsoleWriter.WriteLine(((tr.HasWarnings) ? "Warning".PadLeft(7) : tr.TestState.ToString().PadRight(7)) + ": " + tr.TestPath + "[" + testIndex + "]"); testIndex++; });

                    ConsoleWriter.WriteLine(Resources.LauncherDoubleSeparator);

                    if (ConsoleWriter.ErrorSummaryLines != null && ConsoleWriter.ErrorSummaryLines.Count > 0)
                    {
                        ConsoleWriter.WriteLine("Job Errors summary:");
                        ConsoleWriter.ErrorSummaryLines.ForEach(line => ConsoleWriter.WriteLine(line));
                    }
                }
            }
            finally
            {
                try
                {
                    _runner.Dispose();
                }
                catch (Exception ex)
                {
                    ConsoleWriter.WriteLine(string.Format(Resources.LauncherRunnerDisposeError, ex.Message));
                }
            }
        }

        private void UpdateExitCode(TestSuiteRunResults results, out int successes, out int failures, out int errors, out int warnings)
        {
            failures = results.NumFailures;
            successes = results.TestRuns.Count(t => t.TestState == TestState.Passed);
            errors = results.NumErrors;
            warnings = results.NumWarnings;

            if (_exitCode != ExitCodeEnum.Aborted)
            {
                if (errors > 0)
                {
                    _exitCode = ExitCodeEnum.Failed;
                }
                else if (failures > 0 && successes > 0)
                {
                    _exitCode = ExitCodeEnum.Unstable;
                }
                else if (failures > 0)
                {
                    _exitCode = ExitCodeEnum.Failed;
                }
                else if (warnings > 0)
                {
                    _exitCode = ExitCodeEnum.Unstable;
                }
                else if (successes > 0)
                {
                    _exitCode = ExitCodeEnum.Passed;
                }

                foreach (var testRun in results.TestRuns)
                {
                    if (testRun.FatalErrors > 0 && !string.IsNullOrWhiteSpace(testRun.TestPath))
                    {
                        _exitCode = ExitCodeEnum.Failed;
                        break;
                    }
                }
            }
        }

        private SummaryDataLogger GetSummaryDataLogger()
        {
            SummaryDataLogger summaryDataLogger;

            if (_ciParams.ContainsKey("SummaryDataLog"))
            {
                string[] summaryDataLogFlags = _ciParams["SummaryDataLog"].Split(";".ToCharArray());

                if (summaryDataLogFlags.Length == 4)
                {
                    //If the polling interval is not a valid number, set it to default (10 seconds)
                    int summaryDataLoggerPollingInterval;
                    if (!int.TryParse(summaryDataLogFlags[3], out summaryDataLoggerPollingInterval))
                    {
                        summaryDataLoggerPollingInterval = 10;
                    }

                    summaryDataLogger = new SummaryDataLogger(
                        summaryDataLogFlags[0] == ONE,
                        summaryDataLogFlags[1] == ONE,
                        summaryDataLogFlags[2] == ONE,
                        summaryDataLoggerPollingInterval
                    );
                }
                else
                {
                    summaryDataLogger = new SummaryDataLogger();
                }
            }
            else
            {
                summaryDataLogger = new SummaryDataLogger();
            }

            return summaryDataLogger;
        }

        private List<ScriptRTSModel> GetScriptRtsSet()
        {
            List<ScriptRTSModel> scriptRtsSet = new List<ScriptRTSModel>();

            IEnumerable<string> scriptNames = GetParamsWithPrefix("ScriptRTS");
            foreach (string scriptName in scriptNames)
            {
                ScriptRTSModel scriptRts = new ScriptRTSModel(scriptName);

                IEnumerable<string> additionalAttributes = GetParamsWithPrefix("AdditionalAttribute");
                foreach (string additionalAttribute in additionalAttributes)
                {
                    //Each additional attribute contains: script name, aditional attribute name, value and description
                    string[] additionalAttributeArguments = additionalAttribute.Split(";".ToCharArray());
                    if (additionalAttributeArguments.Length == 4 && additionalAttributeArguments[0].Equals(scriptName))
                    {
                        scriptRts.AddAdditionalAttribute(new AdditionalAttributeModel(
                            additionalAttributeArguments[1],
                            additionalAttributeArguments[2],
                            additionalAttributeArguments[3])
                        );
                    }
                }

                scriptRtsSet.Add(scriptRts);
            }

            return scriptRtsSet;
        }

        /// <summary>
        /// Retrieve the list of valid test to run
        /// </summary>
        /// <param name="propPrefix"></param>
        /// <param name="errorNoTestsFound"></param>
        /// <param name="errorNoValidTests"></param>
        /// <returns>a list of tests</returns>
        private List<TestData> GetValidTests(string propPrefix, string errorNoTestsFound, string errorNoValidTests, string fsTestType)
        {
            if (fsTestType != RERUN_FAILED_TESTS || propPrefix == CLEANUP_TEST)
            {
                List<TestData> tests = new List<TestData>();
                Dictionary<string, string> testsKeyValue = GetKeyValuesWithPrefix(propPrefix);
                if (propPrefix == CLEANUP_TEST && testsKeyValue.Count == 0)
                {
                    return tests;
                }

                foreach (var item in testsKeyValue)
                {
                    tests.Add(new TestData(item.Value, item.Key));
                }

                if (tests.Count == 0)
                {
                    WriteToConsole(errorNoTestsFound);
                }
                else
                {
                    List<TestData> validTests = Helper.ValidateFiles(tests);

                    if (validTests.Count > 0) return validTests;

                    //no valid tests found
                    ConsoleWriter.WriteLine(errorNoValidTests);
                }
            }

            return new List<TestData>();
        }


        /// <summary>
        /// Returns all the valid parameters from the props file (CI args).
        /// </summary>
        /// <returns></returns>
        private List<TestParameter> GetValidParams()
        {
            List<TestParameter> parameters = new List<TestParameter>();

            int initialNumOfTests = _ciParams.ContainsKey("numOfTests") ? int.Parse(_ciParams["numOfTests"]) : 0;

            for (int i = 1; i <= initialNumOfTests; ++i)
            {
                int j = 1;
                while (_ciParams.ContainsKey(string.Format("Param{0}_Name_{1}", i, j)))
                {
                    string name = _ciParams[string.Format("Param{0}_Name_{1}", i, j)].Trim();

                    if (string.IsNullOrWhiteSpace(name))
                    {
                        ConsoleWriter.WriteLine(string.Format("Found no name associated with parameter with index {0} for test {1}.", j, i));
                        continue;
                    }

                    string val = _ciParams[string.Format("Param{0}_Value_{1}", i, j)].Trim();

                    string type = _ciParams[string.Format("Param{0}_Type_{1}", i, j)];
                    if (string.IsNullOrWhiteSpace(type))
                    {
                        ConsoleWriter.WriteLine(string.Format("Found no type associated with parameter {0}.", name));
                        continue;
                    }
                    else if (type == PASSWORD && !string.IsNullOrWhiteSpace(val))
                    {
                        val = Encrypter.Decrypt(val);
                    }

                    parameters.Add(new TestParameter(i, name, val, type.ToLower()));
                    ++j;
                }
            }

            return parameters;
        }

        /// <summary>
        /// Check if at least one test needs to run again
        /// </summary>
        /// <param name="numberOfReruns"></param>
        /// <returns>true if there is at least a test that needs to run again, false otherwise</returns>
        private bool CheckListOfRerunValues(List<int> numberOfReruns)
        {
            bool noRerunsSet = true;
            for (var j = 0; j < numberOfReruns.Count; j++)
            {
                if (numberOfReruns.ElementAt(j) <= 0) continue;
                noRerunsSet = false;
                break;
            }

            return noRerunsSet;
        }
    }
}
