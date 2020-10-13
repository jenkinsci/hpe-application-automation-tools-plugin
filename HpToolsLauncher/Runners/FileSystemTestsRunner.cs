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
 * © Copyright 2012-2019 Micro Focus or one of its affiliates..
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

using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Reflection;
using HpToolsLauncher.Properties;
using HpToolsLauncher.TestRunners;
using HpToolsLauncher.RTS;


namespace HpToolsLauncher
{
    public class FileSystemTestsRunner : RunnerBase, IDisposable
    {
        #region Members

        Dictionary<string, string> _jenkinsEnvVariables;
        private List<TestInfo> _tests;
        private static string _uftViewerPath;
        private int _errors, _fail;
        private bool _useUFTLicense;
        private bool _displayController;
        private string _analysisTemplate;
        private SummaryDataLogger _summaryDataLogger;
        private List<ScriptRTSModel> _scriptRTSSet;
        private TimeSpan _timeout = TimeSpan.MaxValue;
        private readonly string _uftRunMode;
        private Stopwatch _stopwatch = null;
        private string _abortFilename = System.IO.Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location) + "\\stop" + Launcher.UniqueTimeStamp + ".txt";

        //LoadRunner Arguments
        private int _pollingInterval;
        private TimeSpan _perScenarioTimeOutMinutes;
        private List<string> _ignoreErrorStrings;

        // parallel runner related information
        private Dictionary<string,List<string>> _parallelRunnerEnvironments;

        //saves runners for cleaning up at the end.
        private Dictionary<TestType, IFileSysTestRunner> _colRunnersForCleanup = new Dictionary<TestType, IFileSysTestRunner>();


        private McConnectionInfo _mcConnection;
        private string _mobileInfoForAllGuiTests;


        #endregion

        /// <summary>
        /// overloaded constructor for adding support for run mode selection
        /// </summary>
        /// <param name="sources"></param>
        /// <param name="timeout"></param>
        /// <param name="uftRunMode"></param>
        /// <param name="reportPath"></param>
        /// <param name="useUftLicense"></param>
        /// <param name="controllerPollingInterval"></param>
        /// <param name="perScenarioTimeOutMinutes"></param>
        /// <param name="ignoreErrorStrings"></param>
        /// <param name="jenkinsEnvVariables"></param>
        /// <param name="mcConnection"></param>
        /// <param name="mobileInfo"></param>
        /// <param name="parallelRunnerEnvironments"></param>
        /// <param name="displayController"></param>
        /// <param name="analysisTemplate"></param>
        /// <param name="summaryDataLogger"></param>
        /// <param name="scriptRtsSet"></param>
        public FileSystemTestsRunner(List<TestData> sources,
                                    TimeSpan timeout,
                                    string uftRunMode,
                                    int controllerPollingInterval,
                                    TimeSpan perScenarioTimeOutMinutes,
                                    List<string> ignoreErrorStrings,
                                    Dictionary<string, string> jenkinsEnvVariables,
                                    McConnectionInfo mcConnection,
                                    string mobileInfo,
                                    Dictionary<string, List<string>> parallelRunnerEnvironments,
                                    bool displayController,
                                    string analysisTemplate,
                                    SummaryDataLogger summaryDataLogger,
                                    List<ScriptRTSModel> scriptRtsSet,
                                    string reportPath,
                                    bool useUftLicense = false)

            :this(sources, timeout, controllerPollingInterval, perScenarioTimeOutMinutes, ignoreErrorStrings, jenkinsEnvVariables, mcConnection, mobileInfo, parallelRunnerEnvironments, displayController, analysisTemplate, summaryDataLogger, scriptRtsSet,reportPath, useUftLicense)
        {
            _uftRunMode = uftRunMode;
        }

