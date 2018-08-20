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
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading;
using HpToolsLauncher.Properties;
using Mercury.TD.Client.Ota.QC9;

//using Mercury.TD.Client.Ota.Api;

namespace HpToolsLauncher
{
    public class AlmTestSetsRunner : RunnerBase, IDisposable
    {
        QcRunMode m_runMode = QcRunMode.RUN_LOCAL;
        double m_timeout = -1;
        bool m_blnConnected = false;
        ITDConnection2 tdConnection = null;
        List<string> colTestSets = new List<string>();
        string m_runHost = null;
        string m_qcServer = null;
        string m_qcUser = null;
        string m_qcProject = null;
        string m_qcDomain = null;

        public bool Connected
        {
            get { return m_blnConnected; }
            set { m_blnConnected = value; }
        }

        public List<string> TestSets
        {
            get { return colTestSets; }
            set { colTestSets = value; }
        }

        public QcRunMode RunMode
        {
            get { return m_runMode; }
            set { m_runMode = value; }
        }

        public double Timeout
        {
            get { return m_timeout; }
            set { m_timeout = value; }
        }

        public string RunHost
        {
            get { return m_runHost; }
            set { m_runHost = value; }
        }

        public ITDConnection2 TdConnection
        {
            get
            {
                if (tdConnection == null)
                    CreateTdConnection();

                return tdConnection;
            }
            set { tdConnection = value; }
        }


        /// <summary>
        /// constructor
        /// </summary>
        /// <param name="qcServer"></param>
        /// <param name="qcUser"></param>
        /// <param name="qcPassword"></param>
        /// <param name="qcDomain"></param>
        /// <param name="qcProject"></param>
        /// <param name="intQcTimeout"></param>
        /// <param name="enmQcRunMode"></param>
        /// <param name="runHost"></param>
        /// <param name="qcTestSets"></param>
        public AlmTestSetsRunner(string qcServer,
                                string qcUser,
                                string qcPassword,
                                string qcDomain,
                                string qcProject,
                                double intQcTimeout,
                                QcRunMode enmQcRunMode,
                                string runHost,
                                List<string> qcTestSets)
        {
            Timeout = intQcTimeout;
            RunMode = enmQcRunMode;
            RunHost = runHost;

            m_qcServer = qcServer;
            m_qcUser = qcUser;
            m_qcProject = qcProject;
            m_qcDomain = qcDomain;

            Connected = ConnectToProject(qcServer, qcUser, qcPassword, qcDomain, qcProject);
            TestSets = qcTestSets;
            if (!Connected)
            {
                Environment.Exit((int)Launcher.ExitCodeEnum.Failed);
            }
        }

        /// <summary>
        /// destructor - ensures dispose of connection
        /// </summary>
        ~AlmTestSetsRunner()
        {
            Dispose(false);
        }


        /// <summary>
        /// runs the tests given to the object.
        /// </summary>
        /// <returns></returns>
        public override TestSuiteRunResults Run()
        {
            if (!Connected)
                return null;
            TestSuiteRunResults activeRunDesc = new TestSuiteRunResults();
            //find all the testSets under if given some folders in our list
            try
            {
                FindAllTestSetsUnderFolders();
            }
            catch (Exception ex)
            {

                ConsoleWriter.WriteErrLine(string.Format(Resources.AlmRunnerErrorBadQcInstallation, ex.Message, ex.StackTrace));
                return null;
            }

            //run all the TestSets
            foreach (string testset in TestSets)
            {
                string testset1 = testset.TrimEnd("\\".ToCharArray());

                int pos = testset1.LastIndexOf('\\');
                string tsDir = "";
                string tsName = testset1;
                if (pos != -1)
                {
                    tsDir = testset1.Substring(0, pos).Trim("\\".ToCharArray());
                    tsName = testset1.Substring(pos, testset1.Length - pos).Trim("\\".ToCharArray());
                }

                TestSuiteRunResults desc = RunTestSet(tsDir, tsName, Timeout, RunMode, RunHost);
                if (desc != null)
                    activeRunDesc.AppendResults(desc);
            }

            return activeRunDesc;
        }

        /// <summary>
        /// creats a connection to Qc
        /// </summary>
        private void CreateTdConnection()
        {

            Type type = Type.GetTypeFromProgID("TDApiOle80.TDConnection");

            if (type == null)
            {
                ConsoleWriter.WriteLine(GetAlmNotInstalledError());
                Environment.Exit((int)Launcher.ExitCodeEnum.Failed);
            }

            try
            {
                object conn = Activator.CreateInstance(type);
                this.tdConnection = conn as ITDConnection2;

            }
            catch (FileNotFoundException ex)
            {
                ConsoleWriter.WriteLine(GetAlmNotInstalledError());
                Environment.Exit((int)Launcher.ExitCodeEnum.Failed);
            }
        }

