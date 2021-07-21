/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2021 Micro Focus or one of its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * ___________________________________________________________________
 */

using HpToolsLauncher.Properties;
using HpToolsLauncher.RTS;
using HpToolsLauncher.TestRunners;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Security.Cryptography;
using System.Text;

namespace HpToolsLauncher
{
    public enum CiName
    {
        Hudson,
        Jenkins,
        TFS,
        CCNET
    }

    public class McConnectionInfo
    {
        public string MobileUserName { get; set; }
        public string MobilePassword { get; set; }
        public string MobileHostAddress { get; set; }
        public string MobileHostPort { get; set; }
        public string MobileTenantId { get; set; }
        public int MobileUseSSL { get; set; }
        public int MobileUseProxy { get; set; }
        public int MobileProxyType { get; set; }
        public string MobileProxySetting_Address { get; set; }
        public int MobileProxySetting_Port { get; set; }
        public int MobileProxySetting_Authentication { get; set; }
        public string MobileProxySetting_UserName { get; set; }
        public string MobileProxySetting_Password { get; set; }


        public McConnectionInfo()
        {
            MobileHostPort = "8080";
            MobileUserName = string.Empty;
            MobilePassword = string.Empty;
            MobileHostAddress = string.Empty;
            MobileTenantId = string.Empty;
            MobileUseSSL = 0;

            MobileUseProxy = 0;
            MobileProxyType = 0;
            MobileProxySetting_Address = string.Empty;
            MobileProxySetting_Port = 0;
            MobileProxySetting_Authentication = 0;
            MobileProxySetting_UserName = string.Empty;
            MobileProxySetting_Password = string.Empty;
        }

        public override string ToString()
        {
            string McConnectionStr =
                 string.Format("UFT Mobile HostAddress: {0}, Port: {1}, Username: {2}, TenantId: {3}, UseSSL: {4}, UseProxy: {5}, ProxyType: {6}, ProxyAddress: {7}, ProxyPort: {8}, ProxyAuth: {9}, ProxyUser: {10}",
                 MobileHostAddress, MobileHostPort, MobileUserName, MobileTenantId, MobileUseSSL, MobileUseProxy, MobileProxyType, MobileProxySetting_Address, MobileProxySetting_Port, MobileProxySetting_Authentication,
                 MobileProxySetting_UserName);
            return McConnectionStr;
        }
    }

    public class Launcher
    {
        private IXmlBuilder _xmlBuilder;
        private bool _ciRun = false;
        private readonly string _paramFileName = null;
        private readonly JavaProperties _ciParams = new JavaProperties();
        private TestStorageType _runType;
        private readonly string _failOnUftTestFailed;
        private static ExitCodeEnum _exitCode = ExitCodeEnum.Passed;
        private const string _dateFormat = "dd/MM/yyyy HH:mm:ss";
        private bool _rerunFailedTests = false;

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
        /// <param name="failOnTestFailed"></param>
        /// <param name="paramFileName"></param>
        /// <param name="runType"></param>
        public Launcher(string failOnTestFailed, string paramFileName, TestStorageType runType)
        {
            _runType = runType;
            if (paramFileName != null)
                _ciParams.Load(paramFileName);
            _paramFileName = paramFileName;

            _failOnUftTestFailed = string.IsNullOrEmpty(failOnTestFailed) ? "N" : failOnTestFailed;
        }

        private static string _secretKey = "EncriptionPass4Java";

        /// <summary>
        /// decrypts strings which were encrypted by Encrypt (in the c# or java code, mainly for qc passwords)
        /// </summary>
        /// <param name="textToDecrypt"></param>
        /// <param name="key"></param>
        /// <returns></returns>
        string Decrypt(string textToDecrypt, string key)
        {
            RijndaelManaged rijndaelCipher = new RijndaelManaged();
            rijndaelCipher.Mode = CipherMode.CBC;
            rijndaelCipher.Padding = PaddingMode.PKCS7;

            rijndaelCipher.KeySize = 0x80;
            rijndaelCipher.BlockSize = 0x80;
            byte[] encryptedData = Convert.FromBase64String(textToDecrypt);
            byte[] pwdBytes = Encoding.UTF8.GetBytes(key);
            byte[] keyBytes = new byte[0x10];
            int len = pwdBytes.Length;
            if (len > keyBytes.Length)
            {
                len = keyBytes.Length;
            }
            Array.Copy(pwdBytes, keyBytes, len);
            rijndaelCipher.Key = keyBytes;
            rijndaelCipher.IV = keyBytes;
            byte[] plainText = rijndaelCipher.CreateDecryptor().TransformFinalBlock(encryptedData, 0, encryptedData.Length);
            return Encoding.UTF8.GetString(plainText);
        }

