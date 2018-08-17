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
