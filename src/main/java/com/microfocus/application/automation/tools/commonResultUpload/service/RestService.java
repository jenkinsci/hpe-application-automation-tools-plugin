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

import com.microfocus.application.automation.tools.common.SSEException;
import com.microfocus.application.automation.tools.commonResultUpload.CommonUploadLogger;
import com.microfocus.application.automation.tools.commonResultUpload.rest.CreateAlmEntityEntityRequest;
import com.microfocus.application.automation.tools.commonResultUpload.rest.GetAlmEntityRequest;
import com.microfocus.application.automation.tools.commonResultUpload.rest.UpdateAlmEntityEntityRequest;
import com.microfocus.application.automation.tools.rest.RestClient;
import com.microfocus.application.automation.tools.sse.common.XPathUtils;
import com.microfocus.application.automation.tools.sse.sdk.ResourceAccessLevel;
import com.microfocus.application.automation.tools.sse.sdk.Response;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RestService {

    private RestClient restClient;
    private CreateAlmEntityEntityRequest createAlmEntityRequest;
    private GetAlmEntityRequest getAlmEntityRequest;
    private UpdateAlmEntityEntityRequest updateAlmEntityRequest;
    private UDFTranslator udt;
    private CommonUploadLogger logger;

    public RestService(RestClient restClient, CommonUploadLogger logger, UDFTranslator udt) {
        this.restClient = restClient;
        this.logger = logger;
        createAlmEntityRequest = new CreateAlmEntityEntityRequest(restClient, logger);
        getAlmEntityRequest = new GetAlmEntityRequest(restClient, logger);
        updateAlmEntityRequest = new UpdateAlmEntityEntityRequest(restClient, logger);
        this.udt = udt;
    }

    public Map<String, String> create(String restPrefix, Map<String, String> valueMap) {
        udt.translate(restPrefix, valueMap);
        return createAlmEntityRequest.perform(restPrefix, valueMap);
    }

    public List<Map<String, String>> get(String id, String restPrefix, String queryString) {
        return getAlmEntityRequest.perform(id, restPrefix, queryString);
    }

    public Map<String, String> update(String restPrefix, Map<String, String> valueMap) {
        udt.translate(restPrefix, valueMap);
        return updateAlmEntityRequest.perform(restPrefix, valueMap);
    }

    public List<String> getDomains() {
        String url = restClient.getServerUrl();
        if (!url.endsWith("/")) {
            url = String.format("%s/", url);
        }
        url = String.format("%s%s/domains", url, "rest");
        return getDomainOrProjectListFromResponse("Domain", url);
    }

    public List<String> getProjects(String domain) {
        String url = restClient.getServerUrl();
        if (!url.endsWith("/")) {
            url = String.format("%s/", url);
        }
        url = String.format("%s%s/domains/%s/projects", url, "rest", domain);
        return getDomainOrProjectListFromResponse("Project", url);
    }

    private List<String> getDomainOrProjectListFromResponse(String dopo, String url) {
        Response response = restClient.httpGet(url, null, null,
                ResourceAccessLevel.PROTECTED);

        List<String> list = new ArrayList<>();
        if (response.isOk() && !response.toString().equals("")) {
            Document document = null;
            try {
                document = XPathUtils.getDocument(response.toString());
            } catch (SSEException e) {
                logger.error("Get xml document failed: " + e.getMessage());
                logger.error("Please check ALM server's status.");
            }
            if (document != null) {
                NodeList domainList = document.getElementsByTagName(dopo);
                for (int i = 0; i < domainList.getLength(); i++) {
                    String project = domainList.item(i).getAttributes().getNamedItem("Name").getTextContent();
                    list.add(project);
                }
            } else {
                logger.error("Cannot get any content from response while getting " + dopo);
            }
        } else {
            logger.error("Get " + dopo + "s failed from: " + url);
            logger.error(response.getFailure().toString());
        }
        return list;
    }
}
