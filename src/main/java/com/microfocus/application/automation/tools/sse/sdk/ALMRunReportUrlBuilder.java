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
package com.microfocus.application.automation.tools.sse.sdk;

import com.microfocus.application.automation.tools.common.ALMRESTVersionUtils;
import com.microfocus.application.automation.tools.common.SSEException;
import com.microfocus.application.automation.tools.model.ALMVersion;
import com.microfocus.application.automation.tools.sse.sdk.request.GetALMVersionRequest;

/**
 * @author Effi Bar-She'an
 */
public class ALMRunReportUrlBuilder {

    public String build(Client client, String serverUrl, String domain, String project, String runId) {
        String ret = "NA";
        try {
            ALMVersion version = getALMVersion(client);
            int majorVersion = toInt(version.getMajorVersion());
            int minorVersion = toInt(version.getMinorVersion());
            if (majorVersion < 12 || (majorVersion == 12 && minorVersion < 2)) {
                ret = client.buildWebUIRequest(String.format("lab/index.jsp?processRunId=%s", runId));
            } else if (majorVersion >= 16) {
                // Url change due to angular js upgrade from ALM16
                ret = String.format("%sui/?redirected&p=%s/%s&execution-report#!/test-set-report/%s",
                        serverUrl,
                        domain,
                        project,
                        runId);
            } else {
                ret = String.format("%sui/?redirected&p=%s/%s&execution-report#/test-set-report/%s",
                        serverUrl,
                        domain,
                        project,
                        runId);
            }
        } catch (Exception e) {
            // result url will be NA (in case of failure like getting ALM version, convert ALM version to number)
        }
        return ret;
    }

    public boolean isNewReport(Client client) {
        ALMVersion version = getALMVersion(client);
        // Newer than 12.2x, including 12.5x, 15.x and later
        return (toInt(version.getMajorVersion()) == 12 && toInt(version.getMinorVersion()) >= 2)
                || toInt(version.getMajorVersion()) > 12;
    }

    private int toInt(String str) {

        return Integer.parseInt(str);
    }

    private ALMVersion getALMVersion(Client client) {

        ALMVersion ret = null;
        Response response = new GetALMVersionRequest(client).execute();
        if(response.isOk()) {
            ret = ALMRESTVersionUtils.toModel(response.getData());
        } else {
            throw new SSEException(
                    String.format("Failed to get ALM version. HTTP status code: %d", response.getStatusCode()),
                    response.getFailure());
        }

        return ret;
    }
}