        /// <summary>
        /// finds all folders in the TestSet list, scans their tree and adds all sets under the given folders
        /// updates the TestSets by expanding the folders, and removing them, so only Test sets remain in the collection
        /// </summary>
        private void FindAllTestSetsUnderFolders()
        {
            List<string> extraSetsList = new List<string>();
            List<string> removeSetsList = new List<string>();
            var tsTreeManager = (ITestSetTreeManager)tdConnection.TestSetTreeManager;

            //go over all the testsets / testSetFolders and check which is which
            foreach (string testsetOrFolder in TestSets)
            {
                //try getting the folder
                ITestSetFolder tsFolder = GetFolder("Root\\" + testsetOrFolder.TrimEnd("\\".ToCharArray()));

                //if it exists it's a folder and should be traversed to find all sets
                if (tsFolder != null)
                {
                    removeSetsList.Add(testsetOrFolder);

                    List<string> setList = GetAllTestSetsFromDirTree(tsFolder);
                    extraSetsList.AddRange(setList);
                }

            }

            TestSets.RemoveAll((a) => removeSetsList.Contains(a));
            TestSets.AddRange(extraSetsList);
        }

        /// <summary>
        /// recursively find all testsets in the qc directory tree, starting from a given folder
        /// </summary>
        /// <param name="tsFolder"></param>
        /// <param name="tsTreeManager"></param>
        /// <returns></returns>
        private List<string> GetAllTestSetsFromDirTree(ITestSetFolder tsFolder)
        {
            List<string> retVal = new List<string>();
            List children = tsFolder.FindChildren("");
            List testSets = tsFolder.FindTestSets("");

            if (testSets != null)
            {
                foreach (ITestSet childSet in testSets)
                {
                    string tsPath = childSet.TestSetFolder.Path;
                    tsPath = tsPath.Substring(5).Trim("\\".ToCharArray());
                    string tsFullPath = tsPath + "\\" + childSet.Name;
                    retVal.Add(tsFullPath.TrimEnd());
                }
            }

            if (children != null)
            {
                foreach (ITestSetFolder childFolder in children)
                {
                    GetAllTestSetsFromDirTree(childFolder);
                }
            }
            return retVal;
        }


        /// <summary>
        /// get a QC folder
        /// </summary>
        /// <param name="testset"></param>
        /// <returns>the folder object</returns>
        private ITestSetFolder GetFolder(string testset)
        {
            var tsTreeManager = (ITestSetTreeManager)tdConnection.TestSetTreeManager;
            ITestSetFolder tsFolder = null;
            try
            {
                tsFolder = (ITestSetFolder)tsTreeManager.get_NodeByPath(testset);
            }
            catch (Exception ex)
            {
                return null;
            }
            return tsFolder;
        }

        /// <summary>
        /// gets test index given it's name
        /// </summary>
        /// <param name="strName"></param>
        /// <param name="results"></param>
        /// <returns></returns>
        public int GetIdxByTestName(string strName, TestSuiteRunResults results)
        {
            TestRunResults res = null;
            int retVal = -1;

            for (int i = 0; i < results.TestRuns.Count(); ++i)
            {
                res = results.TestRuns[i];

                if (res != null && res.TestName == strName)
                {
                    retVal = i;
                    break;
                }
            }
            return retVal;
        }

        /// <summary>
        /// returns a description of the failure
        /// </summary>
        /// <param name="p_Test"></param>
        /// <returns></returns>
        private string GenerateFailedLog(IRun p_Test)
        {
            try
            {
                StepFactory sf = p_Test.StepFactory as StepFactory;
                if (sf == null)
                    return "";
                IList stepList = sf.NewList("") as IList;
                if (stepList == null)
                    return "";

                //var stList = p_Test.StepFactory.NewList("");
                //string l_szReturn = "";
                string l_szFailedMessage = "";

                //' loop on each step in the steps
                foreach (IStep s in stepList)
                {
                    if (s.Status == "Failed")
                        l_szFailedMessage += s["ST_DESCRIPTION"] + "'\n\r";
                }
                return l_szFailedMessage;
            }
            catch
            {
                return "";
            }
        }


        /// <summary>
        /// writes a summary of the test run after it's over
        /// </summary>
        /// <param name="prevTest"></param>
        private string GetTestInstancesString(ITestSet set)
        {
            string retVal = "";
            try
            {
                TSTestFactory factory = set.TSTestFactory;
                List list = factory.NewList("");

                if (list == null)
                    return "";

                foreach (ITSTest testInstance in list)
                {
                    retVal += testInstance.ID + ",";
                }
                retVal.TrimEnd(", \n".ToCharArray());
            }
            catch (Exception ex)
            { }
            return retVal;
        }

