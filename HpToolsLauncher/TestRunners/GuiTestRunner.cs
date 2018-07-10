// (c) Copyright 2012 Hewlett-Packard Development Company, L.P. 
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

using System;
using System.Linq;
using System.IO;
using System.Xml;
using QTObjectModelLib;
using Resources = HpToolsLauncher.Properties.Resources;
using System.Threading;
using System.Diagnostics;
using System.Collections.Generic;
using Microsoft.Win32;

namespace HpToolsLauncher
{
    public class GuiTestRunner : IFileSysTestRunner
    {
        // Setting keys for mobile
        private const string MOBILE_HOST_ADDRESS = "ALM_MobileHostAddress";
        private const string MOBILE_HOST_PORT = "ALM_MobileHostPort";
        private const string MOBILE_USER   = "ALM_MobileUserName";
        private const string MOBILE_PASSWORD = "ALM_MobilePassword";
        private const string MOBILE_TENANT = "EXTERNAL_MobileTenantId";
        private const string MOBILE_USE_SSL = "ALM_MobileUseSSL";
        private const string MOBILE_USE_PROXY= "MobileProxySetting_UseProxy";
        private const string MOBILE_PROXY_SETTING_ADDRESS = "MobileProxySetting_Address";
        private const string MOBILE_PROXY_SETTING_PORT = "MobileProxySetting_Port";
        private const string MOBILE_PROXY_SETTING_AUTHENTICATION = "MobileProxySetting_Authentication";
        private const string MOBILE_PROXY_SETTING_USERNAME = "MobileProxySetting_UserName";
        private const string MOBILE_PROXY_SETTING_PASSWORD = "MobileProxySetting_Password";
        private const string MOBILE_INFO = "mobileinfo";

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
        /// <summary>
        /// constructor
        /// </summary>
        /// <param name="runNotifier"></param>
        /// <param name="useUftLicense"></param>
        /// <param name="timeLeftUntilTimeout"></param>
        public GuiTestRunner(IAssetRunner runNotifier, bool useUftLicense, TimeSpan timeLeftUntilTimeout, string uftRunMode, McConnectionInfo mcConnectionInfo, string mobileInfo)
        {
            _timeLeftUntilTimeout = timeLeftUntilTimeout;
            _uftRunMode = uftRunMode;
            _stopwatch = Stopwatch.StartNew();
            _runNotifier = runNotifier;
            _useUFTLicense = useUftLicense;
            _mcConnection = mcConnectionInfo;
            _mobileInfo = mobileInfo;
        }

        #region QTP

