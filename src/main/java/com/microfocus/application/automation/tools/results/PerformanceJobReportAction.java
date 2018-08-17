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

import com.microfocus.application.automation.tools.results.projectparser.performance.JobLrScenarioResult;
import com.microfocus.application.automation.tools.results.projectparser.performance.LrJobResults;
import hudson.model.Action;
import hudson.model.InvisibleAction;
import hudson.model.Run;
import jenkins.tasks.SimpleBuildStep;
import net.minidev.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * Holds LoadRunner infomation on a specific Job Run / Build
 */
public class PerformanceJobReportAction extends InvisibleAction implements SimpleBuildStep.LastBuildAction {

    private Run<?, ?> build;
    private JSONObject jobDataSet;
    private LrJobResults _resultFiles;

    /**
     * Instantiates a new Performance job report action.
     *
     * @param build       the build
     * @param resultFiles the result dataset
     */
    public PerformanceJobReportAction(Run<?, ?> build, LrJobResults resultFiles) {
        this.build = build;
        this._resultFiles = resultFiles;
    }

    /**
     * Merge results of several runs - especially useful in pipeline jobs with multiple LR steps
     *
     * @param resultFiles the result files
     */
    public void mergeResults(LrJobResults resultFiles)
    {
        for(JobLrScenarioResult scenarioResult : resultFiles.getLrScenarioResults().values())
        {
            this._resultFiles.addScenario(scenarioResult);
        }
    }

    /**
     * Gets lr result build dataset.
     *
     * @return the lr result build dataset
     */
    public LrJobResults getLrResultBuildDataset() {
        return _resultFiles;
    }

    /**
     * Gets json data.
     *
     * @return the json data
     */
    public JSONObject getJsonData()
    {
        return jobDataSet;
    }


    @Override
    public Collection<? extends Action> getProjectActions() {
        List<PerformanceProjectAction> projectActions = new ArrayList<PerformanceProjectAction>();
        projectActions.add(new PerformanceProjectAction(build.getParent()));
        return projectActions;
    }
}
