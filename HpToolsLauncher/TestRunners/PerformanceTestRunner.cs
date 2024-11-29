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
 using HP.LoadRunner.Interop.Wlrun;
using HpToolsLauncher.Properties;
using HpToolsLauncher.RTS;
using HpToolsLauncher.Utils;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Threading;
using System.Xml;

namespace HpToolsLauncher.TestRunners
{


    public class PerformanceTestRunner : IFileSysTestRunner
    {
        public const string LRR_FOLDER = "LRR";
        public const string LRA_FOLDER = "LRA";
        public const string HTML_FOLDER = "HTML";
        public const string ANALYSIS_LAUNCHER = @".\LRAnalysisLauncher.exe";

        private IAssetRunner _runner;
        private TimeSpan _timeout;
        private int _pollingInterval;
        private TimeSpan _perScenarioTimeOutMinutes;
        private RunCancelledDelegate _runCancelled;
        private bool _displayController;
        private string _analysisTemplate;
        private SummaryDataLogger _summaryDataLogger;
        private List<ScriptRTSModel> _scriptRTSSet;

        private bool _scenarioEnded;
        private bool _scenarioEndedEvent;
        private LrEngine _engine;
        private Stopwatch _stopWatch;
        private string _resultsFolder;
        private string _controller_result_dir;
        private List<string> _ignoreErrorStrings;

        private enum VuserStatus
        {
            Down = 1,
            Pending = 2,
            Init = 3,
            Ready = 4,
            Run = 5,
            Rendez = 6,
            Passed = 7,
            Failed = 8,
            Error = 9,
            GradualExiting = 12,
            Exiting = 10,
            Stopped = 11
        };
        private int[] _vuserStatus = new int[13];

        private enum ERRORState { Ignore, Error };

        private class ControllerError
        {
            public ERRORState state { get; set; }
            public int occurences { get; set; }
        };

        Dictionary<string, ControllerError> _errors;
        int _errorsCount;

        public PerformanceTestRunner(IAssetRunner runner, TimeSpan timeout, int pollingInterval, TimeSpan perScenarioTimeOut, List<string> ignoreErrorStrings, bool displayController, string analysisTemplate, SummaryDataLogger summaryDataLogger, List<ScriptRTSModel> scriptRTSSet)
        {
            this._runner = runner;
            this._timeout = timeout;
            this._pollingInterval = pollingInterval;
            this._perScenarioTimeOutMinutes = perScenarioTimeOut;
            this._ignoreErrorStrings = ignoreErrorStrings;
            this._displayController = displayController;
            this._analysisTemplate = analysisTemplate;
            this._summaryDataLogger = summaryDataLogger;
            this._scriptRTSSet = scriptRTSSet;

            this._scenarioEnded = false;
            _engine = null;
            this._errors = null;
            this._errorsCount = 0;
        }

