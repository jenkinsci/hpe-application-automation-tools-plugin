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

package com.microfocus.application.automation.tools.model;

import com.microfocus.application.automation.tools.mc.AuthType;
import hudson.util.Secret;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;

public class AuthModel implements Serializable {
    private final String mcUserName;
    private final Secret mcPassword;
    private final String mcTenantId;
    private final Secret mcExecToken;
    private final String value;
    private AuthType authType;

    @DataBoundConstructor
    public AuthModel(String mcUserName, String mcPassword, String mcTenantId, String mcExecToken, String value) {
        this.mcUserName = mcUserName;
        this.mcPassword = Secret.fromString(mcPassword);
        this.mcTenantId = mcTenantId;
        this.mcExecToken = Secret.fromString(mcExecToken);
        this.value = value;
        authType = AuthType.fromString(value);
        if (authType == AuthType.UNKNOWN) {
            if (StringUtils.isNotBlank(mcExecToken)) {
                authType = AuthType.TOKEN;
            } else if (StringUtils.isNotBlank(mcUserName) && StringUtils.isNotBlank(mcPassword)) {
                authType = AuthType.BASE;
            }
        }
    }

    public String getMcUserName() {
        return mcUserName;
    }

    public String getMcPassword() {
        if (null != mcPassword) {
            return mcPassword.getPlainText();
        } else {
            return null;
        }
    }

    public String getMcTenantId() {
        return mcTenantId;
    }

    public String getMcExecToken() {
        if (null != mcExecToken) {
            return mcExecToken.getPlainText();
        } else {
            return null;
        }
    }

    public String getValue() {
        return value;
    }

    public String getMcEncryptedExecToken() {
        if (null != mcExecToken) {
            return mcExecToken.getEncryptedValue();
        } else {
            return null;
        }
    }

    public String getMcEncryptedPassword() {
        if (null != mcPassword) {
            return mcPassword.getEncryptedValue();
        } else {
            return null;
        }
    }

    public AuthType getAuthType() {
        return authType;
    }
}