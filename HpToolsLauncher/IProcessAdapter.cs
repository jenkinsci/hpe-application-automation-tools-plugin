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
using System.Linq;
using System.Text;

namespace HpToolsLauncher
{
    public interface IProcessAdapter
    {
        int ExitCode { get; }

        bool HasExited { get; }

        void Start();

        void WaitForExit();

        bool WaitForExit(int milliseconds);

        void Kill();

        void Close();
    }

    public class ProcessAdapter : IProcessAdapter
    {
        private Process Process { get; set; }

        public int ExitCode
        {
            get
            {
                return Process.ExitCode;
            }
        }

        public bool HasExited
        {
            get
            {
                return Process.HasExited;
            }
        }

        public ProcessAdapter(Process process)  { Process = process; }

        public void Start()
        {
            Process.Start();
        }

        public void WaitForExit()
        {
            Process.WaitForExit();
        }

        public bool WaitForExit(int milliseconds)
        {
            return Process.WaitForExit(milliseconds);
        }

        public void Kill()
        {
            Process.Kill();
        }

        public void Close()
        {
            Process.Close();
        }
    }

    public class ElevatedProcessAdapter : IProcessAdapter
    {
        private ElevatedProcess ElevatedProcess { get; set; }

        public int ExitCode
        {
            get
            {
                return ElevatedProcess.ExitCode;
            }
        }

        public bool HasExited
        {
            get
            {
                return ElevatedProcess.HasExited;
            }
        }

        public ElevatedProcessAdapter(ElevatedProcess elevatedProcess)  { this.ElevatedProcess = elevatedProcess; }

        public void Start()
        {
            ElevatedProcess.StartElevated();
        }

        public void WaitForExit()
        {
            ElevatedProcess.WaitForExit();
        }

        public bool WaitForExit(int milliseconds)
        {
            return ElevatedProcess.WaitForExit(milliseconds);
        }

        public void Kill()
        {
            ElevatedProcess.Kill();
        }

        public void Close()
        {
            ElevatedProcess.Close();
        }
    }

    public static class ProcessAdapterFactory
    {
        /// <summary>
        /// Create a process adapter based on the type of process.
        /// </summary>
        /// <param name="process">the process object</param>
        /// <returns>an adapter for the given process, null if no adapter available</returns>
        public static IProcessAdapter CreateAdapter(object process)
        { 
            if (process is Process) return new ProcessAdapter((Process)process);
            if (process is ElevatedProcess) return new ElevatedProcessAdapter((ElevatedProcess)process );

            return null;
        }
    }
}
