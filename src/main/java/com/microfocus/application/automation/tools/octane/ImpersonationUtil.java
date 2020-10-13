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

/** *  *
 * util class for user impersonation, to allow internal access on behalf of the Jenkins user associated with an instance of ALM Octane server.
 */

public class ImpersonationUtil {
    private static final Logger logger = SDKBasedLoggerProvider.getLogger(ImpersonationUtil.class);

    public static ACLContext startImpersonation(String instanceId, Long workspaceId) {
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
