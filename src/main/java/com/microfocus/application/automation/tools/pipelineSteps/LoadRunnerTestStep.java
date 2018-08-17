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
 * Load runner pipeline step
 */
public class LoadRunnerTestStep extends AbstractStepImpl {

    private final RunFromFileBuilder runFromFileBuilder;
    private final RunResultRecorder runResultRecorder;

    /**
     * Instantiates a new Lr scenario load step.
     *
     * @param testPaths              the test paths
     * @param archiveTestResultsMode the type of archiving the user wants.
     */
    @DataBoundConstructor
    public LoadRunnerTestStep(String testPaths, String archiveTestResultsMode) {
        this.runFromFileBuilder = new RunFromFileBuilder(testPaths);
        this.runResultRecorder = new RunResultRecorder(archiveTestResultsMode);
    }

    /**
     * Gets archive run test results mode.
     *
     * @return the archive run test results mode
     */
    public String getArchiveTestResultsMode() {
        return runResultRecorder.getResultsPublisherModel().getArchiveTestResultsMode();
    }

    /**
     * Gets controller polling interval.
     *
     * @return the controller polling interval
     */
    public String getControllerPollingInterval() {
        return runFromFileBuilder.getRunFromFileModel().getControllerPollingInterval();
    }

    /**
     * Sets controller polling interval.
     *
     * @param controllerPollingInterval the controller polling interval
     */
    @DataBoundSetter
    public void setControllerPollingInterval(String controllerPollingInterval) {
        runFromFileBuilder.setControllerPollingInterval(controllerPollingInterval);
    }

    /**
     * Gets fs timeout.
     *
     * @return the fs timeout
     */
    public String getFsTimeout() {
        return runFromFileBuilder.getRunFromFileModel().getFsTimeout();
    }

    /**
     * Sets fs timeout.
     *
     * @param fsTimeout the fs timeout
     */
    @DataBoundSetter
    public void setFsTimeout(String fsTimeout) {
        runFromFileBuilder.setFsTimeout(fsTimeout);
    }

    /**
     * Gets per scenario time out.
     *
     * @return the per scenario time out
     */
    public String getPerScenarioTimeOut() {
        return runFromFileBuilder.getRunFromFileModel().getPerScenarioTimeOut();
    }

    /**
     * Sets per scenario time out.
     *
     * @param perScenarioTimeOut the per scenario time out
     */
    @DataBoundSetter
    public void setPerScenarioTimeOut(String perScenarioTimeOut) {
        runFromFileBuilder.setPerScenarioTimeOut(perScenarioTimeOut);
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
     * Gets ignore error strings.
     *
     * @return the ignore error strings
     */
    public String getIgnoreErrorStrings() {
        return runFromFileBuilder.getRunFromFileModel().getIgnoreErrorStrings();
    }

    /**
     * Sets ignore error strings.
     *
     * @param ignoreErrorStrings the ignore error strings
     */
    @DataBoundSetter
    public void setIgnoreErrorStrings(String ignoreErrorStrings) {
        runFromFileBuilder.setIgnoreErrorStrings(ignoreErrorStrings);
    }

    /**
     * Gets analysis template.
     *
     * @return the analysis template
     */
    public String getAnalysisTemplate() {
        return runFromFileBuilder.getRunFromFileModel().getAnalysisTemplate();
    }

    /**
     * Sets analysis template.
     *
     * @param analysisTemplate the analysis template
     */
    @DataBoundSetter
    public void setAnalysisTemplate(String analysisTemplate) {
        runFromFileBuilder.setAnalysisTemplate(analysisTemplate);
    }


    /**
     * Gets display controller.
     *
     * @return the display controller
     */
    public String getDisplayController() {
        return runFromFileBuilder.getRunFromFileModel().getDisplayController();
    }

    /**
     * Sets display controller.
     *
     * @param displayController the display controller
     */
    @DataBoundSetter
    public void setDisplayController(String displayController) {
        runFromFileBuilder.setDisplayController(displayController);
    }

    /**
     * Gets run from file builder.
     *
     * @return the run from file builder
     */
    public RunFromFileBuilder getRunFromFileBuilder() {
        return runFromFileBuilder;
    }

    public RunResultRecorder getRunResultRecorder() {
        return runResultRecorder;
    }

    /**
     * The type Descriptor.
     */
    @Extension
    @Symbol("loadRunnerTest")
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {
        /**
         * Instantiates a new Descriptor.
         */
        public DescriptorImpl() {
            super(LrScenarioLoadStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "loadRunnerTest";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Run LoadRunner performance scenario tests";
        }

        /**
         * Gets report archive modes.
         *
         * @return the report archive modes
         */
        public List<EnumDescription> getReportArchiveModes() {

            return ResultsPublisherModel.archiveModes;
        }

        /**
         * Do check fs tests form validation.
         *
         * @param value the value
         * @return the form validation
         */
        @SuppressWarnings("squid:S1172")
        public FormValidation doCheckFsTests(@QueryParameter String value) {
            return FormValidation.ok();
        }

        /**
         * Do check ignore error strings form validation.
         *
         * @param value the value
         * @return the form validation
         */
        @SuppressWarnings("squid:S1172")
        public FormValidation doCheckIgnoreErrorStrings(@QueryParameter String value) {

            return FormValidation.ok();
        }

        /**
         * Do check fs timeout form validation.
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

        /**
         * Do check controller polling interval form validation.
         *
         * @param value the value
         * @return the form validation
         */
        public FormValidation doCheckControllerPollingInterval(@QueryParameter String value) {
            if (StringUtils.isEmpty(value)) {
                return FormValidation.ok();
            }

            if (!StringUtils.isNumeric(value)) {
                return FormValidation.error("Controller Polling Interval must be a number");
            }

            return FormValidation.ok();
        }

        /**
         * Do check per scenario time out form validation.
         *
         * @param value the value
         * @return the form validation
         */
        public FormValidation doCheckPerScenarioTimeOut(@QueryParameter String value) {
            if (StringUtils.isEmpty(value)) {
                return FormValidation.ok();
            }

            if (!StringUtils.isNumeric(value)) {
                return FormValidation.error("Per Scenario Timeout must be a number");
            }

            return FormValidation.ok();
        }

    }

}
