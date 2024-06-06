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

package com.microfocus.application.automation.tools.sv.runner;

import javax.annotation.Nonnull;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.microfocus.application.automation.tools.model.SvServerSettingsModel;
import com.microfocus.application.automation.tools.settings.SvServerSettingsGlobalConfiguration;

import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;

public abstract class AbstractSvRunDescriptor extends BuildStepDescriptor<Builder> {
    private final String displayName;

    protected AbstractSvRunDescriptor(String displayName) {
        this.displayName = displayName;
        load();
    }

    @Override
    public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {
        return true;
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return displayName;
    }

    public SvServerSettingsModel[] getServers() {
        return SvServerSettingsGlobalConfiguration.getInstance().getServers();
    }

    @SuppressWarnings("unused")
    public ListBoxModel doFillServerNameItems(@AncestorInPath Item project, 
                                              @QueryParameter String serverName) {            
        if (project == null || !project.hasPermission(Item.CONFIGURE)) {
            return new StandardListBoxModel().includeCurrentValue(serverName);
        }

        StandardListBoxModel items = new StandardListBoxModel();

        SvServerSettingsModel[] servers = getServers();
        if (servers != null) {
            for (SvServerSettingsModel server : servers) {
                if (StringUtils.isNotBlank(server.getName())) {
                    items.add(server.getName(), server.getName());
                }
            }
        }

        return items;
    }
}