        /// <summary>
        /// creates instance of the runner given a source.
        /// </summary>
        /// <param name="sources"></param>
        /// <param name="timeout"></param>
        /// <param name="scriptRtsSet"></param>
        /// <param name="reportPath"></param>
        /// <param name="controllerPollingInterval"></param>
        /// <param name="perScenarioTimeOutMinutes"></param>
        /// <param name="ignoreErrorStrings"></param>
        /// <param name="jenkinsEnvVariables"></param>
        /// <param name="mcConnection"></param>
        /// <param name="mobileInfo"></param>
        /// <param name="parallelRunnerEnvironments"></param>
        /// <param name="displayController"></param>
        /// <param name="analysisTemplate"></param>
        /// <param name="summaryDataLogger"></param>
        /// <param name="useUftLicense"></param>
        public FileSystemTestsRunner(List<TestData> sources,
                                    TimeSpan timeout,
                                    int controllerPollingInterval,
                                    TimeSpan perScenarioTimeOutMinutes,
                                    List<string> ignoreErrorStrings,
                                    Dictionary<string, string> jenkinsEnvVariables,
                                    McConnectionInfo mcConnection,
                                    string mobileInfo,
                                    Dictionary<string, List<string>> parallelRunnerEnvironments,
                                    bool displayController,
                                    string analysisTemplate,
                                    SummaryDataLogger summaryDataLogger,
                                    List<ScriptRTSModel> scriptRtsSet,
                                    string reportPath,
                                    bool useUftLicense = false)
        {
            _jenkinsEnvVariables = jenkinsEnvVariables;
            //search if we have any testing tools installed
            if (!Helper.IsTestingToolsInstalled(TestStorageType.FileSystem))
            {
                ConsoleWriter.WriteErrLine(string.Format(Resources.FileSystemTestsRunner_No_HP_testing_tool_is_installed_on, System.Environment.MachineName));
                Environment.Exit((int)Launcher.ExitCodeEnum.Failed);
            }

            _timeout = timeout;
            ConsoleWriter.WriteLine("FileSystemTestRunner timeout is " + _timeout );
            _stopwatch = Stopwatch.StartNew();

            _pollingInterval = controllerPollingInterval;
            _perScenarioTimeOutMinutes = perScenarioTimeOutMinutes;
            _ignoreErrorStrings = ignoreErrorStrings;
            
            _useUFTLicense = useUftLicense;
            _displayController = displayController;
            _analysisTemplate = analysisTemplate;
            _summaryDataLogger = summaryDataLogger;
            _scriptRTSSet = scriptRtsSet;
            _tests = new List<TestInfo>();

            _mcConnection = mcConnection;
            _mobileInfoForAllGuiTests = mobileInfo;

            _parallelRunnerEnvironments = parallelRunnerEnvironments;

            ConsoleWriter.WriteLine("UFT Mobile connection info is - " + _mcConnection.ToString());

            if (reportPath != null)
            {
                ConsoleWriter.WriteLine("Results directory is: " + reportPath);
            }

            //go over all sources, and create a list of all tests
            foreach (TestData source in sources)
            {
                List<TestInfo> testGroup = new List<TestInfo>();
                try
                {
                    //--handle directories which contain test subdirectories (recursively)
                    if (Helper.IsDirectory(source.Tests))
                    {

                        var testsLocations = Helper.GetTestsLocations(source.Tests);
                        foreach (var loc in testsLocations)
                        {
                            var test = new TestInfo(loc, loc, source.Tests,source.Id);
                            testGroup.Add(test);
                        }
                    }
                    //--handle mtb files (which contain links to tests)
                    else
                    //file might be LoadRunner scenario or
                    //mtb file (which contain links to tests)
                    //other files are dropped
                    {
                        testGroup = new List<TestInfo>();
                        FileInfo fi = new FileInfo(source.Tests);
                        if (fi.Extension == Helper.LoadRunnerFileExtention)
                            testGroup.Add(new TestInfo(source.Tests, source.Tests, source.Tests,source.Id));
                        else if (fi.Extension == ".mtb")
                        //if (source.TrimEnd().EndsWith(".mtb", StringComparison.CurrentCultureIgnoreCase))
                        {
                            MtbManager manager = new MtbManager();
                            var paths = manager.Parse(source.Tests);
                            foreach (var p in paths)
                            {
                                testGroup.Add(new TestInfo(p, p, source.Tests,source.Id));
                            }
                        }
                        else if (fi.Extension == ".mtbx")
                        {
                            testGroup = MtbxManager.Parse(source.Tests, _jenkinsEnvVariables, source.Tests);

                            // set the test Id for each test from the group
                            // this is important for parallel runner
                            foreach(var testInfo in testGroup)
                            {
                                testInfo.TestId = source.Id;
                            }
                        }
                    }
                }
                catch (Exception)
                {
                    testGroup = new List<TestInfo>();
                }

                //--handle single test dir, add it with no group
                if (testGroup.Count == 1)
                {
                    testGroup[0].TestGroup = "Test group";
                }

                _tests.AddRange(testGroup);
            }

            if (_tests == null || _tests.Count == 0)
            {
                ConsoleWriter.WriteLine(Resources.FsRunnerNoValidTests);
                Environment.Exit((int)Launcher.ExitCodeEnum.Failed);
            }

            // if a custom path was provided,set the custom report path for all the valid tests(this will overwrite the default location)
            if (reportPath != null)
            {
                _tests.ForEach(test => test.ReportPath = reportPath);
            }

            ConsoleWriter.WriteLine(string.Format(Resources.FsRunnerTestsFound, _tests.Count));

            foreach(var test in _tests)
            {
                ConsoleWriter.WriteLine("" + test.TestName);
                if(parallelRunnerEnvironments.ContainsKey(test.TestId))
                {
                    parallelRunnerEnvironments[test.TestId].ForEach(
                        env => ConsoleWriter.WriteLine("    " + env));
                }
            }

            ConsoleWriter.WriteLine(Resources.GeneralDoubleSeperator);
        }

