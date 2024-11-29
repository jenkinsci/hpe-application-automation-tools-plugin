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
package com.microfocus.application.automation.tools.run;

import com.hp.octane.integrations.OctaneSDK;
import com.microfocus.application.automation.tools.octane.executor.UftConstants;
import com.microfocus.application.automation.tools.octane.model.processors.projects.JobProcessorFactory;
import com.microfocus.application.automation.tools.octane.tests.HPRunnerType;
import hudson.model.*;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class UftOctaneUtils {

    private UftOctaneUtils(){
        //for codeclimate
    }

    /**
     * This step is important for integration with Octane when job is executed as workflow job.
     * Our plugin can recognize UFT build step when its executed in context of freeStyle job, but its not possible to do it
     * when this step executed in workflow job.
     * So , in this method we add parameter of RunnerType.UFT to sign this job as UFT runner.
     * @param build
     * @param listener
     */
    public static void setUFTRunnerTypeAsParameter(@Nonnull Run<?, ?> build, @Nonnull TaskListener listener) {
        if (JobProcessorFactory.WORKFLOW_RUN_NAME.equals(build.getClass().getName()) && OctaneSDK.hasClients()) {
            listener.getLogger().println("Set HPRunnerType = HPRunnerType.UFT");
            ParametersAction parameterAction = build.getAction(ParametersAction.class);
            List<ParameterValue> newParams = (parameterAction != null) ? new ArrayList<>(parameterAction.getAllParameters()) : new ArrayList<>();
            newParams.add(new StringParameterValue(HPRunnerType.class.getSimpleName(), HPRunnerType.UFT.name()));
            ParametersAction newParametersAction = new ParametersAction(newParams);
            build.addOrReplaceAction(newParametersAction);

            if (parameterAction == null || parameterAction.getParameter(UftConstants.UFT_CHECKOUT_FOLDER) == null) {
                listener.getLogger().println("NOTE : If you need to integrate test results with an ALM Octane pipeline, and tests are located outside of the job workspace, define a parameter  " + UftConstants.UFT_CHECKOUT_FOLDER +
                        " with the path to the repository root in the file system. This enables ALM Octane to display the test name, rather than the full path to your test.");
                listener.getLogger().println("");
            }
        }
    }
}
