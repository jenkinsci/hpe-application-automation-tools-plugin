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

package com.microfocus.application.automation.tools.results.parser.testngxml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.microfocus.application.automation.tools.results.parser.ReportParseException;
import com.microfocus.application.automation.tools.results.parser.ReportParser;
import com.microfocus.application.automation.tools.results.parser.testngxml.TestngResults.Suite;
import com.microfocus.application.automation.tools.results.parser.testngxml.TestngResults.Suite.Test;
import com.microfocus.application.automation.tools.results.parser.testngxml.TestngResults.Suite.Test.Class.TestMethod;
import com.microfocus.application.automation.tools.results.parser.util.ParserUtil;
import com.microfocus.application.automation.tools.results.service.almentities.AlmRun;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTest;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTestInstance;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTestInstanceImpl;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTestSet;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTestSetImpl;
import com.microfocus.application.automation.tools.results.service.almentities.EntityRelation;
import com.microfocus.application.automation.tools.results.service.almentities.IAlmConsts;
import com.microfocus.application.automation.tools.sse.sdk.Base64Encoder;

public class TestNGXmlReportParserImpl implements ReportParser {
	
	public List<AlmTestSet> parseTestSets(InputStream reportInputStream,
                                          String testingFramework, String testingTool) throws ReportParseException {
		
		try {
			return parseTestSetFromTestNGXmlReport(reportInputStream, testingFramework, testingTool);
		} catch (Throwable e) {
		
			throw new ReportParseException();
		}
	}	
    
	private TestngResults parseFromTestNGXmlReport(InputStream reportInputStream) throws JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(TestngResults.class);
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		return (TestngResults)unmarshaller.unmarshal(reportInputStream);
	}

	private AlmTest createExternalTestForTestNGXmlReport(String className, String methodName, String testingFramework, String testingTool) {

		return ParserUtil.createExternalTest(className, methodName, testingFramework, testingTool);
	}

	private ArrayList<AlmTestSet> parseTestSetFromTestNGXmlReport(InputStream reportInputStream, String testingFramework, String testingTool) throws JAXBException {
		
		TestngResults testngresults = parseFromTestNGXmlReport(reportInputStream);
		
		ArrayList<AlmTestSet> testSets = new ArrayList<AlmTestSet>();
		for( Suite suite : testngresults.suite) {
			AlmTestSet testSet = new AlmTestSetImpl();
			testSet.setFieldValue( AlmTestSet.TESTSET_NAME, suite.getName());
			testSet.setFieldValue( AlmTestSet.TESTSET_SUB_TYPE_ID, EXTERNAL_TEST_SET_TYPE_ID);
			testSets.add(testSet);
			
			
			for (Test t: suite.getTest()) {
				
				for(TestngResults.Suite.Test.Class c: t.getClazz() ) {
					
					for(TestMethod tm : c.getTestMethod()) {
						AlmTestInstance testInstance = new AlmTestInstanceImpl();
						testInstance.setFieldValue( AlmTestInstance.TEST_INSTANCE_SUBTYPE_ID, EXTERNAL_TEST_INSTANCE_TYPE_ID);
						testSet.addRelatedEntity(EntityRelation.TESTSET_TO_TESTINSTANCE_CONTAINMENT_RELATION, testInstance);
						
						AlmTest test = createExternalTestForTestNGXmlReport( c.getName(), tm.getName(), testingFramework, testingTool);
						testInstance.addRelatedEntity(EntityRelation.TEST_TO_TESTINSTANCE_REALIZATION_RELATION, test);
						
						AlmRun run = ParserUtil.createRun(getRunStatus(tm), tm.getStartedAt(), String.valueOf(tm.getDurationMs()), getRunDetail(tm));
						testInstance.addRelatedEntity(EntityRelation.TESTINSTANCE_TO_RUN_REALIZATION_RELATION, run);
					}
				}
			}
		}
		return testSets;
	}

	private String getRunStatus(TestMethod tm) {		
		String status = tm.getStatus();
		if(status != null && status.length()>0){
			status = status.trim();

			if("PASS".equalsIgnoreCase(status)) {
				return IAlmConsts.IStatuses.PASSED;
			} else if ("FAIL".equalsIgnoreCase(status)) {
				return IAlmConsts.IStatuses.FAILED;
			} else {
				return IAlmConsts.IStatuses.NO_RUN;
			}
		} else {
			return IAlmConsts.IStatuses.NO_RUN;
		}
	}
	
	private String getRunDetail(TestMethod tm){
		String detail = ParserUtil.marshallerObject(TestMethod.class, tm);
		return Base64Encoder.encode(detail.getBytes());
	}
}
