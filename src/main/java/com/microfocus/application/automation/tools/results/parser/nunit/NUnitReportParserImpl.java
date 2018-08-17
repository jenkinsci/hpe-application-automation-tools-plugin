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

package com.microfocus.application.automation.tools.results.parser.nunit;

import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.microfocus.application.automation.tools.results.parser.ReportParseException;
import com.microfocus.application.automation.tools.results.parser.ReportParser;
import com.microfocus.application.automation.tools.results.parser.util.ParserUtil;
import com.microfocus.application.automation.tools.results.parser.util.TimeUtil;
import com.microfocus.application.automation.tools.results.service.almentities.AlmRun;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTest;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTestInstance;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTestInstanceImpl;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTestSet;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTestSetImpl;
import com.microfocus.application.automation.tools.results.service.almentities.EntityRelation;
import com.microfocus.application.automation.tools.results.service.almentities.IAlmConsts;
import com.microfocus.application.automation.tools.sse.sdk.Base64Encoder;

public class NUnitReportParserImpl implements ReportParser {

	public List<AlmTestSet> parseTestSets(InputStream reportInputStream,
                                          String testingFramework, String testingTool) throws ReportParseException {
		
		try {
			return parseTestSetFromNUnitReport(reportInputStream, testingFramework, testingTool);
		} catch (Throwable e) {
			throw new ReportParseException();
		}
	}	
    
