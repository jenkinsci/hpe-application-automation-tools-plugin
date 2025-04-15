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
using HpToolsLauncher.Properties;
using Microsoft.Win32;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Web.UI;
using System.Xml;
using System.Xml.Linq;
using System.Xml.XPath;
using System.Xml.Xsl;

namespace HpToolsLauncher.Utils
{
    public enum TestType
    {
        Unknown,
        QTP,
        ST,
        LoadRunner,
        ParallelRunner
    }

    public enum TestState
    {
        Waiting,
        Running,
        NoRun,
        Passed,
        Failed,
        Error,
        Warning,
        Unknown,
    }

    public enum TestResult
    {
        Passed,
        Failed,
        Warning,
        Done,
    }

    public static class Helper
    {
        #region Constants

        public const string FT_REG_ROOT = @"SOFTWARE\Mercury Interactive\QuickTest Professional\CurrentVersion";

        internal const string FT_REG_ROOT_64_BIT = @"SOFTWARE\Wow6432Node\Mercury Interactive\QuickTest Professional\CurrentVersion";

        public const string FT_ROOT_PATH_KEY = "QuickTest Professional";
        private const string QTP_ROOT_ENV_VAR_NAME = "QTP_TESTS_ROOT";

        public const string ServiceTestRegistryKey = @"SOFTWARE\Hewlett-Packard\HP Service Test";
        public const string ServiceTesCurrentVersionRegistryKey = ServiceTestRegistryKey + @"\CurrentVersion";

        public const string ServiceTesWOW64RegistryKey = @"SOFTWARE\Wow6432Node\Hewlett-Packard\HP Service Test";
        public const string ServiceTesCurrentVersionWOW64RegistryKey = ServiceTesWOW64RegistryKey + @"\CurrentVersion";

        public const string LoadRunnerRegistryKey = @"SOFTWARE\Mercury Interactive\LoadRunner";
        public const string LoadRunner64RegisryKey = @"SOFTWARE\Wow6432Node\Mercury Interactive\LoadRunner";
        public const string LoadRunnerControllerRegistryKey = @"CustComponent\Controller\CurrentVersion";
        public const string LoadRunnerControllerDirRegistryKey = @"\CurrentVersion";

        public const string LoadRunnerControllerDirRegistryValue = @"\Controller";

        public static readonly System.Collections.ObjectModel.ReadOnlyCollection<string> LoadRunnerENVVariables =
            new System.Collections.ObjectModel.ReadOnlyCollection<string>(new[] { "LG_PATH", "LR_PATH" });

        public const string InstalltionFolderValue = "LOCAL_MLROOT";

        public const string UftViewerInstalltionFolderRegistryKey = @"SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\{E86D56AE-6660-4357-890F-8B79AB7A8F7B}";

        public const string UftViewerInstalltionFolderRegistryKey64Bit = @"SOFTWARE\Wow6432Node\Microsoft\Windows\CurrentVersion\Uninstall\{E86D56AE-6660-4357-890F-8B79AB7A8F7B}";

        public const string ResultsFileName = "Results.xml";
        public const string QTPReportProcessPath = @"bin\reportviewer.exe";

        public const string STFileExt = ".st";
        public const string QTPFileExt = ".tsp";
        public const string LoadRunnerFileExt = ".lrs";
        public const string MtbFileExt = ".mtb";
        public const string MtbxFileExt = ".mtbx";

        private const string SOFTWARE_Classes_AppID_A67EB23A = @"SOFTWARE\Classes\AppID\{A67EB23A-1B8F-487D-8E38-A6A3DD150F0B}";

        private const string SYSTEM_CURRENTCONTROLSET_CONTROL = @"SYSTEM\CurrentControlSet\Control";
        private const string INTERACTIVE_USER = "Interactive User";
        private const string RUN_AS = "RunAs";
        private const string CONTAINER_TYPE = "ContainerType";
        private const string UNABLE_TO_CHANGE_DCOM_SETTINGS = "Unable to change DCOM settings. To change it manually: run dcomcnfg.exe -> My Computer -> DCOM Config -> QuickTest Professional Automation -> Identity -> and select The Interactive User.";
        private const string BYPASS_DCOM_SETTINGS_CHECK = "Bypass DCOM settings check, since the process runs inside a Docker container.";

        #endregion

