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
import com.microfocus.application.automation.tools.results.projectparser.performance.GoalResult;
import com.microfocus.application.automation.tools.results.projectparser.performance.JobLrScenarioResult;
import com.microfocus.application.automation.tools.results.projectparser.performance.LrJobResults;
import com.microfocus.application.automation.tools.results.projectparser.performance.LrProjectScenarioResults;
import com.microfocus.application.automation.tools.results.projectparser.performance.LrTest;
import com.microfocus.application.automation.tools.results.projectparser.performance.PercentileTransactionWholeRun;
import com.microfocus.application.automation.tools.results.projectparser.performance.ProjectLrResults;
import com.microfocus.application.automation.tools.results.projectparser.performance.TimeRangeResult;
import com.microfocus.application.automation.tools.results.projectparser.performance.WholeRunResult;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Run;
import hudson.util.RunList;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import static com.microfocus.application.automation.tools.results.projectparser.performance.JobLrScenarioResult
        .DEFAULT_CONNECTION_MAX;
import static com.microfocus.application.automation.tools.results.projectparser.performance.JobLrScenarioResult
        .DEFAULT_SCENARIO_DURATION;

/**
 * The type Performance project action.
 */
public class PerformanceProjectAction implements Action {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger
            .getLogger(PerformanceProjectAction.class.getName());
    private static final int MAX_DISPLAY_BUILDS = 20;
    /**
     * The Current project.
     */
    public final Job<?, ?> currentProject;
    private ArrayList<LrJobResults> jobLrResults;
    private int lastBuildId = -1;
    private ArrayList<Integer> _workedBuilds;
    private ProjectLrResults _projectResult;
    private Collection<Action> projectActions;


    /**
     * Instantiates a new Performance job action.
     *
     * @param job the job
     */
    public PerformanceProjectAction(Job<?, ?> job) {
        this._workedBuilds = new ArrayList<Integer>();
        this.jobLrResults = new ArrayList<LrJobResults>();
        this.currentProject = job;
        projectActions = new ArrayList<>();
    }

    private void updateLastBuild() {

        // TODO: The first one is the last one!!

    }

    /**
     * Gets scenario list.
     *
     * @return the scenario list
     */
    @JavaScriptMethod
    public JSONArray getScenarioList() {
        JSONArray scenarioList = new JSONArray();
        for (String scenarioName : _projectResult.getScenarioResults().keySet()) {
            JSONObject scenario = new JSONObject();
            scenario.put("ScenarioName", scenarioName);
            scenarioList.add(scenario);
        }
        return scenarioList;
    }

    /**
     * Collates graph data per scenario per build for the whole project.
     * Adds the respected graphs with scenario as the key
     *
     * @return the graph data
     */
    @JavaScriptMethod
    public JSONObject getGraphData() {
        JSONObject projectDataSet = new JSONObject();
        if (_projectResult == null) {
//            getUpdatedData();
            return new JSONObject();
        }


        for (SortedMap.Entry<String, LrProjectScenarioResults> scenarioResults : _projectResult.getScenarioResults()
                .entrySet()) {

            JSONObject scenarioData = new JSONObject();
            JSONObject scenarioStats = new JSONObject();
            //            LrGraphUtils
            //                    .constructVuserSummary(scenarioResults.getValue().getvUserSummary(), scenarioStats, _workedBuilds
            //                            .size());
            //            LrGraphUtils.constructDurationSummary(scenarioResults.getValue().getDurationData(), scenarioStats);
            //            LrGraphUtils.constructConnectionSummary(scenarioResults.getValue().getMaxConnectionsCount(), scenarioStats);
            //            LrGraphUtils.constructTransactionSummary(scenarioResults.getValue().getTransactionSum(), scenarioStats,
            //                    _workedBuilds.size());


            scenarioData.put("scenarioStats", scenarioStats);

            JSONObject scenarioGraphData = new JSONObject();
            //Scenario data graphs
//            LrGraphUtils.constructVuserGraph(scenarioResults, scenarioGraphData);
//            LrGraphUtils.constructConnectionsGraph(scenarioResults, scenarioGraphData);
            //Scenario SLA graphs
            LrGraphUtils.constructTotalHitsGraph(scenarioResults, scenarioGraphData);
            LrGraphUtils.constructAvgHitsGraph(scenarioResults, scenarioGraphData);
            LrGraphUtils.constructTotalThroughputGraph(scenarioResults, scenarioGraphData);
            LrGraphUtils.constructAverageThroughput(scenarioResults, scenarioGraphData);
            LrGraphUtils.constructErrorGraph(scenarioResults, scenarioGraphData);
            LrGraphUtils.constructAvgTransactionGraph(scenarioResults, scenarioGraphData);
            LrGraphUtils.constructPercentileTransactionGraph(scenarioResults, scenarioGraphData);

            scenarioData.put("scenarioData", scenarioGraphData);

            String scenarioName = scenarioResults.getKey();
            projectDataSet.put(scenarioName, scenarioData);
        }
        return projectDataSet;
    }


