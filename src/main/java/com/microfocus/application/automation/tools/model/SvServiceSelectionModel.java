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

import javax.annotation.Nonnull;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class SvServiceSelectionModel extends AbstractDescribableImpl<SvServiceSelectionModel> {

    protected final SelectionType selectionType;
    protected final String service;
    protected final String projectPath;
    protected final Secret projectPassword;

    @DataBoundConstructor
    public SvServiceSelectionModel(SelectionType selectionType, String service, String projectPath, String projectPassword) {
        Validate.notNull(selectionType, "SelectionType must be specified");
        this.selectionType = selectionType;
        this.service = StringUtils.trim(service);
        this.projectPath = StringUtils.trim(projectPath);
        this.projectPassword = Secret.fromString(projectPassword);
    }

    public SelectionType getSelectionType() {
        return selectionType;
    }

    public String getService() {
        return (StringUtils.isNotBlank(service)) ? service : null;
    }

    public String getProjectPath() {
        return (StringUtils.isNotBlank(projectPath)) ? projectPath : null;
    }

    public String getProjectPassword() {
        return (projectPassword != null) ? projectPassword.getPlainText() : null;
    }

    @SuppressWarnings("unused")
    public boolean isSelected(String selectionType) {
        return SelectionType.valueOf(selectionType) == this.selectionType;
    }

    public enum SelectionType {
        /**
         * Select service by name or id
         */
        SERVICE,
        /**
         * Select all services from project
         */
        PROJECT,
        /**
         * Select all deployed services
         */
        ALL_DEPLOYED,
        /**
         * Specific case for deployment. Uses project & optionally service names.
         */
        DEPLOY
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<SvServiceSelectionModel> {

        @Nonnull
        public String getDisplayName() {
            return "Service Selection";
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckService(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Service name or id must be set");
            }
            return FormValidation.ok();
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckProjectPath(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Project path cannot be empty");
            }
            return FormValidation.ok();
        }
    }
}
