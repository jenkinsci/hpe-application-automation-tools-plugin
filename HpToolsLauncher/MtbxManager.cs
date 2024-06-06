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

using HpToolsLauncher.Properties;
using HpToolsLauncher.TestRunners;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Xml.Linq;
using System.Xml.Schema;

namespace HpToolsLauncher
{
    public class MtbxManager
    {

        //the xml format of an mtbx file below:
        /*
         <Mtbx>
            <Test Name="test1" path="${workspace}\test1">
                <Parameter Name="mee" Value="12" Type="Integer"/>
                <Parameter Name="mee1" Value="12.0" Type="Double"/>
                <Parameter Name="mee2" Value="abc" Type="String"/>
                <Parameter name="ParamBoolean" type="boolean" value="False"/>
                <DataTable path="c:\tables\my_data_table.xls"/>
                <Iterations mode="rngIterations|rngAll|oneIteration" start="2" end="3"/>
            </Test>
            <Test Name="test2" path="${workspace}\test2">
                <Parameter Name="mee" Value="12" Type="Integer"/>
                <Parameter Name="mee1" Value="12.0" Type="Double"/>
                <Parameter Name="mee2" Value="abc" Type="String"/>
                <Parameter name="ParamBoolean" type="boolean" value="False"/>
            </Test>
         </Mtbx>
        */
        public static List<TestInfo> LoadMtbx(string mtbxContent, string testGroup)
        {
            return LoadMtbx(mtbxContent, null, testGroup);
        }

        public static List<TestInfo> Parse(string mtbxFileName)
        {
            string xmlContent = File.ReadAllText(mtbxFileName);
            return Parse(xmlContent, null, mtbxFileName);
        }

        private static XAttribute GetAttribute(XElement x, XName attributeName)
        {
            return x.Attributes().FirstOrDefault(a => a.Name.Namespace == attributeName.Namespace
            && string.Equals(a.Name.LocalName, attributeName.LocalName, StringComparison.OrdinalIgnoreCase));
        }

        private static XElement GetElement(XElement x, XName eName)
        {
            return x.Elements().FirstOrDefault(a => a.Name.Namespace == eName.Namespace
             && string.Equals(a.Name.LocalName, eName.LocalName, StringComparison.OrdinalIgnoreCase));
        }

        private static IEnumerable<XElement> GetElements(XElement x, XName eName)
        {
            return x.Elements().Where(a => a.Name.Namespace == eName.Namespace
             && string.Equals(a.Name.LocalName, eName.LocalName, StringComparison.OrdinalIgnoreCase));
        }

        public static List<TestInfo> Parse(string mtbxFileName, Dictionary<string, string> jenkinsEnvVars, string testGroupName)
        {
            return LoadMtbx(File.ReadAllText(mtbxFileName), jenkinsEnvVars, testGroupName);
        }
        private static string ReplaceString(string str, string oldValue, string newValue, StringComparison comparison)
        {
            StringBuilder sb = new StringBuilder();

            int previousIndex = 0;
            int index = str.IndexOf(oldValue, comparison);
            while (index != -1)
            {
                sb.Append(str.Substring(previousIndex, index - previousIndex));
                sb.Append(newValue);
                index += oldValue.Length;

                previousIndex = index;
                index = str.IndexOf(oldValue, index, comparison);
            }
            sb.Append(str.Substring(previousIndex));

            return sb.ToString();
        }

