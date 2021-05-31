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
                try
                {
                    if (Directory.Exists(parentFolder))
                    {
                        Directory.Delete(parentFolder, true);
                    }
                    ConsoleWriter.WriteLine("Using parent folder : " + parentFolder);
                }
                catch (Exception e)
                {
                    ConsoleWriter.WriteErrLine("Failed to delete parent folder : " + e.Message);
                }

                Directory.CreateDirectory(parentFolder);
                DirectoryInfo parentDir = new DirectoryInfo(parentFolder);

                try
                {
                    if (_qtpApplication.Launched)
                    {
                        _qtpApplication.Quit();
                    }
                }
                catch (Exception e)
                {
                    ConsoleWriter.WriteErrLine("Failed to close qtpApp : " + e.Message);
                }



                //START Test creation
                //_qtpApplication.Launch();
                //_qtpApplication.Visible = false;
                foreach (var test in tests)
                {
                    DateTime startTotal = DateTime.Now;
                    ConsoleWriter.WriteLine("Creation of " + test.Name + " *****************************");
                    LoadNeededAddins(_qtpApplication, test.UnderlyingTests);

                    try
                    {
                        DateTime startSub1 = DateTime.Now;

                        _qtpApplication.New();
                        ConsoleWriter.WriteLine(string.Format("_qtpApplication.New took {0:0.0} secs", DateTime.Now.Subtract(startSub1).TotalSeconds));
                        QTObjectModelLib.Action qtAction1 = _qtpApplication.Test.Actions[1];
                        qtAction1.Description = "unitIds=" + string.Join(",", test.UnitIds);

                        //https://myskillpoint.com/how-to-use-loadandrunaction-in-uft/#LoadAndRunAction_Having_Input-Output_Parameters
                        //LoadAndRunAction "E:\UFT_WorkSpace\TestScripts\SampleTest","Action1",0,"inputParam1","inputParam2",outParameterVal
                        //string actionContent = "LoadAndRunAction \"c:\\Temp\\GUITest2\\\",\"Action1\"";
                        string actionContent = File.Exists(test.Script) ? File.ReadAllText(test.Script) : test.Script;
                        qtAction1.ValidateScript(actionContent);
                        qtAction1.SetScript(actionContent);

                        DirectoryInfo fullDir = parentDir;
                        if (!string.IsNullOrEmpty(test.PackageName))
                        {
                            fullDir = fullDir.CreateSubdirectory(test.PackageName);
                        }
        
                        string fullPath = fullDir.CreateSubdirectory(test.Name).FullName;
                        _qtpApplication.Test.SaveAs(fullPath);
                        double sec = DateTime.Now.Subtract(startTotal).TotalSeconds;
                        ConsoleWriter.WriteLine(string.Format("MBT test was created in {0} in {1:0.0} secs", fullPath, sec));

                    }
                    catch (Exception e)
                    {
                        ConsoleWriter.WriteErrLine("Fail in MBTRunner : " + e.Message);
                    }
                }
                if (_qtpApplication.Launched)
                {
                    _qtpApplication.Quit();
                }
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
                        DateTime start1 = DateTime.Now;
                        var testAddinsObj = _qtpApplication.GetAssociatedAddinsForTest(fileName);
                        ConsoleWriter.WriteLine(string.Format("GetAssociatedAddinsForTest took {0:0.0} secs", DateTime.Now.Subtract(start1).TotalSeconds));
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

                //if (_qtpApplication.Launched)
                //{
                //_qtpApplication.Quit();
                //ConsoleWriter.WriteLine("LoadNeededAddins : _qtpApplication.Quit");
                //}

                object erroDescription = null;

                string[] addinsArr = new string[addinsSet.Count];
                addinsSet.CopyTo(addinsArr);
                ConsoleWriter.WriteLine("Loading Addins : " + string.Join(",", addinsArr));
                DateTime start2 = DateTime.Now;
                _qtpApplication.SetActiveAddins(addinsArr, out erroDescription);
                ConsoleWriter.WriteLine(String.Format("SetActiveAddins took {0:0.0} secs", DateTime.Now.Subtract(start2).TotalSeconds));
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
        public string Name { get; set; }
        public string Script { get; set; }
        public string UnitIds { get; set; }
        public List<String> UnderlyingTests { get; set; }
        public string PackageName { get; set; }
    }


}
