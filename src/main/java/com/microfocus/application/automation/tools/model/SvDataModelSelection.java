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
package com.microfocus.application.automation.tools.model;

import javax.annotation.Nonnull;

import java.io.Serializable;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class SvDataModelSelection extends AbstractDescribableImpl<SvDataModelSelection> implements Serializable {

    protected final SelectionType selectionType;
    protected final String dataModel;

    @DataBoundConstructor
    public SvDataModelSelection(SelectionType selectionType, String dataModel) {
        this.selectionType = selectionType;
        this.dataModel = StringUtils.trim(dataModel);
    }

    public static void validateField(FormValidation result) {
        if (!result.equals(FormValidation.ok())) {
            throw new IllegalArgumentException(StringEscapeUtils.unescapeXml(result.getMessage()));
        }
    }

    @SuppressWarnings("unused")
    public SelectionType getSelectionType() {
        return selectionType;
    }

    public String getDataModel() {
        return (StringUtils.isNotBlank(dataModel)) ? dataModel : null;
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
                return dataModel;
            case NONE:
                return "<none>";
            case DEFAULT:
            default:
                return "<default>";
        }
    }

    public String getSelectedModelName() {
        switch (selectionType) {
            case BY_NAME:
                validateField(DescriptorImpl.doCheckDataModelImpl(dataModel));
                return dataModel;
            default:
                return null;
        }
    }

    public enum SelectionType implements Serializable {
        BY_NAME,
        NONE,
        /**
         * Default means first model in alphabetical order by model name
         */
        DEFAULT,
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<SvDataModelSelection> {

        @Nonnull
        public String getDisplayName() {
            return "Data model Selection";
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckDataModel(@QueryParameter String dataModel) {
            return doCheckDataModelImpl(dataModel);
        }

        private static FormValidation doCheckDataModelImpl(@QueryParameter String dataModel) {
            if (StringUtils.isBlank(dataModel)) {
                return FormValidation.error("Data model cannot be empty if 'Specific' model is selected");
            }
            return FormValidation.ok();
        }
    }
}
