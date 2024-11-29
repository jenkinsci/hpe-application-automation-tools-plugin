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
 using System;
using System.ComponentModel;
using System.Linq;
using HpToolsLauncher.Properties;
using HpToolsLauncher.Utils;

namespace HpToolsLauncher
{
    public class McConnectionInfo
    {
        private const string EQ = "=";
        private const string SEMI_COLON = ";";
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
        private const int ZERO = 0;
        private const int ONE = 1;
        private static readonly char[] SLASH = new char[] { '/' };
        private static readonly char[] COLON = new char[] { ':' };
        private static readonly char[] DBL_QUOTE = new char[] { '"' };

        private const string MOBILEHOSTADDRESS = "MobileHostAddress";
        private const string MOBILEUSESSL = "MobileUseSSL";
        private const string MOBILEUSERNAME = "MobileUserName";
        private const string MOBILEPASSWORD = "MobilePassword";
        private const string MOBILETENANTID = "MobileTenantId";
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

        public struct AuthTokenInfo
        {
            public string ClientId { get; set; }
            public string SecretKey { get; set; }
        }

        private bool _useSSL;
        private bool _useProxy;
        private bool _useProxyAuth;

        // if token auth was specified this is populated
        private AuthTokenInfo _token;
        private string _execToken;
        private AuthType _authType = AuthType.UsernamePassword;
        private DigitalLabType _labType = DigitalLabType.UFT;

        public string UserName { get; set; }
        public string Password { get; set; }

        public string ExecToken
        {
            get
            {
                return _execToken;
            }
            set
            {
                if (value == null)
                {
                    _execToken = null;
                    _token.ClientId = _token.SecretKey = null;
                }
                else
                {
                    _execToken = value.Trim().Trim(DBL_QUOTE);
                    _token = ParseExecToken();
                }
            }
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
        public bool UseSSL { get { return _useSSL; } set { _useSSL = value; } }
        public int UseSslAsInt { get { return _useSSL ? ONE : ZERO; } }
        public bool UseProxy { get { return _useProxy; } set { _useProxy = value; } }
        public int UseProxyAsInt { get { return _useProxy ? ONE : ZERO; } }
        public int ProxyType { get; set; }
        public string ProxyAddress { get; set; }
        public int ProxyPort { get; set; }
        public bool UseProxyAuth { get { return _useProxyAuth; } set { _useProxyAuth = value; } }
        public string ProxyUserName { get; set; }
        public string ProxyPassword { get; set; }

        public McConnectionInfo()
        {
            HostPort = PORT_8080;
            UserName =
                ExecToken =
                Password =
                HostAddress =
                TenantId =
                ProxyAddress =
                ProxyUserName =
                ProxyPassword = string.Empty;
        }

        public McConnectionInfo(JavaProperties ciParams) : this()
        {
            if (ciParams.ContainsKey(MOBILEHOSTADDRESS))
            {
                //ssl
                if (ciParams.ContainsKey(MOBILEUSESSL))
                {
                    string strUseSSL = ciParams[MOBILEUSESSL];
                    if (!string.IsNullOrEmpty(strUseSSL))
                    {
                        int intUseSSL;
                        int.TryParse(ciParams[MOBILEUSESSL], out intUseSSL);
                        _useSSL = intUseSSL == ONE;
                    }
                }

                string mcServerUrl = ciParams[MOBILEHOSTADDRESS].Trim();
                if (string.IsNullOrEmpty(mcServerUrl))
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
                    if (!string.IsNullOrEmpty(mcUsername))
                    {
                        UserName = mcUsername;
                    }
                }

                //mc password
                if (ciParams.ContainsKey(MOBILEPASSWORD))
                {
                    string mcPassword = ciParams[MOBILEPASSWORD];
                    if (!string.IsNullOrEmpty(mcPassword))
                    {
                        Password = Encrypter.Decrypt(mcPassword);
                    }
                }

                //mc tenantId
                if (ciParams.ContainsKey(MOBILETENANTID))
                {
                    string mcTenantId = ciParams[MOBILETENANTID];
                    if (!string.IsNullOrEmpty(mcTenantId))
                    {
                        TenantId = mcTenantId;
                    }
                }

                //mc exec token	
                if (ciParams.ContainsKey(MOBILEEXECTOKEN))
                {
                    var mcExecToken = ciParams[MOBILEEXECTOKEN];
                    if (!string.IsNullOrEmpty(mcExecToken))
                    {
                        ExecToken = Encrypter.Decrypt(mcExecToken);
                    }
                }

                if (ciParams.ContainsKey(DIGITALLABTYPE))
                {
                    var dlLabType = ciParams[DIGITALLABTYPE];
                    if (!string.IsNullOrEmpty(dlLabType))
                    {
                        Enum.TryParse(dlLabType, true, out _labType);
                    }
                }

                //Proxy enabled flag
                if (ciParams.ContainsKey(MOBILEUSEPROXY))
                {
                    string useProxy = ciParams[MOBILEUSEPROXY];
                    if (!string.IsNullOrEmpty(useProxy))
                    {
                        int useProxyAsInt = int.Parse(useProxy);
                        _useProxy = useProxyAsInt == ONE;
                    }
                }

                //Proxy type
                if (ciParams.ContainsKey(MOBILEPROXYTYPE))
                {
                    string proxyType = ciParams[MOBILEPROXYTYPE];
                    if (!string.IsNullOrEmpty(proxyType))
                    {
                        ProxyType = int.Parse(proxyType);
                    }
                }

                //proxy address
                string proxyAddress = ciParams.GetOrDefault(MOBILEPROXYSETTING_ADDRESS);
                if (!string.IsNullOrEmpty(proxyAddress))
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
                    if (!string.IsNullOrEmpty(proxyAuth))
                    {
                        int useProxyAuthAsInt = int.Parse(proxyAuth);
                        _useProxyAuth = useProxyAuthAsInt == ONE;
                    }
                }

                //Proxy username
                if (ciParams.ContainsKey(MOBILEPROXYSETTING_USERNAME))
                {
                    string proxyUsername = ciParams[MOBILEPROXYSETTING_USERNAME];
                    if (!string.IsNullOrEmpty(proxyUsername))
                    {
                        ProxyUserName = proxyUsername;
                    }
                }

                //Proxy password
                if (ciParams.ContainsKey(MOBILEPROXYSETTING_PASSWORD))
                {
                    string proxyPassword = ciParams[MOBILEPROXYSETTING_PASSWORD];
                    if (!string.IsNullOrEmpty(proxyPassword))
                    {
                        ProxyPassword = Encrypter.Decrypt(proxyPassword);
                    }
                }
            }
            else
            {
                throw new NoMcConnectionException();
            }
        }

