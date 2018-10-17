/*
 *
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * © Copyright 2012-2018 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors (“Micro Focus”) are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 *
 */


package com.microfocus.application.automation.tools.octane.testrunner;

import com.hp.octane.integrations.executor.TestsToRunConverterResult;
import com.hp.octane.integrations.executor.TestsToRunConvertersFactory;
import com.hp.octane.integrations.executor.TestsToRunFramework;
import com.microfocus.application.automation.tools.model.EnumDescription;
import com.microfocus.application.automation.tools.octane.executor.UftConstants;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.List;

/**
 * Builder for available frameworks for converting
 */
public class TestsToRunConverterBuilder extends Builder {

    private TestsToRunConverterModel framework;

    private final String TESTS_TO_RUN_PARAMETER = "testsToRun";
    private final String TESTS_TO_RUN_CONVERTED_PARAMETER = "testsToRunConverted";

    private final String DEFAULT_EXECUTING_DIRECTORY = "${workspace}";
    private final String CHECKOUT_DIRECTORY_PARAMETER = "testsToRunCheckoutDirectory";

    @DataBoundConstructor
    public TestsToRunConverterBuilder(String framework) {
        this.framework = new TestsToRunConverterModel(framework);
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        ParametersAction parameterAction = build.getAction(ParametersAction.class);
        String rawTests = null;
        String executingDirectory = DEFAULT_EXECUTING_DIRECTORY;
        if (parameterAction != null) {
            ParameterValue suiteIdParameter = parameterAction.getParameter(UftConstants.SUITE_ID_PARAMETER_NAME);
            if (suiteIdParameter != null) {
                printToConsole(listener, UftConstants.SUITE_ID_PARAMETER_NAME + " : " + suiteIdParameter.getValue());
            }
            ParameterValue suiteRunIdParameter = parameterAction.getParameter(UftConstants.SUITE_RUN_ID_PARAMETER_NAME);
            if (suiteRunIdParameter != null) {
                printToConsole(listener, UftConstants.SUITE_RUN_ID_PARAMETER_NAME + " : " + suiteRunIdParameter.getValue());
            }

            ParameterValue testsParameter = parameterAction.getParameter(TESTS_TO_RUN_PARAMETER);
            if (testsParameter != null && testsParameter.getValue() instanceof String) {
                rawTests = (String) testsParameter.getValue();
                printToConsole(listener, TESTS_TO_RUN_PARAMETER + " found with value : " + rawTests);
            }

            ParameterValue checkoutDirParameter = parameterAction.getParameter(CHECKOUT_DIRECTORY_PARAMETER);
            if (checkoutDirParameter != null) {
                if (testsParameter.getValue() instanceof String && StringUtils.isNotEmpty((String) checkoutDirParameter.getValue())) {
                    executingDirectory = (String) checkoutDirParameter.getValue();//"%" + CHECKOUT_DIRECTORY_PARAMETER + "%";
                    printToConsole(listener, CHECKOUT_DIRECTORY_PARAMETER + " parameter found with value : " + executingDirectory);
                } else {
                    printToConsole(listener, CHECKOUT_DIRECTORY_PARAMETER + " parameter found, but its value is empty or its type is not String. Using default value.");
                }
            }
            printToConsole(listener, "checkout directory : " + executingDirectory);
        }
        if (StringUtils.isEmpty(rawTests)) {
            printToConsole(listener, TESTS_TO_RUN_PARAMETER + " is not found or has empty value. Skipping.");
            return true;
        }
        if (StringUtils.isEmpty(framework.getFramework())) {
            printToConsole(listener, "No framework is selected. Skipping.");
            return true;
        }

        TestsToRunFramework testsToRunFramework = TestsToRunFramework.fromValue(framework.getFramework());
        TestsToRunConverterResult convertResult = TestsToRunConvertersFactory.createConverter(testsToRunFramework).convert(rawTests, executingDirectory);
        printToConsole(listener, "Found #tests : " + convertResult.getTestsData().size());
        printToConsole(listener, TESTS_TO_RUN_CONVERTED_PARAMETER + " = " + convertResult.getConvertedTestsString());

        VariableInjectionAction via = new VariableInjectionAction(TESTS_TO_RUN_CONVERTED_PARAMETER, convertResult.getConvertedTestsString());
        build.addAction(via);
        return true;
    }


    public TestsToRunConverterModel getTestsToRunConverterModel() {
        return framework;
    }

    private void printToConsole(BuildListener listener, String msg) {
        listener.getLogger().println(this.getClass().getSimpleName() + " : " + msg);
    }


    @Extension
    public static class Descriptor extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;//FreeStyleProject.class.isAssignableFrom(jobType);
        }

        @Override
        public String getDisplayName() {
            return "Micro Focus ALM Octane testing framework converter";
        }

        /**
         * Gets report archive modes.
         *
         * @return the report archive modes
         */
        public List<EnumDescription> getFrameworks() {

            return TestsToRunConverterModel.Frameworks;
        }
    }
}
