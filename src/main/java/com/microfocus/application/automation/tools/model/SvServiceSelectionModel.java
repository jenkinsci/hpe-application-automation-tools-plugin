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
import hudson.util.Secret;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class SvServiceSelectionModel extends AbstractDescribableImpl<SvServiceSelectionModel> implements Serializable {

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
