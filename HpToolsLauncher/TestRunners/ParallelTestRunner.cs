/*
 * Certain versions of software accessible here may contain branding from Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 * This software was acquired by Micro Focus on September 1, 2017, and is now offered by OpenText.
 * Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright 2012-2024 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ___________________________________________________________________
 */

using HpToolsLauncher.ParallelRunner;
using HpToolsLauncher.Utils;
using QTObjectModelLib;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using Environment = System.Environment;
using Resources = HpToolsLauncher.Properties.Resources;
namespace HpToolsLauncher.TestRunners
{
    /// <summary>
    /// The ParallelTestRunner class
    /// Contains all methods for running a file system test using parallel runner.
    /// </summary>
    class ParallelTestRunner : IFileSysTestRunner
    {
        // each test has a list of environments that it will run on
        private readonly Dictionary<string, List<string>> _environments;
        private readonly IAssetRunner _runner;
        private readonly McConnectionInfo _mcConnectionInfo;
        private const string ParallelRunnerExecutable = "ParallelRunner.exe";
        private string _parallelRunnerPath;
        private RunCancelledDelegate _runCancelled;
        private const int PollingTimeMs = 500;
        private readonly bool _canRun = false;
        private const string ParallelRunnerArguments = "-o static -c \"{0}\"";

        private List<string> _configFiles = new List<string>();
        private IProcessAdapter _processAdapter;
        private readonly Type _qtType = Type.GetTypeFromProgID("Quicktest.Application");
        private RunAsUser _uftRunAsUser;

        public ParallelTestRunner(IAssetRunner runner, McConnectionInfo mcConnectionInfo, Dictionary<string, List<string>> environments, RunAsUser uftRunAsUser)
        {
            _runner = runner;
            _mcConnectionInfo = mcConnectionInfo;
            _environments = environments;
            _canRun = TrySetupParallelRunner();
            _uftRunAsUser = uftRunAsUser;
        }

        /// <summary>
        /// Tries to find the parallel runner executable.
        /// </summary>
        /// <returns>
        /// True if the executable has been found,False otherwise
        /// </returns>
        private bool TrySetupParallelRunner()
        {
            _parallelRunnerPath = Helper.GetParallelRunnerDirectory(ParallelRunnerExecutable);

            ConsoleWriter.WriteLine("Attempting to start parallel runner from: " + _parallelRunnerPath);

            return _parallelRunnerPath != null && File.Exists(_parallelRunnerPath);
        }

        /// <summary>
        /// Set the test run results based on the parallel runner exit code.
        /// </summary>
        /// <param name="runResults"></param>
        /// <param name="exitCode"></param>
        /// <param name="failureReason"></param>
        /// <param name="errorReason"></param>
        private void RunResultsFromParallelRunnerExitCode(TestRunResults runResults, int exitCode, string failureReason, ref string errorReason)
        {
            // set the status of the build based on the exit code
            switch (exitCode)
            {
                case (int)ParallelRunResult.Pass:
                    runResults.TestState = TestState.Passed;
                    break;
                case (int)ParallelRunResult.Warning:
                    runResults.TestState = TestState.Warning;
                    break;
                case (int)ParallelRunResult.Fail:
                    runResults.ErrorDesc = "ParallelRunner test has FAILED!";
                    runResults.TestState = TestState.Failed;
                    break;
                case (int)ParallelRunResult.Cancelled:
                    runResults.ErrorDesc = "ParallelRunner was stopped since job has timed out!";
                    ConsoleWriter.WriteErrLine(runResults.ErrorDesc);
                    runResults.TestState = TestState.Error;
                    break;
                case (int)ParallelRunResult.Error:
                    errorReason = failureReason;
                    runResults.ErrorDesc = errorReason;
                    ConsoleWriter.WriteErrLine(runResults.ErrorDesc);
                    runResults.TestState = TestState.Error;
                    break;
                default:
                    ConsoleWriter.WriteErrLine(errorReason);
                    runResults.ErrorDesc = errorReason;
                    runResults.TestState = TestState.Error;
                    break;
            }
        }

