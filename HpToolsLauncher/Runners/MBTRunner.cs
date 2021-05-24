using QTObjectModelLib;
using System;
using System.Collections.Generic;
using System.IO;

namespace HpToolsLauncher
{
    public class MBTRunner : RunnerBase, IDisposable
    {
        private readonly object _lockObject = new object();
        private string parentFolder;
        private IEnumerable<MBTTest> tests;

        public MBTRunner(string parentFolder, IEnumerable<MBTTest> tests)
        {
            this.parentFolder = parentFolder;
            this.tests = tests;
        }

        public override TestSuiteRunResults Run()
        {
            var type = Type.GetTypeFromProgID("Quicktest.Application");

            lock (_lockObject)
            {
                Application _qtpApplication = Activator.CreateInstance(type) as Application;
                if (Directory.Exists(parentFolder))
                {
                    Directory.Delete(parentFolder, true);
                }
                Directory.CreateDirectory(parentFolder);

                DirectoryInfo parentDir = new DirectoryInfo(parentFolder);

                //LOAD LoadNeededAddins
                HashSet<String> allUnderlyingTests = new HashSet<string>();
                foreach (var test in tests)
                {
                    foreach (var underlyingTest in test.UnderlyingTests)
                    {
                        allUnderlyingTests.Add(underlyingTest);
                    }

                }
                LoadNeededAddins(_qtpApplication, allUnderlyingTests);


                //START Test creation
                _qtpApplication.Launch();
                _qtpApplication.Visible = false;
                foreach (var test in tests)
                {
                    ConsoleWriter.WriteLine("Creation of " + test.Name);
                    try
                    {
                        DateTime start = DateTime.Now;
                        _qtpApplication.New();
                        QTObjectModelLib.Action qtAction1 = _qtpApplication.Test.Actions[1];
                        //string actionContent = "LoadAndRunAction \"c:\\Temp\\GUITest2\\\",\"Action1\"";
                        string actionContent = File.Exists(test.Script) ? File.ReadAllText(test.Script) : test.Script;
                        qtAction1.ValidateScript(actionContent);
                        qtAction1.SetScript(actionContent);

                        string fullPath = parentDir.CreateSubdirectory(test.Name).FullName;
                        if (Directory.Exists(fullPath))
                        {
                            Directory.Delete(fullPath, true/*recursive*/);
                        }

                        _qtpApplication.Test.SaveAs(fullPath);
                        double sec = DateTime.Now.Subtract(start).TotalSeconds;
                        ConsoleWriter.WriteLine($"MBT test was created in {fullPath} in {sec:0.0} secs");

                    }
                    catch (Exception e)
                    {
                        ConsoleWriter.WriteErrLine("Fail in MBTRunner : " + e.Message);
                    }

                }
                _qtpApplication.Quit();
            }

            return null;
        }

        private void LoadNeededAddins(Application _qtpApplication, IEnumerable<String> fileNames)
        {
            try
            {
                HashSet<string> addinsSet = new HashSet<string>();
                foreach (string fileName in fileNames)
                {
                    try
                    {
                        var testAddinsObj = _qtpApplication.GetAssociatedAddinsForTest(fileName);
                        object[] tempTestAddins = (object[])testAddinsObj;

                        foreach (string addin in tempTestAddins)
                        {
                            addinsSet.Add(addin);
                        }
                    }
                    catch (Exception testErr)
                    {
                        ConsoleWriter.WriteErrLine("Fail to LoadNeededAddins for : " + fileName + ", " + testErr.Message);
                    }
                }

                if (_qtpApplication.Launched)
                {
                    _qtpApplication.Quit();
                }

                object erroDescription = null;

                string[] addinsArr = new string[addinsSet.Count];
                addinsSet.CopyTo(addinsArr);
                ConsoleWriter.WriteLine("Loading Addins : " + string.Join(",", addinsArr));
                _qtpApplication.SetActiveAddins(addinsArr, out erroDescription);
                if (!string.IsNullOrEmpty((string)erroDescription))
                {
                    ConsoleWriter.WriteErrLine("Fail to SetActiveAddins : " + erroDescription);
                }
            }
            catch (Exception globalErr)
            {
                ConsoleWriter.WriteErrLine("Fail to LoadNeededAddins : " + globalErr.Message);
                // Try anyway to run the test
            }
        }
    }

    public class MBTTest
    {
        public String Name { get; set; }
        public String Script { get; set; }

        public List<String> UnderlyingTests { get; set; }
    }


}
