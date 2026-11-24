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

import com.cloudbees.hudson.plugins.folder.properties.FolderCredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.util.Secret;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import java.io.IOException;

/**
 * Base class for the Autonomous Tester credentials that hold a JSON configuration
 * ({@link AuTeLlmCredentials} and {@link AuTeAplsLicenseCredentials}).
 *
 * <p>These credentials can be stored only in folder credential stores, see {@link AuTeFolderCredentials}.</p>
 */
public abstract class AuTeJsonCredentials extends BaseStandardCredentials {

    private static final long serialVersionUID = 1L;

    private final Secret configurationJson;

    protected AuTeJsonCredentials(CredentialsScope scope, String id, String description, Secret configurationJson) {
        super(scope, id, description);
        this.configurationJson = configurationJson;
    }

    public Secret getConfigurationJson() {
        return configurationJson;
    }

    /** Parses the stored configuration; it must be a JSON object. */
    public JSONObject readJson() throws IOException {
        Object json = JSONValue.parse(Secret.toString(configurationJson));
        if (!(json instanceof JSONObject)) {
            throw new IOException("[AuTe][ERROR] '%s' credential '%s' must contain a JSON object."
                    .formatted(getDescriptor().getDisplayName(), getId()));
        }
        return (JSONObject) json;
    }

    /** Offers the credential type only in folder credential stores. */
    public abstract static class FolderOnlyDescriptor extends BaseStandardCredentialsDescriptor {

        @Override
        public boolean isApplicable(CredentialsProvider provider) {
            return provider instanceof FolderCredentialsProvider;
        }
    }
}