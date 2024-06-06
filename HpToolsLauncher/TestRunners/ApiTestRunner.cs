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

using HpToolsLauncher.Properties;
using HpToolsLauncher.Utils;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Reflection;

namespace HpToolsLauncher
{
    public class ApiTestRunner : IFileSysTestRunner
    {
        public const string STRunnerName = "ServiceTestExecuter.exe";
        public const string STRunnerTestArg = @"-test";
        public const string STRunnerReportArg = @"-report";
        public const string STRunnerInputParamsArg = @"-inParams";
        public const string STRunnerEncodingArg = @"-encoding";
        private const int PollingTimeMs = 500;
        private bool _stCanRun;
        private string _stExecuterPath = Directory.GetCurrentDirectory();
        private readonly IAssetRunner _runner;
        private TimeSpan _timeout = TimeSpan.MaxValue;
        private Stopwatch _stopwatch = null;
        private RunCancelledDelegate _runCancelled;
        private string _encoding;
        private RunAsUser _uftRunAsUser;
        private bool _printInputParams;

        /// <summary>
        /// constructor
        /// </summary>
        /// <param name="runner">parent runner</param>
        /// <param name="timeout">the global timout</param>
        public ApiTestRunner(IAssetRunner runner, TimeSpan timeout, string encoding, bool printInputParams, RunAsUser uftRunAsUser)
        {
            _stopwatch = Stopwatch.StartNew();
            _timeout = timeout;
            _stCanRun = TrySetSTRunner();
            _runner = runner;
            _encoding = encoding;
            _printInputParams = printInputParams;
            _uftRunAsUser = uftRunAsUser;
        }

        /// <summary>
        /// Search ServiceTestExecuter.exe in the current running process directory,
        /// and if not found, in the installation folder (taken from registry)
        /// </summary>
        /// <returns></returns>
        public bool TrySetSTRunner()
        {
            if (File.Exists(STRunnerName))
                return true;
            _stExecuterPath = Helper.GetSTInstallPath();
            if (!string.IsNullOrEmpty(_stExecuterPath))
            {
                _stExecuterPath += "bin";
                return true;
            }
            _stCanRun = false;
            return false;
        }

        /// <summary>
        /// runs the given test
        /// </summary>
        /// <param name="testinf"></param>
        /// <param name="errorReason"></param>
        /// <param name="runCancelled">cancellation delegate, holds the function that checks cancellation</param>
        /// <returns></returns>
        public TestRunResults RunTest(TestInfo testinf, ref string errorReason, RunCancelledDelegate runCancelled, out Dictionary<string, string> outParams)
        {
            outParams = new Dictionary<string, string>();
            TestRunResults runDesc = new TestRunResults { TestType = TestType.ST };
            ConsoleWriter.ActiveTestRun = runDesc;
            ConsoleWriter.WriteLineWithTime("Running: " + testinf.TestPath);

            runDesc.TestPath = testinf.TestPath;

            // default report location is the test path
            runDesc.ReportLocation = testinf.TestPath;

            // check if the report path has been defined
            if (!string.IsNullOrEmpty(testinf.ReportPath))
            {
                if (!Helper.TrySetTestReportPath(runDesc, testinf, ref errorReason))
                {
                    return runDesc;
                }
            }

            runDesc.ErrorDesc = errorReason;
            runDesc.TestState = TestState.Unknown;
            if (!Helper.IsServiceTestInstalled())
            {
                runDesc.TestState = TestState.Error;
                runDesc.ErrorDesc = string.Format(Resources.LauncherStNotInstalled, System.Environment.MachineName);
                ConsoleWriter.WriteErrLine(runDesc.ErrorDesc);
                Environment.ExitCode = (int)Launcher.ExitCodeEnum.Failed;
                return runDesc;
            }

            _runCancelled = runCancelled;
            if (!_stCanRun)
            {
                runDesc.TestState = TestState.Error;
                runDesc.ErrorDesc = Resources.STExecuterNotFound;
                return runDesc;
            }
            string fileName = Path.Combine(_stExecuterPath, STRunnerName);

            if (!File.Exists(fileName))
            {
                runDesc.TestState = TestState.Error;
                runDesc.ErrorDesc = Resources.STExecuterNotFound;
                ConsoleWriter.WriteErrLine(Resources.STExecuterNotFound);
                return runDesc;
            }

            //write the input parameter xml file for the API test
            string paramFileName = Guid.NewGuid().ToString().Replace("-", string.Empty).Substring(0, 10);
            string tempPath = Path.Combine(Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location), "TestParams");
            Directory.CreateDirectory(tempPath);
            string paramsFilePath = Path.Combine(tempPath, "params" + paramFileName + ".xml");

