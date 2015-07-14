package com.ge.applications.automation.testStepRetriever;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;

import qc.rest.examples.infrastructure.Response;
import qc.rest.examples.infrastructure.RestConnector;

/**
 * Abstract class for making rest calls
 *
 * @param <T> Return type for getResult
 */
public abstract class ALMRestHandler<T>{

	/**
	 * Get the string to be appended to the rest url generated
	 * by con.buildEntityCollectionUrl(). 
	 * e.g. "tests/5"
	 * @return Returns the request.
	 */
	public abstract String getRequest();
	
	/**
	 * Extract data from xml response
	 * @param xml Xml response from alm
	 * @return Returns needed data from xml
	 * @throws JAXBException
	 */
	public abstract T parseXml(String xml) throws JAXBException;
	
	/**
	 * Make a call to the rest api and extract the needed data
	 * @param con RestConnector to use
	 * @param logger Logger
	 * @return Returns output from parseXml
	 */
	public T getResult(RestConnector con, PrintStream logger) {
		T output = null;
		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.put("Accept", "application/xml");
		
		String requestUrl = con.buildEntityCollectionUrl(getRequest());
		
		// get xml from alm rest api
		String responseXml = null;
		try {
			Response response = con.httpGet(requestUrl, "", requestHeaders);
			responseXml = response.toString();
		} catch (Exception e) {
			e.printStackTrace(logger);
		}
		
		// attempt to parse the response
		if (responseXml != null) {
			try {
				output = parseXml(responseXml);
			} catch (JAXBException e) {
				e.printStackTrace(logger);
			}
		}
		
		return output;
	}
	
}
