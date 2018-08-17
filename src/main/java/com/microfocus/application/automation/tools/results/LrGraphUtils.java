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

package com.microfocus.application.automation.tools.results;

import com.microfocus.application.automation.tools.results.projectparser.performance.AvgTransactionResponseTime;
import com.microfocus.application.automation.tools.results.projectparser.performance.LrProjectScenarioResults;
import com.microfocus.application.automation.tools.results.projectparser.performance.PercentileTransactionWholeRun;
import com.microfocus.application.automation.tools.results.projectparser.performance.TimeRangeResult;
import com.microfocus.application.automation.tools.results.projectparser.performance.WholeRunResult;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The type Lr graph utils.
 */
public final class LrGraphUtils {

    /**
     * The constant X_AXIS_TITLE.
     */
    private static final String X_AXIS_TITLE = "x_axis_title";
    /**
     * The constant Y_AXIS_TITLE.
     */
    private static final String Y_AXIS_TITLE = "y_axis_title";
    /**
     * The constant DESCRIPTION.
     */
    private static final String DESCRIPTION = "description";
    /**
     * The constant TITLE.
     */
    private static final String TITLE = "title";
    /**
     * The constant LABELS.
     */
    private static final String LABELS = "labels";
    /**
     * The constant BUILD_NUMBER.
     */
    private static final String BUILD_NUMBER = "Build number";
    /**
     * The constant PERCENTILE_TRANSACTION_RESPONSE_TIME.
     */
    private static final String PERCENTILE_TRANSACTION_RESPONSE_TIME = "Percentile Transaction Response Time";
    /**
     * The constant TRANSACTIONS_RESPONSE_TIME_SECONDS.
     */
    private static final String TRANSACTIONS_RESPONSE_TIME_SECONDS = "Time (Sec)";
    private static final String PRECENTILE_GRAPH_DESCRIPTION =
            "Displays the average time taken to perform transactions during each second of the load test." +
                    " This graph helps you determine whether the performance of the server is within " +
                    "acceptable minimum and maximum transaction performance time ranges defined for your " +
                    "system.";
    public static final String SERIES = "series";

    private LrGraphUtils() {
    }

    /**
     * creates dataset for Percentile transaction graph
     *
     * @param scenarioResults   the relative scenario results to create the graph
     * @param scenarioGraphData the target graph data set
     */
    static void constructPercentileTransactionGraph(Map.Entry<String, LrProjectScenarioResults> scenarioResults,
                                                    JSONObject scenarioGraphData) {
        Map<Integer, TreeMap<String, PercentileTransactionWholeRun>> percentileTransactionResults =
                scenarioResults.getValue().getPercentileTransactionResults();
        JSONObject percentileTransactionResultsGraphSet =
                extractPercentileTransactionSet(percentileTransactionResults,
                        scenarioResults.getValue().getTransactions());
        if (!percentileTransactionResultsGraphSet.getJSONArray(LABELS).isEmpty()) {
            percentileTransactionResultsGraphSet
                    .put(TITLE, PERCENTILE_TRANSACTION_RESPONSE_TIME);
            percentileTransactionResultsGraphSet
                    .put(X_AXIS_TITLE, BUILD_NUMBER);
            percentileTransactionResultsGraphSet.put(Y_AXIS_TITLE,
                    TRANSACTIONS_RESPONSE_TIME_SECONDS);
            percentileTransactionResultsGraphSet
                    .put(DESCRIPTION, PRECENTILE_GRAPH_DESCRIPTION);
            scenarioGraphData.put("percentileTransaction", percentileTransactionResultsGraphSet);
        }
    }

    private static JSONObject extractPercentileTransactionSet(
            Map<Integer, TreeMap<String, PercentileTransactionWholeRun>> graphData, HashSet<String> transactions) {
        JSONObject graphDataSet = new JSONObject();
        JSONArray labels = new JSONArray();

        HashMap<String, ArrayList<Number>> percentileTrtData = new HashMap<String, ArrayList<Number>>(0);
        for (String transaction : transactions) {
            percentileTrtData.put(transaction, new ArrayList<Number>(0));
        }

        for (Map.Entry<Integer, TreeMap<String, PercentileTransactionWholeRun>> result : graphData.entrySet()) {
            labels.add(result.getKey());

            for (String transaction : transactions) {
                if (!result.getValue().containsKey(transaction)) {
                    percentileTrtData.get(transaction).add(null);
                    // TODO:change to null
                    continue;
                }
                percentileTrtData.get(transaction).add((result.getValue()).get(transaction).getActualValue());
            }
        }

        graphDataSet.put(LABELS, labels);
        graphDataSet.put(SERIES, createGraphDatasets(percentileTrtData));

        return graphDataSet;
    }

