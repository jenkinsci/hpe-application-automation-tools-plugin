package com.ge.applications.automation.testStepRetriever;

import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import qc.rest.examples.infrastructure.Entity;
import qc.rest.examples.infrastructure.Entity.Fields.Field;
import qc.rest.examples.infrastructure.EntityMarshallingUtils;
import qc.rest.examples.infrastructure.RestConnector;

import com.hp.application.automation.tools.sse.result.model.junit.Failure;
import com.hp.application.automation.tools.sse.result.model.junit.Testcase;
import com.hp.application.automation.tools.sse.result.model.junit.Testsuite;
import com.hp.application.automation.tools.sse.result.model.junit.Testsuites;

/**
 * Class to handle logic associated with making alm rest api calls.
 */
public class AlmTestStepRetriever {

	// field names used by ALM
	public static final String NAME = "name";
	public static final String STATUS = "status";
	public static final String DESCRIPTION = "description";
	public static final String TEST_ID = "test-id";
	public static final String PARENT_ID = "parent-id";
	public static final String EXECUTION_DATE = "execution-date";
	public static final String EXECUTION_TIME = "execution-time";
	public static final String ID = "id";
	
	// pattern for how test step iterations are reported from ALM
	private static final String ITERATION_PREFIX = "Start .* Iteration ";
	private static final String ITERATION_REGEX = ITERATION_PREFIX + "(\\d)";
	private static final String ALM_DATE_STRING = "yyyy-MM-dd HH:mm:ss";
	
	private SimpleDateFormat almDateFormat;
	private RestConnector con;
	private PrintStream logger;
	private Date buildStartTime;
	private Map<Integer, TestSetFolder> almFolders;
	
	/**
	 * Create a new instance of ALMTestStepRetriever
	 * @param con
	 * @param logger
	 * @param buildStartTime
	 */
	public AlmTestStepRetriever(RestConnector con, PrintStream logger, Date buildStartTime) {
		this.con = con;
		this.logger = logger;
		this.buildStartTime = buildStartTime;
		almDateFormat = new SimpleDateFormat(ALM_DATE_STRING);
	}
	
	/**
	 * @param testSetPaths Array of test set paths
	 * 		example path: "Root\automation\TestSetA"
	 * @return Returns the corresponding testsuite
	 */
	public Testsuites getTestSetResults(String[] testSetPaths) {
        List<Integer> testSetIds = new ArrayList<Integer>();
		for (String testSetPath: testSetPaths) {
			Integer testSetId = getTestSetIdFromPath(testSetPath);
			if (testSetId != null) {
				testSetIds.add(testSetId);
			}
        }
		almFolders = null;
		
		return getTestSetResults(testSetIds);
	}
	
	/**
	 * Get test results corresponding to a group of test sets
	 * @param testSetIds 
	 * @return
	 */
	public Testsuites getTestSetResults(List<Integer>testSetIds) {
		Testsuites testsuites = new Testsuites();
		List<Integer> testIds = new ArrayList<Integer>();
		 
		// get test ids connected to test set ids
		for (Integer testSetId: testSetIds) {
			testIds.addAll(getTestIds(testSetId));
		}
		
		// get run steps for each test id
		for (int testId: testIds) {
			Testsuite testsuite = new Testsuite();
			testsuite.setPackage("All tests");
			testsuite.setName(getTestNameById(testId));
			testsuites.getTestsuite().add(testsuite);
			
			Integer runId = getMostRecentRunIdFromTestId(testId);
			if (runId != null) {
				logger.println("Retrieving steps for test id " + testId + "; run id " + runId);
				testsuite.getTestcase().addAll(getTestRunResults(runId));
			} else {
				logger.println("No new test results found for test id " + testId);
			}
		}
		
		testsuites.setName("All tests");
		testsuites.updateCounts();
		return testsuites;
	}
	
	/**
	 * Make rest call to get test ids linked to a test set id
	 * @return Returns List of test ids
	 */
	private List<Integer> getTestIds(final int testSetId) {
		
		AlmRestEntitiesHandler<Integer> testIdGetter = new AlmRestEntitiesHandler<Integer>() {
			
			@Override
			public String getRequest() {
				return buildRequestWithFields("test-instances?query={cycle-id[" + testSetId + "]}");
			}

			@Override
			public String[] getRequiredFieldNames() {
				return new String[] {TEST_ID};
			}

			@Override
			public Integer processEntity(Map<String, String> fieldValues) {
				return Integer.parseInt(fieldValues.get(TEST_ID));
			}
			
		};
		return testIdGetter.getResult(con, logger);
	}
	
