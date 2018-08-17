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

import com.microfocus.application.automation.tools.model.SvDataModelSelection;
import com.microfocus.application.automation.tools.model.SvPerformanceModelSelection;
import com.microfocus.application.automation.tools.model.SvServiceSelectionModel;
import com.microfocus.application.automation.tools.run.SvChangeModeBuilder;
import com.hp.sv.jsvconfigurator.core.impl.jaxb.ServiceRuntimeConfiguration;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class SvChangeModeStep extends AbstractSvStep {
    private final ServiceRuntimeConfiguration.RuntimeMode mode;
    private final SvDataModelSelection dataModel;
    private final SvPerformanceModelSelection performanceModel;
    private final SvServiceSelectionModel serviceSelection;

    @DataBoundConstructor
    public SvChangeModeStep(String serverName, boolean force, ServiceRuntimeConfiguration.RuntimeMode mode,
                            SvDataModelSelection dataModel, SvPerformanceModelSelection performanceModel, SvServiceSelectionModel serviceSelection) {
        super(serverName, force);
        this.mode = mode;
        this.dataModel = dataModel;
        this.performanceModel = performanceModel;
        this.serviceSelection = serviceSelection;
    }

    public ServiceRuntimeConfiguration.RuntimeMode getMode() {
        return mode;
    }

    public SvDataModelSelection getDataModel() {
        return dataModel;
    }

    public SvPerformanceModelSelection getPerformanceModel() {
        return performanceModel;
    }

    public SvServiceSelectionModel getServiceSelection() {
        return serviceSelection;
    }

    @Override
    protected SimpleBuildStep getBuilder() {
        return new SvChangeModeBuilder(serverName, force, mode, dataModel, performanceModel, serviceSelection);
    }

    @Extension
    public static class DescriptorImpl extends AbstractSvStepDescriptor<SvChangeModeBuilder.DescriptorImpl> {
        public DescriptorImpl() {
            super(SvExecution.class, "svChangeModeStep", new SvChangeModeBuilder.DescriptorImpl());
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckDataModel(@QueryParameter String value, @QueryParameter("mode") String mode,
                                               @QueryParameter("serviceSelectionKind") String kind) {
            return builderDescriptor.doCheckDataModel(value, mode, kind);
        }

        @SuppressWarnings("unused")
        public ListBoxModel doFillModeItems() {
            return builderDescriptor.doFillModeItems();
        }
    }
}
