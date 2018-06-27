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
