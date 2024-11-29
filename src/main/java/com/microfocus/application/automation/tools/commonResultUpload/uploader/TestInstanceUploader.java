/*
 *  Certain versions of software accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 *  This software was acquired by Micro Focus on September 1, 2017, and is now
 *  offered by OpenText.
 *  Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical
 *  in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the
 *  property of their respective owners.
 *  OpenText is a trademark of Open Text.
 *  __________________________________________________________________
 *  MIT License
 *
 *  Copyright 2012-2025 Open Text.
 *
 *  The only warranties for products and services of Open Text and
 *  its affiliates and licensors ("Open Text") are as may be set forth
 *  in the express warranty statements accompanying such products and services.
 *  Nothing herein should be construed as constituting an additional warranty.
 *  Open Text shall not be liable for technical or editorial errors or
 *  omissions contained herein. The information contained herein is subject
 *  to change without notice.
 *
 *  Except as specifically indicated otherwise, this document contains
 *  confidential information and a valid license is required for possession,
 *  use or copying. If this work is provided to the U.S. Government,
 *  consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 *  Computer Software Documentation, and Technical Data for Commercial Items are
 *  licensed to the U.S. Government under vendor's standard commercial license.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ___________________________________________________________________
 */
package com.microfocus.application.automation.tools.commonResultUpload.uploader;

import com.microfocus.application.automation.tools.commonResultUpload.CommonUploadLogger;
import com.microfocus.application.automation.tools.commonResultUpload.service.CustomizationService;
import com.microfocus.application.automation.tools.commonResultUpload.service.RestService;
import com.microfocus.application.automation.tools.commonResultUpload.xmlreader.model.XmlResultEntity;
import com.microfocus.application.automation.tools.results.service.AttachmentUploadService;
import com.microfocus.application.automation.tools.results.service.almentities.AlmCommonProperties;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTest;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTestInstance;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microfocus.application.automation.tools.commonResultUpload.ParamConstant.ACTUAL_USER;

public class TestInstanceUploader {

    private static final String TEST_CONFIG_PREFIX = "test-configs";
    public static final String TEST_INSTANCE_PREFIX = "test-instances";

    private CommonUploadLogger logger;
    private Map<String, String> params;
    private RestService restService;
    private RunUploader runUploader;
    private CustomizationService customizationService;

    public TestInstanceUploader(CommonUploadLogger logger, Map<String, String> params,
                                RestService restService, RunUploader runUploader,
                                CustomizationService customizationService) {
        this.logger = logger;
        this.params = params;
        this.restService = restService;
        this.runUploader = runUploader;
        this.customizationService = customizationService;
    }

    public void upload(Map<String, String> testset, XmlResultEntity xmlResultEntity, String attachment) {
        Map<String, String> test = xmlResultEntity.getValueMap();
        Map<String, String> testconfig = getMainTestConfig(test);
        Map<String, String> testinstance;

        if (testconfig != null) {
            List<Map<String, String>> testInstances = getExistTestInstances(testset, test, testconfig);
            if (testInstances != null) {
                if (testInstances.size() == 0) {
                    testinstance = buildNewTestInstance(testset, test, testconfig);
                    testinstance.putAll(restService.create(TEST_INSTANCE_PREFIX, testinstance));
                } else {
                    testinstance = testInstances.get(0);
                }

                if (StringUtils.isNotEmpty(attachment)) {
                    AttachmentUploadService.getInstance().upload(attachment, TEST_INSTANCE_PREFIX, testinstance.get("id"));
                }

                // Upload run
                if (xmlResultEntity.getSubEntities().size() > 0) {
                    runUploader.upload(testset, test, testconfig, testinstance,
                            xmlResultEntity.getSubEntities().get(0).getValueMap());
                } else {
                    logger.info("No run is found for test: " + test.get("name"));
                }
            }
        }
    }

    private Map<String, String> buildNewTestInstance(
            Map<String, String> testset,
            Map<String, String> test,
            Map<String, String> testconfig) {
        Map<String, String> testinstance = new HashMap<>();
        testinstance.put(AlmTestInstance.TEST_INSTANCE_SUBTYPE_ID,
                customizationService.getTestInstanceSubtypeIdByTest(test.get(AlmTest.TEST_TYPE)));
        testinstance.put(AlmTestInstance.TEST_INSTANCE_TESTSET_ID,
                String.valueOf(testset.get(AlmCommonProperties.ID)));
        testinstance.put(AlmTestInstance.TEST_INSTANCE_CONFIG_ID,
                String.valueOf(testconfig.get(AlmCommonProperties.ID)));
        testinstance.put(AlmTestInstance.TEST_INSTANCE_TEST_ID,
                String.valueOf(test.get(AlmCommonProperties.ID)));
        testinstance.put(AlmTestInstance.TEST_INSTANCE_TESTER_NAME,
                params.get(ACTUAL_USER));
        return testinstance;
    }

    private Map<String, String> getMainTestConfig(Map<String, String> test) {
        String queryString = String.format("query={parent-id[%s]}&fields=id,name",
                String.valueOf(test.get(AlmCommonProperties.ID)));
        List<Map<String, String>> testconfigs = restService.get(null, TEST_CONFIG_PREFIX, queryString);
        if (testconfigs != null && testconfigs.size() > 0) {
            return testconfigs.get(0);
        } else {
            return null;
        }
    }

    private List<Map<String, String>> getExistTestInstances(
            Map<String, String> testset,
            Map<String, String> test,
            Map<String, String> testconfig) {
        String queryString = String.format(
                "query={cycle-id[%s];test-config-id[%s];test-id[%s]}&fields=id,name,subtype-id",
                String.valueOf(testset.get(AlmCommonProperties.ID)),
                String.valueOf(testconfig.get(AlmCommonProperties.ID)),
                String.valueOf(test.get(AlmCommonProperties.ID)));
        return restService.get(null, TEST_INSTANCE_PREFIX, queryString);
    }
}
