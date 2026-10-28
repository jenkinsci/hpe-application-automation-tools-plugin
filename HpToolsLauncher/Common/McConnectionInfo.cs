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
using System.ComponentModel;
using System.Security;
using HpToolsLauncher.Properties;
using HpToolsLauncher.Utils;

namespace HpToolsLauncher
{
    public class McConnectionInfo : IDisposable
    {
        private const string EQ = "=";
        private const char EQ_CH = '=';
        private const char SEMI_COLON_CH = ';';
        private const char DBL_QUOTE_CH = '"';
        private const string YES = "Yes";
        private const string NO = "No";
        private const string SYSTEM = "System";
        private const string HTTP = "Http";
        private const string HTTPS = "Https";
        private const string PORT_8080 = "8080";
        private const string PORT_443 = "443";
        private const string CLIENT = "client";
        private const string SECRET = "secret";
        private const string TENANT = "tenant";
        private const string MASKED = "****";
        private const int ZERO = 0;
        private const int ONE = 1;
        private static readonly char[] SLASH = new char[] { '/' };
        private static readonly char[] COLON = new char[] { ':' };

        private const string MOBILEHOSTADDRESS = "MobileHostAddress";
        private const string MOBILEUSESSL = "MobileUseSSL";
        private const string MOBILEUSERNAME = "MobileUserName";
        private const string MOBILEPASSWORD = "MobilePassword";
        private const string MOBILEWORKSPACE = "MobileWorkspaceName";
        private const string MOBILEDEVICEMETRICS = "MobileDeviceMetrics";
        private const string MOBILEEXECTOKEN = "MobileExecToken";
        private const string DIGITALLABTYPE = "DigitalLabType";
        private const string MOBILEUSEPROXY = "MobileUseProxy";
        private const string MOBILEPROXYTYPE = "MobileProxyType";
        private const string MOBILEPROXYSETTING_ADDRESS = "MobileProxySetting_Address";
        private const string MOBILEPROXYSETTING_AUTHENTICATION = "MobileProxySetting_Authentication";
        private const string MOBILEPROXYSETTING_USERNAME = "MobileProxySetting_UserName";
        private const string MOBILEPROXYSETTING_PASSWORD = "MobileProxySetting_Password";

        // auth types for MC
        public enum AuthType
        {
            [Description("Username Password")]
            UsernamePassword,
            [Description("Access Key")]
            AuthToken
        }

        public enum DigitalLabType
        {
            UFT = 0,
            Lite = 1,
            ValueEdge = 2
        }

        public sealed class AuthTokenInfo : IDisposable
        {
            public string ClientId { get; set; }
            public SecureString SecretKey { get; set; }

            public void Dispose()
            {
                ClientId = null;
                if (SecretKey != null)
                {
                    SecretKey.Dispose();
                    SecretKey = null;
                }
            }
        }

        private bool _useSSL;
        private bool _useProxy;
        private bool _useProxyAuth;

        // if token auth was specified this is populated
        private AuthTokenInfo _token = new AuthTokenInfo();
        private SecureString _execToken;
        private AuthType _authType = AuthType.UsernamePassword;
        private DigitalLabType _labType = DigitalLabType.UFT;

        public string UserName { get; set; }
        public SecureString Password { get; set; }

        public SecureString ExecToken
        {
            get
            {
                return _execToken;
            }
            set
            {
                ClearExecTokenState();
                if (value == null)
                {
                    return;
                }

                var trimmed = value.Trim(DBL_QUOTE_CH);
                AuthTokenInfo parsed = null;
                try
                {
                    if (trimmed.Length == 0)
                    {
                        return;
                    }

                    string tenantId;
                    parsed = ParseExecToken(trimmed, out tenantId);

                    _execToken = trimmed;
                    _token = parsed;
                    TenantId = tenantId;
                    _authType = AuthType.AuthToken;

                    trimmed = null;
                    parsed = null;
                }
                finally
                {
                    if (parsed != null)
                    {
                        parsed.Dispose();
                    }
                    if (trimmed != null)
                    {
                        trimmed.Dispose();
                    }
                }
            }
        }