        /// <summary>
        /// Runs the provided test on all the environments.
        /// </summary>
        /// <param name="testInfo"> The test information. </param>
        /// <param name="errorReason"> failure reason </param>
        /// <param name="runCancelled"> delegate to RunCancelled </param>
        /// <returns>
        /// The run results for the current test.
        /// </returns>
        public TestRunResults RunTest(TestInfo testInfo, ref string errorReason, RunCancelledDelegate runCancelled, out Dictionary<string, string> outParams)
        {
            outParams = new Dictionary<string, string>();

            testInfo.ReportPath = testInfo.TestPath + @"\ParallelReport";

            // this is to make sure that we do not overwrite the report
            // when we run the same test multiple times on the same build
            string resFolder = Helper.GetNextResFolder(testInfo.ReportPath, "Res");

            var runResults = new TestRunResults
            {
                ReportLocation = testInfo.ReportPath,
                ErrorDesc = errorReason,
                TestState = TestState.Unknown,
                TestPath = testInfo.TestPath,
                TestType = TestType.ParallelRunner
            };

            // set the active test run
            ConsoleWriter.ActiveTestRun = runResults;

            if (!_canRun)
            {
                ConsoleWriter.WriteLine("Could not find parallel runner executable!");
                errorReason = Resources.ParallelRunnerExecutableNotFound;
                runResults.TestState = TestState.Error;
                runResults.ErrorDesc = errorReason;
                return runResults;
            }

            // Try to create the ParalleReport path
            try
            {
                Directory.CreateDirectory(runResults.ReportLocation);
            }
            catch (Exception)
            {
                errorReason = string.Format(Resources.FailedToCreateTempDirError, runResults.ReportLocation);
                runResults.TestState = TestState.Error;
                runResults.ErrorDesc = errorReason;

                Environment.ExitCode = (int)Launcher.ExitCodeEnum.Failed;
                return runResults;
            }

            ConsoleWriter.WriteLineWithTime("Using ParallelRunner to execute test: " + testInfo.TestPath);

            _runCancelled = runCancelled;

            // prepare the json file for the process
            string configFilePath;

            try
            {
                configFilePath = ParallelRunnerEnvironmentUtil.GetConfigFilePath(testInfo, _mcConnectionInfo, _environments);
                _configFiles.Add(configFilePath);
            }
            catch (ParallelRunnerConfigurationException ex) // invalid configuration
            {
                errorReason = ex.Message;
                runResults.ErrorDesc = errorReason;
                runResults.TestState = TestState.Error;
                return runResults;
            }

            // Parallel runner argument "-c" for config path and "-o static" so that
            // the output from ParallelRunner is compatible with Jenkins
            var arguments = string.Format(ParallelRunnerArguments, configFilePath);

            // the test can be started now
            runResults.TestState = TestState.Running;

            var runTime = new Stopwatch();
            runTime.Start();

            string failureReason = null;
            runResults.ErrorDesc = null;

            // execute parallel runner and get the run result status
            int exitCode = ExecuteProcess(arguments, ref failureReason);

            // set the status of the build based on the exit code
            RunResultsFromParallelRunnerExitCode(runResults, exitCode, failureReason, ref errorReason);

            // update the run time
            runResults.Runtime = runTime.Elapsed;

            // update the report location as the report should be 
            // generated by now
            runResults.ReportLocation = resFolder;

            return runResults;
        }

        public void CleanUp()
        {
            // DONT'T remove the json files from _configFiles as they are useful for troubleshooting
        }

        private void CloseUft()
        {
            try
            {
                var qtpApplication = Activator.CreateInstance(_qtType) as Application;

                //if the app is running, close it.
                if (qtpApplication.Launched)
                {
                    qtpApplication.Quit();
                }
            }
            catch
            {
                //nothing to do. (cleanup code should not throw exceptions, and there is no need to log this as an error in the test)
            }
        }

        public void SafelyCancel()
        {
            ConsoleWriter.WriteLine(Resources.GeneralStopAborted);
            CloseUft();
            if (_processAdapter != null && !_processAdapter.HasExited)
            {
                try
                {
                    _processAdapter.Close();
                }
                catch
                {
                    _processAdapter.Kill();
                }
            }
            CleanUp();
            ConsoleWriter.WriteLine(Resources.GeneralAbortedByUser);
        }

        #region Process

        /// <summary>
        /// Check if the parent of the current process is running in the user session.
        /// </summary>
        /// <returns>true if the parent process is running in the user session, false otherwise.</returns>
        private bool IsParentProcessRunningInUserSession()
        {
            Process currentProcess = Process.GetCurrentProcess();
            Process parentProcess = currentProcess.Parent();

            Process[] explorers;
            try
            {
                explorers = Process.GetProcessesByName("explorer");
            }
            catch (InvalidOperationException)
            {
                // try to start the process from the current session
                return true;
            }

            // could not retrieve the explorer process
            if (explorers == null || explorers.Length == 0)
            {
                // try to start the process from the current session
                return true;
            }

            // if they are not in the same session we will assume it is a service
            return explorers.Where(p => p.SessionId == parentProcess.SessionId).Any();
        }

