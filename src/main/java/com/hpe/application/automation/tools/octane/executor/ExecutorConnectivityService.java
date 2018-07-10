/*
 * © Copyright 2013 EntIT Software LLC
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright (c) 2018 Micro Focus Company, L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ___________________________________________________________________
 *
 */

package com.hpe.application.automation.tools.octane.executor;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.executor.CredentialsInfo;
import com.hp.octane.integrations.dto.executor.TestConnectivityInfo;
import com.hpe.application.automation.tools.common.HttpStatus;
import com.hpe.application.automation.tools.octane.executor.scmmanager.ScmPluginFactory;
import com.hpe.application.automation.tools.octane.executor.scmmanager.ScmPluginHandler;
import hudson.model.Item;
import hudson.model.User;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * Utility for handling connectivity with scm repositories
 */
public class ExecutorConnectivityService {

    private static final Logger logger = LogManager.getLogger(ExecutorConnectivityService.class);
    private static final Map<Permission, String> requirePremissions = initRequirePremissions();
    private static final Map<Permission, String> credentialsPremissions = initCredentialsPremissions();

    /**
     * Validate that scm repository is valid
     *
     * @param testConnectivityInfo contains values to check
     * @return OctaneResponse return status code and error to show for client
     */
    public static OctaneResponse checkRepositoryConnectivity(TestConnectivityInfo testConnectivityInfo) {
        OctaneResponse result = DTOFactory.getInstance().newDTO(OctaneResponse.class);
        if (testConnectivityInfo.getScmRepository() != null && StringUtils.isNotEmpty(testConnectivityInfo.getScmRepository().getUrl())) {

            BaseStandardCredentials credentials = null;
            if (StringUtils.isNotEmpty(testConnectivityInfo.getUsername()) && testConnectivityInfo.getPassword() != null) {
                credentials = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null, testConnectivityInfo.getUsername(), testConnectivityInfo.getPassword());
            } else if (StringUtils.isNotEmpty(testConnectivityInfo.getCredentialsId())) {
                credentials = getCredentialsById(testConnectivityInfo.getCredentialsId());
            }


            Jenkins jenkins = Jenkins.getActiveInstance();

            List<String> permissionResult = checkCIPermissions(jenkins, credentials != null);

            if (permissionResult != null && !permissionResult.isEmpty()) {
                String user = User.current() != null ? User.current().getId() : jenkins.ANONYMOUS.getPrincipal().toString();
                String error = String.format("Failed : User \'%s\' is missing permissions \'%s\' on CI server", user, permissionResult);
                logger.error(error);
                result.setStatus(HttpStatus.FORBIDDEN.getCode());
                result.setBody(error);
                return result;
            }

            if (!ScmPluginFactory.isPluginInstalled(testConnectivityInfo.getScmRepository().getType())) {
                result.setStatus(HttpStatus.BAD_REQUEST.getCode());
                result.setBody(String.format("%s plugin is not installed.", testConnectivityInfo.getScmRepository().getType().value().toUpperCase()));
            } else {
                ScmPluginHandler handler = ScmPluginFactory.getScmHandler(testConnectivityInfo.getScmRepository().getType());
                handler.checkRepositoryConnectivity(testConnectivityInfo, credentials, result);
            }

        } else {
            result.setStatus(HttpStatus.BAD_REQUEST.getCode());
            result.setBody("Missing input for testing");
        }
        return result;
    }

    /**
     * Insert of update(if already exist) of credentials in Jenkins.
     * If credentialsInfo contains credentialsId - we update existing credentials with new user/password, otherwise - create new credentials
     *
     * @param credentialsInfo contains values to insert / update - exist credentials will be updated or recreate if deleted in jenkins
     * @return OctaneResponse created/updated credentials with filled credentials id as body
     */
    public static OctaneResponse upsertRepositoryCredentials(final CredentialsInfo credentialsInfo) {

        OctaneResponse result = DTOFactory.getInstance().newDTO(OctaneResponse.class);
        result.setStatus(HttpStatus.CREATED.getCode());

        if (StringUtils.isNotEmpty(credentialsInfo.getCredentialsId())) {
            BaseStandardCredentials cred = getCredentialsById(credentialsInfo.getCredentialsId());
            if (cred != null) {
                BaseStandardCredentials newCred = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credentialsInfo.getCredentialsId(),
                        null, credentialsInfo.getUsername(), credentialsInfo.getPassword());
                CredentialsStore store = new SystemCredentialsProvider.StoreImpl();
                try {
                    store.updateCredentials(Domain.global(), cred, newCred);
                    result.setStatus(HttpStatus.CREATED.getCode());
                    result.setBody(newCred.getId());
                } catch (IOException e) {
                    logger.error("Failed to update credentials " + e.getMessage());
                    result.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
                    result.setBody("Failed to update credentials " + e.getMessage());
                }
                return result;
            }
        }
        if (StringUtils.isNotEmpty(credentialsInfo.getUsername()) && credentialsInfo.getPassword() != null) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            String desc = "Created by the HPE Application Automation Tools plugin on " + formatter.format(new Date());
            BaseStandardCredentials c = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credentialsInfo.getCredentialsId(), desc, credentialsInfo.getUsername(), credentialsInfo.getPassword());
            CredentialsStore store = new SystemCredentialsProvider.StoreImpl();
            try {
                store.addCredentials(Domain.global(), c);
                result.setStatus(HttpStatus.CREATED.getCode());
                result.setBody(c.getId());
            } catch (IOException e) {
                logger.error("Failed to add credentials " + e.getMessage());
                result.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
                result.setBody("Failed to add credentials " + e.getMessage());
            }
        }

        return result;
    }

    private static BaseStandardCredentials getCredentialsById(String credentialsId) {
        List<BaseStandardCredentials> list = CredentialsProvider.lookupCredentials(BaseStandardCredentials.class, (Item) null, null, (DomainRequirement) null);
        for (BaseStandardCredentials cred : list) {
            if (cred.getId().equals(credentialsId))
                return cred;
        }
        return null;
    }

    private static List<String> checkCIPermissions(final Jenkins jenkins, boolean hasCredentials) {
        List<String> result = new ArrayList<>();
        checkPermissions(jenkins, result, requirePremissions);
        if (hasCredentials) {
            checkPermissions(jenkins, result, credentialsPremissions);
        }
        return result;
    }

    private static void checkPermissions(Jenkins jenkins, List<String> result, Map<Permission, String> permissions) {
        for (Permission permission : permissions.keySet()) {
            if (!jenkins.hasPermission(permission)) {
                result.add(permissions.get(permission));
            }
        }
    }

    private static Map<Permission, String> initRequirePremissions() {
        Map<Permission, String> result = new HashedMap();
        result.put(Item.CREATE, "Job.CREATE");
        result.put(Item.DELETE, "Job.DELETE");
        result.put(Item.READ, "Job.READ");
        return result;
    }

    private static Map<Permission, String> initCredentialsPremissions() {
        Map<Permission, String> result = new HashedMap();
        result.put(CredentialsProvider.CREATE, "Credentials.CREATE");
        result.put(CredentialsProvider.UPDATE, "Credentials.UPDATE");
        return result;

    }
}
