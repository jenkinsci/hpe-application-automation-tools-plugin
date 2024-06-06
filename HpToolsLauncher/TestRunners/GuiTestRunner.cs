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

using HpToolsLauncher.TestRunners;
using HpToolsLauncher.Utils;
using Microsoft.Win32;
using QTObjectModelLib;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices;
using System.Threading;
using Resources = HpToolsLauncher.Properties.Resources;
using AuthType = HpToolsLauncher.McConnectionInfo.AuthType;
using DigitalLabType = HpToolsLauncher.McConnectionInfo.DigitalLabType;

namespace HpToolsLauncher
{
    public class GuiTestRunner : IFileSysTestRunner
    {
        // Setting keys for mobile
        private const string MC_TYPE = "MobileCenterType";
        private const string MOBILE_HOST_ADDRESS = "ALM_MobileHostAddress";
        private const string MOBILE_HOST_PORT = "ALM_MobileHostPort";
        private const string MOBILE_USER = "ALM_MobileUserName";
        private const string MOBILE_PASSWORD = "ALM_MobilePassword";
        private const string MOBILE_CLIENTID = "EXTERNAL_MobileClientID";
        private const string MOBILE_SECRET = "EXTERNAL_MobileSecretKey";
        private const string MOBILE_AUTH_TYPE = "EXTERNAL_MobileAuthType";
        private const string MOBILE_TENANT = "EXTERNAL_MobileTenantId";
        private const string MOBILE_USE_SSL = "ALM_MobileUseSSL";
        private const string MOBILE_USE_PROXY = "EXTERNAL_MobileProxySetting_UseProxy";
        private const string MOBILE_PROXY_SETTING = "EXTERNAL_MobileProxySetting";
        private const string MOBILE_PROXY_SETTING_ADDRESS = "EXTERNAL_MobileProxySetting_Address";
        private const string MOBILE_PROXY_SETTING_PORT = "EXTERNAL_MobileProxySetting_Port";
        private const string MOBILE_PROXY_SETTING_AUTHENTICATION = "EXTERNAL_MobileProxySetting_Authentication";
        private const string MOBILE_PROXY_SETTING_USERNAME = "EXTERNAL_MobileProxySetting_UserName";
        private const string MOBILE_PROXY_SETTING_PASSWORD = "EXTERNAL_MobileProxySetting_Password";
        private const string MOBILE_INFO = "mobileinfo";
        private const string REPORT = "Report";
        private const string READY = "Ready";
        private const string WAITING = "Waiting";
        private const string BUSY = "Busy";
        private const string RUNNING = "Running";
        private const string PASSED = "Passed";
        private const string WARNING = "Warning";
        private const int MEMBER_NOT_FOUND = -2147352573;
        private const string PROTECT_BstrToBase64_FAILED = "ProtectBSTRToBase64 failed for {0}.";
        private const string WEB = "Web";
        private const string CLOUD_BROWSER = "CloudBrowser";
        private const string SYSTEM_PROXY = "System Proxy";
        private const string HTTP_PROXY = "HTTP Proxy";
        private const string DEFAULT_WORKSPACE = "default workspace";

        private readonly Type _qtType = Type.GetTypeFromProgID("Quicktest.Application");
        private readonly IAssetRunner _runNotifier;
        private readonly object _lockObject = new object();
        private TimeSpan _timeLeftUntilTimeout = TimeSpan.MaxValue;
        private readonly string _uftRunMode;
        private Stopwatch _stopwatch = null;
        private Application _qtpApplication;
        private ParameterDefinitions _qtpParamDefs;
        private Parameters _qtpParameters;
        private bool _useUFTLicense;
        private RunCancelledDelegate _runCancelled;
        private McConnectionInfo _mcConnection;
        private string _mobileInfo;
        private CloudBrowser _cloudBrowser;
        private bool _printInputParams;
        private bool _isCancelledByUser;
        private RunAsUser _uftRunAsUser;

        /// <summary>
        /// constructor
        /// </summary>
        /// <param name="runNotifier"></param>
        /// <param name="useUftLicense"></param>
        /// <param name="timeLeftUntilTimeout"></param>
        public GuiTestRunner(IAssetRunner runNotifier, bool useUftLicense, TimeSpan timeLeftUntilTimeout, string uftRunMode, DigitalLab digitalLab, bool printInputParams, RunAsUser uftRunAsUser)
        {
            _timeLeftUntilTimeout = timeLeftUntilTimeout;
            _uftRunMode = uftRunMode;
            _stopwatch = Stopwatch.StartNew();
            _runNotifier = runNotifier;
            _useUFTLicense = useUftLicense;
            _mcConnection = digitalLab.ConnectionInfo;
            _mobileInfo = digitalLab.MobileInfo;
            _cloudBrowser = digitalLab.CloudBrowser;
            _printInputParams = printInputParams;
            _uftRunAsUser = uftRunAsUser;
        }