        public TestRunResults RunTest(TestInfo scenarioInf, ref string errorReason, RunCancelledDelegate runCancelled, out Dictionary<string, string> outParams)
        {
            outParams = new Dictionary<string, string>();
            string scenarioPath = scenarioInf.TestPath;
            //prepare the instance that will contain test results for JUnit
            TestRunResults runDesc = new TestRunResults { TestType = TestType.LoadRunner };

            ConsoleWriter.ActiveTestRun = runDesc;
            ConsoleWriter.WriteLineWithTime("Running: " + scenarioPath);

            _resultsFolder = Helper.GetTempDir();
            if (!scenarioInf.ReportPath.IsNullOrEmpty())
            {
                _resultsFolder = scenarioInf.ReportPath;
            }

            //a directory with this name may already exist. try to delete it.
            if (Directory.Exists(_resultsFolder))
            {
                try
                {
                    // Directory.Delete(_resultsFolder, true);
                    DirectoryInfo dir = new DirectoryInfo(_resultsFolder);
                    dir.GetFiles().ToList().ForEach(file => file.Delete());
                    dir.GetDirectories().ToList().ForEach(subdir => subdir.Delete());
                }
                catch (Exception)
                {
                    Console.WriteLine(string.Format(Resources.CannotDeleteReportFolder, _resultsFolder));
                }
            }
            else
            {
                try
                {
                    Directory.CreateDirectory(_resultsFolder);
                }
                catch
                {
                    errorReason = string.Format(Resources.FailedToCreateTempDirError, _resultsFolder);
                    runDesc.TestState = TestState.Error;
                    runDesc.ErrorDesc = errorReason;

                    Environment.ExitCode = (int)Launcher.ExitCodeEnum.Failed;
                    return runDesc;
                }
            }
            //create LRR folder:
            _controller_result_dir = Path.Combine(_resultsFolder, LRR_FOLDER);

            Directory.CreateDirectory(_controller_result_dir);

            //init result params
            runDesc.ErrorDesc = errorReason;
            runDesc.TestPath = scenarioPath;
            ConsoleWriter.WriteLine(runDesc.TestPath);
            runDesc.TestState = TestState.Unknown;

            if (!Helper.isLoadRunnerInstalled())
            {
                runDesc.TestState = TestState.Error;
                runDesc.ErrorDesc = string.Format(Resources.LoadRunnerNotInstalled, System.Environment.MachineName);
                ConsoleWriter.WriteErrLine(runDesc.ErrorDesc);
                Environment.ExitCode = (int)Launcher.ExitCodeEnum.Failed;
                return runDesc;
            }

            //from here on, we may delegate runCancelled().
            _runCancelled = runCancelled;

            //start scenario stop watch
            Stopwatch scenarioStopWatch = Stopwatch.StartNew();

            //set state to running
            runDesc.TestState = TestState.Running;

            //and run the scenario
            bool res = runScenario(scenarioPath, ref errorReason, runCancelled);

            if (!res)
            {
                //runScenario failed. print the error and set test as failed
                ConsoleWriter.WriteErrLine(errorReason);
                runDesc.TestState = TestState.Error;
                runDesc.ErrorDesc = errorReason;
                runDesc.Runtime = scenarioStopWatch.Elapsed;

                //and try to close the controller
                closeController();
                return runDesc;
            }
            else
            {
                try
                {
                    ConsoleWriter.WriteLine(Resources.GeneralDoubleSeperator);
                    runDesc.ReportLocation = _resultsFolder;
                    ConsoleWriter.WriteLine(Resources.LrAnalysingResults);

                    //close the controller, so Analysis can be opened
                    ConsoleWriter.WriteLine("closing Controller");
                    closeController();
                    ConsoleWriter.WriteLine("Controller closed");

                    //generate report using Analysis:
                    ConsoleWriter.WriteLine("calling analysis report generator");
                    generateAnalysisReport(runDesc);
                    ConsoleWriter.WriteLine("analysis report generator finished");

                    //check for errors:
                    if (File.Exists(Path.Combine(_resultsFolder, "errors.xml")))
                    {
                        checkForErrors();
                    }

                    ConsoleWriter.WriteLine(Resources.LRErrorsSummary);

                    //count how many ignorable errors and how many fatal errors occured.
                    int ignore = getErrorsCount(ERRORState.Ignore);
                    int fatal = getErrorsCount(ERRORState.Error);
                    runDesc.FatalErrors = fatal;
                    ConsoleWriter.WriteLine(string.Format(Resources.LrErrorSummeryNum, ignore, fatal));
                    ConsoleWriter.WriteLine(string.Empty);
                    if (_errors != null && _errors.Count > 0)
                    {
                        foreach (ERRORState state in Enum.GetValues(typeof(ERRORState)))
                        {
                            ConsoleWriter.WriteLine(printErrorSummary(state));
                        }
                    }

                    //if scenario ended with fatal errors, change test state
                    if (fatal > 0)
                    {
                        ConsoleWriter.WriteErrLine(string.Format(Resources.LRTestFailDueToFatalErrors, fatal));
                        errorReason = buildErrorReasonForErrors();
                        runDesc.TestState = TestState.Failed;
                    }
                    else if (ignore > 0)
                    {
                        ConsoleWriter.WriteLine(string.Format(Resources.LRTestWarningDueToIgnoredErrors, ignore));
                        runDesc.HasWarnings = true;
                        runDesc.TestState = TestState.Warning;
                    }
                    else
                    {
                        Console.WriteLine(Resources.LRTestPassed);
                        runDesc.TestState = TestState.Passed;
                    }
                }
                catch (Exception e)
                {
                    ConsoleWriter.WriteException(Resources.LRExceptionInAnalysisRunner, e);
                    runDesc.TestState = TestState.Error;
                    runDesc.ErrorDesc = Resources.LRExceptionInAnalysisRunner;
                    runDesc.Runtime = scenarioStopWatch.Elapsed;
                }

                //runDesc.ReportLocation = _resultsFolder;
            }

            runDesc.Runtime = scenarioStopWatch.Elapsed;
            if (!string.IsNullOrEmpty(errorReason))
                runDesc.ErrorDesc = errorReason;
            KillController();
            return runDesc;
        }

