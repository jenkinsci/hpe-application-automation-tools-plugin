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