            Dictionary<string, object> paramDict;
            try
            {
                paramDict = testinf.GetParameterDictionaryForQTP();
            }
            catch (ArgumentException)
            {
                ConsoleWriter.WriteErrLine(string.Format(Resources.FsDuplicateParamNames));
                throw;
            }

            string paramFileContent = testinf.GenerateAPITestXmlForTest(paramDict, _printInputParams);

            string argumentString;
            if (!string.IsNullOrWhiteSpace(paramFileContent))
            {
                File.WriteAllText(paramsFilePath, paramFileContent);
                argumentString = string.Format("{0} \"{1}\" {2} \"{3}\" {4} \"{5}\" {6} {7}", STRunnerTestArg, testinf.TestPath, STRunnerReportArg, runDesc.ReportLocation, STRunnerInputParamsArg, paramsFilePath, string.IsNullOrWhiteSpace(_encoding) ? string.Empty : STRunnerEncodingArg, _encoding);
            }
            else
            {
                argumentString = string.Format("{0} \"{1}\" {2} \"{3}\" {4} {5}", STRunnerTestArg, testinf.TestPath, STRunnerReportArg, runDesc.ReportLocation, string.IsNullOrWhiteSpace(_encoding) ? string.Empty : STRunnerEncodingArg, _encoding);
            }

            Stopwatch s = Stopwatch.StartNew();
            runDesc.TestState = TestState.Running;

            if (!ExecuteProcess(fileName, argumentString, ref errorReason))
            {
                runDesc.TestState = TestState.Error;
                runDesc.ErrorDesc = errorReason;
            }
            else
            {
                runDesc.ReportLocation = Path.Combine(runDesc.ReportLocation, "Report");
                if (!File.Exists(Path.Combine(runDesc.ReportLocation, "Results.xml")) && !File.Exists(Path.Combine(runDesc.ReportLocation, "run_results.html")))
                {
                    runDesc.TestState = TestState.Error;
                    runDesc.ErrorDesc = "No Results.xml or run_results.html file found";
                }
            }
            //File.Delete(paramsFilePath);
            runDesc.Runtime = s.Elapsed;
            return runDesc;
        }

        /// <summary>
        /// performs global cleanup code for this type of runner
        /// </summary>
        public void CleanUp()
        {
        }

        public void SafelyCancel()
        {
        }

        #region Process

        /// <summary>
        /// executes the run of the test by using the Init and RunProcss routines
        /// </summary>
        /// <param name="proc"></param>
        /// <param name="fileName"></param>
        /// <param name="arguments"></param>
        /// <param name="enableRedirection"></param>
        private bool ExecuteProcess(string fileName, string arguments, ref string failureReason)
        {
            Process proc = null;
            try
            {
                using (proc = new Process())
                {
                    InitProcess(proc, fileName, arguments, true);
                    RunProcess(proc, true);

                    //it could be that the process already exited before we could handle the cancel request
                    if (_runCancelled())
                    {
                        ConsoleWriter.WriteLine(Resources.GeneralTimeoutExpired);

                        if (!proc.HasExited)
                        {
                            proc.OutputDataReceived -= OnOutputDataReceived;
                            proc.ErrorDataReceived -= OnErrorDataReceived;
                            proc.Kill();
                            return false;
                        }
                    }
                    if (proc.ExitCode != 0)
                    {
                        failureReason = "The Api test runner's exit code was: " + proc.ExitCode;
                        ConsoleWriter.WriteLine(failureReason);
                        return false;
                    }
                }
            }
            catch (Exception e)
            {
                failureReason = e.Message;
                return false;
            }
            finally
            {
                if (proc != null)
                {
                    proc.Close();
                }
            }

            return true;
        }

