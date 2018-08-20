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
using System.Runtime.InteropServices;

namespace HpToolsLauncher
{
    public static class WinUserNativeMethods
    {
        private static class EncodeUtilsWrap
        {
            [DllImport("EncodeUtilsWrap", CallingConvention = CallingConvention.Cdecl)]
            public static extern void UnprotectBSTRFromBase64([MarshalAs(UnmanagedType.BStr)] string input, [MarshalAs(UnmanagedType.BStr)] out string result, [MarshalAs(UnmanagedType.Bool)] bool bCrypt);

            [DllImport("EncodeUtilsWrap", CallingConvention = CallingConvention.Cdecl)]
            public static extern void ProtectBSTRToBase64([MarshalAs(UnmanagedType.BStr)] string input, [MarshalAs(UnmanagedType.BStr)] out string result, [MarshalAs(UnmanagedType.Bool)] bool bCrypt);
        }

        private static class EncodeUtilsWrapD
        {
            [DllImport("EncodeUtilsWrapD", CallingConvention = CallingConvention.Cdecl)]
            public static extern void UnprotectBSTRFromBase64([MarshalAs(UnmanagedType.BStr)] string input, [MarshalAs(UnmanagedType.BStr)] out string result, [MarshalAs(UnmanagedType.Bool)] bool bCrypt);

            [DllImport("EncodeUtilsWrapD", CallingConvention = CallingConvention.Cdecl)]
            public static extern void ProtectBSTRToBase64([MarshalAs(UnmanagedType.BStr)] string input, [MarshalAs(UnmanagedType.BStr)] out string result, [MarshalAs(UnmanagedType.Bool)] bool bCrypt);
        }

        public static string ProtectBSTRToBase64(string clearData)
        {
            string result = null;

            try
            {
                EncodeUtilsWrap.ProtectBSTRToBase64(clearData, out result, true);
            }
            catch (DllNotFoundException)
            {
                try
                {
                    EncodeUtilsWrapD.ProtectBSTRToBase64(clearData, out result, true);
                }
                catch (DllNotFoundException)
                {
                    
                }
            }

            return result;
        }

        public static string UnprotectBSTRFromBase64(string protectedData)
        {
            if (string.IsNullOrEmpty(protectedData))
                return string.Empty;

            string result = null;

            try
            {
                EncodeUtilsWrap.UnprotectBSTRFromBase64(protectedData, out result, true);
            }
            catch (DllNotFoundException)
            {
                EncodeUtilsWrapD.UnprotectBSTRFromBase64(protectedData, out result,true);
            }

            return result;
        }
    }
}
