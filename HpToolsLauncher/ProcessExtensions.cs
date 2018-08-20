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
using System.ComponentModel;
using System.Diagnostics;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;

namespace HpToolsLauncher
{
    public static class ProcessExtensions
    {
        /// <summary>
        /// Get the parent process for a given process handle.
        /// </summary>
        /// <param name="hProcess">the process handle</param>
        /// <returns>The parent process</returns>
        private static Process GetParentProcess(IntPtr hProcess)
        {
            NativeProcess.PROCESS_BASIC_INFORMATION pbi = new NativeProcess.PROCESS_BASIC_INFORMATION();
            int pbiLength = Marshal.SizeOf(pbi);
            int returnLength = 0;

            int status = NativeProcess.NtQueryInformationProcess(hProcess,NativeProcess.PROCESSINFOCLASS.ProcessBasicInformation,
                ref pbi,pbiLength,out returnLength);

            if(status != 0)
            {
                throw new Win32Exception(status);
            }

            try
            {
                return Process.GetProcessById(pbi.InheritedFromUniqueProcessId.ToInt32());
            }
            catch (ArgumentException)
            { // Not found
                return null;
            }
        }
        /// <summary>
        /// Returns the parent process of a given process
        /// </summary>
        /// <param name="process">the process for which to find the parent</param>
        /// <returns>the parent process</returns>
        public static Process Parent(this Process process)
        {
            return GetParentProcess(process.Handle);
        }
    }
}
