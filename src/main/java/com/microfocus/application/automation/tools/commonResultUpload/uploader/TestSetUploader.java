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
import com.microfocus.application.automation.tools.commonResultUpload.service.FolderService;
import com.microfocus.application.automation.tools.commonResultUpload.service.RestService;
import com.microfocus.application.automation.tools.commonResultUpload.xmlreader.model.XmlResultEntity;
import com.microfocus.application.automation.tools.results.service.AttachmentUploadService;
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

        Map<String, String> folder = createOrFindTestsetFolder();

        for (XmlResultEntity xmlResultEntity : xmlResultEntities) {
            Map<String, String> testset = xmlResultEntity.getValueMap();

            // Find if there is test set with same name in the defined folder
            Map<String, String> existTestset = folderService.findEntityInFolder(folder, testset,
                    TEST_SET_REST_PREFIX, TEST_SET_FOLDERS_REST_PREFIX,
                    new String[]{"id", "name", "subtype-id"});

            uploadOrUpdateTestset(existTestset, testset, xmlResultEntity);
        }
    }

    private boolean uploadOrUpdateTestset(Map<String, String> existTestset,
                                          Map<String, String> testset, XmlResultEntity xmlResultEntity) {
        Map<String, String> newTestset;
        String attachment = testset.get("attachment");
        testset.remove("attachment");

        if (existTestset != null) {
            // If yes, use the exist one to update.
            newTestset = existTestset;
        } else {
            // If no, create test set under folder
            newTestset =  restService.create(TEST_SET_REST_PREFIX, testset);
        }

        if (newTestset == null) {
            return false;
        } else {
            if (StringUtils.isNotEmpty(attachment)) {
                AttachmentUploadService.getInstance().upload(attachment, TEST_SET_REST_PREFIX, newTestset.get("id"));
            }
            testuploader.upload(newTestset, xmlResultEntity.getSubEntities());
        }
        return true;
    }

    private Map<String, String> createOrFindTestsetFolder() {
        if (!StringUtils.isEmpty(params.get(ALM_TESTSET_FOLDER))) {
            return folderService.createOrFindPath(
                    TEST_SET_FOLDERS_REST_PREFIX,
                    "0",
                    params.get(ALM_TESTSET_FOLDER));
        } else {
            return null;
        }
    }
}
