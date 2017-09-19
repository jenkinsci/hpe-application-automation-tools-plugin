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

package com.hpe.application.automation.tools.results.service;


public class TestDefaultExternalEntityUploadServiceImpl {

	
	private static void testJunit(int i) throws Exception{
		AlmRestInfo loginInfo = new AlmRestInfo(
				"http://localhost:8085/qcbin",
				"DEFAULT",
				"testexternal1",
				"sa",
				"",
				""
				);
		AlmRestTool u = new AlmRestTool(loginInfo, new SystemOutLogger());
		
		IExternalEntityUploadService service = new DefaultExternalEntityUploadServiceImpl(u,new SystemOutLogger());
		String reportFilePath = TestDefaultExternalEntityUploadServiceImpl.class.getResource("junitResult.xml").getPath();
		String testingFramework = "JUnit";
		String testingTool = "Jenkins";
		String subversion = "1";
		String testFolderPath = "Import\\New Test Folder\\junit" + i;
		String testsetFolderPath = "Import\\New Test Set Folder\\junit" +i;
		long start = System.currentTimeMillis();
		service.UploadExternalTestSet(loginInfo,reportFilePath, testsetFolderPath, testFolderPath, testingFramework, testingTool, subversion, "local","http://localhost:8085/");
		long end = System.currentTimeMillis();
		System.out.println("total time:" + (end -start));		
		
	}
	
	private static void testtestNG(int i) throws Exception{
		AlmRestInfo loginInfo = new AlmRestInfo(
				"http://localhost:8085/qcbin",
				"DEFAULT",
				"testexternal1",
				"sa",
				"",
				""
				);
		AlmRestTool u = new AlmRestTool(loginInfo, new SystemOutLogger());
		
		IExternalEntityUploadService service = new DefaultExternalEntityUploadServiceImpl(u, new SystemOutLogger());
		
		String reportFilePath = TestDefaultExternalEntityUploadServiceImpl.class.getResource("testng-results.xml").getPath();
		String testingFramework = "TestNG";
		String testingTool = "Jenkins testng";
		String subversion = "1";
		String testFolderPath = "Import\\New Test Folder\\testng"+i;
		String testsetFolderPath = "Import\\New Test Set Folder\\testng"+i;
		long start = System.currentTimeMillis();
		service.UploadExternalTestSet(loginInfo,reportFilePath, testsetFolderPath, testFolderPath, testingFramework, testingTool, subversion, "local","http://localhost:8085/");
		long end = System.currentTimeMillis();
		System.out.println("total time:" + (end -start));		
	}
	
	private static void testnunit(int i) throws Exception{
		AlmRestInfo loginInfo = new AlmRestInfo(
				"http://localhost:8085/qcbin",
				"DEFAULT",
				"testexternal1",
				"sa",
				"",
				""
				);
		AlmRestTool u = new AlmRestTool(loginInfo, new SystemOutLogger());
		
		IExternalEntityUploadService service = new DefaultExternalEntityUploadServiceImpl(u, new SystemOutLogger());
		
		String reportFilePath = TestDefaultExternalEntityUploadServiceImpl.class.getResource("NUnitReport.xml").getPath();

		String testingFramework = "NUNit";
		String testingTool = "Jenkins nunit";
		String subversion = "1";
		String testFolderPath = "Import\\New Test Folder\\nunit"+i;
		String testsetFolderPath = "Import\\New Test Set Folder\\nunit"+i;
		long start = System.currentTimeMillis();
		service.UploadExternalTestSet(loginInfo,reportFilePath, testsetFolderPath, testFolderPath, testingFramework, testingTool, subversion, "local","http://localhost:8085/");
		long end = System.currentTimeMillis();
		System.out.println("total time:" + (end -start));	
	}
	
	public static void main(String[] argc) throws Exception{
		testJunit(109);
		//testtestNG(107);
		//testnunit(108);
	}
}
