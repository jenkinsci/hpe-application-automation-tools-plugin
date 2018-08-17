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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.microfocus.application.automation.tools.results.parser.ReportParserManager;
import com.microfocus.application.automation.tools.results.service.almentities.AlmCommonProperties;
import com.microfocus.application.automation.tools.results.service.almentities.AlmEntity;
import com.microfocus.application.automation.tools.results.service.almentities.AlmRun;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTest;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTestConfig;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTestConfigImpl;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTestFolder;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTestFolderImpl;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTestImpl;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTestInstance;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTestInstanceImpl;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTestSet;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTestSetFolder;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTestSetFolderImpl;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTestSetImpl;
import com.microfocus.application.automation.tools.results.service.almentities.EntityRelation;
import com.microfocus.application.automation.tools.results.service.almentities.IAlmConsts;
import com.microfocus.application.automation.tools.sse.sdk.Logger;
import hudson.FilePath;

public class DefaultExternalEntityUploadServiceImpl implements
		IExternalEntityUploadService {

	Logger logger;
	private AlmRestTool restTool;
	private FilePath workspace;
	
	public DefaultExternalEntityUploadServiceImpl(AlmRestTool restTool, FilePath workspace, Logger logger) {
		this.restTool = restTool;
		this.logger = logger;
		this.workspace = workspace;
	}

	private String [] getTestCreationFields() {
		
		return new String [] {	AlmTest.TEST_NAME,
								AlmTest.TEST_TYPE,
								AlmTest.TS_TESTING_FRAMEWORK,
								AlmTest.TS_TESTING_TOOL,
								AlmTest.TS_UT_PACKAGE_NAME,
								AlmTest.TS_UT_CLASS_NAME,
								AlmTest.TS_UT_METHOD_NAME,
								AlmCommonProperties.PARENT_ID,
								AlmTest.TEST_RESPONSIBLE
							};
	}
	
	private AlmTest importTest(AlmTest test , int testFolderId, String testingTool, String testdesigner) throws ExternalEntityUploadException{

		String className = (String) test.getFieldValue(AlmTest.TS_UT_CLASS_NAME);
		String methodName = (String) test.getFieldValue(AlmTest.TS_UT_METHOD_NAME);
		String packageName = (String) test.getFieldValue(AlmTest.TS_UT_PACKAGE_NAME);
		String testingFramework = (String) test.getFieldValue(AlmTest.TS_TESTING_FRAMEWORK);
		
		String queryString = String.format("query={parent-id[%s];subtype-id[EXTERNAL-TEST];ut-class-name[%s];ut-method-name[%s]}&fields=id,name,ut-package-name,ut-class-name,ut-method-name,testing-framework&page-size=2000", 
											String.valueOf(testFolderId),
											AlmRestTool.getEncodedString(className),
											AlmRestTool.getEncodedString(methodName));		
		List<AlmTestImpl> existingTests = restTool.getAlmEntity(new AlmTestImpl(), queryString);
		
		AlmTestImpl importedTest = null;//restTool.getEntityUnderParentFolder(AlmTestImpl.class, testFolderId, test.getName());
		
		if(existingTests != null && existingTests.size() >0) {
			boolean exists = false;
			Map<String, AlmTestImpl> existingTestMap = new HashMap<String, AlmTestImpl> ();
			
			for(AlmTestImpl existingTest : existingTests) {
				if(existingTest.getKey().endsWith(test.getKey())) {
					exists = true;
					importedTest = existingTest;
					break;
				}
				existingTestMap.put(existingTest.getName(), existingTest);
			}
			
			if(!exists) {
				String tempName = className + "_" + methodName;
				if(!existingTestMap.containsKey(tempName)) {
					test.setFieldValue(AlmTest.TEST_NAME, tempName);
				} else { 
					tempName = packageName + "_" +tempName;
					if(!existingTestMap.containsKey(tempName)) {
						test.setFieldValue(AlmTest.TEST_NAME, tempName);
					} else {
						tempName = tempName +"_" +testingFramework;
						if(!existingTestMap.containsKey(tempName)) {
							test.setFieldValue(AlmTest.TEST_NAME, tempName);
						}
					}
				}
				
			}
		}
		
		if(importedTest	== null) {
			test.setFieldValue(AlmCommonProperties.PARENT_ID, String.valueOf(testFolderId));	
			test.setFieldValue(AlmTest.TS_TESTING_TOOL, testingTool);
			test.setFieldValue(AlmTest.TEST_RESPONSIBLE, testdesigner);
			return restTool.createAlmEntity(test, getTestCreationFields());
		}


		return importedTest;
	}
	
	private String [] getTestSetCreationFields() {
		return new String [] {	AlmCommonProperties.PARENT_ID,
								AlmTestSet.TESTSET_NAME,
								AlmTestSet.TESTSET_SUB_TYPE_ID};
	}
	
	private AlmTestSet importTestSet(AlmTestSet testset, int testsetFolderId) throws ExternalEntityUploadException{

		
		AlmTestSetImpl
                importedTestset = restTool.getEntityUnderParentFolder(AlmTestSetImpl.class, testsetFolderId, testset.getName());
		
		if(importedTestset == null) {
			
			testset.setFieldValue(AlmCommonProperties.PARENT_ID, String.valueOf(testsetFolderId));
			return restTool.createAlmEntity(testset, getTestSetCreationFields());
	        
		}

		
		return importedTestset;
	}
	
	private AlmTestConfig getMainTestConfig(AlmTest test){
	
        AlmTestConfigImpl testConfigImpl = new AlmTestConfigImpl();
        String queryString = String.format("query={parent-id[%s]}&fields=id,name", String.valueOf(test.getId()) );
        List<AlmTestConfigImpl> testconfigs = restTool.getAlmEntity(testConfigImpl, queryString);
		if(testconfigs != null && testconfigs.size() >0) {
			return testconfigs.get(0);
		} else {
			return null;
		}
		
	}
	
	private String [] getTestInstanceCreationFields (){
		return new String [] {	AlmTestInstance.TEST_INSTANCE_TESTSET_ID,
								AlmTestInstance.TEST_INSTANCE_CONFIG_ID,
								AlmTestInstance.TEST_INSTANCE_TEST_ID,
								AlmTestInstance.TEST_INSTANCE_TESTER_NAME,
								AlmTestInstance.TEST_INSTANCE_SUBTYPE_ID
		};
		
	}
	
	private AlmTestInstance importTestInstance(AlmTestInstance testinstance, String testsetId, String testId, String testconfigId, String tester) throws ExternalEntityUploadException{
		
		String queryString = String.format("query={cycle-id[%s];test-config-id[%s];test-id[%s]}&fields=id,name",
										String.valueOf(testsetId), String.valueOf(testconfigId), String.valueOf(testId) );

        List<AlmTestInstanceImpl> testInstances = restTool.getAlmEntity(new AlmTestInstanceImpl(), queryString);

		if(testInstances!=null && testInstances.size() > 0){
			return testInstances.get(0);
		} else {

			testinstance.setFieldValue(AlmTestInstance.TEST_INSTANCE_TESTSET_ID, String.valueOf(testsetId));
			testinstance.setFieldValue(AlmTestInstance.TEST_INSTANCE_CONFIG_ID, String.valueOf(testconfigId));
			testinstance.setFieldValue(AlmTestInstance.TEST_INSTANCE_TEST_ID, String.valueOf(testId));
			testinstance.setFieldValue(AlmTestInstance.TEST_INSTANCE_TESTER_NAME, tester);

			return restTool.createAlmEntity(testinstance, getTestInstanceCreationFields());
		}
	}
	
    private String generateImportRunName() {
        Calendar cal = new GregorianCalendar();
        cal.setTime(new java.sql.Date(System.currentTimeMillis()));
        return String.format(
                IAlmConsts.IMPORT_RUN_NAME_TEMPLATE,
                cal.get(Calendar.MONTH) + 1, // java.util.Calendar represents months from 0 to 11 instead of from 1 to 12. That's why it should be incremented.
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                cal.get(Calendar.SECOND));
    }
    

    private String[] getRunCreationFields() {
    	return new String[]{
    			AlmRun.RUN_CONFIG_ID,
    			AlmRun.RUN_CYCLE_ID,
    			AlmRun.RUN_TEST_ID,
    			AlmRun.RUN_TESTCYCL_UNIQUE_ID,
    			AlmRun.RUN_BUILD_REVISION,
    			AlmCommonProperties.NAME,
    			AlmCommonProperties.OWNER,
    			AlmRun.RUN_STATUS,
    			AlmRun.RUN_SUBTYPE_ID,
    			AlmRun.RUN_DETAIL,
    			AlmRun.RUN_DURATION,
    			AlmRun.RUN_JENKINS_JOB_NAME,
    			AlmRun.RUN_JENKINS_URL,
    			AlmRun.RUN_EXECUTION_DATE,
    			AlmRun.RUN_EXECUTION_TIME,
    			AlmRun.RUN_STATUS
    	};
    }
    
	private AlmRun generateRun(String tester, 
								AlmRun run, 
								String testsetId, 
								String testId, 
								String testInstanceId, 
								String testconfigId, 
								String subversion,
								String jobName,
								String buildUrl) throws ExternalEntityUploadException{
		
		run.setFieldValue(AlmRun.RUN_CONFIG_ID, String.valueOf(testconfigId));
		run.setFieldValue(AlmRun.RUN_CYCLE_ID, String.valueOf(testsetId));
		run.setFieldValue(AlmRun.RUN_TEST_ID, String.valueOf(testId));
		run.setFieldValue(AlmRun.RUN_TESTCYCL_UNIQUE_ID, String.valueOf(testInstanceId));
		run.setFieldValue(AlmRun.RUN_JENKINS_JOB_NAME, jobName);
		run.setFieldValue(AlmRun.RUN_JENKINS_URL, buildUrl);
		
		
		if(subversion != null && subversion.length() >0 ) {
			run.setFieldValue(AlmRun.RUN_BUILD_REVISION, subversion);
		} else {
			run.setFieldValue(AlmRun.RUN_BUILD_REVISION, "");
		}
		
		run.setFieldValue(AlmCommonProperties.NAME, generateImportRunName());
		run.setFieldValue(AlmCommonProperties.OWNER, tester);
		
		return restTool.createAlmEntity(run, getRunCreationFields());


	}

	private String[] getCreationFieldsForTestFolder() {
		return new String[] {AlmCommonProperties.NAME, AlmCommonProperties.PARENT_ID};
	}
	
	private AlmTestFolder createTestFolder(int parentId, String folderName) throws ExternalEntityUploadException {
		
		AlmTestFolderImpl testFolder = restTool.getEntityUnderParentFolder(AlmTestFolderImpl.class, parentId, folderName);
		String encodedFolderName = folderName;

		if(testFolder == null) {
			testFolder = new AlmTestFolderImpl();
			testFolder.setFieldValue(AlmCommonProperties.PARENT_ID, String.valueOf(parentId));
			testFolder.setFieldValue(AlmCommonProperties.NAME, encodedFolderName);
			return restTool.createAlmEntity(testFolder, getCreationFieldsForTestFolder());
		} else {
			return testFolder;
		}
	}
	String FOLDER_SEPERATOR = "\\";
	
	
	private AlmTestFolder createTestFolderPath(int parentId, String path) throws ExternalEntityUploadException {
		List<AlmTestFolder> folders = new ArrayList<AlmTestFolder>();
		
		StringTokenizer tokenizer = new StringTokenizer(path, FOLDER_SEPERATOR);
        
        while (tokenizer.hasMoreTokens()) {
            String itemString = tokenizer.nextToken();
			AlmTestFolder testFolder = createTestFolder(parentId, itemString);
			if(testFolder != null) {
				folders.add(testFolder);
				parentId = Integer.valueOf(testFolder.getId());
			}            
        }

        if(folders.size() >0 ){
			return folders.get(folders.size()-1);
		} else  {
			return null;
		}
	}
	
	private String[] getCreationFieldsForTestSetFolder() {
		return new String[] {AlmCommonProperties.NAME, AlmCommonProperties.PARENT_ID};
	}
	
	private AlmTestSetFolder createTestSetFolder(int parentId, String folderName) throws ExternalEntityUploadException {
		
		AlmTestSetFolderImpl
                testsetFolder = restTool.getEntityUnderParentFolder(AlmTestSetFolderImpl.class, parentId, folderName);
		
		String encodedFolderName = folderName;

		if(testsetFolder == null) {
			testsetFolder = new AlmTestSetFolderImpl();
			testsetFolder.setFieldValue(AlmCommonProperties.PARENT_ID, String.valueOf(parentId));
			testsetFolder.setFieldValue(AlmCommonProperties.NAME, encodedFolderName);
			return restTool.createAlmEntity(testsetFolder, getCreationFieldsForTestSetFolder());
		} else {
			return testsetFolder;
		}
	}

	private AlmTestSetFolder createTestSetFolderPath(int parentId, String path) throws ExternalEntityUploadException {

		List<AlmTestSetFolder> folders = new ArrayList<AlmTestSetFolder>();

		StringTokenizer tokenizer = new StringTokenizer(path, FOLDER_SEPERATOR);
        
        while (tokenizer.hasMoreTokens()) {
            String itemString = tokenizer.nextToken();
            AlmTestSetFolder testsetFolder = createTestSetFolder(parentId, itemString);
			if(testsetFolder != null) {
				folders.add(testsetFolder);
				parentId = Integer.valueOf(testsetFolder.getId());
			}            
        }		
		if(folders.size() >0 ){
			return folders.get(folders.size()-1);
		} else  {
			return null;
		}
	}	
	
	@Override
	public void UploadExternalTestSet(AlmRestInfo loginInfo,
							String reportFilePath, 
							String testsetFolderPath, 
							String testFolderPath, 
							String testingFramework, 
							String testingTool, 
							String subversion,
							String jobName, 
							String buildUrl) throws ExternalEntityUploadException{
		
		logger.log("INFO: Start to parse file: " +reportFilePath);

		ReportParserManager reportParserManager = ReportParserManager.getInstance(workspace, logger);

		List<AlmTestSet> testsets = reportParserManager.parseTestSets(reportFilePath, testingFramework,  testingTool);

		if(testsets == null) {
			logger.log("Failed to parse file: " + reportFilePath);
			throw new ExternalEntityUploadException("Failed to parse file: " + reportFilePath);
		} else  {
			logger.log("INFO: parse resut file succeed.");
		}
		
		if(testsets != null && testsets.size() >0 ) {
			logger.log("INFO: Start to login to ALM Server.");
			try {
				if( restTool.login() ) {
				
					logger.log("INFO: Checking test folder...");
					AlmTestFolder testFolder = createTestFolderPath(2, testFolderPath);
					logger.log("INFO: Checking testset folder...");
					AlmTestSetFolder testsetFolder = createTestSetFolderPath (0, testsetFolderPath);
					if(testFolder != null && testsetFolder != null){
						logger.log("INFO: Uploading ALM Entities...");
						importExternalTestSet(
								testsets, 
								loginInfo.getUserName(), 
								Integer.valueOf(testsetFolder.getId()), 
								Integer.valueOf(testFolder.getId()), 
								testingTool, 
								subversion, 
								jobName, 
								buildUrl);
					}
				} else {
					throw new ExternalEntityUploadException("Failed to login to ALM Server.");
				}
			} catch (Exception e) {
				throw new ExternalEntityUploadException(e);
			}
		}
	}
	
	
	private void importExternalTestSet(List<AlmTestSet> testsets, String tester, int testsetFolderId, int testFolderId, String testingTool, String subversion, String jobName, String buildUrl ) throws ExternalEntityUploadException{

		
		for (AlmTestSet testset : testsets){
			AlmTestSet importedTestSet = importTestSet(testset, testsetFolderId);
			if(importedTestSet == null ) {
				continue;
			}
			List<AlmEntity> testinstances = testset.getRelatedEntities().get(EntityRelation.TESTSET_TO_TESTINSTANCE_CONTAINMENT_RELATION);
			if(testinstances == null || testinstances.size() <=0) {
				continue;
			}

			for(AlmEntity testinstanceEntity: testinstances){
				AlmTestInstance testInstance = (AlmTestInstance) testinstanceEntity;
				List<AlmEntity> tests = testInstance.getRelatedEntities().get(EntityRelation.TEST_TO_TESTINSTANCE_REALIZATION_RELATION);
				if(tests == null || tests.size() <= 0) {
					continue;
				}

				AlmTest test = (AlmTest) tests.get(0);
				AlmTest importedTest = importTest(test, testFolderId, testingTool, tester);
				if(importedTest == null) {
					continue;
				}
				
				AlmTestConfig mainTestConfig = getMainTestConfig(importedTest);
				if(mainTestConfig == null) {
					continue;
				}

				AlmTestInstance importedTestInstance = importTestInstance(testInstance, importedTestSet.getId(), importedTest.getId(), mainTestConfig.getId(), tester);
				List<AlmEntity> runs = testInstance.getRelatedEntities().get(EntityRelation.TESTINSTANCE_TO_RUN_REALIZATION_RELATION);
				if(runs == null || runs.size() <= 0) {
					continue;
				}
				
				AlmRun run = (AlmRun) runs.get(0);
				generateRun(tester, 
							run,  
							importedTestSet.getId(),
							importedTest.getId(), 
							importedTestInstance.getId(), 
							mainTestConfig.getId(), 
							subversion,
							jobName,
							buildUrl
							);
			}
		}
		
	}
	
}