        private string buildErrorReasonForErrors()
        {
            //ConsoleWriter.WriteLine("building Error ResultString");
            string res = Resources.LRErrorReasonSummaryTitle;
            res += printErrorSummary(ERRORState.Error);
            return res;
        }

        private string printErrorSummary(ERRORState state)
        {
            string res = "";
            List<string> validKeys = (from x in _errors where x.Value.state == state select x.Key).ToList<string>();
            if (validKeys.Count == 0)
            {
                return "";
            }

            res += state + " summary:\n";
            foreach (string errorString in validKeys)
            {
                res += (_errors[errorString].occurences + " : " + errorString) + "\n";
            }
            return res;
        }

        private bool runScenario(string scenario, ref string errorReason, RunCancelledDelegate runCancelled)
        {
            cleanENV();

            ConsoleWriter.WriteLine(string.Format(Resources.LrInitScenario, scenario));

            //start controller
            _engine = new LrEngine();
            if (_engine == null)
            {
                errorReason = string.Format(Resources.LrFailToOpenController, scenario);
                return false;
            }

            //try to register the end scenario event:
            _scenarioEndedEvent = false;
            try
            {
                _engine.Events.ScenarioEvents.OnScenarioEnded += ScenarioEvents_OnScenarioEnded;
                _scenarioEndedEvent = true;
            }
            catch (Exception e)
            {
                ConsoleWriter.WriteException(Resources.LrFailToRegisterEndScenarioEvent, e);
                _scenarioEndedEvent = false;
            }


            if (_displayController == true)
            {
                _engine.ShowMainWindow(1);
            }
            else
            {
                _engine.ShowMainWindow(0);
            }

            //pointer to the scenario object:
            LrScenario currentScenario = _engine.Scenario;

            //try to open the scenario and validate the scenario and connect to load generators
            if (openScenario(scenario, ref errorReason) && validateScenario(currentScenario, ref errorReason))
            {
                //apply rts to scripts
                foreach (ScriptRTSModel scriptRTS in _scriptRTSSet)
                {
                    try
                    {
                        LrScripts currentScripts = currentScenario.Scripts;
                        LrScript currentScript = currentScripts.Item[scriptRTS.GetScriptName()];
                        string runtimeSettings = "",
                               actionLogic = "";
                        currentScript.GetScriptRunTimeSettings(ref runtimeSettings, ref actionLogic);
                        RTSHelper rtsHelper = new RTSHelper(runtimeSettings, RTSHelper.COMMAND_ARGUMENTS, scriptRTS.GetKeyValuePairs());
                        string updatedRuntimeSettings = rtsHelper.GetUpdatedIniFileText();
                        currentScript.SetScriptRunTimeSettings(updatedRuntimeSettings, actionLogic);
                    }
                    catch (Exception e)
                    {
                        errorReason = string.Format(Resources.LrRTSError, scriptRTS.GetScriptName(), e.Message);
                        return false;
                    }
                }

                //set the result dir:
                ConsoleWriter.WriteLine("setting scenario result folder to " + Path.Combine(_resultsFolder, LRR_FOLDER));
                currentScenario.ResultDir = Path.Combine(_resultsFolder, LRR_FOLDER);
                ConsoleWriter.WriteLine("scenario result folder: " + currentScenario.ResultDir);

                //check if canceled or timedOut:
                if (_runCancelled())
                {
                    errorReason = Resources.GeneralTimedOut;
                    return false;
                }

                _scenarioEnded = false;

                ConsoleWriter.WriteLine(Resources.LrStartScenario);

                int ret = currentScenario.Start();

                if (!currentScenario.ResultDir.Equals(Path.Combine(_resultsFolder, LRR_FOLDER)))
                {
                    ConsoleWriter.WriteLine("controller failed to write to " + Path.Combine(_resultsFolder, LRR_FOLDER) + " setting result folder to " + currentScenario.ResultDir);
                    _controller_result_dir = new DirectoryInfo(currentScenario.ResultDir).Name;
                    ConsoleWriter.WriteLine("controller reult dir: " + _controller_result_dir);
                }

                if (ret != 0)
                {
                    errorReason = string.Format(Resources.LrStartScenarioFail, scenario, ret);
                    return false;
                }
                //per scenario timeout stopwatch
                _stopWatch = Stopwatch.StartNew();
                //wait for scenario to end:
                if (!waitForScenario(ref errorReason))
                {
                    //something went wrong during scenario execution, error reason set in errorReason string
                    return false;
                }
                else
                {//scenario has ended
                    Console.WriteLine(string.Format(Resources.LrScenarioEnded, scenario, _stopWatch.Elapsed.Hours, _stopWatch.Elapsed.Minutes, _stopWatch.Elapsed.Seconds));

                    //collate results
                    collateResults();
                }
            }
            else
            {
                return false;
            }

            return true;
        }

