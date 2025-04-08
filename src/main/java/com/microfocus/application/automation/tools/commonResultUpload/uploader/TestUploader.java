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

package com.microfocus.application.automation.tools.commonResultUpload.uploader;

import com.microfocus.application.automation.tools.commonResultUpload.CommonUploadLogger;
import com.microfocus.application.automation.tools.commonResultUpload.service.CriteriaTranslator;
import com.microfocus.application.automation.tools.commonResultUpload.service.CustomizationService;
import com.microfocus.application.automation.tools.commonResultUpload.service.FolderService;
import com.microfocus.application.automation.tools.commonResultUpload.service.RestService;
import com.microfocus.application.automation.tools.commonResultUpload.service.VersionControlService;
import com.microfocus.application.automation.tools.commonResultUpload.xmlreader.model.XmlResultEntity;
import com.microfocus.application.automation.tools.results.service.almentities.AlmCommonProperties;
import org.apache.commons.lang.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.microfocus.application.automation.tools.commonResultUpload.ParamConstant.*;
import static com.microfocus.application.automation.tools.results.service.AlmRestTool.getEncodedString;

public class TestUploader {

    public static final String TEST_REST_PREFIX = "tests";
    private static final String TEST_FOLDERS_REST_PREFIX = "test-folders";
    public static final String[] NO_VERSION_TESTS = new String[]{"ALT-SCENARIO",
            "LEANFT-TEST", "LR-SCENARIO", "QAINSPECT-TEST"};
    private static final String VC_VERSION_NUMBER = "vc-version-number";
    private static final String SUB_TYPE_ID = "subtype-id";
    private static final int triedTimes = 30;

    private Map<String, String> params;
    private CommonUploadLogger logger;
    private RestService restService;
    private FolderService folderService;
    private CustomizationService customizationService;
    private TestInstanceUploader testInstanceUploader;
    private VersionControlService versionControlService;

    public TestUploader(CommonUploadLogger logger, Map<String, String> params,
                        RestService restService, FolderService folderService,
                        TestInstanceUploader testInstanceUploader,
                        CustomizationService customizationService,
                        VersionControlService versionControlService) {
        this.logger = logger;
        this.params = params;
        this.restService = restService;
        this.folderService = folderService;
        this.testInstanceUploader = testInstanceUploader;
        this.customizationService = customizationService;
        this.versionControlService = versionControlService;
    }