        #region QTP

        /// <summary>
        /// runs the given test and returns resutls
        /// </summary>
        /// <param name="testPath"></param>
        /// <param name="errorReason"></param>
        /// <param name="runCancelled"></param>
        /// <returns></returns>
        public TestRunResults RunTest(TestInfo testinf, ref string errorReason, RunCancelledDelegate runCancelled, out Dictionary<string, string> outParams)
        {
            outParams = new Dictionary<string, string>();
            var testPath = testinf.TestPath;
            TestRunResults runDesc = new TestRunResults { TestType = TestType.QTP };
            ConsoleWriter.ActiveTestRun = runDesc;
            ConsoleWriter.WriteLineWithTime("Running: " + testPath);

            runDesc.TestPath = testPath;

            // default report location is the test path
            runDesc.ReportLocation = testPath;
            // check if the report path has been defined
            if (!string.IsNullOrEmpty(testinf.ReportPath))
            {
                if (!Helper.TrySetTestReportPath(runDesc, testinf, ref errorReason))
                {
                    return runDesc;
                }
            }

            runDesc.TestState = TestState.Unknown;

            _runCancelled = runCancelled;

            if (!Helper.IsQtpInstalled())
            {
                runDesc.TestState = TestState.Error;
                runDesc.ErrorDesc = string.Format(Resources.GeneralQtpNotInstalled, Environment.MachineName);
                ConsoleWriter.WriteErrLine(runDesc.ErrorDesc);
                Environment.ExitCode = (int)Launcher.ExitCodeEnum.Failed;
                return runDesc;
            }

            string reason;
            if (!Helper.CanUftProcessStart(out reason))
            {
                runDesc.TestState = TestState.Error;
                runDesc.ErrorDesc = reason;
                ConsoleWriter.WriteErrLine(runDesc.ErrorDesc);
                Environment.ExitCode = (int)Launcher.ExitCodeEnum.Failed;
                return runDesc;
            }

            Version qtpVersion;
            try
            {
                lock (_lockObject)
                {
                    _qtpApplication = Activator.CreateInstance(_qtType) as Application;
                    if (_uftRunAsUser != null)
                    {
                        try
                        {
                            if (_qtpApplication.Launched)
                            {
                                QTPTestCleanup();
                                CleanUpAndKillQtp();
                            }
                            _qtpApplication.LaunchAsUser(_uftRunAsUser.Username, _uftRunAsUser.EncodedPassword);
                        }
                        catch (COMException e)
                        {
                            if (e.ErrorCode == MEMBER_NOT_FOUND)
                            {
                                errorReason = Resources.UftLaunchAsUserNotSupported;
                            }
                            throw;
                        }
                        if (_qtpApplication.Visible)
                        {
                            _qtpApplication.Visible = false;
                        }
                    }

                    qtpVersion = Version.Parse(_qtpApplication.Version);
                    if (qtpVersion.Equals(new Version(11, 0)))
                    {
                        runDesc.ReportLocation = GetReportLocation(testinf, testPath);
                    }
#if DEBUG
                    Console.WriteLine(string.Format("UFT One version = {0}", qtpVersion));
#endif
                    // Check for required Addins
                    LoadNeededAddins(testPath);

                    SetMobileInfo(qtpVersion);

                    if (!_qtpApplication.Launched)
                    {
                        if (_runCancelled())
                        {
                            QTPTestCleanup();
                            CleanUpAndKillQtp();
                            runDesc.TestState = TestState.Error;
                            return runDesc;
                        }
                        // Launch application after set Addins
                        _qtpApplication.Launch();
                        _qtpApplication.Visible = false;
                    }
                }
            }
            catch (Exception e)
            {
                if (string.IsNullOrEmpty(errorReason))
                {
                    errorReason = Resources.QtpNotLaunchedError;
                    runDesc.ErrorDesc = e.Message;
                    ConsoleWriter.WriteErrLine(e.Message);
                }
                else
                {
                    runDesc.ErrorDesc = errorReason;
                    ConsoleWriter.WriteErrLine(errorReason);
                }
#if DEBUG
                ConsoleWriter.WriteException(e);
#endif
                runDesc.TestState = TestState.Error;
                runDesc.ReportLocation = string.Empty;
                return runDesc;
            }

            if (_qtpApplication.Test != null && _qtpApplication.Test.Modified)
            {
                var message = Resources.QtpNotLaunchedError;
                errorReason = message;
                runDesc.TestState = TestState.Error;
                runDesc.ErrorDesc = errorReason;
                return runDesc;
            }

            _qtpApplication.UseLicenseOfType(_useUFTLicense ? tagUnifiedLicenseType.qtUnifiedFunctionalTesting : tagUnifiedLicenseType.qtNonUnified);

            Dictionary<string, object> paramDict;
            try
            {
                paramDict = testinf.GetParameterDictionaryForQTP();
            }
            catch (ArgumentException)
            {
                ConsoleWriter.WriteErrLine(Resources.FsDuplicateParamNames);
                throw;
            }

            //if (!HandleDigitalLab(qtpVersion, ref errorReason))
            //{
            //    runDesc.TestState = TestState.Error;
            //    runDesc.ErrorDesc = errorReason;
            //    return runDesc;
            //}

            if (!HandleInputParameters(testPath, ref errorReason, paramDict, testinf))
            {
                runDesc.TestState = TestState.Error;
                runDesc.ErrorDesc = errorReason;
                return runDesc;
            }

            if (!HandleCloudBrowser(qtpVersion, ref errorReason))
            {
                ConsoleWriter.WriteErrLine(errorReason);
                runDesc.TestState = TestState.Error;
                runDesc.ErrorDesc = errorReason;
                return runDesc;
            }

            GuiTestRunResult guiTestRunResult = ExecuteQTPRun(runDesc);
            runDesc.ReportLocation = guiTestRunResult.ReportPath;
            if (!guiTestRunResult.IsSuccess)
            {
                runDesc.TestState = TestState.Error;
                return runDesc;
            }

            if (!HandleOutputArguments(ref errorReason, out outParams))
            {
                runDesc.TestState = TestState.Error;
                runDesc.ErrorDesc = errorReason;
                return runDesc;
            }

            QTPTestCleanup();
            _qtpApplication = null;

            return runDesc;
        }

