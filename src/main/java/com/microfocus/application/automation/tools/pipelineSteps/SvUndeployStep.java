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

import com.microfocus.application.automation.tools.model.SvServiceSelectionModel;
import com.microfocus.application.automation.tools.run.SvUndeployBuilder;
import hudson.Extension;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;

public class SvUndeployStep extends AbstractSvStep {
    private final boolean continueIfNotDeployed;
    private final SvServiceSelectionModel serviceSelection;

    @DataBoundConstructor
    public SvUndeployStep(String serverName, boolean continueIfNotDeployed, boolean force, SvServiceSelectionModel serviceSelection) {
        super(serverName, force);
        this.continueIfNotDeployed = continueIfNotDeployed;
        this.serviceSelection = serviceSelection;
    }

    @SuppressWarnings("unused")
    public boolean isContinueIfNotDeployed() {
        return continueIfNotDeployed;
    }

    public SvServiceSelectionModel getServiceSelection() {
        return serviceSelection;
    }

    @Override
    protected SimpleBuildStep getBuilder() {
        return new SvUndeployBuilder(serverName, continueIfNotDeployed, force, serviceSelection);
    }

    @Extension
    public static class DescriptorImpl extends AbstractSvStepDescriptor<SvUndeployBuilder.DescriptorImpl> {
        public DescriptorImpl() {
            super(SvExecution.class, "svUndeployStep", new SvUndeployBuilder.DescriptorImpl());
        }
    }
}
