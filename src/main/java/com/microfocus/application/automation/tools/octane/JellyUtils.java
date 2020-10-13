/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2019 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors ("Micro Focus") are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.octane;

import com.hp.octane.integrations.OctaneClient;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.services.entities.EntitiesService;
import com.microfocus.application.automation.tools.model.OctaneServerSettingsModel;
import com.microfocus.application.automation.tools.octane.configuration.ConfigurationService;
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

}