        /// <summary>
        /// runs the given test and returns resutls
        /// </summary>
        /// <param name="testPath"></param>
        /// <param name="errorReason"></param>
        /// <param name="runCanclled"></param>
        /// <returns></returns>
        public TestRunResults RunTest(TestInfo testinf, ref string errorReason, RunCancelledDelegate runCanclled)
        {
            var testPath = testinf.TestPath;
            TestRunResults runDesc = new TestRunResults();
            ConsoleWriter.ActiveTestRun = runDesc;
            ConsoleWriter.WriteLine(DateTime.Now.ToString(Launcher.DateFormat) + " Running: " + testPath);
            runDesc.ReportLocation = testPath;


            runDesc.TestPath = testPath;
            runDesc.TestState = TestState.Unknown;

            _runCancelled = runCanclled;

            if (!Helper.IsQtpInstalled())
            {
                runDesc.TestState = TestState.Error;
                runDesc.ErrorDesc = string.Format(Resources.GeneralQtpNotInstalled, System.Environment.MachineName);
                ConsoleWriter.WriteErrLine(runDesc.ErrorDesc);
                Environment.ExitCode = (int)Launcher.ExitCodeEnum.Failed;
                return runDesc;
            }

            string reason = string.Empty;
            if (!Helper.CanUftProcessStart(out reason))
            {
                runDesc.TestState = TestState.Error;
                runDesc.ErrorDesc = reason;
                ConsoleWriter.WriteErrLine(runDesc.ErrorDesc);
                Environment.ExitCode = (int)Launcher.ExitCodeEnum.Failed;
                return runDesc;
            }

            try
            {
                ChangeDCOMSettingToInteractiveUser();
                var type = Type.GetTypeFromProgID("Quicktest.Application");

                lock (_lockObject)
                {
                    _qtpApplication = Activator.CreateInstance(type) as Application;

                    Version qtpVersion = Version.Parse(_qtpApplication.Version);
                    if (qtpVersion.Equals(new Version(11, 0)))
                    {
                        runDesc.ReportLocation = Path.Combine(testPath, "Report");
                        if (Directory.Exists(runDesc.ReportLocation))
                        {
                            Directory.Delete(runDesc.ReportLocation, true);
                            Directory.CreateDirectory(runDesc.ReportLocation);
                        }
                    }


                    // Check for required Addins
                    LoadNeededAddins(testPath);

                    // set Mc connection and other mobile info into rack if neccesary
                    #region Mc connection and other mobile info

                    // Mc Address, username and password
                    if (!string.IsNullOrEmpty(_mcConnection.MobileHostAddress))
                    {
                        _qtpApplication.TDPierToTulip.SetTestOptionsVal(MOBILE_HOST_ADDRESS, _mcConnection.MobileHostAddress);
                        if (!string.IsNullOrEmpty(_mcConnection.MobileHostPort))
                        {
                            _qtpApplication.TDPierToTulip.SetTestOptionsVal(MOBILE_HOST_PORT, _mcConnection.MobileHostPort);
                        }
                    }

                    if (!string.IsNullOrEmpty(_mcConnection.MobileUserName))
                    {
                        _qtpApplication.TDPierToTulip.SetTestOptionsVal(MOBILE_USER, _mcConnection.MobileUserName);
                    }

                    if (!string.IsNullOrEmpty(_mcConnection.MobileTenantId))
                    {
                        _qtpApplication.TDPierToTulip.SetTestOptionsVal(MOBILE_TENANT, _mcConnection.MobileTenantId);
                    }

                    if (!string.IsNullOrEmpty(_mcConnection.MobilePassword))
                    {
                        string encriptedMcPassword = WinUserNativeMethods.ProtectBSTRToBase64(_mcConnection.MobilePassword);
                        if (encriptedMcPassword == null)
                        {
                            ConsoleWriter.WriteLine("ProtectBSTRToBase64 fail for mcPassword");
                            throw new Exception("ProtectBSTRToBase64 fail for mcPassword");
                        }
                        _qtpApplication.TDPierToTulip.SetTestOptionsVal(MOBILE_PASSWORD, encriptedMcPassword);
                    }

                    // ssl and proxy info
                    _qtpApplication.TDPierToTulip.SetTestOptionsVal(MOBILE_USE_SSL, _mcConnection.MobileUseSSL);

                    if (_mcConnection.MobileUseProxy == 1)
                    {
                        _qtpApplication.TDPierToTulip.SetTestOptionsVal(MOBILE_USE_PROXY, _mcConnection.MobileUseProxy);
                        _qtpApplication.TDPierToTulip.SetTestOptionsVal(MOBILE_PROXY_SETTING_ADDRESS, _mcConnection.MobileProxySetting_Address);
                        _qtpApplication.TDPierToTulip.SetTestOptionsVal(MOBILE_PROXY_SETTING_PORT, _mcConnection.MobileProxySetting_Port);
                        _qtpApplication.TDPierToTulip.SetTestOptionsVal(MOBILE_PROXY_SETTING_AUTHENTICATION, _mcConnection.MobileProxySetting_Authentication);
                        _qtpApplication.TDPierToTulip.SetTestOptionsVal(MOBILE_PROXY_SETTING_USERNAME, _mcConnection.MobileProxySetting_UserName);
                        string encriptedMcProxyPassword = WinUserNativeMethods.ProtectBSTRToBase64(_mcConnection.MobileProxySetting_Password);
                        if (encriptedMcProxyPassword == null)
                        {
                            ConsoleWriter.WriteLine("ProtectBSTRToBase64 fail for mc proxy Password");
                            throw new Exception("ProtectBSTRToBase64 fail for mc proxy Password");
                        }
                        _qtpApplication.TDPierToTulip.SetTestOptionsVal(MOBILE_PROXY_SETTING_PASSWORD, encriptedMcProxyPassword);
                    }
                    
                    // Mc info (device, app, launch and terminate data)
                    if (!string.IsNullOrEmpty(_mobileInfo))
                    {
                        _qtpApplication.TDPierToTulip.SetTestOptionsVal(MOBILE_INFO, _mobileInfo);
                    }

                    #endregion


                    if (!_qtpApplication.Launched)
                    {
                        if (_runCancelled())
                        {
                            QTPTestCleanup();
                            KillQtp();
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
                errorReason = Resources.QtpNotLaunchedError;
                runDesc.TestState = TestState.Error;
                runDesc.ReportLocation = "";
                runDesc.ErrorDesc = e.Message;
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

            _qtpApplication.UseLicenseOfType(_useUFTLicense
                                                 ? tagUnifiedLicenseType.qtUnifiedFunctionalTesting
                                                 : tagUnifiedLicenseType.qtNonUnified);

            if (!HandleInputParameters(testPath, ref errorReason, testinf.GetParameterDictionaryForQTP(), testinf.DataTablePath))
            {
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

            if (!HandleOutputArguments(ref errorReason))
            {
                runDesc.TestState = TestState.Error;
                runDesc.ErrorDesc = errorReason;
                return runDesc;
            }

            QTPTestCleanup();


            return runDesc;
        }

        /// <summary>
        /// performs global cleanup code for this type of runner
        /// </summary>
        public void CleanUp()
        {
            try
            {
                //if we don't have a qtp instance, create one
                if (_qtpApplication == null)
                {
                    var type = Type.GetTypeFromProgID("Quicktest.Application");
                    _qtpApplication = Activator.CreateInstance(type) as Application;
                }

                //if the app is running, close it.
                if (_qtpApplication.Launched)
                    _qtpApplication.Quit();
            }
            catch
            {
                //nothing to do. (cleanup code should not throw exceptions, and there is no need to log this as an error in the test)
            }
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
                    if (_qtpApplication.Launched)
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
                _qtpApplication.Options.Run.RunMode = _uftRunMode;

                //Check for cancel before executing
                if (_runCancelled())
                {
                    testResults.TestState = TestState.Error;
                    testResults.ErrorDesc = Resources.GeneralTestCanceled;
                    ConsoleWriter.WriteLine(Resources.GeneralTestCanceled);
                    result.IsSuccess = false;
                    return result;
                }
                ConsoleWriter.WriteLine(string.Format(Resources.FsRunnerRunningTest, testResults.TestPath));

                _qtpApplication.Test.Run(options, false, _qtpParameters);

                result.ReportPath = Path.Combine(testResults.ReportLocation, "Report");
                int slept = 0;
                while ((slept < 20000 && _qtpApplication.GetStatus().Equals("Ready")) || _qtpApplication.GetStatus().Equals("Waiting"))
                {
                    Thread.Sleep(50);
                    slept += 50;
                }


                while (!_runCancelled() && (_qtpApplication.GetStatus().Equals("Running") || _qtpApplication.GetStatus().Equals("Busy")))
                {
                    Thread.Sleep(200);
                    if (_timeLeftUntilTimeout - _stopwatch.Elapsed <= TimeSpan.Zero)
                    {
                        _qtpApplication.Test.Stop();
                        testResults.TestState = TestState.Error;
                        testResults.ErrorDesc = Resources.GeneralTimeoutExpired;
                        ConsoleWriter.WriteLine(Resources.GeneralTimeoutExpired);

                        result.IsSuccess = false;
                        return result;
                    }
                }

                if (_runCancelled())
                {
                    QTPTestCleanup();
                    KillQtp();
                    testResults.TestState = TestState.Error;
                    testResults.ErrorDesc = Resources.GeneralTestCanceled;
                    ConsoleWriter.WriteLine(Resources.GeneralTestCanceled);
                    Launcher.ExitCode = Launcher.ExitCodeEnum.Aborted;
                    result.IsSuccess = false;
                    return result;
                }
                string lastError = _qtpApplication.Test.LastRunResults.LastError;

                //read the lastError
                if (!String.IsNullOrEmpty(lastError))
                {
                    testResults.TestState = TestState.Error;
                    testResults.ErrorDesc = lastError;
                }

                // the way to check the logical success of the target QTP test is: app.Test.LastRunResults.Status == "Passed".
                if (_qtpApplication.Test.LastRunResults.Status.Equals("Passed"))
                {
                    testResults.TestState = TestState.Passed;

                }
                else if (_qtpApplication.Test.LastRunResults.Status.Equals("Warning"))
                {
                    testResults.TestState = TestState.Passed;
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
                KillQtp();
                ConsoleWriter.WriteLine(string.Format(Resources.GeneralErrorWithStack, e.Message, e.StackTrace));
                testResults.TestState = TestState.Error;
                testResults.ErrorDesc = Resources.QtpRunError;

                result.IsSuccess = false;
                return result;
            }
            catch (Exception e2)
            {

                ConsoleWriter.WriteLine(string.Format(Resources.GeneralErrorWithStack, e2.Message, e2.StackTrace));
                testResults.TestState = TestState.Error;
                testResults.ErrorDesc = Resources.QtpRunError;

                result.IsSuccess = false;
                return result;
            }


            return result;
        }

        private void KillQtp()
        {
            //error during run, process may have crashed (need to cleanup, close QTP and qtpRemote for next test to run correctly)
            CleanUp();

            //kill the qtp automation, to make sure it will run correctly next time
            Process[] processes = Process.GetProcessesByName("qtpAutomationAgent");
            Process qtpAuto = processes.Where(p => p.SessionId == Process.GetCurrentProcess().SessionId).FirstOrDefault();
            if (qtpAuto != null)
                qtpAuto.Kill();
        }

        private bool HandleOutputArguments(ref string errorReason)
        {
            try
            {
                var outputArguments = new XmlDocument { PreserveWhitespace = true };
                outputArguments.LoadXml("<Arguments/>");

                for (int i = 1; i <= _qtpParamDefs.Count; ++i)
                {
                    var pd = _qtpParamDefs[i];
                    if (pd.InOut == qtParameterDirection.qtParamDirOut)
                    {
                        var node = outputArguments.CreateElement(pd.Name);
                        var value = _qtpParameters[pd.Name].Value;
                        if (value != null)
                            node.InnerText = value.ToString();

                        outputArguments.DocumentElement.AppendChild(node);
                    }
                }
            }
            catch (Exception e)
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
                    legal = paramValue is string;
                    break;

                case qtParameterType.qtParamTypeString:
                    legal = paramValue is string;
                    break;

                default:
                    legal = true;
                    break;
            }

            return legal;
        }

        private bool HandleInputParameters(string fileName, ref string errorReason, Dictionary<string, object> inputParams, string dataTablePath)
        {
            try
            {
                string path = fileName;

                if (_runCancelled())
                {
                    QTPTestCleanup();
                    KillQtp();
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
                        if (inputParams.ContainsKey(paramName))
                        {
                            // first verify that the type is correct
                            object paramValue = inputParams[paramName];
                            if (!VerifyParameterValueType(paramValue, type))
                            {
                                ConsoleWriter.WriteErrLine(string.Format("Illegal input parameter type (skipped). param: '{0}'. expected type: '{1}'. actual type: '{2}'", paramName, Enum.GetName(typeof(qtParameterType), type), paramValue.GetType()));
                            }
                            else
                            {
                                _qtpParameters[paramName].Value = paramValue;
                            }
                        }
                    }
                }

                // specify data table path
                if (dataTablePath != null)
                {
                    _qtpApplication.Test.Settings.Resources.DataTablePath = dataTablePath;
                    ConsoleWriter.WriteLine("Using external data table: " + dataTablePath);
                }
            }
            catch (Exception e)
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
                        if (_qtpApplication.GetStatus().Equals("Running") || _qtpApplication.GetStatus().Equals("Busy"))
                        {
                            try
                            {
                                _qtpApplication.Test.Stop();
                            }
                            catch (Exception e)
                            {
                            }
                            finally
                            {

                            }
                        }
                    }
                }
            }
            catch (Exception ex)
            {
            }

            _qtpParameters = null;
            _qtpParamDefs = null;
            _qtpApplication = null;
        }


