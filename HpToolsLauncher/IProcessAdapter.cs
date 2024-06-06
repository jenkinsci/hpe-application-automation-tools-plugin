/*
 * Certain versions of software accessible here may contain branding from Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 * This software was acquired by Micro Focus on September 1, 2017, and is now offered by OpenText.
 * Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright 2012-2024 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ___________________________________________________________________
 */

using System;
using System.Diagnostics;

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

        public ProcessAdapter(Process process) { Process = process; }

        public void Start()
        {
            Process.Start();

            if (Process.StartInfo.RedirectStandardError)
            {
                Process.BeginErrorReadLine();
            }

            if (Process.StartInfo.RedirectStandardOutput)
            {
                Process.BeginOutputReadLine();
            }
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

        public ElevatedProcessAdapter(ElevatedProcess elevatedProcess) { this.ElevatedProcess = elevatedProcess; }

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
            if (process is ElevatedProcess) return new ElevatedProcessAdapter((ElevatedProcess)process);

            return null;
        }
    }
}