        private bool openScenario(string scenario, ref string errorReason)
        {
            int ret;
            try
            {
                ThreadStart tstart = () =>
                {
                    try
                    {
                        ret = _engine.Scenario.Open(scenario, false);
                        if (ret != 0)
                        {
                            throw new Exception(ret.ToString());
                        }
                    }
                    catch { }
                };
                Thread t = new Thread(tstart);
                t.Start();
                if (!t.Join(_pollingInterval * 1000 * 2))
                {
                    errorReason = "cannot open scenario - timeout!";
                    return false;
                }
            }
            catch (Exception e)
            {
                errorReason = string.Format(Resources.LrFailedToOpenScenario, scenario, int.Parse(e.Message));
                return false;
            }
            return true;
        }

        private void LogDataDuringScenarioExecution()
        {
            ThreadStart summaryDataLoggingThread = () =>
            {
                try
                {
                    LrScenario currentScenario = _engine.Scenario;

                    while (!_scenarioEnded)
                    {
                        _summaryDataLogger.LogSummaryData(currentScenario);
                        Thread.Sleep(_summaryDataLogger.GetPollingInterval());
                    }
                }
                catch (Exception e)
                {
                    ConsoleWriter.WriteErrLine(string.Format(Resources.LrSummaryDataLoggingError, e.Message));
                    return;
                }
            };
            Thread t = new Thread(summaryDataLoggingThread);
            t.Start();
        }

