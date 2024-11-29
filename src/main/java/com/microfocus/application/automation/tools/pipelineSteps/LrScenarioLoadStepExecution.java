/*
 *  Certain versions of software accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 *  This software was acquired by Micro Focus on September 1, 2017, and is now
 *  offered by OpenText.
 *  Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical
 *  in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the
 *  property of their respective owners.
 *  OpenText is a trademark of Open Text.
 *  __________________________________________________________________
 *  MIT License
 *
 *  Copyright 2012-2025 Open Text.
 *
 *  The only warranties for products and services of Open Text and
 *  its affiliates and licensors ("Open Text") are as may be set forth
 *  in the express warranty statements accompanying such products and services.
 *  Nothing herein should be construed as constituting an additional warranty.
 *  Open Text shall not be liable for technical or editorial errors or
 *  omissions contained herein. The information contained herein is subject
 *  to change without notice.
 *
 *  Except as specifically indicated otherwise, this document contains
 *  confidential information and a valid license is required for possession,
 *  use or copying. If this work is provided to the U.S. Government,
 *  consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 *  Computer Software Documentation, and Technical Data for Commercial Items are
 *  licensed to the U.S. Government under vendor's standard commercial license.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ___________________________________________________________________
 */
package com.microfocus.application.automation.tools.pipelineSteps;

import com.microfocus.application.automation.tools.run.RunFromFileBuilder;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;


/**
 * The Load runner pipeline step execution.
 */
public class LrScenarioLoadStepExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {

    /**
     * Logger.
     */
    private static final Logger logger = Logger
            .getLogger(LrScenarioLoadStepExecution.class.getName());

  private static final long serialVersionUID = 1L;
  @Inject
  @SuppressWarnings("squid:S3306")
  private transient LoadRunnerTestStep step;
  @StepContextParameter
  private transient TaskListener listener;
  @StepContextParameter
  private transient FilePath ws;
  @StepContextParameter
  private transient Run build;
  @StepContextParameter
  private transient Launcher launcher;

    public LrScenarioLoadStepExecution() {
        //no need for actual construction
    }

    @Override
    protected Void run() throws InterruptedException {
        listener.getLogger().println("Running LoadRunner Scenario step");
         try {
                step.getRunFromFileBuilder().perform(build, ws, launcher, listener);
            } catch (IOException e) {
                listener.fatalError("LoadRunnner scenario run stage encountered an IOException " + e);
                build.setResult(Result.FAILURE);
                return null;
         }
            HashMap<String, String> resultFilename = new HashMap<String, String>(0);
            resultFilename.put(RunFromFileBuilder.class.getName(), step.getRunFromFileBuilder().getRunResultsFileName());

            try {
                step.getRunResultRecorder().pipelinePerform(build, ws, launcher, listener, resultFilename);
            } catch (IOException e) {
                listener.fatalError("LoadRunnner scenario run result recorder stage encountered an IOException " + e);
                build.setResult(Result.FAILURE);
                return null;
            }

        return null;
    }
}