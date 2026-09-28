/*
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
 *  Copyright 2012-2026 Open Text.
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
using System;
using System.Runtime.InteropServices;
using System.Security;
using HpToolsLauncher;
using Microsoft.VisualStudio.TestTools.UnitTesting;

namespace HpToolsLauncherTests
{
    [TestClass]
    public class MobileConnectionInfoTests
    {
        [TestMethod]
        public void ExecToken_IsNullByDefault()
        {
            using (var connectionInfo = new McConnectionInfo())
            {
                Assert.IsNull(connectionInfo.ExecToken);
            }
        }

        [TestMethod]
        public void ExecToken_DoParseAndProtectSecret()
        {
            using (var connectionInfo = new McConnectionInfo())
            using (var execToken = ToSecureString("client=client-id; secret=secret-value; tenant=tenant-id;"))
            {
                connectionInfo.ExecToken = execToken;

                Assert.AreEqual(McConnectionInfo.AuthType.AuthToken, connectionInfo.MobileAuthType);
                Assert.AreEqual("client-id", connectionInfo.GetAuthToken().ClientId);
                Assert.AreEqual("tenant-id", connectionInfo.TenantId);
                Assert.AreEqual("secret-value", ReadSecureString(connectionInfo.GetAuthToken().SecretKey));
            }
        }

        [TestMethod]
        public void ExecToken_RejectsPartWithMultipleKeyValueSeparators()
        {
            using (var connectionInfo = new McConnectionInfo())
            using (var execToken = ToSecureString("client=client-id=unexpected; secret=secret-value; tenant=tenant-id;"))
            {
                try
                {
                    connectionInfo.ExecToken = execToken;
                    Assert.Fail("An invalid execution token should be rejected.");
                }
                catch (ArgumentException)
                {
                }
            }
        }

        [TestMethod]
        public void ExecToken_RejectsTokenWithMissingPart()
        {
            using (var connectionInfo = new McConnectionInfo())
            using (var execToken = ToSecureString("client=client-id; secret=secret-value;"))
            {
                try
                {
                    connectionInfo.ExecToken = execToken;
                    Assert.Fail("An execution token with missing parts should be rejected.");
                }
                catch (ArgumentException)
                {
                }

                Assert.AreEqual(McConnectionInfo.AuthType.UsernamePassword, connectionInfo.MobileAuthType);
                Assert.IsTrue(connectionInfo.GetAuthToken().SecretKey == null);
            }
        }

        [TestMethod]
        public void ExecToken_StaysClearedWhenTokenIsInvalid()
        {
            using (var connectionInfo = new McConnectionInfo())
            using (var invalidToken = ToSecureString("client=invalid; secret=secret-value;"))
            {
                try
                {
                    connectionInfo.ExecToken = invalidToken;
                    Assert.Fail("An invalid execution token should be rejected.");
                }
                catch (ArgumentException)
                {
                }

                Assert.IsNull(connectionInfo.GetAuthToken().ClientId);
                Assert.IsTrue(connectionInfo.GetAuthToken().SecretKey == null);
                Assert.AreEqual(string.Empty, connectionInfo.TenantId);
                Assert.AreEqual(McConnectionInfo.AuthType.UsernamePassword, connectionInfo.MobileAuthType);
                Assert.IsNull(connectionInfo.ExecToken);
            }
        }

        [TestMethod]
        public void ExecToken_ClearingTokenAlsoClearsTenantId()
        {
            using (var connectionInfo = new McConnectionInfo())
            using (var execToken = ToSecureString("client=client-id; secret=secret-value; tenant=tenant-id;"))
            {
                connectionInfo.ExecToken = execToken;
                connectionInfo.ExecToken = null;

                Assert.IsNull(connectionInfo.ExecToken);
                Assert.AreEqual(string.Empty, connectionInfo.TenantId);
                Assert.AreEqual(McConnectionInfo.AuthType.UsernamePassword, connectionInfo.MobileAuthType);
                Assert.IsNull(connectionInfo.GetAuthToken().ClientId);
                Assert.IsTrue(connectionInfo.GetAuthToken().SecretKey == null);
            }
        }

        private static SecureString ToSecureString(string value)
        {
            var result = new SecureString();
            foreach (char character in value)
            {
                result.AppendChar(character);
            }
            result.MakeReadOnly();
            return result;
        }

        private static string ReadSecureString(SecureString value)
        {
            IntPtr bstr = IntPtr.Zero;
            try
            {
                bstr = Marshal.SecureStringToBSTR(value);
                return Marshal.PtrToStringBSTR(bstr);
            }
            finally
            {
                if (bstr != IntPtr.Zero)
                {
                    Marshal.ZeroFreeBSTR(bstr);
                }
            }
        }
    }
}
