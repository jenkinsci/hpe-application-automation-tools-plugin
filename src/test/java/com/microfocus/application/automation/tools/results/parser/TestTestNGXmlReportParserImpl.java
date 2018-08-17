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

package com.microfocus.application.automation.tools.results.parser;

import java.io.InputStream;
import java.util.List;

import com.microfocus.application.automation.tools.results.service.almentities.AlmTestSet;
import org.junit.Assert;

import com.microfocus.application.automation.tools.results.parser.testngxml.TestNGXmlReportParserImpl;

@SuppressWarnings("squid:S2698")
public class TestTestNGXmlReportParserImpl {

	//@Test
	public void testParseTestSets() throws Exception {

		InputStream in = TestTestNGXmlReportParserImpl.class.getResourceAsStream("testng-results.xml");
		TestNGXmlReportParserImpl parser = new TestNGXmlReportParserImpl();
		List<AlmTestSet> testsets = parser.parseTestSets(in, "TestNG", "jenkins");

		Assert.assertEquals(1, testsets.size());
		Assert.assertEquals("Suite1", testsets.get(0).getName());
	}

}