        /// <summary>
        /// runs a test set with given parameters (and a valid connection to the QC server)
        /// </summary>
        /// <param name="tsFolderName">testSet folder name</param>
        /// <param name="tsName">testSet name</param>
        /// <param name="timeout">-1 for unlimited, or number of miliseconds</param>
        /// <param name="runMode">run on LocalMachine or remote</param>
        /// <param name="runHost">if run on remote machine - remote machine name</param>
        /// <returns></returns>
        public TestSuiteRunResults RunTestSet(string tsFolderName, string tsName, double timeout, QcRunMode runMode, string runHost)
        {
            string currentTestSetInstances = "";
            TestSuiteRunResults runDesc = new TestSuiteRunResults();
            TestRunResults activeTestDesc = null;

            var tsFactory = tdConnection.TestSetFactory;
            var tsTreeManager = (ITestSetTreeManager)tdConnection.TestSetTreeManager;
            List tsList = null;
            string tsPath = "Root\\" + tsFolderName;
            ITestSetFolder tsFolder = null;
            bool isTestPath = false;
            string testName = "";
            string testSuiteName = tsName;

            try
            {
                tsFolder = (ITestSetFolder)tsTreeManager.get_NodeByPath(tsPath);
                isTestPath = false;
            }
            catch (COMException ex)
            {
                //not found
                tsFolder = null;
            }

			// test set not found, try to find specific test by path
            if(tsFolder == null)
            {
                // if test set path was not found, the path may points to specific test
                // remove the test name and try find test set with parent path
                try
                {
                    int pos = tsPath.LastIndexOf("\\") + 1;
                    testName = testSuiteName;
                    testSuiteName = tsPath.Substring(pos, tsPath.Length - pos);
                    tsPath = tsPath.Substring(0, pos - 1);

                    tsFolder = (ITestSetFolder)tsTreeManager.get_NodeByPath(tsPath);
                    isTestPath = true;
                }
                catch (COMException ex)
                {
                    tsFolder = null;
                }
            }

            if (tsFolder == null)
            {
                //node wasn't found, folder = null
                ConsoleWriter.WriteErrLine(string.Format(Resources.AlmRunnerNoSuchFolder, tsFolder));

                //this will make sure run will fail at the end. (since there was an error)
                Launcher.ExitCode = Launcher.ExitCodeEnum.Failed;
                return null;
            }
            else
            {
                tsList = tsFolder.FindTestSets(testSuiteName);
            }
            if (tsList == null)
            {
                ConsoleWriter.WriteLine(string.Format(Resources.AlmRunnerCantFindTest, testSuiteName));

                //this will make sure run will fail at the end. (since there was an error)
                Launcher.ExitCode = Launcher.ExitCodeEnum.Failed;
                return null;
            }
            ITestSet targetTestSet = null;
            foreach (ITestSet ts in tsList)
            {
                string tempName = ts.Name;
                if (tempName.Equals(testSuiteName, StringComparison.InvariantCultureIgnoreCase))
                {
                    targetTestSet = ts;
                    break;
                }
            }

            if (targetTestSet == null)
            {
                ConsoleWriter.WriteLine(string.Format(Resources.AlmRunnerCantFindTest, testSuiteName));

                //this will make sure run will fail at the end. (since there was an error)
                Launcher.ExitCode = Launcher.ExitCodeEnum.Failed;
                return null;
            }


            ConsoleWriter.WriteLine(Resources.GeneralDoubleSeperator);
            ConsoleWriter.WriteLine(Resources.AlmRunnerStartingExecution);
            ConsoleWriter.WriteLine(string.Format(Resources.AlmRunnerDisplayTest, testSuiteName, targetTestSet.ID));

            ITSScheduler Scheduler = null;
            try
            {
                //need to run this to install everything needed http://AlmServer:8080/qcbin/start_a.jsp?common=true
                //start the scheduler
                Scheduler = targetTestSet.StartExecution("");


            }
            catch (Exception ex)
            {
                Scheduler = null;
            }
            try
            {

                currentTestSetInstances = GetTestInstancesString(targetTestSet);
            }
            catch (Exception ex)
            {
            }

            if (Scheduler == null)
            {
                Console.WriteLine(GetAlmNotInstalledError());

                //proceeding with program execution is tasteless, since nothing will run without a properly installed QC.
                Environment.Exit((int)Launcher.ExitCodeEnum.Failed);
            }

            TSTestFactory tsTestFactory = targetTestSet.TSTestFactory;
            ITDFilter2 tdFilter = tsTestFactory.Filter;
            tdFilter["TC_CYCLE_ID"] = targetTestSet.ID.ToString();

            IList tList = tsTestFactory.NewList(tdFilter.Text);

            if (isTestPath)
            {
                // index starts from 1 !!!
                int tListCount = 0;
                tListCount = tList.Count;

				// must loop from end to begin
                for (int index = tListCount; index > 0; index--)
                {
                    string tListIndexName = tList[index].Name;
                    string tListIndexTestName = tList[index].TestName;
                    if (!string.IsNullOrEmpty(tListIndexName) && !string.IsNullOrEmpty(testName) && !testName.Equals(tListIndexName))
                    {
                        tList.Remove(index);
                    }
                }
            }

            try
            {
                //set up for the run depending on where the test instances are to execute
                switch (runMode)
                {
                    case QcRunMode.RUN_LOCAL:
                        // run all tests on the local machine
                        Scheduler.RunAllLocally = true;
                        break;
                    case QcRunMode.RUN_REMOTE:
                        // run tests on a specified remote machine
                        Scheduler.TdHostName = runHost;
                        break;
                    // RunAllLocally must not be set for remote invocation of tests. As such, do not do this: Scheduler.RunAllLocally = False
                    case QcRunMode.RUN_PLANNED_HOST:
                        // run on the hosts as planned in the test set
                        Scheduler.RunAllLocally = false;
                        break;
                }
            }
            catch (Exception ex)
            {
                ConsoleWriter.WriteLine(string.Format(Resources.AlmRunnerProblemWithHost, ex.Message));
            }

            ConsoleWriter.WriteLine(Resources.AlmRunnerNumTests + tList.Count);

            int i = 1;
			
            foreach (ITSTest3 test in tList)
            {
                string runOnHost = runHost;
                if (runMode == QcRunMode.RUN_PLANNED_HOST)
                    runOnHost = test.HostName;

                //if host isn't taken from QC (PLANNED) and not from the test definition (REMOTE), take it from LOCAL (machineName)
                string hostName = runOnHost;
                if (runMode == QcRunMode.RUN_LOCAL)
                {
                    hostName = Environment.MachineName;
                }
                ConsoleWriter.WriteLine(string.Format(Resources.AlmRunnerDisplayTestRunOnHost, i, test.Name, hostName));

                Scheduler.RunOnHost[test.ID] = runOnHost;

                var testResults = new TestRunResults();
                testResults.TestName = test.Name;
                runDesc.TestRuns.Add(testResults);

                i = i + 1;
            }

            if (tList.Count == 0)
            {
                ConsoleWriter.WriteErrLine("Specified test not found on ALM, please check your test path.");
                //this will make sure run will fail at the end. (since there was an error)
                Launcher.ExitCode = Launcher.ExitCodeEnum.Failed;
                return null;
            }

            Stopwatch sw = Stopwatch.StartNew();
            
            try
            {
                //tests are actually run
                Scheduler.Run(tList);
            }
            catch (Exception ex)
            {
                ConsoleWriter.WriteLine(Resources.AlmRunnerRunError + ex.Message);
            }

            ConsoleWriter.WriteLine(Resources.AlmRunnerSchedStarted + DateTime.Now.ToString(Launcher.DateFormat));
            ConsoleWriter.WriteLine(Resources.SingleSeperator);
            IExecutionStatus executionStatus = Scheduler.ExecutionStatus;
            bool tsExecutionFinished = false;
            ITSTest prevTest = null;
            ITSTest currentTest = null;
            string abortFilename = System.IO.Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location) + "\\stop" + Launcher.UniqueTimeStamp + ".txt";
            //wait for the tests to end ("normally" or because of the timeout)
            while ((tsExecutionFinished == false) && (timeout == -1 || sw.Elapsed.TotalSeconds < timeout))
            {
                executionStatus.RefreshExecStatusInfo("all", true);
                tsExecutionFinished = executionStatus.Finished;

                if (System.IO.File.Exists(abortFilename))
                {
                    break;
                }

                for (int j = 1; j <= executionStatus.Count; ++j)
                {
                    TestExecStatus testExecStatusObj = executionStatus[j];

                    currentTest = targetTestSet.TSTestFactory[testExecStatusObj.TSTestId];
                    if (currentTest == null)
                    {
                        ConsoleWriter.WriteLine(string.Format("currentTest is null for test.{0} during execution", j));
                        continue;
                    }

                    activeTestDesc = UpdateTestStatus(runDesc, targetTestSet, testExecStatusObj, true);

                    if (activeTestDesc.PrevTestState != activeTestDesc.TestState)
                    {
                        TestState tstate = activeTestDesc.TestState;
                        if (tstate == TestState.Running)
                        {
                            //currentTest = targetTestSet.TSTestFactory[testExecStatusObj.TSTestId];
                            int testIndex = GetIdxByTestName(currentTest.Name, runDesc);

                            int prevRunId = GetTestRunId(currentTest);
                            runDesc.TestRuns[testIndex].PrevRunId = prevRunId;

                            //closing previous test
                            if (prevTest != null)
                            {
                                WriteTestRunSummary(prevTest);
                            }

                            //starting new test
                            prevTest = currentTest;

                            //assign the new test the consol writer so it will gather the output

                            ConsoleWriter.ActiveTestRun = runDesc.TestRuns[testIndex];

                            ConsoleWriter.WriteLine(DateTime.Now.ToString(Launcher.DateFormat) + " Running: " + currentTest.Name);

                            //tell user that the test is running
                            ConsoleWriter.WriteLine(DateTime.Now.ToString(Launcher.DateFormat) + " Running test: " + activeTestDesc.TestName + ", Test id: " + testExecStatusObj.TestId + ", Test instance id: " + testExecStatusObj.TSTestId);

                            //start timing the new test run
                            string foldername = "";
                            ITestSetFolder folder = targetTestSet.TestSetFolder as ITestSetFolder;

                            if (folder != null)
                                foldername = folder.Name.Replace(".", "_");

                            //the test group is it's test set. (dots are problematic since jenkins parses them as seperators between packadge and class)
                            activeTestDesc.TestGroup = foldername + "\\" + targetTestSet.Name;
                            activeTestDesc.TestGroup = activeTestDesc.TestGroup.Replace(".", "_");
                        }

                        TestState enmState = GetTsStateFromQcState(testExecStatusObj.Status as string);
                        string statusString = enmState.ToString();

                        if (enmState == TestState.Running)
                        {
                            ConsoleWriter.WriteLine(string.Format(Resources.AlmRunnerStat, activeTestDesc.TestName, testExecStatusObj.TSTestId, statusString));
                        }
                        else if (enmState != TestState.Waiting)
                        {
                            ConsoleWriter.WriteLine(string.Format(Resources.AlmRunnerStatWithMessage, activeTestDesc.TestName, testExecStatusObj.TSTestId, statusString, testExecStatusObj.Message));
                        }
                        if (System.IO.File.Exists(abortFilename))
                        {
                            break;
                        }
                    }
                }

                //wait 0.2 seconds
                Thread.Sleep(200);

                //check for abortion
                if (System.IO.File.Exists(abortFilename))
                {
                    _blnRunCancelled = true;

                    ConsoleWriter.WriteLine(Resources.GeneralStopAborted);

                    //stop all test instances in this testSet.
                    Scheduler.Stop(currentTestSetInstances);

                    ConsoleWriter.WriteLine(Resources.GeneralAbortedByUser);

                    //stop working 
                    Environment.Exit((int)Launcher.ExitCodeEnum.Aborted);
                }
            }