        /// <summary>
        /// encrypts strings to be decrypted by decrypt function(in the c# or java code, mainly for qc passwords)
        /// </summary>
        /// <param name="textToEncrypt"></param>
        /// <param name="key"></param>
        /// <returns></returns>
        string Encrypt(string textToEncrypt, string key)
        {
            RijndaelManaged rijndaelCipher = new RijndaelManaged();
            rijndaelCipher.Mode = CipherMode.CBC;
            rijndaelCipher.Padding = PaddingMode.PKCS7;

            rijndaelCipher.KeySize = 0x80;
            rijndaelCipher.BlockSize = 0x80;
            byte[] pwdBytes = Encoding.UTF8.GetBytes(key);
            byte[] keyBytes = new byte[0x10];
            int len = pwdBytes.Length;
            if (len > keyBytes.Length)
            {
                len = keyBytes.Length;
            }
            Array.Copy(pwdBytes, keyBytes, len);
            rijndaelCipher.Key = keyBytes;
            rijndaelCipher.IV = keyBytes;
            ICryptoTransform transform = rijndaelCipher.CreateEncryptor();
            byte[] plainText = Encoding.UTF8.GetBytes(textToEncrypt);
            return Convert.ToBase64String(transform.TransformFinalBlock(plainText, 0, plainText.Length));
        }

        /// <summary>
        /// writes to console using the ConsolWriter class
        /// </summary>
        /// <param name="message"></param>
        private static void WriteToConsole(string message)
        {
            ConsoleWriter.WriteLine(message);
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

            UniqueTimeStamp = _ciParams.ContainsKey("uniqueTimeStamp") ? _ciParams["uniqueTimeStamp"] : resultsFilename.ToLower().Replace("results", string.Empty).Replace(".xml", string.Empty);

            List<TestData> failedTests = new List<TestData>();


            //run the entire set of test once
            //create the runner according to type
            IAssetRunner runner = CreateRunner(_runType, true, failedTests);

            //runner instantiation failed (no tests to run or other problem)
            if (runner == null)
            {
                ConsoleWriter.WriteLine("empty runner;");
                Environment.Exit((int)ExitCodeEnum.Failed);
                return;
            }

            TestSuiteRunResults results = runner.Run();

            if (!_runType.Equals(TestStorageType.MBT))
            {
                RunTests(runner, resultsFilename, results);
            }

            if (_runType.Equals(TestStorageType.FileSystem))
            {
                string onCheckFailedTests = (_ciParams.ContainsKey("onCheckFailedTest") ? _ciParams["onCheckFailedTest"] : string.Empty);

                _rerunFailedTests = !string.IsNullOrEmpty(onCheckFailedTests) && Convert.ToBoolean(onCheckFailedTests.ToLower());

                //the "On failure" option is selected and the run build contains failed tests
                if (_rerunFailedTests && _exitCode != ExitCodeEnum.Passed)
                {
                    ConsoleWriter.WriteLine("There are failed tests.");

                    //rerun the selected tests (either the entire set, just the selected tests or only the failed tests)
                    List<TestRunResults> runResults = results.TestRuns;
                    int index = 0;
                    foreach (var item in runResults)
                    {
                        if (item.TestState == TestState.Failed || item.TestState == TestState.Error)
                        {
                            index++;
                            failedTests.Add(new TestData(item.TestPath, "FailedTest" + index));
                        }
                    }


                    //create the runner according to type
                    runner = CreateRunner(_runType, false, failedTests);

                    //runner instantiation failed (no tests to run or other problem)
                    if (runner == null)
                    {
                        Environment.Exit((int)ExitCodeEnum.Failed);
                        return;
                    }

                    TestSuiteRunResults rerunResults = runner.Run();

                    results.AppendResults(rerunResults);
                    RunTests(runner, resultsFilename, results);
                }
            }
        }