        private void generateAnalysisReport(TestRunResults runDesc)
        {
            string lrrLocation = Path.Combine(runDesc.ReportLocation, LRR_FOLDER, LRR_FOLDER + ".lrr");
            string lraLocation = Path.Combine(runDesc.ReportLocation, LRA_FOLDER, LRA_FOLDER + ".lra");
            string htmlLocation = Path.Combine(runDesc.ReportLocation, HTML_FOLDER, HTML_FOLDER + ".html");

            ProcessStartInfo analysisRunner = new ProcessStartInfo();
            analysisRunner.FileName = ANALYSIS_LAUNCHER;
            analysisRunner.Arguments = "\"" + lrrLocation + "\" \"" + lraLocation + "\" \"" + htmlLocation + "\" \"" + _analysisTemplate + "\"";
            analysisRunner.UseShellExecute = false;
            analysisRunner.RedirectStandardOutput = true;

            ConsoleWriter.WriteLine("executing Analysis launcher with arguments : " + analysisRunner.Arguments);
            ConsoleWriter.WriteLine("time for analysis: " + _perScenarioTimeOutMinutes.ToString(@"dd\:\:hh\:mm\:ss"));
            analysisRunner.RedirectStandardOutput = true;
            analysisRunner.RedirectStandardError = true;
            Process runner = Process.Start(analysisRunner);
            if (runner != null)
            {
                runner.OutputDataReceived += runner_OutputDataReceived;
                runner.ErrorDataReceived += runner_ErrorDataReceived;
                runner.BeginOutputReadLine();
                runner.BeginErrorReadLine();
                Stopwatch analysisStopWatch = Stopwatch.StartNew();

                while (!runner.WaitForExit(_pollingInterval * 1000) && analysisStopWatch.Elapsed < _perScenarioTimeOutMinutes) ;

                analysisStopWatch.Stop();
                runner.CancelOutputRead();
                runner.CancelErrorRead();
                ConsoleWriter.WriteLine("time passed: " + analysisStopWatch.Elapsed.ToString(@"dd\:\:hh\:mm\:ss"));
                if (analysisStopWatch.Elapsed > _perScenarioTimeOutMinutes)
                {
                    runDesc.ErrorDesc = Resources.LrAnalysisTimeOut;
                    ConsoleWriter.WriteErrLine(runDesc.ErrorDesc);
                    runDesc.TestState = TestState.Error;
                    if (!runner.HasExited)
                    {
                        runner.Kill();
                    }
                }
                //ConsoleWriter.WriteLine("checking error code");
                if (runner.ExitCode != (int)Launcher.ExitCodeEnum.Passed)
                {
                    runDesc.ErrorDesc = Resources.LrAnalysisRunTimeError;
                    ConsoleWriter.WriteErrLine(runDesc.ErrorDesc);
                    runDesc.TestState = TestState.Error;
                }
                //using (StreamReader reader = runner.StandardOutput)
                //{
                //    string result = reader.ReadToEnd();
                //    ConsoleWriter.WriteLine(Resources.LrAnlysisResults);
                //    ConsoleWriter.WriteLine("");
                //    ConsoleWriter.WriteLine(result);
                //}

            }
            else
            {
                runDesc.ErrorDesc = Resources.LrAnlysisInitFail;
                ConsoleWriter.WriteErrLine(runDesc.ErrorDesc);
                runDesc.TestState = TestState.Error;
            }

        }

        void runner_ErrorDataReceived(object sender, DataReceivedEventArgs errData)
        {
            if (!errData.Data.IsNullOrEmpty())
                ConsoleWriter.WriteErrLine(errData.Data);
        }

        void runner_OutputDataReceived(object sender, DataReceivedEventArgs outLine)
        {
            if (!outLine.Data.IsNullOrEmpty())
            {
                ConsoleWriter.WriteLine(outLine.Data);
            }
        }

        private void collateResults()
        {
            int ret = _engine.Scenario.CollateResults();
            if (ret != 0)
            {
                ConsoleWriter.WriteErrLine(string.Format(Resources.LrScenarioCollateFail, ret));
            }

            while (!_engine.Scenario.IsResultsCollated() && _stopWatch.Elapsed < _perScenarioTimeOutMinutes)
            {

                Stopper collateStopper = new Stopper(_pollingInterval * 1000);
                collateStopper.Start();
            }
        }

        private void closeController()
        {
            //try to gracefully shut down the controller
            if (_engine != null)
            {
                try
                {
                    var process = Process.GetProcessesByName("Wlrun");
                    if (process.Length > 0)
                    {
                        int rc = _engine.CloseController();
                        if (rc != 0)
                        {
                            ConsoleWriter.WriteErrLine(
                                "\t\tFailed to close Controller with CloseController API function, rc: " + rc);
                        }
                    }

                    //give the controller 15 secs to shutdown. otherwise, print an error.
                    Stopper controllerStopper = new Stopper(15000);
                    controllerStopper.Start();

                    if (_engine != null)
                    {

                        process = Process.GetProcessesByName("Wlrun");
                        if (process.Length > 0)
                        {
                            ConsoleWriter.WriteErrLine("\t\tThe Controller is still running...");
                            Stopper wlrunStopper = new Stopper(10000);
                            wlrunStopper.Start();
                            return;
                        }
                    }
                }
                catch (Exception e)
                {
                    ConsoleWriter.WriteErrLine("\t\t Cannot close Controller gracefully, exception details:");
                    ConsoleWriter.WriteErrLine(e.Message);
                    ConsoleWriter.WriteErrLine(e.StackTrace);
                    ConsoleWriter.WriteErrLine("killing Controller process");
                    cleanENV();
                }
            }
            _engine = null;
        }

