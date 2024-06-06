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

package com.microfocus.application.automation.tools.commonResultUpload.service;

import com.microfocus.application.automation.tools.commonResultUpload.CommonUploadLogger;
import com.microfocus.application.automation.tools.rest.RestClient;
import com.microfocus.application.automation.tools.sse.common.XPathUtils;
import com.microfocus.application.automation.tools.sse.sdk.ResourceAccessLevel;
import com.microfocus.application.automation.tools.sse.sdk.Response;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.Map;

public class CustomizationService {

    public static final String RUN_ENTITY_NAME = "run";
    public static final String TEST_INSTANCE_ENTITY_NAME = "test-instance";
    public static final String TEST_ENTITY_NAME = "test";
    public static final String TEST_SET_ENTITY_NAME = "test-set";

    private RestClient client;
    private CommonUploadLogger logger;
    private Map<String, Map<String, String>> subtypeCache;
    private Map<String, Map<String, String>> fieldCache;

    public CustomizationService(RestClient client, CommonUploadLogger logger) {
        this.client = client;
        this.logger = logger;
        subtypeCache = new HashMap<>();
        fieldCache = new HashMap<>();
    }

    public String getRunSubtypeIdByTestInstance(String testInstanceSubtypeId) {
        return getSubtypeIdByName(RUN_ENTITY_NAME,
                getSubtypeNameById(TEST_INSTANCE_ENTITY_NAME, testInstanceSubtypeId));
    }

    public String getTestInstanceSubtypeIdByTest(String testSubtypeId) {
        return getSubtypeIdByName(TEST_INSTANCE_ENTITY_NAME,
                getSubtypeNameById(TEST_ENTITY_NAME, testSubtypeId));
    }

    public String getSubtypeIdByName(String entityName, String subtypeName) {
        return getEntitySubTypes(entityName).get(subtypeName);
    }

    public String getSubtypeNameById(String entityName, String subtypeId) {
        for (Map.Entry<String, String> subtype : getEntitySubTypes(entityName).entrySet()) {
            if (subtype.getValue().equals(subtypeId)) {
                return subtype.getKey();
            }
        }
        return null;
    }

    public Map<String, String> getEntitySubTypes(String entityName) {
        if (subtypeCache.get(entityName) == null) {
            String suffix = String.format("customization/entities/%s/types", entityName);
            String url = client.buildRestRequest(suffix);
            Response response = client.httpGet(
                    url,
                    null,
                    null,
                    ResourceAccessLevel.PROTECTED);
            if (response.isOk() && !response.toString().equals("")) {
                logger.info(String.format("Get customization entity subtypes success. [%s]", entityName));
                Map<String, String> customizationMap = XPathUtils.getEntitySubtypesMap(response.toString());
                subtypeCache.put(entityName, customizationMap);
                return customizationMap;
            } else {
                logger.error("Get customization entity subtypes failed from: " + url);
                logger.error(response.getFailure().toString());
                return null;
            }
        } else {
            return subtypeCache.get(entityName);
        }
    }

    public String getUDFNameByLabel(String entityName, String label) {
        return getEntityFields(entityName).get(label);
    }

    public Map<String, String> getEntityFields(String entityName) {
        if (fieldCache.get(entityName) == null) {
            String suffix = String.format("customization/entities/%s/fields", entityName);
            String url = client.buildRestRequest(suffix);
            Response response = client.httpGet(
                    url,
                    null,
                    null,
                    ResourceAccessLevel.PROTECTED);
            if (response.isOk() && !response.toString().equals("")) {
                logger.info(String.format("Get customization entity fields success. [%s]", entityName));
                Map<String, String> entityFieldsMap = XPathUtils.getEntityFieldsMap(response.toString());
                fieldCache.put(entityName, entityFieldsMap);
                return entityFieldsMap;
            } else {
                logger.error("Get customization entity fields failed from: " + url);
                logger.error(response.getFailure().toString());
                return null;
            }
        } else {
            return fieldCache.get(entityName);
        }
    }

    public boolean isVersioningEnabled(String entityName) {
        String suffix = String.format("customization/entities/%s", entityName);
        String url = client.buildRestRequest(suffix);
        Response response = client.httpGet(
                url,
                null,
                null,
                ResourceAccessLevel.PROTECTED);
        if (response.isOk() && !response.toString().equals("")) {
            logger.log(String.format("INFO: -- Get Entity Resource Descriptor success. [%s]", entityName));
            Document document = XPathUtils.getDocument(response.toString());
            Element element = (Element) document.getElementsByTagName("SupportsVC").item(0);
            return "true".equals(element.getTextContent());
        } else {
            logger.log("ERR: Get entities failed from: " + url);
            logger.log("ERR: " + response.getFailure());
            return false;
        }
    }
}
