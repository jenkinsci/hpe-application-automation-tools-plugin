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

package com.microfocus.application.automation.tools.pipelineSteps;

import com.microfocus.application.automation.tools.run.RunFromFileBuilder;
import com.microfocus.application.automation.tools.run.UftOctaneUtils;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

import javax.annotation.Nonnull;
import java.util.HashMap;


/**
 * The UFT pipeline step execution.
 */
public  class UftScenarioLoadStepExecution extends SynchronousNonBlockingStepExecution<Void> {

    private static final long serialVersionUID = 1L;


    private transient final UftScenarioLoadStep step;


    private transient TaskListener listener;


    private transient FilePath ws;


    private transient Run build;


    private transient Launcher launcher;

    protected UftScenarioLoadStepExecution(@Nonnull StepContext context, UftScenarioLoadStep step) {
        super(context);
        this.step = step;
    }


    @Override
    protected Void run() throws Exception{
        ws = getContext().get(FilePath.class);
        listener = getContext().get(TaskListener.class);
        build = getContext().get(Run.class);
        launcher = getContext().get(Launcher.class);

        listener.getLogger().println("Running UftScenarioLoadStepExecution");

        step.getRunFromFileBuilder().perform(build, ws, launcher, listener);

        HashMap<String, String> resultFilename = new HashMap<String, String>(0);
        resultFilename.put(RunFromFileBuilder.class.getName(), step.getRunFromFileBuilder().getRunResultsFileName());

        step.getRunResultRecorder().pipelinePerform(build, ws, launcher, listener, resultFilename);

        return null;
    }
}
