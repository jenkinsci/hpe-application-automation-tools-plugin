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
using System.Linq;
using System.Runtime.Serialization;
using System.Text;

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
