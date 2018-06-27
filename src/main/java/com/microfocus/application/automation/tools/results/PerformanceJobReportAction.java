/*
 * © Copyright 2013 EntIT Software LLC
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright (c) 2018 Micro Focus Company, L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
