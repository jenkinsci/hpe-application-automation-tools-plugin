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

package com.microfocus.application.automation.tools.mc;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

public class HttpResponse {

    private Map<String, List<String>> headers;
    private String strCookies;
    private JSONObject jsonObject;
    private JSONArray jsonArray;

    public HttpResponse() {

    }

    public HttpResponse(Map<String, List<String>> headers, JSONObject jsonObject) {
        this.headers = headers;
        this.jsonObject = jsonObject;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    public void setJsonObject(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public void setCookiesString(String cookies) {
        this.strCookies = cookies;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    public JSONArray getJsonArray() {
        return jsonArray;
    }

    public void setJsonArray(JSONArray jsonArray) {
        this.jsonArray = jsonArray;
    }

    public String getCookiesAsString() {
        if (StringUtils.isBlank(strCookies))
        {
            StringBuilder sb = new StringBuilder();
            List<String> cookies = headers.get(Constants.SET_COOKIE);
            if (cookies != null)
                for (String cookie : cookies) {
                    int eqIdx = cookie.indexOf('=');
                    int semicolonIdx = cookie.indexOf(';');
                    String key = cookie.substring(0, eqIdx);
                    String val = cookie.substring(eqIdx + 1, semicolonIdx);
                    sb.append(key).append("=").append(val).append(";");
                }
            strCookies = sb.toString();
        }
        return strCookies;
    }
}
