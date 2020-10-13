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
