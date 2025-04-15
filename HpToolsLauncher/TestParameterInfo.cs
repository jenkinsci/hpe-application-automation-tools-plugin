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

namespace HpToolsLauncher
{
    public class TestParameterInfo
    {
        string _name;

        public string Name
        {
            get { return _name; }
            set { _name = value; }
        }
        string _value;

        public string Value
        {
            get { return _value; }
            set { _value = value; }
        }
        string _type;

        public string Type
        {
            get { return _type; }
            set { _type = value; }
        }

        public string Source { get; set; }


        /// <summary>
        /// parses the value string and returns an object of the specified type.
        /// </summary>
        /// <returns></returns>
        public object ParseValue()
        {
            object val = null;
            bool ok = false;
            switch (_type.ToLower())
            {
                case "int":
                    int v;
                    ok = int.TryParse(_value, out v);
                    if (ok)
                    {
                        val = v;
                    }
                    break;
                case "number":
                case "password":
                case "string":
                case "any":
                    val = _value;
                    break;
                case "float":
                    float v1;
                    ok = float.TryParse(_value, out v1);
                    if (ok)
                    {
                        val = v1;
                    }

                    break;
                case "double":
                    double v2;
                    ok = double.TryParse(_value, out v2);
                    if (ok)
                    {
                        val = v2;
                    }
                    break;
                case "datetime":
                case "date":
                    DateTime v3;
                    ok = DateTime.TryParseExact(_value,
                        new string[] {
                                            "yyyy-MM-ddTHH:mm:ss",
                                            "dd/MM/yyyy HH:mm:ss.fff",
                                            "dd/M/yyyy HH:mm:ss.fff",
                                            "d/MM/yyyy HH:mm:ss.fff",
                                            "dd/MM/yyyy hh:mm:ss.fff tt" ,
                                            "d/MM/yyyy hh:mm:ss.fff tt" ,
                                            "dd/M/yyyy hh:mm:ss.fff tt" ,
                                            "d/M/yyyy hh:mm:ss.fff tt" ,
                                            "dd-MM-yyyy HH:mm:ss.fff",
                                            "dd.MM.yyyy HH:mm:ss.fff",
                                            "dd.MM.yyyy hh:mm:ss.fff tt" ,
                                            "dd/MM/yyyy HH:mm:ss",
                                            "dd-MM-yyyy HH:mm:ss",
                                            "dd/MM/yyyy hh:mm:ss tt",
                                            "dd/M/yyyy hh:mm:ss tt",
                                            "d/M/yyyy hh:mm:ss tt",
                                            "d/MM/yyyy HH:mm:ss",
                                            "d/MM/yyyy hh:mm:ss tt",
                                            "d-MM-yyyy HH:mm:ss",
                                            "d.MM.yyyy HH:mm:ss",
                                            "d.MM.yyyy hh:mm:ss tt" ,
                                            "dd/MM/yyyy",
                                            "dd-MM-yyyy",
                                            "dd.MM.yyyy",
                                            "d/MM/yyyy" ,
                                            "d-MM-yyyy" ,
                                            "d.MM.yyyy" ,
                                            "M/d/yyyy HH:mm:ss.fff",
                                            "M.d.yyyy hh:mm:ss.fff tt",
                                            "M.d.yyyy hh:mm:ss tt",
                                            "M.d.yyyy HH:mm:ss.fff",
                                            "M/d/yyyy hh:mm:ss.fff t",
                                            "MM/dd/yyyy hh:mm:ss.fff tt",
                                            "MM/d/yyyy hh:mm:ss.fff tt",
                                            "MM/dd/yyyy hh:mm:ss tt",
                                            "MM/d/yyyy hh:mm:ss tt",
                                            "MM/dd/yyyy HH:mm:ss",
                                            "MM.dd.yyyy HH:mm:ss",
                                            "M/dd/yyyy HH:mm:ss.fff",
                                            "M/dd/yyyy hh:mm:ss.fff tt",
                                            "M/dd/yyyy hh:mm:ss tt",
                                            "M.dd.yyyy HH:mm:ss.fff",
                                            "M.dd.yyyy hh:mm:ss.fff tt",
                                            "M.dd.yyyy hh:mm:ss tt",
                                            "MM/dd/yyyy",
                                            "MM.dd.yyyy",
                                            "M/dd/yyyy",
                                            "M.dd.yyyy"
                                            },
                        null,
                        System.Globalization.DateTimeStyles.None,
                        out v3);

                    //ok = DateTime.TryParse(param.Value, out v3);
                    if (ok)
                    {
                        val = v3;
                    }
                    break;

                case "long":
                    long v4;
                    ok = long.TryParse(_value, out v4);
                    if (ok)
                    {
                        val = v4;
                    }
                    break;
                case "bool":
                case "boolean":
                    bool v5;
                    ok = bool.TryParse(_value, out v5);
                    if (ok)
                    {
                        val = v5;
                    }
                    break;
                case "decimal":
                    decimal v6;
                    ok = decimal.TryParse(_value, out v6);
                    if (ok)
                    {
                        val = v6;
                    }
                    break;
            }
            return val;
        }
    }
}
