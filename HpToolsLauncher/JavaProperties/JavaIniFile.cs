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
using System.Linq;
using System.Text;
using System.IO;
using System.Text.RegularExpressions;

namespace HpToolsLauncher
{
    public class JavaIniFile : Dictionary<string, JavaProperties>
    {
        Regex reg = new Regex(@"\[(?<section>.*?)\]", RegexOptions.Compiled);
        public void Load(string filename)
        {
            this.Clear();
            //Dictionary
            StringBuilder sb = new StringBuilder();
            //Dictionary<string, JavaProperties> retVal = new Dictionary<string, JavaProperties>();

            string[] fileLines = File.ReadAllLines(filename, Encoding.Default);
            int i = 0;
            string currentSectionName = null;
            foreach (string line in fileLines)
            {
                Match match = reg.Match(line);
                if (match.Success)
                {
                    //get the new section name
                    string sectName = match.Groups["section"].Value;
                    if (currentSectionName != null)
                    {
                        //load the previous section to a properties object
                        JavaProperties props = new JavaProperties();
                        using (MemoryStream s = new MemoryStream(Encoding.Default.GetBytes(sb.ToString())))
                        {
                            props.Load(s);
                        }

                        //add to the dictionary
                        this.Add(currentSectionName, props);
                    }
                    //the current section becomes the new section
                    currentSectionName = sectName;
                    //clear the old section from the builder
                    sb.Clear();
                }
                else
                {
                    sb.AppendLine(line);
                }
                ++i;
            }

            if (currentSectionName != null)
            {
                //load the last section to a properties object
                JavaProperties props1 = new JavaProperties();
                using (MemoryStream s1 = new MemoryStream(Encoding.Default.GetBytes(sb.ToString())))
                {
                    props1.Load(s1);
                }

                //add to the dictionary
                this.Add(currentSectionName, props1);
            }
        }

        public void Save(string fileName)
        {
            StringBuilder sb = new StringBuilder();
            foreach (string sect in this.Keys)
            {
                sb.AppendLine("[" + sect + "]");
                JavaProperties props = this[sect];
                using (MemoryStream s = new MemoryStream())
                {
                    props.Save(s, null);
                    sb.AppendLine(s.ToString());
                }
            }
            File.WriteAllText(fileName,sb.ToString());
        }
    }
}
