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

package com.microfocus.application.automation.tools.sse.sdk.handler;

import java.util.List;
import java.util.Map;

import com.microfocus.application.automation.tools.sse.common.StringUtils;
import com.microfocus.application.automation.tools.sse.common.XPathUtils;
import com.microfocus.application.automation.tools.sse.sdk.Client;
import com.microfocus.application.automation.tools.sse.sdk.Logger;
import com.microfocus.application.automation.tools.sse.sdk.Response;
import com.microfocus.application.automation.tools.sse.sdk.request.EventLogRequest;

public class EventLogHandler extends Handler {
    
    private String _timeslotId = StringUtils.EMPTY_STRING;
    private int _lastRead = -1;
    
    public EventLogHandler(Client client, String timeslotId) {
        
        super(client, timeslotId);
        _timeslotId = timeslotId;
    }
    
    public boolean log(Logger logger) {
        
        boolean ret = false;
        Response eventLog = null;
        try {
            eventLog = getEventLog();
            String xml = eventLog.toString();
            List<Map<String, String>> entities = XPathUtils.toEntities(xml);
            for (Map<String, String> currEntity : entities) {
                if (isNew(currEntity)) {
                    logger.log(String.format(
                            "%s:%s",
                            currEntity.get("creation-time"),
                            currEntity.get("description")));
                }
            }
            ret = true;
        } catch (Throwable cause) {
            logger.log(String.format(
                    "Failed to print Event Log: %s (run id: %s, reservation id: %s). Cause: %s",
                    eventLog,
                    _runId,
                    _timeslotId,
                    cause));
        }
        
        return ret;
    }
    
    private boolean isNew(Map<String, String> currEntity) {
        
        boolean ret = false;
        int currEvent = Integer.parseInt(currEntity.get("id"));
        if (currEvent > _lastRead) {
            _lastRead = currEvent;
            ret = true;
        }
        
        return ret;
    }
    
    private Response getEventLog() {
        
        return new EventLogRequest(_client, _timeslotId).execute();
    }
    
}
