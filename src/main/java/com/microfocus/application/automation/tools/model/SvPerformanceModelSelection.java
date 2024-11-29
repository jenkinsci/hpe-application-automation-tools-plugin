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
package com.microfocus.application.automation.tools.model;

import javax.annotation.Nonnull;

import java.io.Serializable;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class SvPerformanceModelSelection extends AbstractDescribableImpl<SvPerformanceModelSelection> implements Serializable {

    protected final SelectionType selectionType;
    protected final String performanceModel;

    @DataBoundConstructor
    public SvPerformanceModelSelection(SelectionType selectionType, String performanceModel) {
        this.selectionType = selectionType;
        this.performanceModel = StringUtils.trim(performanceModel);
    }

    @SuppressWarnings("unused")
    public SelectionType getSelectionType() {
        return selectionType;
    }

    @SuppressWarnings("unused")
    public String getPerformanceModel() {
        return (StringUtils.isNotBlank(performanceModel)) ? performanceModel : null;
    }

    @SuppressWarnings("unused")
    public boolean isSelected(String type) {
        return SelectionType.valueOf(type) == this.selectionType;
    }

    public boolean isNoneSelected() {
        return selectionType == SelectionType.NONE;
    }

    public boolean isDefaultSelected() {
        return selectionType == SelectionType.DEFAULT;
    }

    @Override
    public String toString() {
        switch (selectionType) {
            case BY_NAME:
                return performanceModel;
            case NONE:
                return "<none>";
            case OFFLINE:
                return "<offline>";
            default:
                return "<default>";
        }
    }

    public String getSelectedModelName() {
        switch (selectionType) {
            case BY_NAME:
                SvDataModelSelection.validateField(DescriptorImpl.doCheckPerformanceModelImpl(performanceModel));
                return performanceModel;
            case OFFLINE:
                return "Offline";
            default:
                return null;
        }
    }

    public enum SelectionType {
        BY_NAME,
        NONE,
        OFFLINE,
        /**
         * Default means first model in alphabetical order by model name
         */
        DEFAULT,
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<SvPerformanceModelSelection> {

        @Nonnull
        public String getDisplayName() {
            return "Performance Model Selection";
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckPerformanceModel(@QueryParameter String performanceModel) {
            return doCheckPerformanceModelImpl(performanceModel);
        }

        private static FormValidation doCheckPerformanceModelImpl(@QueryParameter String performanceModel) {
            if (StringUtils.isBlank(performanceModel)) {
                return FormValidation.error("Performance model cannot be empty if 'Specific' model is selected");
            }
            return FormValidation.ok();
        }
    }
}