            //check status for each test
            if (timeout == -1 || sw.Elapsed.TotalSeconds < timeout)
            {
                //close last test
                if (prevTest != null)
                {
                    WriteTestRunSummary(prevTest);
                }

                //done with all tests, stop collecting output in the testRun object.
                ConsoleWriter.ActiveTestRun = null;
                for (int k = 1; k <= executionStatus.Count; ++k)
                {
                    if (System.IO.File.Exists(abortFilename))
                    {
                        break;
                    }

                    TestExecStatus testExecStatusObj = executionStatus[k];
                    currentTest = targetTestSet.TSTestFactory[testExecStatusObj.TSTestId];
                    if (currentTest == null)
                    {
                        ConsoleWriter.WriteLine(string.Format("currentTest is null for test.{0} after whole execution", k));
                        continue;
                    }

                    activeTestDesc = UpdateTestStatus(runDesc, targetTestSet, testExecStatusObj, false);

                    UpdateCounters(activeTestDesc, runDesc);

                    //currentTest = targetTestSet.TSTestFactory[testExecStatusObj.TSTestId];

                    string testPath = "Root\\" + tsFolderName + "\\" + testSuiteName + "\\" + activeTestDesc.TestName;

                    activeTestDesc.TestPath = testPath;
                }

                //update the total runtime
                runDesc.TotalRunTime = sw.Elapsed;

                ConsoleWriter.WriteLine(string.Format(Resources.AlmRunnerTestsetDone, testSuiteName, DateTime.Now.ToString(Launcher.DateFormat)));
            }
            else
            {
                _blnRunCancelled = true;
                ConsoleWriter.WriteLine(Resources.GeneralTimedOut);
                Launcher.ExitCode = Launcher.ExitCodeEnum.Aborted;
            }

