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

namespace HpToolsLauncher
{
    enum ExportTypeSuffixEnum
    {
        ExportTypeSuffix_SD, //step details
        ExportTypeSuffix_DT, //data table
        ExportTypeSuffix_SM, //system monitor
        ExportTypeSuffix_SR, //screen recorder
        ExportTypeSuffix_LT  //log tracking
    }

    public class ExportOptions
    {
        public const string ExportDataTable = "ExportDataTable";
        public const string ExportForFailed = "ExportForFailed";
        public const string ExportLogTracking = "ExportLogTracking";
        public const string ExportScreenRecorder = "ExportScreenRecorder";
        public const string ExportStepDetails = "ExportStepDetails";
        public const string ExportSystemMonitor = "ExportSystemMonitor";
        public const string ExportLocation = "ExportLocation";
        public const string XslPath = "XSLPath";
        public const string ExportFormat = "ExportFormat";
        public const string ExportType = "ExportType";

    }
}