	/**
	 * Make rest call to get the name associated with a test id
	 * @return Returns test name
	 */
	private String getTestNameById(final int testId) {
		AlmRestHandler<String> testNameGetter = new AlmRestHandler<String>() {

			@Override
			public String parseXml(String xml) throws JAXBException {
				Entity testEntity = EntityMarshallingUtils.marshal(Entity.class, xml);
				List<Field> fields = testEntity.getFields().getField();
	        	for (Field field : fields) {
		        	if (field.getName().equals(NAME)) {
		        		return field.getValue().get(0);
		        	}
		        }
	        	// if nothing found, return null
	        	return null;
			}
			
			@Override
			public String getRequest() {
				return "tests/" + testId + "?fields=name";
			}
			
		};
		return testNameGetter.getResult(con, logger);
	}
	
	/**
	 * @param testId
	 * @return Returns run id of most recent run of a given test
	 */
	private Integer getMostRecentRunIdFromTestId(final int testId) {
		
		AlmRestEntitiesHandler<Integer> runIdGetter = new AlmRestEntitiesHandler<Integer>() {

			@Override
			public String getRequest() {
				return buildRequestWithFields(
						"runs?query={test-id[" + testId + "]}"
						+ "&page-size=1");
			}

			@Override
			public String[] getRequiredFieldNames() {
				return new String[] {ID, EXECUTION_DATE, EXECUTION_TIME};
			}
			
			@Override
			public Integer processEntity(Map<String, String> fieldValues) {
				Integer runId = Integer.parseInt(fieldValues.get(ID));
				String executionDate = fieldValues.get(EXECUTION_DATE);
				String executionTime = fieldValues.get(EXECUTION_TIME);
				
				if (runId != null && executionDate != null && executionTime != null) {
					try {
	        			Date lastRunTime = almDateFormat.parse(executionDate + " " + executionTime);
	        			if (lastRunTime.compareTo(buildStartTime) > 0) {
							return runId;
						}
					} catch (ParseException e) {
						e.printStackTrace(logger);
					}
				}
				
				// if we got here, we didn't find a valid runId
				return null;
			}
		};
		
		List<Integer> runIds = runIdGetter.getResult(con, logger);
		return (runIds.size() == 1)? runIds.get(0) : null;
	}
	
	/**
	 * Get test results for a give test run.
	 * Each Testcase corresponds to one call to Reporter.Report in UFT
	 * @param runId
	 * @return Returns a list of Testcases associated with a run
	 */
	private List<Testcase> getTestRunResults(final int runId) {
		
		AlmRestEntitiesHandler<Testcase> resultGetter = new AlmRestEntitiesHandler<Testcase>() {
			
			private int currentIteration = 0;
			
			@Override
			public String getRequest() {
				return buildRequestWithFields("runs/" + runId + "/run-steps?");
			}

			@Override
			public String[] getRequiredFieldNames() {
				return new String[] {NAME, STATUS, DESCRIPTION};
			}

			@Override
			public Testcase processEntity(Map<String, String> fieldValues) {
				Testcase testcase = null;
				
				String status = fieldValues.get(STATUS);
				String description = fieldValues.get(DESCRIPTION);
				
				// if status exists, build the testcase
				if (!status.isEmpty()) {
					String testName = String.format("[%d]%s", 
							currentIteration, 
							fieldValues.get(NAME));
					
        			testcase = new Testcase(testName, status);
        			
        			// add failure info if needed
        			if (testcase.isFailure()) {
        				testcase.getFailure().add(new Failure(description));
        			}
				} 
				
				// if announcing a new iteration, update iteration
				else if (description.matches(ITERATION_REGEX)) {
					currentIteration = Integer.parseInt(
    						description.replaceAll(ITERATION_PREFIX, ""));
				}
				return testcase;
			}
		};
		
		return resultGetter.getResult(con, logger);
	}
	
