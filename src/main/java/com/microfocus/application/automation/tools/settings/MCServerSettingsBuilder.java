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

package com.microfocus.application.automation.tools.settings;

import com.microfocus.application.automation.tools.model.MCServerSettingsModel;
import hudson.CopyOnWrite;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link MCServerSettingsBuilder.MCDescriptorImpl#newInstance(StaplerRequest)} is invoked and a new
 * {@link AlmServerSettingsBuilder} is created. The created instance is persisted to the project
 * configuration XML by using XStream, so this allows you to use instance fields (like {@link #name}
 * ) to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(hudson.model.AbstractBuild, hudson.Launcher, hudson.model.BuildListener)} method
 * will be invoked.
 *
 * @author jingwei
 */
public class MCServerSettingsBuilder extends Builder {
    @Override
    public MCDescriptorImpl getDescriptor() {
        return (MCDescriptorImpl) super.getDescriptor();
    }

    /**
     * Descriptor for {@link MCServerSettingsBuilder}. Used as a singleton. The class is marked as
     * public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt> for the
     * actual HTML fragment for the configuration screen.
     */
    @Extension
    // This indicates to Jenkins that this is an implementation of an extension
    // point.
    public static final class MCDescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(
                @SuppressWarnings("rawtypes") Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project
            // types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "";
        }

        public MCDescriptorImpl() {
            load();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            // useFrench = formData.getBoolean("useFrench");
            // ^Can also use req.bindJSON(this, formData);
            // (easier when there are many fields; need set* methods for this,
            // like setUseFrench)
            // req.bindParameters(this, "locks.");

            setInstallations(req.bindParametersToList(MCServerSettingsModel.class, "mc.").toArray(
                    new MCServerSettingsModel[0]));

            save();

            return super.configure(req, formData);
        }

        public FormValidation doCheckMCServerName(@QueryParameter String value) {
            FormValidation ret = FormValidation.ok();
            if (StringUtils.isBlank(value)) {
                ret = FormValidation.error("MC server name cannot be empty");
            }

            return ret;
        }

        public FormValidation doCheckMCServerURL(@QueryParameter String value) {
            FormValidation ret = FormValidation.ok();
            if (StringUtils.isBlank(value)) {
                ret = FormValidation.error("MC server cannot be empty");
            }

            return ret;
        }

        @CopyOnWrite
        private MCServerSettingsModel[] installations = new MCServerSettingsModel[0];

        public MCServerSettingsModel[] getInstallations() {
            return installations;
        }

        public void setInstallations(MCServerSettingsModel... installations) {
            this.installations = installations;
        }

        public Boolean hasMCServers() {
            return installations.length > 0;
        }
    }
}
