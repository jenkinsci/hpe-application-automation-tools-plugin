/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.hpe.application.automation.tools.results.parser;

import java.io.InputStream;
import java.util.List;

import org.junit.Assert;

import org.junit.Test;

import com.hpe.application.automation.tools.results.parser.antjunit.AntJUnitReportParserImpl;
import com.hpe.application.automation.tools.results.service.almentities.AlmTestSet;
@SuppressWarnings("squid:S2698")
public class TestAntJUnitReportParserImpl {

	//@Test
	public void testParseTestSets() throws Exception {
		InputStream in = TestAntJUnitReportParserImpl.class.getResourceAsStream("AntTESTS-TestSuites.xml");
		AntJUnitReportParserImpl parser = new AntJUnitReportParserImpl();
		List<AlmTestSet> testsets = parser.parseTestSets(in, "JUnit", "Ant");
		Assert.assertEquals(testsets.size(), 1);
		Assert.assertEquals("TestProg1Prj1Test1", testsets.get(0).getName()  );
	}   

}
