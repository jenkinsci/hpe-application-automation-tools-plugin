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

import com.hpe.application.automation.tools.results.parser.nunit.NUnitReportParserImpl;
import com.hpe.application.automation.tools.results.service.almentities.AlmTestSet;
import org.junit.Assert;

import org.junit.Test;

@SuppressWarnings({"squid:S2699","squid:S3658"})
public class TestNUnitReportParserImpl {

	//@Test
	public void testParseTestSets() throws Exception{
		InputStream in = TestNUnitReportParserImpl.class.getResourceAsStream("NUnitReport.xml");
		NUnitReportParserImpl parser = new NUnitReportParserImpl();
		List<AlmTestSet> testsets = parser.parseTestSets(in, "NUnit", "Selenium");
		Assert.assertEquals(1, testsets.size());
		Assert.assertEquals("NUnit_Test1.dll_ExampleTestOfNUnit", testsets.get(0).getName());
	}

}