        private string GetReportLocation(TestInfo testinf, string testPath)
        {
            // use the defined report path if provided
            string rptLocation = string.IsNullOrEmpty(testinf.ReportPath) ?
                            Path.Combine(testPath, REPORT) :
                            Path.Combine(testinf.ReportPath, REPORT);

            if (Directory.Exists(rptLocation))
            {
                int lastIndex = rptLocation.IndexOf("\\");
                var location = rptLocation.Substring(0, lastIndex);
                var name = rptLocation.Substring(lastIndex + 1);
                rptLocation = Helper.GetNextResFolder(location, name);
                Directory.CreateDirectory(rptLocation);
            }
            return rptLocation;
        }

        private void SetMobileInfo(Version qtpVersion)
        {
            if (_mcConnection == null || _mcConnection.HostAddress.IsNullOrEmpty())
                return;

            #region Mc connection and other mobile info

            ITDPierToTulip tulip = _qtpApplication.TDPierToTulip;
            //if (qtpVersion < new Version(2023, 4)) // for version >= 23.4 use the method HandleDigitalLab
            {
                tulip.SetTestOptionsVal(MC_TYPE, (int)_mcConnection.LabType);

                tulip.SetTestOptionsVal(MOBILE_HOST_ADDRESS, _mcConnection.HostAddress);
                if (!_mcConnection.HostPort.IsNullOrEmpty())
                {
                    tulip.SetTestOptionsVal(MOBILE_HOST_PORT, _mcConnection.HostPort);
                }

                AuthType mcAuthType = _mcConnection.MobileAuthType;
                tulip.SetTestOptionsVal(MOBILE_AUTH_TYPE, mcAuthType);
                switch (mcAuthType)
                {
                    case AuthType.AuthToken:
                        var token = _mcConnection.GetAuthToken();
                        tulip.SetTestOptionsVal(MOBILE_CLIENTID, token.ClientId);
                        tulip.SetTestOptionsVal(MOBILE_SECRET, token.SecretKey);
                        break;
                    case AuthType.UsernamePassword:
                        if (!_mcConnection.UserName.IsNullOrEmpty())
                        {
                            tulip.SetTestOptionsVal(MOBILE_USER, _mcConnection.UserName);
                            if (!_mcConnection.Password.IsNullOrEmpty())
                            {
                                tulip.SetTestOptionsVal(MOBILE_PASSWORD, GetEncryptedPassword(_mcConnection.Password));
                            }
                        }
                        break;
                }

                // set tenantID
                if (!_mcConnection.TenantId.IsNullOrEmpty())
                {
                    tulip.SetTestOptionsVal(MOBILE_TENANT, _mcConnection.TenantId);
                }

                // ssl and proxy info
                tulip.SetTestOptionsVal(MOBILE_USE_SSL, _mcConnection.UseSslAsInt);

                if (_mcConnection.UseProxy)
                {
                    tulip.SetTestOptionsVal(MOBILE_USE_PROXY, _mcConnection.UseProxyAsInt);
                    tulip.SetTestOptionsVal(MOBILE_PROXY_SETTING, _mcConnection.ProxyType);
                    tulip.SetTestOptionsVal(MOBILE_PROXY_SETTING_ADDRESS, _mcConnection.ProxyAddress);
                    tulip.SetTestOptionsVal(MOBILE_PROXY_SETTING_PORT, _mcConnection.ProxyPort);
                    if (_mcConnection.UseProxyAuth)
                    {
                        tulip.SetTestOptionsVal(MOBILE_PROXY_SETTING_AUTHENTICATION, 1);
                        tulip.SetTestOptionsVal(MOBILE_PROXY_SETTING_USERNAME, _mcConnection.ProxyUserName);
                        tulip.SetTestOptionsVal(MOBILE_PROXY_SETTING_PASSWORD, GetEncryptedPassword(_mcConnection.ProxyPassword));
                    }
                }
            }

            if (!string.IsNullOrEmpty(_mobileInfo))
            {
                tulip.SetTestOptionsVal(MOBILE_INFO, _mobileInfo);
            }

            #endregion
        }

