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

import com.microfocus.application.automation.tools.model.SvDataModelSelection;
import com.microfocus.application.automation.tools.model.SvPerformanceModelSelection;
import com.microfocus.application.automation.tools.model.SvServiceSelectionModel;
import com.microfocus.application.automation.tools.run.SvChangeModeBuilder;
import com.microfocus.application.automation.tools.sv.pipeline.AbstractSvStep;
import com.microfocus.application.automation.tools.sv.pipeline.AbstractSvStepDescriptor;
import com.microfocus.sv.svconfigurator.core.impl.jaxb.ServiceRuntimeConfiguration;
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
    public SimpleBuildStep getBuilder() {
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
