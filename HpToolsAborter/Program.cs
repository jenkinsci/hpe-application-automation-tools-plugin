//© Copyright 2013 Hewlett-Packard Development Company, L.P.
//Permission is hereby granted, free of charge, to any person obtaining a copy of this software
//and associated documentation files (the "Software"), to deal in the Software without restriction,
//including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
//and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
//subject to the following conditions:

//The above copyright notice and this permission notice shall be included in all copies or
//substantial portions of the Software.

//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
//INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
//PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE 
//LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
//TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE 
//OR OTHER DEALINGS IN THE SOFTWARE.

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Management;
using System.Diagnostics;
using System.IO;
using HpToolsLauncher;

namespace HpToolsAborter
{
    class Program
    {
        static void Main(string[] args)
        {
            try
            {
                if (args == null || args.Length ==0)
                {
                    Console.Out.WriteLine("Usage: HpToolsAborter paramfile");
                    return;
                }

                if (!File.Exists(args[0]))
                {
                    Console.Out.WriteLine("File {0} is missing", args[0]);
                    return;
                }

                string paramfile;

                using (FileStream fs = File.Open(args[0], FileMode.Open, FileAccess.Read, FileShare.Read))
                {
                    using (StreamReader sr = new StreamReader(fs))
                    {
                        paramfile = sr.ReadToEnd();
                    }
                }

                Console.Out.WriteLine("============================================================================");
                Console.Out.WriteLine("Aborting testing tool related processes");

               JavaProperties _ciParams = new JavaProperties();

                _ciParams.Load(args[0]);
                string timeout = _ciParams["timeout"];

                if(!string.IsNullOrEmpty(timeout))
                {
                    int timeoutInSeconds = Int32.Parse(timeout);
                    System.Threading.Thread.Sleep(timeoutInSeconds * 1000);
                }

                string runType = _ciParams["runType"];

                if (string.IsNullOrEmpty(runType))
                {
                    Console.Out.WriteLine("Unable to find runType in " + args[0]);
                    return;
                }

                if (runType=="FileSystem")
                {
                    KillQtpAutomationProcess();
                    KillLoadRunnerAutomationProcess();
                }

                if (runType=="Alm")
                {
                     string almRunMode = _ciParams["almRunMode"];
                    if (almRunMode=="RUN_LOCAL")
                    {
                        KillQtpAutomationFromAlm();
                        KillServiceTestFromAlm();
                    }
                    else if (almRunMode == "RUN_REMOTE")
                    {
                        Console.Out.WriteLine(string.Format("Stopping a test in a remote machine is not supported. Test in {0} should be stopped manually",_ciParams["almRunHost"]));
                    }
                }
            }
            catch (Exception ex)
            {
                Console.Out.WriteLine(string.Format("Error in HpToolsAborter: {0} ",ex.Message));
            }
        }

        private static void KillLoadRunnerAutomationProcess()
        {
            var lrAutomationProcess = Process.GetProcessesByName("Wlrun").FirstOrDefault();
            List<ProcessData> children = new List<ProcessData>();

            if (lrAutomationProcess != null)
            {
                GetProcessChildren(lrAutomationProcess.Id, children);
                foreach (var child in children)
                {
                    var proc = Process.GetProcessById(child.ID);
                    if (proc != null)
                    {
                        KillProcess(proc);
                    }
                }
                KillProcess(lrAutomationProcess);

            }

        }


        private static void KillQtpAutomationProcess()
        {
            var qtpAutomationProcess = Process.GetProcessesByName("QtpAutomationAgent").FirstOrDefault();

            List<ProcessData> children = new List<ProcessData>();
            if (qtpAutomationProcess != null)
            {
                GetProcessChildren(qtpAutomationProcess.Id, children);
            }

            if (qtpAutomationProcess != null)
            {
                KillProcess(qtpAutomationProcess);

                foreach (var child in children)
                {
                    var proc = Process.GetProcessById(child.ID);
                    if (proc != null)
                    {
                        KillProcess(proc);
                    }
                }
            }
        }

        private static void KillQtpAutomationFromAlm()
        {
            var remoteAgent = Process.GetProcessesByName("AQTRmtAgent").FirstOrDefault();

            if (remoteAgent != null)
            {
                KillProcess(remoteAgent);
            }
            // new remote agent
            remoteAgent = Process.GetProcessesByName("UFTRemoteAgent").FirstOrDefault();
            if (remoteAgent != null)
            {
                KillProcess(remoteAgent);
            }

            KillQtpAutomationProcess();

            //some how if run from ALM, use the above method cannot stop uft.exe
            var uft = Process.GetProcessesByName("UFT").FirstOrDefault();
            if (uft != null)
            {
                Console.Out.WriteLine(string.Format("begin to kill uft.exe"));
                KillProcess(uft);
            }
        }

        private static void KillServiceTestFromAlm()
        {

            var dllHostProcesses = Process.GetProcessesByName("dllhost");

            foreach (var dllhostProcess in dllHostProcesses)
            {
                List<ProcessData> children = new List<ProcessData>();

                GetProcessChildren(dllhostProcess.Id, children);

                var internalExecuterData = children.Where(i => i.Name == "HP.ST.Fwk.InternalExecuter").FirstOrDefault();

                if (internalExecuterData != null)
                {
                    var process = Process.GetProcessById(internalExecuterData.ID);
                    KillProcess(process);

                    KillProcess(dllhostProcess);
                    break;
                }
            }
        }

        /// <summary>
        /// Kill a process, and all of its children.
        /// </summary>
        /// <param name="pid">Process ID.</param>
        private static void KillProcessAndChildren(int pid)
        {
            ManagementObjectSearcher searcher = new ManagementObjectSearcher("Select * From Win32_Process Where ParentProcessID=" + pid);
            ManagementObjectCollection moc = searcher.Get();
            foreach (ManagementObject mo in moc)
            {
                KillProcessAndChildren(Convert.ToInt32(mo["ProcessID"]));
            }
            try
            {
                Process proc = Process.GetProcessById(pid);
                proc.Kill();
            }
            catch (ArgumentException ex)
            {
                // Process already exited.
            }
        }

        private static void GetProcessChildren(int pid, List<ProcessData> children)
        {
            ManagementObjectSearcher searcher = new ManagementObjectSearcher("Select * From Win32_Process Where ParentProcessID=" + pid);
            ManagementObjectCollection moc = searcher.Get();

            foreach (ManagementObject mo in moc)
            {
                int procId = Convert.ToInt32(mo["ProcessID"]);
                string procName = mo["Name"].ToString().Replace(".exe", "");

                children.Add(new ProcessData(procId, procName));
                GetProcessChildren(procId, children);
            }


        }

        private static void KillProcess(Process process)
        {
            try
            {
                Console.Out.Write(string.Format("Trying to terminate {0}", process.ProcessName));
                process.Kill();
                Console.Out.WriteLine("...Terminated");
            }
            catch (Exception ex)
            {
                Console.Out.Write(string.Format("...Failed to terminate {0}.Reason: {1} ", process.ProcessName, ex.Message));
            }
        }

    }

    public class ProcessData
    {
        public ProcessData(int id, string name)
        {
            this.ID = id;
            this.Name = name;
        }

        public int ID { get; private set; }
        public string Name { get; private set; }
    }

}
