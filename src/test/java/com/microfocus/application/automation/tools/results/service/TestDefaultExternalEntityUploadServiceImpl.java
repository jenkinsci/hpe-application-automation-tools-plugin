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

package com.microfocus.application.automation.tools.results.service;

import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
//import org.junit.Before;
//import org.junit.ClassRule;
//import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;

public class TestDefaultExternalEntityUploadServiceImpl {

	FreeStyleProject project;

	public static final JenkinsRule jenkins = new JenkinsRule();

	public void initialize() throws IOException {
		project = jenkins.createFreeStyleProject("freestyle-project");
	}

	public void testJunit() throws Exception{
		AlmRestInfo loginInfo = new AlmRestInfo(
				"http://localhost:8085/qcbin",
				"DEFAULT",
				null,
				"testexternal1",
				"sa",
				"",
				""
				);
		int i = 107;
		AlmRestTool u = new AlmRestTool(loginInfo, new SystemOutLogger());

		AbstractBuild build = project.scheduleBuild2(0).get();

		IExternalEntityUploadService service = new DefaultExternalEntityUploadServiceImpl(u, build.getWorkspace(), new SystemOutLogger());
		String reportFilePath = this.getClass().getResource("junitResult.xml").getPath();
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

	public void testtestNG() throws Exception{
		AlmRestInfo loginInfo = new AlmRestInfo(
				"http://localhost:8085/qcbin",
				"DEFAULT",
				null,
				"testexternal1",
				"sa",
				"",
				""
				);

		int i = 108;
		AlmRestTool u = new AlmRestTool(loginInfo, new SystemOutLogger());

		AbstractBuild build = project.scheduleBuild2(0).get();
		IExternalEntityUploadService service = new DefaultExternalEntityUploadServiceImpl(u, build.getWorkspace(), new SystemOutLogger());

		String reportFilePath = this.getClass().getResource("testng-results.xml").getPath();
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

	public void testnunit() throws Exception{
		int i = 109;
		AlmRestInfo loginInfo = new AlmRestInfo(
				"http://localhost:8085/qcbin",
				"DEFAULT",
				null,
				"testexternal1",
				"sa",
				"",
				""
				);
		AlmRestTool u = new AlmRestTool(loginInfo, new SystemOutLogger());

		AbstractBuild build = project.scheduleBuild2(0).get();
		IExternalEntityUploadService service = new DefaultExternalEntityUploadServiceImpl(u, build.getWorkspace(), new SystemOutLogger());
		
		String reportFilePath = this.getClass().getResource("NUnitReport.xml").getPath();

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

}