        private void ClearExecTokenState()
        {
            if (_execToken != null)
            {
                _execToken.Dispose();
                _execToken = null;
            }
            _token.Dispose();
            _token = new AuthTokenInfo();
            TenantId = string.Empty;
            _authType = AuthType.UsernamePassword;
        }

        public AuthType MobileAuthType
        {
            get
            {
                return _authType;
            }
            private set
            {
                _authType = value;
            }
        }

        public DigitalLabType LabType
        {
            get
            {
                return _labType;
            }
        }

        public string HostAddress { get; set; }
        public string HostPort { get; set; }
        public string TenantId { get; set; }
        public string WorkspaceName { get; set; }
        public string DeviceMetrics { get; set; }
        public bool UseSSL { get { return _useSSL; } set { _useSSL = value; } }
        public int UseSslAsInt { get { return _useSSL ? ONE : ZERO; } }
        public bool UseProxy { get { return _useProxy; } set { _useProxy = value; } }
        public int UseProxyAsInt { get { return _useProxy ? ONE : ZERO; } }
        public int ProxyType { get; set; }
        public string ProxyAddress { get; set; }
        public int ProxyPort { get; set; }
        public bool UseProxyAuth { get { return _useProxyAuth; } set { _useProxyAuth = value; } }
        public string ProxyUserName { get; set; }
        public SecureString ProxyPassword { get; set; }

        public McConnectionInfo()
        {
            HostPort = PORT_8080;
            UserName =
                HostAddress =
                TenantId =
                WorkspaceName =
                ProxyAddress =
                ProxyUserName = string.Empty;
        }

