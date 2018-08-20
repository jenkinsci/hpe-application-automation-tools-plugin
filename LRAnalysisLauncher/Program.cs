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
using System.Xml;
using Analysis.Api;
using Analysis.ApiLib.Sla;
using LRAnalysisLauncher.Properties;
using HpToolsLauncher;
using Analysis.ApiSL;
using Analysis.Api.Dictionaries;
using System.Diagnostics;
using System.Globalization;
using System.Linq;

namespace LRAnalysisLauncher
{
    class Program
    {

        static Program()
        {
            AppDomain.CurrentDomain.AssemblyResolve += new ResolveEventHandler(CurrentDomain_AssemblyResolve);
        }

        static void log()
        {
            log("");
        }
        static void log(string msg)
        {
            Console.WriteLine(msg);
            //  writer.WriteLine(msg);
        }
        //        static StreamWriter writer = new StreamWriter(new FileStream("c:\\AnalysisLauncherOutput.txt", FileMode.OpenOrCreate, FileAccess.Write));
        //args: lrr location, lra location, html report location
        [STAThread]
        static int Main(string[] args)
        {
            HpToolsLauncher.ConsoleQuickEdit.Disable();
            Console.OutputEncoding = System.Text.Encoding.GetEncoding("utf-8");
            log("starting analysis launcher");
            int iPassed = (int)Launcher.ExitCodeEnum.Passed;//variable to keep track of whether all of the SLAs passed
            try
            {
                //The app uses 3 default arguments, a 4th optional one can be used to specify the path to an analysis template
                if (args.Length != 3 && args.Length != 4)
                {
                    ShowHelp();
                    return (int)Launcher.ExitCodeEnum.Aborted;
                }

                string lrrlocation = args[0];
                string lralocation = args[1];
                string htmlLocation = args[2];
                string analysisTemplateLocation = (args.Length == 4 ? args[3] : "");

                log("creating analysis COM object");
                LrAnalysis analysis = new LrAnalysis();

                Session session = analysis.Session;
                log("creating analysis session");
                //Apply a template and create LRA folder
                if (session.CreateWithTemplateFile(lralocation, lrrlocation, analysisTemplateLocation))
                {
                    log("analysis session created");
                    log("creating HTML reports");
                    HtmlReportMaker reportMaker = session.CreateHtmlReportMaker();
                    reportMaker.AddGraph("Connections");
                    reportMaker.AddGraph("ConnectionsPerSecond");
                    reportMaker.CreateDefaultHtmlReport(
                        Path.Combine(Path.GetDirectoryName(htmlLocation), "IE", Path.GetFileName(htmlLocation)),
                        ApiBrowserType.IE);
                    reportMaker.CreateDefaultHtmlReport(
                        Path.Combine(Path.GetDirectoryName(htmlLocation), "Netscape", Path.GetFileName(htmlLocation)),
                        ApiBrowserType.Netscape);
                    log("HTML reports created");

                    XmlDocument xmlDoc = new XmlDocument();

                    log("loading errors, if any");
                    session.ErrorMessages.LoadValuesIfNeeded();
                    if (session.ErrorMessages.Count != 0)
                    {
                        log("error count: " + session.ErrorMessages.Count);
                        if (session.ErrorMessages.Count > 1000)
                        {
                            log("more then 1000 error during scenario run, analyzing only the first 1000.");
                        }
                        log(Resources.ErrorsReportTitle);
                        XmlElement errorRoot = xmlDoc.CreateElement("Errors");
                        xmlDoc.AppendChild(errorRoot);
                        int limit = 1000;
                        ErrorMessage[] errors = session.ErrorMessages.ToArray();
                        //foreach (ErrorMessage err in session.ErrorMessages)
                        for (int i = 0; i < limit && i < errors.Length; i++)
                        {
                            ErrorMessage err = errors[i];
                            XmlElement elem = xmlDoc.CreateElement("Error");
                            elem.SetAttribute("ID", err.ID.ToString());
                            elem.AppendChild(xmlDoc.CreateTextNode(err.Name));
                            log("ID: " + err.ID + " Name: " + err.Name);
                            errorRoot.AppendChild(elem);
                        }
                        xmlDoc.Save(Path.Combine(Path.GetDirectoryName(Path.GetDirectoryName(lrrlocation)), "Errors.xml"));

                        xmlDoc.RemoveAll();
                        log("");
                    }
                    XmlDocument runReprotDoc = new XmlDocument();
                    log("Gathering Run statistics");
                    XmlElement runsRoot = runReprotDoc.CreateElement("Runs");
                    runReprotDoc.AppendChild(runsRoot);

                    XmlElement general = runReprotDoc.CreateElement("General");
                    runsRoot.AppendChild(general);

                    XmlElement durationElement = runReprotDoc.CreateElement("Time");
                    durationElement.SetAttribute("End", "-1");
                    durationElement.SetAttribute("Start", "-1");
                    durationElement.SetAttribute("Duration", "-1");

                    Stopper stopper = new Stopper(10000);
                    stopper.Start();
                    //foreach (Run currentRun in analysis.Session.Runs)
                    //{
                    //    stopper.Start();
                    //    log("Gathering Duration statistics");
                    //    stopper.Start();
                    //DateTime startTime = Helper.FromUnixTime(currentRun.StartTime);
                    //DateTime endTime = Helper.FromUnixTime(currentRun.EndTime);
                    //durationElement.SetAttribute("End", endTime.ToString());
                    //durationElement.SetAttribute("Start", startTime.ToString());
                    //durationElement.SetAttribute("Duration", Helper.GetScenarioDuration(currentRun));
                    //}
                    general.AppendChild(durationElement);

                    XmlElement vUsers = runReprotDoc.CreateElement("VUsers");
                    //log("Adding VUser statistics");
                    Dictionary<string, int> vuserCountDictionary = new Dictionary<string, int>(4)
                    {
                        {"Passed", 0},
                        {"Stopped", 0},
                        {"Failed", 0},
                        {"Error", 0}
                    };
                    //vuserCountDictionary = Helper.GetVusersCountByStatus(analysis);
                    foreach (KeyValuePair<string, int> kvp in vuserCountDictionary)
                    {
                        //log(msg: String.Format("{0} vUsers: {1}", kvp.Key, kvp.Value));
                        vUsers.SetAttribute(kvp.Key, kvp.Value.ToString());
                    }
                    vUsers.SetAttribute("Count", session.VUsers.Count.ToString());
                    general.AppendChild(vUsers);

                    XmlElement transactions = runReprotDoc.CreateElement("Transactions");
                    Dictionary<string, double> transactionSumStatusDictionary = new Dictionary<string, double>()
                    {
                        {"Count", 0},
                        {"Pass", 0},
                        {"Fail", 0},
                        {"Stop", 0}
                    };
                    Dictionary<string, Dictionary<string, double>> transactionDictionary =
                        Helper.CalcFailedTransPercent(analysis);
                    foreach (KeyValuePair<string, Dictionary<string, double>> kvp in transactionDictionary)
                    {
                        XmlElement transaction = runReprotDoc.CreateElement("Transaction");
                        foreach (var transStatus in kvp.Value)
                        {
                            transaction.SetAttribute(transStatus.Key, transStatus.Value.ToString());
                            transactionSumStatusDictionary[transStatus.Key] += transStatus.Value;
                            transactionSumStatusDictionary["Count"] += transStatus.Value;
                        }
                        transaction.SetAttribute("Name", kvp.Key);
                        transactions.AppendChild(transaction);
                    }
                    foreach (var transStatus in transactionSumStatusDictionary)
                    {
                        transactions.SetAttribute(transStatus.Key, transStatus.Value.ToString());
                        //log(msg: String.Format("{0} transaction: {1}", transStatus.Key, transStatus.Value));
                    }
                    general.AppendChild(transactions);

                    string connectionsMaximum = "0";
                    //connectionsMaximum = Helper.GetConnectionsCount(analysis).ToString();
                    XmlElement connections = runReprotDoc.CreateElement("Connections");
                    connections.SetAttribute("MaxCount", connectionsMaximum);
                    general.AppendChild(connections);


                    log("");
                    log("closing session");
                    session.Close();
                    log(Resources.SLAReportTitle);
                    log("calculating SLA");
                    SlaResult slaResult = Session.CalculateSla(lralocation, true);
                    log("SLA calculation done");
                    XmlElement root = xmlDoc.CreateElement("SLA");
                    xmlDoc.AppendChild(root);

                    int iCounter = 0; // set counter
                    log("WholeRunRules : " + slaResult.WholeRunRules.Count);
                    CultureInfo formatProvider = new CultureInfo("en-US");
                    foreach (SlaWholeRunRuleResult a in slaResult.WholeRunRules)
                    {
                        log(Resources.DoubleLineSeperator);
                        XmlElement elem;
                        if (a.Measurement.Equals(SlaMeasurement.PercentileTRT))
                        {
                            SlaPercentileRuleResult b = slaResult.TransactionRules.PercentileRules[iCounter];
                            elem = xmlDoc.CreateElement("SLA_GOAL"); //no white space in the element name
                            log("Transaction Name : " + b.TransactionName);
                            elem.SetAttribute("TransactionName", b.TransactionName.ToString());
                            log("Percentile : " + b.Percentage);
                            elem.SetAttribute("Percentile", b.Percentage.ToString(formatProvider));
                            elem.SetAttribute("FullName", b.RuleUiName);
                            log("Full Name : " + b.RuleUiName);
                            log("Measurement : " + b.Measurement);
                            elem.SetAttribute("Measurement", b.Measurement.ToString());
                            log("Goal Value : " + b.GoalValue);
                            elem.SetAttribute("GoalValue", b.GoalValue.ToString(formatProvider));
                            log("Actual value : " + b.ActualValue);
                            elem.SetAttribute("ActualValue", b.ActualValue.ToString(formatProvider));
                            log("status : " + b.Status);
                            elem.AppendChild(xmlDoc.CreateTextNode(b.Status.ToString()));

                            if (b.Status.Equals(SlaRuleStatus.Failed)) // 0 = failed
                            {
                                iPassed = (int)Launcher.ExitCodeEnum.Failed;
                            }
                            iCounter++;
                        }
                        else
                        {
                            elem = xmlDoc.CreateElement("SLA_GOAL"); //no white space in the element name
                            elem.SetAttribute("FullName", a.RuleUiName);
                            log("Full Name : " + a.RuleUiName);
                            log("Measurement : " + a.Measurement);
                            elem.SetAttribute("Measurement", a.Measurement.ToString());
                            log("Goal Value : " + a.GoalValue);
                            elem.SetAttribute("GoalValue", a.GoalValue.ToString(formatProvider));
                            log("Actual value : " + a.ActualValue);
                            elem.SetAttribute("ActualValue", a.ActualValue.ToString(formatProvider));
                            log("status : " + a.Status);
                            elem.AppendChild(xmlDoc.CreateTextNode(a.Status.ToString()));

                            if (a.Status.Equals(SlaRuleStatus.Failed)) // 0 = failed
                            {
                                iPassed = (int)Launcher.ExitCodeEnum.Failed;
                            }
                        }
                        root.AppendChild(elem);
                        log(Resources.DoubleLineSeperator);
                    }

                    iCounter = 0; // reset counter
                    log("TimeRangeRules : " + slaResult.TimeRangeRules.Count);
                    foreach (SlaTimeRangeRuleResult a in slaResult.TimeRangeRules)
                    {

                        log(Resources.DoubleLineSeperator);
                        XmlElement rule;
                        if (a.Measurement.Equals(SlaMeasurement.AverageTRT))
                        {
                            SlaTransactionTimeRangeRuleResult b = slaResult.TransactionRules.TimeRangeRules[iCounter];
                            rule = xmlDoc.CreateElement("SLA_GOAL"); //no white space in the element name
                            log("Transaction Name: " + b.TransactionName);
                            rule.SetAttribute("TransactionName", b.TransactionName);
                            log("Full Name : " + b.RuleUiName);
                            rule.SetAttribute("FullName", b.RuleUiName);
                            log("Measurement : " + b.Measurement);
                            rule.SetAttribute("Measurement", b.Measurement.ToString());
                            log("SLA Load Threshold Value : " + b.CriteriaMeasurement);
                            rule.SetAttribute("SLALoadThresholdValue", b.CriteriaMeasurement.ToString());
                            log("LoadThresholds : " + b.LoadThresholds.Count);
                            foreach (SlaLoadThreshold slat in b.LoadThresholds)
                            {
                                XmlElement loadThr = xmlDoc.CreateElement("SlaLoadThreshold");
                                loadThr.SetAttribute("StartLoadValue", slat.StartLoadValue.ToString(formatProvider));
                                loadThr.SetAttribute("EndLoadValue", slat.EndLoadValue.ToString(formatProvider));
                                loadThr.SetAttribute("ThresholdValue", slat.ThresholdValue.ToString(formatProvider));
                                rule.AppendChild(loadThr);

                            }
                            XmlElement timeRanges = xmlDoc.CreateElement("TimeRanges");
                            log("TimeRanges : " + b.TimeRanges.Count);
                            int passed = 0;
                            int failed = 0;
                            int noData = 0;
                            foreach (SlaTimeRangeInfo slatri in b.TimeRanges)
                            {
                                XmlElement subsubelem = xmlDoc.CreateElement("TimeRangeInfo");
                                subsubelem.SetAttribute("StartTime", slatri.StartTime.ToString());
                                subsubelem.SetAttribute("EndTime", slatri.EndTime.ToString());
                                subsubelem.SetAttribute("GoalValue", slatri.GoalValue.ToString(formatProvider));
                                subsubelem.SetAttribute("ActualValue", slatri.ActualValue.ToString(formatProvider));
                                subsubelem.SetAttribute("LoadValue", slatri.LoadValue.ToString(formatProvider));
                                subsubelem.InnerText = slatri.Status.ToString();
                                switch (slatri.Status)
                                {
                                    case SlaRuleStatus.Failed:
                                        failed++;
                                        break;
                                    case SlaRuleStatus.Passed:
                                        passed++;
                                        break;
                                    case SlaRuleStatus.NoData:
                                        noData++;
                                        break;
                                    default:
                                        break;
                                }
                                timeRanges.AppendChild(subsubelem);
                            }
                            rule.AppendChild(timeRanges);
                            SlaRuleStatus currentRuleStatus = b.Status;
                            if (currentRuleStatus.Equals(SlaRuleStatus.NoData) && (passed > noData))
                            {
                                currentRuleStatus = SlaRuleStatus.Passed;
                            }
                            log("status : " + currentRuleStatus);
                            rule.AppendChild(xmlDoc.CreateTextNode(currentRuleStatus.ToString()));
                            if (currentRuleStatus.Equals(SlaRuleStatus.Failed)) // 0 = failed
                            {
                                iPassed = (int)Launcher.ExitCodeEnum.Failed;
                            }
                            iCounter++;
                        }
                        else
                        {
                            rule = xmlDoc.CreateElement("SLA_GOAL"); //no white space in the element name
                            log("Full Name : " + a.RuleUiName);
                            rule.SetAttribute("FullName", a.RuleUiName);
                            log("Measurement : " + a.Measurement);
                            rule.SetAttribute("Measurement", a.Measurement.ToString());
                            log("SLA Load Threshold Value : " + a.CriteriaMeasurement);
                            rule.SetAttribute("SLALoadThresholdValue", a.CriteriaMeasurement.ToString());
                            log("LoadThresholds : " + a.LoadThresholds.Count);
                            foreach (SlaLoadThreshold slat in a.LoadThresholds)
                            {
                                XmlElement loadThr = xmlDoc.CreateElement("SlaLoadThreshold");
                                loadThr.SetAttribute("StartLoadValue", slat.StartLoadValue.ToString(formatProvider));
                                loadThr.SetAttribute("EndLoadValue", slat.EndLoadValue.ToString(formatProvider));
                                loadThr.SetAttribute("ThresholdValue", slat.ThresholdValue.ToString(formatProvider));
                                rule.AppendChild(loadThr);

                            }
                            XmlElement timeRanges = xmlDoc.CreateElement("TimeRanges");
                            log("TimeRanges : " + a.TimeRanges.Count);
                            int passed = 0;
                            int failed = 0;
                            int noData = 0;
                            foreach (SlaTimeRangeInfo slatri in a.TimeRanges)
                            {
                                XmlElement subsubelem = xmlDoc.CreateElement("TimeRangeInfo");
                                subsubelem.SetAttribute("StartTime", slatri.StartTime.ToString());
                                subsubelem.SetAttribute("EndTime", slatri.EndTime.ToString());
                                subsubelem.SetAttribute("GoalValue", slatri.GoalValue.ToString(formatProvider));
                                subsubelem.SetAttribute("ActualValue", slatri.ActualValue.ToString(formatProvider));
                                subsubelem.SetAttribute("LoadValue", slatri.LoadValue.ToString(formatProvider));
                                subsubelem.InnerText = slatri.Status.ToString();
                                switch (slatri.Status)
                                {
                                    case SlaRuleStatus.Failed:
                                        failed++;
                                        break;
                                    case SlaRuleStatus.Passed:
                                        passed++;
                                        break;
                                    case SlaRuleStatus.NoData:
                                        noData++;
                                        break;
                                    default:
                                        break;
                                }
                                timeRanges.AppendChild(subsubelem);
                            }
                            rule.AppendChild(timeRanges);
                            SlaRuleStatus currentRuleStatus = a.Status;
                            if (currentRuleStatus.Equals(SlaRuleStatus.NoData) && (passed > noData))
                            {
                                currentRuleStatus = SlaRuleStatus.Passed;
                            }
                            log("status : " + currentRuleStatus);
                            rule.AppendChild(xmlDoc.CreateTextNode(currentRuleStatus.ToString()));
                            if (currentRuleStatus.Equals(SlaRuleStatus.Failed))
                            {
                                iPassed = (int)Launcher.ExitCodeEnum.Failed;
                            }

                        }
                        root.AppendChild(rule);

                        log(Resources.DoubleLineSeperator);
                    }

                    XmlNode slaNode = runReprotDoc.ImportNode(root, true);
                    runsRoot.AppendChild(slaNode);
                    log("saving RunReport.xml to " +
                        Path.Combine(Path.GetDirectoryName(Path.GetDirectoryName(lrrlocation)), "RunReport.xml"));
                    runReprotDoc.Save(Path.Combine(Path.GetDirectoryName(Path.GetDirectoryName(lrrlocation)),
                        "RunReport.xml"));
                    runReprotDoc.RemoveAll();

                    //write XML to location:
                    log("saving SLA.xml to " +
                        Path.Combine(Path.GetDirectoryName(Path.GetDirectoryName(lrrlocation)), "SLA.xml"));
                    xmlDoc.Save(Path.Combine(Path.GetDirectoryName(Path.GetDirectoryName(lrrlocation)), "SLA.xml"));

                }
                else
                {

                    log(Resources.CannotCreateSession);
                    return (int)Launcher.ExitCodeEnum.Aborted;
                }
                log("closing analysis session");
                session.Close();
            }
            catch (TypeInitializationException ex)
            {
                if (ex.InnerException is UnauthorizedAccessException)
                    log(
                        "UnAuthorizedAccessException: Please check the account privilege of current user, LoadRunner tests should be run by administrators.");
                else
                {
                    log(ex.Message);
                    log(ex.StackTrace);
                }
                return (int)Launcher.ExitCodeEnum.Aborted;
            }
            catch (Exception e)
            {
                log(e.Message);
                log(e.StackTrace);
                return (int)Launcher.ExitCodeEnum.Aborted;
            }


            // return SLA status code, if any SLA fails return a fail here.
            // writer.Flush();
            // writer.Close();
            return iPassed;

        }

        static System.Reflection.Assembly CurrentDomain_AssemblyResolve(object sender, ResolveEventArgs args)
        {
            System.Reflection.AssemblyName name = new System.Reflection.AssemblyName(args.Name);
            if (name.Name.ToLowerInvariant().EndsWith(".resources")) return null;
            string installPath = HpToolsLauncher.Helper.getLRInstallPath();
            if (installPath == null)
            {
                log(Resources.CannotLocateInstallDir);
                Environment.Exit((int)Launcher.ExitCodeEnum.Aborted);
            }
            //log(Path.Combine(installPath, "bin", name.Name + ".dll"));
            return System.Reflection.Assembly.LoadFrom(Path.Combine(installPath, "bin", name.Name + ".dll"));
        }

        private static void ShowHelp()
        {
            log("Micro Focus LoadRunner Analysis Command Line Executer");
            log();
            Console.Write("Usage: LRAnalysisLauncher.exe");
            Console.ForegroundColor = ConsoleColor.Cyan;
            Console.Write("[.lrr file location] [.lra output location] [html report output folder] [.tem file location(optional)]");
            Console.ResetColor();
            Environment.Exit((int)Launcher.ExitCodeEnum.Failed);
        }
    }
}