            return runDesc;
        }

        /// <summary>
        /// writes a summary of the test run after it's over
        /// </summary>
        /// <param name="prevTest"></param>
        private void WriteTestRunSummary(ITSTest prevTest)
        {
            int prevRunId = ConsoleWriter.ActiveTestRun.PrevRunId;

            int runid = GetTestRunId(prevTest);
            if (runid > prevRunId)
            {
                string stepsString = GetTestStepsDescFromQc(prevTest);

                if (string.IsNullOrWhiteSpace(stepsString) && ConsoleWriter.ActiveTestRun.TestState != TestState.Error)
                    stepsString = GetTestRunLog(prevTest);

                if (!string.IsNullOrWhiteSpace(stepsString))
                    ConsoleWriter.WriteLine(stepsString);

                string linkStr = GetTestRunLink(prevTest, runid);

                ConsoleWriter.WriteLine("\n" + string.Format(Resources.AlmRunnerDisplayLink, linkStr));
            }
            ConsoleWriter.WriteLine(DateTime.Now.ToString(Launcher.DateFormat) + " " + Resources.AlmRunnerTestCompleteCaption + " " + prevTest.Name +
                ((runid > prevRunId) ? ", " + Resources.AlmRunnerRunIdCaption + " " + runid : "")
                + "\n-------------------------------------------------------------------------------------------------------");
        }