    public void upload(Map<String, String> testset, List<XmlResultEntity> xmlResultEntities) {
        logger.info("Test upload start.");
        for (XmlResultEntity xmlResultEntity : xmlResultEntities) {
            Map<String, String> test = xmlResultEntity.getValueMap();
            Map<String, String> newTest = null;

            String attachment = test.get("attachment");
            test.remove("attachment");

            boolean isNew = false;
            boolean isUpdate = false;

            if (!StringUtils.isEmpty(params.get(ALM_TEST_FOLDER))) {
                // Create or find a exists folder
                Map<String, String> folder = folderService.createOrFindPath(
                        TEST_FOLDERS_REST_PREFIX, "2", params.get(ALM_TEST_FOLDER));
                if (folder == null) {
                    continue;
                }

                // Find exists test under folder
                Map<String, String> existsTest = folderService.findEntityInFolder(folder, test,
                        TEST_REST_PREFIX, TEST_FOLDERS_REST_PREFIX,
                        new String[]{"id", "name", SUB_TYPE_ID, VC_VERSION_NUMBER});
                if (existsTest != null) {
                    // If exists, update the test.
                    if (xmlResultEntity.getSubEntities().size() > 0) {
                        Map<String,String> runFieldsMap = xmlResultEntity.getSubEntities().get(0).getValueMap();
                        if (runFieldsMap != null && runFieldsMap.containsKey("stepMessage")) {
                            if (params.get(UPDATE_DESSTEPS).equals("true")) {
                                isUpdate = true;
                            } else {
                                test.put(AlmCommonProperties.PARENT_ID, folder.get(AlmCommonProperties.ID));
                                newTest = createNewTest(test);
                                isNew = true;
                            }
                        }
                    }
                    if (!isNew) {
                        existsTest.putAll(test);
                        newTest = restService.update(TEST_REST_PREFIX, existsTest);
                    }
                } else {
                    logger.log("Test not found by criteria:");
                    for (Map.Entry<String, String> entry : test.entrySet()) {
                        if (entry.getKey().equals("name") || entry.getKey().startsWith(CriteriaTranslator.CRITERIA_PREFIX)) {
                            logger.log("----" + entry.getKey() + "=" + entry.getValue());
                        }
                    }

                    // If not, create the test under the folder
                    test.put(AlmCommonProperties.PARENT_ID, folder.get(AlmCommonProperties.ID));
                    if (params.get(CREATE_NEW_TEST).equals("true")) {
                        newTest = restService.create(TEST_REST_PREFIX, test);
                        isNew = true;
                    } else {
                        newTest = null;
                        logger.log("Test not found and not created: " + test.toString());
                    }
                }
            } else {
                // If no path was specified, put test under root
                test.put(AlmCommonProperties.PARENT_ID, "0");
                if (params.get(CREATE_NEW_TEST).equals("true")) {
                    newTest = restService.create(TEST_REST_PREFIX, test);
                    isNew = true;
                } else {
                    newTest = null;
                    logger.log("Test not found and not created: " + test.toString());
                }
            }

            if (newTest == null) {
                continue;
            } else {
                // upload test instance
                getVersionNumberForVC(newTest);
                test.putAll(newTest);
                testInstanceUploader.upload(testset, xmlResultEntity, attachment, isNew||isUpdate);
            }
        }
    }

    private Map<String, String> createNewTest(Map<String, String> test) {
        Map<String, String> newTest = null;
        String testName = test.get(AlmCommonProperties.NAME);
        for (int i = 0; i < triedTimes; i++) {
            String query = String.format("fields=id,name&query={parent-id[%s];name[%s]}",test.get(AlmCommonProperties.PARENT_ID),getEncodedString(testName));
            List<Map<String, String>> tests = restService.get(null, TEST_REST_PREFIX, query);
            if (tests != null && tests.size() > 0) {
                logger.log("Test[" + testName + "] already exists.");
                testName = buildNewTestName(testName);
            } else {
                test.put(AlmCommonProperties.NAME,testName);
                newTest = restService.create(TEST_REST_PREFIX, test);
                break;
            }
        }
        return newTest;
    }

    private String buildNewTestName(String testName) {
        Date currentDate = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        String formattedDate = formatter.format(currentDate);

        String suffixPattern = "_Copy_(\\d+)_" + formattedDate + "$";
        Pattern pattern = Pattern.compile(suffixPattern);
        Matcher matcher = pattern.matcher(testName);
        if (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1)) + 1;
            String newSuffix = "_Copy_" + index + "_" + formattedDate;
            testName = testName.replace(matcher.group(0), newSuffix);
        } else {
            testName = testName + "_Copy_0_" + formattedDate;
        }
        return testName;
    }

    private void getVersionNumberForVC(Map<String, String> newTest) {
        // Some test type doesn't have version support
        for (String noVersionTest : NO_VERSION_TESTS) {
            if (newTest.get(SUB_TYPE_ID).equals(noVersionTest)) {
                return;
            }
        }

        boolean versioningEnabled = customizationService.isVersioningEnabled(
                CustomizationService.TEST_ENTITY_NAME);
        if (versioningEnabled && StringUtils.isEmpty(newTest.get(VC_VERSION_NUMBER))) {
            versionControlService.refreshEntityVersion(TEST_REST_PREFIX,
                    newTest.get(AlmCommonProperties.ID));

            newTest.putAll(restService.get(newTest.get(AlmCommonProperties.ID),
                    TEST_REST_PREFIX, CriteriaTranslator.getCriteriaString(
                            new String[]{"id", "name", SUB_TYPE_ID, VC_VERSION_NUMBER}, newTest)).get(0));
        }
    }
}
