// (c) Copyright 2012 Hewlett-Packard Development Company, L.P. 
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

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
            message = message.Replace("\\n", "\n");
            message = FilterXmlProblematicChars(message);
            //File.AppendAllText("c:\\stam11.stam", message);
            Console.Out.WriteLine(message);
            if (activeTestRun != null)
                activeTestRun.ConsoleOut += message + "\n";
        }
    }
}
