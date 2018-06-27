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

package com.microfocus.application.automation.tools.results.service.rest;

import com.microfocus.application.automation.tools.common.Pair;
import com.microfocus.adm.performancecenter.plugins.common.rest.RESTConstants;
import com.microfocus.application.automation.tools.results.service.almentities.AlmEntity;
import com.microfocus.application.automation.tools.sse.sdk.Client;
import com.microfocus.application.automation.tools.sse.sdk.request.PostRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateAlmEntityRequest extends PostRequest {
	
	List<Pair<String, String>> attrForCreation;
	AlmEntity almEntity;
	private static final String IGNORE_REQUIRED_FIELDS_VALIDATION = "X-QC-Ignore-Customizable-Required-Fields-Validation";
	public CreateAlmEntityRequest(Client client, AlmEntity almEntity, List<Pair<String, String>> attrForCreation){
		super(client, "");
		this.attrForCreation = attrForCreation;
		this.almEntity = almEntity;
	}
	
    @Override
    protected Map<String, String> getHeaders() {

        Map<String, String> ret = new HashMap<String, String>();
        ret.put(RESTConstants.CONTENT_TYPE, RESTConstants.APP_XML);
        ret.put(RESTConstants.ACCEPT, RESTConstants.APP_XML);
        ret.put(IGNORE_REQUIRED_FIELDS_VALIDATION, "Y");
        return ret;
    }
    
	@Override
	protected String getSuffix() {
		// TODO Auto-generated method stub
		return almEntity.getRestPrefix();
	}

    protected List<Pair<String, String>> getDataFields() {
        
        return attrForCreation;
    }
}