        public McConnectionInfo(JavaProperties ciParams) : this()
        {
            if (ciParams.ContainsKey(MOBILEHOSTADDRESS))
            {
                //ssl
                if (ciParams.ContainsKey(MOBILEUSESSL))
                {
                    string strUseSSL = ciParams[MOBILEUSESSL];
                    if (!strUseSSL.IsNullOrEmpty())
                    {
                        int intUseSSL;
                        int.TryParse(ciParams[MOBILEUSESSL], out intUseSSL);
                        _useSSL = intUseSSL == ONE;
                    }
                }

                string mcServerUrl = ciParams[MOBILEHOSTADDRESS].Trim();
                if (mcServerUrl.IsNullOrEmpty())
                {
                    throw new NoMcConnectionException();
                }
                //url is something like http://xxx.xxx.xxx.xxx:8080
                string[] arr = mcServerUrl.Split(COLON, StringSplitOptions.RemoveEmptyEntries);
                if (arr.Length == 1)
                {
                    if (arr[0].Trim().In(true, HTTP, HTTPS))
                        throw new ArgumentException(string.Format(Resources.McInvalidUrl, mcServerUrl));
                    HostAddress = arr[0].TrimEnd(SLASH);
                    HostPort = _useSSL ? PORT_443 : PORT_8080;
                }
                else if (arr.Length == 2)
                {
                    if (arr[0].Trim().In(true, HTTP, HTTPS))
                    {
                        HostAddress = arr[1].Trim(SLASH);
                        HostPort = _useSSL ? PORT_443 : PORT_8080;
                    }
                    else
                    {
                        HostAddress = arr[0].Trim(SLASH);
                        HostPort = arr[1].Trim();
                    }
                }
                else if (arr.Length == 3)
                {
                    HostAddress = arr[1].Trim(SLASH);
                    HostPort = arr[2].Trim();
                }

                if (HostAddress.Trim() == string.Empty)
                {
                    throw new ArgumentException(Resources.McEmptyHostAddress);
                }

                //mc username
                if (ciParams.ContainsKey(MOBILEUSERNAME))
                {
                    string mcUsername = ciParams[MOBILEUSERNAME];
                    if (!mcUsername.IsNullOrEmpty())
                    {
                        UserName = mcUsername;
                    }
                }

                //mc password
                if (ciParams.ContainsKey(MOBILEPASSWORD))
                {
                    string mcPassword = ciParams[MOBILEPASSWORD];
                    if (!mcPassword.IsNullOrEmpty())
                    {
                        Password = Encrypter.DecryptToSecureString(mcPassword);
                    }
                }

                //Digital Lab workspace
                if (ciParams.ContainsKey(MOBILEWORKSPACE))
                {
                    string mcWorkspaceName = ciParams[MOBILEWORKSPACE];
                    if (!mcWorkspaceName.IsNullOrEmpty())
                    {
                        WorkspaceName = mcWorkspaceName;
                    }
                }

                //Device Metrics
                {
                    string mcDeviceMetrics = ciParams[MOBILEDEVICEMETRICS];
                    if (!mcDeviceMetrics.IsNullOrEmpty())
                    {
                        DeviceMetrics = mcDeviceMetrics;
                    }
                }

                //mc exec token	
                if (ciParams.ContainsKey(MOBILEEXECTOKEN))
                {
                    var mcExecToken = ciParams[MOBILEEXECTOKEN];
                    if (!mcExecToken.IsNullOrEmpty())
                    {
                        // the setter keeps a trimmed copy, so the decrypted original is wiped right away
                        using (var token = Encrypter.DecryptToSecureString(mcExecToken))
                        {
                            ExecToken = token;
                        }
                    }
                }

                if (ciParams.ContainsKey(DIGITALLABTYPE))
                {
                    var dlLabType = ciParams[DIGITALLABTYPE];
                    if (!dlLabType.IsNullOrEmpty())
                    {
                        Enum.TryParse(dlLabType, true, out _labType);
                    }
                }

                //Proxy enabled flag
                if (ciParams.ContainsKey(MOBILEUSEPROXY))
                {
                    string useProxy = ciParams[MOBILEUSEPROXY];
                    if (!useProxy.IsNullOrEmpty())
                    {
                        int useProxyAsInt = int.Parse(useProxy);
                        _useProxy = useProxyAsInt == ONE;
                    }
                }

                //Proxy type
                if (ciParams.ContainsKey(MOBILEPROXYTYPE))
                {
                    string proxyType = ciParams[MOBILEPROXYTYPE];
                    if (!proxyType.IsNullOrEmpty())
                    {
                        ProxyType = int.Parse(proxyType);
                    }
                }

                //proxy address
                string proxyAddress = ciParams.GetOrDefault(MOBILEPROXYSETTING_ADDRESS);
                if (!proxyAddress.IsNullOrEmpty())
                {
                    // data is something like "16.105.9.23:8080"
                    string[] arrProxyAddress = proxyAddress.Split(new char[] { ':' });
                    if (arrProxyAddress.Length == 2)
                    {
                        ProxyAddress = arrProxyAddress[0];
                        ProxyPort = int.Parse(arrProxyAddress[1]);
                    }
                }


                //Proxy authentication
                if (ciParams.ContainsKey(MOBILEPROXYSETTING_AUTHENTICATION))
                {
                    string proxyAuth = ciParams[MOBILEPROXYSETTING_AUTHENTICATION];
                    if (!proxyAuth.IsNullOrEmpty())
                    {
                        int useProxyAuthAsInt = int.Parse(proxyAuth);
                        _useProxyAuth = useProxyAuthAsInt == ONE;
                    }
                }

                //Proxy username
                if (ciParams.ContainsKey(MOBILEPROXYSETTING_USERNAME))
                {
                    string proxyUsername = ciParams[MOBILEPROXYSETTING_USERNAME];
                    if (!proxyUsername.IsNullOrEmpty())
                    {
                        ProxyUserName = proxyUsername;
                    }
                }

                //Proxy password
                if (ciParams.ContainsKey(MOBILEPROXYSETTING_PASSWORD))
                {
                    string proxyPassword = ciParams[MOBILEPROXYSETTING_PASSWORD];
                    if (!proxyPassword.IsNullOrEmpty())
                    {
                        ProxyPassword = Encrypter.DecryptToSecureString(proxyPassword);
                    }
                }
            }
            else
            {
                throw new NoMcConnectionException();
            }
        }

