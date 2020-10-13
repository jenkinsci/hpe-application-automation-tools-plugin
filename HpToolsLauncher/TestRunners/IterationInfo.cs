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
 * © Copyright 2012-2019 Micro Focus or one of its affiliates..
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

namespace HpToolsLauncher.TestRunners
{
    public class IterationInfo
    {
        public const string RANGE_ITERATION_MODE = "rngIterations";
        public const string ONE_ITERATION_MODE = "oneIteration";
        public const string ALL_ITERATION_MODE = "rngAll";
        public static ISet<String> AvailableTypes = new HashSet<String>() { RANGE_ITERATION_MODE, ONE_ITERATION_MODE, ALL_ITERATION_MODE };

        public string IterationMode { get; set; }

        public string StartIteration { get; set; }

        public string EndIteration { get; set; }
    }
}
