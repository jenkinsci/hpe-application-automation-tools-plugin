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
 namespace HpToolsLauncher.ParallelTestRunConfiguraion
{
    public class UFTSettings
    {
        public MCSettings mc { get; set; }
    }
    public class MCSettings
    {
        public string protocol { get; set; }
        public string hostname { get; set; }
        public int port { get; set; }
        public string pathname { get; set; }
        public string account { get; set; }
        public string username { get; set; }
        public string password { get; set; }
        public string tenantId { get; set; }
        public string workspaceId { get; set; }
        public ProxySettings proxy { get; set; }
        public int authType { get; set; }
        public string accessKey {  get; set; }
    }
    public class ProxySettings
    {
        public string type { get; set; }
        public string hostname { get; set; }
        public int port { get; set; }
        public string pathname { get; set; }
        public AuthenticationSettings authentication { get; set; }
    }
    public class AuthenticationSettings
    {
        public string username { get; set; }
        public string password { get; set; }
    }

    public class TestRunConfigurationBase
    {
        /// <summary>
        /// The Report Path
        /// </summary>
        public string reportPath { get; set; }
        /// <summary>
        /// The Log Folder
        /// </summary>
        public string logFolder { get; set; }
        /// <summary>
        /// The Log Level
        /// </summary>
        public string logLevel { get; set; }
        /// <summary>
        /// Run Mode: Normal|Fast , default is Fast
        /// </summary>
        public string runMode { get; set; }

        public UFTSettings settings { get; set; }
    }

    public class ParallelTestRunConfiguration : TestRunConfigurationBase
    {
        /// <summary>
        /// The Test Runs
        /// </summary>
        public ParallelTestRunConfigurationItem[] parallelRuns { get; set; }
    }

    /// <summary>
    /// TestRun Configuration for qtdrv
    /// </summary>
    public class TestRunConfiguration : TestRunConfigurationBase
    {
        /// <summary>
        /// GUI Test Path
        /// e.g. test = C:\GUITest1
        /// </summary>
        public string test { get; set; }
        /// <summary>
        /// The Web|Mobile Environment (Optional)
        /// </summary>
        public Environment env { get; set; }
    }

    /// <summary>
    /// Test run result for ParallelRunner
    /// </summary>
    public class RunResult : TestRunConfiguration
    {
        public string errorMessage { get; set; }
    }

    /// <summary>
    /// TestRun Configuration for ParallelRunner
    /// </summary>
    public class ParallelTestRunConfigurationItem : TestRunConfiguration
    {
        /// <summary>
        /// Report Suffix (Optional)
        /// e.g. reportSuffix= ID_C3ZDU,reportPath = C:\parallelexecution\Res1, test= C:\GUITest1
        /// Full ReportPath = C:\parallelexecution\Res1\GUITest1_ID_C3ZDU
        /// </summary>
        public string reportSuffix { get; set; }
    }

    public class Environment
    {
        /// <summary>
        /// Web Environment
        /// </summary>
        public WebEnvironment web { get; set; }
        /// <summary>
        /// Mobile Environment
        /// </summary>
        public MobileEnvironment mobile { get; set; }
    }

    #region Web Environment
    /// <summary>
    /// SRF Web Desktop Environment
    /// </summary>
    public class WebEnvironment
    {
        /// <summary>
        ///  Lab "LocalBrowser"|"MobileCenter"|"SRF"
        /// </summary>
        public string lab { get; set; }
        public string platform { get; set; }
        public string browser { get; set; }
        public string resolution { get; set; }
        public string tunnelName { get; set; }
    }

    #endregion

    #region Mobile Environment
    public class MobileEnvironment
    {
        /// <summary>
        ///  Lab "Disable"|"MobileCenter"|"SRF"
        /// </summary>
        public string lab { get; set; }
        public Device device { get; set; }
    }

    /// <summary>
    /// Device Info
    /// e.g.
    /// "deviceID": "EYKNTO6999999999",
    /// "model": "8681-A01",
    /// "osVersion": "5.1",
    /// "osType": "ANDROID",
    /// "manufacturer": "QiKU"
    /// </summary>
    public class Device
    {
        public string deviceID { get; set; }
        public string manufacturer { get; set; }
        public string model { get; set; }
        public string osVersion { get; set; }
        public string osType { get; set; }
    }
    #endregion
}
