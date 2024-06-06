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

package com.microfocus.application.automation.tools.sse.result;

import com.microfocus.application.automation.tools.sse.common.XPathUtils;
import com.microfocus.application.automation.tools.sse.sdk.Client;
import com.microfocus.application.automation.tools.sse.sdk.Logger;
import com.microfocus.application.automation.tools.sse.sdk.Response;
import com.microfocus.application.automation.tools.sse.sdk.request.GetPCRunEntityTestSetRunsRequest;
import com.microfocus.application.automation.tools.sse.sdk.request.GetRequest;

/**
 * 
 * @author Effi Bar-She'an
 * @author Dani Schreiber
 * 
 */
public class PCPublisher extends Publisher {
    
    public PCPublisher(Client client, String entityId, String runId) {
        
        super(client, entityId, runId);
    }
    
    protected String getEntityName(String nameSuffix, Logger logger) {
        
        String ret = "Unnamed Entity";
        try {
            Response response = getEntityName(nameSuffix);
            if (response.isOk() && !response.toString().equals("")) {
                String runId = XPathUtils.getAttributeValue(response.toString(), "id");
                String testId = XPathUtils.getAttributeValue(response.toString(), "testcycl-id");
                String testSetId = XPathUtils.getAttributeValue(response.toString(), "cycle-id");
                ret =
                        String.format(
                                "PC Test ID: %s, Run ID: %s, Test Set ID: %s",
                                testId,
                                runId,
                                testSetId);
            } else {
                Throwable failure = response.getFailure();
                logger.log(String.format(
                        "Failed to get Entity name. Exception: %s",
                        failure == null ? "null" : failure.getMessage()));
            }
        } catch (Throwable e) {
            logger.log("Failed to get Entity name");
        }
        
        return ret;
    }
    
    @Override
    protected GetRequest getRunEntityTestSetRunsRequest(Client client, String runId) {
        
        return new GetPCRunEntityTestSetRunsRequest(client, runId);
    }
}