        public static Assembly HPToolsAssemblyResolver(object sender, ResolveEventArgs args)
        {
            AssemblyName asmName = new AssemblyName(args.Name);
            if (asmName == null) return null;

            string assemblyName = asmName.Name;
            if (assemblyName.EndsWith(".resources")) return null;

            if (assemblyName == "HpToolsLauncher.XmlSerializers") return null;

            string installtionPath = null;
            installtionPath = Helper.getLRInstallPath();
            if (installtionPath == null)
            {
                ConsoleWriter.WriteErrLine(string.Format(Resources.LoadRunnerNotInstalled,
                    System.Environment.MachineName));
                Environment.Exit((int)Launcher.ExitCodeEnum.Aborted);
            }

            installtionPath = Path.Combine(installtionPath, "bin");

            Assembly ans;
            if (!File.Exists(Path.Combine(installtionPath, assemblyName + ".dll")))
            {
                //resource!
                ConsoleWriter.WriteErrLine("cannot locate " + assemblyName + ".dll in installation directory");
                Environment.Exit((int)Launcher.ExitCodeEnum.Aborted);
            }
            else
            {
                //Console.WriteLine("loading " + assemblyName + " from " + Path.Combine(installtionPath, assemblyName + ".dll"));
                ans = Assembly.LoadFrom(Path.Combine(installtionPath, assemblyName + ".dll"));

                AssemblyName loadedName = ans.GetName();
                if (loadedName.Name == "Interop.Wlrun")
                {
                    if (loadedName.Version.Major > 11 ||
                        (loadedName.Version.Major == 11 && loadedName.Version.Minor >= 52))
                    {
                        return ans;
                    }
                    else
                    {
                        ConsoleWriter.WriteErrLine(string.Format(Resources.HPToolsAssemblyResolverWrongVersion,
                            Environment.MachineName));
                        Environment.Exit((int)Launcher.ExitCodeEnum.Aborted);
                    }
                }
                else
                {
                    return ans;
                }
            }
            return null;
        }

        public static string GetRootDirectoryPath()
        {
            string directoryPath;
            RegistryKey regkey = Registry.LocalMachine.OpenSubKey(FT_REG_ROOT);

            if (regkey != null)
                directoryPath = (string)regkey.GetValue(FT_ROOT_PATH_KEY);
            else
            {
                //TRY 64 bit REG path
                regkey = Registry.LocalMachine.OpenSubKey(FT_REG_ROOT_64_BIT);
                if (regkey != null)
                    directoryPath = (string)regkey.GetValue(FT_ROOT_PATH_KEY);
                else
                    directoryPath = GetRootFromEnvironment();
            }

            return directoryPath;
        }

        // verify that files/folders exist (does not recurse into folders)
        public static List<TestData> ValidateFiles(IEnumerable<TestData> tests)
        {
            List<TestData> validTests = new List<TestData>();
            string testPath = string.Empty;

            foreach (TestData test in tests)
            {
                // If for FS tests params are specified, it should like this: <testPath> <paramList> separated by one or more spaces,
                // we should only validate the path, otherwise failure
                testPath = GetTestPathWithoutParams(test.Tests);

                if (!File.Exists(testPath) && !Directory.Exists(testPath))
                {
                    ConsoleWriter.WriteErrLine(string.Format("File/Folder not found: '{0}'", test.Tests));
                    Launcher.ExitCode = Launcher.ExitCodeEnum.Failed;
                }
                else
                {
                    validTests.Add(test);
                }
            }
            return validTests;
        }

        public static string GetTestPathWithoutParams(string test)
        {
            int quotationMarkIndex = test.IndexOf(" \"", StringComparison.Ordinal);
            return quotationMarkIndex == -1 ? test : test.Substring(0, quotationMarkIndex).Trim();
        }

        public static bool FileExists(string filePath)
        {
            bool isFileValid = true;
            if (!File.Exists(filePath))
            {
                ConsoleWriter.WriteLine(string.Format(">>>> File not found: '{0}'", filePath));
                isFileValid = false;
                Launcher.ExitCode = Launcher.ExitCodeEnum.Failed;
            }

            return isFileValid;
        }

        /// <summary>
        /// Checks if test parameters list is valid or not
        /// </summary>
        /// <param name="params"></param>
        /// <param name="paramNames"></param>
        /// <param name="paramValues"></param>
        /// <returns>true if parameters the list of parameters is valid, false otherwise</returns>
        public static bool ValidateInlineParams(string[] @params, out IList<string> paramNames, out IList<string> paramValues)
        {
            if (@params == null) throw new ArgumentNullException("Parameters are missing");
            paramNames = new List<string>();
            paramValues = new List<string>();

            if (@params.Any())
            {
                foreach (var parameterPair in @params)
                {
                    if (!string.IsNullOrWhiteSpace(parameterPair))
                    {
                        string[] pair = parameterPair.Split(':');

                        string paramName = pair[0].Trim();
                        if (!CheckParamFormat(paramName))
                        {
                            ConsoleWriter.WriteLine(string.Format(Resources.MissingQuotesInParamFormat, "parameter name"));
                            return false;
                        }

                        paramName = NormalizeParam(paramName);
                        if (string.IsNullOrWhiteSpace(paramName))
                        {
                            ConsoleWriter.WriteLine(Resources.MissingParameterName);
                            return false;
                        }
                        paramNames.Add(paramName);

                        string paramValue = pair[1].Trim();
                        if (!CheckParamFormat(paramValue))
                        {
                            ConsoleWriter.WriteLine(string.Format(Resources.MissingQuotesInParamFormat, "parameter value"));
                            return false;
                        }

                        paramValue = NormalizeParam(paramValue);
                        if (paramValue == null)
                        {
                            ConsoleWriter.WriteLine(Resources.MissingParameterValue);
                            return false;
                        }
                        paramValues.Add(paramValue);
                    }
                }
            }

            return true;
        }