        private string GetEncryptedPassword(string clearPassword)
        {
            string encPassword = WinUserNativeMethods.ProtectBSTRToBase64(clearPassword);
            if (encPassword == null)
            {
                ConsoleWriter.WriteLine(string.Format(PROTECT_BstrToBase64_FAILED, "DL Password"));
                throw new Exception(string.Format(PROTECT_BstrToBase64_FAILED, "DL Password"));
            }
            return encPassword;
        }

        /// <summary>
        /// performs global cleanup code for this type of runner
        /// </summary>
        public void CleanUp()
        {
            try
            {
                lock (_lockObject)
                {
                    //if we don't have a qtp instance, create one
                    if (_qtpApplication == null)
                    {
                        _qtpApplication = Activator.CreateInstance(_qtType) as Application;
                    }

                    _qtpApplication.Quit();
                }
            }
            catch
            {
                //nothing to do. (cleanup code should not throw exceptions, and there is no need to log this as an error in the test)
            }
        }

        public void SafelyCancel()
        {
            _isCancelledByUser = true;
            ConsoleWriter.WriteLine(Resources.GeneralStopAborted);
            QTPTestCleanup();
            CleanUpAndKillQtp();
            ConsoleWriter.WriteLine(Resources.GeneralAbortedByUser);
        }

        static HashSet<string> _colLoadedAddinNames = null;
        /// <summary>
        /// Set the test Addins 
        /// </summary>
        private void LoadNeededAddins(string fileName)
        {
            bool blnNeedToLoadAddins = false;

            //if not launched, we have no addins.
            if (!_qtpApplication.Launched)
                _colLoadedAddinNames = null;

            try
            {
                HashSet<string> colCurrentTestAddins = new HashSet<string>();

                object erroDescription;
                var testAddinsObj = _qtpApplication.GetAssociatedAddinsForTest(fileName);
                object[] testAddins = (object[])testAddinsObj;

                foreach (string addin in testAddins)
                {
                    colCurrentTestAddins.Add(addin);
                }

                if (_colLoadedAddinNames != null)
                {
                    //check if we have a missing addin (and need to quit Qtp, and reload with new addins)
                    foreach (string addin in testAddins)
                    {
                        if (!_colLoadedAddinNames.Contains(addin))
                        {
                            blnNeedToLoadAddins = true;
                            break;
                        }
                    }

                    //check if there is no extra addins that need to be removed
                    if (_colLoadedAddinNames.Count != colCurrentTestAddins.Count)
                    {
                        blnNeedToLoadAddins = true;
                    }
                }
                else
                {
                    //first time = load addins.
                    blnNeedToLoadAddins = true;
                }

                _colLoadedAddinNames = colCurrentTestAddins;

                //the addins need to be refreshed, load new addins
                if (blnNeedToLoadAddins)
                {
                    if (_qtpApplication.Launched && _uftRunAsUser == null)
                        _qtpApplication.Quit();
                    _qtpApplication.SetActiveAddins(ref testAddinsObj, out erroDescription);
                }
            }
            catch (Exception)
            {
                // Try anyway to run the test
            }
        }


