/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2021 Micro Focus or one of its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * ___________________________________________________________________
 */

using System;
using System.Collections.Generic;

namespace HpToolsLauncher
{
    public class TestSuiteRunResults
    {
        private List<TestRunResults> m_testRuns = new List<TestRunResults>();
        private int m_numErrors = 0;
        private int m_numFailures = 0;
        private int m_numTests = 0;
        private int m_numWarnings = 0;
        private TimeSpan m_totalRunTime = TimeSpan.Zero;

        public string SuiteName { get; set; }

        public int NumFailures
        {
            get { return m_numFailures; }
            set { m_numFailures = value; }
        }

        public int NumTests
        {
            get { return m_numTests; }
            set { m_numTests = value; }
        }

        public int NumWarnings
		{
            get { return m_numWarnings; }
            set { m_numWarnings = value; }
		}

        public TimeSpan TotalRunTime
        {
            get { return m_totalRunTime; }
            set { m_totalRunTime = value; }
        }

        public List<TestRunResults> TestRuns
        {
            get { return m_testRuns; }
            set { m_testRuns = value; }
        }

        public int NumErrors
        {
            get { return m_numErrors; }
            set { m_numErrors = value; }
        }


        internal void AppendResults(TestSuiteRunResults desc)
        {
            this.TestRuns.AddRange(desc.TestRuns);
            this.TotalRunTime += desc.TotalRunTime;
            this.NumErrors += desc.NumErrors;
            this.NumFailures += desc.NumFailures;
            this.NumTests += desc.NumTests;
            this.NumWarnings += desc.NumWarnings;
        }
    }
}