        /// <summary>
        /// 
        /// </summary>
        /// <param name="param"></param>
        /// <returns></returns>
        private static bool CheckParamFormat(string param)
        {
            long _unused;
            if (!string.IsNullOrEmpty(param) && long.TryParse(param, out _unused))
            {
                return true;
            }
            else
            {
                // must be at least 2 characters wide, containing at least 2 double quotes
                if (param.Length < 2) return false;

                // first and at last characters have to be double quotes
                if (!param.StartsWith("\"") && !param.EndsWith("\"")) return false;

                return true;
            }
        }

        /// <summary>
        /// Normalizes test parameter, by removing the double quotes
        /// </summary>
        /// <param name="param"></param>
        /// <returns>true if parameter is valid, false otherwise</returns>
        private static string NormalizeParam(string param)
        {
            if (!string.IsNullOrWhiteSpace(param))
            {
                long n = long.MaxValue;
                bool isNumeric = !string.IsNullOrEmpty(param) && long.TryParse(param, out n);

                if (isNumeric)
                {
                    return n.ToString();
                }
                else
                {
                    if (param.Length >= 2)
                    {
                        return param.Substring(1, param.Length - 2);
                    }
                }
            }

            return null;
        }

        public static bool IsTestingToolsInstalled(TestStorageType type)
        {
            //we want to check if we have Service Test, QTP installed on a machine

            return IsQtpInstalled() || IsServiceTestInstalled() || isLoadRunnerInstalled();

        }

        public static bool isLoadRunnerInstalled()
        {
            //try 32 bit
            RegistryKey regkey = Registry.LocalMachine.OpenSubKey(LoadRunnerRegistryKey);

            if (regkey == null)
            {
                //try 64-bit
                regkey = Registry.LocalMachine.OpenSubKey(LoadRunner64RegisryKey);

            }

            if (regkey != null)
            {

                //LoadRunner Exist.
                //check if Controller is installed (not SA version)

                if (regkey.OpenSubKey(LoadRunnerControllerRegistryKey) != null)
                {
                    return true;
                }

            }
            return false;

        }

        public static bool IsQtpInstalled()
        {
            RegistryKey regkey;
            string value;
            regkey = Registry.LocalMachine.OpenSubKey(FT_REG_ROOT);
            if (regkey == null)
            {
                //try 64 bit
                regkey = Registry.LocalMachine.OpenSubKey(FT_REG_ROOT_64_BIT);
            }

            if (regkey != null)
            {
                value = (string)regkey.GetValue(FT_ROOT_PATH_KEY);
                if (!string.IsNullOrEmpty(value))
                {
                    return true;
                }
            }
            return false;
        }

        public static bool IsServiceTestInstalled()
        {
            RegistryKey regkey;
            string value;
            regkey = Registry.LocalMachine.OpenSubKey(ServiceTesCurrentVersionRegistryKey);
            if (regkey == null)
            {
                //try 64 bit
                regkey = Registry.LocalMachine.OpenSubKey(ServiceTesCurrentVersionWOW64RegistryKey);
            }

            if (regkey != null)
            {
                value = (string)regkey.GetValue(InstalltionFolderValue);
                if (!string.IsNullOrEmpty(value))
                {
                    return true;
                }
            }
            return false;
        }

        private static string GetRootFromEnvironment()
        {
            string qtpRoot = Environment.GetEnvironmentVariable(QTP_ROOT_ENV_VAR_NAME, EnvironmentVariableTarget.Process);

            if (string.IsNullOrEmpty(qtpRoot))
            {
                qtpRoot = Environment.GetEnvironmentVariable(QTP_ROOT_ENV_VAR_NAME, EnvironmentVariableTarget.User);

                if (string.IsNullOrEmpty(qtpRoot))
                {
                    qtpRoot = Environment.GetEnvironmentVariable(QTP_ROOT_ENV_VAR_NAME,
                        EnvironmentVariableTarget.Machine);

                    if (string.IsNullOrEmpty(qtpRoot))
                    {
                        qtpRoot = Environment.CurrentDirectory;
                    }
                }
            }

            return qtpRoot;
        }

        public static string GetSTInstallPath()
        {
            string ret = string.Empty;
            var regKey = Registry.LocalMachine.OpenSubKey(ServiceTesCurrentVersionRegistryKey);
            if (regKey != null)
            {
                var val = regKey.GetValue(InstalltionFolderValue);
                if (null != val)
                {
                    ret = val.ToString();
                }
            }
            else
            {
                regKey = Registry.LocalMachine.OpenSubKey(ServiceTesCurrentVersionWOW64RegistryKey);
                if (regKey != null)
                {
                    var val = regKey.GetValue(InstalltionFolderValue);
                    if (null != val)
                    {
                        ret = val.ToString();
                    }
                }
                else
                {
                    ret = GetRootDirectoryPath() ?? string.Empty;
                }
            }

            if (!string.IsNullOrEmpty(ret))
            {
                ret = ret.EndsWith("\\") ? ret : (ret + "\\");
                if (ret.EndsWith("\\bin\\"))
                {
                    int endIndex = ret.LastIndexOf("\\bin\\");
                    if (endIndex != -1)
                    {
                        ret = ret.Substring(0, endIndex) + "\\";
                    }
                }
            }

            return ret;
        }

