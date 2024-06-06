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

import net.minidev.json.JSONObject;
import org.apache.commons.lang.StringUtils;

import java.util.regex.Pattern;

public class Oauth2TokenUtil {

    private static String client;
    private static String secret;
    private static String tenant;

    private Oauth2TokenUtil() {
    }

    public static boolean isValid(String auth2) {
        String strCleaned = removeQuotes(auth2.trim());
        if (StringUtils.isBlank(strCleaned)) {
            return false;
        }

        String[] a = strCleaned.split(Pattern.quote(";"));
        for (String s : a) {
            if (StringUtils.isEmpty(s)) {
                continue;
            }
            if (!extractField(s.trim())) {
                return false;
            }
        }
        return true;
    }

    private static String removeQuotes(final String str) {
        if (str.endsWith("\"") && str.startsWith("\"") && str.length() > 1) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    private static boolean extractField(String str) {
        int pos = str.indexOf('=');
        if (pos < 0) {
            return false;
        }

        String key = str.substring(0, pos).trim();
        String value = str.substring(pos + 1).trim();

        if ("client".equalsIgnoreCase(key)) {
            client = value;
        } else if ("secret".equalsIgnoreCase(key)) {
            secret = value;
        } else if ("tenant".equalsIgnoreCase(key)) {
            tenant = value;
        } else {
            return false;
        }
        return true;
    }

    public static JSONObject getJSONObject() {
        JSONObject sendObject = new JSONObject();
        sendObject.put("client", client);
        sendObject.put("secret", secret);
        sendObject.put("tenant", tenant);
        return sendObject;
    }

}