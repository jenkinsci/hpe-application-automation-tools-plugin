
/*
 * © Copyright 2013 EntIT Software LLC
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright (c) 2018 Micro Focus Company, L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ___________________________________________________________________
 *
 */

package com.hpe.application.automation.tools.pipelineSteps;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;
import com.hpe.application.automation.tools.model.*;
import com.hpe.application.automation.tools.model.AlmServerSettingsModel;
import com.hpe.application.automation.tools.results.RunResultRecorder;
import com.hpe.application.automation.tools.run.SseBuilder;
import com.hpe.application.automation.tools.settings.AlmServerSettingsBuilder;
import com.hpe.application.automation.tools.model.CdaDetails;
import com.hpe.application.automation.tools.model.EnumDescription;
import com.hpe.application.automation.tools.model.ResultsPublisherModel;
import com.hpe.application.automation.tools.model.SseModel;
import com.hpe.application.automation.tools.model.SseProxySettings;
import hudson.Extension;
import hudson.Util;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.util.List;

public class SseBuildAndPublishStep extends AbstractStepImpl {

    private SseBuilder sseBuilder;
    private RunResultRecorder runResultRecorder;

    /**
     * Get the ssebuilder
     * @return sseBuilder
     */
    public SseBuilder getSseBuilder() {
        return sseBuilder;
    }

    /**
     * Get the runResultRecorder
     * @return runResultRecorder
     */
    public RunResultRecorder getRunResultRecorder() {
        return runResultRecorder;
    }

    @DataBoundConstructor
    public SseBuildAndPublishStep(String almServerName,
                      String almProject,
                      String credentialsId,
                      String almDomain,
                      String runType,
                      String almEntityId,
                      String timeslotDuration,
                      String archiveTestResultsMode) {

        sseBuilder = new SseBuilder(almServerName,
                almProject,
                credentialsId,
                almDomain,
                runType,
                almEntityId,
                timeslotDuration);

        runResultRecorder = new RunResultRecorder(archiveTestResultsMode);
    }

    public String getAlmServerName() {
        return sseBuilder.getAlmServerName();
    }

    public String getAlmProject() {
        return sseBuilder.getAlmProject();
    }

    public String getCredentialsId() {
        return sseBuilder.getCredentialsId();
    }

    public String getAlmDomain() {
        return sseBuilder.getAlmDomain();
    }

    public String getRunType() {
        return sseBuilder.getRunType();
    }

    public String getAlmEntityId() {
        return sseBuilder.getAlmEntityId();
    }

    public String getTimeslotDuration() {
        return sseBuilder.getTimeslotDuration();
    }

    public String getArchiveTestResultsMode() {
        return runResultRecorder.getResultsPublisherModel().getArchiveTestResultsMode();
    }

    @DataBoundSetter
    public void setDescription(String description) {
        sseBuilder.setDescription(description);
    }

    public String getDescription() {
        return sseBuilder.getDescription();
    }

    @DataBoundSetter
    public void setPostRunAction(String postRunAction) {
        sseBuilder.setPostRunAction(postRunAction);
    }

    public String getPostRunAction() {
        return sseBuilder.getPostRunAction();
    }

    @DataBoundSetter
    public void setEnvironmentConfigurationId(String environmentConfigurationId) {
        sseBuilder.setEnvironmentConfigurationId(environmentConfigurationId);
    }

    public String getEnvironmentConfigurationId() {
        return sseBuilder.getEnvironmentConfigurationId();
    }

    @DataBoundSetter
    public void setCdaDetails(CdaDetails cdaDetails) {
        sseBuilder.setCdaDetails(cdaDetails);
    }

    public CdaDetails getCdaDetails() {
        return sseBuilder.getCdaDetails();
    }

    @DataBoundSetter
    public void setProxySettings(SseProxySettings proxySettings) {
        sseBuilder.setProxySettings(proxySettings);
    }

    public SseProxySettings getProxySettings() {
        return sseBuilder.getProxySettings();
    }

