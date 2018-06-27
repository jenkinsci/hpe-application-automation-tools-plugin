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

package com.microfocus.application.automation.tools.sse.sdk.request;

import java.util.ArrayList;
import java.util.List;

import com.microfocus.application.automation.tools.common.Pair;
import com.microfocus.application.automation.tools.model.CdaDetails;
import com.microfocus.application.automation.tools.sse.common.StringUtils;
import com.microfocus.application.automation.tools.sse.sdk.Client;

/***
 * 
 * @author Effi Bar-She'an
 * @author Dani Schreiber
 * 
 */

public class StartRunEntityRequest extends PostRequest {
    
    private final String _duration;
    private final String _suffix;
    private final String _environmentConfigurationId;
    private final CdaDetails _cdaDetails;
    
    public StartRunEntityRequest(
            Client client,
            String suffix,
            String runId,
            String duration,
            String postRunAction,
            String environmentConfigurationId,
            CdaDetails cdaDetails) {
        
        super(client, runId);
        _duration = duration;
        _suffix = suffix;
        _environmentConfigurationId = environmentConfigurationId;
        _cdaDetails = cdaDetails;
    }
    
    @Override
    protected List<Pair<String, String>> getDataFields() {
        
        List<Pair<String, String>> ret = new ArrayList<Pair<String, String>>();
        ret.add(new Pair<String, String>("duration", _duration));
        ret.add(new Pair<String, String>("vudsMode", "false"));
        ret.add(new Pair<String, String>("reservationId", "-1"));
        if (!StringUtils.isNullOrEmpty(_environmentConfigurationId)) {
            ret.add(new Pair<String, String>("valueSetId", _environmentConfigurationId));
            if (_cdaDetails != null) {
                ret.add(new Pair<String, String>("topologyAction", _cdaDetails.getTopologyAction()));
                ret.add(new Pair<String, String>(
                        "realizedTopologyName",
                        _cdaDetails.getDeployedEnvironmentName()));
                
            }
        }
        
        return ret;
    }
    
    @Override
    protected String getSuffix() {
        
        return _suffix;
    }
    
}
