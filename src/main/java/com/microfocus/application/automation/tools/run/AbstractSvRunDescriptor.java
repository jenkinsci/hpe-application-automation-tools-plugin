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

package com.microfocus.application.automation.tools.run;

import javax.annotation.Nonnull;

import com.microfocus.application.automation.tools.model.SvServerSettingsModel;
import com.microfocus.application.automation.tools.settings.SvServerSettingsBuilder;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

public abstract class AbstractSvRunDescriptor extends BuildStepDescriptor<Builder> {
    private final String displayName;

    protected AbstractSvRunDescriptor(String displayName) {
        this.displayName = displayName;
        load();
    }

    @Override
    public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {
        return true;
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return displayName;
    }

    public SvServerSettingsModel[] getServers() {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            throw new IllegalStateException("Cannot get Jenkins instance, probably not running inside Jenkins");
        }
        return jenkins.getDescriptorByType(SvServerSettingsBuilder.DescriptorImpl.class).getServers();
    }

    @SuppressWarnings("unused")
    public ListBoxModel doFillServerNameItems() {
        ListBoxModel items = new ListBoxModel();
        for (SvServerSettingsModel server : getServers()) {
            if (StringUtils.isNotBlank(server.getName())) {
                items.add(server.getName(), server.getName());
            }
        }

        return items;
    }
}
