/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2021 Micro Focus or one of its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.octane.executor;

/**
 * Constants for UFT executors jobs
 */
public class UftConstants {

    public static final String SUITE_ID_PARAMETER_NAME = "suiteId";
    public static final String SUITE_RUN_ID_PARAMETER_NAME = "suiteRunId";
    public static final String BUILD_ID_PARAMETER_NAME = "buildId";
    public static final String EXECUTION_ID_PARAMETER_NAME = "executionId";
    public static final String FULL_SCAN_PARAMETER_NAME = "Full sync";
    public static final String ADD_GLOBAL_PARAMETERS_TO_TESTS_PARAM = "ADD_GLOBAL_PARAMETERS_TO_TESTS";
    public static final String OCTANE_WORKSPACE_PARAMETER_NAME = "octaneWorkspaceId";
    public static final String OCTANE_SPACE_PARAMETER_NAME = "octaneSpaceId";


    public static final String TESTS_TO_RUN_PARAMETER_NAME = "testsToRun";
    public static final String CHECKOUT_DIR_PARAMETER_NAME = "testsToRunCheckoutDirectory";
    public static final String TEST_RUNNER_ID_PARAMETER_NAME = "Test Runner ID";
    public static final String TEST_RUNNER_LOGICAL_NAME_PARAMETER_NAME = "Test Runner logical name";

    public static final String DISCOVERY_JOB_MIDDLE_NAME_WITH_TEST_RUNNERS = "UFT-test-discovery-job-Test-Runner-ID";
    public static final String EXECUTION_JOB_MIDDLE_NAME_WITH_TEST_RUNNERS = "UFT-test-execution-job-Test-Runner-ID";

    public static final String DISCOVERY_JOB_MIDDLE_NAME_WITH_TEST_RUNNERS_NEW = "UFT-discovery-job";
    public static final String EXECUTION_JOB_MIDDLE_NAME_WITH_TEST_RUNNERS_NEW = "UFT-test-runner";

    public static final String NO_USERNAME_DEFINED = "No username defined in Jenkins Configure System page";
    public static final String NO_CLIENT_ID_DEFINED = "No client ID defined in Jenkins Configure System page";

    public static final String UFT_CHECKOUT_FOLDER = "UFT_CHECKOUT_FOLDER";
}
