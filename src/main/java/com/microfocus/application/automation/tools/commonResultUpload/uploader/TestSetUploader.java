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
import com.microfocus.application.automation.tools.commonResultUpload.service.FolderService;
import com.microfocus.application.automation.tools.commonResultUpload.service.RestService;
import com.microfocus.application.automation.tools.commonResultUpload.xmlreader.model.XmlResultEntity;
import com.microfocus.application.automation.tools.results.service.almentities.AlmCommonProperties;
import com.microfocus.application.automation.tools.sse.sdk.Logger;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;

import static com.microfocus.application.automation.tools.commonResultUpload.ParamConstant.ALM_TESTSET_FOLDER;

public class TestSetUploader {

    public static final String TEST_SET_REST_PREFIX = "test-sets";
    private static final String TEST_SET_FOLDERS_REST_PREFIX = "test-set-folders";

    private Map<String, String> params;
    private CommonUploadLogger logger;
    private RestService restService;
    private FolderService folderService;
    private TestUploader testuploader;

    public TestSetUploader(CommonUploadLogger logger, Map<String, String> params,
                           RestService restService,
                           FolderService folderService,
                           TestUploader testuploader) {
        this.logger = logger;
        this.params = params;
        this.restService = restService;
        this.folderService = folderService;
        this.testuploader = testuploader;
    }

    public void upload(List<XmlResultEntity> xmlResultEntities) {
        logger.info("Test set upload start.");
        for (XmlResultEntity xmlResultEntity : xmlResultEntities) {
            Map<String, String> testset = xmlResultEntity.getValueMap();

            // Find if there is test set with same name
            List<Map<String, String>> existTestsets = restService.get(null,
                    TEST_SET_REST_PREFIX,
                    CriteriaTranslator.getCriteriaString(
                            new String[]{"id", "name", "subtype-id"}, testset));

            if (existTestsets == null) {
                continue;
            }

            uploadOrUpdateTestset(existTestsets, testset, xmlResultEntity);
        }
    }

    private boolean uploadOrUpdateTestset(List<Map<String, String>> existTestsets,
                                          Map<String, String> testset, XmlResultEntity xmlResultEntity) {
        Map<String, String> newTestset;
        if (existTestsets.size() > 0) {
            newTestset = existTestsets.get(0);
        } else {
            // If no, create test set under folder
            newTestset = createTestsetInFolder(testset);
        }
        if (newTestset == null) {
            return false;
        } else {
            testuploader.upload(newTestset, xmlResultEntity.getSubEntities());
        }
        return true;
    }

    private Map<String, String> createTestsetInFolder(Map<String, String> testset) {
        if (!StringUtils.isEmpty(params.get(ALM_TESTSET_FOLDER))) {
            Map<String, String> folder = folderService.createOrFindPath(
                    TEST_SET_FOLDERS_REST_PREFIX,
                    "0",
                    params.get(ALM_TESTSET_FOLDER));
            if (folder == null) {
                return null;
            }
            testset.put(AlmCommonProperties.PARENT_ID, folder.get(AlmCommonProperties.ID));
        } else {
            // Or create test set under root
            testset.put(AlmCommonProperties.PARENT_ID, "0");
        }
        return restService.create(TEST_SET_REST_PREFIX, testset);
    }
}
