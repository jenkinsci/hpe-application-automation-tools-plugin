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

import com.hp.octane.integrations.dto.executor.impl.TestingToolType;

/**
 * Constants for UFT executors jobs
 */
public class UftConstants {

    public static final String BUILD_ID_PARAMETER_NAME = "buildId";
    public static final String FULL_SCAN_PARAMETER_NAME = "Full sync";


    public static final String TESTS_TO_RUN_PARAMETER_NAME = "testsToRun";
    public static final String CHECKOUT_DIR_PARAMETER_NAME = "testsToRunCheckoutDirectory";
    public static final String TEST_RUNNER_ID_PARAMETER_NAME = "Test Runner ID";
    public static final String TEST_RUNNER_LOGICAL_NAME_PARAMETER_NAME = "Test Runner logical name";

    public static final String DISCOVERY_JOB_MIDDLE_NAME_WITH_TEST_RUNNERS = "UFT-test-discovery-job-Test-Runner-ID";
    public static final String EXECUTION_JOB_MIDDLE_NAME_WITH_TEST_RUNNERS = "UFT-test-execution-job-Test-Runner-ID";

    public static final String DISCOVERY_JOB_MIDDLE_NAME_WITH_TEST_RUNNERS_TEMPLATE = "%s-discovery-job";
    public static final String UFT_DISCOVERY_JOB_MIDDLE_NAME_WITH_TEST_RUNNERS_NEW = String.format(DISCOVERY_JOB_MIDDLE_NAME_WITH_TEST_RUNNERS_TEMPLATE, TestingToolType.UFT);
    public static final String MBT_DISCOVERY_JOB_MIDDLE_NAME_WITH_TEST_RUNNERS_NEW = String.format(DISCOVERY_JOB_MIDDLE_NAME_WITH_TEST_RUNNERS_TEMPLATE, TestingToolType.MBT);
    public static final String EXECUTION_JOB_MIDDLE_NAME_WITH_TEST_RUNNERS_TEMPLATE = "%s-test-runner";
    public static final String UFT_EXECUTION_JOB_MIDDLE_NAME_WITH_TEST_RUNNERS_NEW = String.format(EXECUTION_JOB_MIDDLE_NAME_WITH_TEST_RUNNERS_TEMPLATE, TestingToolType.UFT);
    public static final String MBT_EXECUTION_JOB_MIDDLE_NAME_WITH_TEST_RUNNERS_NEW = String.format(EXECUTION_JOB_MIDDLE_NAME_WITH_TEST_RUNNERS_TEMPLATE, TestingToolType.MBT);

    public static final String NO_USERNAME_DEFINED = "No username defined in Jenkins Configure System page";
    public static final String NO_CLIENT_ID_DEFINED = "No client ID defined in Jenkins Configure System page";

    public static final String UFT_CHECKOUT_FOLDER = "UFT_CHECKOUT_FOLDER";
    public static final String CODELESS_FOLDER_TEMPLATE = "codeless_%s";
}