	private ResultType parseFromNUnitReport(InputStream reportInputStream) throws JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(ResultType.class);
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		return (ResultType)unmarshaller.unmarshal(reportInputStream);
	}

	private AlmTest createExternalTestForNUnitReport(TestCaseType testcase, String testingFramework, String testingTool) {

		String temp = testcase.getName();
		String methodName = "";
		String className = "";
		int indexMethod = temp.lastIndexOf(".");
		if(indexMethod >= 0) {
			methodName = temp.substring(indexMethod+1);
			className = temp.substring(0, indexMethod);
		}
		return ParserUtil.createExternalTest(className, methodName, testingFramework, testingTool);
	}

	private void createTestSetAndTest(TestSuiteType testSuite, 
										String uplevelSuiteName, 
										String execDate, 
										String execTime, 
										List<AlmTestSet> testsets,
										String testingFramework,
										String testingTool) {
		ResultsType resultsOfSuite = testSuite.getResults();
		List<TestCaseType> testcases = resultsOfSuite.getTestCase();
		List<TestSuiteType> testSuites = resultsOfSuite.getTestSuite();
		

		String currentSuiteName = testSuite.getName();
		int index = currentSuiteName.lastIndexOf("\\");
		if(index >=0 ) {
			currentSuiteName = currentSuiteName.substring(index+1);
		}
		
		String testsetName = currentSuiteName;
		
		if(uplevelSuiteName!=null && uplevelSuiteName.length() >0) {
			testsetName = uplevelSuiteName+"_"+currentSuiteName;
		}
		
		if(testcases != null && testcases.size() >0) {
			AlmTestSet testSet = new AlmTestSetImpl();
			testSet.setFieldValue( AlmTestSet.TESTSET_NAME,testsetName );
			testSet.setFieldValue( AlmTestSet.TESTSET_SUB_TYPE_ID, EXTERNAL_TEST_SET_TYPE_ID);
			testsets.add(testSet);
			
			for(TestCaseType testcase: testcases) {
				AlmTestInstance testInstance = new AlmTestInstanceImpl();
				testInstance.setFieldValue( AlmTestInstance.TEST_INSTANCE_SUBTYPE_ID, EXTERNAL_TEST_INSTANCE_TYPE_ID);
				testSet.addRelatedEntity(EntityRelation.TESTSET_TO_TESTINSTANCE_CONTAINMENT_RELATION, testInstance);
				
				AlmTest test = createExternalTestForNUnitReport( testcase, testingFramework, testingTool);
				testInstance.addRelatedEntity(EntityRelation.TEST_TO_TESTINSTANCE_REALIZATION_RELATION, test);
				
				String execDateTime = "";
				if(execDate != null && execTime != null){
					execDateTime = execDate +" " +execTime;
				}
				AlmRun run = ParserUtil.createRun(getRunStatus(testcase),
													execDateTime, 
													String.valueOf(testcase.getTime()), 
													getRunDetail(testcase));
				testInstance.addRelatedEntity(EntityRelation.TESTINSTANCE_TO_RUN_REALIZATION_RELATION, run);
			}
		}
		
		for(TestSuiteType s: testSuites){
			createTestSetAndTest(s, testsetName, execDate, execTime, testsets, testingFramework, testingTool);
		}
	}
	
	private Date getDate( int dateFormat, String dateStr) {

		DateFormat df = DateFormat.getDateInstance(dateFormat);
		Date date = null;
		try {
			date = df.parse(dateStr);
			return date;
		} catch( Exception e) {
			
		}
		return null;
	}
	
	private Date getTime( int timeFormat, String dateStr) {

		DateFormat df = DateFormat.getTimeInstance(timeFormat);
		Date date = null;
		try {
			date = df.parse(dateStr);
			return date;
		} catch( Exception e) {
			
		}
		return null;
	}
	private static String supportedDateFormat [] = {
		"yyyy-MM-dd",
		"yyyy/MM/dd"
	};
	private static String supportedTimeFormat [] = {
		"HH:mm:ss",
		"hh:mm:ss"
	};
	private Date getDateBySupportedDateFormat(String dateStr) {
		
		for(String format : supportedDateFormat) {
			try {
				Date date = TimeUtil.getDateFormatter(format).parse(dateStr);
				return date;
			}catch (Exception e) {
				
			}
		}
		return null;
	}
	
	private Date getTimeBySupportedTimeFormat(String timeStr) {
		
		for(String format : supportedTimeFormat) {
			try {
				Date date = TimeUtil.getDateFormatter(format).parse(timeStr);
				return date;
			}catch (Exception e) {
				
			}
		}
		return null;
	}
	
	private String convertDateString(String dateStr) {
		Date date = null;
		date = getDate(DateFormat.LONG, dateStr);
		if(date == null) {
			date = getDate(DateFormat.MEDIUM, dateStr);
		}
		
		if(date == null) {
			date = getDate(DateFormat.SHORT, dateStr);
		}
		
		if(date == null) {
			date = getDate(DateFormat.FULL, dateStr);
		}
		
		if(date == null) {
			date = getDateBySupportedDateFormat(dateStr);
		}
		
		if(date != null) {
			return TimeUtil.getDateFormatter().format(date);
		} else {
			return null;
		}
	}
	
	private String convertTimeString(String timeStr) {
		Date time = null;
		time = getTime(DateFormat.LONG, timeStr);
		
		if(time == null) {
			time = getTime(DateFormat.MEDIUM, timeStr);
		}
		
		if(time == null) {
			time = getTime(DateFormat.SHORT, timeStr);
		}
		
		if(time == null) {
			time = getTime(DateFormat.FULL, timeStr);
		}
		
		if(time == null) {
			time = getTimeBySupportedTimeFormat(timeStr);
		}
		
		if(time != null) {
			return TimeUtil.getTimeFormatter().format(time);
		} else {
			return null;
		}
	}
	

	private ArrayList<AlmTestSet> parseTestSetFromNUnitReport(InputStream reportInputStream, String testingFramework, String testingTool) throws JAXBException {
		
		ResultType results = parseFromNUnitReport(reportInputStream);
		
		TestSuiteType testSuite = results.getTestSuite();
		String dateStr = results.getDate();
		String timeStr = results.getTime();
		String convertedDate = convertDateString(dateStr);
		String convertedTime = convertTimeString(timeStr);
		
		if(convertedDate == null || convertedTime == null) {
			Date executeDate = new Date(System.currentTimeMillis());
			convertedDate = TimeUtil.dateToString(executeDate);
			convertedTime = TimeUtil.timeToString(executeDate);
		}
		
		ArrayList<AlmTestSet> testSets = new ArrayList<AlmTestSet>();
		createTestSetAndTest(testSuite, "", convertedDate, convertedTime, testSets, testingFramework, testingTool);
		return testSets;
	}

	private String getRunStatus(TestCaseType testcase) {		
		String executed = testcase.getExecuted();
		if("True".equalsIgnoreCase(executed)) {
			String success = testcase.getSuccess();

			if("True".equalsIgnoreCase(success)) {
				return IAlmConsts.IStatuses.PASSED;
			} else {
				return IAlmConsts.IStatuses.FAILED;
			} 
		} else {
			return IAlmConsts.IStatuses.NO_RUN;
		}
	}
	
	private String getRunDetail(TestCaseType testcase){
		String detail = ParserUtil.marshallerObject(TestCaseType.class, testcase);		
		return Base64Encoder.encode(detail.getBytes());
	}
}
