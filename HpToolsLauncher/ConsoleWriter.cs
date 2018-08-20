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
            message = FilterXmlProblematicChars(message);
            //File.AppendAllText("c:\\stam11.stam", message);
            Console.Out.WriteLine(message);
            if (activeTestRun != null)
                activeTestRun.ConsoleOut += message + "\n";
        }
    }
}