        /// <summary>
        /// creates the correct runner according to the given type
        /// </summary>
        /// <param name="runType"></param>
        /// <param name="initialTestRun"></param>
        private IAssetRunner CreateRunner(TestStorageType runType, bool initialTestRun, List<TestData> failedTests)
        {
            IAssetRunner runner = null;

            switch (runType)
            {
                case TestStorageType.AlmLabManagement:

                case TestStorageType.Alm:
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
                    List<string> sets = GetParamsWithPrefix("TestSet");

                    if (sets.Count == 0)
                    {
                        ConsoleWriter.WriteLine(Resources.LauncherNoTests);
                        return null;
                    }

                    //check if filterTests flag is selected; if yes apply filters on the list
                    bool isFilterSelected;
                    string filter = _ciParams.ContainsKey("FilterTests") ? _ciParams["FilterTests"] : string.Empty;

                    isFilterSelected = !string.IsNullOrEmpty(filter) && Convert.ToBoolean(filter.ToLower());

                    string filterByName = _ciParams.ContainsKey("FilterByName") ? _ciParams["FilterByName"] : string.Empty;

                    string statuses = _ciParams.ContainsKey("FilterByStatus") ? _ciParams["FilterByStatus"] : string.Empty;

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
                    string clientID = _ciParams.ContainsKey("almClientID") ? _ciParams["almClientID"] : string.Empty;
                    string apiKey = _ciParams.ContainsKey("almApiKeySecret") ? Decrypt(_ciParams["almApiKeySecret"], _secretKey) : string.Empty;
                    string almRunHost = _ciParams.ContainsKey("almRunHost") ? _ciParams["almRunHost"] : string.Empty;

                    //create an Alm runner
                    runner = new AlmTestSetsRunner(_ciParams["almServerUrl"],
                                     _ciParams["almUsername"],
                                     Decrypt(_ciParams["almPassword"], _secretKey),
                                     _ciParams["almDomain"],
                                     _ciParams["almProject"],
                                     dblQcTimeout,
                                     enmQcRunMode,
                                     almRunHost,
                                     sets,
                                     isFilterSelected,
                                     filterByName,
                                     filterByStatuses,
                                     initialTestRun,
                                     runType,
                                     isSSOEnabled,
                                     clientID, apiKey);
                    break;
                case TestStorageType.FileSystem:
                    bool displayController = _ciParams.ContainsKey("displayController") && _ciParams["displayController"] == "1";
                    string analysisTemplate = (_ciParams.ContainsKey("analysisTemplate") ? _ciParams["analysisTemplate"] : string.Empty);

                    List<TestData> validBuildTests = GetValidTests("Test", Resources.LauncherNoTestsFound, Resources.LauncherNoValidTests, string.Empty);

                    if (validBuildTests.Count == 0)
                    {
                        Environment.Exit((int)ExitCodeEnum.Failed);
                    }

                    //add build tests and cleanup tests in correct order
                    List<TestData> validTests = new List<TestData>();

                    if (!_rerunFailedTests)
                    {
                        ConsoleWriter.WriteLine("Run build tests");

                        //run only the build tests
                        foreach (var item in validBuildTests)
                        {
                            validTests.Add(item);
                        }
                    }
                    else
                    { //add also cleanup tests
                        string fsTestType = (_ciParams.ContainsKey("testType") ? _ciParams["testType"] : string.Empty);

                        List<TestData> validFailedTests = GetValidTests("FailedTest", Resources.LauncherNoFailedTestsFound, Resources.LauncherNoValidFailedTests, fsTestType);
                        List<TestData> validCleanupTests = new List<TestData>();
                        if (GetValidTests("CleanupTest", Resources.LauncherNoCleanupTestsFound, Resources.LauncherNoValidCleanupTests, fsTestType).Count > 0)
                        {
                            validCleanupTests = GetValidTests("CleanupTest", Resources.LauncherNoCleanupTestsFound, Resources.LauncherNoValidCleanupTests, fsTestType);
                        }
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
                                case "Rerun the entire set of tests": ConsoleWriter.WriteLine("The entire test set will run again."); break;
                                case "Rerun specific tests in the build": ConsoleWriter.WriteLine("Only the selected tests will run again."); break;
                                case "Rerun only failed tests": ConsoleWriter.WriteLine("Only the failed tests will run again."); break;
                            }

                            for (int i = 0; i < numberOfReruns.Count; i++)
                            {
                                var currentRerun = numberOfReruns.ElementAt(i);

                                if (fsTestType.Equals("Rerun the entire set of tests"))
                                {
                                    while (currentRerun > 0)
                                    {
                                        if (validCleanupTests.Count > 0)
                                        {
                                            validTests.Add(validCleanupTests.ElementAt(i));
                                        }

                                        foreach (var item in validFailedTests)
                                        {
                                            validTests.Add(item);
                                        }

                                        currentRerun--;
                                    }
                                }

                                if (fsTestType.Equals("Rerun specific tests in the build"))
                                {
                                    while (currentRerun > 0)
                                    {
                                        if (validCleanupTests.Count > 0)
                                        {
                                            validTests.Add(validCleanupTests.ElementAt(i));
                                        }

                                        validTests.Add(validFailedTests.ElementAt(i));

                                        currentRerun--;
                                    }
                                }

                                if (fsTestType.Equals("Rerun only failed tests"))
                                {
                                    while (currentRerun > 0)
                                    {
                                        if (validCleanupTests.Count > 0)
                                        {
                                            validTests.Add(validCleanupTests.ElementAt(i));
                                        }

                                        if (failedTests.Count != 0)
                                        {
                                            validTests.AddRange(failedTests);
                                        }
                                        else
                                        {
                                            Console.WriteLine("There are no failed tests to rerun.");
                                            break;
                                        }

                                        currentRerun--;
                                    }
                                }
                            }
                        }
                    }

