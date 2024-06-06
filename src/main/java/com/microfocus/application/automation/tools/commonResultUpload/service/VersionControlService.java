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
import com.microfocus.application.automation.tools.sse.sdk.ResourceAccessLevel;
import com.microfocus.application.automation.tools.sse.sdk.Response;

public class VersionControlService  {

    public static final String CHECK_IN = "check-in";
    public static final String CHECK_OUT = "check-out";
    public static final String CHECK_UNDO_CHECK_OUT = "undo-check-out";

    private RestClient client;
    private CommonUploadLogger logger;

    public VersionControlService(RestClient client, CommonUploadLogger logger) {
        this.client = client;
        this.logger = logger;
    }

    /**
     * For new created entity, you can only get it's version number after check out and undo check out.
     * @param restPrefix
     * @param entityId
     * @return
     */
    public boolean refreshEntityVersion(String restPrefix, String entityId) {
        boolean result = false;
        result = versionsCheck(restPrefix, entityId, CHECK_OUT);
        if (!result) {
            return false;
        }
        return versionsCheck(restPrefix, entityId, CHECK_UNDO_CHECK_OUT);
    }

    /**
     * check in, check out, undo check out a entity
     * @param restPrefix
     * @param entityId
     * @param operation
     * @return
     */
    public boolean versionsCheck(String restPrefix, String entityId, String operation) {
        String suffix = String.format("%s/%s/versions/%s", restPrefix, entityId, operation);
        String url = client.buildRestRequest(suffix);
        Response response = client.httpPost(url, null, null, ResourceAccessLevel.PROTECTED);
        if (!response.isOk()) {
            logger.error(String.format("%s entity failed. %s(%s)", operation, restPrefix, entityId));
            logger.error(response.getFailure().toString());
            return false;
        } else {
            return true;
        }
    }
}
