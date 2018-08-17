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

package com.microfocus.application.automation.tools.results.parser.antjunit;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.microfocus.application.automation.tools.results.parser.ReportParseException;
import com.microfocus.application.automation.tools.results.parser.ReportParser;
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

public class AntJUnitReportParserImpl implements ReportParser {

	public List<AlmTestSet> parseTestSets(InputStream reportInputStream,
                                          String testingFramework, String testingTool) throws ReportParseException {
		
		try {
			return parseTestSetsFromAntJUnitReport(reportInputStream, testingFramework, testingTool);
		} catch (Exception e) {
		
			e.printStackTrace();
			throw new ReportParseException();
		}
	}	
	
	private Testsuites parseFromAntJUnitReport(InputStream reportInputStream) throws JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(Testsuites.class);
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		return (Testsuites)unmarshaller.unmarshal(reportInputStream);
	}
	
	private AlmTest createExternalTestForAntJUnit(Testcase tc, String testingFramework, String testingTool) {
	
		return ParserUtil.createExternalTest(tc.getClassname(), tc.getName(), testingFramework, testingTool);
	}
	
	private String getRunDetail(Testcase tc){
		String detail = ParserUtil.marshallerObject(Testcase.class, tc);
		return Base64Encoder.encode(detail.getBytes());
	}


	private ArrayList<AlmTestSet> parseTestSetsFromAntJUnitReport(InputStream reportInputStream, String testingFramework, String testingTool) throws JAXBException {
		Testsuites testsuites = parseFromAntJUnitReport(reportInputStream);
		
		ArrayList<AlmTestSet> testSets = new ArrayList<AlmTestSet>();
		
		for(Testsuite ts : testsuites.getTestsuite()) {
			AlmTestSet testSet = new AlmTestSetImpl();
			testSet.setFieldValue(AlmTestSet.TESTSET_NAME, ParserUtil.replaceInvalidCharsForTestSetName(ts.getName()));
			testSet.setFieldValue(AlmTestSet.TESTSET_SUB_TYPE_ID, EXTERNAL_TEST_SET_TYPE_ID);
			testSets.add(testSet);

			for (Testcase tc: ts.getTestcase()) {
				AlmTestInstance testInstance = new AlmTestInstanceImpl();
				testInstance.setFieldValue(AlmTestInstance.TEST_INSTANCE_SUBTYPE_ID, EXTERNAL_TEST_INSTANCE_TYPE_ID);
				testSet.addRelatedEntity(EntityRelation.TESTSET_TO_TESTINSTANCE_CONTAINMENT_RELATION, testInstance);
				
				AlmTest test = createExternalTestForAntJUnit(tc, testingFramework, testingTool);
				testInstance.addRelatedEntity(EntityRelation.TEST_TO_TESTINSTANCE_REALIZATION_RELATION, test);
				
				AlmRun run = ParserUtil.createRun(getRunStatus(tc),
												ts.getTimestamp(),  
												tc.getTime(), 
												getRunDetail (tc));
				testInstance.addRelatedEntity(EntityRelation.TESTINSTANCE_TO_RUN_REALIZATION_RELATION, run);
			}			
		}
		
		return testSets;
	}
	
	private String getRunStatus(Testcase testcase){
		
		
		if(testcase.getError().size()>0) {
			return IAlmConsts.IStatuses.FAILED;
		}
		if(testcase.getFailure().size()>0) {
			return IAlmConsts.IStatuses.FAILED;
		}
		if(testcase.getStatus() == null) {
			return IAlmConsts.IStatuses.PASSED;
		}
		
		String status = testcase.getStatus();
		if(status != null ){
			status = status.trim();
			if (status.length()>0){
				return status;
			} else {
				return IAlmConsts.IStatuses.PASSED;
			}
		} else {
			return IAlmConsts.IStatuses.PASSED;
		}
		
	}
}
