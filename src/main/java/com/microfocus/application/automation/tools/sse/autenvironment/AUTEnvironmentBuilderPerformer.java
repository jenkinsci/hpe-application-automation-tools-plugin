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

package com.microfocus.application.automation.tools.sse.autenvironment;

import com.microfocus.application.automation.tools.common.SSEException;
import com.microfocus.application.automation.tools.model.AUTEnvironmentResolvedModel;
import com.microfocus.application.automation.tools.model.AutEnvironmentParameterModel;
import com.microfocus.application.automation.tools.rest.RestClient;
import com.microfocus.application.automation.tools.sse.common.StringUtils;
import com.microfocus.application.automation.tools.sse.sdk.Logger;
import com.microfocus.application.automation.tools.sse.sdk.authenticator.AuthenticationTool;
import hudson.EnvVars;
import hudson.util.VariableResolver;

import java.util.Collection;
import java.util.List;

/**
 * Created by barush on 29/10/2014.
 */
public class AUTEnvironmentBuilderPerformer {

    private Logger logger;
    private AUTEnvironmentResolvedModel model;
    private RestClient restClient;
    private VariableResolver<String> buildVariableResolver;
    private String autEnvironmentConfigurationIdToReturn;

    public AUTEnvironmentBuilderPerformer(
            AUTEnvironmentResolvedModel model,
            VariableResolver<String> buildVariableResolver,
            Logger logger) {

        this.model = model;
        this.logger = logger;
        this.buildVariableResolver = buildVariableResolver;
    }

    public void start(EnvVars envVars) {
        try {
            if (AuthenticationTool.getInstance().authenticate(getClient(),
                    model.getAlmUserName(),
                    model.getAlmPassword(),
                    model.getAlmServerUrl(),
                    model.getClientType(),
                    logger)) {
                logger.log(String.format(
                        "Alm server url: %s", model.getAlmServerUrl()));
                performAutOperations(envVars);
            }
        } catch (Throwable cause) {
            logger.log(String.format(
                    "Failed to update ALM AUT Environment. Cause: %s",
                    cause.getMessage()));
            throw cause;
        }
    }

    public String getAutEnvironmentConfigurationIdToReturn() {
        return autEnvironmentConfigurationIdToReturn;
    }

    private void performAutOperations(EnvVars envVars) {
        String autEnvironmentId = model.getAutEnvironmentId();

        AUTEnvironmentManager autEnvironmentManager =
                new AUTEnvironmentManager(getClient(), logger);
        String parametersRootFolderId =
                autEnvironmentManager.getParametersRootFolderIdByAutEnvId(autEnvironmentId);
        String autEnvironmentConfigurationId =
                getAutEnvironmentConfigurationId(autEnvironmentManager, autEnvironmentId);

        assignValuesToAutParameters(autEnvironmentConfigurationId, parametersRootFolderId, envVars);
        autEnvironmentConfigurationIdToReturn = autEnvironmentConfigurationId;

    }

    private void assignValuesToAutParameters(
            String autEnvironmentConfigurationId,
            String parametersRootFolderId,
            EnvVars envVars) {

        List<AutEnvironmentParameterModel> autEnvironmentParameters =
                model.getAutEnvironmentParameters();
        if (autEnvironmentParameters == null || autEnvironmentParameters.size() == 0) {
            logger.log("There's no AUT Environment parameters to assign for this build...");
            return;
        }

        String selectedNode = envVars.get("NODE_NAME");
        AUTEnvironmentParametersManager parametersManager =
                new AUTEnvironmentParametersManager(
                        getClient(),
                        autEnvironmentParameters,
                        parametersRootFolderId,
                        autEnvironmentConfigurationId,
                        buildVariableResolver,
                        model.getPathToJsonFile(),
                        logger,
                        selectedNode);

        Collection<AUTEnvironmnentParameter> parametersToUpdate =
                parametersManager.getParametersToUpdate();
        parametersManager.updateParametersValues(parametersToUpdate);
        logger.log("assignValuesToAutParameters");
    }

    private String getAutEnvironmentConfigurationId(
            AUTEnvironmentManager autEnvironmentManager,
            String autEnvironmentId) {

        String autEnvironmentConfigurationId =
                autEnvironmentManager.shouldUseExistingConfiguration(model)
                        ? model.getExistingAutEnvConfId()
                        : autEnvironmentManager.createNewAutEnvironmentConfiguration(
                        autEnvironmentId,
                        model);

        if (StringUtils.isNullOrEmpty(autEnvironmentConfigurationId)) {
            throw new SSEException("There's no AUT Environment Configuration in order to proceed");
        }
        return autEnvironmentConfigurationId;

    }

    private RestClient getClient() {
        if (restClient == null) {
            restClient =
                    new RestClient(
                            model.getAlmServerUrl(),
                            model.getAlmDomain(),
                            model.getAlmProject(),
                            model.getAlmUserName());
        }

        return restClient;
    }
}
