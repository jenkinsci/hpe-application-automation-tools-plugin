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

import com.microfocus.application.automation.tools.JenkinsUtils;
import com.microfocus.application.automation.tools.settings.AlmServerSettingsGlobalConfiguration;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.util.List;

/**
 * Created by barush on 21/10/2014.
 */
public class AutEnvironmentModel extends AbstractDescribableImpl<AutEnvironmentModel> {

    private final String almServerName;
    private String almServerUrl;
    private final String almUserName;
    private final SecretContainer almPassword;
    private final String almDomain;
    private final String almProject;
    private final String clientType;

    private final String autEnvironmentId;
    private final boolean useExistingAutEnvConf;
    private final String existingAutEnvConfId;
    private final boolean createNewAutEnvConf;
    private final String newAutEnvConfName;

    private List<AutEnvironmentParameterModel> autEnvironmentParameters;

    private final String pathToJsonFile;
    private final String outputParameter;

    @DataBoundConstructor
    public AutEnvironmentModel(
            String almServerName,
            String almUserName,
            String almPassword,
            String almDomain,
            String almProject,
            String clientType,
            String autEnvironmentId,
            boolean useExistingAutEnvConf,
            String existingAutEnvConfId,
            boolean createNewAutEnvConf,
            String newAutEnvConfName,
            List<AutEnvironmentParameterModel> autEnvironmentParameters,
            String pathToJsonFile,
            String outputParameter) {

        this.almServerName = almServerName;
        this.almUserName = almUserName;
        this.almPassword = setPassword(almPassword);
        this.almDomain = almDomain;
        this.almProject = almProject;
        this.clientType = clientType;
        this.autEnvironmentId = autEnvironmentId;
        this.useExistingAutEnvConf = useExistingAutEnvConf;
        this.existingAutEnvConfId = existingAutEnvConfId;
        this.createNewAutEnvConf = createNewAutEnvConf;
        this.newAutEnvConfName = newAutEnvConfName;
        this.autEnvironmentParameters = autEnvironmentParameters;
        this.pathToJsonFile = pathToJsonFile;
        this.outputParameter = outputParameter;

    }

    protected SecretContainer setPassword(String almPassword) {

        SecretContainer secretContainer = new SecretContainerImpl();
        secretContainer.initialize(almPassword);

        return secretContainer;
    }

    public String getPathToJsonFile() {
        return pathToJsonFile;
    }

    public boolean isCreateNewAutEnvConf() {
        return createNewAutEnvConf;
    }

    public boolean isUseExistingAutEnvConf() {
        return useExistingAutEnvConf;
    }

    public String getAlmServerName() {

        return almServerName;
    }

    public String getAlmServerUrl() {

        return almServerUrl;
    }

    public void setAlmServerUrl(String almServerUrl) {

        this.almServerUrl = almServerUrl;
    }

    public String getAlmUserName() {

        return almUserName;
    }

    public String getAlmPassword() {

        return almPassword.toString();
    }

    public String getAlmDomain() {

        return almDomain;
    }

    public String getAlmProject() {

        return almProject;
    }

    public String getClientType() {
        return clientType;
    }

    public String getOutputParameter() {
        return outputParameter;
    }

    public String getAutEnvironmentId() {
        return autEnvironmentId;
    }

    public String getExistingAutEnvConfId() {
        return existingAutEnvConfId;
    }

    public String getNewAutEnvConfName() {
        return newAutEnvConfName;
    }

    public List<AutEnvironmentParameterModel> getAutEnvironmentParameters() {
        return autEnvironmentParameters;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<AutEnvironmentModel> {

        public String getDisplayName() {
            return "UFT ALM AUT Environment Preparation Model";
        }

        public AlmServerSettingsModel[] getAlmServers() {
            return AlmServerSettingsGlobalConfiguration.getInstance().getInstallations();
        }

        public FormValidation doCheckAlmUserName(@QueryParameter String value) {

            return generalCheckWithError(value, "User name must be set");
        }

        public FormValidation doCheckAlmDomain(@QueryParameter String value) {

            return generalCheckWithError(value, "Domain must be set");
        }

        public FormValidation doCheckAlmProject(@QueryParameter String value) {

            return generalCheckWithError(value, "Project must be set");
        }

        public FormValidation doCheckAutEnvironmentId(@QueryParameter String value) {

            return generalCheckWithError(value, "AUT Environment ID must be set");
        }

        public FormValidation doCheckAlmPassword(@QueryParameter String value) {

            FormValidation ret = FormValidation.ok();
            if (StringUtils.isBlank(value)) {
                ret = FormValidation.warning("Password for ALM server is empty");
            }

            return ret;
        }

        public FormValidation doCheckOutputParameter(@QueryParameter String value) {

            FormValidation ret = FormValidation.ok();
            if (StringUtils.isBlank(value)) {
                ret =
                        FormValidation.warning("AUT Environment Configuration ID isn't assigned to any environment variable");
            }

            return ret;
        }

        private FormValidation generalCheckWithError(String value, String errorMessage) {

            FormValidation ret = FormValidation.ok();
            if (StringUtils.isBlank(value)) {
                ret = FormValidation.error(errorMessage);
            }

            return ret;
        }

        public boolean getHasConfigurePermission() {
            return JenkinsUtils.hasItemConfigurePermission();
        }
    }
}
