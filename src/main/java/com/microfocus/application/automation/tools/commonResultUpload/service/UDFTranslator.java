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
import com.microfocus.application.automation.tools.commonResultUpload.uploader.RunUploader;
import com.microfocus.application.automation.tools.commonResultUpload.uploader.TestSetUploader;
import com.microfocus.application.automation.tools.commonResultUpload.uploader.TestUploader;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UDFTranslator {

    public static final String UDF_PREFIX = "udf|";
    private CommonUploadLogger logger;
    private CustomizationService customizationService;

    public UDFTranslator(CustomizationService customizationService, CommonUploadLogger logger) {
        this.customizationService = customizationService;
        this.logger = logger;
    }

    public void translate(String restPrefix, Map<String, String> valueMap) {
        switch (restPrefix) {
            case TestSetUploader.TEST_SET_REST_PREFIX:
                transUDFNames(CustomizationService.TEST_SET_ENTITY_NAME, valueMap);
                break;
            case TestUploader.TEST_REST_PREFIX:
                transUDFNames(CustomizationService.TEST_ENTITY_NAME, valueMap);
                break;
            case RunUploader.RUN_PREFIX:
                transUDFNames(CustomizationService.RUN_ENTITY_NAME, valueMap);
                break;
            default:
        }
    }

    private void transUDFNames(String entityName, Map<String, String> entityMap) {
        List<String> tobeRemoved = new ArrayList<>();
        Map<String, String> tobeAdded = new HashMap<>();

        for (Map.Entry<String, String> entry : entityMap.entrySet()) {
            String name = entry.getKey();
            if (name.startsWith(UDF_PREFIX)) {
                String label = name.substring(
                        name.indexOf(UDF_PREFIX) + UDF_PREFIX.length());
                String realName = customizationService.getUDFNameByLabel(entityName, label);
                if (StringUtils.isNotEmpty(realName)) {
                    tobeAdded.put(realName, entityMap.get(name));
                    tobeRemoved.add(name);
                } else {
                    logger.error(String.format("No user defined field with label [%s] was found.", label));
                }
            }
        }
        entityMap.putAll(tobeAdded);
        for (String item : tobeRemoved) {
            entityMap.remove(item);
        }
    }
}
