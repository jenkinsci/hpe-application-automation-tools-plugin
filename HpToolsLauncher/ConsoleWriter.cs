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
using System.Collections.Generic;
using System.Text.RegularExpressions;
namespace HpToolsLauncher
{
    public static class ConsoleWriter
    {
        static TestRunResults activeTestRun = null;
        static List<string> _errSummaryLines = new List<string>();

        /// <summary>
        /// lines to append to the summary at the end (used for files/dirs not found)
        /// </summary>
        public static List<string> ErrorSummaryLines
        {
            get { return _errSummaryLines; }
            set { _errSummaryLines = value; }
        }

        public static TestRunResults ActiveTestRun
        {
            get { return activeTestRun; }
            set { activeTestRun = value; }
        }

        public static void WriteException(string message, Exception ex)
        {
            Console.Out.WriteLine(message);
            Console.Out.WriteLine(ex.Message);
            Console.Out.WriteLine(ex.StackTrace);
            if (activeTestRun != null)
                activeTestRun.ConsoleErr += message + "\n" + ex.Message + "\n" + ex.StackTrace + "\n";
        }

        public static void WriteException(Exception ex)
        {
            Console.Out.WriteLine(ex.StackTrace);
            if (activeTestRun != null)
                activeTestRun.ConsoleErr += ex.StackTrace + "\n";
        }

        public static void WriteErrLine(string message)
        {
            string errMessage = "Error: " + message;
            WriteLine(errMessage);

            if (activeTestRun != null)
            {
                activeTestRun.ConsoleErr += errMessage + "\n";
            }
        }
        private static Regex _badXmlCharsReg = new Regex(@"[\u0000-\u0008]|[\u000B-\u000C]|[\u000E-\u001F]|[\uD800-\uDFFF]", RegexOptions.Singleline | RegexOptions.IgnoreCase | RegexOptions.Compiled);

        /// <summary>
        /// filters out any bad characters that may affect xml generation/parsing
        /// </summary>
        /// <param name="subjectString"></param>
        /// <returns></returns>
        public static string FilterXmlProblematicChars(string subjectString)
        {
            //allowed chars: #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
            return _badXmlCharsReg.Replace(subjectString, "?");
        }

        public static void WriteLine(string[] messages)
        {
            foreach (string message in messages)
            {

                WriteLine(message);
                if (activeTestRun != null)
                {
                    activeTestRun.ConsoleOut += message + "\n";
                }
            }
        }

        public static void WriteLine(string message)
        {
            message = FilterXmlProblematicChars(message); 
            
            Console.WriteLine(message);

            if (activeTestRun != null)
                activeTestRun.ConsoleOut += message + "\n";
        }

        public static void WriteLineWithTime(string message)
        {
            WriteLine(string.Format("{0} {1}", DateTime.Now.ToString(Launcher.DateFormat), message));
        }

        public static void WriteErrLineWithTime(string message)
        {
            WriteLine(string.Format("Error: {0} {1}", DateTime.Now.ToString(Launcher.DateFormat), message));
        }
    }
}