    private static JSONArray createGraphDatasets(Map<String, ArrayList<Number>> datasets) {
        JSONArray graphSeries = new JSONArray();
        for (Map.Entry<String, ArrayList<Number>> transactionData : datasets.entrySet()) {
            JSONObject dataset = new JSONObject();
            dataset.put("name", transactionData.getKey());
            JSONArray data = new JSONArray();
            data.addAll(transactionData.getValue());
            dataset.put("data", data);
            graphSeries.add(dataset);
        }
        return graphSeries;
    }

    /**
     * Construct avg transaction graph.
     *
     * @param scenarioResults   the scenario results
     * @param scenarioGraphData the scenario graph data
     */
    static void constructAvgTransactionGraph(Map.Entry<String, LrProjectScenarioResults> scenarioResults,
                                             JSONObject scenarioGraphData) {
        Map<Integer, TreeMap<String, AvgTransactionResponseTime>> avgTransactionResponseTimeResults =
                scenarioResults.getValue().getAvgTransactionResponseTimeResults();
        JSONObject avgTransactionResponseTimeGraphSet =
                extractAvgTrtData(avgTransactionResponseTimeResults, scenarioResults.getValue().getTransactions());
        if (!avgTransactionResponseTimeGraphSet.getJSONArray(LABELS).isEmpty()) {
            avgTransactionResponseTimeGraphSet.put(TITLE, "Average Transaction Response TIme");
            avgTransactionResponseTimeGraphSet.put(X_AXIS_TITLE, "Build number");
            avgTransactionResponseTimeGraphSet
                    .put(Y_AXIS_TITLE, "Time (Sec.)");
            avgTransactionResponseTimeGraphSet.put(DESCRIPTION,
                    "Displays the average time taken to perform transactions during each second of the load test." +
                            " This graph helps you determine whether the performance of the server is within " +
                            "acceptable minimum and maximum transaction performance time ranges defined for your " +
                            "system.");
            scenarioGraphData.put("averageTransactionResponseTime", avgTransactionResponseTimeGraphSet);
        }
    }

    private static JSONObject extractAvgTrtData(Map<Integer, TreeMap<String, AvgTransactionResponseTime>> graphData,
                                                HashSet<String> transactions) {
        HashMap<String, ArrayList<Number>> averageTRTData = new HashMap<String, ArrayList<Number>>(0);
        JSONObject graphDataSet = new JSONObject();

        JSONArray labels = new JSONArray();

        for (String transaction : transactions) {
            averageTRTData.put(transaction, new ArrayList<Number>(0));
        }

        for (Map.Entry<Integer, TreeMap<String, AvgTransactionResponseTime>> result : graphData.entrySet()) {
            labels.add(result.getKey());

            for (String transaction : transactions) {
                if (!result.getValue().containsKey(transaction)) {
                    averageTRTData.get(transaction).add(null);
                    // TODO:change to null
                    continue;
                }
                averageTRTData.get(transaction).add((result.getValue()).get(transaction).getActualValueAvg());
            }
        }

        graphDataSet.put(LABELS, labels);
        JSONArray datasets = createGraphDatasets(averageTRTData);
        graphDataSet.put(SERIES, datasets);
        return graphDataSet;
    }

    /**
     * Construct error graph.
     *
     * @param scenarioResults   the scenario results
     * @param scenarioGraphData the scenario graph data
     */
    static void constructErrorGraph(Map.Entry<String, LrProjectScenarioResults> scenarioResults,
                                    JSONObject scenarioGraphData) {
        Map<Integer, TimeRangeResult> errPerSecResults = scenarioResults.getValue().getErrPerSecResults();
        JSONObject errPerSecResultsResultsGraphSet =
                extractTimeRangeResult(errPerSecResults);
        if (!errPerSecResultsResultsGraphSet.getJSONArray(LABELS).isEmpty()) {
            errPerSecResultsResultsGraphSet.put(TITLE, "Total errors per second");
            errPerSecResultsResultsGraphSet.put(X_AXIS_TITLE, "Build number");
            errPerSecResultsResultsGraphSet.put(Y_AXIS_TITLE, "Errors");
            errPerSecResultsResultsGraphSet.put(DESCRIPTION, "");
            scenarioGraphData.put("errorPerSecResults", errPerSecResultsResultsGraphSet);
        }
    }