        /// <summary>
        /// initializes the ServiceTestExecuter process
        /// </summary>
        /// <param name="proc"></param>
        /// <param name="fileName"></param>
        /// <param name="arguments"></param>
        /// <param name="enableRedirection"></param>
        private void InitProcess(Process proc, string fileName, string arguments, bool enableRedirection)
        {
            var procStartInfo = new ProcessStartInfo
            {
                FileName = fileName,
                Arguments = arguments,
                WorkingDirectory = Directory.GetCurrentDirectory()
            };
            if (_uftRunAsUser != null)
            {
                procStartInfo.UserName = _uftRunAsUser.Username;
                procStartInfo.Password = _uftRunAsUser.Password;
            }

            Console.WriteLine("{0} {1}", STRunnerName, arguments);

            if (!enableRedirection) return;

            procStartInfo.ErrorDialog = false;
            procStartInfo.UseShellExecute = false;
            procStartInfo.RedirectStandardOutput = true;
            procStartInfo.RedirectStandardError = true;

            proc.StartInfo = procStartInfo;

            proc.EnableRaisingEvents = true;
            proc.StartInfo.CreateNoWindow = true;

            proc.OutputDataReceived += OnOutputDataReceived;
            proc.ErrorDataReceived += OnErrorDataReceived;
        }

        /// <summary>
        /// runs the ServiceTestExecuter process after initialization
        /// </summary>
        /// <param name="proc"></param>
        /// <param name="enableRedirection"></param>
        private void RunProcess(Process proc, bool enableRedirection)
        {
            proc.Start();
            if (enableRedirection)
            {
                proc.BeginOutputReadLine();
                proc.BeginErrorReadLine();
            }
            proc.WaitForExit(PollingTimeMs);
            while (!_runCancelled() && !proc.HasExited)
            {
                proc.WaitForExit(PollingTimeMs);
            }
        }

        /// <summary>
        /// callback function for spawnd process errors
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void OnErrorDataReceived(object sender, DataReceivedEventArgs e)
        {
            var p = sender as Process;

            if (p == null) return;
            try
            {
                if (!p.HasExited || p.ExitCode == 0) return;
            }
            catch { return; }

            string errorData = e.Data;

            if (string.IsNullOrEmpty(errorData))
            {
                errorData = string.Format("External process has exited with code {0}", p.ExitCode);
            }

            ConsoleWriter.WriteErrLine(errorData);
        }

        /// <summary>
        /// callback function for spawnd process output
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void OnOutputDataReceived(object sender, DataReceivedEventArgs e)
        {
            if (!string.IsNullOrEmpty(e.Data))
            {
                string data = e.Data;
                ConsoleWriter.WriteLine(data);
            }
        }

        #endregion

        public static bool VerifyParameterValueType(object paramValue, string type)
        {
            bool legal = false;

            switch (type)
            {
                case "boolean":
                    legal = paramValue is bool;
                    break;

                case "dateTime":
                    legal = paramValue is DateTime;
                    break;

                case "int":
                case "long":
                    legal = ((paramValue is int) || (paramValue is long));
                    break;

                case "float":
                case "double":
                case "decimal":
                    legal = ((paramValue is decimal) || (paramValue is float) || (paramValue is double));
                    break;

                case "string":
                    legal = paramValue is string;
                    break;

                default:
                    legal = false;
                    break;
            }

            return legal;
        }

    }
}