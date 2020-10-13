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
import com.microfocus.application.automation.tools.sse.sdk.ResourceAccessLevel;
import com.microfocus.application.automation.tools.sse.sdk.Response;

import java.util.HashMap;
import java.util.Map;

public class UpdateAlmEntityEntityRequest extends BasicPostEntityRequest {

    public UpdateAlmEntityEntityRequest(RestClient client, CommonUploadLogger logger) {
        super(client, logger, "Update");
    }

    @Override
    public Map<String, String> perform(String restPrefix, Map<String, String> valueMap) {
        String url = client.buildRestRequest(String.format("%s/%s", restPrefix, valueMap.get("id")));
        Response response = client.httpPut(
                url,
                getDataBytes(valueMap),
                getHeaders(),
                ResourceAccessLevel.PROTECTED);
        return handleResult(response, valueMap, restPrefix);
    }

    @Override
    protected Map<String, String> getHeaders() {
        Map<String, String> ret = new HashMap<String, String>();
        ret.put(RESTConstants.CONTENT_TYPE, RESTConstants.APP_XML_BULK);
        ret.put(RESTConstants.ACCEPT, RESTConstants.APP_XML);
        return ret;
    }
}
