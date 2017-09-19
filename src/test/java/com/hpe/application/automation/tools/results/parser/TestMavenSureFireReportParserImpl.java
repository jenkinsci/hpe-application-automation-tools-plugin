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

import com.hpe.application.automation.tools.results.parser.mavensurefire.MavenSureFireReportParserImpl;
import com.hpe.application.automation.tools.results.service.almentities.AlmTestSet;
import org.junit.Assert;

import org.junit.Test;


public class TestMavenSureFireReportParserImpl {

	//@Test
	public void testParseTestSets()throws Exception {
		InputStream in = TestMavenSureFireReportParserImpl.class.getResourceAsStream("MAVENTEST-com.demoapp.demo.AppTest.xml");
		MavenSureFireReportParserImpl parser = new MavenSureFireReportParserImpl();
		List<AlmTestSet> testsets = parser.parseTestSets(in, "JUnit", "Maven");
		assert (testsets.size () == 1);
		AlmTestSet testset = testsets.get(0);
		Assert.assertEquals("com.demoapp.demo.AppTest", testset.getName());
	}

}