        public static string getLRInstallPath()
        {
            string installPath = null;
            System.Collections.IDictionary envVariables = Environment.GetEnvironmentVariables();

            //try to find LoadRunner install path in environment vars
            foreach (string variable in LoadRunnerENVVariables)
            {
                if (envVariables.Contains(variable))
                    return envVariables[variable] as string;
            }

            //Fallback to registry
            //try 32 bit
            RegistryKey regkey = Registry.LocalMachine.OpenSubKey(LoadRunnerRegistryKey);

            if (regkey == null)
            {
                //try 64-bit
                regkey = Registry.LocalMachine.OpenSubKey(LoadRunner64RegisryKey);
            }

            if (regkey != null)
            {
                //LoadRunner Exists. check if Controller is installed (not SA version)
                regkey = regkey.OpenSubKey(LoadRunnerControllerDirRegistryKey);
                if (regkey != null)
                    return regkey.GetValue("Controller").ToString();
            }

            return installPath;
        }

        public static List<string> GetTestsLocations(string baseDir)
        {
            var testsLocations = new List<string>();
            if (string.IsNullOrEmpty(baseDir) || !Directory.Exists(baseDir))
            {
                return testsLocations;
            }

            WalkDirectoryTree(new DirectoryInfo(baseDir), ref testsLocations);
            return testsLocations;
        }

        public static TestType GetTestType(string path)
        {
            if ((File.GetAttributes(path) & FileAttributes.Directory) == FileAttributes.Directory)
            {
                //ST and QTP uses folder as test locations
                var stFiles = Directory.GetFiles(path,
                    @"*.st?",
                    SearchOption.TopDirectoryOnly);

                return (stFiles.Count() > 0) ? TestType.ST : TestType.QTP;
            }
            else //not directory
            {
                //loadrunner is a path to file...
                return TestType.LoadRunner;
            }
        }

        public static bool IsDirectory(string path)
        {
            var fa = File.GetAttributes(path);
            var isDirectory = false;
            if ((fa & FileAttributes.Directory) != 0)
            {
                isDirectory = true;
            }
            return isDirectory;
        }

        static void WalkDirectoryTree(DirectoryInfo root, ref List<string> results)
        {
            FileInfo[] files = null;
            DirectoryInfo[] subDirs;

            // First, process all the files directly under this folder
            try
            {

                files = root.GetFiles("*" + STFileExt);
                files = files.Union(root.GetFiles("*" + QTPFileExt)).ToArray();
                files = files.Union(root.GetFiles("*" + LoadRunnerFileExt)).ToArray();
            }
            catch (Exception)
            {
                // This code just writes out the message and continues to recurse.
                // You may decide to do something different here. For example, you
                // can try to elevate your privileges and access the file again.
                //log.Add(e.Message);
            }

            if (files != null)
            {
                foreach (FileInfo fi in files)
                {
                    if (fi.Extension == LoadRunnerFileExt)
                        results.Add(fi.FullName);
                    else
                        results.Add(fi.Directory.FullName);

                    // In this example, we only access the existing FileInfo object. If we
                    // want to open, delete or modify the file, then
                    // a try-catch block is required here to handle the case
                    // where the file has been deleted since the call to TraverseTree().
                }

                // Now find all the subdirectories under this directory.
                subDirs = root.GetDirectories();

                foreach (DirectoryInfo dirInfo in subDirs)
                {
                    // Recursive call for each subdirectory.
                    WalkDirectoryTree(dirInfo, ref results);
                }
            }
        }

        public static string GetTempDir()
        {
            string baseTemp = Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location);

            string dirName = Guid.NewGuid().ToString().Replace("-", string.Empty).Substring(0, 6);
            string tempDirPath = Path.Combine(baseTemp, dirName);

            return tempDirPath;
        }

        public static string CreateTempDir()
        {
            string tempDirPath = GetTempDir();

            Directory.CreateDirectory(tempDirPath);

            return tempDirPath;
        }

        public static bool IsNetworkPath(string path)
        {
            if (path.StartsWith(@"\\"))
                return true;
            var dir = new DirectoryInfo(path);
            var drive = new DriveInfo(dir.Root.ToString());
            return drive.DriveType == DriveType.Network;
        }

        public static bool IsLeanFTRunning()
        {
            bool bRet = false;
            Process[] procArray = Process.GetProcessesByName("LFTRuntime");
            // Hardcoded temporarily since LeanFT does not store the process name anywhere
            if (procArray.Length != 0)
            {
                bRet = true;
            }
            return bRet;
        }

