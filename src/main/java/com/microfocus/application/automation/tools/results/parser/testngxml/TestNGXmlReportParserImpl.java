/*
 * Certain versions of software accessible here may contain branding from Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 * This software was acquired by Micro Focus on September 1, 2017, and is now offered by OpenText.
 * Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright 2012-2024 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ___________________________________________________________________
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
		JAXBContext jaxbContext;
		Thread t = Thread.currentThread();
		ClassLoader orig = t.getContextClassLoader();
		t.setContextClassLoader(TestNGXmlReportParserImpl.class.getClassLoader());
		try {
			jaxbContext = JAXBContext.newInstance(TestngResults.class);
		} finally {
			t.setContextClassLoader(orig);
		}
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
				return IAlmConsts.IStatuses.PASSED.value();
			} else if ("FAIL".equalsIgnoreCase(status)) {
				return IAlmConsts.IStatuses.FAILED.value();
			} else {
				return IAlmConsts.IStatuses.NO_RUN.value();
			}
		} else {
			return IAlmConsts.IStatuses.NO_RUN.value();
		}
	}
	
	private String getRunDetail(TestMethod tm){
		String detail = ParserUtil.marshallerObject(TestMethod.class, tm);
		return Base64Encoder.encode(detail.getBytes());
	}
}