        /// <summary>
        /// Activate all Installed Addins 
        /// </summary>
        private void ActivateAllAddins()
        {
            try
            {
                // Get Addins collection
                Addins qtInstalledAddins = _qtpApplication.Addins;

                if (qtInstalledAddins.Count > 0)
                {
                    string[] qtAddins = new string[qtInstalledAddins.Count];

                    // Addins Object is 1 base order
                    for (int idx = 1; idx <= qtInstalledAddins.Count; ++idx)
                    {
                        // Our list is 0 base order
                        qtAddins[idx - 1] = qtInstalledAddins[idx].Name;
                    }

                    object erroDescription;
                    var addinNames = (object)qtAddins;

                    _qtpApplication.SetActiveAddins(ref addinNames, out erroDescription);
                }
            }
            catch (Exception)
            {
                // Try anyway to run the test
            }
        }

        /// <summary>
        /// runs the given test QTP and returns results
        /// </summary>
        /// <param name="testResults">the test results object containing test info and also receiving run results</param>
        /// <returns></returns>
        private GuiTestRunResult ExecuteQTPRun(TestRunResults testResults)
        {
            GuiTestRunResult result = new GuiTestRunResult { IsSuccess = true };
            try
            {
                Type runResultsOptionstype = Type.GetTypeFromProgID("QuickTest.RunResultsOptions");
                var options = (RunResultsOptions)Activator.CreateInstance(runResultsOptionstype);
                options.ResultsLocation = testResults.ReportLocation;
                if (_uftRunMode != null)
                {
                    _qtpApplication.Options.Run.RunMode = _uftRunMode;
                }

                //Check for cancel before executing
                if (_runCancelled())
                {
                    QTPTestCleanup();
                    CleanUpAndKillQtp();
                    testResults.TestState = TestState.Error;
                    testResults.ErrorDesc = Resources.GeneralTestCanceled;
                    ConsoleWriter.WriteLine(Resources.GeneralTestCanceled);
                    result.IsSuccess = false;
                    Launcher.ExitCode = Launcher.ExitCodeEnum.Aborted;
                    return result;
                }
                ConsoleWriter.WriteLine(string.Format(Resources.FsRunnerRunningTest, testResults.TestPath));

                _qtpApplication.Test.Run(options, false, _qtpParameters);

                result.ReportPath = Path.Combine(testResults.ReportLocation, REPORT);
                int slept = 0;
                while ((slept < 20000 && _qtpApplication.GetStatus() == READY) || _qtpApplication.GetStatus() == WAITING)
                {
                    Thread.Sleep(50);
                    slept += 50;
                }

                while (!_runCancelled() && (_qtpApplication.GetStatus() == RUNNING || _qtpApplication.GetStatus() == BUSY))
                {
                    Thread.Sleep(200);
                    if (_timeLeftUntilTimeout - _stopwatch.Elapsed <= TimeSpan.Zero)
                    {
                        QTPTestCleanup();
                        CleanUpAndKillQtp();
                        testResults.TestState = TestState.Error;
                        testResults.ErrorDesc = Resources.GeneralTimeoutExpired;
                        ConsoleWriter.WriteLine(Resources.GeneralTimeoutExpired);
                        Launcher.ExitCode = Launcher.ExitCodeEnum.Aborted;
                        result.IsSuccess = false;
                        return result;
                    }
                }

                if (_runCancelled())
                {
                    QTPTestCleanup();
                    CleanUpAndKillQtp();
                    testResults.TestState = TestState.Error;
                    testResults.ErrorDesc = Resources.GeneralTestCanceled;
                    ConsoleWriter.WriteLine(Resources.GeneralTestCanceled);
                    Launcher.ExitCode = Launcher.ExitCodeEnum.Aborted;
                    result.IsSuccess = false;
                    return result;
                }
                string lastError = _qtpApplication.Test.LastRunResults.LastError;

                //read the lastError
                if (!string.IsNullOrEmpty(lastError))
                {
                    testResults.TestState = TestState.Error;
                    testResults.ErrorDesc = lastError;
                }

                // the way to check the logical success of the target QTP test is: app.Test.LastRunResults.Status == "Passed".
                if (_qtpApplication.Test.LastRunResults.Status == PASSED)
                {
                    testResults.TestState = TestState.Passed;
                }
                else if (_qtpApplication.Test.LastRunResults.Status == WARNING)
                {
                    testResults.TestState = TestState.Warning;
                    testResults.HasWarnings = true;

                    if (Launcher.ExitCode != Launcher.ExitCodeEnum.Failed && Launcher.ExitCode != Launcher.ExitCodeEnum.Aborted)
                        Launcher.ExitCode = Launcher.ExitCodeEnum.Unstable;
                }
                else
                {
                    testResults.TestState = TestState.Failed;
                    testResults.FailureDesc = "Test failed";

                    Launcher.ExitCode = Launcher.ExitCodeEnum.Failed;
                }
            }
            catch (NullReferenceException e)
            {
                ConsoleWriter.WriteLine(string.Format(Resources.GeneralErrorWithStack, e.Message, e.StackTrace));
                testResults.TestState = TestState.Error;
                testResults.ErrorDesc = Resources.QtpRunError;

                result.IsSuccess = false;
                return result;
            }
            catch (SystemException e)
            {
                CleanUpAndKillQtp();
                ConsoleWriter.WriteLine(string.Format(Resources.GeneralErrorWithStack, e.Message, e.StackTrace));
                testResults.TestState = TestState.Error;
                testResults.ErrorDesc = Resources.QtpRunError;

                result.IsSuccess = false;
                return result;
            }
            catch (Exception e2)
            {
                if (_isCancelledByUser)
                {
                    testResults.TestState = TestState.Error;
                    testResults.ErrorDesc = Resources.GeneralStopAborted;
                }
                else
                {
                    ConsoleWriter.WriteLine(string.Format(Resources.GeneralErrorWithStack, e2.Message, e2.StackTrace));
                    testResults.TestState = TestState.Error;
                    testResults.ErrorDesc = Resources.QtpRunError;
                }

                result.IsSuccess = false;
                return result;
            }

            return result;
        }