                    //get the tests
                    //IEnumerable<string> tests = GetParamsWithPrefix("Test");

                    IEnumerable<string> jenkinsEnvVariablesWithCommas = GetParamsWithPrefix("JenkinsEnv");
                    Dictionary<string, string> jenkinsEnvVariables = new Dictionary<string, string>();
                    foreach (string var in jenkinsEnvVariablesWithCommas)
                    {
                        string[] nameVal = var.Split(",;".ToCharArray());
                        jenkinsEnvVariables.Add(nameVal[0], nameVal[1]);
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
                    McConnectionInfo mcConnectionInfo = new McConnectionInfo();
                    if (_ciParams.ContainsKey("MobileHostAddress"))
                    {
                        string mcServerUrl = _ciParams["MobileHostAddress"];

                        if (!string.IsNullOrEmpty(mcServerUrl))
                        {
                            //url is something like http://xxx.xxx.xxx.xxx:8080
                            string[] strArray = mcServerUrl.Split(new char[] { ':' });
                            if (strArray.Length == 3)
                            {
                                mcConnectionInfo.MobileHostAddress = strArray[1].Replace("/", string.Empty);
                                mcConnectionInfo.MobileHostPort = strArray[2];
                            }

                            //mc username
                            if (_ciParams.ContainsKey("MobileUserName"))
                            {
                                string mcUsername = _ciParams["MobileUserName"];
                                if (!string.IsNullOrEmpty(mcUsername))
                                {
                                    mcConnectionInfo.MobileUserName = mcUsername;
                                }
                            }

                            //mc password
                            if (_ciParams.ContainsKey("MobilePassword"))
                            {
                                string mcPassword = _ciParams["MobilePassword"];
                                if (!string.IsNullOrEmpty(mcPassword))
                                {
                                    mcConnectionInfo.MobilePassword = Decrypt(mcPassword, _secretKey);
                                }
                            }

                            //mc tenantId
                            if (_ciParams.ContainsKey("MobileTenantId"))
                            {
                                string mcTenantId = _ciParams["MobileTenantId"];
                                if (!string.IsNullOrEmpty(mcTenantId))
                                {
                                    mcConnectionInfo.MobileTenantId = mcTenantId;
                                }
                            }

                            //ssl
                            if (_ciParams.ContainsKey("MobileUseSSL"))
                            {
                                string mcUseSSL = _ciParams["MobileUseSSL"];
                                if (!string.IsNullOrEmpty(mcUseSSL))
                                {
                                    mcConnectionInfo.MobileUseSSL = int.Parse(mcUseSSL);
                                }
                            }

                            //Proxy enabled flag
                            if (_ciParams.ContainsKey("MobileUseProxy"))
                            {
                                string useProxy = _ciParams["MobileUseProxy"];
                                if (!string.IsNullOrEmpty(useProxy))
                                {
                                    mcConnectionInfo.MobileUseProxy = int.Parse(useProxy);
                                }
                            }

                            //Proxy type
                            if (_ciParams.ContainsKey("MobileProxyType"))
                            {
                                string proxyType = _ciParams["MobileProxyType"];
                                if (!string.IsNullOrEmpty(proxyType))
                                {
                                    mcConnectionInfo.MobileProxyType = int.Parse(proxyType);
                                }
                            }

                            //proxy address
                            string proxyAddress = _ciParams.GetOrDefault("MobileProxySetting_Address");
                            CheckAndSetMobileProxySettings(ref mcConnectionInfo, proxyAddress);

                            //Proxy authentication
                            if (_ciParams.ContainsKey("MobileProxySetting_Authentication"))
                            {
                                string proxyAuthentication = _ciParams["MobileProxySetting_Authentication"];
                                if (!string.IsNullOrEmpty(proxyAuthentication))
                                {
                                    mcConnectionInfo.MobileProxySetting_Authentication = int.Parse(proxyAuthentication);
                                }
                            }

                            //Proxy username
                            if (_ciParams.ContainsKey("MobileProxySetting_UserName"))
                            {
                                string proxyUsername = _ciParams["MobileProxySetting_UserName"];
                                if (!string.IsNullOrEmpty(proxyUsername))
                                {
                                    mcConnectionInfo.MobileProxySetting_UserName = proxyUsername;
                                }
                            }

                            //Proxy password
                            if (_ciParams.ContainsKey("MobileProxySetting_Password"))
                            {
                                string proxyPassword = _ciParams["MobileProxySetting_Password"];
                                if (!string.IsNullOrEmpty(proxyPassword))
                                {
                                    mcConnectionInfo.MobileProxySetting_Password = Decrypt(proxyPassword, _secretKey);
                                }
                            }
                        }
                    }

                    // other mobile info
                    string mobileinfo = string.Empty;
                    if (_ciParams.ContainsKey("mobileinfo"))
                    {
                        mobileinfo = _ciParams["mobileinfo"];
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
                                reportPath = jenkinsEnvVariables[fsReportPath];
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

                    SummaryDataLogger summaryDataLogger = GetSummaryDataLogger();
                    List<ScriptRTSModel> scriptRTSSet = GetScriptRtsSet();
                    if (_ciParams.ContainsKey("fsUftRunMode"))
                    {
                        string uftRunMode = _ciParams["fsUftRunMode"];
                        runner = new FileSystemTestsRunner(validTests, timeout, uftRunMode, pollingInterval, perScenarioTimeOutMinutes, ignoreErrorStrings, jenkinsEnvVariables, mcConnectionInfo, mobileinfo, parallelRunnerEnvironments, displayController, analysisTemplate, summaryDataLogger, scriptRTSSet, reportPath);
                    }
                    else
                    {
                        runner = new FileSystemTestsRunner(validTests, timeout, pollingInterval, perScenarioTimeOutMinutes, ignoreErrorStrings, jenkinsEnvVariables, mcConnectionInfo, mobileinfo, parallelRunnerEnvironments, displayController, analysisTemplate, summaryDataLogger, scriptRTSSet, reportPath);
                    }

                    break;

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
            return runner;
        }

        private void CheckAndSetMobileProxySettings(ref McConnectionInfo mcConnectionInfo, string proxyAddress)
        {
            if (!string.IsNullOrEmpty(proxyAddress))
            {
                // data is something like "16.105.9.23:8080"
                string[] strArrayForProxyAddress = proxyAddress.Split(new char[] { ':' });
                if (strArrayForProxyAddress.Length == 2)
                {
                    mcConnectionInfo.MobileProxySetting_Address = strArrayForProxyAddress[0];
                    mcConnectionInfo.MobileProxySetting_Port = int.Parse(strArrayForProxyAddress[1]);
                }
            }
        }

        private List<string> GetParamsWithPrefix(string prefix)
        {
            int idx = 1;
            List<string> parameters = new List<string>();
            while (_ciParams.ContainsKey(prefix + idx))
            {
                string set = _ciParams[prefix + idx];
                if (set.StartsWith("Root\\"))
                    set = set.Substring(5);
                set = set.TrimEnd("\\".ToCharArray());
                parameters.Add(set.TrimEnd());
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
                set = set.TrimEnd("\\".ToCharArray());
                string key = prefix + idx;
                dict[key] = set.TrimEnd();
                ++idx;
            }

            return dict;
        }

        /// <summary>
        /// used by the run fuction to run the tests
        /// </summary>
        /// <param name="runner"></param>
        /// <param name="resultsFile"></param>
        ///
        private void RunTests(IAssetRunner runner, string resultsFile, TestSuiteRunResults results)
        {
            try
            {
                if (_ciRun)
                {
                    _xmlBuilder = new JunitXmlBuilder();
                    _xmlBuilder.XmlName = resultsFile;
                }

                if (results == null)
                {
                    Environment.Exit((int)ExitCodeEnum.Failed);
                    return;
                }

                _xmlBuilder.CreateXmlFromRunResults(results);

                if (results.TestRuns.Count == 0)
                {
                    Console.WriteLine("No tests were run");
                    _exitCode = ExitCodeEnum.Failed;
                    Environment.Exit((int)_exitCode);
                }

                //if there is an error
                if (results.TestRuns.Any(tr => tr.TestState == TestState.Failed || tr.TestState == TestState.Error))
                {
                    _exitCode = ExitCodeEnum.Failed;
                }

                int numFailures = results.TestRuns.Count(t => t.TestState == TestState.Failed);
                int numSuccess = results.TestRuns.Count(t => t.TestState == TestState.Passed);
                int numErrors = results.TestRuns.Count(t => t.TestState == TestState.Error);
                int numWarnings = results.TestRuns.Count(t => t.TestState == TestState.Warning);

                if ((numErrors <= 0) && (numFailures > 0))
                {
                    _exitCode = ExitCodeEnum.Failed;
                }

                if ((numErrors <= 0) && (numFailures > 0) && (numSuccess > 0))
                {
                    _exitCode = ExitCodeEnum.Unstable;
                }

                foreach (var testRun in results.TestRuns)
                {
                    if (testRun.FatalErrors > 0 && !string.IsNullOrWhiteSpace(testRun.TestPath))
                    {
                        _exitCode = ExitCodeEnum.Failed;
                        break;
                    }
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
                        runStatus = "Job unstable (Passed with failed tests)";
                        break;
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

                ConsoleWriter.WriteLine(Resources.LauncherDoubleSeperator);
                ConsoleWriter.WriteLine(string.Format(Resources.LauncherDisplayStatistics, runStatus, results.TestRuns.Count, numSuccess, numFailures, numErrors, numWarnings));

                int testIndex = 1;
                if (!runner.RunWasCancelled)
                {
                    results.TestRuns.ForEach(tr => { ConsoleWriter.WriteLine(((tr.HasWarnings) ? "Warning".PadLeft(7) : tr.TestState.ToString().PadRight(7)) + ": " + tr.TestPath + "[" + testIndex + "]"); testIndex++; });

                    ConsoleWriter.WriteLine(Resources.LauncherDoubleSeperator);

                    if (ConsoleWriter.ErrorSummaryLines != null && ConsoleWriter.ErrorSummaryLines.Count > 0)
                    {
                        ConsoleWriter.WriteLine("Job Errors summary:");
                        ConsoleWriter.ErrorSummaryLines.ForEach(line => ConsoleWriter.WriteLine(line));
                    }

                    string onCheckFailedTests = (_ciParams.ContainsKey("onCheckFailedTest") ? _ciParams["onCheckFailedTest"] : string.Empty);

                    _rerunFailedTests = !string.IsNullOrEmpty(onCheckFailedTests) && Convert.ToBoolean(onCheckFailedTests.ToLower());

                    if (!_rerunFailedTests)
                    {
                        Environment.Exit((int)_exitCode);
                    }
                }
            }
            finally
            {
                try
                {
                    runner.Dispose();
                }
                catch (Exception ex)
                {
                    ConsoleWriter.WriteLine(string.Format(Resources.LauncherRunnerDisposeError, ex.Message));
                };
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
                        summaryDataLogFlags[0].Equals("1"),
                        summaryDataLogFlags[1].Equals("1"),
                        summaryDataLogFlags[2].Equals("1"),
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
        /// <param name="propertiesParameter"></param>
        /// <param name="errorNoTestsFound"></param>
        /// <param name="errorNoValidTests"></param>
        /// <returns>a list of tests</returns>
        private List<TestData> GetValidTests(string propertiesParameter, string errorNoTestsFound, string errorNoValidTests, string fsTestType)
        {
            if (fsTestType != "Rerun only failed tests" || propertiesParameter == "CleanupTest")
            {
                List<TestData> tests = new List<TestData>();
                Dictionary<string, string> testsKeyValue = GetKeyValuesWithPrefix(propertiesParameter);
                if (propertiesParameter == "CleanupTest" && testsKeyValue.Count == 0)
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

                List<TestData> validTests = Helper.ValidateFiles(tests);

                if (tests.Count <= 0 || validTests.Count != 0) return validTests;

                //no valid tests found
                ConsoleWriter.WriteLine(errorNoValidTests);
            }

            return new List<TestData>();
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