        /// <summary>
        /// runs all tests given to this runner and returns a suite of run results
        /// </summary>
        /// <returns>The rest run results for each test</returns>
        public override TestSuiteRunResults Run()
        {
            //create a new Run Results object
            TestSuiteRunResults activeRunDesc = new TestSuiteRunResults();

            double totalTime = 0;
            try
            {
                var start = DateTime.Now;

                Dictionary<string, int> indexList = new Dictionary<string, int>();
                foreach(var test in _tests)
                {
                    indexList[test.TestPath] = 0;
                }

                Dictionary<string, int> rerunList = createDictionary(_tests);

                foreach (var test in _tests)
                {
                    if (indexList[test.TestPath] == 0)
                    {
                        indexList[test.TestPath] = 1;
                    }

                    if (RunCancelled()) break;

                    var testStart = DateTime.Now;

                    string errorReason = string.Empty;
                    TestRunResults runResult = null;
                    try
                    {
                        runResult = RunHpToolsTest(test, ref errorReason);
                    }
                    catch (Exception ex)
                    {
                        runResult = new TestRunResults
                        {
                            TestState = TestState.Error,
                            ErrorDesc = ex.Message,
                            TestName = test.TestName,
                            TestPath = test.TestPath
                        };
                    }

                    //get the original source for this test, for grouping tests under test classes
                    runResult.TestGroup = test.TestGroup;

                    activeRunDesc.TestRuns.Add(runResult);

                    //if fail was terminated before this step, continue
                    if (runResult.TestState != TestState.Failed)
                    {
                        if (runResult.TestState != TestState.Error)
                        {
                            Helper.GetTestStateFromReport(runResult);
                        }
                        else
                        {
                            if (string.IsNullOrEmpty(runResult.ErrorDesc))
                            {
                                runResult.ErrorDesc = RunCancelled() ? HpToolsLauncher.Properties.Resources.ExceptionUserCancelled : HpToolsLauncher.Properties.Resources.ExceptionExternalProcess;
                            }
                            runResult.ReportLocation = null;
                            runResult.TestState = TestState.Error;
                        }
                    }

                    if (runResult.TestState == TestState.Passed && runResult.HasWarnings)
                    {
                        runResult.TestState = TestState.Warning;
                        ConsoleWriter.WriteLine(Resources.FsRunnerTestDoneWarnings);
                    }
                    else
                    {
                        ConsoleWriter.WriteLine(string.Format(Resources.FsRunnerTestDone, runResult.TestState));
                    }

                    ConsoleWriter.WriteLine(DateTime.Now.ToString(Launcher.DateFormat) + " Test complete: " + runResult.TestPath + "\n-------------------------------------------------------------------------------------------------------");

                    UpdateCounters(runResult.TestState);
                    var testTotalTime = (DateTime.Now - testStart).TotalSeconds;

                    //create test folders
                    if (rerunList[test.TestPath] > 0)
                    {
                        if (!Directory.Exists(Path.Combine(test.TestPath, "Report1")))
                        {
                            rerunList[test.TestPath]--;
                        }
                        else
                        {
                            indexList[test.TestPath]++;
                            rerunList[test.TestPath]--;
                        }
                    }

                    //update report folder
                    String uftReportDir = Path.Combine(test.TestPath, "Report");
                    String uftReportDirNew = Path.Combine(test.TestPath, "Report" + indexList[test.TestPath]);

                    try
                    {
                        if (Directory.Exists(uftReportDir))
                        {
                            if (Directory.Exists(uftReportDirNew))
                            {
                                Helper.DeleteDirectory(uftReportDirNew);
                            }

                            Directory.Move(uftReportDir, uftReportDirNew);
                        }
                    }
                    catch
                    {
                        System.Threading.Thread.Sleep(1000);
                        Directory.Move(uftReportDir, uftReportDirNew);
                    } 
                }

                totalTime = (DateTime.Now - start).TotalSeconds;
            }
            finally
            {
                activeRunDesc.NumTests = _tests.Count;
                activeRunDesc.NumErrors = _errors;
                activeRunDesc.TotalRunTime = TimeSpan.FromSeconds(totalTime);
                activeRunDesc.NumFailures = _fail;

                foreach (IFileSysTestRunner cleanupRunner in _colRunnersForCleanup.Values)
                {
                    cleanupRunner.CleanUp();
                }
            }

            return activeRunDesc;
        }

             
        private Dictionary<string, int> createDictionary(List<TestInfo> validTests)
        {
            var rerunList = new Dictionary<string, int>();
            foreach (var item in validTests)
            {
                if (!rerunList.ContainsKey(item.TestPath))
                {
                    rerunList.Add(item.TestPath, 1);
                }
                else
                {
                    rerunList[item.TestPath]++;
                }
            }

            return rerunList;
        }