        private void CleanUpAndKillQtp()
        {
            //error during run, process may have crashed (need to cleanup, close QTP and qtpRemote for next test to run correctly)
            CleanUp();

            //kill the qtp automation, to make sure it will run correctly next time
            Process[] processes = Process.GetProcessesByName("qtpAutomationAgent");
            Process qtpAuto = processes.Where(p => p.SessionId == Process.GetCurrentProcess().SessionId).FirstOrDefault();
            if (qtpAuto != null)
            {
                qtpAuto.Kill();
            }
        }
        private bool HandleDigitalLab(Version qtpVersion, ref string errorReason)
        {
            if (_mcConnection == null || _mcConnection.HostAddress.IsNullOrEmpty() || qtpVersion < new Version(2023, 4))
                return true;

            return SetDLOptions(_qtpApplication.Options.DLConnection, ref errorReason);
        }

        private bool SetDLOptions(DLConnectionOptions opt, ref string errorReason)
        {
            try
            {
                int type = (int)DigitalLabType.UFT; // TODO set and get it from props
                opt.Type = type.ToString();
                if (_mcConnection.MobileAuthType == AuthType.AuthToken)
                {
                    opt.AuthType = AuthType.AuthToken.GetEnumDescription();
                    opt.AccessKey = _mcConnection.ExecToken;
                }
                else if (!_mcConnection.UserName.IsNullOrEmpty())
                {
                    opt.AuthType = AuthType.UsernamePassword.GetEnumDescription();
                    opt.UserName = _mcConnection.UserName;
                    opt.Password = _mcConnection.Password;
                }
                opt.Server = _mcConnection.HostAddress;
                opt.Port = _mcConnection.HostPort;
                opt.UseSSL = _mcConnection.UseSSL;
                if (_mcConnection.UseProxy)
                {
                    opt.UseProxySettings = true;
                    opt.ProxyType = _mcConnection.ProxyType == 1 ? SYSTEM_PROXY : HTTP_PROXY;
                    opt.ProxyAddress = _mcConnection.ProxyAddress;
                    opt.ProxyPort = _mcConnection.ProxyPort.ToString();
                    if (_mcConnection.UseProxyAuth)
                    {
                        opt.SpecifyAuthentication = true;
                        opt.ProxyUserName = _mcConnection.ProxyUserName;
                        opt.ProxyPassword = _mcConnection.ProxyPassword;
                    }
                }
                opt.ShowRemoteWndOnRun = true;
                opt.WorkSpace = DEFAULT_WORKSPACE;
                return true;
            }
            catch (Exception ex)
            {
#if DEBUG
                Console.WriteLine(ex.Message);
#endif
                errorReason = ex.Message;
                return false;
            }
        }

