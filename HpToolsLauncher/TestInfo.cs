using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Xml.Linq;
using System.Xml.Schema;

namespace HpToolsLauncher
{
    public class TestInfo
    {
        public TestInfo(string testPath)
        {
            TestPath = testPath;
        }
        public string GenerateAPITestXmlForTest()
        {
            Dictionary<string, TestParameterInfo> paramDict = new Dictionary<string, TestParameterInfo>();
            foreach (var param in ParameterList)
            {
                paramDict.Add(param.Name.ToLower(), param);
            }
            string paramXmlFileName = Path.Combine(TestPath, "TestInputParameters.xml");
            XDocument doc = XDocument.Load(paramXmlFileName);
            string schemaStr = doc.Descendants("Schema").First().Elements().First().ToString();
            XElement xArgs = doc.Descendants("Arguments").FirstOrDefault();
            if (xArgs!=null)
            foreach (XElement arg in xArgs.Elements())
            {
                string paramName = arg.Name.ToString().ToLower();
                if (paramDict.ContainsKey(paramName))
                {
                    var param = paramDict[paramName];
                    arg.Value = NormalizeParamValue(param);
                }
            }
            string argumentSectionStr = doc.Descendants("Values").First().Elements().First().ToString();
            try
            {
                XDocument doc1 = XDocument.Parse(argumentSectionStr);
                XmlSchema schema = XmlSchema.Read(new MemoryStream(Encoding.ASCII.GetBytes(schemaStr), false), null);
                
                XmlSchemaSet schemas = new XmlSchemaSet();
                schemas.Add(schema);

                string validationMessages = "";
                doc1.Validate(schemas, (o, e) =>
                {
                    validationMessages += e.Message + Environment.NewLine;
                });
                
                if (!string.IsNullOrWhiteSpace(validationMessages))
                    ConsoleWriter.WriteLine("parameter schema validation errors: \n" + validationMessages);
            }
            catch (Exception ex)
            {
                ConsoleWriter.WriteErrLine("An error occured while creating ST parameter file, check the validity of TestInputParameters.xml in your test directory and of your mtbx file");
            }
            return doc.ToString();
        }


        private string NormalizeParamValue(TestParameterInfo param)
        {
            switch (param.Type.ToLower())
            {
                case "datetime":
                case "date":
                    string retStr = "";
                    try
                    {
                        retStr = ((DateTime)param.ParseValue()).ToString("yyyy-MM-ddTHH:mm:ss");
                    }
                    catch
                    {
                        ConsoleWriter.WriteErrLine("incorrect dateTime value format in parameter: " + param.Name);
                    }
                    return retStr;
                default:
                    return param.Value;
            }
        }

        private string NormalizeParamType(string pType)
        {
            switch (pType.ToLower())
            {
                case "datetime":
                case "date":
                    return "dateTime";

                case "any":
                case "string":
                case "password":
                    return "string";

                case "int":
                case "integer":
                case "number":
                    return "integer";
                case "bool":
                case "boolean":
                    return "boolean";
                default:
                    return pType.ToLower();
            }
        }

        public TestInfo(string testPath, string testName)
        {
            TestPath = testPath;
            TestName = testName;
        }

        public TestInfo(string testPath, string testName, string testGroup)
        {
            _testPath = testPath;
            TestGroup = testGroup;
            _testName = testName;
        }


        List<TestParameterInfo> _paramList = new List<TestParameterInfo>();
        string _testName;
        string _testGroup;
        string _dataTablePath;

        public string TestGroup
        {
            get { return _testGroup; }
            set { _testGroup = value.TrimEnd("\\/".ToCharArray()).Replace(".", "_"); }
        }

        public string TestName
        {
            get { return _testName; }
            set { _testName = value; }
        }
        string _testPath;

        public string TestPath
        {
            get { return _testPath; }
            set { _testPath = value; }
        }
        public List<TestParameterInfo> ParameterList
        {
            get { return _paramList; }
            set { _paramList = value; }
        }

        public string DataTablePath
        {
            get { return _dataTablePath; }
            set { _dataTablePath = value; }
        }


        internal Dictionary<string, object> GetParameterDictionaryForQTP()
        {
            Dictionary<string, object> retval = new Dictionary<string, object>();
            foreach (var param in _paramList)
            {
                object val = param.ParseValue();
                retval.Add(param.Name, val);
            }
            return retval;
        }
    }
}
