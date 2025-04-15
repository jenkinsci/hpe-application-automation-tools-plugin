/**
 *  Certain versions of software accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 *  This software was acquired by Micro Focus on September 1, 2017, and is now
 *  offered by OpenText.
 *  Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical
 *  in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the
 *  property of their respective owners.
 *  OpenText is a trademark of Open Text.
 *  __________________________________________________________________
 *  MIT License
 *
 *  Copyright 2012-2025 Open Text.
 *
 *  The only warranties for products and services of Open Text and
 *  its affiliates and licensors ("Open Text") are as may be set forth
 *  in the express warranty statements accompanying such products and services.
 *  Nothing herein should be construed as constituting an additional warranty.
 *  Open Text shall not be liable for technical or editorial errors or
 *  omissions contained herein. The information contained herein is subject
 *  to change without notice.
 *
 *  Except as specifically indicated otherwise, this document contains
 *  confidential information and a valid license is required for possession,
 *  use or copying. If this work is provided to the U.S. Government,
 *  consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 *  Computer Software Documentation, and Technical Data for Commercial Items are
 *  licensed to the U.S. Government under vendor's standard commercial license.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ___________________________________________________________________
 */
 using HP.LoadRunner.Interop.Wlrun;
using System;
using System.Text;

namespace HpToolsLauncher.TestRunners
{
    public class SummaryDataLogger
    {
        private bool m_logVusersStates;
        private bool m_logErrorCount;
        private bool m_logTransactionStatistics;
        private int m_pollingInterval;

        public SummaryDataLogger(bool logVusersStates, bool logErrorCount, bool logTransactionStatistics, int pollingInterval)
        {
            m_logVusersStates = logVusersStates;
            m_logErrorCount = logErrorCount;
            m_logTransactionStatistics = logTransactionStatistics;
            m_pollingInterval = pollingInterval;
        }

        public SummaryDataLogger() { }

        public int GetPollingInterval()
        {
            return m_pollingInterval * 1000;
        }

        private enum VUSERS_STATE
        {
            Down = 1,
            Pending = 2,
            Init = 3,
            Ready = 4,
            Run = 5,
            Rendez = 6,
            Passed = 7,
            Failed = 8,
            Error = 9,
            Exiting = 10,
            Stopped = 11,
            G_Exit = 12 //Gradual Exiting
        }

        private void LogVuserStates(LrScenario scenario)
        {
            StringBuilder headerBuilder = new StringBuilder(),
                          bodyBuilder = new StringBuilder();

            foreach (var vuserState in Enum.GetValues(typeof(VUSERS_STATE)))
            {
                headerBuilder.Append(string.Format("{0, -10}", vuserState.ToString()));
            }

            foreach (var vuserState in Enum.GetValues(typeof(VUSERS_STATE)))
            {
                bodyBuilder.Append(string.Format("{0, -10}", scenario.GetVusersCount((int)vuserState)));
            }

            ConsoleWriter.WriteLine(headerBuilder.ToString());
            ConsoleWriter.WriteLine(bodyBuilder.ToString());
        }

        private void LogErrorCount(LrScenario scenario)
        {

            int errorsCount = scenario.GetErrorsCount("");

            ConsoleWriter.WriteLine("Error count: " + errorsCount);
        }

        private void LogScenarioDuration(LrScenario scenario)
        {
            int scenarioDuration = scenario.ScenarioDuration;
            TimeSpan time = TimeSpan.FromSeconds(scenarioDuration);
            string convertedTime = time.ToString(@"dd\:hh\:mm\:ss");

            ConsoleWriter.WriteLine("Elapsed Time (D:H:M:S): " + convertedTime);
        }

        public void LogSummaryData(LrScenario scenario)
        {
            if (m_logVusersStates || m_logErrorCount || m_logTransactionStatistics)
            {
                LogScenarioDuration(scenario);

                if (m_logVusersStates)
                {
                    LogVuserStates(scenario);
                }

                if (m_logErrorCount)
                {
                    LogErrorCount(scenario);
                }

                if (m_logTransactionStatistics)
                {
                    LogTransactionStatistics(scenario);
                }
            }
        }

        public bool IsAnyDataLogged()
        {
            return (m_logVusersStates || m_logErrorCount || m_logTransactionStatistics);
        }

        private void LogTransactionStatistics(LrScenario scenario)
        {
            int passed = 0, failed = 0;
            double hitsPerSecond = 0;
            scenario.GetTransactionStatistics(ref passed, ref failed, ref hitsPerSecond);

            ConsoleWriter.WriteLine("Passed transactions: " + passed);
            ConsoleWriter.WriteLine("Failed transactions: " + failed);
            ConsoleWriter.WriteLine("Hits per second: " + Math.Round(hitsPerSecond, 2));
        }

        public void LogTransactionDetails(LrScenario scenario)
        {
            string transactionDetails;
            scenario.GetTransactionStatisticsDetails(out transactionDetails);

            ConsoleWriter.WriteLine("Transaction details: " + transactionDetails);
        }
    }
}