    private static JSONObject extractTimeRangeResult(Map<Integer, TimeRangeResult> graphData) {
        JSONObject graphDataSet;
        graphDataSet = new JSONObject();

        JSONArray labels = new JSONArray();
        JSONArray datasets = new JSONArray();
        JSONArray data = new JSONArray();

        for (Map.Entry<Integer, TimeRangeResult> result : graphData.entrySet()) {
            if (result.getValue().getTimeRanges().isEmpty()) {
                labels.add(result.getKey());
                data.add(result.getValue().getActualValueAvg());
            }
        }

        graphDataSet.put(LABELS, labels);
        datasets.add(data);
        graphDataSet.put(SERIES, datasets);
        return graphDataSet;
    }

    /**
     * Construct average throughput.
     *
     * @param scenarioResults   the scenario results
     * @param scenarioGraphData the scenario graph data
     */
    static void constructAverageThroughput(Map.Entry<String, LrProjectScenarioResults> scenarioResults,
                                           JSONObject scenarioGraphData) {
        Map<Integer, WholeRunResult> averageThroughputResults = scenarioResults.getValue().getAverageThroughputResults();
        JSONObject averageThroughputResultsGraphSet =
                extractWholeRunSlaResult(averageThroughputResults, "Bytes/Sec");
        if (!averageThroughputResultsGraphSet.getJSONArray(LABELS).isEmpty()) {
            averageThroughputResultsGraphSet.put(TITLE, "Average Throughput per second");
            averageThroughputResultsGraphSet.put(X_AXIS_TITLE, "Build number");
            averageThroughputResultsGraphSet.put(Y_AXIS_TITLE, "Bytes");
            averageThroughputResultsGraphSet.put(DESCRIPTION,
                    " Displays the amount of throughput (in bytes) on the Web server during the load test. " +
                            "Throughput represents the amount of data that the Vusers received from the server at" +
                            " any given second. This graph helps you to evaluate the amount of load Vusers " +
                            "generate, in terms of server throughput.\n");
            scenarioGraphData.put("averageThroughput", averageThroughputResultsGraphSet);
        }
    }

    /**
     * @param graphData
     * @return
     */
    private static JSONObject extractWholeRunSlaResult(Map<Integer, WholeRunResult> graphData, String seriesName) {
        JSONObject graphDataSet;
        graphDataSet = new JSONObject();
        JSONObject wholeRunSlaResults = new JSONObject();
        JSONArray labels = new JSONArray();
        JSONArray datasets = new JSONArray();
        JSONArray data = new JSONArray();
        for (Map.Entry<Integer, WholeRunResult> result : graphData.entrySet()) {
            labels.add(result.getKey());
            data.add(result.getValue().getActualValue());
        }
        graphDataSet.put(LABELS, labels);
        wholeRunSlaResults.put("name",seriesName);
        wholeRunSlaResults.put("data",data);
        datasets.add(wholeRunSlaResults);
        graphDataSet.put(SERIES, datasets);
        return graphDataSet;
    }

    /**
     * Construct total throughput graph.
     *
     * @param scenarioResults   the scenario results
     * @param scenarioGraphData the scenario graph data
     */
    static void constructTotalThroughputGraph(Map.Entry<String, LrProjectScenarioResults> scenarioResults,
                                              JSONObject scenarioGraphData) {
        Map<Integer, WholeRunResult> totalThroughputResults = scenarioResults.getValue().getTotalThroughtputResults();
        JSONObject totalThroughputResultsGraphSet =
                extractWholeRunSlaResult(totalThroughputResults, "Bytes");
        if (!totalThroughputResultsGraphSet.getJSONArray(LABELS).isEmpty()) {
            totalThroughputResultsGraphSet.put(TITLE, "Total Throughput");
            totalThroughputResultsGraphSet.put(X_AXIS_TITLE, "Build number");
            totalThroughputResultsGraphSet.put(Y_AXIS_TITLE, "Bytes");
            totalThroughputResultsGraphSet.put(DESCRIPTION,
                    " Displays the amount of throughput (in bytes) on the Web server during the load test. " +
                            "Throughput represents the amount of data that the Vusers received from the server at" +
                            " any given second. This graph helps you to evaluate the amount of load Vusers " +
                            "generate, in terms of server throughput.\n");
            scenarioGraphData.put("totalThroughput", totalThroughputResultsGraphSet);
        }
    }

