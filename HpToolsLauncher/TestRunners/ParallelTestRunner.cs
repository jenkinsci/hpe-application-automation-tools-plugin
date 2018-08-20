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
 * © Copyright 2012-2018 Micro Focus or one of its affiliates.
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

using HpToolsLauncher.ParallelRunner;
using Microsoft.Win32;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices;
using System.Threading;
using System.Web.Script.Serialization;
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
        private TimeSpan _timeout;
        private readonly McConnectionInfo _mcConnectionInfo;
        private readonly string _mobileInfo;
        private const string ParallelRunnerExecutable = "ParallelRunner.exe";
        private string _parallelRunnerPath;
        private RunCancelledDelegate _runCancelled;
        private const int PollingTimeMs = 500;
        private readonly bool _canRun = false;
        private const string ParallelRunnerArguments = "-o static -c \"{0}\"";

        private List<string> _configFiles = new List<string>();

        public ParallelTestRunner(IAssetRunner runner, TimeSpan timeout, McConnectionInfo mcConnectionInfo,
            string mobileInfo, Dictionary<string, List<string>> environments)
        {
            _runner = runner;
            _timeout = timeout;
            _mcConnectionInfo = mcConnectionInfo;
            _mobileInfo = mobileInfo;
            _environments = environments;
            _canRun = TrySetupParallelRunner();
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
                case (int)ParallelRunResult.Canceled:
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
        public TestRunResults RunTest(TestInfo testInfo, ref string errorReason, RunCancelledDelegate runCancelled)
        {
            // change the DCOM setting for qtp application
            Helper.ChangeDCOMSettingToInteractiveUser();

            testInfo.ReportPath = testInfo.TestPath + @"\ParallelReport";

            // this is to make sure that we do not overwrite the report
            // when we run the same test multiple times on the same build
            string resFolder = Helper.GetNextResFolder(testInfo.ReportPath);

            var runResults = new TestRunResults
            {
                ReportLocation = testInfo.ReportPath,
                ErrorDesc = errorReason,
                TestState = TestState.Unknown,
                TestPath = testInfo.TestPath,
                TestType = TestType.ParallelRunner.ToString()
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
            }catch(Exception)
            {
                errorReason = string.Format(Resources.FailedToCreateTempDirError, runResults.ReportLocation);
                runResults.TestState = TestState.Error;
                runResults.ErrorDesc = errorReason;

                Environment.ExitCode = (int)Launcher.ExitCodeEnum.Failed;
                return runResults;
            }

            ConsoleWriter.WriteLine(DateTime.Now.ToString(Launcher.DateFormat) + " => Using ParallelRunner to execute test: " + testInfo.TestPath);

            _runCancelled = runCancelled;

            // prepare the json file for the process
            var configFilePath = string.Empty;

            try
            {
                configFilePath =  ParallelRunnerEnvironmentUtil.GetConfigFilePath(testInfo,_mcConnectionInfo,_environments);
                _configFiles.Add(configFilePath);
            }catch(ParallelRunnerConfigurationException ex) // invalid configuration
            {
                errorReason = ex.Message;
                runResults.ErrorDesc = errorReason;
                runResults.TestState = TestState.Error;
                return runResults;
            }

            // Parallel runner argument "-c" for config path and "-o static" so that
            // the output from ParallelRunner is compatible with Jenkins
            var arguments = String.Format(ParallelRunnerArguments, configFilePath);

            // the test can be started now
            runResults.TestState = TestState.Running;

            var runTime = new Stopwatch();
            runTime.Start();
            
            string failureReason = null;
            runResults.ErrorDesc = null;

            // execute parallel runner and get the run result status
            int exitCode = ExecuteProcess(_parallelRunnerPath, arguments, ref failureReason);

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
            // we need to remove the json config files as they are no longer needed
            foreach(var configFile in _configFiles)
            {
                try
                {
                    File.Delete(configFile);
                }
                catch (Exception) {
                    ConsoleWriter.WriteErrLine("Unable to remove configuration file: " + configFile);
                }
            }
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

            // if they are not in the same session we will assume it is a service
            Process explorer = null;
            try
            {
                explorer = Process.GetProcessesByName("explorer").FirstOrDefault();
            }
            catch (InvalidOperationException)
            {
                return false;
            }

            // could not retrieve the explorer process
            if(explorer == null)
            {
                // try to start the process from the current session
                return false;
            }

            return parentProcess.SessionId != explorer.SessionId;
        }

        /// <summary>
        /// Return the corresponding process object based on the type of jenkins instance.
        /// </summary>
        /// <param name="fileName">the filename to be ran</param>
        /// <param name="arguments">the arguments for the process</param>
        /// <returns>the corresponding process type, based on the jenkins instance</returns>
        private object GetProcessTypeForCurrentSession(string fileName,string arguments)
        {
            try
            {
                if (!IsParentProcessRunningInUserSession())
                {
                    Process process = new Process();

                    InitProcess(process, fileName, arguments);

                    return process;
                }

                ConsoleWriter.WriteLine("Starting ParallelRunner from service session!");

                // the process must be started in the user session
                ElevatedProcess elevatedProcess = new ElevatedProcess(fileName, arguments, Helper.GetSTInstallPath());
                return elevatedProcess;
            }
            catch (Exception)
            {
                return null;
            }
        }

        /// <summary>
        /// executes the run of the test by using the Init and RunProcss routines
        /// </summary>
        /// <param name="fileName">the prcess file name</param>
        /// <param name="arguments">the arguments for the process</param>
        /// <param name="failureReason"> the reason why the process failed </param>
        /// <returns> the exit code of the process </returns>
        private int ExecuteProcess(string fileName, string arguments, ref string failureReason)
        {
            IProcessAdapter processAdapter = ProcessAdapterFactory.CreateAdapter(GetProcessTypeForCurrentSession(fileName, arguments));

            if (processAdapter == null)
            {
                failureReason = "Could not create ProcessAdapter instance!";
                return (int)ParallelRunResult.Error;
            }

            try
            {
                int exitCode = RunProcess(processAdapter);

                if (_runCancelled())
                {
                    if (!processAdapter.HasExited)
                    {
                        processAdapter.Kill();
                        return (int)ParallelRunResult.Canceled;
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
                if (processAdapter != null)
                {
                    processAdapter.Close();
                }
            }
        }

        /// <summary>
        /// Initializes the ParallelRunner process
        /// </summary>
        /// <param name="proc"> the process </param>
        /// <param name="fileName">the file name</param>
        /// <param name="arguments"> the process arguments </param>
        private void InitProcess(Process proc, string fileName, string arguments)
        {
            var processStartInfo = new ProcessStartInfo
            {
                FileName = fileName,
                Arguments = arguments,
                WorkingDirectory = Directory.GetCurrentDirectory(),
                WindowStyle = ProcessWindowStyle.Hidden
            };

            proc.StartInfo = processStartInfo;
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
        Canceled = 1007,
        Error = 1008,
    }
}