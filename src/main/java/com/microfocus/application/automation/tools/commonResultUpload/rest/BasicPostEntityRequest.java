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
package com.microfocus.application.automation.tools.commonResultUpload.rest;

import com.microfocus.adm.performancecenter.plugins.common.rest.RESTConstants;
import com.microfocus.application.automation.tools.commonResultUpload.CommonUploadLogger;
import com.microfocus.application.automation.tools.rest.RestClient;
import com.microfocus.application.automation.tools.sse.common.RestXmlUtils;
import com.microfocus.application.automation.tools.sse.common.XPathUtils;
import com.microfocus.application.automation.tools.sse.sdk.Response;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.HashMap;
import java.util.Map;

public abstract class BasicPostEntityRequest {

    private static final String START = "<Title>";
    private static final String END = "</Title>";
    private static final String IGNORE_REQUIRED_FIELDS_VALIDATION = "X-QC-Ignore-Customizable-Required-Fields-Validation";
    private static final String X_XSRF_TOKEN = "X-XSRF-TOKEN";

    protected RestClient client;
    protected CommonUploadLogger logger;
    protected String operation;

    protected BasicPostEntityRequest(RestClient client, CommonUploadLogger logger, String operation) {
        this.client = client;
        this.logger = logger;
        this.operation = operation;
    }

    public abstract Map<String, String> perform(String restPrefix, Map<String, String> valueMap);

    protected Map<String, String> getHeaders() {
        Map<String, String> ret = new HashMap<String, String>();
        ret.put(RESTConstants.CONTENT_TYPE, RESTConstants.APP_XML);
        ret.put(RESTConstants.ACCEPT, RESTConstants.APP_XML);
        ret.put(IGNORE_REQUIRED_FIELDS_VALIDATION, "Y");
        ret.put(X_XSRF_TOKEN, client.getXsrfTokenValue());
        return ret;
    }

    private String getRestErrorMessage(String responseContent) {
        return responseContent.substring(
                responseContent.indexOf(START) + START.length(),
                responseContent.indexOf(END)
        );
    }

    protected byte[] getDataBytes(Map<String, String> valueMap) {
        StringBuilder builder = new StringBuilder("<Entity><Fields>");
        for (Map.Entry<String, String> entry : valueMap.entrySet()) {
            builder.append(RestXmlUtils.fieldXml(entry.getKey(), StringEscapeUtils.escapeXml10(entry.getValue())));
        }
        builder.append("</Fields></Entity>");
        logger.info("Request body: " + builder.toString());
        return builder.toString().getBytes();
    }

    private String getResultNameAndId(Map<String, String> result) {
        return "id:" + result.get("id") + ", " + "name:" + result.get("name");
    }

    protected Map<String, String> handleResult(Response response, Map<String, String> valueMap, String restPrefix) {
        if (response.isOk() && !response.toString().equals("")) {
            Map<String, String> result = XPathUtils.toEntities(response.toString()).get(0);
            logger.info(String.format("%s entity success. %s(%s)", operation, restPrefix,
                    getResultNameAndId(result)));
            return result;
        } else {
            logger.error(String.format("%s entity failed. %s(%s)", operation, restPrefix,
                    getResultNameAndId(valueMap)));
            logger.error(response.getFailure().toString());
            logger.error(getRestErrorMessage(response.toString()));
            return null;
        }
    }
}