    /**
     * Construct avg hits graph.
     *
     * @param scenarioResults   the scenario results
     * @param scenarioGraphData the scenario graph data
     */
    static void constructAvgHitsGraph(Map.Entry<String, LrProjectScenarioResults> scenarioResults,
                                      JSONObject scenarioGraphData) {
        Map<Integer, WholeRunResult> avgHitsPerSec = scenarioResults.getValue().getAverageHitsPerSecondResults();
        JSONObject avgHitsPerSecGraphSet = extractWholeRunSlaResult(avgHitsPerSec, "Hits/Sec");
        if (!avgHitsPerSecGraphSet.getJSONArray(LABELS).isEmpty()) {
            avgHitsPerSecGraphSet.put(TITLE, "Average Hits per Second");
            avgHitsPerSecGraphSet.put(X_AXIS_TITLE, "Build number");
            avgHitsPerSecGraphSet.put(Y_AXIS_TITLE, "Hits");
            avgHitsPerSecGraphSet.put(DESCRIPTION,
                    "Displays the number of hits made on the Web server by Vusers " +
                            "during each second of the load test. This graph helps you evaluate the amount of load " +
                            "Vusers" +
                            " " +
                            "generate, in terms of the number of hits.");
            scenarioGraphData.put("avgHitsPerSec", avgHitsPerSecGraphSet);
        }
    }

    /**
     * Construct total hits graph.
     *
     * @param scenarioResults   the scenario results
     * @param scenarioGraphData the scenario graph data
     */
    static void constructTotalHitsGraph(Map.Entry<String, LrProjectScenarioResults> scenarioResults,
                                        JSONObject scenarioGraphData) {
        Map<Integer, WholeRunResult> totalHitsResults = scenarioResults.getValue().getTotalHitsResults();
        JSONObject totalHitsGraphSet = extractWholeRunSlaResult(totalHitsResults, "Hits");
        if (!totalHitsGraphSet.getJSONArray(LABELS).isEmpty()) {
            totalHitsGraphSet.put(TITLE, "Total Hits");
            totalHitsGraphSet.put(X_AXIS_TITLE, "Build number");
            totalHitsGraphSet.put(Y_AXIS_TITLE, "Hits");
            totalHitsGraphSet.put(DESCRIPTION,
                    "Displays the number of hits made on the Web server by Vusers " +
                            "during each second of the load test. This graph helps you evaluate the amount of load " +
                            "Vusers" +
                            " " +
                            "generate, in terms of the number of hits.");
            scenarioGraphData.put("totalHits", totalHitsGraphSet);
        }
    }

    private static JSONObject extractVuserResult(Map<Integer, TreeMap<String, Integer>> graphData) {
        JSONObject graphDataSet;
        graphDataSet = new JSONObject();
        JSONArray labels = new JSONArray();

        HashMap<String, ArrayList<Number>> vUserState = new HashMap<String, ArrayList<Number>>(0);
        vUserState.put("Passed", new ArrayList<Number>(0));
        vUserState.put("Failed", new ArrayList<Number>(0));
        vUserState.put("Stopped", new ArrayList<Number>(0));
        vUserState.put("Error", new ArrayList<Number>(0));
        for(Map.Entry<Integer, TreeMap<String, Integer>> run : graphData.entrySet())
        {
            Number tempVUserCount = run.getValue().get("Count");
            if(tempVUserCount != null && tempVUserCount.intValue() > 0)
            {
                labels.add(run.getKey());
                vUserState.get("Passed").add(run.getValue().get("Passed"));
                vUserState.get("Failed").add(run.getValue().get("Failed"));
                vUserState.get("Stopped").add(run.getValue().get("Stopped"));
                vUserState.get("Error").add(run.getValue().get("Error"));
            }
        }

        graphDataSet.put(LABELS, labels);
        graphDataSet.put(SERIES, createGraphDatasets(vUserState));
        return graphDataSet;
    }


    static void constructVuserGraph(Map.Entry<String, LrProjectScenarioResults> scenarioResults,
                                         JSONObject scenarioGraphData) {
        Map<Integer, TreeMap<String, Integer>> vUserResults = scenarioResults.getValue().getvUserPerRun();
        JSONObject vUserGraphSet = extractVuserResult(vUserResults);
        if (!vUserGraphSet.getJSONArray(LABELS).isEmpty()) {
            vUserGraphSet.put(TITLE, "VUser");
            vUserGraphSet.put(X_AXIS_TITLE, "Build number");
            vUserGraphSet.put(Y_AXIS_TITLE, "Vuser count");
            vUserGraphSet.put(DESCRIPTION, "");
            scenarioGraphData.put("VUser", vUserGraphSet);
        }
    }

