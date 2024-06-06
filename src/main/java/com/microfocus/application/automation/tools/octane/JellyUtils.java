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

package com.microfocus.application.automation.tools.octane;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.hp.octane.integrations.OctaneClient;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.services.entities.EntitiesService;
import com.microfocus.application.automation.tools.model.OctaneServerSettingsModel;
import com.microfocus.application.automation.tools.octane.configuration.ConfigurationService;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public class JellyUtils {

    public static final String NONE = "none";
    public static final String NONE_DISPLAY = "- none - ";

    private JellyUtils(){
        //for code climate
    }

    public static ListBoxModel createComboModelWithNoneValue() {
        ListBoxModel m = new ListBoxModel();
        m.add(NONE_DISPLAY, NONE);
        return m;
    }

    public static ListBoxModel fillWorkspaceModel(String configurationId, String workspaceId) {
        ListBoxModel m = createComboModelWithNoneValue();
        if (StringUtils.isNotEmpty(configurationId) && !NONE.equals(configurationId)) {
            try {
                EntitiesService entitiesService = OctaneSDK.getClientByInstanceId(configurationId).getEntitiesService();
                List<Entity> workspaces = entitiesService.getEntities(null, "workspaces", null, null);
                for (Entity workspace : workspaces) {
                    m.add(workspace.getId() + " " + workspace.getName(), String.valueOf(workspace.getId()));
                }
            } catch (Exception e) {
                //octane configuration not found
                m.add(workspaceId, workspaceId);
                return m;
            }
        }
        return m;
    }

    public static ListBoxModel fillConfigurationIdModel() {
        ListBoxModel m = createComboModelWithNoneValue();

        for (OctaneClient octaneClient : OctaneSDK.getClients()) {
            OctaneServerSettingsModel model = ConfigurationService.getSettings(octaneClient.getInstanceId());
            m.add(model.getCaption(), model.getIdentity());
        }
        return m;
    }


    public static ListBoxModel fillCredentialsIdItems(Item project, String credentialsId, CredentialsMatcher credentialsMatcher) {

        if (project == null || !project.hasPermission(Item.CONFIGURE)) {
            return new StandardUsernameListBoxModel().includeCurrentValue(credentialsId);
        }

        return new StandardListBoxModel()
                .includeEmptyValue()
                .includeMatchingAs(
                        project instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task) project) : ACL.SYSTEM,
                        project,
                        StandardCredentials.class,
                        URIRequirementBuilder.create().build(),
                        credentialsMatcher)
                .includeCurrentValue(credentialsId);
    }

}
