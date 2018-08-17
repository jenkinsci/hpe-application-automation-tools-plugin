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

package com.microfocus.application.automation.tools.results.service.almentities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class  AlmEntityImpl implements AlmEntity {

	private final Map<String, String> fields = new HashMap<String, String>();
    private final Map<String, List<AlmEntity>> relatedEntities =
            new HashMap<String, List<AlmEntity>>();
	
            
    public String getName() {
    	return (String) getFieldValue(AlmCommonProperties.NAME);
    }
    
    public String getId() {
    	return  getFieldValue(AlmCommonProperties.ID);
    	
    }
    
    public void setId(String id) {
    	setFieldValue(AlmCommonProperties.ID, id);
    }
    
	@Override
	public void setFieldValue(String fieldName, String fieldValue) {
		fields.put(fieldName, fieldValue);

	}
	
	@Override
	public String getFieldValue(String fieldName) {
		return fields.get(fieldName);
	}

	@Override
	public void addRelatedEntity(String relationName, AlmEntity entity) {
        List<AlmEntity> entities = relatedEntities.get(relationName);
        if (entities == null){
            relatedEntities.put(relationName, new ArrayList<AlmEntity>());
            entities = relatedEntities.get(relationName);
        }

        entities.add(entity);
	}

	@Override
    public Map<String, List<AlmEntity>> getRelatedEntities() {
        return relatedEntities;
    }	
	
	public String toString(){
		String s = this.getRestPrefix() +":";
		
		for(Map.Entry<String, String> field : fields.entrySet()) {
			s += field.getKey();
			s += "=";
			s += field.getValue();
		}
		return s;
	}
}
