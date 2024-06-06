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

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jingwei
 * Date: 5/17/16
 * Time: 4:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class UploadAppModel {
    private String mcServerName;
    private AuthModel authModel;
    private ProxySettings proxySettings;
    private List<UploadAppPathModel> applicationPaths;

    @DataBoundConstructor
    public UploadAppModel(String mcServerName, AuthModel authModel, ProxySettings proxySettings, List<UploadAppPathModel> applicationPaths) {
        this.mcServerName = mcServerName;
        this.authModel = authModel;
        this.proxySettings = proxySettings;
        this.applicationPaths = applicationPaths;
    }

    public String getMcServerName() {
        return mcServerName;
    }

    public ProxySettings getProxySettings() {
        return proxySettings;
    }

    public boolean isUseProxy() {
        return proxySettings != null;
    }

    public boolean isUseAuthentication() {
        return proxySettings != null && StringUtils.isNotBlank(proxySettings.getFsProxyUserName());
    }

    public List<UploadAppPathModel> getApplicationPaths() {
        return applicationPaths;
    }

    public AuthModel getAuthModel() {
        return authModel;
    }

    public String getAuthType() {
        return authModel == null ? "base" : authModel.getValue();
    }
}
