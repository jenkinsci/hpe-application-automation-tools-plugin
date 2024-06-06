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

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.hp.octane.integrations.services.pullrequestsandbranches.PullRequestAndBranchService;
import com.hp.octane.integrations.services.pullrequestsandbranches.factory.FetchHandler;
import com.hp.octane.integrations.services.pullrequestsandbranches.factory.RepoTemplates;
import com.hp.octane.integrations.services.pullrequestsandbranches.rest.authentication.AuthenticationStrategy;
import com.hp.octane.integrations.services.pullrequestsandbranches.rest.authentication.BasicAuthenticationStrategy;
import com.hp.octane.integrations.services.pullrequestsandbranches.rest.authentication.NoCredentialsStrategy;
import com.hp.octane.integrations.services.pullrequestsandbranches.rest.authentication.PATStrategy;
import hudson.model.Run;
import hudson.model.User;
import hudson.util.Secret;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.TimeZone;
import java.util.function.Consumer;

public class GitFetchUtils {

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm";

    private GitFetchUtils() {
        //codacy recommendation : Add a private constructor to hide the implicit public one.
    }

    /**
     * Generate date format for template : yyyy-MM-dd HH:mm
     *
     * @return
     */
    public static SimpleDateFormat generateDateFormat() {
        SimpleDateFormat temp = new SimpleDateFormat(DATE_TIME_FORMAT);
        TimeZone utc = TimeZone.getTimeZone("UTC");
        temp.setTimeZone(utc);
        return temp;
    }

    /**
     * Get user id by email and login. This method is used to return the same user Id for commits/pull request/branches
     *
     * @param email
     * @param login
     * @return
     */
    public static String getUserIdForCommit(String email, String login) {
        if (login != null) {
            User user = User.get(login, false, Collections.emptyMap());
            if (user != null) {
                return user.getId();
            }
        }
        if (email != null && email.contains("@")) {
            String[] emailParts = email.split("@");
            return emailParts[0];

        }
        return login;
    }

    /**
     * Get user name password credentials by id.
     */
    public static StandardCredentials getCredentialsById(String credentialsId, Run<?, ?> run, PrintStream logger) {

        StandardCredentials credentials = null;
        if (!StringUtils.isEmpty(credentialsId)) {
            credentials = CredentialsProvider.findCredentialById(credentialsId,
                    StandardCredentials.class,
                    run,
                    URIRequirementBuilder.create().build());
            if (credentials == null) {
                logger.println("Can not find credentials with the credentialsId:" + credentialsId);
            }
        }

        return credentials;
    }

    public static AuthenticationStrategy getAuthenticationStrategy(StandardCredentials credentials) {
        AuthenticationStrategy authenticationStrategy;
        if (credentials == null) {
            authenticationStrategy = new NoCredentialsStrategy();
        } else if (credentials instanceof StringCredentials) {
            Secret secret = ((StringCredentials) credentials).getSecret();
            authenticationStrategy = new PATStrategy(secret.getPlainText());
        } else if (credentials instanceof StandardUsernamePasswordCredentials) {
            StandardUsernamePasswordCredentials cr = (StandardUsernamePasswordCredentials) credentials;
            authenticationStrategy = new BasicAuthenticationStrategy(cr.getUsername(), cr.getPassword().getPlainText());
        } else {
            throw new IllegalArgumentException("Credentials type is not supported : " + credentials.getClass().getCanonicalName());
        }

        return authenticationStrategy;
    }

    public static void updateRepoTemplates(PullRequestAndBranchService pullRequestAndBranchService, FetchHandler fetcherHandler, String repoHttpUrlForTemplates, String repoUrlForOctane, Long workspaceId, Consumer<String> logConsumer) {
        //update repo templates
        try {
            RepoTemplates repoTemplates = fetcherHandler.buildRepoTemplates(repoHttpUrlForTemplates);
            if (pullRequestAndBranchService.updateRepoTemplates(repoUrlForOctane, workspaceId, repoTemplates)) {
                logConsumer.accept("Repo template are updated successfully in ALM Octane");
            }
        } catch (Exception e) {
            logConsumer.accept("Failed to update repo templates : " + e.getMessage());
        }
    }

}
