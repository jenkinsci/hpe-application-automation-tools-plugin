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
import com.microfocus.application.automation.tools.commonResultUpload.service.FolderService;
import com.microfocus.application.automation.tools.commonResultUpload.service.RestService;
import com.microfocus.application.automation.tools.commonResultUpload.service.UDFTranslator;
import com.microfocus.application.automation.tools.commonResultUpload.service.VersionControlService;
import com.microfocus.application.automation.tools.commonResultUpload.xmlreader.configloader.RunStatusMapLoader;
import com.microfocus.application.automation.tools.commonResultUpload.xmlreader.model.EntitiesFieldMap;
import com.microfocus.application.automation.tools.commonResultUpload.xmlreader.configloader.EntitiesFieldMapLoader;
import com.microfocus.application.automation.tools.commonResultUpload.xmlreader.model.RunStatusMap;
import com.microfocus.application.automation.tools.commonResultUpload.xmlreader.XmlReader;
import com.microfocus.application.automation.tools.commonResultUpload.xmlreader.model.XmlResultEntity;
import com.microfocus.application.automation.tools.rest.RestClient;
import com.microfocus.application.automation.tools.results.service.AlmRestTool;
import com.microfocus.application.automation.tools.results.service.AttachmentUploadService;
import com.microfocus.application.automation.tools.sse.sdk.authenticator.AuthenticationTool;
import hudson.FilePath;
import hudson.model.Run;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.microfocus.application.automation.tools.commonResultUpload.ParamConstant.*;

public class Uploader {

    private RestClient restClient;
    private Map<String, String> params;
    private CommonUploadLogger logger;
    private CustomizationService cs;
    private VersionControlService vs;
    private UDFTranslator udt;
    private RestService rs;
    private FolderService fs;
    private Run<?, ?> run;
    private FilePath workspace;

    public Uploader(Run<?, ?> run, FilePath workspace, CommonUploadLogger logger, Map<String, String> params) {
        this.run = run;
        this.workspace = workspace;
        this.logger = logger;
        this.params = params;
    }

    public void upload() {
        restClient = new RestClient(params.get(ALM_SERVER_URL),
                params.get(ALM_DOMAIN),
                params.get(ALM_PROJECT),
                params.get(USERNAME));

        boolean login = AuthenticationTool.getInstance().authenticate(restClient,
                params.get(USERNAME), params.get(PASS),
                params.get(ALM_SERVER_URL), params.get(CLIENT_TYPE), logger);
        if (login) {
            init();
            if (!rs.getDomains().contains(params.get(ALM_DOMAIN))) {
                logger.error("Invalid domain name:" + params.get(ALM_DOMAIN));
                return;
            }
            if (!rs.getProjects(params.get(ALM_DOMAIN)).contains(params.get(ALM_PROJECT))) {
                logger.error("Invalid project name:" + params.get(ALM_PROJECT));
                return;
            }
            List<XmlResultEntity> xmlResultEntities = getUploadData();
            if (xmlResultEntities == null || xmlResultEntities.size() == 0) {
                return;
            }
            TestSetUploader testSetUploader = getTestSetUploader();
            if (testSetUploader == null) {
                return;
            }
            AlmRestTool almRestTool = new AlmRestTool(restClient, logger);
            params.put(ACTUAL_USER, almRestTool.getActualUsername());
            testSetUploader.upload(xmlResultEntities);
        } else {
            logger.error("Login failed.");
        }
    }

    private void init() {
        cs = new CustomizationService(restClient, logger);
        vs = new VersionControlService(restClient, logger);
        udt = new UDFTranslator(cs, logger);
        rs = new RestService(restClient, logger, udt);
        fs = new FolderService(rs);
        AttachmentUploadService.init(run, workspace, restClient, logger);
    }

    private TestSetUploader getTestSetUploader() {
        RunStatusMap runStatusMap = RunStatusMapLoader.load(params.get(RUN_STATUS_MAPPING), logger);
        if (runStatusMap == null) {
            return null;
        }
        RunUploader runu = new RunUploader(logger, params, rs, cs, runStatusMap.getStatus());
        TestInstanceUploader tiu = new TestInstanceUploader(logger, params, rs, runu, cs);
        TestUploader testu = new TestUploader(logger, params, rs, fs, tiu, cs, vs);
        return new TestSetUploader(logger, params, rs, fs, testu);
    }

    private List<XmlResultEntity> getUploadData() {
        List<XmlResultEntity> xmlResultEntities = new ArrayList<>();

        EntitiesFieldMap entitiesFieldMap = EntitiesFieldMapLoader.load(params.get(FIELD_MAPPING), logger, cs,
                "true".equals(params.get(CREATE_NEW_TEST)));
        if (entitiesFieldMap == null) {
            return xmlResultEntities;
        }

        XmlReader xmlReader = new XmlReader(run, workspace, logger);
        xmlResultEntities = xmlReader.scan(params.get(TESTING_RESULT_FILE), entitiesFieldMap);
        if (xmlResultEntities == null || xmlResultEntities.size() == 0) {
            logger.error("No test result content is found.");
        }
        return xmlResultEntities;
    }
}
