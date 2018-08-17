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

package com.microfocus.application.automation.tools.pipelineSteps;

import com.microfocus.application.automation.tools.model.EnumDescription;
import com.microfocus.application.automation.tools.model.ResultsPublisherModel;
import com.microfocus.application.automation.tools.model.RunFromFileSystemModel;
import com.microfocus.application.automation.tools.results.RunResultRecorder;
import com.microfocus.application.automation.tools.run.RunFromFileBuilder;
import hudson.Extension;
import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

/**
 * UFT pipeline step
 */
public class UftScenarioLoadStep extends AbstractStepImpl {

    private final RunFromFileBuilder runFromFileBuilder;
    private final RunResultRecorder runResultRecorder;

    /**
     * Instantiates a new UFT scenario load step.
     *
     * @param testPaths              the test paths
     * @param archiveTestResultsMode the type of archiving the user wants.
     */
    @DataBoundConstructor
    public UftScenarioLoadStep(String testPaths, String archiveTestResultsMode) {
        this.runFromFileBuilder = new RunFromFileBuilder(testPaths);
        this.runResultRecorder = new RunResultRecorder(archiveTestResultsMode);
    }

    /**
     * Gets archive test result mode.
     *
     * @return the archive run test results mode
     */
    public String getArchiveTestResultsMode() {
        return runResultRecorder.getResultsPublisherModel().getArchiveTestResultsMode();
    }

    /**
     * Gets fsTimeout
     *
     * @return fsTimeout value
     */
    public String getFsTimeout() {
        return runFromFileBuilder.getRunFromFileModel().getFsTimeout();
    }

    /**
     * Sets fsTimeout value
     *
     * @param fsTimeout the fsTimeout value
     */
    @DataBoundSetter
    public void setFsTimeout(String fsTimeout) {
        runFromFileBuilder.setFsTimeout(fsTimeout);
    }

    /**
     * Gets fsUftRunMode
     *
     * @return fsUftRunMode value
     */
    public String getFsUftRunMode() {
        return runFromFileBuilder.getRunFromFileModel().getFsUftRunMode();
    }

    /**
     * Sets fsUftRunMode value
     *
     * @param fsUftRunMode the fsUftRunMode value
     */
    @DataBoundSetter
    public void setFsUftRunMode(String fsUftRunMode) {
        runFromFileBuilder.setFsUftRunMode(fsUftRunMode);
    }

    /**
     * Gets fsUftRunModes
     *
     * @return fsUftRunModes value
     */
    public List<EnumDescription> getFsUftRunModes() {
        return RunFromFileSystemModel.fsUftRunModes;
    }

    /**
     * Gets test paths.
     *
     * @return the test paths
     */
    public String getTestPaths() {
        return runFromFileBuilder.getRunFromFileModel().getFsTests();
    }

    /**
     * Gets run from file builder.
     *
     * @return the run from file builder
     */
    public RunFromFileBuilder getRunFromFileBuilder() {
        return runFromFileBuilder;
    }

    /**
     * Gets run result builder
     *
     * @return the run result builder
     */
    public RunResultRecorder getRunResultRecorder() {
        return runResultRecorder;
    }

    /**
     * The type Descriptor.
     */
    @Extension
    @Symbol("UftScenarioLoad")
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {

        /**
         * Instantiates a new Descriptor.
         */
        public DescriptorImpl() {
            super(UftScenarioLoadStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "uftScenarioLoad";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Run UFT scenario";
        }

        /**
         * Gets fs runModes
         *
         * @return the fs runModes
         */
        public List<EnumDescription> getFsUftRunModes() { return RunFromFileSystemModel.fsUftRunModes; }

        /**
         * Gets report archive modes.
         *
         * @return the report archive modes
         */
        public List<EnumDescription> getReportArchiveModes() {

            return ResultsPublisherModel.archiveModes;
        }

        /**
         * Do check test paths validation.
         *
         * @param value the value
         * @return the form validation
         */
        public FormValidation doCheckTestPaths(@QueryParameter String value) {

            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Test path must be set");
            }

            return FormValidation.ok();
        }

        /**
         * Do check fs tests form validation.
         *
         * @param value the value
         * @return the form validation
         */
        public FormValidation doCheckFsTests(@QueryParameter String value) {

            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Test path must be set");
            }

            return FormValidation.ok();
        }

        /**
         * Do check fs timeout validation
         *
         * @param value the value
         * @return the form validation
         */
        public FormValidation doCheckFsTimeout(@QueryParameter String value) {
            if (StringUtils.isEmpty(value)) {
                return FormValidation.ok();
            }

            String val1 = value.trim();
            if (val1.length() > 0 && val1.charAt(0) == '-') {
                val1 = val1.substring(1);
            }

            if (!StringUtils.isNumeric(val1) && !Objects.equals(val1, "")) {
                return FormValidation.error("Timeout name must be a number");
            }

            return FormValidation.ok();
        }

    }

}
