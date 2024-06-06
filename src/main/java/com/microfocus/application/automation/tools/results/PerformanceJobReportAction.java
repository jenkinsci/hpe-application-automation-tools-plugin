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
