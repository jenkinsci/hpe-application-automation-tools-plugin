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
 using HpToolsLauncher;
using HpToolsLauncher.ParallelRunner;
using HpToolsLauncher.ParallelTestRunConfiguraion;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using System.Collections.Generic;

namespace HpToolsLauncherTests
{
    [TestClass]
    public class ParallelRunnerEnvironmentUtilTests
    {
        private static readonly string MobileEnvironment = "osType : Android,osVersion : 4.4.2,manufacturerAndModel : \"samsung GT-I9515\"";
        private static readonly string WebEnvironment = "browser : chrome";

        [TestMethod]
        public void GetEnvironmentPropertiesTest_ValidEnvironment_ReturnsExpectedProperties()
        {
            Dictionary<string, string> properties = ParallelRunnerEnvironmentUtil.
                GetEnvironmentProperties(MobileEnvironment);

            // there should be 3 properties
            Assert.AreEqual(properties.Count, 3);

            // check if all the properties are present
            Assert.IsTrue(properties.ContainsKey("ostype"));
            Assert.IsTrue(properties.ContainsKey("osversion"));
            Assert.IsTrue(properties.ContainsKey("manufacturerandmodel"));

            Assert.AreEqual("android", properties["ostype"]);
            Assert.AreEqual("4.4.2", properties["osversion"]);
            Assert.AreEqual("\"samsung gt-i9515\"", properties["manufacturerandmodel"]);
        }

        [TestMethod]
        public void GetEnvironmentPropertiesTest_NoValueEnvironment_ReturnsEmptyDictionary()
        {
            string environment = "deviceId:   ";

            Dictionary<string, string> properties = ParallelRunnerEnvironmentUtil.
                GetEnvironmentProperties(environment);

            // there should be 0 properties
            Assert.AreEqual(0, properties.Count);
        }

        [TestMethod]
        public void GetEnvironmentPropertiesTest_NoKeyEnvironment_ReturnsEmptyDictionary()
        {
            string environment = ":  Android ";

            Dictionary<string, string> properties = ParallelRunnerEnvironmentUtil.
                GetEnvironmentProperties(environment);

            // there should be 0 properties
            Assert.AreEqual(0, properties.Count);
        }

        [TestMethod]
        public void GetEnvironmentPropertiesTest_NullEnvironment_ReturnsEmptyDictionary()
        {
            string environment = null;

            Dictionary<string, string> properties = ParallelRunnerEnvironmentUtil.
                GetEnvironmentProperties(environment);

            // there should be 0 properties
            Assert.AreEqual(0, properties.Count);
        }

        [TestMethod]
        public void GetEnvironmentPropertiesTest_EmptyEnvironment_ReturnsEmptyDictionary()
        {
            string environment = "";

            Dictionary<string, string> properties = ParallelRunnerEnvironmentUtil.
                GetEnvironmentProperties(environment);

            // there should be 0 properties
            Assert.AreEqual(0, properties.Count);
        }

        [TestMethod]
        public void ParseMobileEnvironmentTest_ValidMobileEnvironment_ReturnsExpectedMobileEnvironment()
        {
            MobileEnvironment mobileEnvironment = ParallelRunnerEnvironmentUtil.
                ParseMobileEnvironment(MobileEnvironment);

            // function suceeded
            Assert.IsNotNull(mobileEnvironment);

            // propertes should be non-null
            Assert.IsNotNull(mobileEnvironment.device);
            Assert.IsNotNull(mobileEnvironment.lab);

            Device device = mobileEnvironment.device;

            // not present in the string
            Assert.IsNull(device.deviceID);

            // only the manufacturer should be filled
            Assert.IsNull(device.model);

            // must be present
            Assert.IsNotNull(device.manufacturer);
            Assert.IsNotNull(device.osType);
            Assert.IsNotNull(device.osVersion);

            Assert.AreEqual("android", device.osType);
            Assert.AreEqual("4.4.2", device.osVersion);
            Assert.AreEqual("\"samsung gt-i9515\"", device.manufacturer);
        }

        [TestMethod]
        public void ParseMobileEnvironmentTest_InvalidKeyMobileEnvironment_ReturnsNull()
        {
            string invalidEnvironment = "invalid: android";

            MobileEnvironment mobileEnvironment = ParallelRunnerEnvironmentUtil.
                ParseMobileEnvironment(invalidEnvironment);

            // function suceeded
            Assert.IsNull(mobileEnvironment);
        }

        [TestMethod]
        public void ParseMobileEnvironmentTest_NullMobileEnvironment_ReturnsNull()
        {
            MobileEnvironment mobileEnvironment = ParallelRunnerEnvironmentUtil.
                ParseMobileEnvironment(null);

            // function suceeded
            Assert.IsNull(mobileEnvironment);
        }

        [TestMethod]
        public void ParseMobileEnvironmentTest_EmptyMobileEnvironment_ReturnsNull()
        {
            MobileEnvironment mobileEnvironment = ParallelRunnerEnvironmentUtil.
                ParseMobileEnvironment("");

            // function suceeded
            Assert.IsNull(mobileEnvironment);
        }

