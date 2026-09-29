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
using System.Runtime.InteropServices;
using System.Security;
using System.Text;
using System.Linq;
using System.ComponentModel;

namespace HpToolsLauncher.Utils
{
    internal static class Extensions
    {
        public static SecureString ToSecureString(this string plainString)
        {
            if (plainString == null)
                return null;

            SecureString secureString = new SecureString();
            foreach (char c in plainString.ToCharArray())
            {
                secureString.AppendChar(c);
            }
            return secureString;
        }

        /// <summary>
        /// Copies the secret into a char array, the caller is responsible for zeroing it once done with it.
        /// </summary>
        public static char[] ToCharArray(this SecureString secret)
        {
            char[] chars = new char[secret.Length];
            IntPtr bstr = IntPtr.Zero;
            try
            {
                bstr = Marshal.SecureStringToBSTR(secret);
                for (int i = 0; i < chars.Length; i++)
                {
                    chars[i] = (char)Marshal.ReadInt16(bstr, i * sizeof(char));
                }
            }
            finally
            {
                if (bstr != IntPtr.Zero)
                {
                    Marshal.ZeroFreeBSTR(bstr);
                }
            }
            return chars;
        }

        public static SecureString ToSecureString(this char[] chars, int start, int length)
        {
            SecureString secureString = new SecureString();
            for (int i = 0; i < length; i++)
            {
                secureString.AppendChar(chars[start + i]);
            }
            secureString.MakeReadOnly();
            return secureString;
        }

        /// <summary>
        /// Narrows the range to the part of the buffer which is not trimmable at either end.
        /// </summary>
        public static void TrimRange(this char[] buf, ref int start, ref int end, Func<char, bool> isTrimmable)
        {
            while (start < end && isTrimmable(buf[start])) start++;
            while (end > start && isTrimmable(buf[end - 1])) end--;
        }

        /// <summary>
        /// Strips the surrounding whitespace, then the given characters, without materializing the secret as a managed string.
        /// </summary>
        public static SecureString Trim(this SecureString secret, params char[] trimChars)
        {
            char[] buf = secret.ToCharArray();
            try
            {
                int start = 0, end = buf.Length;
                buf.TrimRange(ref start, ref end, char.IsWhiteSpace);
                if (trimChars != null && trimChars.Length > 0)
                {
                    buf.TrimRange(ref start, ref end, c => Array.IndexOf(trimChars, c) >= 0);
                }
                return buf.ToSecureString(start, end - start);
            }
            finally
            {
                Array.Clear(buf, 0, buf.Length);
            }
        }

        /// <summary>
        /// Hands the secret over as a plain string, for the APIs which cannot accept anything else.
        /// The buffer is pinned before it is filled and zeroed on the way out, so the collector cannot leave a readable copy behind on the heap.
        /// </summary>
        public static T UseAsPlainText<T>(this SecureString secret, Func<string, T> func)
        {
            if (secret == null) return func(null);
            if (secret.Length == 0) return func(string.Empty);

            int len = secret.Length;
            string plain = new string('\0', len);
            GCHandle pin = GCHandle.Alloc(plain, GCHandleType.Pinned);
            IntPtr buffer = pin.AddrOfPinnedObject();
            char[] chars = null;
            try
            {
                chars = secret.ToCharArray();
                for (int i = 0; i < len; i++)
                {
                    Marshal.WriteInt16(buffer, i * sizeof(char), chars[i]);
                }
                return func(plain);
            }
            finally
            {
                if (chars != null)
                {
                    Array.Clear(chars, 0, chars.Length);
                }
                for (int i = 0; i < len; i++)
                {
                    Marshal.WriteInt16(buffer, i * sizeof(char), 0);
                }
                pin.Free();
            }
        }

        public static void UseAsPlainText(this SecureString secret, Action<string> action)
        {
            secret.UseAsPlainText<object>(plain => { action(plain); return null; });
        }

        /// <summary>
        /// Base64 encodes the UTF-8 representation of the secret without ever materializing it as a managed string.
        /// </summary>
        public static string ToBase64(this SecureString secret)
        {
            if (secret.IsNullOrEmpty()) return string.Empty;

            char[] chars = secret.ToCharArray();
            byte[] bytes = null;
            try
            {
                bytes = Encoding.UTF8.GetBytes(chars);
                return Convert.ToBase64String(bytes);
            }
            finally
            {
                Array.Clear(chars, 0, chars.Length);
                if (bytes != null)
                {
                    Array.Clear(bytes, 0, bytes.Length);
                }
            }
        }

        public static bool IsNullOrEmpty(this SecureString value)
        {
            return value == null || value.Length == 0;
        }
        public static bool IsNullOrEmpty(this string value)
        {
            return string.IsNullOrEmpty(value);
        }
        public static bool IsNullOrWhiteSpace(this string value)
        {
            return string.IsNullOrWhiteSpace(value);
        }

        public static bool IsEmptyOrWhiteSpace(this string str)
        {
            return str != null && str.Trim() == string.Empty;
        }

        public static bool IsValidUrl(this string url)
        {
            return Uri.IsWellFormedUriString(url, UriKind.RelativeOrAbsolute);
        }

        public static bool EqualsIgnoreCase(this string s1, string s2)
        {
            return (s1 == null || s2 == null) ? (s1 == s2) : s1.Equals(s2, StringComparison.OrdinalIgnoreCase);
        }

        /// <summary>
        /// Compares a range of the buffer against the given value, without extracting the range into a string first.
        /// </summary>
        public static bool EqualsIgnoreCase(this char[] buf, int start, int length, string value)
        {
            if (buf == null || value == null || length != value.Length) return false;

            for (int i = 0; i < length; i++)
            {
                if (char.ToLowerInvariant(buf[start + i]) != char.ToLowerInvariant(value[i])) return false;
            }
            return true;
        }

        public static bool In(this string str, bool ignoreCase, params string[] values)
        {
            if (ignoreCase)
            {
                return values != null && values.Any((string s) => EqualsIgnoreCase(str, s));
            }
            return In(str, values);
        }

        public static bool In<T>(this T obj, params T[] values)
        {
            return values != null && values.Any((T o) => Equals(obj, o));
        }

        public static string GetEnumDescription(this Enum enumValue)
        {
            var fieldInfo = enumValue.GetType().GetField(enumValue.ToString());
            var descrAttrs = (DescriptionAttribute[])fieldInfo.GetCustomAttributes(typeof(DescriptionAttribute), false);
            return descrAttrs.Length > 0 ? descrAttrs[0].Description : enumValue.ToString();
        }
    }
}
