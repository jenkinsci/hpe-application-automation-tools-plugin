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

package com.microfocus.application.automation.tools.settings;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.util.Secret;
import io.jenkins.cli.shaded.org.apache.commons.lang.RandomStringUtils;
import jenkins.model.GlobalConfiguration;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;
import java.security.SecureRandom;

@Extension
public class UFTEncryptionGlobalConfiguration extends GlobalConfiguration implements Serializable {
    // won't be displayed anywhere, a bit of a hack, but should be secure

    // seems important, if further changes needed after release
    private static final long serialVersionUID = 1L;

    private static Secret generateKey() {
        return Secret.fromString(RandomStringUtils.random(32, 0, 0, true, true, null, new SecureRandom()));
    }

    public static UFTEncryptionGlobalConfiguration getInstance() throws NullPointerException {
        UFTEncryptionGlobalConfiguration config = GlobalConfiguration.all().get(UFTEncryptionGlobalConfiguration.class);

        if (config == null) throw new NullPointerException();

        return config;
    }

    private Secret encKey;

    @DataBoundConstructor
    public UFTEncryptionGlobalConfiguration() {
        load();
    }

    @NonNull
    @Override
    public String getDisplayName() {
        return "UFT Encryption Global Configuration (Should not appear)";
    }

    /**
     * Returns in encrypted form the current encryption key, generates one if this master doesn't have one.
     * @return encrypted encryption key
     */
    public String getEncKey() {
        if (encKey == null) {
            encKey = generateKey();
            save();
        }

        return encKey.getEncryptedValue();
    }

}
