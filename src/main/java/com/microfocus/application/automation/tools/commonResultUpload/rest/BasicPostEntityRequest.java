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
            builder.append(RestXmlUtils.fieldXml(entry.getKey(), StringEscapeUtils.escapeHtml4(entry.getValue())));
        }
        builder.append("</Fields></Entity>");
        logger.info("Request body: " + builder.toString());
        return builder.toString().getBytes();
    }

    private String getResultNameOrId(Map<String, String> result) {
        return result.get("name") != null ? result.get("name") : result.get("id");
    }

    protected Map<String, String> handleResult(Response response, Map<String, String> valueMap, String restPrefix) {
        if (response.isOk() && !response.toString().equals("")) {
            Map<String, String> result = XPathUtils.toEntities(response.toString()).get(0);
            logger.info(String.format("%s entity success. %s(%s)", operation, restPrefix,
                    getResultNameOrId(result)));
            return result;
        } else {
            logger.error(String.format("%s entity failed. %s(%s)", operation, restPrefix,
                    getResultNameOrId(valueMap)));
            logger.error(response.getFailure().toString());
            logger.error(getRestErrorMessage(response.toString()));
            return null;
        }
    }
}