        /// <summary>
        /// Parses the execution token and separates it into clientId, secretKey and tenantId.
        /// The token is parsed over a scratch buffer which is zeroed afterwards, so no fragment of the secret is left on the managed heap.
        /// </summary>
        /// <returns></returns>
        /// <exception cref="ArgumentException"></exception>
        private AuthTokenInfo ParseExecToken(SecureString execToken, out string tenantId)
        {
            tenantId = null;

            // exec token consists of three parts:
            // 1. client id
            // 2. secret key
            // 3. optionally tenant id
            // separator is ;
            // key-value pairs are separated with =

            // e.g., "client=oauth2-QHxvc8bOSz4lwgMqts2w@microfocus.com; secret=EHJp8ea6jnVNqoLN6HkD; tenant=999999999;"
            var ret = new AuthTokenInfo();
            if (execToken.Length == 0) return ret; // empty string was given as token, may signal that it wasn't specified

            string parsedTenantId = null;
            AuthTokenInfo parsed = execToken.UseAsCharArray(buf =>
            {
                try
                {
                    int pairCount = 0;
                    int pos = 0;
                    while (pos < buf.Length)
                    {
                        int end = Array.IndexOf(buf, SEMI_COLON_CH, pos);
                        if (end < 0) end = buf.Length;
                        if (end > pos)
                        {
                            pairCount++;
                            if (pairCount > 3)
                                throw new ArgumentException(Resources.McInvalidToken);

                            ParseExecTokenPart(buf, pos, end, ret, ref parsedTenantId);
                        }
                        pos = end + 1;
                    }

                    if (pairCount != 3) throw new ArgumentException(Resources.McInvalidToken);
                    return ret;
                }
                catch
                {
                    ret.Dispose();
                    throw;
                }
            });

            tenantId = parsedTenantId;
            return parsed;
        }

        private static void ParseExecTokenPart(char[] buf, int start, int end, AuthTokenInfo ret, ref string tenantId)
        {
            int eq = -1;
            for (int i = start; i < end; i++)
            {
                if (buf[i] != EQ_CH)
                    continue;

                if (eq >= 0)
                    throw new ArgumentException(Resources.McMalformedTokenMissingKeyValuePair);

                eq = i;
            }

            if (eq < 0)
                throw new ArgumentException(string.Format(Resources.McMalformedTokenInvalidKeyValueSeparator, EQ));

            int keyStart = start, keyEnd = eq;
            buf.TrimRange(ref keyStart, ref keyEnd, char.IsWhiteSpace);
            int valStart = eq + 1, valEnd = end;
            buf.TrimRange(ref valStart, ref valEnd, char.IsWhiteSpace);

            if (buf.EqualsIgnoreCase(keyStart, keyEnd - keyStart, CLIENT))
            {
                ret.ClientId = new string(buf, valStart, valEnd - valStart);
            }
            else if (buf.EqualsIgnoreCase(keyStart, keyEnd - keyStart, SECRET))
            {
                if (ret.SecretKey != null)
                {
                    ret.SecretKey.Dispose();
                }
                ret.SecretKey = buf.ToSecureString(valStart, valEnd - valStart);
            }
            else if (buf.EqualsIgnoreCase(keyStart, keyEnd - keyStart, TENANT))
            {
                tenantId = new string(buf, valStart, valEnd - valStart);
            }
            else
            {
                throw new ArgumentException(string.Format(Resources.McMalformedTokenInvalidKey, new string(buf, keyStart, keyEnd - keyStart)));
            }
        }

        /// <summary>
        /// Returns the parsed tokens from the execution token.
        /// </summary>
        /// <returns></returns>
        public AuthTokenInfo GetAuthToken()
        {
            return _token;
        }

