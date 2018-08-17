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

package com.microfocus.application.automation.tools.results.parser.util;

import java.io.StringWriter;
import java.util.Date;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import com.microfocus.application.automation.tools.results.parser.ReportParser;
import com.microfocus.application.automation.tools.results.service.almentities.AlmRun;
import com.microfocus.application.automation.tools.results.service.almentities.AlmRunImpl;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTest;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTestImpl;

public class ParserUtil {
	public static char nameConnector = '_';
	public static char[] testNameInvalidChars = new char[] { '\\', '/', ':', '"', '?', '\'', '<', '>', '|', '*', '%' };
	public static char[] testSetNameInvalidChars = new char[] { '\\', '^', ',', '"', '*' };

	public static String repaceInvalidChars(char[] invalidChars, char newChar, String source)
    {
		StringBuffer temp = new StringBuffer(source);
      
		for (int i=0; i<temp.length(); i++ )
		{
			for(int j=0; j< invalidChars.length; j ++) {
				if(temp.charAt(i) == invalidChars[j]) {
					temp.setCharAt(i, newChar);
				}
			}
		}

		return temp.toString();
    }
	
	public static String replaceInvalidCharsForTestSetName(String testsetName) {
		return ParserUtil.repaceInvalidChars(testSetNameInvalidChars, nameConnector, testsetName );
	}
	
	public static String replaceInvalidCharsForTestName(String testName) {
		return ParserUtil.repaceInvalidChars(testNameInvalidChars, nameConnector, testName );
	}
	
	public static AlmTest createExternalTest (String inputClassName, String inputMethodName, String testingFramework, String testingTool) {
		AlmTest test = new AlmTestImpl();

		String packageName = "";
		
		String className = replaceInvalidCharsForTestName(inputClassName);
		String methodName = replaceInvalidCharsForTestName(inputMethodName);

		String temp = className;
		int indexDot = temp.lastIndexOf(".");
		if(indexDot >= 0) {
			packageName = temp.substring(0, indexDot);
			className = temp.substring(indexDot+1);
		}
		
		String testName = className + "_" + methodName;
		test.setFieldValue( AlmTest.TS_UT_PACKAGE_NAME, packageName);
		test.setFieldValue( AlmTest.TEST_NAME, testName );
		test.setFieldValue( AlmTest.TEST_TYPE, ReportParser.EXTERNAL_TEST_TYPE);
		test.setFieldValue( AlmTest.TS_TESTING_FRAMEWORK, testingFramework);
		test.setFieldValue( AlmTest.TS_TESTING_TOOL, testingTool);
		test.setFieldValue( AlmTest.TS_UT_CLASS_NAME, className);
		test.setFieldValue( AlmTest.TS_UT_METHOD_NAME, methodName);		
		return test;
	}
	
	public static String marshallerObject(Class c, Object o){
		String s = "<?xml version=\"1.0\" ?>";
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(c);
			Marshaller marshaller =  jaxbContext.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_ENCODING,"utf-8");
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
			
			StringWriter baos = new StringWriter();  
			marshaller.marshal(o, baos);
			s += baos.toString();
		}catch (Exception e) {
		}
		return s;
	}
	
	public static AlmRun createRun(String runStatus, String execDateTime, String duration, String detail) {
		AlmRun run = new AlmRunImpl();
		run.setFieldValue( AlmRun.RUN_SUBTYPE_ID, ReportParser.EXTERNAL_RUN_TYPE_ID);
		run.setFieldValue( AlmRun.RUN_STATUS, runStatus);
		run.setFieldValue( AlmRun.RUN_DETAIL, detail);
		Date executeDate;
		if(execDateTime != null && execDateTime.length() >0 ) {
			execDateTime = execDateTime.replaceAll("T", " ");
			executeDate = TimeUtil.stringToDate(execDateTime);
		} else {
			executeDate = new Date(System.currentTimeMillis());
		}
		
		run.setFieldValue( AlmRun.RUN_EXECUTION_DATE, 
				TimeUtil.dateToString(executeDate));
		run.setFieldValue( AlmRun.RUN_EXECUTION_TIME,
				TimeUtil.timeToString(executeDate));
        
		if(duration != null && duration.length() >0 ) {
			Float durationTime = 0.0f;
			try {
				durationTime = Float.valueOf(duration);
			} catch (NumberFormatException e) {
				//the exception may be ignored.
			}
			run.setFieldValue( AlmRun.RUN_DURATION, String.valueOf(durationTime.intValue()));
		} else {
			run.setFieldValue( AlmRun.RUN_DURATION, String.valueOf(0) );
		}
		
		return run;
	}	
	
}