    static void constructConnectionsGraph(Map.Entry<String, LrProjectScenarioResults> scenarioResults,
                                    JSONObject scenarioGraphData) {
        SortedMap<Integer, Integer> connectionsResults = scenarioResults.getValue().getMaxConnectionsCount();
        JSONObject maxConnectionsGraphSet = extractConnectionResults(connectionsResults);
        if (!maxConnectionsGraphSet.getJSONArray(LABELS).isEmpty()) {
            maxConnectionsGraphSet.put(TITLE, "Connections");
            maxConnectionsGraphSet.put(X_AXIS_TITLE, "Build number");
            maxConnectionsGraphSet.put(Y_AXIS_TITLE, "Connection count");
            maxConnectionsGraphSet.put(DESCRIPTION, "");
            scenarioGraphData.put("Connections", maxConnectionsGraphSet);
        }
    }

    private static JSONObject extractConnectionResults(SortedMap<Integer, Integer> connectionsResults) {
        JSONObject graphDataSet;
        graphDataSet = new JSONObject();
        JSONArray labels = new JSONArray();
        JSONArray data = new JSONArray();
        JSONArray dataSets = new JSONArray();
        JSONObject maxConnections = new JSONObject();
        for(Map.Entry<Integer, Integer> runConnectionMax: connectionsResults.entrySet())
        {
            if(runConnectionMax.getValue()!= null && runConnectionMax.getValue() > 0)
            {
                labels.add(runConnectionMax.getKey());
                data.add(runConnectionMax.getValue().toString());
            }
        }
        maxConnections.put("name", "Max connections");
        maxConnections.put("data", data);
        dataSets.add(maxConnections);
        graphDataSet.put(LABELS, labels);
        graphDataSet.put(SERIES, dataSets);
        return graphDataSet;
    }

    static void constructVuserSummary(SortedMap<String, Integer> vUserResults,
                                      JSONObject scenarioStats, int size){
        JSONObject vUserSummary = new JSONObject();
        int vUserCount = vUserResults.get("Count");
        if(vUserCount != 0) {
            double passedVuserPercentile = ((double) vUserResults.get("Passed") / vUserCount) * 100;
            vUserSummary.put("PassedVuserPercentile", passedVuserPercentile);
            double failedVuserPercentile = ((double) vUserResults.get("Failed") / vUserCount) * 100;
            vUserSummary.put("FailedVuserPercentile", failedVuserPercentile);
            double errorVuserPercentile = ((double) vUserResults.get("Error") / vUserCount) * 100;
            vUserSummary.put("ErroredVuserPercentile", errorVuserPercentile);
            int avgMaxVuser = vUserResults.get("MaxVuserRun") / size;
            vUserSummary.put("AvgMaxVuser", avgMaxVuser);
            scenarioStats.put("VUserSummary", vUserSummary);
        }
    }

    

    static void constructConnectionSummary(SortedMap<Integer, Integer> maxConnectionPerRun,
                                      JSONObject scenarioStats){
        JSONObject maxConnectionsSummary = new JSONObject();
        int connectionSum = 0;
        for(Integer runConnectionMax: maxConnectionPerRun.values())
        {
            if((runConnectionMax > 0))
            {
               connectionSum += runConnectionMax;
               continue;
            }
        }

        int connectionMaxAverage = connectionSum / maxConnectionPerRun.size();
        maxConnectionsSummary.put("AvgMaxConnection", connectionMaxAverage);
        scenarioStats.put("AvgMaxConnections", maxConnectionsSummary);
    }

    static void constructTransactionSummary(SortedMap<String, Integer> transactionSummary,
                                           JSONObject scenarioStats, int size){
        JSONObject transactionSum = new JSONObject();
        for(SortedMap.Entry<String, Integer> transaction : transactionSummary.entrySet()){
            transactionSum.put(transaction.getKey(), (transaction.getValue().intValue() / size ));
        }
        scenarioStats.put("TransactionSummary", transactionSum);
    }

    static void constructDurationSummary(SortedMap<Integer, Long> durationData, JSONObject scenarioStats) {
        JSONObject durationSummary = new JSONObject();
        long runDurationSum = 0;
        for(Long runDuration: durationData.values())
        {
            if((runDuration > 0))
            {
                runDurationSum += runDuration;
                continue;
            }
        }
        double scenarioDurationAverage = -1;
        if(!durationData.isEmpty())
        {
            scenarioDurationAverage = (double) runDurationSum / durationData.size();
        }
        durationSummary.put("AvgDuration", scenarioDurationAverage);
        scenarioStats.put("AvgScenarioDuration", durationSummary);
    }
}