        /// <summary>
        /// Return the corresponding process object based on the type of jenkins instance.
        /// </summary>
        /// <param name="fileName">the filename to be ran</param>
        /// <param name="arguments">the arguments for the process</param>
        /// <returns>the corresponding process type, based on the jenkins instance</returns>
        private object GetProcessTypeForCurrentSession(string fileName, string arguments)
        {
            try
            {
                //print command line 
                ConsoleWriter.WriteLineWithTime(string.Format("{0} {1}", fileName, arguments));

                if (IsParentProcessRunningInUserSession())
                {
                    return InitProcess(fileName, arguments);
                }

                if (_uftRunAsUser != null)
                {
                    ConsoleWriter.WriteLine("Starting ParallelRunner as different user from service session is not supported at this moment.");
                    return null;
                }
                ConsoleWriter.WriteLine("Starting ParallelRunner from service session!");

                // the process must be started in the user session
                return new ElevatedProcess(fileName, arguments, Helper.GetSTInstallPath());
            }
            catch (Exception)
            {
                return null;
            }
        }

        /// <summary>
        /// executes the run of the test by using the Init and RunProcss routines
        /// </summary>
        /// <param name="arguments">the arguments for the process</param>
        /// <param name="failureReason"> the reason why the process failed </param>
        /// <returns> the exit code of the process </returns>
        private int ExecuteProcess(string arguments, ref string failureReason)
        {
            _processAdapter = ProcessAdapterFactory.CreateAdapter(GetProcessTypeForCurrentSession(_parallelRunnerPath, arguments));

            if (_processAdapter == null)
            {
                failureReason = "Could not create ProcessAdapter instance!";
                return (int)ParallelRunResult.Error;
            }

            try
            {
                int exitCode = RunProcess(_processAdapter);

                if (_runCancelled())
                {
                    if (!_processAdapter.HasExited)
                    {
                        _processAdapter.Kill();
                        return (int)ParallelRunResult.Cancelled;
                    }
                }

                return exitCode;
            }
            catch (Exception e)
            {
                failureReason = e.Message;
                return (int)ParallelRunResult.Error;
            }
            finally
            {
                if (_processAdapter != null)
                {
                    _processAdapter.Close();
                }
            }
        }

        /// <summary>
        /// Initializes the ParallelRunner process
        /// </summary>
        /// <param name="fileName">the file name</param>
        /// <param name="arguments"> the process arguments </param>
        private Process InitProcess(string fileName, string arguments)
        {
            var info = new ProcessStartInfo
            {
                FileName = fileName,
                Arguments = arguments,
                WorkingDirectory = Directory.GetCurrentDirectory(),
                WindowStyle = ProcessWindowStyle.Hidden
            };
            if (_uftRunAsUser != null)
            {
                info.UserName = _uftRunAsUser.Username;
                info.Password = _uftRunAsUser.Password;
                info.UseShellExecute = false;
                info.RedirectStandardOutput = true;
                info.RedirectStandardError = true;
            }

            Process p = new Process { StartInfo = info };
            p.ErrorDataReceived += (sender, e) => { if (!string.IsNullOrEmpty(e.Data)) Console.Error.WriteLine(e.Data); };
            p.OutputDataReceived += (sender, e) => { if (!string.IsNullOrEmpty(e.Data)) Console.Out.WriteLine(e.Data); };
            return p;
        }

        /// <summary>
        /// runs the ParallelRunner process after initialization
        /// </summary>
        /// <param name="proc"></param>
        /// <param name="enableRedirection"></param>
        private int RunProcess(IProcessAdapter proc)
        {
            proc.Start();

            proc.WaitForExit(PollingTimeMs);

            while (!_runCancelled() && !proc.HasExited)
            {
                proc.WaitForExit(PollingTimeMs);
            }

            return proc.ExitCode;
        }

        #endregion
    }

    public enum ParallelRunResult : int
    {
        Pass = 1004,
        Warning = 1005,
        Fail = 1006,
        Cancelled = 1007,
        Error = 1008,
    }
}