        [TestMethod]
        public void ParseWebEnvironmentTest_ValidWebEnvironment_ReturnsExpectedWebEnvironment()
        {
            WebEnvironment webEnvironment = ParallelRunnerEnvironmentUtil.
                ParseWebEnvironment(WebEnvironment);

            // function suceeded
            Assert.IsNotNull(webEnvironment);

            // browser should be present
            Assert.IsNotNull(webEnvironment.browser);

            // local browser lab should be present
            Assert.IsNotNull(webEnvironment.lab);

            // browser should be 'chrome'
            Assert.AreEqual("chrome", webEnvironment.browser);

            Assert.AreEqual("LocalBrowser", webEnvironment.lab);
        }

        [TestMethod]
        public void ParseWebEnvironmentTest_InvalidKeyWebEnvironment_ReturnsExpectedWebEnvironment()
        {
            string invalidKey = "brows: Chrome";

            WebEnvironment webEnvironment = ParallelRunnerEnvironmentUtil.
                ParseWebEnvironment(invalidKey);

            // function suceeded
            Assert.IsNull(webEnvironment);
        }

        [TestMethod]
        public void ParseWebEnvironmentTest_NullWebEnvironment_ReturnsExpectedWebEnvironment()
        {
            WebEnvironment webEnvironment = ParallelRunnerEnvironmentUtil.
                ParseWebEnvironment(null);

            // function suceeded
            Assert.IsNull(webEnvironment);
        }

        [TestMethod]
        public void ParseWebEnvironmentTest_EmptyWebEnvironment_ReturnsExpectedWebEnvironment()
        {
            WebEnvironment webEnvironment = ParallelRunnerEnvironmentUtil.
                ParseWebEnvironment("");

            // function suceeded
            Assert.IsNull(webEnvironment);
        }

        [TestMethod]
        public void GetEvironmentTypeTest_ValidMobileEnvironment_ReturnMobileEnvironment()
        {
            EnvironmentType type =
                ParallelRunnerEnvironmentUtil.GetEnvironmentType(MobileEnvironment);

            Assert.AreEqual(EnvironmentType.MOBILE, type);
        }

        [TestMethod]
        public void GetEvironmentTypeTest_ValidWebEnvironment_ReturnWebEnvironment()
        {
            EnvironmentType type =
                ParallelRunnerEnvironmentUtil.GetEnvironmentType(WebEnvironment);

            Assert.AreEqual(EnvironmentType.WEB, type);
        }

        [TestMethod]
        public void GetEvironmentTypeTest_NullEnvironment_ReturnUnknownEnvironment()
        {
            EnvironmentType type =
                ParallelRunnerEnvironmentUtil.GetEnvironmentType(null);

            Assert.AreEqual(EnvironmentType.UNKNOWN, type);
        }

        [TestMethod]
        public void GetEvironmentTypeTest_EnvironmentEmpty_ReturnUnknownEnvironment()
        {
            EnvironmentType type =
                ParallelRunnerEnvironmentUtil.GetEnvironmentType("");

            Assert.AreEqual(EnvironmentType.UNKNOWN, type);
        }

        [TestMethod]
        public void ParseEnvironmentStringsTest_ValidEnvironments_ReturnValidRunConfiguration()
        {
            IList<string> environments = new List<string>{
                MobileEnvironment,
                WebEnvironment
            };

            TestInfo mockTesInfo = new TestInfo("c:\tests\test1",
                    "c:\tests\test1", "1", "Test1");

            mockTesInfo.ReportPath = mockTesInfo.TestPath;

            ParallelTestRunConfiguration configuration = null;

            try
            {
                configuration = ParallelRunnerEnvironmentUtil.
                    ParseEnvironmentStrings(environments, mockTesInfo);
            }
            catch (ParallelRunnerConfigurationException)
            {
                // since the environments are valid there should be no exception
                Assert.Fail();
            }

            // report paths should be equal
            Assert.AreEqual(mockTesInfo.ReportPath, configuration.reportPath);

            // we have two parallel runs
            // one for web and one for mobile
            Assert.AreEqual(2, configuration.parallelRuns.Length);
        }

        [TestMethod]
        [ExpectedException(typeof(ParallelRunnerConfigurationException),
            "Invalid environments were allowed!")]
        public void ParseEnvironmentStringsTest_InvalidEnvironments_ThrowException()
        {
            // the list of supported browsers
            IList<string> environments = new List<string>{
                "browser: unknown",
            };

            TestInfo mockTesInfo = new TestInfo("c:\tests\test1",
                "c:\tests\test1", "1", "Test1");

            mockTesInfo.ReportPath = mockTesInfo.TestPath;

            ParallelTestRunConfiguration configuration = ParallelRunnerEnvironmentUtil.
                    ParseEnvironmentStrings(environments, mockTesInfo);
        }

