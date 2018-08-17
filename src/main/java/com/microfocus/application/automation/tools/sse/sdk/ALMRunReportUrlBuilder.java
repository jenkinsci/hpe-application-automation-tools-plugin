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
 * © Copyright 2012-2018 Micro Focus or one of its affiliates.
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
            if (isNewReport(client)) {
                ret = String.format("%sui/?redirected&p=%s/%s&execution-report#/test-set-report/%s",
                        serverUrl,
                        domain,
                        project,
                        runId);
            } else {
                ret = client.buildWebUIRequest(String.format("lab/index.jsp?processRunId=%s", runId));
            }
        } catch (Exception e) {
            // result url will be NA (in case of failure like getting ALM version, convert ALM version to number)
        }

        return ret;
    }

    public boolean isNewReport(Client client) {

        ALMVersion version = getALMVersion(client);

        return toInt(version.getMajorVersion()) >= 12 && toInt(version.getMinorVersion()) >= 2;
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