        /// <summary>
        /// gets a link string for the test run in Qc
        /// </summary>
        /// <param name="prevTest"></param>
        /// <param name="runid"></param>
        /// <returns></returns>
        private string GetTestRunLink(ITSTest prevTest, int runid)
        {
            bool oldQc = CheckIsOldQc();
            bool useSSL = (m_qcServer.Contains("https://"));

            ITestSet set = prevTest.TestSet;
            string testRunLink = useSSL ? ("tds://" + m_qcProject + "." + m_qcDomain + "." + m_qcServer.Replace("https://", "") + "/TestLabModule-000000003649890581?EntityType=IRun&EntityID=" + runid) 
                : ("td://" + m_qcProject + "." + m_qcDomain + "." + m_qcServer.Replace("http://", "") + "/TestLabModule-000000003649890581?EntityType=IRun&EntityID=" + runid);
            string testRunLinkQc10 = useSSL ? ("tds://" + m_qcProject + "." + m_qcDomain + "." + m_qcServer.Replace("https://", "") + "/Test%20Lab?Action=FindRun&TestSetID=" + set.ID + "&TestInstanceID=" + prevTest.ID + "&RunID=" + runid) 
                : ("td://" + m_qcProject + "." + m_qcDomain + "." + m_qcServer.Replace("http://", "") + "/Test%20Lab?Action=FindRun&TestSetID=" + set.ID + "&TestInstanceID=" + prevTest.ID + "&RunID=" + runid);
            string linkStr = (oldQc ? testRunLinkQc10 : testRunLink);
            return linkStr;
        }

        private string GetAlmNotInstalledError()
        {
            return "Could not create scheduler, please verify ALM client installation on run machine by downloading and in installing the add-in form: " + GetQcCommonInstallationURl(m_qcServer);
        }

        /// <summary>
        /// summerizes test steps after test has run
        /// </summary>
        /// <param name="test"></param>
        /// <returns>a string containing descriptions of step states and messags</returns>
        string GetTestStepsDescFromQc(ITSTest test)
        {
            StringBuilder sb = new StringBuilder();
            try
            {
                //get runs for the test
                RunFactory rfactory = test.RunFactory;
                List runs = rfactory.NewList("");
                if (runs.Count == 0)
                    return "";

                //get steps from run
                StepFactory stepFact = runs[runs.Count].StepFactory;
                List steps = stepFact.NewList("");
                if (steps.Count == 0)
                    return "";

                //go over steps and format a string
                foreach (IStep step in steps)
                {
                    sb.Append("Step: " + step.Name);

                    if (!string.IsNullOrWhiteSpace(step.Status))
                        sb.Append(", Status: " + step.Status);

                    string desc = step["ST_DESCRIPTION"] as string;
                    if (!string.IsNullOrEmpty(desc))
                    {
                        desc = "\n\t" + desc.Trim().Replace("\n", "\t").Replace("\r", "");
                        if (!string.IsNullOrWhiteSpace(desc))
                            sb.AppendLine(desc);
                    }
                }
            }
            catch (Exception ex)
            {
                sb.AppendLine("Exception while reading step data: " + ex.Message);
            }
            return sb.ToString().TrimEnd();
        }

        private void UpdateCounters(TestRunResults test, TestSuiteRunResults testSuite)
        {
            if (test.TestState != TestState.Running &&
                test.TestState != TestState.Waiting &&
                test.TestState != TestState.Unknown)
                ++testSuite.NumTests;

            if (test.TestState == TestState.Failed)
                ++testSuite.NumFailures;

            if (test.TestState == TestState.Error)
                ++testSuite.NumErrors;
        }