        public static bool IsSprinterRunning()
        {
            RegistryKey key =
                Microsoft.Win32.Registry.LocalMachine.OpenSubKey("SOFTWARE\\Hewlett-Packard\\Manual Runner\\Process");
            if (key == null)
                return false;

            var arrayName = key.GetSubKeyNames();
            if (arrayName.Length == 0)
                return false;
            foreach (string s in arrayName)
            {
                Process[] sprinterProcArray = Process.GetProcessesByName(Path.GetFileNameWithoutExtension(s));
                if (sprinterProcArray.Length != 0)
                    return true;

            }
            return false;
        }

        public static bool CanUftProcessStart(out string reason)
        {
            //Close UFT when some of the Sprinter processes is running
            if (IsSprinterRunning())
            {
                reason = Resources.UFT_Sprinter_Running;
                return false;
            }

            //Close UFT when LeanFT engine is running
            if (IsLeanFTRunning())
            {
                reason = Resources.UFT_LeanFT_Running;
                return false;
            }
            reason = string.Empty;
            return true;
        }

        public static string GetParallelRunnerDirectory(string parallelRunnerExecutable)
        {
            if (parallelRunnerExecutable == null) return null;

            var uftFolder = GetSTInstallPath();

            if (uftFolder == null) return null;

            return uftFolder + @"bin\" + parallelRunnerExecutable;
        }

        /// <summary>
        /// Why we need this? If we run jenkins in a master slave node where there is a jenkins service installed in the slave machine, we need to change the DCOM settings as follow:
        /// dcomcnfg.exe -> My Computer -> DCOM Config -> QuickTest Professional Automation -> Identity -> and select The Interactive User
        /// </summary>
        public static void ChangeDCOMSettingToInteractiveUser()
        {
            if (IsInDocker())
            {
                ConsoleWriter.WriteLine(BYPASS_DCOM_SETTINGS_CHECK);
                return;
            }
            try
            {
                var regKey = GetQuickTestProfessionalAutomationRegKey(RegistryView.Registry32);

                if (regKey == null)
                {
                    regKey = GetQuickTestProfessionalAutomationRegKey(RegistryView.Registry64);
                }

                if (regKey == null)
                    throw new Exception(string.Format("Unable to find in registry key {0}", SOFTWARE_Classes_AppID_A67EB23A));

                object runAsKey = regKey.GetValue(RUN_AS);

                if (runAsKey == null || !runAsKey.ToString().Equals(INTERACTIVE_USER))
                {
                    regKey.SetValue(RUN_AS, INTERACTIVE_USER);
                }
            }
            catch (Exception ex)
            {
                throw new Exception(string.Format("{0}. Error: ", UNABLE_TO_CHANGE_DCOM_SETTINGS, ex.Message));
            }
        }

        private static bool IsInDocker()
        {
            int containerType = 0;
            try
            {
                RegistryKey regKey = RegistryKey.OpenBaseKey(RegistryHive.LocalMachine, RegistryView.Registry64);
                regKey = regKey.OpenSubKey(SYSTEM_CURRENTCONTROLSET_CONTROL, false);
                containerType = (regKey.GetValue(CONTAINER_TYPE) as int?).GetValueOrDefault();
            }
            catch (Exception ex)
            {
                ConsoleWriter.WriteErrLine(string.Format("IsInDocker() function error: {0}", ex.Message));
            }
            return containerType > 0;
        }

        public static RegistryKey GetQuickTestProfessionalAutomationRegKey(RegistryView registry32)
        {
            RegistryKey localKey = RegistryKey.OpenBaseKey(RegistryHive.LocalMachine, RegistryView.Registry64);
            localKey = localKey.OpenSubKey(SOFTWARE_Classes_AppID_A67EB23A, true);

            return localKey;
        }

        /// <summary>
        /// Return the path of the available results folder for the parallel runner.
        /// </summary>
        /// <param name="testInfo"> The test information. </param>
        /// <returns>
        /// the path to the results folder 
        /// </returns>
        public static string GetNextResFolder(string reportPath, string resultFolderName)
        {
            // since ParallelRunner will store the report as "Res1...ResN"
            // we need to know before parallel runner creates the result folder
            // what the folder name will be
            // so we know which result is ours(or if there was any result)
            int resultFolderIndex = 1;

            while (Directory.Exists(Path.Combine(reportPath, resultFolderName + resultFolderIndex)))
            {
                resultFolderIndex += 1;
            }

            return reportPath + "\\" + resultFolderName + resultFolderIndex;
        }

        #region Report Related

        /// <summary>
        /// Set the error for a test when the report path is invalid.
        /// </summary>
        /// <param name="runResults"> The test run results </param>
        /// <param name="errorReason"> The error reason </param>
        /// <param name="testInfo"> The test informatio </param>
        public static void SetTestReportPathError(TestRunResults runResults, ref string errorReason, TestInfo testInfo)
        {
            // Invalid path was provided, return useful description
            errorReason = string.Format(Resources.InvalidReportPath, runResults.ReportLocation);

            // since the report path is invalid, the test should fail
            runResults.TestState = TestState.Error;
            runResults.ErrorDesc = errorReason;

            // output the error for the current test run
            ConsoleWriter.WriteErrLine(runResults.ErrorDesc);

            // include the error in the summary
            ConsoleWriter.ErrorSummaryLines.Add(runResults.ErrorDesc);

            // provide the appropriate exit code for the launcher
            Environment.ExitCode = (int)Launcher.ExitCodeEnum.Failed;
        }