        public override string ToString()
        {
            string strUseSsl = string.Format("UseSSL: {0}",  UseSslAsInt == ONE ? YES : NO);
            string strUserNameOrClientId = string.Empty;
            if (MobileAuthType == AuthType.AuthToken)
            {
                strUserNameOrClientId = string.Format("ClientId: {0}", _token.ClientId.IsNullOrEmpty() ? string.Empty : MASKED);
            }
            else if (MobileAuthType == AuthType.UsernamePassword)
            {
                strUserNameOrClientId = string.Format("Username: {0}", UserName);
            }
            string strTenantId = TenantId.IsNullOrWhiteSpace() ? string.Empty : string.Format(", TenantId: {0}", MASKED);
            string strProxy = string.Format("UseProxy: {0}", UseProxyAsInt == ONE ? YES : NO);
            if (UseProxy)
            {
                strProxy += string.Format(", ProxyType: {0}", ProxyType == ONE ? SYSTEM : HTTP);
                if (!ProxyAddress.IsNullOrWhiteSpace())
                {
                    strProxy += string.Format(", ProxyAddress: {0}", ProxyAddress);
                    if (ProxyPort > 0)
                    {
                        strProxy += string.Format(", ProxyPort: {0}", ProxyPort);
                    }
                }
                strProxy += string.Format(", ProxyAuth: {0}", _useProxyAuth ? YES : NO);
                if (_useProxy && !ProxyUserName.IsNullOrWhiteSpace())
                {
                    strProxy += string.Format(", ProxyUserName: {0}", MASKED);
                }
            }
            return string.Format("HostAddress: {0}, Port: {1}, AuthType: {2}, {3}{4}, {5}, {6}", HostAddress, HostPort, MobileAuthType, strUserNameOrClientId, strTenantId, strUseSsl, strProxy);
        }

        public void Dispose()
        {
            _token.Dispose();
            if (_execToken != null)
            {
                _execToken.Dispose();
                _execToken = null;
            }
            if (Password != null)
            {
                Password.Dispose();
                Password = null;
            }
            if (ProxyPassword != null)
            {
                ProxyPassword.Dispose();
                ProxyPassword = null;
            }
        }
    }

    public class NoMcConnectionException : Exception
    {
    }

    public class CloudBrowser
    {
        private const string EQ = "=";
        private const string SEMI_COLON = ";";
        private const string URL = "url";
        private const string TYPE = "type";
        private const string _OS = "os";
        private const string VERSION = "version";
        private const string REGION = "region";

        private static readonly char[] DBL_QUOTE = new char[] { '"' };

        private string _url;
        private string _os;
        private string _type;
        private string _version;
        private string _location;
        public CloudBrowser(string url, string os, string type, string version, string location)
        {
            _url = url;
            _os = os;
            _type = type;
            _version = version;
            _location = location;
        }
        public string Url { get { return _url; } }
        public string OS { get { return _os; } }
        public string Browser { get { return _type;} }
        public string Version { get { return _version; } }
        public string Region { get { return _location; } }

        public static bool TryParse(string strCloudBrowser, out CloudBrowser cloudBrowser)
        {
            cloudBrowser = null;
            try
            {
                string[] arrKeyValPairs = strCloudBrowser.Trim().Trim(DBL_QUOTE).Split(SEMI_COLON.ToCharArray(), StringSplitOptions.RemoveEmptyEntries);
                string url = null, os = null, type = null, version = null, region = null;

                // key-values are separated by =, we need its value, the key is known
                foreach (var pair in arrKeyValPairs)
                {
                    string[] arrKVP = pair.Split(EQ.ToCharArray(), 2);

                    if (arrKVP.Length < 2)
                        continue;

                    var key = arrKVP[0].Trim();
                    var value = arrKVP[1].Trim();
                    switch (key.ToLower())
                    {
                        case URL:
                            url = value; break;
                        case _OS:
                            os = value; break;
                        case TYPE:
                            type = value; break;
                        case VERSION:
                            version = value; break;
                        case REGION:
                            region = value; break;
                        default:
                            break;
                    }
                }
                cloudBrowser = new CloudBrowser(url, os, type, version, region);
                return true;
            }
            catch (Exception ex)
            {
                ConsoleWriter.WriteErrLine(ex.Message);
                return false;
            }
        }
    }

    public class DigitalLab
    {
        private McConnectionInfo _connInfo;
        private string _mobileInfo;
        private CloudBrowser _cloudBrowser;
        public DigitalLab(McConnectionInfo mcConnInfo, string mobileInfo, CloudBrowser cloudBrowser)
        {
            _connInfo = mcConnInfo;
            _mobileInfo = mobileInfo;
            _cloudBrowser = cloudBrowser;
        }
        public McConnectionInfo ConnectionInfo { get { return _connInfo; } }
        public string MobileInfo { get { return _mobileInfo; } }
        public CloudBrowser CloudBrowser { get { return _cloudBrowser; } }
    }
}
