/*
 *
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * © Copyright 2012-2019 Micro Focus or one of its affiliates..
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors (“Micro Focus”) are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 *
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
import com.microfocus.application.automation.tools.sse.sdk.Logger;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;

import static com.microfocus.application.automation.tools.commonResultUpload.ParamConstant.ALM_TEST_FOLDER;

public class TestUploader {

    public static final String TEST_REST_PREFIX = "tests";
    private static final String TEST_FOLDERS_REST_PREFIX = "test-folders";
    public static final String[] NO_VERSION_TESTS = new String[]{"ALT-SCENARIO",
            "LEANFT-TEST", "LR-SCENARIO", "QAINSPECT-TEST"};
    private static final String VC_VERSION_NUMBER = "vc-version-number";
    private static final String SUB_TYPE_ID = "subtype-id";

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
            Map<String, String> newTest;

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
                    newTest = existsTest;
                } else {
                    // If not, create the test under the folder
                    test.put(AlmCommonProperties.PARENT_ID, folder.get(AlmCommonProperties.ID));
                    newTest = restService.create(TEST_REST_PREFIX, test);
                }
            } else {
                // If no path was specified, put test under root
                test.put(AlmCommonProperties.PARENT_ID, "0");
                newTest = restService.create(TEST_REST_PREFIX, test);
            }

            if (newTest == null) {
                continue;
            } else {
                // upload test instance
                getVersionNumberForVC(newTest);
                test.putAll(newTest);
                testInstanceUploader.upload(testset, xmlResultEntity);
            }
        }
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