        private void closeController_Kill()
        {
            //try to gracefully shut down the controller
            if (_engine != null)
            {
                try
                {
                    var process = Process.GetProcessesByName("Wlrun");
                    if (process.Length > 0)
                    {
                        int rc = _engine.CloseController();
                        if (rc != 0)
                        {
                            ConsoleWriter.WriteErrLine(
                                "\t\tFailed to close Controller with CloseController API function, rc: " + rc);
                        }
                    }

                    if (_engine != null)
                    {

                        process = Process.GetProcessesByName("Wlrun");
                        if (process.Length > 0)
                        {
                            ConsoleWriter.WriteErrLine("\t\tThe Controller is still running...");
                            Stopper wlrunStopper = new Stopper(10000);
                            wlrunStopper.Start();
                            KillController();
                            return;
                        }
                    }
                }
                catch (Exception e)
                {
                    ConsoleWriter.WriteErrLine("\t\t Cannot close Controller gracefully, exception details:");
                    ConsoleWriter.WriteErrLine(e.Message);
                    ConsoleWriter.WriteErrLine(e.StackTrace);
                    ConsoleWriter.WriteErrLine("killing Controller process");
                    cleanENV();
                }
            }
            _engine = null;
        }

        void DoTask(Object state)
        {
            AutoResetEvent are = (AutoResetEvent)state;

            while (!_scenarioEnded)
            {
                //Currently logging events causes controller scenario end event to unregister causing hptoolslauncher to be stuck
                if (!_scenarioEndedEvent || _summaryDataLogger.IsAnyDataLogged())
                {
                    //if all Vusers are in ending state, scenario is finished.
                    _scenarioEnded = isFinished();
                    Thread.Sleep(5000);
                }
            }
            are.Set();
        }

        AutoResetEvent autoEvent = new AutoResetEvent(false);

        private bool waitForScenario(ref string errorReason)
        {
            ConsoleWriter.WriteLine("Scenario run has started");

            ThreadPool.QueueUserWorkItem(DoTask, autoEvent);

            //wait for the scenario to end gracefully:
            int time = _pollingInterval * 1000;

            if (_summaryDataLogger.IsAnyDataLogged())
            {
                LogDataDuringScenarioExecution();
            }
            while (_stopWatch.Elapsed <= _perScenarioTimeOutMinutes)
            {
                if (_runCancelled())
                {
                    errorReason = Resources.GeneralTimedOut;
                    return false;
                }
                if (autoEvent.WaitOne(time))
                {
                    break;
                }
            }

            if (_stopWatch.Elapsed > _perScenarioTimeOutMinutes)
            {
                _stopWatch.Stop();
                errorReason = string.Format(Resources.LrScenarioTimeOut, _stopWatch.Elapsed.ToString("dd\\:hh\\:mm\\:ss"));
                ConsoleWriter.WriteErrLine(errorReason);
            }

            if (_scenarioEndedEvent)
            {
                try
                {
                    //ConsoleWriter.WriteLine("unregistering event");
                    _engine.Events.ScenarioEvents.OnScenarioEnded -= ScenarioEvents_OnScenarioEnded;
                    _scenarioEndedEvent = false;
                }
                catch { }
            }

            //if scenario not ended until now, force stop it.
            if (!_scenarioEnded)
            {
                int ret = _engine.Scenario.StopNow();
                if (ret != 0)
                {
                    errorReason = string.Format(Resources.LrStopScenarioEnded);
                    return false;
                }

                int tries = 2;
                while (_engine.Scenario.IsActive() && tries > 0)
                {
                    //ConsoleWriter.WriteLine("\t\tScenario is still running. Waiting for the scenario to stop...");
                    Stopper wlrunStopper = new Stopper(_pollingInterval * 1000);
                    wlrunStopper.Start();
                    tries--;
                }

                if (_engine.Scenario.IsActive())
                {
                    errorReason = Resources.LrControllerFailedToStop;
                    return false;
                }
            }

            return true;
        }