        private bool HandleOutputArguments(ref string errorReason, out Dictionary<string, string> outParams)
        {
            outParams = new Dictionary<string, string>();
            try
            {
                for (int i = 1; i <= _qtpParamDefs.Count; ++i)
                {
                    var pd = _qtpParamDefs[i];
                    if (pd.InOut == qtParameterDirection.qtParamDirOut)
                    {
                        var value = _qtpParameters[pd.Name].Value;
                        if (value != null)
                        {
                            if (outParams.ContainsKey(pd.Name))
                            {
                                outParams[pd.Name] = value.ToString();
                            }
                            else
                            {
                                outParams.Add(pd.Name, value.ToString());
                            }
                        }
                    }
                }
            }
            catch (Exception)
            {
                errorReason = Resources.QtpNotLaunchedError;
                return false;
            }
            return true;
        }
        private bool VerifyParameterValueType(object paramValue, qtParameterType type)
        {
            bool legal = false;

            switch (type)
            {
                case qtParameterType.qtParamTypeBoolean:
                    legal = paramValue is bool;
                    break;

                case qtParameterType.qtParamTypeDate:
                    legal = paramValue is DateTime;
                    break;

                case qtParameterType.qtParamTypeNumber:
                    legal = ((paramValue is int) || (paramValue is long) || (paramValue is decimal) || (paramValue is float) || (paramValue is double));
                    break;

                case qtParameterType.qtParamTypePassword:
                case qtParameterType.qtParamTypeString:
                case qtParameterType.qtParamTypeAny:
                    legal = paramValue is string;
                    break;

                default:
                    legal = false;
                    break;
            }

            return legal;
        }

        private bool HandleCloudBrowser(Version qtpVersion, ref string errorReason)
        {
            if (_cloudBrowser != null)
            {
                if (qtpVersion < new Version(2023, 4))
                {
                    errorReason = string.Format(Resources.CloudBrowserNotSupported, qtpVersion.ToString(2));
                    return false;
                }
                try
                {
                    var launcher = _qtpApplication.Test.Settings.Launchers[WEB];
                    launcher.Active = true;
                    launcher.SetLab(CLOUD_BROWSER);
                    if (!_cloudBrowser.Url.IsNullOrWhiteSpace())
                        launcher.Address = _cloudBrowser.Url;
                    launcher.CloudBrowser.OS = _cloudBrowser.OS;
                    launcher.CloudBrowser.Browser = _cloudBrowser.Browser;
                    launcher.CloudBrowser.BrowserVersion = _cloudBrowser.Version;
                    launcher.CloudBrowser.Location = _cloudBrowser.Region;
                }
                catch (Exception e)
                {
                    ConsoleWriter.WriteLine(string.Format(Resources.GeneralErrorWithStack, e.Message, e.StackTrace));
                    errorReason = e.Message;
                    return false;
                }
            }
            return true;
        }

