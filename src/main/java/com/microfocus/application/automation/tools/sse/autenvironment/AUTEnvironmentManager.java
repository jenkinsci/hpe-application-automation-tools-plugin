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
import com.microfocus.application.automation.tools.sse.autenvironment.request.get.GetAutEnvironmentByIdOldApiRequest;
import com.microfocus.application.automation.tools.sse.autenvironment.request.get.GetAutEnvironmentByIdRequest;
import com.microfocus.application.automation.tools.sse.autenvironment.request.get.GetAutEnvironmentConfigurationByIdRequest;
import com.microfocus.application.automation.tools.sse.autenvironment.request.post.CreateAutEnvConfRequest;
import com.microfocus.application.automation.tools.sse.common.StringUtils;
import com.microfocus.application.automation.tools.sse.common.XPathUtils;
import com.microfocus.application.automation.tools.sse.sdk.Client;
import com.microfocus.application.automation.tools.sse.sdk.Logger;
import com.microfocus.application.automation.tools.sse.sdk.Response;

import java.net.HttpURLConnection;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * Created by barush on 03/11/2014.
 */
public class AUTEnvironmentManager {
    
    public final static String ALM_AUT_ENVIRONMENT_CONFIGURATION_ID_FIELD = "id";
    
    private Logger logger;
    private Client client;
    
    public AUTEnvironmentManager(Client client, Logger logger) {
        
        this.client = client;
        this.logger = logger;
    }
    
    public String getParametersRootFolderIdByAutEnvId(String autEnvironmentId) {
        
        String parametersRootFolderId = null;
        Response response = new GetAutEnvironmentByIdRequest(client, autEnvironmentId).execute();
        /**
         * This if here for backward compatibility to ALM 11.52. After the support for version <
         * 12.00 will be removed this 'if' statement can be removed
         * **/
        if (!response.isOk() && response.getStatusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
            response = new GetAutEnvironmentByIdOldApiRequest(client, autEnvironmentId).execute();
        }
        try {
            List<Map<String, String>> entities = XPathUtils.toEntities(response.toString());
            if (!response.isOk() || entities.size() != 1) {
                throw new SSEException(String.format(
                        "Failed to get AUT Environment with ID: [%s]",
                        autEnvironmentId), response.getFailure());
            }
            
            Map<String, String> autEnvironment = entities.get(0);
            parametersRootFolderId =
                    autEnvironment == null ? null : autEnvironment.get("root-app-param-folder-id");
        } catch (Throwable e) {
            logger.log(String.format("Failed to parse response: %s", response));
        }
        
        return parametersRootFolderId;
    }
    
    public String createNewAutEnvironmentConfiguration(
            String autEnvironmentId,
            AUTEnvironmentResolvedModel autEnvironmentModel) {
        
        String newConfigurationName =
                autEnvironmentModel.isUseExistingAutEnvConf()
                        || StringUtils.isNullOrEmpty(autEnvironmentModel.getNewAutEnvConfName())
                        ? createTempConfigurationName()
                        : autEnvironmentModel.getNewAutEnvConfName();
        return createNewAutEnvironmentConfiguration(autEnvironmentId, newConfigurationName);
        
    }
    
    private String createNewAutEnvironmentConfiguration(
            String autEnvironmentId,
            String newAutEnvConfigurationName) {
        
        String newAutEnvironmentConfigurationId = null;
        Response response =
                new CreateAutEnvConfRequest(client, autEnvironmentId, newAutEnvConfigurationName).execute();
        if (!response.isOk()) {
            logger.log(String.format(
                    "Failed to create new AUT Environment Configuration named: [%s] for AUT Environment with id: [%s]",
                    newAutEnvConfigurationName,
                    autEnvironmentId));
            return null;
        }
        try {
            newAutEnvironmentConfigurationId =
                    XPathUtils.getAttributeValue(
                            response.toString(),
                            ALM_AUT_ENVIRONMENT_CONFIGURATION_ID_FIELD);
        } catch (Throwable e) {
            logger.log(String.format("Failed to parse response: %s", response));
        }
        
        return newAutEnvironmentConfigurationId;
    }
    
    public boolean shouldUseExistingConfiguration(AUTEnvironmentResolvedModel autEnvironmentModel) {
        
        return autEnvironmentModel.isUseExistingAutEnvConf()
               && isAutEnvironmentConfigurationExists(autEnvironmentModel.getExistingAutEnvConfId());
        
    }
    
    private boolean isAutEnvironmentConfigurationExists(String existingAutEnvConfId) {
        
        Response response =
                new GetAutEnvironmentConfigurationByIdRequest(client, existingAutEnvConfId).execute();
        if (!response.isOk() || XPathUtils.toEntities(response.toString()).size() != 1) {
            logger.log(String.format(
                    "Failed to get AUT Environment Configuration with ID: [%s]. Will try to create a new one",
                    existingAutEnvConfId));
            return false;
        }
        return true;
        
    }
    
    private String createTempConfigurationName() {
        
        return "Configuration_" + Calendar.getInstance().getTime().toString();
    }
}