        private void checkForErrors()
        {
            //init variables
            string message = null;

            XmlDocument errorsXML = new XmlDocument();
            errorsXML.Load(Path.Combine(_resultsFolder, "errors.xml"));

            _errors = new Dictionary<string, ControllerError>();

            //new unseen error(s)
            foreach (XmlNode errorNode in errorsXML.DocumentElement.ChildNodes)
            {
                message = errorNode.InnerText;

                //ControllerError cerror;
                //if error exist, just update the count:
                bool added = false;
                //check if the error is ignorable
                foreach (string ignoreError in _ignoreErrorStrings)
                {
                    if (message.ToLowerInvariant().Contains(ignoreError.ToLowerInvariant()))
                    {
                        ConsoleWriter.WriteLine(string.Format(Resources.LrErrorIgnored, message, ignoreError));
                        if (_errors.ContainsKey(message))
                            _errors[message].occurences++;
                        else
                            _errors.Add(message, new ControllerError { state = ERRORState.Ignore, occurences = 1 });
                        added = true;
                        _errorsCount++;
                    }
                }

                //error was not ignored, and was not added yet. add it now.
                if (!added)
                {
                    // non ignorable error message,
                    ConsoleWriter.WriteErrLine(message);//+ ", time " + time + ", host: " + host + ", VuserID: " + vuserId + ", script: " + script + ", line: " + line);

                    if (_errors.ContainsKey(message))
                    {
                        _errors[message].occurences++;
                    }
                    else
                    {
                        _errors.Add(message, new ControllerError { state = ERRORState.Error, occurences = 1 });
                    }
                    _errorsCount++;
                    //if the scenario ended event was not registered, we need to provide the opportunity to check the vuser status.
                    //if (!_scenarioEndedEvent)
                    //{
                    break;
                    //}
                }

            }
            ConsoleWriter.WriteErrLine("total number of errors: " + _errorsCount);
        }

        private int getErrorsCount(ERRORState state)
        {
            return (_errors != null) ? (from x in _errors where x.Value.state == state select x.Value.occurences).Sum() : 0;
        }

        private void updateError(string message)
        {
            ControllerError s = _errors[message];
            if (s != null)
            {
                s.occurences++;
                _errors[message] = s;
            }
        }

        private void ScenarioEvents_OnScenarioEnded()
        {
            //ConsoleWriter.WriteLine("scenario ended event");
            _scenarioEnded = true;

        }

        private bool isFinished()
        {
            bool isFinished = false;
            try
            {
                updateVuserStatus();
            }
            catch
            {
                ConsoleWriter.WriteErrLine("Lost connection to Controller");
                return true;
            }

            isFinished = _vuserStatus[(int)VuserStatus.Down] == 0 &&
                         _vuserStatus[(int)VuserStatus.Pending] == 0 &&
                         _vuserStatus[(int)VuserStatus.Init] == 0 &&
                         _vuserStatus[(int)VuserStatus.Ready] == 0 &&
                         _vuserStatus[(int)VuserStatus.Run] == 0 &&
                         _vuserStatus[(int)VuserStatus.Rendez] == 0 &&
                         _vuserStatus[(int)VuserStatus.Exiting] == 0 &&
                         _vuserStatus[(int)VuserStatus.GradualExiting] == 0;

            return isFinished;
        }

        private void printVusersStatus()
        {
            Console.WriteLine("Vusers status:");
            string res = "";
            foreach (var val in Enum.GetValues(typeof(VuserStatus)))
            {
                res += ((VuserStatus)val) + ": " + _vuserStatus[(int)val] + " , ";
            }
            res = res.Substring(0, res.LastIndexOf(","));
            ConsoleWriter.WriteLine(res);
        }