        [TestMethod]
        public void ParseEnvironmentStringsTest_UnknownEnvironments_ReturnEmptyConfiguration()
        {
            // the list of supported browsers
            IList<string> environments = new List<string>{
                "test: ",
            };

            TestInfo mockTesInfo = new TestInfo("c:\tests\test1",
                "c:\tests\test1", "1", "Test1");

            mockTesInfo.ReportPath = mockTesInfo.TestPath;
            ParallelTestRunConfiguration configuration = null;

            try
            {
                configuration = ParallelRunnerEnvironmentUtil.
                        ParseEnvironmentStrings(environments, mockTesInfo);
            }
            catch (ParallelRunnerConfigurationException)
            {
                Assert.Fail();
            }

            Assert.AreEqual(0, configuration.parallelRuns.Length);
        }

        [TestMethod]
        public void GetMCProxySettingsTest_ValidMCSettings_ReturnsExpectedProxySettings()
        {
            McConnectionInfo mcConnectionInfo = new McConnectionInfo();
            mcConnectionInfo.ProxyAddress = "192.168.1.1";
            mcConnectionInfo.ProxyPort = 8080;
            mcConnectionInfo.UseProxyAuth = true;
            mcConnectionInfo.ProxyUserName = "test";
            mcConnectionInfo.ProxyPassword = "test";

            ProxySettings settings = ParallelRunnerEnvironmentUtil.GetMCProxySettings(mcConnectionInfo);

            Assert.IsNotNull(settings);
            Assert.IsNotNull(settings.authentication);

            Assert.AreEqual(mcConnectionInfo.ProxyUserName, settings.authentication.username);
            Assert.AreEqual(mcConnectionInfo.ProxyAddress, settings.hostname);
            Assert.AreEqual(mcConnectionInfo.ProxyPort, settings.port);
        }

        [TestMethod]
        public void GetMCProxySettingsTest_InvalidMCSettings_ReturnsNullProxySettings()
        {
            McConnectionInfo mcConnectionInfo = new McConnectionInfo();

            ProxySettings settings = ParallelRunnerEnvironmentUtil.GetMCProxySettings(mcConnectionInfo);

            Assert.IsNull(settings);
        }

        [TestMethod]
        public void ParseMCSettingsTest_ValidMCSettingsSSL_ReturnsExpectedSettings()
        {
            McConnectionInfo mcConnectionInfo = new McConnectionInfo();
            mcConnectionInfo.HostAddress = "192.168.1.1";
            mcConnectionInfo.HostPort = "8080";
            mcConnectionInfo.UserName = "test";
            mcConnectionInfo.Password = "test";
            mcConnectionInfo.UseSSL = true;

            UFTSettings settings = ParallelRunnerEnvironmentUtil.ParseMCSettings(mcConnectionInfo);

            Assert.IsNotNull(settings);
            Assert.IsNotNull(settings.mc);

            Assert.AreEqual(mcConnectionInfo.HostAddress, settings.mc.hostname);
            Assert.AreEqual(mcConnectionInfo.HostPort, settings.mc.port.ToString());
            Assert.AreEqual(mcConnectionInfo.UserName, settings.mc.username);
            Assert.AreEqual("https", settings.mc.protocol);
        }

        [TestMethod]
        public void ParseMCSettingsTest_ValidMCSettingsNonSSL_ReturnsExpectedSettings()
        {
            McConnectionInfo mcConnectionInfo = new McConnectionInfo();
            mcConnectionInfo.HostAddress = "192.168.1.1";
            mcConnectionInfo.HostPort = "8080";
            mcConnectionInfo.UserName = "test";
            mcConnectionInfo.Password = "test";

            UFTSettings settings = ParallelRunnerEnvironmentUtil.ParseMCSettings(mcConnectionInfo);

            Assert.IsNotNull(settings);
            Assert.IsNotNull(settings.mc);

            Assert.AreEqual(mcConnectionInfo.HostAddress, settings.mc.hostname);
            Assert.AreEqual(mcConnectionInfo.HostPort, settings.mc.port.ToString());
            Assert.AreEqual(mcConnectionInfo.UserName, settings.mc.username);
            Assert.AreEqual("http", settings.mc.protocol);
        }

        [TestMethod]
        public void ParseMCSettingsTest_InvalidMCSettings_ReturnsNullSettings()
        {
            McConnectionInfo mcConnectionInfo = new McConnectionInfo();

            UFTSettings settings = ParallelRunnerEnvironmentUtil.ParseMCSettings(mcConnectionInfo);

            Assert.IsNull(settings);
        }

        [TestMethod]
        public void IsKnownMobilePropertyTest_KnownProperty_ReturnsTrue()
        {
            bool isKnown = ParallelRunnerEnvironmentUtil.IsKnownMobileProperty("deviceId")
                           && ParallelRunnerEnvironmentUtil.IsKnownMobileProperty("osVersion")
                           && ParallelRunnerEnvironmentUtil.IsKnownMobileProperty("osType")
                           && ParallelRunnerEnvironmentUtil.IsKnownMobileProperty("manufacturerAndModel");

            Assert.IsTrue(isKnown);
        }

        [TestMethod]
        public void IsKnownMobilePropertyTest_UnknownProperty_ReturnsTrue()
        {
            bool isKnown = ParallelRunnerEnvironmentUtil.IsKnownMobileProperty("unknown");
            Assert.IsFalse(isKnown);
        }
    }
}
