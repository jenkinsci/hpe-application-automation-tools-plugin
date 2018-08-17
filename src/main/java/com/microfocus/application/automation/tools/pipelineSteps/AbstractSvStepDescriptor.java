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

import javax.annotation.Nonnull;

import com.microfocus.application.automation.tools.model.SvServerSettingsModel;
import com.microfocus.application.automation.tools.run.AbstractSvRunDescriptor;
import hudson.util.ListBoxModel;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.StepExecution;

public abstract class AbstractSvStepDescriptor<T extends AbstractSvRunDescriptor> extends AbstractStepDescriptorImpl {

    final protected T builderDescriptor;
    final private String functionName;

    protected AbstractSvStepDescriptor(Class<? extends StepExecution> executionType, String functionName, T builderDescriptor) {
        super(executionType);
        this.functionName = functionName;
        this.builderDescriptor = builderDescriptor;
    }

    @Override
    public String getFunctionName() {
        return functionName;
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return builderDescriptor.getDisplayName();
    }

    @Override
    public String getConfigPage() {
        return builderDescriptor.getConfigPage();
    }

    public SvServerSettingsModel[] getServers() {
        return builderDescriptor.getServers();
    }

    @SuppressWarnings("unused")
    public ListBoxModel doFillServerNameItems() {
        return builderDescriptor.doFillServerNameItems();
    }
}