        /// <summary>
        /// translate the qc states into a state enum
        /// </summary>
        /// <param name="qcTestStatus"></param>
        /// <returns></returns>
        private TestState GetTsStateFromQcState(string qcTestStatus)
        {
            if (qcTestStatus == null)
                return TestState.Unknown;
            switch (qcTestStatus)
            {
                case "Waiting":
                    return TestState.Waiting;
                case "Error":
                    return TestState.Error;
                case "No Run":
                    return TestState.NoRun;
                case "Running":
                case "Connecting":
                    return TestState.Running;
                case "Success":
                case "Finished":
                case "FinishedPassed":
                    return TestState.Passed;
                case "FinishedFailed":
                    return TestState.Failed;
            }
            return TestState.Unknown;
        }

        /// <summary>
        /// updates the test status in our list of tests
        /// </summary>
        /// <param name="targetTestSet"></param>
        /// <param name="testExecStatusObj"></param>
        private TestRunResults UpdateTestStatus(TestSuiteRunResults runResults, ITestSet targetTestSet, TestExecStatus testExecStatusObj, bool onlyUpdateState)
        {
            TestRunResults qTest = null;
            ITSTest currentTest = null;
            try
            {
                //find the test for the given status object
                currentTest = targetTestSet.TSTestFactory[testExecStatusObj.TSTestId];

                if (currentTest == null)
                {
                    return qTest;
                }

                //find the test in our list
                int testIndex = GetIdxByTestName(currentTest.Name, runResults);
                qTest = runResults.TestRuns[testIndex];

                if (qTest.TestType == null)
                {
                    qTest.TestType = GetTestType(currentTest);
                }

                //update the state
                qTest.PrevTestState = qTest.TestState;
                qTest.TestState = GetTsStateFromQcState(testExecStatusObj.Status);

                if (!onlyUpdateState)
                {
                    try
                    {
                        //duration and status are updated according to the run
                        qTest.Runtime = TimeSpan.FromSeconds(currentTest.LastRun.Field("RN_DURATION"));
                    }
                    catch
                    {
                        //a problem getting duration, maybe the test isn't done yet - don't stop the flow..
                    }

                    switch (qTest.TestState)
                    {
                        case TestState.Failed:
                            qTest.FailureDesc = GenerateFailedLog(currentTest.LastRun);

                            if (string.IsNullOrWhiteSpace(qTest.FailureDesc))
                                qTest.FailureDesc = testExecStatusObj.Status + " : " + testExecStatusObj.Message;
                            break;
                        case TestState.Error:
                            qTest.ErrorDesc = testExecStatusObj.Status + " : " + testExecStatusObj.Message;
                            break;
                        default:
                            break;
                    }

                    //check qc version for link type
                    bool oldQc = CheckIsOldQc();

                    //string testLink = "<a href=\"testdirector:mydtqc01.isr.hp.com:8080/qcbin," + m_qcProject + "," + m_qcDomain + "," + targetTestSet.Name+ ";test-instance:" + testExecStatusObj.TestInstance + "\"> Alm link</a>";
                    string serverURl = m_qcServer.TrimEnd("/".ToCharArray());
                    if (serverURl.ToLower().StartsWith("http://"))
                        serverURl = serverURl.Substring(7);

                    //string testLinkInLabQc10 = "td://" + m_qcProject + "." + m_qcDomain + "." + m_qcServer.Replace("http://", "") + "/Test%20Lab?Action=FindTestInstance&TestSetID=" + targetTestSet.ID + "&TestInstanceID=" + testExecStatusObj.TSTestId;
                    //string testLinkInLab = "td://" + m_qcProject + "." + m_qcDomain + "." + m_qcServer.Replace("http://", "") + "/TestLabModule-000000003649890581?EntityType=ITestInstance&EntityID=" + testExecStatusObj.TSTestId;

                    int runid = GetTestRunId(currentTest);
                    string linkStr = GetTestRunLink(currentTest, runid);

                    string statusString = GetTsStateFromQcState(testExecStatusObj.Status as string).ToString();
                    ConsoleWriter.WriteLine(string.Format(Resources.AlmRunnerTestStat, currentTest.Name, statusString, testExecStatusObj.Message, linkStr));
                    runResults.TestRuns[testIndex] = qTest;
                }
            }
            catch (Exception ex)
            {
                ConsoleWriter.WriteLine(string.Format(Resources.AlmRunnerErrorGettingStat, currentTest.Name, ex.Message));
            }

            return qTest;
        }

        /// <summary>
        /// gets the runId for the given test
        /// </summary>
        /// <param name="currentTest">a test instance</param>
        /// <returns>the run id</returns>
        private static int GetTestRunId(ITSTest currentTest)
        {
            int runid = -1;
            IRun lastrun = currentTest.LastRun as IRun;
            if (lastrun != null)
                runid = lastrun.ID;
            return runid;
        }

