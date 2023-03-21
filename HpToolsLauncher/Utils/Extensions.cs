using System.Runtime.InteropServices;
using System;
using System.Security;
using System.Linq;

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
        public static string ToPlainString(this SecureString value)
        {
            IntPtr valuePtr = IntPtr.Zero;
            try
            {
                valuePtr = Marshal.SecureStringToBSTR(value);
                return Marshal.PtrToStringBSTR(valuePtr);
            }
            finally
            {
                Marshal.ZeroFreeBSTR(valuePtr);
            }
        }
        public static bool EqualsIgnoreCase(this string s1, string s2)
        {
            if (s1 == null || s2 == null) return s1 == s2;
            return s1.Equals(s2, StringComparison.OrdinalIgnoreCase);
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
    }
}
