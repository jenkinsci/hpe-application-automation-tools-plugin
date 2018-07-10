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

package com.hpe.application.automation.tools.results.service;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hpe.application.automation.tools.common.Pair;
import com.hpe.application.automation.tools.rest.RestClient;
import com.hpe.application.automation.tools.results.service.almentities.AlmEntity;
import com.hpe.application.automation.tools.results.service.rest.CreateAlmEntityRequest;
import com.hpe.application.automation.tools.results.service.rest.GetAlmEntityRequest;
import com.hpe.application.automation.tools.results.service.rest.UpdateAlmEntityRequest;
import com.hpe.application.automation.tools.sse.common.XPathUtils;
import com.hpe.application.automation.tools.sse.sdk.Logger;
import com.hpe.application.automation.tools.sse.sdk.Response;
import com.hpe.application.automation.tools.sse.sdk.authenticator.AuthenticationTool;

public class AlmRestTool {
	
	private Logger _logger ;
	private RestClient restClient;
	private AlmRestInfo almLoginInfo;
	
	public AlmRestTool (AlmRestInfo almLoginInfo, Logger logger) {
		this.restClient = new RestClient(
        							almLoginInfo.getServerUrl(),
        							almLoginInfo.getDomain(),
        							almLoginInfo.getProject(),
        							almLoginInfo.getUserName());
		this.almLoginInfo = almLoginInfo;
		this._logger = logger;
	}

    /**
     * Get rest client
     */
	public RestClient getRestClient() {
		return this.restClient;
	}

    /**
     * Login
     */
	public boolean login() throws Exception {
		boolean ret;
        try {
			ret = AuthenticationTool.authenticate(restClient, almLoginInfo.getUserName(),
					almLoginInfo.getPassword(), almLoginInfo.getServerUrl(), almLoginInfo.getClientType(), _logger);
        } catch (Exception cause) {
            ret = false;
            throw new AlmRestException (cause);
        }
        return ret;
	}

    /**
     * Get Pair list for ALM entity fields
     */
	public List<Pair<String, String>> getPairListForAlmEntityFields(AlmEntity almEntity, List<String> fieldNames){
		List<Pair<String, String>> pairs = new ArrayList<Pair<String, String>>();
		for(String fieldName : fieldNames) {
			pairs.add(new Pair<String, String>(fieldName, String.valueOf(almEntity.getFieldValue(fieldName))));	
		}
		return pairs;
	}

    /**
     * Get pair list for ALM entity fields
     */
	public List<Pair<String, String>> getPairListForAlmEntityFields(AlmEntity almEntity, String[] fieldNames){
		List<Pair<String, String>> pairs = new ArrayList<Pair<String, String>>();
		for(String fieldName : fieldNames) {
			pairs.add(new Pair<String, String>(fieldName, String.valueOf(almEntity.getFieldValue(fieldName))));	
		}
		return pairs;
	}

    /**
     * Get map list for ALM entity fields
     */
	public List<Map<String, String>> getMapListForAlmEntityFields(AlmEntity almEntity, String[] fieldNames){
		List<Map<String, String>> fieldsMapList = new ArrayList<Map<String, String>>();
		
		Map<String, String> fieldsMap = new HashMap<String, String> ();
		
		for(String fieldName : fieldNames) {
			fieldsMap.put(fieldName, String.valueOf(almEntity.getFieldValue(fieldName)));	
		}
		fieldsMapList.add(fieldsMap);
		return fieldsMapList;
	}

    /**
     * Populate ALM entity field value
     */
	public void populateAlmEntityFieldValue(Map<String, String> mapFieldValue, AlmEntity almEntity) {
		for(Map.Entry<String, String> entry : mapFieldValue.entrySet()){
			almEntity.setFieldValue(entry.getKey(), entry.getValue());
		}
	}

