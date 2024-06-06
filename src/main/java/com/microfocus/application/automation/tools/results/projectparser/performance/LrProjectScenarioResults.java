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

package com.microfocus.application.automation.tools.results.projectparser.performance;

import java.util.HashSet;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Data model for a project / build / pipeline that contains multiple runs per scenario being run.
 */
public class LrProjectScenarioResults extends LrScenario {

    public int getBuildCount() {
        return buildCount;
    }

    public void incBuildCount() {
        this.buildCount++;
    }

    private int buildCount;
    private SortedMap<Integer, WholeRunResult> totalThroughtputResults;
    private SortedMap<Integer, WholeRunResult> averageHitsPerSecondResults;
    private SortedMap<Integer, WholeRunResult> totalHitsResults;
    private SortedMap<Integer, TimeRangeResult> errPerSecResults;
    private SortedMap<Integer, TreeMap<String, PercentileTransactionWholeRun>> percentileTransactionResults;
    private SortedMap<Integer, TreeMap<String, AvgTransactionResponseTime>> avgTransactionResponseTimeResults;
    //Holds the data of a transactionss for the whole Job
    private HashSet<String> transactions;
    //Holds the data of connections per run for the whole Job
    private SortedMap<Integer, Integer> maxConnectionsCount;
    //Holds the summary data of vuser status(count, fail, pass, error) for the whole Job
    private SortedMap<String, Integer> vUserSummary;
    //Holds the summary data of vuser status(count, fail, pass, error) **per run** for the whole Job
    private SortedMap<Integer, TreeMap<String, Integer>> vUserPerRun;
    //Holds the summary status data of transactions(count, fail, pass, error) for the whole Job
    private SortedMap<String, Integer> transactionSum;
    //Holds the summary data of transaction status(count, fail, pass, error) **per run** for the whole Job
    private SortedMap<String, TreeMap<String, Integer>> transactionData;
    //Holds the duration aggragation for all scenario runs
    private SortedMap<Integer, Long> durationData;
    //Holds the data of an SLA rule per run for the whole Job
    private SortedMap<Integer, WholeRunResult> averageThroughputResults;
    private SortedMap<Integer, TreeMap<String, TreeMap<String, Integer>>> transactionPerRun;
    /**
     * Instantiates a new Lr project scenario results.
     *
     * @param scenarioName the scenario name
     */
    public LrProjectScenarioResults(String scenarioName) {
        this.setScenrioName(scenarioName);
        averageThroughputResults = new TreeMap<Integer, WholeRunResult>();
        totalThroughtputResults = new TreeMap<Integer, WholeRunResult>();
        averageHitsPerSecondResults = new TreeMap<Integer, WholeRunResult>();
        totalHitsResults = new TreeMap<Integer, WholeRunResult>();
        errPerSecResults = new TreeMap<Integer, TimeRangeResult>();
        percentileTransactionResults = new TreeMap<>();
        avgTransactionResponseTimeResults = new TreeMap<Integer, TreeMap<String, AvgTransactionResponseTime>>();
        transactions = new HashSet<String>();
        maxConnectionsCount = new TreeMap<>();

        durationData = new TreeMap<>();

        vUserSummary = new TreeMap<String, Integer>();
        vUserPerRun = new TreeMap<Integer, TreeMap<String, Integer>>();

        transactionSum = new TreeMap<String, Integer>();
        transactionData = new TreeMap<String, TreeMap<String, Integer>>();
        transactionPerRun = new TreeMap<Integer, TreeMap<String, TreeMap<String, Integer>>>();

        vUserMapInit(vUserSummary);
        vTransactionMapInit(transactionSum);

        buildCount = 0;
    }

    /**
     * initilize vuser maps with required values
     *
     * @param map the map
     */
    public static void vUserMapInit(SortedMap<String, Integer> map) {
        map.put("Passed", 0);
        map.put("Stopped", 0);
        map.put("Failed", 0);
        map.put("Count", 0);
        map.put("MaxVuserRun", 0);
    }

    /**
     * initilize vuser maps with required values
     *
     * @param map the map
     */
    public static void vTransactionMapInit(SortedMap<String, Integer> map) {
        map.put("Pass", 0);
        map.put("Stop", 0);
        map.put("Fail", 0);
        map.put("Count", 0);
    }

    /**
     * Gets total throughtput results.
     *
     * @return the total throughtput results
     */
    public SortedMap<Integer, WholeRunResult> getTotalThroughtputResults() {
        return totalThroughtputResults;
    }

    /**
     * Gets average hits per second results.
     *
     * @return the average hits per second results
     */
    public SortedMap<Integer, WholeRunResult> getAverageHitsPerSecondResults() {
        return averageHitsPerSecondResults;
    }

    /**
     * Gets total hits results.
     *
     * @return the total hits results
     */
    public SortedMap<Integer, WholeRunResult> getTotalHitsResults() {
        return totalHitsResults;
    }

    /**
     * Gets err per sec results.
     *
     * @return the err per sec results
     */
    public SortedMap<Integer, TimeRangeResult> getErrPerSecResults() {
        return errPerSecResults;
    }

    /**
     * Gets percentile transaction results.
     *
     * @return the percentile transaction results
     */
    public SortedMap<Integer, TreeMap<String, PercentileTransactionWholeRun>> getPercentileTransactionResults() {
        return percentileTransactionResults;
    }

    /**
     * Gets avg transaction response time results.
     *
     * @return the avg transaction response time results
     */
    public SortedMap<Integer, TreeMap<String, AvgTransactionResponseTime>> getAvgTransactionResponseTimeResults() {
        return avgTransactionResponseTimeResults;
    }

    /**
     * Gets transactions.
     *
     * @return the transactions
     */
    public HashSet<String> getTransactions() {
        return transactions;
    }

    /**
     * Gets max connections count.
     *
     * @return the max connections count
     */
    public SortedMap<Integer, Integer> getMaxConnectionsCount() {
        return maxConnectionsCount;
    }

    /**
     * Gets user summary.
     *
     * @return the user summary
     */
    public SortedMap<String, Integer> getvUserSummary() {
        return vUserSummary;
    }

    /**
     * Gets user per run.
     *
     * @return the user per run
     */
    public SortedMap<Integer, TreeMap<String, Integer>> getvUserPerRun() {
        return vUserPerRun;
    }

    /**
     * Gets transaction sum.
     *
     * @return the transaction sum
     */
    public SortedMap<String, Integer> getTransactionSum() {
        return transactionSum;
    }

    /**
     * Gets transaction data.
     *
     * @return the transaction data
     */
    public SortedMap<String, TreeMap<String, Integer>> getTransactionData() {
        return transactionData;
    }

    /**
     * Gets duration data.
     *
     * @return the duration data
     */
    public SortedMap<Integer, Long> getDurationData() {
        return durationData;
    }

    /**
     * Gets average throughput results.
     *
     * @return the average throughput results
     */
    public SortedMap<Integer, WholeRunResult> getAverageThroughputResults() {
        return averageThroughputResults;
    }

    /**
     * Gets transaction per run.
     *
     * @return the transaction per run
     */
    public SortedMap<Integer, TreeMap<String, TreeMap<String, Integer>>> getTransactionPerRun() {
        return transactionPerRun;
    }
}