        public static List<TestInfo> LoadMtbx(string xmlContent, Dictionary<string, string> jankinsEnvVars, string testGroupName)
        {
            var localEnv = Environment.GetEnvironmentVariables();

            foreach (string varName in localEnv.Keys)
            {
                string value = (string)localEnv[varName];
                xmlContent = ReplaceString(xmlContent, "%" + varName + "%", value, StringComparison.OrdinalIgnoreCase);
                xmlContent = ReplaceString(xmlContent, "${" + varName + "}", value, StringComparison.OrdinalIgnoreCase);
            }

            if (jankinsEnvVars != null)
            {
                foreach (string varName in jankinsEnvVars.Keys)
                {
                    string value = jankinsEnvVars[varName];
                    xmlContent = ReplaceString(xmlContent, "%" + varName + "%", value, StringComparison.OrdinalIgnoreCase);
                    xmlContent = ReplaceString(xmlContent, "${" + varName + "}", value, StringComparison.OrdinalIgnoreCase);
                }
            }

            List<TestInfo> retval = new List<TestInfo>();
            XDocument doc = XDocument.Parse(xmlContent);

            XmlSchemaSet schemas = new XmlSchemaSet();

            var assembly = Assembly.GetExecutingAssembly();

            var schemaStream = assembly.GetManifestResourceStream("HpToolsLauncher.MtbxSchema.xsd");

            XmlSchema schema = XmlSchema.Read(schemaStream, null);

            schemas.Add(schema);

            string validationMessages = "";
            doc.Validate(schemas, (o, e) =>
            {
                validationMessages += e.Message + Environment.NewLine;
            });

            if (!string.IsNullOrWhiteSpace(validationMessages))
                ConsoleWriter.WriteLine("mtbx schema validation errors: " + validationMessages);
            try
            {
                var root = doc.Root;
                foreach (var test in GetElements(root, "Test"))
                {
                    string path = GetAttribute(test, "path").Value;

                    if (!Directory.Exists(path))
                    {
                        string line = string.Format(Resources.GeneralFileNotFound, path);
                        ConsoleWriter.WriteLine(line);
                        ConsoleWriter.ErrorSummaryLines.Add(line);
                        Launcher.ExitCode = Launcher.ExitCodeEnum.Failed;
                        continue;
                    }

                    XAttribute xname = GetAttribute(test, "name");
                    string name = "Unnamed Test";
                    if (xname != null && xname.Value != "")
                    {
                        name = xname.Value;
                    }

                    // optional report path attribute
                    XAttribute xReportPath = GetAttribute(test, "reportPath");
                    string reportPath = null;

                    if (xReportPath != null)
                    {
                        reportPath = xReportPath.Value;
                    }

                    TestInfo testInfo = new TestInfo(path, name, testGroupName)
                    {
                        ReportPath = reportPath
                    };

                    HashSet<string> paramNames = new HashSet<string>();

                    foreach (var param in GetElements(test, "Parameter"))
                    {
                        string pname = GetAttribute(param, "name").Value;
                        string pval = GetAttribute(param, "value").Value;
                        XAttribute attrType = GetAttribute(param, "type");
                        XAttribute attrSource = GetAttribute(param, "source");
                        string ptype = "string";
                        string source = null;

                        if (attrType != null)
                            ptype = attrType.Value;
                        if (attrSource != null)
                            source = attrSource.Value;

                        var testParam = new TestParameterInfo() { Name = pname, Type = ptype, Value = pval, Source = source };
                        if (!paramNames.Contains(testParam.Name))
                        {
                            paramNames.Add(testParam.Name);
                            testInfo.Params.Add(testParam);
                        }
                        else
                        {
                            string line = string.Format(Resources.GeneralDuplicateParameterWarning, pname, path);
                            ConsoleWriter.WriteLine(line);
                        }
                    }

                    XElement dataTable = GetElement(test, "DataTable");
                    if (dataTable != null)
                    {
                        testInfo.DataTablePath = GetAttribute(dataTable, "path").Value;
                    }

                    XElement iterations = GetElement(test, "Iterations");
                    if (iterations != null)
                    {
                        IterationInfo ii = new IterationInfo();
                        XAttribute modeAttr = GetAttribute(iterations, "mode");
                        if (modeAttr != null)
                        {
                            ii.IterationMode = modeAttr.Value;
                        }
                        XAttribute startAttr = GetAttribute(iterations, "start");
                        if (startAttr != null)
                        {
                            ii.StartIteration = startAttr.Value;
                        }
                        XAttribute endAttr = GetAttribute(iterations, "end");
                        if (endAttr != null)
                        {
                            ii.EndIteration = endAttr.Value;
                        }

                        testInfo.IterationInfo = ii;
                    }

                    retval.Add(testInfo);
                }
            }
            catch (Exception ex)
            {
                ConsoleWriter.WriteException("Problem while parsing Mtbx file", ex);
            }
            return retval;
        }
    }
}