    // This indicates to Jenkins that this is an implementation of an extension point
    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {
        private final int minimumDurationTime = 30;

        /**
         * Instantiates a new Descriptor.
         */
        public DescriptorImpl() {
            super(SseBuilderPublishResultStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "sseBuildAndPublish";
        }

        @Override
        public String getDisplayName() {
            return "Execute Micro Focus tests using Micro Focus ALM Lab Management and Publish Micro Focus tests result";
        }

        public boolean hasAlmServers() {

            return Hudson.getInstance().getDescriptorByType(
                    AlmServerSettingsBuilder.DescriptorImpl.class).hasAlmServers();
        }

        public AlmServerSettingsModel[] getAlmServers() {
            return Hudson.getInstance().getDescriptorByType(
                    AlmServerSettingsBuilder.DescriptorImpl.class).getInstallations();
        }

        public FormValidation doCheckTimeslotDuration(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Timeslot duration must be set");
            }

            String val1 = value.trim();

            if (!StringUtils.isNumeric(val1)) {
                return FormValidation.error("Timeslot duration must be a number");
            }

            if (Integer.valueOf(val1) < minimumDurationTime) {
                return FormValidation.error("Timeslot duration must be higher than " + minimumDurationTime);
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckAlmDomain(@QueryParameter String value) {

            FormValidation ret = FormValidation.ok();
            if (StringUtils.isBlank(value)) {
                ret = FormValidation.error("Domain must be set");
            }

            return ret;
        }

        public FormValidation doCheckAlmProject(@QueryParameter String value) {

            FormValidation ret = FormValidation.ok();
            if (StringUtils.isBlank(value)) {
                ret = FormValidation.error("Project must be set");
            }

            return ret;
        }

        public FormValidation doCheckAlmEntityId(@QueryParameter String value) {

            FormValidation ret = FormValidation.ok();
            if (StringUtils.isBlank(value)) {
                ret = FormValidation.error("Entity ID must be set.");
            }

            return ret;
        }

        public List<EnumDescription> getRunTypes() {

            return SseModel.getRunTypes();
        }

        public List<EnumDescription> getPostRunActions() {

            return SseModel.getPostRunActions();
        }

        public List<EnumDescription> getDeploymentActions() {

            return CdaDetails.getDeploymentActions();
        }

        public static List<EnumDescription> getDeprovisioningActions() {

            return CdaDetails.getDeprovisioningActions();
        }

        /**
         * To fill in the credentials drop down list which's field is 'credentialsId'.
         * This method's name works with tag <c:select/>.
         */
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item project,
                                                     @QueryParameter String credentialsId) {

            if (project == null || !project.hasPermission(Item.CONFIGURE)) {
                return new StandardUsernameListBoxModel().includeCurrentValue(credentialsId);
            }
            return new StandardUsernameListBoxModel()
                    .includeEmptyValue()
                    .includeAs(
                            project instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task) project) : ACL.SYSTEM,
                            project,
                            StandardUsernamePasswordCredentials.class,
                            URIRequirementBuilder.create().build())
                    .includeCurrentValue(credentialsId);
        }

        /**
         * To fill in the credentials drop down list which's field is 'FsProxyCredentialsId'.
         */
        public ListBoxModel doFillFsProxyCredentialsIdItems(@AncestorInPath Item project,
                                                            @QueryParameter String credentialsId) {
            return doFillCredentialsIdItems(project, credentialsId);
        }

        public FormValidation doCheckCredentialsId(@AncestorInPath Item project,
                                                   @QueryParameter String url,
                                                   @QueryParameter String value) {
            if (project == null || !project.hasPermission(Item.EXTENDED_READ)) {
                return FormValidation.ok();
            }

            value = Util.fixEmptyAndTrim(value);
            if (value == null) {
                return FormValidation.ok();
            }

            url = Util.fixEmptyAndTrim(url);
            if (url == null)
            // not set, can't check
            {
                return FormValidation.ok();
            }

            if (url.indexOf('$') >= 0)
            // set by variable, can't check
            {
                return FormValidation.ok();
            }

            for (ListBoxModel.Option o : CredentialsProvider.listCredentials(
                    StandardUsernamePasswordCredentials.class,
                    project,
                    project instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task) project) : ACL.SYSTEM,
                    URIRequirementBuilder.create().build(),
                    new IdMatcher(value))) {

                if (StringUtils.equals(value, o.value)) {
                    return FormValidation.ok();
                }
            }
            // no credentials available, can't check
            return FormValidation.warning("Cannot find any credentials with id " + value);
        }

        /**
         * Gets report archive modes.
         *
         * @return the report archive modes
         */
        public List<EnumDescription> getReportArchiveModes() {
            return ResultsPublisherModel.archiveModes;
        }
    }
}
