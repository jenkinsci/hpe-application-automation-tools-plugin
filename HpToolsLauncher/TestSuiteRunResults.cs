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