        /// <summary>
        /// Why we need this? If we run jenkins in a master slave node where there is a jenkins service installed in the slave machine, we need to change the DCOM settings as follow:
        /// dcomcnfg.exe -> My Computer -> DCOM Config -> QuickTest Professional Automation -> Identity -> and select The Interactive User
        /// </summary>
        private void ChangeDCOMSettingToInteractiveUser()
        {
            string errorMsg = "Unable to change DCOM settings. To chage it manually: " +
                              "run dcomcnfg.exe -> My Computer -> DCOM Config -> QuickTest Professional Automation -> Identity -> and select The Interactive User";

            string interactiveUser = "Interactive User";
            string runAs = "RunAs";

            try
            {
                var regKey = GetQuickTestProfessionalAutomationRegKey(RegistryView.Registry32);

                if (regKey == null)
                {
                    regKey = GetQuickTestProfessionalAutomationRegKey(RegistryView.Registry64);
                }

                if (regKey == null)
                    throw new Exception(@"Unable to find in registry SOFTWARE\Classes\AppID\{A67EB23A-1B8F-487D-8E38-A6A3DD150F0B");

                object runAsKey = regKey.GetValue(runAs);

                if (runAsKey == null || !runAsKey.ToString().Equals(interactiveUser))
                {
                    regKey.SetValue(runAs, interactiveUser);
                }

            }
            catch (Exception ex)
            {
                throw new Exception(errorMsg + "detailed error is : " + ex.Message);
            }


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
                ReportPath = "";
            }

            public bool IsSuccess { get; set; }
            public string ReportPath { get; set; }
        }
    }
}
