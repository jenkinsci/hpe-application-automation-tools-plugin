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
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Text;

namespace HpToolsLauncher
{
    /// <summary>
    /// The .mtb file format originally used in QTP batch tool
    /// has the .INI format. For backward compatibility, we maintain the exact format.
    /// This class calls some methods from Kernel32 to read and write .INI file
    /// </summary>
    public class IniManager
    {
        private string _IniPath;

        [DllImport("kernel32")]
        private static extern long WritePrivateProfileString(string section,
          string key, string val, string filePath);

        [DllImport("kernel32")]
        private static extern int GetPrivateProfileString(string section,
          string key, string def, StringBuilder retVal,
          int size, string filePath);

        [DllImport("kernel32")]
        static extern int GetPrivateProfileString(string Section, int Key,
               string Value, [MarshalAs(UnmanagedType.LPArray)] byte[] Result,
               int Size, string FileName);

        [DllImport("kernel32")]
        static extern int GetPrivateProfileString(int Section, string Key,
               string Value, [MarshalAs(UnmanagedType.LPArray)] byte[] Result,
               int Size, string FileName);

        public IniManager(string iniIniPath)
        {
            _IniPath = iniIniPath;
        }

        public void WriteValue(string section, string key, string value)
        {
            WritePrivateProfileString(section, key, value, _IniPath);
        }

        public string ReadValue(string section, string key)
        {
            var buffer = new StringBuilder(255);
            GetPrivateProfileString(section, key, "", buffer, 255, _IniPath);
            return buffer.ToString();
        }

        /// <summary>
        /// The Function called to obtain the SectionHeaders, and returns them in an Dynamic Array.
        /// </summary>
        /// <returns></returns>
        public HashSet<string> GetSectionNames()
        {
            //    Sets the maxsize buffer to 500, if the more
            //    is required then doubles the size each time.
            for (int maxsize = 500; true; maxsize *= 2)
            {
                //    Obtains the information in bytes and stores
                //    them in the maxsize buffer (Bytes array)
                byte[] bytes = new byte[maxsize];
                int size = GetPrivateProfileString(0, "", "", bytes, maxsize, _IniPath);

                // Check the information obtained is not bigger
                // than the allocated maxsize buffer - 2 bytes.
                // if it is, then skip over the next section
                // so that the maxsize buffer can be doubled.
                if (size < maxsize - 2)
                {
                    // Converts the bytes value into an UTF8 char. This is one long string.
                    string Selected = Encoding.UTF8.GetString(bytes, 0,
                                               size - (size > 0 ? 1 : 0));
                    // Splits the Long string into an array based on the "\0"
                    // or null (Newline) value and returns the value(s) in an array
                    string[] sections = Selected.Split(new char[] { '\0' });
                    HashSet<string> sects = new HashSet<string>(sections);
                    return sects;
                }
            }
        }

        /// <summary>
        /// The Function called to obtain the EntryKey's from the given SectionHeader string passed and returns them in an Dynamic Array
        /// </summary>
        /// <param name="section"></param>
        /// <returns></returns>
        public HashSet<string> GetEntryNames(string section)
        {
            //    Sets the maxsize buffer to 500, if the more
            //    is required then doubles the size each time. 
            for (int maxsize = 500; true; maxsize *= 2)
            {
                //    Obtains the EntryKey information in bytes
                //    and stores them in the maxsize buffer (Bytes array).
                //    Note that the SectionHeader value has been passed.
                byte[] bytes = new byte[maxsize];
                int size = GetPrivateProfileString(section, 0, "", bytes, maxsize, _IniPath);

                // Check the information obtained is not bigger
                // than the allocated maxsize buffer - 2 bytes.
                // if it is, then skip over the next section
                // so that the maxsize buffer can be doubled.
                if (size < maxsize - 2)
                {
                    // Converts the bytes value into an UTF8 char.
                    // This is one long string.
                    string entries = Encoding.Default.GetString(bytes, 0,
                                              size - (size > 0 ? 1 : 0));
                    // Splits the Long string into an array based on the "\0"
                    // or null (Newline) value and returns the value(s) in an array
                    string[] ents1 = entries.Split(new char[] { '\0' });
                    HashSet<string> hashEnts = new HashSet<string>(ents1);
                    return hashEnts;
                }
            }
        }

        public Dictionary<string, string> GetSectionAsDictionary(string section)
        {
            Dictionary<string, string> retval = new Dictionary<string, string>();
            HashSet<string> entries = GetEntryNames(section);
            foreach (string ent in entries)
            {
                retval.Add(ent, ReadValue(section, ent));
            }
            return retval;
        }

        public static Dictionary<string, Dictionary<string, string>> LoadIniFileAsDictionary(string strFileName)
        {
            Dictionary<string, Dictionary<string, string>> retVal = new Dictionary<string, Dictionary<string, string>>();
            IniManager man = new IniManager(strFileName);
            foreach (string sect in man.GetSectionNames())
            {
                retVal.Add(sect, man.GetSectionAsDictionary(sect));
            }
            return retVal;
        }
    }
}