        /// <summary>
        /// checks if timeout has expired
        /// </summary>
        /// <returns></returns>
        private bool CheckTimeout()
        {
            TimeSpan timeLeft = _timeout - _stopwatch.Elapsed;
            return (timeLeft > TimeSpan.Zero);
        }

        /// <summary>
        /// creates a correct type of runner and runs a single test.
        /// </summary>
        /// <param name="testInfo"></param>
        /// <param name="errorReason"></param>
        /// <returns></returns>
        private TestRunResults RunHpToolsTest(TestInfo testInfo, ref string errorReason)
        {
            var testPath = testInfo.TestPath;
         
            var type = Helper.GetTestType(testPath);

            // if we have at least one environment for parallel runner,
            // then it must be enabled
            var isParallelRunnerEnabled = _parallelRunnerEnvironments.Count > 0;

            if (isParallelRunnerEnabled && type == TestType.QTP)
            {
                type = TestType.ParallelRunner;
            }
            // if the current test is an api test ignore the parallel runner flag
            // and just continue as usual
            else if (isParallelRunnerEnabled && type == TestType.ST)
            {
                ConsoleWriter.WriteLine("ParallelRunner does not support API tests, treating as normal test.");
            }

            IFileSysTestRunner runner = null;
            switch (type)
            {
                case TestType.ST:
                    runner = new ApiTestRunner(this, _timeout - _stopwatch.Elapsed);
                    break;
                case TestType.QTP:
                    runner = new GuiTestRunner(this, _useUFTLicense, _timeout - _stopwatch.Elapsed, _uftRunMode, _mcConnection, _mobileInfoForAllGuiTests);
                    break;
                case TestType.LoadRunner:
                    AppDomain.CurrentDomain.AssemblyResolve += Helper.HPToolsAssemblyResolver;
                    runner = new PerformanceTestRunner(this, _timeout, _pollingInterval, _perScenarioTimeOutMinutes, _ignoreErrorStrings, _displayController, _analysisTemplate, _summaryDataLogger, _scriptRTSSet);
                    break;
                case TestType.ParallelRunner:
                    runner = new ParallelTestRunner(this, _timeout - _stopwatch.Elapsed, _mcConnection, _mobileInfoForAllGuiTests, _parallelRunnerEnvironments);
                    break;
            }
            
            if (runner != null)
            {
                if (!_colRunnersForCleanup.ContainsKey(type))
                    _colRunnersForCleanup.Add(type, runner);

                Stopwatch s = Stopwatch.StartNew();

                var results = runner.RunTest(testInfo, ref errorReason, RunCancelled);

                results.Runtime = s.Elapsed;
                if (type == TestType.LoadRunner)
                    AppDomain.CurrentDomain.AssemblyResolve -= Helper.HPToolsAssemblyResolver;

                return results;
            }

            //check for abortion
            if (System.IO.File.Exists(_abortFilename))
            {

                ConsoleWriter.WriteLine(Resources.GeneralStopAborted);

                //stop working 
                Environment.Exit((int)Launcher.ExitCodeEnum.Aborted);
            }

            return new TestRunResults { ErrorDesc = "Unknown TestType", TestState = TestState.Error };
        }


        /// <summary>
        /// checks if run was cancelled/aborted
        /// </summary>
        /// <returns></returns>
        public bool RunCancelled()
        {
            //if timeout has passed
            if (_stopwatch.Elapsed > _timeout && !_blnRunCancelled)
            {
                ConsoleWriter.WriteLine(Resources.SmallDoubleSeparator);
                ConsoleWriter.WriteLine(Resources.GeneralTimedOut);
                ConsoleWriter.WriteLine(Resources.SmallDoubleSeparator);

                Launcher.ExitCode = Launcher.ExitCodeEnum.Aborted;
                _blnRunCancelled = true;
            }

            return _blnRunCancelled;
        }

        /// <summary>
        /// sums errors and failed tests
        /// </summary>
        /// <param name="testState"></param>
        private void UpdateCounters(TestState testState)
        {
            switch (testState)
            {
                case TestState.Error:
                    _errors += 1;
                    break;
                case TestState.Failed:
                    _fail += 1;
                    break;
            }
        }


        /// <summary>
        /// Opens the report viewer for the given report directory
        /// </summary>
        /// <param name="reportDirectory"></param>
        public static void OpenReport(string reportDirectory)
        {
            Helper.OpenReport(reportDirectory, ref _uftViewerPath);
        }
    }
}