    /**
     * Get ALM entity list
     */
	public <E extends AlmEntity>  List<E> getAlmEntityList(List<Map<String, String>> entities, Class<E> c) {
		
		List<E> entityList = new ArrayList<E> ();
		for(Map<String, String> fieldValueMap:  entities) {
			try {
				E entity = c.newInstance();
				populateAlmEntityFieldValue(fieldValueMap, entity);
				entityList.add(entity);
			} catch (Exception e) {
				
			}
		}
		
		return entityList;
		
	}

    /**
     * Get encode string
     */
	public static String getEncodedString(String s) {
		String quotedStr = "\"" + s +"\"";
		try {
			quotedStr = URLEncoder.encode(quotedStr, "UTF-8");
		} catch (Exception e) {
			
		}
		return quotedStr;
	}

    /**
     * Get entity under parent folder
     */
	public <E extends AlmEntity > E getEntityUnderParentFolder( Class<E> entityClass, int parentId, String entityName ){

		String getEntityUnderParentFolderQuery = String.format("fields=id,name&query={parent-id[%s];name[%s]}", String.valueOf(parentId), getEncodedString(entityName));
		try {
			AlmEntity entity = entityClass.newInstance();
			GetAlmEntityRequest getRequest = new GetAlmEntityRequest(entity, getRestClient(), getEntityUnderParentFolderQuery);
			Response response = getRequest.perform();
			if(response.isOk() ) {
				List<Map<String, String>> entities2 = XPathUtils.toEntities(response.toString());
				List<E> entities = getAlmEntityList(entities2, entityClass);
	
				if(entities.size()>0){
					return entities.get(0);
				} else {
					return null;
				}
			} else {
				_logger.log("Failed to get Entity:" +getEntityUnderParentFolderQuery );
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

    /**
     * Get ALM entity
     */
	public <E extends AlmEntity > List<E> getAlmEntity( E entity, String queryString){

		List<E> ret = new ArrayList<E>();
		
		try {
			GetAlmEntityRequest getRequest = new GetAlmEntityRequest(entity, getRestClient(), queryString);
			Response response = getRequest.perform();
			if(response.isOk() && !response.toString().equals("")) {
				List<Map<String, String>> entities2 = XPathUtils.toEntities(response.toString());
				List entities = getAlmEntityList(entities2, entity.getClass());
				return entities;
			} else {
				return ret;
			}

		} catch (Exception e) {
			e.printStackTrace();
			_logger.log("Failed to get Entity:" + entity.toString() +" with query string:" +queryString);
			return ret;
		}

	}

    /**
     * Create ALM entity
     */
	public <E extends AlmEntity> E createAlmEntity (E entity, String[] fieldsForCreation) throws ExternalEntityUploadException {
		
		CreateAlmEntityRequest createRequest = new CreateAlmEntityRequest(getRestClient(), entity, getPairListForAlmEntityFields(entity, fieldsForCreation) );
		Response response = createRequest.perform();	
		if(response.isOk() && !response.toString().equals("")){
			List<Map<String, String>> entities2 = XPathUtils.toEntities(response.toString());
			List entities = getAlmEntityList(entities2, entity.getClass());
	
			if(entities.size()>0){
				
				return (E)entities.get(0);
			} else {
				_logger.log("Failed to create Entity:" + entity.toString());
				throw new ExternalEntityUploadException("Failed to create Entity:" + entity.toString());
			}
		} else {
			_logger.log("Failed to create Entity:" + entity.toString());
			throw new ExternalEntityUploadException("Failed to create Entity:" + entity.toString());
		}
		
	}

    /**
     * Update ALM entity
     */
	public <E extends AlmEntity> void updateAlmEntity (E entity, String[] fieldsForUpdate) {
		
		UpdateAlmEntityRequest updateRequest = new UpdateAlmEntityRequest(getRestClient(), entity, getMapListForAlmEntityFields(entity, fieldsForUpdate)) ;
		Response response = updateRequest.execute();
		
		if(!response.isOk()) {
			_logger.log("Failed to update entity:" + entity.toString());
		}

	}	
	
}
