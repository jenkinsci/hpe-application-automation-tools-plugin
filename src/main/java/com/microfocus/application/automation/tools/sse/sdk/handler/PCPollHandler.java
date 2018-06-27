/*
 * © Copyright 2013 EntIT Software LLC
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright (c) 2018 Micro Focus Company, L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ___________________________________________________________________
 *
 */

package com.microfocus.application.automation.tools.sse.sdk.handler;

import java.util.Arrays;
import java.util.List;

import com.microfocus.application.automation.tools.sse.common.StringUtils;
import com.microfocus.application.automation.tools.sse.common.XPathUtils;
import com.microfocus.application.automation.tools.sse.sdk.Client;
import com.microfocus.application.automation.tools.sse.sdk.Logger;
import com.microfocus.application.automation.tools.sse.sdk.Response;
import com.microfocus.application.automation.tools.sse.sdk.request.GetPCRunEntityDataRequest;

/***
 * 
 * @author Effi Bar-She'an
 * @author Dani Schreiber
 * 
 */
public class PCPollHandler extends PollHandler {
    
    private final static List<String> FINAL_STATES = Arrays.asList("N/A", "Failed", "Passed");
    
    public PCPollHandler(Client client, String entityId) {
        
        super(client, entityId);
    }
    
    public PCPollHandler(Client client, String entityId, int interval) {
        
        super(client, entityId, interval);
    }
    
    @Override
    protected Response getResponse() {
        
        return new GetPCRunEntityDataRequest(_client, _runId).execute();
    }
    
    @Override
    protected boolean isFinished(Response response, Logger logger) {
        
        boolean ret = false;
        try {
            String xml = response.toString();
            String pcEndTime = XPathUtils.getAttributeValue(xml, "pc-end-time");
            String status = XPathUtils.getAttributeValue(xml, "status");
            if (!StringUtils.isNullOrEmpty(pcEndTime)) {
                logger.log(String.format("PC test end time: %s", pcEndTime));
                ret = true;
            } else if (!StringUtils.isNullOrEmpty(status)) {
                if (FINAL_STATES.contains(status)) {
                    ret = true;
                }
            }
        } catch (Throwable cause) {
            logger.log(String.format("Failed to parse response: %s", response));
            ret = true;
        }
        
        return ret;
    }
    
    @Override
    protected boolean logRunEntityResults(Response response, Logger logger) {
        
        boolean ret = false;
        try {
            String xml = response.toString();
            String status = XPathUtils.getAttributeValue(xml, "status");
            String state = XPathUtils.getAttributeValue(xml, "state");
            logger.log(String.format("Run status of %s: %s, State: %s", _runId, status, state));
            ret = true;
            
        } catch (Throwable cause) {
            logger.log(String.format("Failed to parse response: %s", response));
        }
        
        return ret;
    }
    
    @Override
    protected Response getRunEntityResultsResponse() {
        
        return new GetPCRunEntityDataRequest(_client, _runId).execute();
    }
}