        private bool HandleInputParameters(string fileName, ref string errorReason, Dictionary<string, object> inputParams, TestInfo testInfo)
        {
            try
            {
                string path = fileName;

                if (_runCancelled())
                {
                    QTPTestCleanup();
                    CleanUpAndKillQtp();
                    return false;
                }

                _qtpApplication.Open(path, true, false);
                _qtpParamDefs = _qtpApplication.Test.ParameterDefinitions;
                _qtpParameters = _qtpParamDefs.GetParameters();

                // handle all parameters (index starts with 1 !!!)
                for (int i = 1; i <= _qtpParamDefs.Count; i++)
                {
                    // input parameters
                    if (_qtpParamDefs[i].InOut == qtParameterDirection.qtParamDirIn)
                    {
                        string paramName = _qtpParamDefs[i].Name;
                        qtParameterType type = _qtpParamDefs[i].Type;

                        // if the caller supplies value for a parameter we set it
                        if (!inputParams.ContainsKey(paramName)) continue;

                        // first verify that the type is correct
                        object paramValue = inputParams[paramName];
                        if (!VerifyParameterValueType(paramValue, type))
                        {
                            try
                            {
                                ConsoleWriter.WriteErrLine(string.Format(Resources.GeneralParameterTypeMismatchWith2Types, paramName, Enum.GetName(typeof(qtParameterType), type), paramValue.GetType()));
                            }
                            catch (Exception)
                            {
                                ConsoleWriter.WriteErrLine(string.Format(Resources.GeneralParameterTypeMismatchWith2Types, paramName, Enum.GetName(typeof(qtParameterType), type), null));
                            }
                        }
                        else
                        {
                            // second-check
                            try
                            {
                                _qtpParameters[paramName].Value = paramValue;
                                if (_printInputParams)
                                {
                                    if (type == qtParameterType.qtParamTypePassword)
                                        ConsoleWriter.WriteLine(string.Format(Resources.GeneralParameterUsageMask, paramName));
                                    else
                                        ConsoleWriter.WriteLine(string.Format(Resources.GeneralParameterUsage, paramName, type != qtParameterType.qtParamTypeDate ? paramValue : ((DateTime)paramValue).ToShortDateString()));
                                }
                            }
                            catch (Exception)
                            {
                                ConsoleWriter.WriteErrLine(string.Format(Resources.GeneralParameterTypeMismatchWith1Type, paramName));
                            }
                        }
                    }
                }

                // specify data table path
                if (testInfo.DataTablePath != null)
                {
                    _qtpApplication.Test.Settings.Resources.DataTablePath = testInfo.DataTablePath;
                    ConsoleWriter.WriteLine("Using external data table: " + testInfo.DataTablePath);
                }

                // specify iteration mode
                if (testInfo.IterationInfo != null)
                {
                    try
                    {
                        IterationInfo ii = testInfo.IterationInfo;
                        if (!IterationInfo.AvailableTypes.Contains(ii.IterationMode))
                        {
                            throw new ArgumentException(string.Format("Illegal iteration mode '{0}'. Available modes are : {1}", ii.IterationMode, string.Join(", ", IterationInfo.AvailableTypes)));
                        }

                        bool rangeMode = IterationInfo.RANGE_ITERATION_MODE.Equals(ii.IterationMode);
                        if (rangeMode)
                        {
                            int start = int.Parse(ii.StartIteration);
                            int end = int.Parse(ii.EndIteration);

                            _qtpApplication.Test.Settings.Run.StartIteration = start;
                            _qtpApplication.Test.Settings.Run.EndIteration = end;
                        }

                        _qtpApplication.Test.Settings.Run.IterationMode = testInfo.IterationInfo.IterationMode;

                        ConsoleWriter.WriteLine("Using iteration mode: " + testInfo.IterationInfo.IterationMode +
                       (rangeMode ? " " + testInfo.IterationInfo.StartIteration + "-" + testInfo.IterationInfo.EndIteration : string.Empty));
                    }
                    catch (Exception e)
                    {
                        string msg = "Failed to parse 'Iterations' element . Using default iteration settings. Error : " + e.Message;
                        ConsoleWriter.WriteLine(msg);
                    }
                }
            }
            catch (Exception)
            {
                errorReason = Resources.QtpRunError;
                return false;
            }
            return true;
        }

        /// <summary>
        /// stops and closes qtp test, to make sure nothing is left floating after run.
        /// </summary>
        private void QTPTestCleanup()
        {
            try
            {
                lock (_lockObject)
                {
                    if (_qtpApplication == null)
                    {
                        return;
                    }

                    var qtpTest = _qtpApplication.Test;
                    if (qtpTest != null)
                    {
                        try
                        {
                            _qtpApplication.Test.Stop();
                            _qtpApplication.Test.Close();
                        }
                        catch (Exception)
                        { }
                    }
                }
            }
            catch (Exception)
            {
            }

            _qtpParameters = null;
            _qtpParamDefs = null;
        }

        private RegistryKey GetQuickTestProfessionalAutomationRegKey(RegistryView registryView)
        {
            RegistryKey localKey = RegistryKey.OpenBaseKey(Microsoft.Win32.RegistryHive.LocalMachine, RegistryView.Registry64);
            localKey = localKey.OpenSubKey(@"SOFTWARE\Classes\AppID\{A67EB23A-1B8F-487D-8E38-A6A3DD150F0B}", true);

            return localKey;
        }

        #endregion

        /// <summary>
        /// holds the resutls for a GUI test
        /// </summary>
        private class GuiTestRunResult
        {
            public GuiTestRunResult()
            {
                ReportPath = string.Empty;
            }

            public bool IsSuccess { get; set; }
            public string ReportPath { get; set; }
        }
    }
}
