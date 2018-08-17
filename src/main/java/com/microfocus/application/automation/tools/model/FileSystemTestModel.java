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

package com.microfocus.application.automation.tools.model;

import com.microfocus.application.automation.tools.sse.common.StringUtils;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.microfocus.application.automation.tools.model.RunFromFileSystemModel.isMtbxContent;

/**
 * Represents the tests that ParallelRunner should run and the environments on which to run them.
 */
public class FileSystemTestModel extends AbstractDescribableImpl<FileSystemTestModel> {
    private String tests; // tests to run
    private List<ParallelRunnerEnvironmentModel> parallelRunnerEnvironments; // the environments to run on

    /**
     * Constructs a new FileSystemTestModel
     * @param tests the tests to be run
     * @param parallelRunnerEnvironments the environments on which to run the tests on
     */
    @DataBoundConstructor
    public FileSystemTestModel(String tests,List<ParallelRunnerEnvironmentModel> parallelRunnerEnvironments) {
        this.tests = tests;
        this.parallelRunnerEnvironments = parallelRunnerEnvironments;
    }

    /**
     * Returns the tests.
     * @return the tests
     */
    public String getTests() {
        if(this.tests == null) {
            this.tests = "\n";
        }
        else if(!this.tests.contains("\n")) {
            this.tests+="\n";
        }

        return tests;
    }

    /**
     * Adds a new line to the tests string if not present (keep the expandableTextBox expanded).
     * @param tests the tests
     */
    private void setTestsWithNewLine(String tests) {
        this.tests = tests.trim();

        if (!this.tests.contains("\n")) {
            this.tests += "\n";
        }
    }

    /**
     * Returns the environments on which the tests will run.
     * @return the parallel runner environments
     */
    public List<ParallelRunnerEnvironmentModel> getParallelRunnerEnvironments() {
        return parallelRunnerEnvironments;
    }


    /**
     * Expand the tests based on the environment variables.
     * @param envVars the environment variables
     * @return the parsed list of tests
     */
    public List<String> parseTests(EnvVars envVars) {
        String expandedFsTests = envVars.expand(this.tests);

        List<String> result = new ArrayList<>();

        if(StringUtils.isNullOrEmpty(this.tests))
            return result;

        if (isMtbxContent(expandedFsTests)) {
            result.add(expandedFsTests);
        } else {
            result = Arrays.asList(expandedFsTests.replaceAll("\r", "").split("\n"));
        }

        return result;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<FileSystemTestModel> {
        @Nonnull
        public String getDisplayName() {return "File System test model";}
    }
}