    /**
     * Gets build performance report list.
     *
     * @return the build performance report list
     */
    public List<String> getBuildPerformanceReportList() {
        // this.buildPerformanceReportList = new ArrayList<String>(0);
        // if (null == this.currentProject) {
        // return this.buildPerformanceReportList;
        // }

        // if (null == this.currentProject.getSomeBuildWithWorkspace()) {
        // return buildPerformanceReportList;
        // }

        // List<? extends AbstractBuild<?, ?>> builds = currentProject.getBuilds();
        // int nbBuildsToAnalyze = builds.size();
        //// Range buildsLimits = getFirstAndLastBuild(request, builds);

        // for (AbstractBuild<?, ?> currentBuild : builds) {
        //
        // buildPerformanceReportList.add(currentBuild.getId());
        // }
        // return buildPerformanceReportList;
        return new ArrayList<String>(0);
    }

    @Override
    public String getIconFileName() {
        return "/plugin/hp-application-automation-tools-plugin/PerformanceReport/LoadRunner.png";
    }

    @Override
    public String getDisplayName() {
        return "Project Performance report";
    }

    @Override
    public String getUrlName() {
        return "PerformanceProjectReport";
    }

    /**
     * Is visible boolean.
     *
     * @return the boolean
     */
    boolean isVisible() {
        List<? extends Run<?, ?>> builds = currentProject.getBuilds();
        for (Run run : builds) {
            if (run.getAction(PerformanceJobReportAction.class) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets updated data.
     */
    public synchronized void getUpdatedData() {
        if (!isUpdateDataNeeded()) {
            return;
        }

        this._projectResult = new ProjectLrResults();

        _workedBuilds = new ArrayList<Integer>();

        RunList<? extends Run> projectBuilds = currentProject.getBuilds();

        // updateLastBuild();

        for (Run run : projectBuilds) {
            PerformanceJobReportAction performanceJobReportAction = run.getAction(PerformanceJobReportAction.class);
            if (performanceJobReportAction == null) {
                continue;
            }
            if (run.isBuilding()) {
                continue;
            }

            int runNumber = run.getNumber();
            if (_workedBuilds.contains(runNumber)) {
                continue;
            }



            _workedBuilds.add(runNumber);
            LrJobResults jobLrResult = performanceJobReportAction.getLrResultBuildDataset();

            // get all the ran scenario results from this run and insert them into the project
            for (Map.Entry<String, JobLrScenarioResult> runResult : jobLrResult.getLrScenarioResults().entrySet()) {
                // add the scenario if it's the first time it's ran in this build (allows scenarios to be also added
                // at diffrent time)
                if (!_projectResult.getScenarioResults().containsKey(runResult.getKey())) {
                    _projectResult.addScenario(new LrProjectScenarioResults(runResult.getKey()));
                }
                // Join the SLA rule results
                LrProjectScenarioResults lrProjectScenarioResults =
                        _projectResult.getScenarioResults().get(runResult.getKey());
                if(lrProjectScenarioResults.getBuildCount() > MAX_DISPLAY_BUILDS)
                {
                    continue;
                }
                lrProjectScenarioResults.incBuildCount();
                JobLrScenarioResult scenarioRunResult = runResult.getValue();
                for (GoalResult goalResult : scenarioRunResult.scenarioSlaResults) {
                    scenarioGoalResult(runNumber, lrProjectScenarioResults, goalResult);
                }

                // Join sceanrio stats
                joinSceanrioConnectionsStats(runNumber, lrProjectScenarioResults, scenarioRunResult);
                joinVUserScenarioStats(runNumber, lrProjectScenarioResults, scenarioRunResult);
                joinTransactionScenarioStats(runNumber, lrProjectScenarioResults, scenarioRunResult);
                joinDurationStats(runNumber, lrProjectScenarioResults, scenarioRunResult);

            }

        }
    }

    private void joinDurationStats(int runNumber, LrProjectScenarioResults lrProjectScenarioResults,
                                   JobLrScenarioResult scenarioRunResult) {
        long scenarioConnectionMax = scenarioRunResult.getScenarioDuration();
        if (scenarioConnectionMax != DEFAULT_SCENARIO_DURATION) {
            lrProjectScenarioResults.getDurationData().put(runNumber, scenarioConnectionMax);
        }
    }

    private void joinTransactionScenarioStats(int runNumber, LrProjectScenarioResults lrProjectScenarioResults,
                                              JobLrScenarioResult scenarioRunResult) {
        SortedMap<Integer, TreeMap<String, TreeMap<String, Integer>>> projectTransactionPerRun =
                lrProjectScenarioResults.getTransactionPerRun();
        SortedMap<String, Integer> projectTransactionSum = lrProjectScenarioResults.getTransactionSum();

        final TreeMap<String, TreeMap<String, Integer>> scenarioTransactionData =
                scenarioRunResult.transactionData;
        final TreeMap<String, Integer> scenarioTransactionSum = scenarioRunResult.transactionSum;

        if (scenarioTransactionData == null || scenarioTransactionSum == null) {
            return;
        }

        if (!scenarioTransactionData.isEmpty()) {
            //store transaction state data per run
            projectTransactionPerRun.put(runNumber, scenarioTransactionData);
            //add all summary transcation states to project level summary
            for (SortedMap.Entry<String, Integer> transactionState : scenarioTransactionSum.entrySet()) {
                int previousCount = 0;
                if (projectTransactionSum.containsKey(transactionState.getKey())) {
                    previousCount = projectTransactionSum.get(transactionState.getKey());
                }
                projectTransactionSum.put(transactionState.getKey(), previousCount + transactionState.getValue());
            }


            //add all per transcation states to project level per transaction summary
            SortedMap<String, TreeMap<String, Integer>> projectTransactionsData =
                    lrProjectScenarioResults.getTransactionData();
            for (SortedMap.Entry<String, TreeMap<String, Integer>> scenarioTransactionDataSet :
                    scenarioTransactionData.entrySet()) {
                String transactionName = scenarioTransactionDataSet.getKey();
                TreeMap<String, Integer> TransactionStateData = scenarioTransactionDataSet.getValue();
                if (!projectTransactionsData.containsKey(transactionName)) {
                    projectTransactionsData.put(transactionName, new TreeMap<String, Integer>(TransactionStateData));
                    continue;
                }

                TreeMap<String, Integer> projectTransactionState = projectTransactionsData.get(transactionName);
                for (Map.Entry<String, Integer> scenarioTransactionState : TransactionStateData.entrySet()) {
                    Integer currentValue = scenarioTransactionState.getValue();
                    projectTransactionState.put(scenarioTransactionState.getKey(), currentValue +
                            scenarioTransactionState.getValue());
                }
            }
        }
    }

    private void joinVUserScenarioStats(int runNumber, LrProjectScenarioResults lrProjectScenarioResults,
                                        JobLrScenarioResult scenarioRunResult) {
        SortedMap<Integer, TreeMap<String, Integer>> vUserPerRun = lrProjectScenarioResults.getvUserPerRun();
        if (scenarioRunResult.vUserSum != null && !scenarioRunResult.vUserSum.isEmpty()) {
            for (SortedMap.Entry<String, Integer> vUserStat : scenarioRunResult.vUserSum.entrySet()) {
                if (!vUserPerRun.containsKey(runNumber)) {
                    vUserPerRun.put(runNumber, new TreeMap<String, Integer>());
                    LrProjectScenarioResults.vUserMapInit(vUserPerRun.get(runNumber));
                }
                vUserPerRun.get(runNumber).put(vUserStat.getKey(), vUserStat.getValue());
                int previousCount = 0;
                if (lrProjectScenarioResults.getvUserSummary().containsKey(vUserStat.getKey())) {
                    previousCount = lrProjectScenarioResults.getvUserSummary().get(vUserStat.getKey());
                }
                lrProjectScenarioResults.getvUserSummary()
                        .put(vUserStat.getKey(), previousCount + vUserStat.getValue());
            }
        }
    }

    private void joinSceanrioConnectionsStats(int runNumber, LrProjectScenarioResults lrProjectScenarioResults,
                                              JobLrScenarioResult scenarioRunResult) {
        int scenarioConnectionMax = scenarioRunResult.getConnectionMax();
        if (scenarioConnectionMax != DEFAULT_CONNECTION_MAX) {
            lrProjectScenarioResults.getMaxConnectionsCount().put(runNumber, scenarioConnectionMax);
        }
    }

    private static void scenarioGoalResult(int runNumber, LrProjectScenarioResults lrProjectScenarioResults,
                                           GoalResult goalResult) {
        if(goalResult.getStatus().equals(LrTest.SLA_STATUS.NoData))
        {
            return;
        }
        switch (goalResult.getSlaGoal()) {
            case AverageThroughput:
                lrProjectScenarioResults.getAverageThroughputResults()
                        .put(runNumber, (WholeRunResult) goalResult);
                break;
            case TotalThroughput:
                lrProjectScenarioResults.getTotalThroughtputResults().put(runNumber, (WholeRunResult) goalResult);
                break;
            case AverageHitsPerSecond:
                lrProjectScenarioResults.getAverageHitsPerSecondResults()
                        .put(runNumber, (WholeRunResult) goalResult);
                break;
            case TotalHits:
                lrProjectScenarioResults.getTotalHitsResults().put(runNumber, (WholeRunResult) goalResult);
                break;
            case ErrorsPerSecond:
                lrProjectScenarioResults.getErrPerSecResults()
                        .put(runNumber, (TimeRangeResult) goalResult);
                break;
            case PercentileTRT:
                if (!lrProjectScenarioResults.getPercentileTransactionResults().containsKey(runNumber)) {
                    lrProjectScenarioResults.getPercentileTransactionResults()
                            .put(runNumber, new TreeMap<String, PercentileTransactionWholeRun>());
                }
                lrProjectScenarioResults.getTransactions()
                        .add(((PercentileTransactionWholeRun) goalResult).getName());
                lrProjectScenarioResults.getPercentileTransactionResults().get(runNumber)
                        .put(((PercentileTransactionWholeRun) goalResult).getName(),
                                (PercentileTransactionWholeRun) goalResult);
                break;
            case AverageTRT:
                if (!lrProjectScenarioResults.getAvgTransactionResponseTimeResults()
                        .containsKey(runNumber)) {
                    lrProjectScenarioResults.getAvgTransactionResponseTimeResults()
                            .put(runNumber, new TreeMap<String, AvgTransactionResponseTime>());
                }
                lrProjectScenarioResults.getTransactions()
                        .add(((AvgTransactionResponseTime) goalResult).getName());
                lrProjectScenarioResults.getAvgTransactionResponseTimeResults().get(runNumber)
                        .put(((AvgTransactionResponseTime) goalResult).getName(),
                                (AvgTransactionResponseTime) goalResult);
                break;
            default:
                break;
        }
    }

    private boolean isUpdateDataNeeded() {
//        final Run<?, ?> lastBuild = currentProject.getLastBuild();
//        if (null == lastBuild) {
//            return false;
//        }
//
//        int latestBuildNumber = lastBuild.getNumber();
//        if (latestBuildNumber == lastBuildId) {
//            return true;
//        }

        return true;
    }

//    @Override
//    public Collection<? extends Action> getProjectActions() {
//        this.projectActions.add(this);
//        this.projectActions.add(new TestResultProjectAction(currentProject));
//        return this.projectActions;
//    }

}
