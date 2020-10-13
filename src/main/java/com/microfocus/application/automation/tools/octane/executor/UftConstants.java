/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2019 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors ("Micro Focus") are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 */
package com.microfocus.application.automation.tools.octane.executor;

/**
 * Constants for UFT executors jobs
 */
public class UftConstants {

    public static final String SUITE_ID_PARAMETER_NAME = "suiteId";
    public static final String SUITE_RUN_ID_PARAMETER_NAME = "suiteRunId";
    public static final String FULL_SCAN_PARAMETER_NAME = "Full sync";

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
}