	/**
	 * @param testSetPath In the form "Root\(directories\)*TestSetName
	 * @return Returns the test set id for a given test set path
	 */
	private Integer getTestSetIdFromPath(String testSetPath) {
		// split path at slashes to separate directory names and the test set name
		final String[] parts = testSetPath.split("[/\\\\]");
		
		// get all test sets with the given test set name
		List<TestSetFolder> possibleTestSets = getPossibleTestSets(parts[parts.length - 1]); 
		
		Integer testSetId = null;
		if (possibleTestSets != null) {
			if (possibleTestSets.size() == 0) {
				logger.println("Test set \"" + testSetPath + "\" not found.");
			} else if (possibleTestSets.size() == 1) {
				testSetId = possibleTestSets.get(0).getId();
			} else {
				TestSetFolder testSet = getActualTestSet(parts, possibleTestSets);
				if (testSet != null) {
					testSetId = testSet.getId();
				}
			}
		}
		
		return testSetId;
	}
	
	/**
	 * @param testSetName
	 * @return Returns a list of test sets with the provided name
	 */
	private List<TestSetFolder> getPossibleTestSets(final String testSetName) {
		
		AlmRestEntitiesHandler<TestSetFolder> testSetGetter = new AlmRestEntitiesHandler<TestSetFolder>() {

			@Override
			public String getRequest() {
				return buildRequestWithFields(
						"test-sets?"
						+ "query={name[" + testSetName + "]}");
			}

			@Override
			public String[] getRequiredFieldNames() {
				return new String[] {NAME, ID, PARENT_ID};
			}

			@Override
			public TestSetFolder processEntity(Map<String, String> fieldValues) {
				return new TestSetFolder(
						fieldValues.get(NAME),
						Integer.parseInt(fieldValues.get(ID)),
						Integer.parseInt(fieldValues.get(PARENT_ID)));
			}
		
		};
		return testSetGetter.getResult(con, logger);
	}
	
	/**
	 * @return Returns a map with test ids as keys and
	 * 		test sets as values
	 */
	private Map<Integer, TestSetFolder> getAlmFolders() {
		
		AlmRestEntitiesHandler<TestSetFolder> folderGetter = new AlmRestEntitiesHandler<TestSetFolder>() {

			@Override
			public String getRequest() {
				return buildRequestWithFields("test-set-folders?");
			}

			@Override
			public String[] getRequiredFieldNames() {
				return new String[] {NAME, ID, PARENT_ID};
			}

			@Override
			public TestSetFolder processEntity(Map<String, String> fieldValues) {
				return new TestSetFolder(
						fieldValues.get(NAME),
						Integer.parseInt(fieldValues.get(ID)),
						Integer.parseInt(fieldValues.get(PARENT_ID)));
			}
		};
		
		Map<Integer, TestSetFolder> directoryMap = new HashMap<Integer, TestSetFolder>();
		List<TestSetFolder> directories = folderGetter.getResult(con, logger);
		
		for (TestSetFolder directory: directories) {
			directoryMap.put(directory.getId(), directory);
		}
		
		return directoryMap;
	}
	
	/**
	 * Get the TestSet that corresponds to the given path
	 * @param pathParts Array of directory names, ending with test set name
	 * 		e.g. {"Root", "automation", "TestSetA"}
	 * @param possibleTestSets List of test sets whose names equal the last element in pathParts
	 * @return Returns the TestSet whose directory structure matches pathParts
	 */
	private TestSetFolder getActualTestSet(String[] pathParts, List<TestSetFolder> possibleTestSets) {
		
		// load test set folder data if it hasn't been done already
		if (almFolders == null) {
			almFolders = getAlmFolders();
		}
		
		// check each test set to see if it matches the provide path
		for (TestSetFolder testSet: possibleTestSets) {
			TestSetFolder currentFolder = almFolders.get(testSet.getParentId());
			int i = pathParts.length - 2;
			
			/*
			 * Begin with the name of the desired test set's parent directory
			 * and the current test set's parent directory.
			 * As long as we have not arrived at the root directory (pathParts[0])
			 * and the current directory's name == the desired directory's name,
			 * move references to parent directory.
			 */
			while (i > 0 
					&& currentFolder != null 
					&& currentFolder.getName().equals(pathParts[i])) {
				currentFolder = almFolders.get(currentFolder.getParentId());
				i--;
			}
			
			// if we made it to the root directory for both the current test set
			// and the desired test set, we are on the correct TestSet
			if (i == 0 && currentFolder == null) {
				return testSet;
			}
		}
		
		// execution should never get here
		return null;
	}
	
}