using QTObjectModelLib;
using System;
using System.Collections;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;

namespace HpToolsLauncher
{
    public class MBTRunner : RunnerBase, IDisposable
    {
        private readonly object _lockObject = new object();
        private string script;
        private string resultTest;
        private IEnumerable<string> tests;

        public MBTRunner(string script, string resultTest, IEnumerable<string> tests)
        {
            this.script = script;
            this.resultTest = resultTest;
            this.tests = tests;
        }

        public override TestSuiteRunResults Run()
        {
            var type = Type.GetTypeFromProgID("Quicktest.Application");

            lock (_lockObject)
            {
                Application _qtpApplication = Activator.CreateInstance(type) as Application;
                LoadNeededAddins(_qtpApplication, tests);

                try
                {
                    Version qtpVersion = Version.Parse(_qtpApplication.Version);
                    _qtpApplication.Launch();
                    _qtpApplication.Visible = false;

                    _qtpApplication.New();
                    QTObjectModelLib.Action qtAction1 = _qtpApplication.Test.Actions[1];
                    //string actionContent = "LoadAndRunAction \"c:\\Temp\\GUITest2\\\",\"Action1\"";
                    string actionContent = File.ReadAllText(script);
                    qtAction1.ValidateScript(actionContent);
                    qtAction1.SetScript(actionContent);

                    if (Directory.Exists(resultTest))
                    {
                        Directory.Delete(resultTest, true/*recursive*/);
                    }

                    _qtpApplication.Test.SaveAs(resultTest);
                    ConsoleWriter.WriteLine("MBT test saved to : " +  resultTest);
                    _qtpApplication.Quit();
                }
                catch (Exception e)
                {
                    ConsoleWriter.WriteErrLine("Fail in MBTRunner : " + e.Message);
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


}