        private void updateVuserStatus()
        {
            foreach (int val in Enum.GetValues(typeof(VuserStatus)))
            {
                _vuserStatus[val] = _engine.Scenario.GetVusersCount(val);
            }
        }

        private bool validateScenario(LrScenario scenario, ref string errorReason)
        {
            //validate that scenario has SLA
            if (!scenario.DoesScenarioHaveSLAConfiguration())
            {
                errorReason = string.Format(Resources.LrScenarioValidationFailNoSLA, scenario.FileName);
                return false;

            }
            //validate that all scripts are available.
            if (!scenario.AreScriptsAccessible(out errorReason))
            {
                //error message in errorReason
                return false;
            }

            //validate LGs:
            if (scenario.Hosts.Count == 0)
            {
                errorReason = string.Format(Resources.LrScenarioValidationFailNoLGs, scenario.FileName);
                return false;
            }

            //connect to all active load generators == hosts:
            int ret;
            foreach (LrHost lg in scenario.Hosts)
            {
                //handle only active hosts
                if (lg.IsUsed())
                {
                    //connect to the host
                    ret = lg.Connect();
                    if (ret != 0)
                    {
                        errorReason = string.Format(Resources.LrScenarioValidationCannotConnectLG, lg.Name);
                        return false;
                    }

                    //sync with the host
                    ret = lg.Sync(60);
                    if (ret <= 0)
                    {
                        errorReason = string.Format(Resources.LrScenarioValidationCannotSyncLG, lg.Name); ;
                        return false;
                    }

                    //if host is not ready after sync, invalidate the test
                    if (lg.Status != LrHostStatus.lrHostReady)
                    {
                        errorReason = string.Format(Resources.LrScenarioValidationLGNotReady, lg.Name);
                        return false;
                    }

                    //if we got this far, lg is connected and ready to go
                    ConsoleWriter.WriteLine(string.Format(Resources.LrScenarioValidationLGConnected, lg.Name));
                }
            }
            return true;
        }

        private void cleanENV()
        {
            ConsoleWriter.WriteLine(Resources.LrCleanENV);
            try
            {
                // check if any mdrv.exe process existed, kill them.
                var mdrvProcesses = Process.GetProcessesByName("mdrv");
                foreach (Process p in mdrvProcesses)
                {
                    p.Kill();
                    Stopper stopper = new Stopper(500);
                    stopper.Start();
                }

                // check if any wlrun.exe process existed, kill them.

                KillController();
            }
            catch (Exception)
            {

            }
        }

        private static void KillController()
        {
            var wlrunProcesses = Process.GetProcessesByName("Wlrun");
            if (wlrunProcesses.Length > 0)
            {
                foreach (Process p in wlrunProcesses)
                {
                    if (!p.HasExited)
                    {
                        p.Kill();
                        // When kill wlrun process directly, there might be a werfault.exe process generated, kill it if it appears.
                        DateTime nowTime = DateTime.Now;
                        while (DateTime.Now.Subtract(nowTime).TotalSeconds < 10)
                        {
                            var werFaultProcesses = Process.GetProcessesByName("WerFault");
                            if (werFaultProcesses.Length > 0)
                            {
                                //Console.WriteLine("Kill process of WerFault");
                                foreach (Process pf in werFaultProcesses)
                                {
                                    pf.Kill();
                                }
                                break;
                            }
                            Stopper werFaultProcessesStopper = new Stopper(2000);
                            werFaultProcessesStopper.Start();
                        }
                        Stopper wlrunStopper = new Stopper(2000);
                        wlrunStopper.Start();
                    }
                }
                ConsoleWriter.WriteLine("wlrun killed");
            }
        }

        public void CleanUp()
        {
            //ConsoleWriter.WriteLine("clenUp");

            //ConsoleWriter.WriteLine("Closing controller");
            closeController();
            cleanENV();
        }
        public void SafelyCancel()
        {
        }
    }
}
