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

namespace HpToolsLauncher
{
    public class TestRunResults
    {
        public TestRunResults()
        {
            FatalErrors = -1;
        }

        private TestState m_enmTestState = TestState.Unknown;
        private TestState m_enmPrevTestState = TestState.Unknown;
        private bool m_hasWarnings = false;

        public bool HasWarnings
        {
            get { return m_hasWarnings; }
            set { m_hasWarnings = value; }
        }
     
        public string TestPath { get; set; }
        public string TestName { get; set; }
        public string TestGroup { get; set; }
        public string ErrorDesc { get; set; }
        public string FailureDesc { get; set; }
        public string ConsoleOut { get; set; }
        public string ConsoleErr { get; set; }
        public TimeSpan Runtime { get; set; }
        public string TestType { get; set; }
        public string ReportLocation { get; set; }
        public int FatalErrors { get; set; }
        public TestState TestState
        {
            get { return m_enmTestState; }
            set { m_enmTestState = value; }
        }

        public TestState PrevTestState
        {
            get { return m_enmPrevTestState; }
            set { m_enmPrevTestState = value; }
        }

        public int PrevRunId { get; set; }
    }
}
