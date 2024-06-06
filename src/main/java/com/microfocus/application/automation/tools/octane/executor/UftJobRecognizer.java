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

package com.microfocus.application.automation.tools.octane.executor;

import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import hudson.model.FreeStyleProject;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import jenkins.model.Jenkins;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;


/***
 * Recognizer for UFT related jobs
 */
public class UftJobRecognizer {

    private static Logger logger = SDKBasedLoggerProvider.getLogger(UftJobRecognizer.class);

    /**
     * Check if current job is EXECUTOR job
     *
     * @param job
     * @return
     */
    public static boolean isExecutorJob(FreeStyleProject job) {
        return (job.getName().startsWith(UftConstants.EXECUTION_JOB_MIDDLE_NAME_WITH_TEST_RUNNERS) ||
                job.getName().startsWith(UftConstants.UFT_EXECUTION_JOB_MIDDLE_NAME_WITH_TEST_RUNNERS_NEW) ||
                job.getName().startsWith(UftConstants.MBT_EXECUTION_JOB_MIDDLE_NAME_WITH_TEST_RUNNERS_NEW));
    }

    /**
     * Check if current job is discovery job
     *
     * @param job
     * @return
     */
    public static boolean isDiscoveryJob(FreeStyleProject job) {
        return (job.getName().startsWith(UftConstants.DISCOVERY_JOB_MIDDLE_NAME_WITH_TEST_RUNNERS) ||
                job.getName().startsWith(UftConstants.UFT_DISCOVERY_JOB_MIDDLE_NAME_WITH_TEST_RUNNERS_NEW) ||
                job.getName().startsWith(UftConstants.MBT_DISCOVERY_JOB_MIDDLE_NAME_WITH_TEST_RUNNERS_NEW));
    }

    /**
     * Extract executor id from the job
     *
     * @param job
     * @return
     */
    public static String getExecutorId(FreeStyleProject job) {
        ParametersDefinitionProperty parameters = job.getProperty(ParametersDefinitionProperty.class);
        ParameterDefinition pd = parameters.getParameterDefinition(UftConstants.TEST_RUNNER_ID_PARAMETER_NAME);
        if (pd != null) {
            return (String) pd.getDefaultParameterValue().getValue();
        } else {
            return null;
        }
    }

    /**
     * Extract executor logical name from the job
     *
     * @param job
     * @return
     */
    public static String getExecutorLogicalName(FreeStyleProject job) {
        ParametersDefinitionProperty parameters = job.getProperty(ParametersDefinitionProperty.class);
        ParameterDefinition pd = parameters.getParameterDefinition(UftConstants.TEST_RUNNER_LOGICAL_NAME_PARAMETER_NAME);
        if (pd != null) {
            return (String) pd.getDefaultParameterValue().getValue();
        } else {
            return null;
        }
    }

    public static void deleteExecutionJobByExecutorIfNeverExecuted(String executorToDelete) {
        List<FreeStyleProject> jobs = Jenkins.getInstanceOrNull().getAllItems(FreeStyleProject.class);
        for (FreeStyleProject proj : jobs) {
            if (UftJobRecognizer.isExecutorJob(proj) && isJobMatchExecutor(executorToDelete, proj)
                    && proj.getLastBuild() == null && !proj.isBuilding() && !proj.isInQueue()) {
                try {
                    logger.warn(String.format("Job '%s' is going to be deleted since matching executor in Octane was deleted and this job was never executed and has no history.", proj.getName()));
                    proj.delete();
                } catch (IOException | InterruptedException e) {
                    logger.error("Failed to delete job  " + proj.getName() + " : " + e.getMessage());
                }
            }
        }
    }

    private static Boolean isJobMatchExecutor(String executorToFind, FreeStyleProject proj) {
        String executorId = UftJobRecognizer.getExecutorId(proj);
        String executorLogicalName = UftJobRecognizer.getExecutorLogicalName(proj);
        return (executorId != null && executorId.equals(executorToFind)) ||
                (executorLogicalName != null && executorLogicalName.equals(executorToFind));
    }

    /**
     * Delete discovery job that related to specific executor in Octane
     *
     * @param executorToDelete
     */
    public static void deleteDiscoveryJobByExecutor(String executorToDelete) {

        List<FreeStyleProject> jobs = Jenkins.getInstanceOrNull().getAllItems(FreeStyleProject.class);
        for (FreeStyleProject proj : jobs) {
            if (UftJobRecognizer.isDiscoveryJob(proj) && isJobMatchExecutor(executorToDelete, proj)) {
                if (proj.isBuilding()) {
                    proj.getLastBuild().getExecutor().interrupt();
                    CIPluginSDKUtils.doWait(10000); //wait before deleting the job, so Jenkins will be able to complete some IO actions
                } else if (proj.isInQueue()) {
                    Jenkins.getInstanceOrNull().getQueue().cancel(proj);
                    CIPluginSDKUtils.doWait(10000); //wait before deleting the job, so Jenkins will be able to complete some IO actions
                }

                try {
                    logger.warn(String.format("Job '%s' is going to be deleted since matching executor in Octane was deleted", proj.getName()));
                    proj.delete();
                } catch (Exception e) {
                    logger.error("Failed to delete job  " + proj.getName() + " : " + e.getMessage());
                }
            }
        }
    }
}
