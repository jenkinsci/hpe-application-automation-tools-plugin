/*
 *  Certain versions of software accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 *  This software was acquired by Micro Focus on September 1, 2017, and is now
 *  offered by OpenText.
 *  Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical
 *  in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the
 *  property of their respective owners.
 *  OpenText is a trademark of Open Text.
 *  __________________________________________________________________
 *  MIT License
 *
 *  Copyright 2012-2025 Open Text.
 *
 *  The only warranties for products and services of Open Text and
 *  its affiliates and licensors ("Open Text") are as may be set forth
 *  in the express warranty statements accompanying such products and services.
 *  Nothing herein should be construed as constituting an additional warranty.
 *  Open Text shall not be liable for technical or editorial errors or
 *  omissions contained herein. The information contained herein is subject
 *  to change without notice.
 *
 *  Except as specifically indicated otherwise, this document contains
 *  confidential information and a valid license is required for possession,
 *  use or copying. If this work is provided to the U.S. Government,
 *  consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 *  Computer Software Documentation, and Technical Data for Commercial Items are
 *  licensed to the U.S. Government under vendor's standard commercial license.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ___________________________________________________________________
 */
package com.microfocus.application.automation.tools.pipelineSteps;

import com.microfocus.application.automation.tools.model.SvServiceSelectionModel;
import com.microfocus.application.automation.tools.run.SvExportBuilder;
import com.microfocus.application.automation.tools.sv.pipeline.AbstractSvStep;
import com.microfocus.application.automation.tools.sv.pipeline.AbstractSvStepDescriptor;
import hudson.Extension;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class SvExportStep extends AbstractSvStep {
    private final String targetDirectory;
    private final boolean cleanTargetDirectory;
    private final SvServiceSelectionModel serviceSelection;
    private final boolean switchToStandByFirst;
    private final boolean archive;

    @DataBoundConstructor
    public SvExportStep(String serverName, boolean force, String targetDirectory, boolean cleanTargetDirectory,
                        SvServiceSelectionModel serviceSelection, boolean switchToStandByFirst, boolean archive) {
        super(serverName, force);
        this.targetDirectory = targetDirectory;
        this.cleanTargetDirectory = cleanTargetDirectory;
        this.serviceSelection = serviceSelection;
        this.switchToStandByFirst = switchToStandByFirst;
        this.archive = archive;
    }

    public String getTargetDirectory() {
        return targetDirectory;
    }

    public boolean isCleanTargetDirectory() {
        return cleanTargetDirectory;
    }

    public SvServiceSelectionModel getServiceSelection() {
        return serviceSelection;
    }

    public boolean isSwitchToStandByFirst() {
        return switchToStandByFirst;
    }

    public boolean isArchive() {
        return archive;
    }

    @Override
    public SimpleBuildStep getBuilder() {
        return new SvExportBuilder(serverName, force, targetDirectory, cleanTargetDirectory, serviceSelection, switchToStandByFirst, archive);
    }

    @Extension
    public static class DescriptorImpl extends AbstractSvStepDescriptor<SvExportBuilder.DescriptorImpl> {
        public DescriptorImpl() {
            super(SvExecution.class, "svExportStep", new SvExportBuilder.DescriptorImpl());
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckTargetDirectory(@QueryParameter String targetDirectory) {
            return builderDescriptor.doCheckTargetDirectory(targetDirectory);
        }
    }
}
