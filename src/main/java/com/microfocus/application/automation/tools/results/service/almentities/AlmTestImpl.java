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

public class AlmTestImpl extends AlmEntityImpl implements AlmTest {
	private static String restPrefix = "tests"; 
	public String getRestPrefix() {
		return restPrefix;
	}
	
	public String getKey(){
		String className = (String)getFieldValue(TS_UT_CLASS_NAME);
		
		String methodName = (String)getFieldValue(TS_UT_METHOD_NAME);
		String packageName = (String)getFieldValue(TS_UT_PACKAGE_NAME);
		if(packageName == null) {
			packageName = "";
		}
		String testingFramework = (String) getFieldValue(TS_TESTING_FRAMEWORK);
		String key = packageName +"_" +className +"_"+ methodName+"_"+testingFramework;
		return key;
	}
	
	public boolean equals(Object o){
		
		if( !(o instanceof AlmTestImpl)){
			return false;
		}
		
		if(this == o) {
			return true;
		}
		
		AlmTestImpl oT = (AlmTestImpl) o;
		
		String className = (String)getFieldValue(TS_UT_CLASS_NAME);
		
		String methodName = (String)getFieldValue(TS_UT_METHOD_NAME);
		String packageName = (String)getFieldValue(TS_UT_PACKAGE_NAME);
		if(packageName == null) {
			packageName = "";
		}
		
		String testingFramework = (String) getFieldValue(TS_TESTING_FRAMEWORK);
		
		return className.equals((String)oT.getFieldValue(TS_UT_CLASS_NAME))
				&& 	packageName.equals((String)oT.getFieldValue(TS_UT_PACKAGE_NAME))
				&& 	methodName.equals((String)oT.getFieldValue(TS_UT_METHOD_NAME))
				&& 	testingFramework.equals((String)oT.getFieldValue(TS_TESTING_FRAMEWORK));
		
	}
	
	public int hashCode(){

		return getKey().hashCode();
		
	}
}
