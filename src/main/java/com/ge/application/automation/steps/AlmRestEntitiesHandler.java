package com.ge.application.automation.steps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import qc.rest.examples.infrastructure.Entities;
import qc.rest.examples.infrastructure.Entity;
import qc.rest.examples.infrastructure.Entity.Fields.Field;
import qc.rest.examples.infrastructure.EntityMarshallingUtils;

/**
 * Abstract class to handle making rest calls and iterating
 * through entities when return type is Entities
 * 
 * @author 212412070
 *
 * @param <T> Type to be returned by parseXml.
 * 		This is the data type the implementation
 * 		is attempting to get from ALM.
 */
public abstract class AlmRestEntitiesHandler<T> extends AlmRestHandler<List<T>> {

	/**
	 * @return Returns an array of fields to get from the rest api
	 */
	public abstract String[] getRequiredFieldNames();
	
	/**
	 * Generate an object based on values from rest call.
	 * If null is returned, it will be discarded.
	 * @param fieldValues Map of fieldName -> fieldValue
	 * 		The keys correspond to values from getRequiredFieldNames
	 * @return Returns an object based on the fieldValues
	 */
	public abstract T processEntity(Map<String, String> fieldValues);
	
	/**
	 * Appends field constraint to the provided request string
	 * @param request Base request to api
	 * @return Returns the base request + "&fields=<fields from getRequiredFieldNames>"
	 */
	public String buildRequestWithFields(String request) {
		StringBuffer requestWithFields = new StringBuffer(request);
		String[] fieldNames = getRequiredFieldNames();
		if (fieldNames.length > 0) {
			requestWithFields.append("&fields=" + fieldNames[0]);
			for (int i = 1; i < fieldNames.length; i++) {
				requestWithFields.append("," + fieldNames[i]);
			}
		}
		return requestWithFields.toString();
	}
	
	@Override
	public List<T> parseXml(String xml) throws JAXBException {
		
		// list to hold non-null results from processEntity
		List<T> results = new ArrayList<T>();
		
		// set of fields to get from each entity returned from rest call
		Set<String> requiredFields = new HashSet<String>(Arrays.asList(getRequiredFieldNames()));
		
		// iterate through all entities returned by api call
		Entities entitySet = EntityMarshallingUtils.marshal(Entities.class, xml);
		List<Entity> entities = entitySet.getEntities();
        for (Entity entity: entities) {
        	Map<String, String> fieldValues = new HashMap<String, String>();
        	List<Field> fields = entity.getFields().getField();
        	
        	// put the desired fields and values into fieldValues
        	for (Field field : fields) {
        		String fieldName = field.getName();
        		if (requiredFields.contains(fieldName)) {
        			fieldValues.put(fieldName, field.getValue().get(0));
        		}
	        }
        	
        	// add output from intermediateResults to list 
        	T intermediateResult = processEntity(fieldValues);
        	if (intermediateResult != null) {
        		results.add(intermediateResult);
        	}
        }
		
		return results;
	}

}
