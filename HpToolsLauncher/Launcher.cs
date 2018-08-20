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

using System;
using System.Collections.Generic;
using System.Linq;
using System.Security.Cryptography;
using System.Text;
using HpToolsLauncher.Properties;

namespace HpToolsLauncher
{
    public enum CIName
    {
        Hudson,
        Jenkins,
        TFS,
        CCNET,
    }

    public class McConnectionInfo
    {
        public string MobileUserName { get; set; }
        public string MobilePassword{ get; set; }
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


        public McConnectionInfo() {
            MobileHostPort = "8080";
            MobileUserName = "";
            MobilePassword = "";
            MobileHostAddress = "";
            MobileTenantId = "";
            MobileUseSSL = 0;

            MobileUseProxy = 0;
            MobileProxyType = 0;
            MobileProxySetting_Address = "";
            MobileProxySetting_Port = 0;
            MobileProxySetting_Authentication = 0;
            MobileProxySetting_UserName = "";
            MobileProxySetting_Password = "";

        }

        public override string ToString()
        {
            string McConnectionStr = 
                string.Format("Mc HostAddress: {0}, McPort: {1}, Username: {2}, TenantId: {3}, UseSSL: {4}, UseProxy: {5}, ProxyType: {6}, ProxyAddress: {7}, ProxyPort: {8}, ProxyAuth: {9}, ProxyUser: {10}",
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
        private JavaProperties _ciParams = new JavaProperties();
        private TestStorageType _runtype = TestStorageType.Unknown;
        private readonly string _failOnUftTestFailed;
        private static ExitCodeEnum _exitCode = ExitCodeEnum.Passed;
        private static string _dateFormat = "dd/MM/yyyy HH:mm:ss";

        public static string DateFormat
        {
            get { return Launcher._dateFormat; }
            set { Launcher._dateFormat = value; }
        }

        /// <summary>
        /// if running an alm job theses strings are mandatory:
        /// </summary>
        private string[] requiredParamsForQcRun = { "almServerUrl",
                                 "almUserName",
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

        public enum ExitCodeEnum
        {
            Passed = 0,
            Failed = -1,
            Unstable = -2,
            Aborted = -3
        }
        /// <summary>
        /// saves the exit code in case we want to run all tests but fail at the end since a file wasn't found
        /// </summary>
        public static ExitCodeEnum ExitCode
        {
            get { return Launcher._exitCode; }
            set { Launcher._exitCode = value; }
        }


        /// <summary>
        /// constructor
        /// </summary>
        /// <param name="failOnTestFailed"></param>
        /// <param name="paramFileName"></param>
        /// <param name="runtype"></param>
        public Launcher(string failOnTestFailed, string paramFileName, TestStorageType runtype)
        {
            _runtype = runtype;
            if (paramFileName != null)
                _ciParams.Load(paramFileName);
            _paramFileName = paramFileName;

            _failOnUftTestFailed = string.IsNullOrEmpty(failOnTestFailed) ? "N" : failOnTestFailed;
        }

        static String secretkey = "EncriptionPass4Java";

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
            if (_runtype == TestStorageType.Unknown)
                Enum.TryParse<TestStorageType>(_ciParams["runType"], true, out _runtype);
            if (_runtype == TestStorageType.Unknown)
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

            if (_ciParams.ContainsKey("uniqueTimeStamp"))
            {
                UniqueTimeStamp = _ciParams["uniqueTimeStamp"];
            }
            else
            {
                UniqueTimeStamp = resultsFilename.ToLower().Replace("results", "").Replace(".xml", "");
            }

            //create the runner according to type
            IAssetRunner runner = CreateRunner(_runtype, _ciParams);

            //runner instantiation failed (no tests to run or other problem)
            if (runner == null)
            {
                Environment.Exit((int)Launcher.ExitCodeEnum.Failed);
            }

            //run the tests!
            RunTests(runner, resultsFilename);

            //Console.WriteLine("Press any key to exit...");
            //Console.ReadKey();
            ConsoleQuickEdit.Enable();
            if (Launcher.ExitCode != ExitCodeEnum.Passed)
                Environment.Exit((int)Launcher.ExitCode);
        }

        /// <summary>
        /// creates the correct runner according to the given type
        /// </summary>
        /// <param name="runType"></param>
        /// <param name="ciParams"></param>
        IAssetRunner CreateRunner(TestStorageType runType, JavaProperties ciParams)
        {
            IAssetRunner runner = null;
            switch (runType)
            {
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
                    double dblQcTimeout = int.MaxValue;
                    if (!double.TryParse(_ciParams["almTimeout"], out dblQcTimeout))
                    {
                        ConsoleWriter.WriteLine(Resources.LauncherTimeoutNotNumeric);
                        dblQcTimeout = int.MaxValue;
                    }

                    ConsoleWriter.WriteLine(string.Format(Resources.LuancherDisplayTimout, dblQcTimeout));

                    QcRunMode enmQcRunMode = QcRunMode.RUN_LOCAL;
                    if (!Enum.TryParse<QcRunMode>(_ciParams["almRunMode"], true, out enmQcRunMode))
                    {
                        ConsoleWriter.WriteLine(Resources.LauncherIncorrectRunmode);
                        enmQcRunMode = QcRunMode.RUN_LOCAL;
                    }
                    ConsoleWriter.WriteLine(string.Format(Resources.LauncherDisplayRunmode, enmQcRunMode.ToString()));

                    //go over testsets in the parameters, and collect them
                    List<string> sets = GetParamsWithPrefix("TestSet");

                    if (sets.Count == 0)
                    {
                        ConsoleWriter.WriteLine(Resources.LauncherNoTests);
                        return null;
                    }

                    //create an Alm runner
                    runner = new AlmTestSetsRunner(_ciParams["almServerUrl"],
                                     _ciParams["almUserName"],
                                     Decrypt(_ciParams["almPassword"], secretkey),
                                     _ciParams["almDomain"],
                                     _ciParams["almProject"],
                                     dblQcTimeout,
                                     enmQcRunMode,
                                     _ciParams["almRunHost"],
                                     sets);
                    break;
                case TestStorageType.FileSystem:
                    //Get displayController var
                    bool displayController = false;
                    if (_ciParams.ContainsKey("displayController")) {
                        if (_ciParams["displayController"] == "1")
                        {
                            displayController = true;
                        }
                    }
                    string analysisTemplate = (_ciParams.ContainsKey("analysisTemplate") ? _ciParams["analysisTemplate"] : "");

                    Dictionary<string, string> testsKeyValue = GetKeyValuesWithPrefix("Test");
                    List<TestData> tests = new List<TestData>();

                    foreach(var item in testsKeyValue)
                    {
                        tests.Add(new TestData(item.Value, item.Key));
                    }

                    //get the tests
                    //IEnumerable<string> tests = GetParamsWithPrefix("Test");

                    IEnumerable<string> jenkinsEnvVariablesWithCommas = GetParamsWithPrefix("JenkinsEnv");
                    Dictionary<string, string> jenkinsEnvVariables = new Dictionary<string,string>();
                    foreach (string var in jenkinsEnvVariablesWithCommas)
                    { 
                        string[] nameVal = var.Split(",;".ToCharArray());
                        jenkinsEnvVariables.Add(nameVal[0], nameVal[1]);
                    }
                    //parse the timeout into a TimeSpan
                    TimeSpan timeout = TimeSpan.MaxValue;
                    if (_ciParams.ContainsKey("fsTimeout"))
                    {
                        string strTimoutInSeconds = _ciParams["fsTimeout"];
                        if (strTimoutInSeconds.Trim() != "-1")
                        {
                            int intTimoutInSeconds = 0;
                            int.TryParse(strTimoutInSeconds, out intTimoutInSeconds);
                            timeout = TimeSpan.FromSeconds(intTimoutInSeconds);
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
                        string strTimoutInMinutes = _ciParams["PerScenarioTimeOut"];
                        //ConsoleWriter.WriteLine("reading PerScenarioTimeout: "+ strTimoutInMinutes);
                        if (strTimoutInMinutes.Trim() != "-1")
                        {
                            int intTimoutInMinutes = 0;
                            if (int.TryParse(strTimoutInMinutes, out intTimoutInMinutes))
                                perScenarioTimeOutMinutes = TimeSpan.FromMinutes(intTimoutInMinutes);
                            //ConsoleWriter.WriteLine("PerScenarioTimeout: "+perScenarioTimeOutMinutes+" minutes");
                        }
                    }
                    ConsoleWriter.WriteLine("PerScenarioTimeout: " + perScenarioTimeOutMinutes.ToString(@"dd\:\:hh\:mm\:ss") + " minutes");

                    char[] delim = { '\n' };
                    List<string> ignoreErrorStrings = new List<string>();
                    if (_ciParams.ContainsKey("ignoreErrorStrings"))
                    {
                        if (_ciParams.ContainsKey("ignoreErrorStrings"))
                        {
                            ignoreErrorStrings.AddRange(Array.ConvertAll(_ciParams["ignoreErrorStrings"].Split(delim, StringSplitOptions.RemoveEmptyEntries), ignoreError => ignoreError.Trim()));
                        }
                    }

                    if (tests == null || tests.Count() == 0)
                    {
                        WriteToConsole(Resources.LauncherNoTestsFound);
                    }

                    List<TestData> validTests = Helper.ValidateFiles(tests);

                    if (tests != null && tests.Count() > 0 && validTests.Count == 0)
                    {
                        ConsoleWriter.WriteLine(Resources.LauncherNoValidTests);
                        return null;
                    }
                    
                    //If a file path was provided and it doesn't exist stop the analysis launcher
                    if (!analysisTemplate.Equals("") && !Helper.FileExists(analysisTemplate)) {
                        return null;
                    }
                    
                    //--MC connection info
                    McConnectionInfo mcConnectionInfo = new McConnectionInfo();
                    if (_ciParams.ContainsKey("MobileHostAddress"))
                    {
                        string mcServerUrl = _ciParams["MobileHostAddress"];

                        if (!string.IsNullOrEmpty(mcServerUrl) )
                        {
                            //url is something like http://xxx.xxx.xxx.xxx:8080
                            string[] strArray = mcServerUrl.Split(new Char[] { ':' });
                            if (strArray.Length == 3)
                            {
                                mcConnectionInfo.MobileHostAddress = strArray[1].Replace("/", "");
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
                                    mcConnectionInfo.MobilePassword = Decrypt(mcPassword, secretkey);
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
                            if (_ciParams.ContainsKey("MobileProxySetting_Address"))
                            {
                                string proxyAddress = _ciParams["MobileProxySetting_Address"];
                                if (!string.IsNullOrEmpty(proxyAddress))
                                {
                                    // data is something like "16.105.9.23:8080"
                                    string[] strArray4ProxyAddr = proxyAddress.Split(new Char[] { ':' });

                                    if (strArray4ProxyAddr.Length == 2)
                                    {
                                        mcConnectionInfo.MobileProxySetting_Address = strArray4ProxyAddr[0];
                                        mcConnectionInfo.MobileProxySetting_Port = int.Parse(strArray4ProxyAddr[1]);
                                    }
                                }
                            }

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
                                    mcConnectionInfo.MobileProxySetting_Password = Decrypt(proxyPassword, secretkey);
                                }
                            }
                            
                        }
                    }
                    
                    // other mobile info
                    string mobileinfo = "";
                    if (_ciParams.ContainsKey("mobileinfo"))
                    {
                        mobileinfo = _ciParams["mobileinfo"];
                    }

                    Dictionary<string, List<String>> parallelRunnerEnvironments = new Dictionary<string, List<string>>();

                    // retrieve the parallel runner environment for each test
                    if(_ciParams.ContainsKey("parallelRunnerMode"))
                    {
                        foreach(var test in validTests)
                        {
                            string envKey = "Parallel" + test.Id + "Env";
                            List<string> testEnvironments = GetParamsWithPrefix(envKey);

                            // add the environments for all the valid tests
                            parallelRunnerEnvironments.Add(test.Id, testEnvironments);
                        }
                    }

                    if (_ciParams.ContainsKey("fsUftRunMode"))
                    {
                        string uftRunMode = "Fast";
                        uftRunMode = _ciParams["fsUftRunMode"];
                        runner = new FileSystemTestsRunner(validTests, timeout, uftRunMode, pollingInterval, perScenarioTimeOutMinutes, ignoreErrorStrings, jenkinsEnvVariables, mcConnectionInfo, mobileinfo, parallelRunnerEnvironments, displayController, analysisTemplate);
                    }
                    else
                    {
                        runner = new FileSystemTestsRunner(validTests, timeout, pollingInterval, perScenarioTimeOutMinutes, ignoreErrorStrings, jenkinsEnvVariables, mcConnectionInfo, mobileinfo, parallelRunnerEnvironments, displayController, analysisTemplate);
                    }

                    break;

                default:
                    runner = null;
                    break;
            }
            return runner;
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

        private Dictionary<string,string> GetKeyValuesWithPrefix(string prefix)
        {
            int idx = 1;

            Dictionary<string, string> dict = new Dictionary<string, string>();

            while(_ciParams.ContainsKey(prefix + idx))
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
        private void RunTests(IAssetRunner runner, string resultsFile)
        {
            try
            {
                if (_ciRun)
                {
                    _xmlBuilder = new JunitXmlBuilder();
                    _xmlBuilder.XmlName = resultsFile;
                }

                TestSuiteRunResults results = runner.Run();

                if (results == null)
                    Environment.Exit((int)Launcher.ExitCodeEnum.Failed);

                _xmlBuilder.CreateXmlFromRunResults(results);

                //if there is an error
                if (results.TestRuns.Any(tr => tr.TestState == TestState.Failed || tr.TestState == TestState.Error))
                {
                    Launcher.ExitCode = Launcher.ExitCodeEnum.Failed;
                }

                int numFailures = results.TestRuns.Count(t => t.TestState == TestState.Failed);
                int numSuccess = results.TestRuns.Count(t => t.TestState == TestState.Passed);
                int numErrors = results.TestRuns.Count(t => t.TestState == TestState.Error);

                //TODO: Temporery fix to remove since jenkins doesnt retrive resutls from jobs that marked as failed and unstable marks jobs with only failed tests
                if ((numErrors <= 0) && (numFailures > 0))
                {
                    Launcher.ExitCode = Launcher.ExitCodeEnum.Unstable;
                }

                foreach (var testRun in results.TestRuns)
                {
                    if (testRun.FatalErrors > 0 && !testRun.TestPath.Equals(""))
                    {
                        Launcher.ExitCode = Launcher.ExitCodeEnum.Failed;
                        break;
                    }
                }

                //this is the total run summary
                ConsoleWriter.ActiveTestRun = null;
                string runStatus = "";
                switch (Launcher.ExitCode)
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
                ConsoleWriter.WriteLine(string.Format(Resources.LauncherDisplayStatistics, runStatus, results.TestRuns.Count, numSuccess, numFailures, numErrors));

                if (!runner.RunWasCancelled)
                {
                    results.TestRuns.ForEach(tr => ConsoleWriter.WriteLine(((tr.HasWarnings) ? "Warning".PadLeft(7) : tr.TestState.ToString().PadRight(7)) + ": " + tr.TestPath));
                    
                    ConsoleWriter.WriteLine(Resources.LauncherDoubleSeperator);
                    if (ConsoleWriter.ErrorSummaryLines != null && ConsoleWriter.ErrorSummaryLines.Count > 0)
                    {
                        ConsoleWriter.WriteLine("Job Errors summary:");
                        ConsoleWriter.ErrorSummaryLines.ForEach(line => ConsoleWriter.WriteLine(line));
                    }
     
                }

                //ConsoleWriter.WriteLine("Returning " + runStatus + ".");
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

    }
}
