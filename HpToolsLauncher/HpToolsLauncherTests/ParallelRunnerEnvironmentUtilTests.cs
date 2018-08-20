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

using HpToolsLauncher;
using HpToolsLauncher.ParallelRunner;
using HpToolsLauncher.ParallelTestRunConfiguraion;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.IO;
using System.Linq;
using System.Text;

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
            }catch(ParallelRunnerConfigurationException)
            {
                Assert.Fail();
            }

            Assert.AreEqual(0, configuration.parallelRuns.Length);
        }

        [TestMethod]
        public void GetMCProxySettingsTest_ValidMCSettings_ReturnsExpectedProxySettings()
        {
            McConnectionInfo mcConnectionInfo = new McConnectionInfo();
            mcConnectionInfo.MobileProxySetting_Address = "192.168.1.1";
            mcConnectionInfo.MobileProxySetting_Port = 8080;
            mcConnectionInfo.MobileProxySetting_Authentication = 1;
            mcConnectionInfo.MobileProxySetting_UserName = "test";
            mcConnectionInfo.MobileProxySetting_Password = "test";

            ProxySettings settings = ParallelRunnerEnvironmentUtil.GetMCProxySettings(mcConnectionInfo);

            Assert.IsNotNull(settings);
            Assert.IsNotNull(settings.authentication);

            Assert.AreEqual(mcConnectionInfo.MobileProxySetting_UserName, settings.authentication.username);
            Assert.AreEqual(mcConnectionInfo.MobileProxySetting_Address, settings.hostname);
            Assert.AreEqual(mcConnectionInfo.MobileProxySetting_Port, settings.port);
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
            mcConnectionInfo.MobileHostAddress = "192.168.1.1";
            mcConnectionInfo.MobileHostPort = "8080";
            mcConnectionInfo.MobileUserName = "test";
            mcConnectionInfo.MobilePassword = "test";
            mcConnectionInfo.MobileUseSSL = 1;

            UFTSettings settings = ParallelRunnerEnvironmentUtil.ParseMCSettings(mcConnectionInfo);

            Assert.IsNotNull(settings);
            Assert.IsNotNull(settings.mc);

            Assert.AreEqual(mcConnectionInfo.MobileHostAddress, settings.mc.hostname);
            Assert.AreEqual(mcConnectionInfo.MobileHostPort, settings.mc.port.ToString());
            Assert.AreEqual(mcConnectionInfo.MobileUserName, settings.mc.username);
            Assert.AreEqual("https", settings.mc.protocol);
        }

        [TestMethod]
        public void ParseMCSettingsTest_ValidMCSettingsNonSSL_ReturnsExpectedSettings()
        {
            McConnectionInfo mcConnectionInfo = new McConnectionInfo();
            mcConnectionInfo.MobileHostAddress = "192.168.1.1";
            mcConnectionInfo.MobileHostPort = "8080";
            mcConnectionInfo.MobileUserName = "test";
            mcConnectionInfo.MobilePassword = "test";

            UFTSettings settings = ParallelRunnerEnvironmentUtil.ParseMCSettings(mcConnectionInfo);

            Assert.IsNotNull(settings);
            Assert.IsNotNull(settings.mc);

            Assert.AreEqual(mcConnectionInfo.MobileHostAddress, settings.mc.hostname);
            Assert.AreEqual(mcConnectionInfo.MobileHostPort, settings.mc.port.ToString());
            Assert.AreEqual(mcConnectionInfo.MobileUserName, settings.mc.username);
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
