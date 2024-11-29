/*
 *  Certain versions of software accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 *  This software was acquired by Micro Focus on September 1, 2017, and is now
 *  offered by OpenText.
 *  Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical
 *  in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the
 *  property of their respective owners.
 *  OpenText is a trademark of Open Text.
 *  __________________________________________________________________
 *  MIT License
 *
 *  Copyright 2012-2025 Open Text.
 *
 *  The only warranties for products and services of Open Text and
 *  its affiliates and licensors ("Open Text") are as may be set forth
 *  in the express warranty statements accompanying such products and services.
 *  Nothing herein should be construed as constituting an additional warranty.
 *  Open Text shall not be liable for technical or editorial errors or
 *  omissions contained herein. The information contained herein is subject
 *  to change without notice.
 *
 *  Except as specifically indicated otherwise, this document contains
 *  confidential information and a valid license is required for possession,
 *  use or copying. If this work is provided to the U.S. Government,
 *  consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 *  Computer Software Documentation, and Technical Data for Commercial Items are
 *  licensed to the U.S. Government under vendor's standard commercial license.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ___________________________________________________________________
 */
package com.microfocus.application.automation.tools.results.parser.mavensurefire;

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

public class MavenSureFireReportParserImpl implements ReportParser {

	public List<AlmTestSet> parseTestSets(InputStream reportInputStream,
                                          String testingFramework, String testingTool) throws ReportParseException {
		
		try {
			return parseTestSetsFromMavenSurefirePluginJUnitReport(reportInputStream, testingFramework, testingTool);
		} catch (Exception e) {
		
			throw new ReportParseException();
		}
	}	
    
	private Testsuite parseFromMavenSurefirePluginJUnitReport(InputStream reportInputStream) throws JAXBException {
		JAXBContext jaxbContext;
		Thread t = Thread.currentThread();
		ClassLoader orig = t.getContextClassLoader();
		t.setContextClassLoader(MavenSureFireReportParserImpl.class.getClassLoader());
		try {
			jaxbContext = JAXBContext.newInstance(Testsuite.class);
		} finally {
			t.setContextClassLoader(orig);
		}
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		return (Testsuite)unmarshaller.unmarshal(reportInputStream);
	}

	private AlmTest createExternalTestForMavenSurefirePluginJUnit(Testcase tc, String testingFramework, String testingTool) {
		
		return ParserUtil.createExternalTest(tc.getClassname(), tc.getName(), testingFramework, testingTool);
	}

	private ArrayList<AlmTestSet> parseTestSetsFromMavenSurefirePluginJUnitReport(InputStream reportInputStream, String testingFramework, String testingTool) throws JAXBException {
		Testsuite testsuite = parseFromMavenSurefirePluginJUnitReport(reportInputStream);		
		ArrayList<AlmTestSet> testSets = new ArrayList<AlmTestSet>();
		
		AlmTestSet testSet = new AlmTestSetImpl();
		testSet.setFieldValue( AlmTestSet.TESTSET_NAME, testsuite.getName());
		testSet.setFieldValue( AlmTestSet.TESTSET_SUB_TYPE_ID, EXTERNAL_TEST_SET_TYPE_ID);
		testSets.add(testSet);
		
		for (Testcase tc: testsuite.getTestcase()) {
			AlmTestInstance testInstance = new AlmTestInstanceImpl();
			testInstance.setFieldValue( AlmTestInstance.TEST_INSTANCE_SUBTYPE_ID, EXTERNAL_TEST_INSTANCE_TYPE_ID);
			testSet.addRelatedEntity(EntityRelation.TESTSET_TO_TESTINSTANCE_CONTAINMENT_RELATION, testInstance);
			
			AlmTest test = createExternalTestForMavenSurefirePluginJUnit(tc, testingFramework, testingTool);
			testInstance.addRelatedEntity(EntityRelation.TEST_TO_TESTINSTANCE_REALIZATION_RELATION, test);
			
			AlmRun run = ParserUtil.createRun(getRunStatus(tc),
												testsuite.getTimestamp(), 
												tc.getTime(), 
												getRunDetail(tc));
			testInstance.addRelatedEntity(EntityRelation.TESTINSTANCE_TO_RUN_REALIZATION_RELATION, run);
		}
		
		return testSets;
	}

	private String getRunStatus(Testcase testcase) {
		if (testcase.getError().size() > 0) {
			return IAlmConsts.IStatuses.FAILED.value();
		}
		if (testcase.getFailure().size() > 0) {
			return IAlmConsts.IStatuses.FAILED.value();
		}
		if (testcase.getStatus() == null) {
			return IAlmConsts.IStatuses.PASSED.value();
		}

		String result;
		String status = testcase.getStatus();
		if (status != null) {
			status = status.trim();
			if (status.length() > 0) {
				try {
					result = IAlmConsts.IStatuses.valueOf(status.toUpperCase()).value();
				} catch (IllegalArgumentException e) {
					result = status;
				}
			} else {
				result = IAlmConsts.IStatuses.PASSED.value();
			}
		} else {
			result = IAlmConsts.IStatuses.PASSED.value();
		}
		return result;
	}
	
	private String getRunDetail(Testcase testcase) {
		String detail = ParserUtil.marshallerObject(Testcase.class, testcase);		
		return Base64Encoder.encode(detail.getBytes());
	}
}
