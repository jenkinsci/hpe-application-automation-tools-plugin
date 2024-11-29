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
package com.microfocus.application.automation.tools.octane;

import com.hp.octane.integrations.exceptions.PermissionException;
import com.microfocus.application.automation.tools.model.OctaneServerSettingsModel;
import com.microfocus.application.automation.tools.octane.configuration.ConfigurationService;
import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import jenkins.model.Jenkins;
import org.acegisecurity.Authentication;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.Logger;

import java.util.Collections;

/**
 * *
 * util class for user impersonation, to allow internal access on behalf of the Jenkins user associated with an instance of ALM Octane server.
 */

public class ImpersonationUtil {
    private static final Logger logger = SDKBasedLoggerProvider.getLogger(ImpersonationUtil.class);

    public static String getUserNameForImpersonation(String instanceId, Long workspaceId) {
        OctaneServerSettingsModel settings = ConfigurationService.getSettings(instanceId);
        if (settings == null) {
            throw new IllegalStateException("failed to retrieve configuration settings by instance ID " + instanceId);
        }

        String userName;
        if (workspaceId != null && settings.getWorkspace2ImpersonatedUserMap().containsKey(workspaceId)) {
            userName = settings.getWorkspace2ImpersonatedUserMap().get(workspaceId);
            logger.info(String.format("Using workspace jenkins user '%s' for workspace '%s'", userName, workspaceId));
        } else {
            userName = settings.getImpersonatedUser();
        }
        return userName;
    }

    public static ACLContext startImpersonation(String instanceId, Long workspaceId) {

        String userName = getUserNameForImpersonation(instanceId, workspaceId);
        User jenkinsUser = null;
        if (!StringUtils.isEmpty(userName)) {
            jenkinsUser = User.get(userName, false, Collections.emptyMap());
            if (jenkinsUser == null) {
                throw new PermissionException(HttpStatus.SC_UNAUTHORIZED);
            }
        }
        return startImpersonation(jenkinsUser);
    }

    public static ACLContext startImpersonation(User jenkinsUser) {

        ACLContext impersonatedContext;
        try {
            impersonatedContext = ACL.as(jenkinsUser);
        } catch (UsernameNotFoundException e) {
            logger.debug("Failed to get impersonatedContext from user. Trial to get impersonatedContext by manual auth creation.");
            //defect#921010 : User impersonation is failing as customer is using custom UserDetailsService that does not have implementation of loadUserByUsername
            Authentication auth = (jenkinsUser == null ? Jenkins.ANONYMOUS : new UsernamePasswordAuthenticationToken(jenkinsUser.getId(), "", new GrantedAuthority[0]));
            impersonatedContext = ACL.as(auth);
        }

        return impersonatedContext;
    }

    public static void stopImpersonation(ACLContext impersonatedContext) {
        if (impersonatedContext != null) {
            impersonatedContext.close();
        }
    }
}
