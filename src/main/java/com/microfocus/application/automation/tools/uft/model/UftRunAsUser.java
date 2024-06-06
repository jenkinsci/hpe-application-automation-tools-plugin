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

package com.microfocus.application.automation.tools.uft.model;

import com.microfocus.application.automation.tools.EncryptionUtils;
import hudson.model.Node;
import hudson.util.Secret;
import org.apache.commons.lang.StringUtils;

import static com.microfocus.application.automation.tools.uft.utils.Constants.*;

public class UftRunAsUser {
    private String username;
    private String encodedPwd;
    private Secret pwd;

    public UftRunAsUser(String username, String encodedPwd) {
        if (StringUtils.isBlank(username) ) {
            throw new IllegalArgumentException(String.format("%s is required", UFT_RUN_AS_USER_NAME));
        } else if (StringUtils.isBlank(encodedPwd)) {
            throw new IllegalArgumentException(String.format("%s is required", UFT_RUN_AS_USER_ENCODED_PWD));
        }
        this.username = username;
        this.encodedPwd = encodedPwd;
    }
    public UftRunAsUser(String username, Secret pwd) {
        if (StringUtils.isBlank(username) ) {
            throw new IllegalArgumentException(String.format("%s is required", UFT_RUN_AS_USER_NAME));
        } else if (pwd == null) {
            throw new IllegalArgumentException(String.format("%s is required", UFT_RUN_AS_USER_PWD));
        }
        this.username = username;
        this.pwd = pwd;
    }

    public String getUsername() {
        return username;
    }

    public String getEncodedPassword() {
        return encodedPwd;
    }

    public String getEncodedPasswordAsEncrypted(Node node) throws EncryptionUtils.EncryptionException {
        return EncryptionUtils.encrypt(encodedPwd, node);
    }

    public Secret getPassword() { return pwd; }

    public String getPasswordAsEncrypted(Node node) throws EncryptionUtils.EncryptionException {
        return EncryptionUtils.encrypt(pwd.getPlainText(), node);
    }
}