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
 *  Copyright 2012-2026 Open Text.
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
package com.microfocus.application.automation.tools.mi;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.model.Job;
import jenkins.model.Jenkins;

import java.io.IOException;

/**
 * An {@link AuTeJsonCredentials} together with the folder whose credentials store holds it.
 *
 * <p>AuTe jobs are created directly in one folder per shared space. Only that folder's store is
 * searched; ancestor, Jenkins global, and user stores are ignored.</p>
 */
public record AuTeFolderCredentials(AbstractFolder<?> folder, AuTeJsonCredentials credentials) {

    /**
     * Searches only the immediate parent folder of {@code job} for a credential of {@code type} with {@code id}.
     *
     * @throws IOException when no folder of the job holds it
     */
    public static AuTeFolderCredentials get(Job<?, ?> job, Class<? extends AuTeJsonCredentials> type, String id) throws IOException {
        // lookupStores includes ancestors; only the immediate folder is a tenant boundary.
        for (CredentialsStore store : CredentialsProvider.lookupStores(job)) {
            if (store.getContext() instanceof AbstractFolder<?> folder && folder == job.getParent()) {
                for (Domain domain : store.getDomains()) {
                    for (Credentials credentials : store.getCredentials(domain)) {
                        if (type.isInstance(credentials) && id.equals(((AuTeJsonCredentials) credentials).getId())) {
                            return new AuTeFolderCredentials(folder, (AuTeJsonCredentials) credentials);
                        }
                    }
                }
            }
        }
        throw new IOException("[AuTe][ERROR] No '%s' credential with id '%s' in the folder credentials of job '%s'.".formatted(Jenkins.get().getDescriptorOrDie(type).getDisplayName(), id, job.getFullName()));
    }
}
