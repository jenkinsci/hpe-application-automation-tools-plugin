/*
 * Certain versions of software accessible here may contain branding from Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 * This software was acquired by Micro Focus on September 1, 2017, and is now offered by OpenText.
 * Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright 2012-2024 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ___________________________________________________________________
 */

using System;
using System.Collections.Generic;
using System.Text.RegularExpressions;

namespace HpToolsLauncher.RTS
{
    /// <summary>
    /// Class responsible for building the runtime settings xml which is sent to controller to update runtime settings
    /// It can modify an existing xml string by editing or adding properties (under the form key-value) to a given section
    /// At the moment it is being used to modify additional attributes
    /// </summary>
    public class RTSHelper
    {
        public struct KeyValuePair
        {
            public string key;
            public string value;

            public KeyValuePair(string key, string value)
            {
                this.key = key;
                this.value = value;
            }
        }

        private const string END_OF_LINE = "\\r\\n";
        public const string COMMAND_ARGUMENTS = "CommandArguments";

        private string m_iniFileText;
        private int m_startSectionIndex;
        private int m_endSectionIndex;
        private bool m_sectionExists;
        private string m_sectionName;
        private List<KeyValuePair> m_inputKeyValuePairs;
        private List<KeyValuePair> m_keyValuePairs;

        /**
         * Set the begging and the end index of a section
         */
        private void SetSectionIndexes()
        {
            Regex regex = new Regex("\\[.*?\\]");
            MatchCollection matches = regex.Matches(m_iniFileText);

            bool sectionFound = false;
            foreach (Match match in matches)
            {
                if (sectionFound == false)
                {
                    if (match.Value.Equals(m_sectionName))
                    {
                        m_startSectionIndex = match.Index;
                        sectionFound = true;
                    }
                }
                else
                {
                    m_endSectionIndex = match.Index;
                    break;
                }
            }

            if (m_startSectionIndex != -1)
            {
                m_startSectionIndex += m_sectionName.Length;
            }
            else
            {
                CreateSection();
            }
        }

        /**
         * Create the section if it doesn't exist 
         */
        private void CreateSection()
        {
            m_iniFileText = m_iniFileText.Insert(m_endSectionIndex, m_sectionName);

            m_startSectionIndex = m_endSectionIndex + m_sectionName.Length;
            m_endSectionIndex = m_iniFileText.Length;
            m_sectionExists = false;
        }

        /**
         * Set initial key-value pairs for the section
         */
        private void SetKeyValuePairs()
        {
            if (!m_sectionExists)
            {
                return;
            }
            string partial = m_iniFileText.Substring(m_startSectionIndex, m_endSectionIndex - m_startSectionIndex);
            string[] keyValuePairs = partial.Split(new string[] { END_OF_LINE }, StringSplitOptions.RemoveEmptyEntries);

            foreach (string keyValuePair in keyValuePairs)
            {
                var results = keyValuePair.Split('=');
                if (results.Length > 1)
                {
                    m_keyValuePairs.Add(new KeyValuePair(results[0], results[1]));
                }
            }
        }

        /**
         * Reconstruct the section with updates or new key-value pairs
         */
        private void ConstructUpdatedSection()
        {
            string sectionText = "";
            foreach (KeyValuePair keyValuePair in m_keyValuePairs)
            {
                sectionText += END_OF_LINE + keyValuePair.key + "=" + keyValuePair.value;
            }
            sectionText += END_OF_LINE;

            UpdateIniFileText(sectionText);
        }

        /**
         * Updates the ini file text with the reconstructed section
         */
        private void UpdateIniFileText(string sectionText)
        {
            m_iniFileText = m_iniFileText.Remove(m_startSectionIndex, m_endSectionIndex - m_startSectionIndex).Insert(m_startSectionIndex, sectionText);
        }

        public RTSHelper(string iniFileText, string sectionName, List<KeyValuePair> inputKeyValuePairs)
        {
            m_startSectionIndex = -1;
            m_iniFileText = iniFileText;
            m_endSectionIndex = m_iniFileText.Length;
            m_keyValuePairs = new List<KeyValuePair>();
            m_sectionExists = true;

            SetSectionName(sectionName);
            SetInputKeyValuePairs(inputKeyValuePairs);

            SetSectionIndexes();
            SetKeyValuePairs();

            foreach (KeyValuePair keyValuePair in m_inputKeyValuePairs)
            {
                UpdateKeyValuePair(keyValuePair);
            }

            ConstructUpdatedSection();
        }

        private void SetSectionName(string sectionName)
        {
            m_sectionName = "[" + sectionName + "]";
        }

        private void SetInputKeyValuePairs(List<KeyValuePair> inputKeyValuePairs)
        {
            m_inputKeyValuePairs = inputKeyValuePairs;
        }

        /**
         * Updates the value of the key if it exists
         * Creates the key otherwise
         */
        private void UpdateKeyValuePair(KeyValuePair keyValuePair)
        {
            //Check if key exists
            bool keyExists = false;
            for (int i = 0; i < m_keyValuePairs.Count && m_sectionExists; i++)
            {
                if (m_keyValuePairs[i].key.Equals(keyValuePair.key))
                {
                    m_keyValuePairs[i] = new KeyValuePair(m_keyValuePairs[i].key, keyValuePair.value);
                    keyExists = true;
                    break;
                }
            }
            //Key doesn't exist, create new entry
            if (!keyExists)
            {
                m_keyValuePairs.Add(new KeyValuePair(keyValuePair.key, keyValuePair.value));
            }
        }

        public string GetUpdatedIniFileText()
        {
            return m_iniFileText;
        }
    }
}
