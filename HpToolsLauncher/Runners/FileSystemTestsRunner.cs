// (c) Copyright 2012 Hewlett-Packard Development Company, L.P. 
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Reflection;
using HpToolsLauncher.Properties;
using HpToolsLauncher.TestRunners;

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
        private TimeSpan _timeout = TimeSpan.MaxValue;
        private readonly string _uftRunMode;
        private Stopwatch _stopwatch = null;
        private string _abortFilename = System.IO.Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location) + "\\stop" + Launcher.UniqueTimeStamp + ".txt";

        //LoadRunner Arguments
        private int _pollingInterval;
        private TimeSpan _perScenarioTimeOutMinutes;
        private List<string> _ignoreErrorStrings;


        //saves runners for cleaning up at the end.
        private Dictionary<TestType, IFileSysTestRunner> _colRunnersForCleanup = new Dictionary<TestType, IFileSysTestRunner>();


        public const string UftJUnitRportName = "uftRunnerRoot";

        private McConnectionInfo _mcConnection;
        private string _mobileInfoForAllGuiTests;

        #endregion

        /// <summary>
        /// overloaded constructor for adding support for run mode selection
        /// </summary>
        /// <param name="sources"></param>
        /// <param name="timeout"></param>
        /// <param name="uftRunMode"></param>
        /// <param name="backgroundWorker"></param>
        /// <param name="useUFTLicense"></param>
        public FileSystemTestsRunner(List<string> sources,
                                    TimeSpan timeout,
                                    string uftRunMode,
                                    int ControllerPollingInterval,
                                    TimeSpan perScenarioTimeOutMinutes,
                                    List<string> ignoreErrorStrings,
                                    Dictionary<string, string> jenkinsEnvVariables,
                                    McConnectionInfo mcConnection,
                                    string mobileInfo,
                                    bool useUFTLicense = false)
            :this(sources, timeout, ControllerPollingInterval, perScenarioTimeOutMinutes, ignoreErrorStrings, jenkinsEnvVariables, mcConnection, mobileInfo, useUFTLicense)
        {
            _uftRunMode = uftRunMode;
        }

        /// <summary>
        /// creates instance of the runner given a source.
        /// </summary>
        /// <param name="sources"></param>
        /// <param name="timeout"></param>
        /// <param name="backgroundWorker"></param>
        /// <param name="useUFTLicense"></param>
        public FileSystemTestsRunner(List<string> sources,
                                    TimeSpan timeout,
                                    int ControllerPollingInterval,
                                    TimeSpan perScenarioTimeOutMinutes,
                                    List<string> ignoreErrorStrings,
                                    Dictionary<string, string> jenkinsEnvVariables,
                                    McConnectionInfo mcConnection,
                                    string mobileInfo,
                                    bool useUFTLicense = false)
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

            _pollingInterval = ControllerPollingInterval;
            _perScenarioTimeOutMinutes = perScenarioTimeOutMinutes;
            _ignoreErrorStrings = ignoreErrorStrings;


            _useUFTLicense = useUFTLicense;
            _tests = new List<TestInfo>();

            _mcConnection = mcConnection;
            _mobileInfoForAllGuiTests = mobileInfo;

            ConsoleWriter.WriteLine("Mc connection info is - " + _mcConnection.ToString());

            //go over all sources, and create a list of all tests
            foreach (string source in sources)
            {
                List<TestInfo> testGroup = new List<TestInfo>();
                try
                {
                    //--handle directories which contain test subdirectories (recursively)
                    if (Helper.IsDirectory(source))
                    {

                        var testsLocations = Helper.GetTestsLocations(source);
                        foreach (var testPath in testsLocations)
                        {
                            var testClass = Directory.GetParent(testPath).Name;
                            var testName = Path.GetFileName(testPath);
                            var test = new TestInfo(testPath, testName, testClass);

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
                        FileInfo fi = new FileInfo(source);
                        if (fi.Extension == Helper.LoadRunnerFileExtention)
                            testGroup.Add(new TestInfo(source, source, source));
                        else if (fi.Extension == ".mtb")
                        //if (source.TrimEnd().EndsWith(".mtb", StringComparison.CurrentCultureIgnoreCase))
                        {
                            MtbManager manager = new MtbManager();
                            var paths = manager.Parse(source);
                            foreach (var p in paths)
                            {
                                testGroup.Add(new TestInfo(p, p, source));
                            }
                        }
                        else if (fi.Extension == ".mtbx")
                        //if (source.TrimEnd().EndsWith(".mtb", StringComparison.CurrentCultureIgnoreCase))
                        {
                            testGroup = MtbxManager.Parse(source, _jenkinsEnvVariables, source);
                        }
                    }
                }
                catch (Exception)
                {
                    testGroup = new List<TestInfo>();
                }

                _tests.AddRange(testGroup);
            }

            if (_tests == null || _tests.Count == 0)
            {
                ConsoleWriter.WriteLine(Resources.FsRunnerNoValidTests);
                Environment.Exit((int)Launcher.ExitCodeEnum.Failed);
            }

            ConsoleWriter.WriteLine(string.Format(Resources.FsRunnerTestsFound, _tests.Count));
            _tests.ForEach(t => ConsoleWriter.WriteLine("" + t.TestName));
            ConsoleWriter.WriteLine(Resources.GeneralDoubleSeperator);
        }

        /// <summary>
        /// runs all tests given to this runner and returns a suite of run resutls
        /// </summary>
        /// <returns>The rest run results for each test</returns>
        public override TestSuiteRunResults Run()
        {
            //create a new Run Results object
            TestSuiteRunResults activeRunDesc = new TestSuiteRunResults();
            TestInfo firstTest = _tests[0];

            double totalTime = 0;
            var start = DateTime.Now;
            KillAllAborterProcesses();
            Process aborter = StartHPToolsAborter();

            try
            {
                LogCleanupTestInfo();    

                foreach (var test in _tests)
                {
                    if (IsTestPlaceholder(test)) continue;
                    if (RunCancelled()) break;

                    TestRunResults runResult = null;

                    runResult = ExecuteTest(test);
                    if (RunCancelled()) break;

                    if (IsTestFailed(runResult) && IsCleanupTestDefined() && !IsCleanupTest(test))
                    {
                        Console.WriteLine("Test Failed: CLEANUP AND RE-RUN");
                        runRobotCleanup();
                        ExecuteTest(GetCleanupTest());
           
                        if (RunCancelled()) break;
                        runResult = ExecuteTest(test);
                    }

                    activeRunDesc.TestRuns.Add(runResult);
                    AnalyzeRunResult(runResult);
                }
            }
            catch (Exception)
            {
                //Ignore
            }
            finally
            {
                if (!aborter.HasExited)
                {
                    aborter.Kill();
                }

                totalTime = (DateTime.Now - start).TotalSeconds;
                activeRunDesc.NumTests = _tests.Count;
                activeRunDesc.NumErrors = _errors;
                activeRunDesc.TotalRunTime = TimeSpan.FromSeconds(totalTime);
                activeRunDesc.NumFailures = _fail;

                runRobotCleanup();
            }
 
            return activeRunDesc;
        }


        private void LogCleanupTestInfo()
        {
            if (IsCleanupTestDefined())
            {
                Console.WriteLine("Used Cleanup Test: " + GetCleanupTest().TestPath);
            }
            else
            {
                Console.WriteLine("No Cleanup Test was Defined!");
            }
        }

        private void AnalyzeRunResult(TestRunResults runResult)
        {
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
                        if (RunCancelled())
                        {
                            runResult.ErrorDesc = HpToolsLauncher.Properties.Resources.ExceptionUserCancelled;
                        }
                        else
                        {
                            runResult.ErrorDesc = HpToolsLauncher.Properties.Resources.ExceptionExternalProcess;
                        }
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
        }

        private TestRunResults ExecuteTest(TestInfo test)
        {
            TestRunResults runResult = null;
            string errorReason = string.Empty;
            var testStart = DateTime.Now;

            try
            {
                runResult = RunHPToolsTest(test, ref errorReason);
            }
            catch (Exception ex)
            {
                runResult = new TestRunResults();
                runResult.TestState = TestState.Error;
                runResult.ErrorDesc = ex.Message;
                runResult.TestName = test.TestName;

                ConsoleWriter.WriteLine("ExecuteTest Exception: " + ex.Message);
            }

            //get the original source for this test, for grouping tests under test classes
            runResult.Runtime = (DateTime.Now - testStart);
            runResult.TestGroup = test.TestGroup;
            return runResult;
        }

        private void KillAllAborterProcesses()
        {
            //kill the qtp automation, to make sure it will run correctly next time
            Process[] processes = Process.GetProcessesByName("HpToolsAborter.exe");
            foreach(Process aProcess in processes)
            {
                aProcess.Kill();
            }
        }

        private Process StartHPToolsAborter()
        {
            ConsoleWriter.WriteLine("Starting HP Tools Aborter");
            JavaProperties aborterProps = new JavaProperties();
            aborterProps.Add("runType", "FileSystem");

            // Timeout + 60 to give time for UFT to stop Gratefully
            aborterProps.Add("timeout", Convert.ToString(Convert.ToInt32(_timeout.TotalSeconds) + 60));
            aborterProps.Save("aborter.properties", "Properties for Stopping the HP Tools Launcher");

            Process process = new Process();
            process.StartInfo.FileName = "HpToolsAborter.exe";
            process.StartInfo.Arguments = "aborter.properties";
            process.StartInfo.WindowStyle = ProcessWindowStyle.Hidden;
            process.StartInfo.RedirectStandardOutput = true;
            process.StartInfo.RedirectStandardError = true;
            process.StartInfo.UseShellExecute = false;
            process.Start();

            return process;
        }

        private TestInfo GetCleanupTest()
        {
            return _tests[0];
        }


        private bool IsCleanupTestDefined()
        {
            if(_tests.Count == 1)
            {
                return false;
            }

            return !IsTestPlaceholder(_tests[0]);
        }

        private bool IsCleanupTest(TestInfo test)
        {
            return IsCleanupTestDefined() && test.TestPath.Equals(_tests[0].TestPath);
        }


        private bool IsTestPlaceholder(TestInfo test)
        {
            return test.TestPath.Equals("-");
        }

        private bool IsTestFailed(TestRunResults runResult)
        {
            return runResult.TestState == TestState.Failed || runResult.TestState == TestState.Error;
        }

        private void runRobotCleanup()
        {

            foreach (IFileSysTestRunner cleanupRunner in _colRunnersForCleanup.Values)
            {
                cleanupRunner.CleanUp();
            }
        }

        /// <summary>
        /// checks if timeout has expired
        /// </summary>
        /// <returns></returns>
        private bool CheckTimeout()
        {
            TimeSpan timeleft = _timeout - _stopwatch.Elapsed;
            return (timeleft > TimeSpan.Zero);
        }

        /// <summary>
        /// creates a correct type of runner and runs a single test.
        /// </summary>
        /// <param name="testPath"></param>
        /// <param name="errorReason"></param>
        /// <returns></returns>
        private TestRunResults RunHPToolsTest(TestInfo testinf, ref string errorReason)
        {

            var testPath = testinf.TestPath;
            var type = Helper.GetTestType(testPath);
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
                    runner = new PerformanceTestRunner(this, _timeout, _pollingInterval, _perScenarioTimeOutMinutes, _ignoreErrorStrings);
                    break;
            }


            if (runner != null)
            {
                if (!_colRunnersForCleanup.ContainsKey(type))
                    _colRunnersForCleanup.Add(type, runner);

                Stopwatch s = Stopwatch.StartNew();

                var results = runner.RunTest(testinf, ref errorReason, RunCancelled);

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
                ConsoleWriter.WriteLine(Resources.GeneralTimedOut);

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
