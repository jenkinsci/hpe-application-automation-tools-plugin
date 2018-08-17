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
using System.Text;
using System.Management;
using System.Diagnostics;
using System.IO;
using HpToolsLauncher;
using System.Runtime.InteropServices;

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
                    KillParallelRunnerAutomationProcesses();
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

        private static void KillParallelRunnerAutomationProcess(Process parallelRunner)
        {
            if(parallelRunner != null)
            {
                List<ProcessData> children = new List<ProcessData>();
                GetProcessChildren(parallelRunner.Id, children);

                foreach(var child in children)
                {
                    var proc = Process.GetProcessById(child.ID);

                    if(proc != null)
                    {
                        KillProcess(proc);
                    }
                }

                KillProcess(parallelRunner);
            }
        }

        private static void KillParallelRunnerAutomationProcesses()
        {
            Process[] paralelRunnerProcesses = Process.GetProcessesByName("ParallelRunner");

            // kill every parallel runner process
            foreach(var proc in paralelRunnerProcesses)
            {
                // we are sending SIGINT as ParallelRunner will handle this message
                // gracefully and will set the test status to aborted
                bool closed = SendSigIntToProcess(proc);

                // let's give SIGINT a chance to execute
                proc.WaitForExit(500);

                // if ctr-c has failed, just kill the process...
                if (!closed || !proc.HasExited)
                {
                    KillParallelRunnerAutomationProcess(proc);
                }
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

        private static bool SendSigIntToProcess(Process process)
        {
            const int waitMs = 500;

            // we can only be attached to one console at a time
            if (!FreeConsole())
                return false;

            // try to attach the console to the process
            // that we want to send the signal to
            if (!AttachConsole((uint)process.Id))
                return false;

            // disable the ctrl handler for our process
            // so we do not close ourselvles
            if (!SetConsoleCtrlHandler(null, true))
            {
                FreeConsole();
                AllocConsole();

                return false;
            }

            // Now generate the event and free the console 
            // that we have attached ourselvles to
            if (GenerateConsoleCtrlEvent(CtrlTypes.CTRL_C_EVENT, 0))
            {
                process.WaitForExit(waitMs);
            }

            // free the console for the process that we have attached to
            FreeConsole();

            // alloc a new console for current process
            // as we might need to display something
            AllocConsole();

            SetConsoleCtrlHandler(null, false);

            return true;
        }

        [DllImport("kernel32.dll", SetLastError = true)]
        static extern bool AttachConsole(uint dwProcessId);

        [DllImport("kernel32.dll", SetLastError = true, ExactSpelling = true)]
        static extern bool FreeConsole();

        [DllImport("kernel32.dll")]
        [return: MarshalAs(UnmanagedType.Bool)]
        private static extern bool GenerateConsoleCtrlEvent(CtrlTypes dwCtrlEvent, uint dwProcessGroupId);

        public delegate bool HandlerRoutine(CtrlTypes CtrlType);

        [DllImport("kernel32")]
        public static extern bool SetConsoleCtrlHandler(HandlerRoutine Handler, bool Add);

        [DllImport("kernel32")]
        static extern bool AllocConsole();
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

    enum CtrlTypes : uint
    {
        CTRL_C_EVENT = 0,
        CTRL_BREAK_EVENT,
        CTRL_CLOSE_EVENT,
        CTRL_LOGOFF_EVENT = 5,
        CTRL_SHUTDOWN_EVENT
    }

}
