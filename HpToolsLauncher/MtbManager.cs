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
using System.Collections.Generic;
using System.IO;
using System.Linq;
using HpToolsLauncher.Properties;
//using UFT.Runner.Properties;

namespace HpToolsLauncher
{
    public class MtbManager 
    {
        public delegate IEnumerable<string> GetContentDelegate();

        private string _mtbName = Resources.DefaultName;
        private string _mtbFileName;

        private const string DefaultFileExt = ".mtb";
        private const string MtbSectionKey = "Files";
        private const string MtbNumFilesKey = "NumberOfFiles";

        private static List<string> GetPathsFromFile(string fileName)
        {
            var paths = new List<string>();
            try
            {
                var iniManager = new IniManager(fileName);
                var count = Convert.ToInt32(iniManager.ReadValue(MtbSectionKey, MtbNumFilesKey));
                for (int i = 1; i <= count; i++)
                {
                    var key = string.Format("File{0}", i);
                    var path = iniManager.ReadValue(MtbSectionKey, key);

                    string testFileName = path.Split(';')[0];

                    if (!Directory.Exists(testFileName))
                    {
                        string line = string.Format(Resources.GeneralFileNotFound, testFileName);
                        ConsoleWriter.WriteLine(line);
                        ConsoleWriter.ErrorSummaryLines.Add(line);
                        Launcher.ExitCode = Launcher.ExitCodeEnum.Failed;
                    }
                    else
                    {
                        paths.Add(testFileName);
                    }
                }
            }
            //The given file is not a valid mtb file
            catch { }

            return paths;
        }

        #region IMtbManager implementation

        public void New()
        {
            _mtbName = Resources.DefaultName;
            _mtbFileName = string.Empty;
            IsDirty = false;
        }

        public List<string> Open()
        {
            return GetPathsFromFile(_mtbFileName);
        }

        /// <summary>
        /// Save the contents to a file in the file system
        /// </summary>
        /// <param name="paths"></param>
        public void Save(IEnumerable<string> paths)
        {
            var count = paths.Count();
            var iniManager = new IniManager(_mtbFileName);
            iniManager.WriteValue(MtbSectionKey, MtbNumFilesKey, count.ToString());

            int i = 1;
            foreach (var path in paths)
            {
                var key = string.Format("File{0}", i++);
                var value = string.Format("{0};1", path);
                iniManager.WriteValue(MtbSectionKey, key, value);
            }
        }

        public List<string> Parse(string fileName)
        {
            return GetPathsFromFile(fileName);
        }

        public void InitializeContext(string mtbName, string mtbFileName)
        {
            _mtbName = mtbName;
            _mtbFileName = mtbFileName;
        }

        public string MtbName
        {
            get { return _mtbName; }
        }

        public string MtbFileName
        {
            get { return _mtbFileName; }
        }

        public string DefaultFileExtension
        {
            get { return DefaultFileExt; }
        }

        public bool IsDirty { get; set; }


        #endregion

    }
}
