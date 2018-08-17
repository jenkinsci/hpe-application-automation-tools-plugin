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

import com.microfocus.application.automation.tools.sse.sdk.Client;
import com.microfocus.application.automation.tools.sse.sdk.Logger;
import com.microfocus.application.automation.tools.sse.sdk.Response;

public abstract class PollHandler extends Handler {
    
    private int _interval = 5000; // millisecond
    
    public PollHandler(Client client, String entityId) {
        
        super(client, entityId);
    }
    
    public PollHandler(Client client, String entityId, int interval) {
        
        super(client, entityId);
        _interval = interval;
    }
    
    public PollHandler(Client client, String entityId, String runId) {
        
        super(client, entityId, runId);
    }

    public boolean poll(Logger logger) throws InterruptedException {

        logger.log(String.format("Polling... Run ID: %s", _runId));

        return doPoll(logger);
    }
    
    protected boolean doPoll(Logger logger) throws InterruptedException {
        
        boolean ret = false;
        int failures = 0;
        while (failures < 3) {
            Response response = getResponse();
            if (isOk(response, logger)) {
                log(logger);
                if (isFinished(response, logger)) {
                    ret = true;
                    logRunEntityResults(getRunEntityResultsResponse(), logger);
                    break;
                }
            } else {
                ++failures;
            }
            if (sleep(logger)) { // interrupted
                break;
            }
        }
        
        return ret;
    }
    
    protected abstract Response getRunEntityResultsResponse();
    
    protected abstract boolean logRunEntityResults(Response response, Logger logger);
    
    protected abstract boolean isFinished(Response response, Logger logger);
    
    protected abstract Response getResponse();
    
    protected boolean isOk(Response response, Logger logger) {
        
        boolean ret = false;
        if (!response.isOk()) {
            Throwable cause = response.getFailure();
            logger.log(String.format(
                    "Polling try failed. Status code: %s, Exception: %s",
                    response.getStatusCode(),
                    cause != null ? cause.getMessage() : "Not Available"));
        } else {
            ret = true;
        }
        
        return ret;
    }
    
    protected boolean sleep(Logger logger) throws InterruptedException {
        
        boolean ret = false;
        try {
            Thread.sleep(_interval);
        } catch (InterruptedException ex) {
            logger.log("Interrupted while polling");
            throw ex;
        }
        
        return ret;
    }
    
    protected void log(Logger logger) {}
}