        /// <summary>
        /// Try to set the custom report path for a given test.
        /// </summary>
        /// <param name="runResults"> The test run results </param>
        /// <param name="testInfo"> The test information </param>
        /// <param name="errorReason"> The error reason </param>
        /// <returns> True if the report path was set, false otherwise </returns>
        public static bool TrySetTestReportPath(TestRunResults runResults, TestInfo testInfo, ref string errorReason)
        {
            string testName = testInfo.TestName.Substring(testInfo.TestName.LastIndexOf('\\') + 1) + "_";
            string reportLocation = Helper.GetNextResFolder(testInfo.ReportPath, testName);

            // set the report location for the run results
            runResults.ReportLocation = reportLocation;
            try
            {
                Directory.CreateDirectory(runResults.ReportLocation);
            }
            catch (Exception)
            {
                SetTestReportPathError(runResults, ref errorReason, testInfo);
                return false;
            }

            return true;
        }

        public static string GetUftViewerInstallPath()
        {
            string ret = string.Empty;
            var regKey = Registry.LocalMachine.OpenSubKey(UftViewerInstalltionFolderRegistryKey) ??
                         Registry.LocalMachine.OpenSubKey(UftViewerInstalltionFolderRegistryKey64Bit);

            if (regKey != null)
            {
                var val = regKey.GetValue("InstallLocation");
                if (null != val)
                {
                    ret = val.ToString();
                }
            }

            if (!string.IsNullOrEmpty(ret))
            {
                ret = ret.EndsWith("\\") ? ret : (ret + "\\");
            }

            return ret;
        }

        public static string GetLastRunFromReport(string reportPath)
        {
            if (!Directory.Exists(reportPath))
                return null;
            string resultsFileFullPath = reportPath + @"\" + ResultsFileName;
            if (!File.Exists(resultsFileFullPath))
                return null;
            try
            {
                var root = XElement.Load(resultsFileFullPath);

                var element = root.XPathSelectElement("//Doc/Summary");
                var lastStartRun = element.Attribute("sTime");
                return lastStartRun.Value.Split(new char[] { ' ' })[0];

            }
            catch (Exception)
            {
                return null;
            }
        }

        public static TestState GetTestStateFromUFTReport(TestRunResults runDesc, string[] resultFiles)
        {
            try
            {
                TestState finalState = TestState.Unknown;

                foreach (string resultsFileFullPath in resultFiles)
                {
                    finalState = TestState.Unknown;
                    string desc = string.Empty;
                    TestState state = GetStateFromUFTResultsFile(resultsFileFullPath, out desc);
                    if (finalState == TestState.Unknown || finalState == TestState.Passed)
                    {
                        finalState = state;
                        if (!string.IsNullOrWhiteSpace(desc))
                        {
                            if (finalState == TestState.Error)
                            {
                                runDesc.ErrorDesc = desc;
                            }
                            if (finalState == TestState.Failed)
                            {
                                runDesc.FailureDesc = desc;
                            }
                        }
                    }
                }

                if (finalState == TestState.Unknown)
                    finalState = TestState.Passed;

                if (finalState == TestState.Failed && string.IsNullOrWhiteSpace(runDesc.FailureDesc))
                    runDesc.FailureDesc = "Test failed";

                runDesc.TestState = finalState;
                return runDesc.TestState;
            }
            catch (Exception)
            {
                return TestState.Unknown;
            }

        }

        public static TestState GetTestStateFromLRReport(TestRunResults runDesc, string[] resultFiles)
        {

            foreach (string resultFileFullPath in resultFiles)
            {
                string desc = string.Empty;
                runDesc.TestState = GetTestStateFromLRReport(resultFileFullPath, out desc);
                if (runDesc.TestState == TestState.Failed)
                {
                    runDesc.ErrorDesc = desc;
                    break;
                }
            }

            return runDesc.TestState;
        }