        /// <summary>
        /// retrieves the run logs for the test when the steps are not reported to Qc (like in ST)
        /// </summary>
        /// <param name="currentTest"></param>
        /// <returns>the test run log</returns>
        private string GetTestRunLog(ITSTest currentTest)
        {
            string TestLog = "log\\vtd_user.log";

            IRun lastrun = currentTest.LastRun as IRun;
            string retVal = "";
            if (lastrun != null)
            {
                try
                {
                    IExtendedStorage storage = lastrun.ExtendedStorage as IExtendedStorage;

                    List list;
                    bool wasFatalError;
                    var path = storage.LoadEx(TestLog, true, out list, out wasFatalError);
                    string logPath = Path.Combine(path, TestLog);

                    if (File.Exists(logPath))
                    {
                        retVal = File.ReadAllText(logPath).TrimEnd();
                    }
                }
                catch
                {
                    retVal = "";
                }
            }
            retVal = ConsoleWriter.FilterXmlProblematicChars(retVal);
            return retVal;
        }

        /// <summary>
        /// checks Qc version (used for link format, 10 and smaller is old) 
        /// </summary>
        /// <returns>true if this QC is an old one</returns>
        private bool CheckIsOldQc()
        {
            string ver = null;
            int intver = -1;
            string build = null;
            TdConnection.GetTDVersion(out ver, out build);
            bool oldQc = false;
            if (ver != null)
            {
                int.TryParse(ver, out intver);
                if (intver <= 10)
                    oldQc = true;
            }
            else
            {
                oldQc = true;
            }
            return oldQc;
        }

        /// <summary>
        /// gets the type for a QC test
        /// </summary>
        /// <param name="currentTest"></param>
        /// <returns></returns>
        private string GetTestType(dynamic currentTest)
        {
            string ttype = currentTest.Test.Type;
            if (ttype.ToUpper() == "SERVICE-TEST")
            {
                ttype = TestType.ST.ToString();
            }
            else
            {
                ttype = TestType.QTP.ToString();
            }
            return ttype;
        }

        /// <summary>
        /// connects to QC and logs in
        /// </summary>
        /// <param name="QCServerURL"></param>
        /// <param name="QCLogin"></param>
        /// <param name="QCPass"></param>
        /// <param name="QCDomain"></param>
        /// <param name="QCProject"></param>
        /// <returns></returns>
        public bool ConnectToProject(string QCServerURL, string QCLogin, string QCPass, string QCDomain, string QCProject)
        {
            if (string.IsNullOrWhiteSpace(QCServerURL)
                || string.IsNullOrWhiteSpace(QCLogin)
                || string.IsNullOrWhiteSpace(QCDomain)
                || string.IsNullOrWhiteSpace(QCProject))
            {
                ConsoleWriter.WriteLine(Resources.AlmRunnerConnParamEmpty);
                return false;
            }

            try
            {
                TdConnection.InitConnectionEx(QCServerURL);
            }
            catch (Exception ex)
            {
                ConsoleWriter.WriteLine(ex.Message);
            }

            if (!TdConnection.Connected)
            {
                ConsoleWriter.WriteErrLine(string.Format(Resources.AlmRunnerServerUnreachable, QCServerURL));
                return false;
            }
            try
            {
                TdConnection.Login(QCLogin, QCPass);
            }
            catch (Exception ex)
            {
                ConsoleWriter.WriteLine(ex.Message);
            }

            if (!TdConnection.LoggedIn)
            {
                ConsoleWriter.WriteErrLine(Resources.AlmRunnerErrorAuthorization);
                return false;
            }

            try
            {
                TdConnection.Connect(QCDomain, QCProject);
            }
            catch (Exception ex)
            {

            }

            if (!TdConnection.ProjectConnected)
            {
                ConsoleWriter.WriteErrLine(Resources.AlmRunnerErrorConnectToProj);
                return false;
            }
            return true;
        }

        private string GetQcCommonInstallationURl(string QCServerURL)
        {
            return QCServerURL + "/TDConnectivity_index.html";
        }

        #region IDisposable Members

        public void Dispose(bool managed)
        {
            if (Connected)
            {
                tdConnection.Disconnect();
                Marshal.ReleaseComObject(tdConnection);
            }
        }

        public void Dispose()
        {
            Dispose(true);
            GC.SuppressFinalize(this);
        }

        #endregion


    }

    public class QCFailure
    {
        public string Name { get; set; }
        public string Desc { get; set; }
    }

    public enum QcRunMode
    {
        RUN_LOCAL,
        RUN_REMOTE,
        RUN_PLANNED_HOST
    }
}
