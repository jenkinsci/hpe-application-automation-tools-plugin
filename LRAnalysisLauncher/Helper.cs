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
using System.Net;
using Analysis.Api;
using Analysis.ApiLib;
using Analysis.ApiLib.Dimensions;
using HpToolsLauncher;
using LRAnalysisLauncher.Properties;

namespace LRAnalysisLauncher
{
    public static class Helper
    {
  
        /// <summary>
        /// Returns the number of run vusers from VuserStateGraph graph
        /// </summary>
        /// <returns>Number of run vusers per catagory</returns>
        public static Dictionary<string, int> GetVusersCountByStatus(LrAnalysis lrAnalysis)
        {

            
            var vuserDictionary = new Dictionary<string, int>(4)
            {
                {"Passed", 0},
                {"Stopped", 0},
                {"Failed", 0},
                {"Error", 0}
            };
            lrAnalysis.Session.VUsers.LoadValuesIfNeeded();

            Graph vUserGraph = lrAnalysis.Session.OpenGraph("VuserStateGraph");
            if (vUserGraph == null)
            {
                return vuserDictionary;
            }

            List<String> vUserStates = new List<String>()
            {
                {"Passed"},
                {"Stopped"},
                {"Failed"},
                {"Error"}
            };

            try
            {
                Console.Write("Counting vUser Results for this scenario from ");
                FilterItem filterDimensionVUser;
                FilterItem item;
                Series vuserRanSeries;
                vUserGraph.Filter.Reset();
                Console.Write(vUserGraph.Name);
                if (vUserGraph.Filter.TryGetValue("Vuser Status", out filterDimensionVUser) &&
                    vUserGraph.Filter.TryGetValue("Vuser End Status", out item))
                {
                    filterDimensionVUser.ClearValues();
                    item.ClearValues();
                    Console.Write(" by ");
                    foreach (string vUserEndStatus in item.AvailableValues.DiscreteValues)
                    {
                        item.ClearValues();
                        Console.Write(vUserEndStatus + " ");
                        item.AddDiscreteValue(vUserEndStatus);
                        vUserGraph.ApplyFilterAndGroupBy();
                        if (vUserGraph.Series.TryGetValue("Quit", out vuserRanSeries))
                        {
                            if (!vuserRanSeries.GraphStatistics.IsFunctionAvailable(StatisticsFunctionKind.Maximum))
                            {
                                continue;
                            }
                            double vUserTypeMax = vuserRanSeries.GraphStatistics.Maximum;
                            if (!HasValue(vUserTypeMax))
                            {
                                continue;
                            }
                            vuserDictionary[vUserEndStatus] = (int)Math.Round(vUserTypeMax);
                        }
                    }
                    Console.WriteLine("");
                }

                ConsoleWriter.WriteLine("Getting maximum ran vUsers this scenarion");
                var vUserStateGraph = lrAnalysis.Session.OpenGraph("VuserStateGraph");
                if (vUserStateGraph == null)
                {
                    return vuserDictionary;
                }
                vUserStateGraph.Granularity = 4;
                if (vUserStateGraph.Filter.TryGetValue("Vuser Status", out filterDimensionVUser) && vUserStateGraph.Series.TryGetValue("Run", out vuserRanSeries))
                    {
                        filterDimensionVUser.ClearValues();
                        vUserGraph.ApplyFilterAndGroupBy();
                        double vUserMax = vuserRanSeries.GraphStatistics.Maximum;
                        if (!HasValue(vUserMax))
                        {
                            vUserMax = -1;
                        }
                        vuserDictionary.Add("MaxVuserRun", (int)Math.Round(vUserMax));
                        ConsoleWriter.WriteLine(String.Format("{0} maximum vUser ran per {1} seconds", vUserMax, vUserStateGraph.Granularity));
                    }
            }
            catch (StackOverflowException exception)
            {
                ConsoleWriter.WriteLine(String.Format("Debug: Error on getting VUsers from Analysis" + exception));
            }

            return vuserDictionary;
        }


        /// <summary>
        /// Calculating the number of transactions by status
        /// </summary>
        /// <returns>Transactions by status</returns>
        public static Dictionary<string, Dictionary<string, double>> CalcFailedTransPercent(LrAnalysis lrAnalysis)
        {
            var transDictionary = new Dictionary<string, Dictionary<string, double>>();
            //Console.WriteLine("Adding Transaction statistics");
            //var transactionGraph = lrAnalysis.Session.OpenGraph("TransactionSummary");
            //if (transactionGraph == null)
            //{
            //    return transDictionary;
            //}
            //transactionGraph.Filter.Reset();
            //transactionGraph.Granularity = 4;
            //FilterItem filterDimension;
            //if (!transactionGraph.Filter.TryGetValue("Transaction End Status", out filterDimension))
            //{
            //    return transDictionary;
            //}

            //foreach (var series in transactionGraph.Series)
            //{
            //    SeriesAttributeValue a;
            //    if (!series.Attributes.TryGetValue("Event Name", out a)) continue;
            //    SeriesAttributeValue transEndStatusAttr;

            //    if (!series.Attributes.TryGetValue("Transaction End Status", out transEndStatusAttr)) continue;

            //    Dictionary<string, double> value;
            //    if (!transDictionary.TryGetValue(a.Value.ToString(), out value))
            //    {
            //        transDictionary.Add(a.Value.ToString(),
            //            new Dictionary<string, double>() {{"Pass", 0}, {"Fail", 0}, {"Stop", 0}});
            //    }
            //    (transDictionary[a.Value.ToString()])[transEndStatusAttr.Value.ToString()] = series.Points[0].Value;
            //}
        
            return transDictionary;
        }

        ///<summary>
        /// Get Connections count
        /// </summary>
        public static double GetConnectionsCount(LrAnalysis lrAnalysis)
        {
            double connectionsCount = 0;

            try
            {
                Graph g;
                // check if Connections graph has data
                if (lrAnalysis.Session.Runs[0].Graphs.TryGetValue("Connections", out g) != true)
                {
                    throw new Exception("Failed to retrieve values from Connections graph");
                }
                if (g.Series.Count == 0)
                {
                    throw new Exception("No data exists in Connections graph");
                }

                g.Granularity = 1;

                foreach (FilterItem fi in g.Filter)
                {
                    fi.ClearValues();
                    fi.IsActive = false;
                    g.ApplyFilterAndGroupBy();
                }

                g.ApplyFilterAndGroupBy();
                connectionsCount = g.Series["Connections"].GraphStatistics.Maximum;
                if (!HasValue(connectionsCount))
                {
                    connectionsCount = -1;
                }
            }
            catch (Exception ex)
            {
                Console.Write(Resources.Helper_GetConnectionsCount_ + ex.Message);
            }

            return connectionsCount;
        }


        /// <summary>
        /// Returns scenario duration
        /// </summary>
        /// <returns>Scenario duration</returns>
        public static String GetScenarioDuration(Run run)
        {
            var testDuration = run.EndTime - run.StartTime;
            return testDuration.ToString();
        }


        public static DateTime FromUnixTime(long unixTime)
        {
            var epoch = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Local);
            return epoch.AddSeconds(unixTime);
        }

        public static bool HasValue(double value)
        {
            return !Double.IsNaN(value) && !Double.IsInfinity(value);
        }

    }
}