        public static TestState GetTestStateFromReport(TestRunResults runDesc)
        {
            try
            {
                if (!Directory.Exists(runDesc.ReportLocation))
                {
                    runDesc.ErrorDesc = string.Format(Resources.DirectoryNotExistError, runDesc.ReportLocation);

                    runDesc.TestState = TestState.Error;
                    return runDesc.TestState;
                }
                //if there is Result.xml -> UFT
                //if there is sla.xml file -> LR
                //if there is parallelrun_results.xml -> ParallelRunner

                string[] resultFiles = Directory.GetFiles(runDesc.ReportLocation, "Results.xml",
                    SearchOption.TopDirectoryOnly);
                if (resultFiles.Length == 0)
                    resultFiles = Directory.GetFiles(runDesc.ReportLocation, "run_results.xml",
                        SearchOption.TopDirectoryOnly);
                //resultFiles = Directory.GetFiles(Path.Combine(runDesc.ReportLocation, "Report"), "Results.xml", SearchOption.TopDirectoryOnly);

                if (resultFiles != null && resultFiles.Length > 0)
                    return GetTestStateFromUFTReport(runDesc, resultFiles);

                resultFiles = Directory.GetFiles(runDesc.ReportLocation, "SLA.xml", SearchOption.AllDirectories);

                if (resultFiles != null && resultFiles.Length > 0)
                {
                    return GetTestStateFromLRReport(runDesc, resultFiles);
                }

                resultFiles = Directory.GetFiles(runDesc.ReportLocation, "parallelrun_results.html", SearchOption.TopDirectoryOnly);

                // the overall status is given by parallel runner
                // at the end of the run
                if (resultFiles != null && resultFiles.Length > 0)
                {
                    return runDesc.TestState;
                }

                //no LR or UFT => error
                runDesc.ErrorDesc = string.Format("no results file found for " + runDesc.TestName);
                runDesc.TestState = TestState.Error;
                return runDesc.TestState;
            }
            catch (Exception)
            {
                return TestState.Unknown;
            }

        }

        private static TestState GetTestStateFromLRReport(string resultFileFullPath, out string desc)
        {
            XmlDocument xdoc = new XmlDocument();
            xdoc.Load(resultFileFullPath);
            return CheckNodeStatus(xdoc.DocumentElement, out desc);
        }

        private static TestState CheckNodeStatus(XmlNode node, out string desc)
        {
            desc = string.Empty;
            if (node == null)
                return TestState.Failed;

            if (node.ChildNodes.Count == 1 && node.ChildNodes[0].NodeType == XmlNodeType.Text)
            {
                if (node.InnerText.ToLowerInvariant() == "failed")
                {
                    if (node.Attributes != null && node.Attributes["FullName"] != null)
                    {
                        desc = string.Format(Resources.LrSLARuleFailed, node.Attributes["FullName"].Value,
                            node.Attributes["GoalValue"].Value, node.Attributes["ActualValue"].Value);
                        ConsoleWriter.WriteLine(desc);
                    }
                    return TestState.Failed;
                }
                else
                {
                    return TestState.Passed;
                }
            }
            //node has children
            foreach (XmlNode childNode in node.ChildNodes)
            {
                TestState res = CheckNodeStatus(childNode, out desc);
                if (res == TestState.Failed)
                {
                    if (string.IsNullOrEmpty(desc) && node.Attributes != null && node.Attributes["FullName"] != null)
                    {
                        desc = string.Format(Resources.LrSLARuleFailed, node.Attributes["FullName"].Value,
                            node.Attributes["GoalValue"].Value, node.Attributes["ActualValue"].Value);
                        ConsoleWriter.WriteLine(desc);
                    }
                    return TestState.Failed;
                }
            }
            return TestState.Passed;
        }

        private static TestState GetStateFromUFTResultsFile(string resultsFileFullPath, out string desc)
        {
            TestState finalState = TestState.Unknown;
            desc = string.Empty;
            string status;
            var doc = new XmlDocument { PreserveWhitespace = true };
            doc.Load(resultsFileFullPath);
            string strFileName = Path.GetFileName(resultsFileFullPath);
            if (strFileName.Equals("run_results.xml"))
            {
                XmlNodeList rNodeList = doc.SelectNodes("/Results/ReportNode/Data");
                if (rNodeList == null)
                {
                    desc = string.Format(Resources.XmlNodeNotExistError, "/Results/ReportNode/Data");
                    finalState = TestState.Error;
                }

                var node = rNodeList.Item(0);
                XmlNode resultNode = ((XmlElement)node).GetElementsByTagName("Result").Item(0);

                status = resultNode.InnerText;
            }
            else
            {
                var testStatusPathNode = doc.SelectSingleNode("//Report/Doc/NodeArgs");
                if (testStatusPathNode == null)
                {
                    desc = string.Format(Resources.XmlNodeNotExistError, "//Report/Doc/NodeArgs");
                    finalState = TestState.Error;
                }

                if (!testStatusPathNode.Attributes["status"].Specified)
                    finalState = TestState.Unknown;

                status = testStatusPathNode.Attributes["status"].Value;
            }

            var result = (TestResult)Enum.Parse(typeof(TestResult), status);
            if (result == TestResult.Passed || result == TestResult.Done)
            {
                finalState = TestState.Passed;
            }
            else if (result == TestResult.Warning)
            {
                finalState = TestState.Warning;
            }
            else
            {
                finalState = TestState.Failed;
            }

            return finalState;
        }

        public static string GetUnknownStateReason(string reportPath)
        {
            if (!Directory.Exists(reportPath))
            {
                return string.Format("Directory '{0}' doesn't exist", reportPath);
            }
            string resultsFileFullPath = reportPath + @"\" + ResultsFileName;
            if (!File.Exists(resultsFileFullPath))
            {
                return string.Format("Could not find results file '{0}'", resultsFileFullPath);
            }

            var doc = new XmlDocument { PreserveWhitespace = true };
            doc.Load(resultsFileFullPath);
            var testStatusPathNode = doc.SelectSingleNode("//Report/Doc/NodeArgs");
            if (testStatusPathNode == null)
            {
                return string.Format("XML node '{0}' could not be found", "//Report/Doc/NodeArgs");
            }
            return string.Empty;
        }

