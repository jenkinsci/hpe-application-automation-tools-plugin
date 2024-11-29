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
using System.IO;
using System.Security;
using System.Security.Cryptography;
using System.Text;

namespace HpToolsLauncher.Utils
{
    public static class Encrypter
    {
        private const string KeyPath = @"secrets/.hptoolslaunchersecret.key";
        private static readonly string SecretKey;
        private static readonly RSACryptoServiceProvider Rsa;

        static Encrypter()
        {
#if DEBUG
            return;
#else
            SecretKey = Environment.GetEnvironmentVariable("hptoolslauncher.key");
            var keyPath = Environment.GetEnvironmentVariable("hptoolslauncher.rootpath");

            if (string.IsNullOrEmpty(SecretKey) || string.IsNullOrEmpty(keyPath))
                throw new ArgumentException("Invalid environment, no secretkey or root path was set, aborting.");

            keyPath += Path.DirectorySeparatorChar + KeyPath;

            string cnt;
            try
            {
                cnt = File.ReadAllText(keyPath);
            }
            catch (IOException)
            {
                ConsoleWriter.WriteErrLine("Failed to open file with decryption key.");
                throw new ArgumentException(
                    "Check the secret key for the hptoolslauncher in the secrets directory or force a new key pair.");
            }
            catch (SecurityException)
            {
                ConsoleWriter.WriteErrLine("You do not have access to open the file with the decryption key.");
                throw new ArgumentException("Check the permissions for the secret key in the secrets directory.");
            }
            catch (UnauthorizedAccessException)
            {
                ConsoleWriter.WriteErrLine("You do not have access to open the file with the decryption key or something else has occurred.");
                throw new ArgumentException("Check the permissions for the secret key in the secrets directory or the existence of the file.");
            }

            var pkXml = DecryptWithPwd(cnt);
            try
            {
                Rsa = new RSACryptoServiceProvider();
                Rsa.FromXmlString(pkXml); // init
            }
            catch (CryptographicException)
            {
                ConsoleWriter.WriteErrLine("The cryptography provider could not be acquired.");
                throw new ArgumentException("Try forcing a new key pair.");
            }
            catch (ArgumentNullException)
            {
                ConsoleWriter.WriteErrLine("No valid private key were provided for cryptography.");
                throw new ArgumentException("Try forcing a new key pair generation.");
            }
#endif
        }

        /// <summary>
        /// Decrypts the data with the node's private key.
        /// </summary>
        /// <param name="textToDecrypt"></param>
        /// <returns></returns>
        public static string Decrypt(string textToDecrypt)
        {
#if DEBUG
            return textToDecrypt;
#else
            var encryptedBytes = Convert.FromBase64String(textToDecrypt);
            byte[] text;
            try
            {
                text = Rsa.Decrypt(encryptedBytes, false);
            }
            catch (CryptographicException)
            {  
                ConsoleWriter.WriteErrLine("Failed to decrypt data using private key, try forcing a new public-private key pair.");
                throw new ArgumentException("Decryption failed using private key.");
            }

            return Encoding.UTF8.GetString(text);
#endif
        }

        /// <summary>
        /// Internal usage only, used for private key decryption.
        /// </summary>
        /// <param name="textToDecrypt"></param>
        /// <returns></returns>
        private static string DecryptWithPwd(string textToDecrypt)
        {
#if DEBUG
            return textToDecrypt;
#else
            var rijndaelCipher = new RijndaelManaged
            {
                BlockSize = 0x80,
                KeySize = 0x100,
                Mode = CipherMode.CBC,
                Padding = PaddingMode.PKCS7
            };

            var encryptedData = Convert.FromBase64String(textToDecrypt);
            var pwdBytes = Encoding.UTF8.GetBytes(SecretKey);

            var ivBytes = new byte[0x10];
            Array.Copy(encryptedData, ivBytes, 16);
            var cntBytes = new byte[encryptedData.Length - 16];
            Array.Copy(encryptedData, 16, cntBytes, 0, cntBytes.Length);

            rijndaelCipher.Key = pwdBytes;
            rijndaelCipher.IV = ivBytes;

            byte[] plainText;
            try
            {
                plainText = rijndaelCipher.CreateDecryptor().TransformFinalBlock(cntBytes, 0, cntBytes.Length);
            }
            catch (CryptographicException)
            {
                ConsoleWriter.WriteErrLine("Failed to decrypt using AES, possibly master key have changed since the encryption.");
                throw new ArgumentException(
                    "Try forcing a new public-private key pair on this node, by deselecting encryption in Node configurations.");
            }

            return Encoding.UTF8.GetString(plainText);
#endif
        }
    }
}
