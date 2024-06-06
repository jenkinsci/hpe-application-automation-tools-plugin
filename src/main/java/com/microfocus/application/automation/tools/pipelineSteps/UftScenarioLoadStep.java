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

package com.microfocus.application.automation.tools.pipelineSteps;

import com.google.common.collect.ImmutableSet;
import com.microfocus.application.automation.tools.model.EnumDescription;
import com.microfocus.application.automation.tools.model.ResultsPublisherModel;
import com.microfocus.application.automation.tools.model.RunFromFileSystemModel;
import com.microfocus.application.automation.tools.results.RunResultRecorder;
import com.microfocus.application.automation.tools.run.RunFromFileBuilder;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.*;
import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * UFT pipeline step
 */
public class UftScenarioLoadStep extends Step {

    private RunFromFileBuilder runFromFileBuilder;
    private RunResultRecorder runResultRecorder;

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

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new UftScenarioLoadStepExecution(stepContext, this);
    }

    @DataBoundSetter
    private void setRunFromFileBuilder(RunFromFileBuilder runFromFileBuilder){
        this.runFromFileBuilder = runFromFileBuilder;
    }

    @DataBoundSetter
    public void setRunResultRecorder(RunResultRecorder runResultRecorder){
        this.runResultRecorder = runResultRecorder;
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

    @DataBoundSetter
    public void setFsReportPath(String fsReportPath) {
        runFromFileBuilder.setFsReportPath(fsReportPath);
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
     * Get the report path.
     * @return the report path
     */
    public String getFsReportPath() {
        return runFromFileBuilder.getRunFromFileModel().getFsReportPath();
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
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, TaskListener.class, FilePath.class);
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