        public static void OpenReport(string reportDirectory, ref string optionalReportViewerPath)
        {
            Process p = null;
            try
            {
                string viewerPath = optionalReportViewerPath;
                string reportPath = reportDirectory;
                string resultsFilePath = reportPath + "\\" + ResultsFileName;

                if (!File.Exists(resultsFilePath))
                {
                    return;
                }

                var si = new ProcessStartInfo();
                if (string.IsNullOrEmpty(viewerPath))
                {
                    viewerPath = GetUftViewerInstallPath();
                    optionalReportViewerPath = viewerPath;
                }
                si.Arguments = " -r \"" + reportPath + "\"";

                si.FileName = Path.Combine(viewerPath, QTPReportProcessPath);
                si.WorkingDirectory = Path.Combine(viewerPath, @"bin" + @"\");

                p = Process.Start(si);
                if (p != null)
                {
                }
                return;
            }
            catch (Exception)
            {
            }
            finally
            {
                if (p != null)
                {
                    p.Close();
                }
            }
            return;
        }

        #endregion

        #region Export Related

        /// <summary>
        /// Copy directories from source to target
        /// </summary>
        /// <param name="sourceDir">full path source directory</param>
        /// <param name="targetDir">full path target directory</param>
        /// <param name="includeSubDirectories">if true, all subdirectories and contents will be copied</param>
        /// <param name="includeRoot">if true, the source directory will be created too</param>
        public static void CopyDirectories(string sourceDir, string targetDir,
            bool includeSubDirectories = false, bool includeRoot = false)
        {
            var source = new DirectoryInfo(sourceDir);
            var target = new DirectoryInfo(targetDir);
            DirectoryInfo workingTarget = target;

            if (includeRoot)
                workingTarget = Directory.CreateDirectory(target.FullName);

            CopyContents(source, workingTarget, includeSubDirectories);
        }

        private static void CopyContents(DirectoryInfo source, DirectoryInfo target, bool includeSubDirectories)
        {
            if (!Directory.Exists(target.FullName))
            {
                Directory.CreateDirectory(target.FullName);
            }


            foreach (FileInfo fi in source.GetFiles())
            {
                string targetFile = Path.Combine(target.ToString(), fi.Name);

                fi.CopyTo(targetFile, true);
            }

            if (includeSubDirectories)
            {
                DirectoryInfo[] subDirectories = source.GetDirectories();
                foreach (DirectoryInfo diSourceSubDir in subDirectories)
                {
                    DirectoryInfo nextTargetSubDir =
                        target.CreateSubdirectory(diSourceSubDir.Name);
                    CopyContents(diSourceSubDir, nextTargetSubDir, true);
                }
            }
        }

        public static void CopyFilesFromFolder(string sourceFolder, IEnumerable<string> fileNames, string targetFolder)
        {
            foreach (var fileName in fileNames)
            {
                var sourceFullPath = Path.Combine(sourceFolder, fileName);
                var targetFullPath = Path.Combine(targetFolder, fileName);
                File.Copy(sourceFullPath, targetFullPath, true);
            }
        }

        /// <summary>
        /// Create html file according to a matching xml
        /// </summary>
        /// <param name="xmlPath">the values xml</param>
        /// <param name="xslPath">the xml transformation file</param>
        /// <param name="targetFile">the full file name - where to save the product</param>
        public static void CreateHtmlFromXslt(string xmlPath, string xslPath, string targetFile)
        {
            var xslTransform = new XslCompiledTransform();
            //xslTransform.Load(xslPath);
            xslTransform.Load(xslPath, new XsltSettings(false, true), null);

            var sb = new StringBuilder();
            var sw = new StringWriter(sb);
            var xmlWriter = new XhtmlTextWriter(sw);
            xslTransform.Transform(xmlPath, null, xmlWriter);


            File.WriteAllText(targetFile, sb.ToString());
        }

        #endregion

        public static void DeleteDirectory(string dirPath)
        {
            DirectoryInfo directory = Directory.CreateDirectory(dirPath);
            foreach (FileInfo file in directory.GetFiles()) file.Delete();
            foreach (DirectoryInfo subDirectory in directory.GetDirectories()) subDirectory.Delete(true);
            Directory.Delete(dirPath);
        }
    }

    public class Stopper
    {
        private readonly int _milliSeconds;

        public Stopper(int milliSeconds)
        {
            this._milliSeconds = milliSeconds;
        }

        /// <summary>
        /// Creates timer in seconds to replace thread.sleep due to ui freezes in jenkins. 
        /// Should be replaced in the future with ASync tasks
        /// </summary>
        public void Start()
        {
            if (_milliSeconds < 1)
            {
                return;
            }
            DateTime desired = DateTime.Now.AddMilliseconds(_milliSeconds);
            var a = 0;
            while (DateTime.Now < desired)
            {
                a += 1;
            }
        }
    }

}