        /// <summary>
        /// Parses the execution token and separates into three parts: clientId, secretKey and tenantId
        /// </summary>
        /// <returns></returns>
        /// <exception cref="ArgumentException"></exception>
        private AuthTokenInfo ParseExecToken()
        {
            // exec token consists of three parts:
            // 1. client id
            // 2. secret key
            // 3. optionally tenant id
            // separator is ;
            // key-value pairs are separated with =

            // e.g., "client=oauth2-QHxvc8bOSz4lwgMqts2w@microfocus.com; secret=EHJp8ea6jnVNqoLN6HkD; tenant=999999999;"
            // "client=oauth2-OuV8k3snnGp9vJugC1Zn@microfocus.com; secret=6XSquF1FUD4CyQM7fb0B; tenant=999999999;"
            // "client=oauth2-OuV8k3snnGp9vJugC1Zn@microfocus.com; secret=6XSquF1FUD7CyQM7fb0B; tenant=999999999;"
            var ret = new AuthTokenInfo();
            if (_execToken.Length == 0) return ret; // empty string was given as token, may semnalize that it wasn't specified

            var tokens = _execToken.Split(SEMI_COLON.ToCharArray(), StringSplitOptions.RemoveEmptyEntries);

            if (tokens.Length != 3) throw new ArgumentException(Resources.McInvalidToken);
            if (!tokens.All(token => token.Contains(EQ)))
                throw new ArgumentException(string.Format(Resources.McMalformedTokenInvalidKeyValueSeparator, EQ));

            // key-values are separated by =, we need its value, the key is known
            foreach (var token in tokens)
            {
                var parts = token.Split(EQ.ToCharArray());

                if (parts.Length != 2)
                    throw new ArgumentException(Resources.McMalformedTokenMissingKeyValuePair);

                var key = parts[0].Trim();
                var value = parts[1].Trim();

                if (CLIENT.EqualsIgnoreCase(key))
                {
                    ret.ClientId = value;
                }
                else if (SECRET.EqualsIgnoreCase(key))
                {
                    ret.SecretKey = value;
                }
                else if (TENANT.EqualsIgnoreCase(key))
                {
                    TenantId = value;
                }
                else
                {
                    throw new ArgumentException(string.Format(Resources.McMalformedTokenInvalidKey, key));
                }
            }

            _authType = AuthType.AuthToken;
            return ret;
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
                strUserNameOrClientId = string.Format("ClientId: {0}", _token.ClientId);
            }
            else if (MobileAuthType == AuthType.UsernamePassword)
            {
                strUserNameOrClientId = string.Format("Username: {0}", UserName);
            }
            string strTenantId = TenantId.IsNullOrWhiteSpace() ? string.Empty : string.Format(", TenantId: {0}", TenantId);
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
                    strProxy += string.Format(", ProxyUserName: {0}", ProxyUserName);
                }
            }
            return string.Format("HostAddress: {0}, Port: {1}, AuthType: {2}, {3}{4}, {5}, {6}", HostAddress, HostPort, MobileAuthType, strUserNameOrClientId, strTenantId, strUseSsl, strProxy);
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
