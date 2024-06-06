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

import com.microfocus.application.automation.tools.results.service.AlmRestTool;
import com.microfocus.application.automation.tools.results.service.almentities.AlmCommonProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CriteriaTranslator {

    public static final String CRITERIA_PREFIX = "q|";

    private CriteriaTranslator() {

    }

    public static String getCriteriaString(String[] fields, Map<String, String> entity) {
        List<String> tobeRemoved = new ArrayList<>();
        Map<String, String> tobeAdded = new HashMap<>();

        StringBuilder sb = new StringBuilder();
        sb.append("fields=");
        for (int i = 0; i < fields.length; i++) {
            sb.append(fields[i]);
            if (i < fields.length - 1) {
                sb.append(",");
            }
        }

        sb.append("&query={");
        for (Map.Entry<String, String> entry : entity.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(CRITERIA_PREFIX)) {
                tobeRemoved.add(key);
                String realName = key.substring(
                        key.indexOf(CRITERIA_PREFIX) + CRITERIA_PREFIX.length());
                tobeAdded.put(realName, entity.get(key));
                sb.append(realName).append("[")
                        .append(AlmRestTool.getEncodedString(entity.get(key)))
                        .append("]").append(";");
            }
        }
        // Search by name by default
        if (tobeRemoved.size() == 0) {
            sb.append(AlmCommonProperties.NAME).append("[")
                    .append(AlmRestTool.getEncodedString(entity.get("name")))
                    .append("];");
        }
        sb.append("}");

        entity.putAll(tobeAdded);
        for (String item : tobeRemoved) {
            entity.remove(item);
        }
        return sb.toString();
    }
}
