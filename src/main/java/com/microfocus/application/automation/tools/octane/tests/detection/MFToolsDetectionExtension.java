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
package com.microfocus.application.automation.tools.octane.tests.detection;

import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import com.microfocus.application.automation.tools.octane.model.processors.projects.JobProcessorFactory;
import com.microfocus.application.automation.tools.octane.tests.HPRunnerType;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.tasks.Builder;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Recognize execution of OpenText Tools
 */
@Extension
public class MFToolsDetectionExtension extends ResultFieldsDetectionExtension {

    private static Logger logger = SDKBasedLoggerProvider.getLogger(MFToolsDetectionExtension.class);
    private static final String STORMRUNNER_LOAD_TEST_RUNNER_CLASS = "StormTestRunner";
    private static final String STORMRUNNER_TEST_RUN_TEST_RUNNER_CLASS = "TestRunBuilder";
    private static final String PERFORMANCE_CENTER_TEST_RUNNER_CLASS = "PcBuilder";
    private static final String RUN_FROM_FILE_BUILDER = "RunFromFileBuilder";
    private static final String RUN_FROM_ALM_BUILDER = "RunFromAlmBuilder";

    private static final String UFT = "UFT";
    public static final String UFT_MBT = "MBT";
    private static final String STORMRUNNER_LOAD = "StormRunner Load";
    private static final String LOAD_RUNNER = "LoadRunner";
    private static final String PERFORMANCE_CENTER_RUNNER = "Performance Center";
    private static final String PERFORMANCE_TEST_TYPE = "Performance";


    private static final String PERFORMANCE_REPORT = "PerformanceReport";
    private static final String TRANSACTION_SUMMARY = "TransactionSummary";

    private static Map<String, HPRunnerType> builder2RunnerType = new HashMap<>();
    private static Map<HPRunnerType, ResultFields> runnerType2ResultFields = new HashMap<>();

    static {
        builder2RunnerType.put(STORMRUNNER_LOAD_TEST_RUNNER_CLASS, HPRunnerType.StormRunnerLoad);
        builder2RunnerType.put(STORMRUNNER_TEST_RUN_TEST_RUNNER_CLASS, HPRunnerType.StormRunnerLoad);
        builder2RunnerType.put(RUN_FROM_FILE_BUILDER, HPRunnerType.UFT);
        builder2RunnerType.put(RUN_FROM_ALM_BUILDER, HPRunnerType.UFT);
        builder2RunnerType.put(PERFORMANCE_CENTER_TEST_RUNNER_CLASS, HPRunnerType.PerformanceCenter);

        runnerType2ResultFields.put(HPRunnerType.PerformanceCenter, new ResultFields(null, PERFORMANCE_CENTER_RUNNER, null, PERFORMANCE_TEST_TYPE));
        runnerType2ResultFields.put(HPRunnerType.UFT, new ResultFields(UFT, UFT, null));
        runnerType2ResultFields.put(HPRunnerType.UFT_MBT, new ResultFields(UFT_MBT, null, null));
        runnerType2ResultFields.put(HPRunnerType.StormRunnerLoad, new ResultFields(null, STORMRUNNER_LOAD, null));
        runnerType2ResultFields.put(HPRunnerType.LoadRunner, new ResultFields(null, LOAD_RUNNER, null));
    }

    /**
     * Detect result fields for ALM Octane tests
     *
     * @param build
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    @Override
    public ResultFields detect(Run<?, ?> build) throws IOException, InterruptedException {
        HPRunnerType runnerType = getRunnerType(build);
        return runnerType2ResultFields.get(runnerType);
    }

    /**
     * Get MF runner type from run
     *
     * @param run
     * @return
     */
    public static HPRunnerType getRunnerType(Run run) {
        HPRunnerType hpRunnerType = HPRunnerType.NONE;
        ParametersAction parameterAction = run.getAction(ParametersAction.class);
        if (JobProcessorFactory.WORKFLOW_RUN_NAME.equals(run.getClass().getName())) {

            ParameterValue runnerTypePv = parameterAction != null ? parameterAction.getParameter(HPRunnerType.class.getSimpleName()) : null;
            if (runnerTypePv != null) {
                hpRunnerType = HPRunnerType.valueOf((String) runnerTypePv.getValue());
            }
        } else {
            List<Builder> builders = JobProcessorFactory.getFlowProcessor(run.getParent()).tryGetBuilders();
            if (builders != null) {
                for (Builder builder : builders) {
                    String builderName = builder.getClass().getSimpleName();
                    if (builder2RunnerType.containsKey(builderName)) {
                        hpRunnerType = builder2RunnerType.get(builderName);
                        break;
                    }
                }
            }
        }

        if (hpRunnerType == HPRunnerType.UFT) {
            ParameterValue octaneFramework = parameterAction != null ? parameterAction.getParameter("octaneTestRunnerFramework") : null;
            if(octaneFramework!=null && octaneFramework.getValue().toString().equalsIgnoreCase("MBT")){
                hpRunnerType = HPRunnerType.UFT_MBT;
            }
        }
        if (hpRunnerType == HPRunnerType.UFT && isLoadRunnerProject(run)) {
            hpRunnerType = HPRunnerType.LoadRunner;
        }
        return hpRunnerType;
    }

    private static boolean isLoadRunnerProject(Run run) {
        if (run.getRootDir() != null) {
            try {
                FilePath performanceReportFolder = new FilePath(run.getRootDir()).child(PERFORMANCE_REPORT);
                FilePath transactionSummaryFolder = new FilePath(run.getRootDir()).child(TRANSACTION_SUMMARY);
                return performanceReportFolder.exists() &&
                        performanceReportFolder.isDirectory() &&
                        transactionSummaryFolder.exists() &&
                        transactionSummaryFolder.isDirectory();
            } catch (IOException | InterruptedException e) {
                logger.error("Failed to check isLoadRunnerProject :" + e.getMessage());
            }
        }
        return false;
    }

}
