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
package com.microfocus.application.automation.tools.uft.model;

import com.microfocus.application.automation.tools.uft.utils.UftToolUtils;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;

public class RerunSettingsModel extends AbstractDescribableImpl<RerunSettingsModel> {
    private String test;
    private Boolean checked;
    private Integer numberOfReruns;
    private String cleanupTest;

    @DataBoundConstructor
    public RerunSettingsModel(String test, Boolean checked, Integer numberOfReruns, String cleanupTest) {
        this.test = test;
        this.checked = checked;
        this.numberOfReruns = numberOfReruns;
        this.cleanupTest = cleanupTest;
    }

    public String getTest() {
        return test;
    }

    @DataBoundSetter
    public void setTest(String test) {
        this.test = test;
    }

    public Boolean getChecked() {
        return checked;
    }

    @DataBoundSetter
    public void setChecked(Boolean checked) {
        this.checked = checked;
    }

    public Integer getNumberOfReruns() {
        return numberOfReruns;
    }

    @DataBoundSetter
    public void setNumberOfReruns(Integer numberOfReruns) {
        this.numberOfReruns = numberOfReruns;
    }

    public String getCleanupTest() {
        return cleanupTest;
    }

    @DataBoundSetter
    public void setCleanupTest(String cleanupTest) {
        this.cleanupTest = cleanupTest;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<RerunSettingsModel> {
        @Nonnull
        @Override
        public String getDisplayName() {
            return "Rerun settings model";
        }

        public FormValidation doCheckNumberOfReruns(@QueryParameter String value) {
            return UftToolUtils.doCheckNumberOfReruns(value);
        }
    }
}
