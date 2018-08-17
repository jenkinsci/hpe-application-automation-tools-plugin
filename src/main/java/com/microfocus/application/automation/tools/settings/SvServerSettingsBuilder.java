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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import com.microfocus.application.automation.tools.model.SvServerSettingsModel;
import com.hp.sv.jsvconfigurator.core.impl.jaxb.ServerInfo;
import com.hp.sv.jsvconfigurator.core.impl.processor.Credentials;
import com.hp.sv.jsvconfigurator.serverclient.ICommandExecutor;
import com.hp.sv.jsvconfigurator.serverclient.impl.CommandExecutorFactory;
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

public class SvServerSettingsBuilder extends Builder {

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * Descriptor for {@link SvServerSettingsBuilder}. Used as a singleton. The class is marked as
     * public so that it can be accessed from views.
     * <p>
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt> for the
     * actual HTML fragment for the configuration screen.
     */
    @Extension
    // This indicates to Jenkins that this is an implementation of an extension
    // point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @CopyOnWrite
        private SvServerSettingsModel[] servers;

        public DescriptorImpl() {
            load();
        }

        private static boolean isHttpsSchema(String url) {
            try {
                return "https".equals(new URL(url).getProtocol());
            } catch (MalformedURLException e) {
                return false;
            }
        }

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

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            List<SvServerSettingsModel> list = req.bindJSONToList(SvServerSettingsModel.class, formData.get("srv"));

            validateMandatoryFields(list);

            servers = list.toArray(new SvServerSettingsModel[list.size()]);

            save();

            return super.configure(req, formData);
        }

        private void validateMandatoryFields(List<SvServerSettingsModel> servers) throws FormException {
            for (SvServerSettingsModel server : servers) {
                validateConfiguration(doCheckName(server.getName()), "name");
                validateConfiguration(doCheckUrl(server.getUrl()), "url");
                validateConfiguration(doCheckUsername(server.getUsername(), server.getUrl()), "username");
                validateConfiguration(doCheckPassword(server.getPassword(), server.getUrl()), "password");
            }
        }

        private void validateConfiguration(FormValidation result, String formField) throws FormException {
            if (!result.equals(FormValidation.ok())) {
                throw new FormException("Validation of property in Service Virtualization server configuration failed: " + result.getMessage(), formField);
            }
        }

        public FormValidation doCheckUrl(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Management Endpoint URL cannot be empty");
            }
            try {
                new URL(value);
            } catch (MalformedURLException e) {
                return FormValidation.error("'" + value + "' is not valid URL");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckUsername(@QueryParameter String value, @QueryParameter("url") final String url) {
            if (isHttpsSchema(url) && StringUtils.isBlank(value)) {
                return FormValidation.error("Username is required for secured server");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckPassword(@QueryParameter String value, @QueryParameter("url") final String url) {
            if (isHttpsSchema(url) && StringUtils.isBlank(value)) {
                return FormValidation.error("Password is required for secured server");
            }
            return FormValidation.ok();
        }

        @SuppressWarnings("unused")
        public FormValidation doTestConnection(@QueryParameter("url") final String url, @QueryParameter("username") final String username,
                                               @QueryParameter("password") final String password) {
            try {
                Credentials credentials = (!StringUtils.isBlank(username)) ? new Credentials(username, password) : null;
                ICommandExecutor commandExecutor = new CommandExecutorFactory().createCommandExecutor(new URL(url), credentials);
                ServerInfo serverInfo = commandExecutor.getClient().getServerInfo();
                return FormValidation.ok("Validation passed. Connected to %s server of version: %s", serverInfo.getServerType(), serverInfo.getProductVersion());
            } catch (Exception e) {
                return FormValidation.error("Validation failed: " + e.getMessage());
            }
        }

        public SvServerSettingsModel[] getServers() {
            return servers;
        }

        public void setServers(SvServerSettingsModel[] servers) {
            this.servers = servers;
        }

        public FormValidation doCheckName(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Name cannot be empty");
            }

            return FormValidation.ok();
        }
    }
}
