package com.ge.application.automation.steps;

import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import com.ge.application.automation.steps.Entity;
import com.ge.application.automation.steps.Entities;
import com.ge.application.automation.steps.Entity.Fields.Field;
import com.hp.application.automation.tools.rest.RestClient;
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
	private static final String ITERATION_PREFIX_REGEX = "Start .* Iteration ";
	private static final String ITERATION_DESCRIPTION_REGEX = ITERATION_PREFIX_REGEX + "(\\d)";
	private static final String DIRECTORY_SPLIT_REGEX = "[/\\\\]";
	private static final String ALM_DATE_STRING = "yyyy-MM-dd HH:mm:ss";
	
	private SimpleDateFormat almDateFormat;
	private RestClient restClient;
	private PrintStream logger;
	private Date buildStartTime;
	private Map<Integer, TestSet> almFolders;
	
	/**
	 * Create a new instance of ALMTestStepRetriever
	 * @param restClient
	 * @param logger
	 * @param buildStartTime
	 */
	public AlmTestStepRetriever(RestClient restClient, PrintStream logger, Date buildStartTime) {
		this.restClient = restClient;
		this.logger = logger;
		this.buildStartTime = buildStartTime;
		almDateFormat = new SimpleDateFormat(ALM_DATE_STRING);
	}
	
	/**
	 * Get test results corresponding to a group of test sets
	 * @param testSetIds 
	 * @return
	 */
	public Testsuites getTestSetResults(String[] testSetPaths) {
		List<TestSet> testSets = getTestSetsFromPaths(testSetPaths);
		Testsuites testsuites = new Testsuites();
		
		for (TestSet testSet: testSets) {
			
			// get test ids for test set
			List<Integer> testIds = getTestIds(testSet.getId());
			for (int testId: testIds) {
				Integer runId = getMostRecentRunIdFromTestId(testId);
				
				// if valid run id, add results
				if (runId != null) {
					
					// get run results as a list of testsuites
					List<Testsuite> testsuiteList = getTestRunResults(
							testSet.getName(),
							getTestNameById(testId), 
							runId);
					
					testsuites.getTestsuite().addAll(testsuiteList);
				} else {
					logger.println("No new test results found for test id " + testId);
				}
			}
		}
		
		testsuites.updateCounts();
		return testsuites;
	}
	
	/**
	 * Get test sets associated with test set paths
	 * @param testSetPaths
	 * @return Returns a list of TestSets in the same order
	 * 		as the test set paths.
	 */
	private List<TestSet> getTestSetsFromPaths(String[] testSetPaths) {
		List<TestSet> testSets = new ArrayList<TestSet>();
		
		// load testSets based on provided test set paths
		for (String testSetPath: testSetPaths) {
			Integer testSetId = getTestSetIdFromPath(testSetPath);
			if (testSetId != null) {
				testSets.add(new TestSet(
						getTestSetName(testSetPath),
						testSetId));
			}
        }
		return testSets;
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
		return testIdGetter.getResult(restClient, logger);
	}
	
	/**
	 * Make rest call to get the name associated with a test id
	 * @return Returns test name
	 */
	private String getTestNameById(final int testId) {
		AlmRestHandler<String> testNameGetter = new AlmRestHandler<String>() {

			@Override
			public String parseXml(String xml) throws JAXBException {
				Entity testEntity = MarshallingUtility.unmarshal(Entity.class, xml);
				List<Field> fields = testEntity.getFields().getField();
	        	for (Field field : fields) {
		        	if (field.getName().equals(NAME)) {
		        		return field.getValue().get(0).getValue();
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
		return testNameGetter.getResult(restClient, logger);
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
		
		List<Integer> runIds = runIdGetter.getResult(restClient, logger);
		return (runIds.size() == 1)? runIds.get(0) : null;
	}
	
	/**
	 * Get test results for a give test run.
	 * Each Testcase corresponds to one call to Reporter.Report in UFT
	 * @param runId
	 * @return Returns a list of Testcases associated with a run
	 */
	private List<Testsuite> getTestRunResults(
			final String testSetName,
			final String testName, 
			final int runId) {
		
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
        			testcase = new Testcase(fieldValues.get(NAME), status);
        			testcase.setIteration(currentIteration);
        			testcase.setClassname(String.format("%s.%s [%d]", 
        					testSetName,
        					toHumanReadable(testName), 
        					currentIteration));
        			// add failure info if needed
        			if (testcase.isFailure()) {
        				testcase.getFailure().add(new Failure(description));
        			}
				} 
				
				// if announcing a new iteration, update iteration
				else if (description.matches(ITERATION_DESCRIPTION_REGEX)) {
					currentIteration = Integer.parseInt(
    						description.replaceAll(ITERATION_PREFIX_REGEX, ""));
				}
				return testcase;
			}
		};
		
		List<Testcase> runSteps = resultGetter.getResult(restClient, logger);
		return convertTestStepsToTestsuites(runSteps, testName);
	}
	
	/**
	 * Group test steps into test suites
	 * @param runSteps List of run steps. If test steps are from multiple iterations,
	 * 		they must be grouped by iteration.
	 * @param testName Name of the test the steps are associated with.
	 * @return Returns a list of Testsuites (1 per iteration)
	 */
	private List<Testsuite> convertTestStepsToTestsuites(List<Testcase> runSteps, String testName) {
		List<Testsuite> tests = new ArrayList<Testsuite>();
		Testsuite currentTest = null;
		int currentIteration = -1;
		for (Testcase runStep: runSteps) {
			
			// create a new testsuite if we reached a new iteration
			if (currentIteration != runStep.getIteration()) {
				currentIteration = runStep.getIteration();
				currentTest = new Testsuite();
				tests.add(currentTest);
				currentTest.setName(String.format("%s[%d]", testName, currentIteration));
			}
			currentTest.getTestcase().add(runStep);
		}
		
		return tests;
	}
	
	/**
	 * @param testSetPath In the form "Root\(directories\)*TestSetName
	 * @return Returns the test set id for a given test set path
	 */
	private Integer getTestSetIdFromPath(String testSetPath) {
		// split path at slashes to separate directory names and the test set name
		final String[] parts = testSetPath.split(DIRECTORY_SPLIT_REGEX);
		
		// get all test sets with the given test set name
		List<TestSet> possibleTestSets = getPossibleTestSets(parts[parts.length - 1]); 
		
		Integer testSetId = null;
		if (possibleTestSets != null) {
			if (possibleTestSets.size() == 0) {
				logger.println("Test set \"" + testSetPath + "\" not found.");
			} else if (possibleTestSets.size() == 1) {
				testSetId = possibleTestSets.get(0).getId();
			} else {
				TestSet testSet = getActualTestSet(parts, possibleTestSets);
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
	private List<TestSet> getPossibleTestSets(final String testSetName) {
		
		AlmRestEntitiesHandler<TestSet> testSetGetter = new AlmRestEntitiesHandler<TestSet>() {

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
			public TestSet processEntity(Map<String, String> fieldValues) {
				return new TestSet(
						fieldValues.get(NAME),
						Integer.parseInt(fieldValues.get(ID)),
						Integer.parseInt(fieldValues.get(PARENT_ID)));
			}
		
		};
		return testSetGetter.getResult(restClient, logger);
	}
	
	/**
	 * @return Returns a map with test ids as keys and
	 * 		test sets as values
	 */
	private Map<Integer, TestSet> getAlmFolders() {
		
		AlmRestEntitiesHandler<TestSet> folderGetter = new AlmRestEntitiesHandler<TestSet>() {

			@Override
			public String getRequest() {
				return buildRequestWithFields("test-set-folders?");
			}

			@Override
			public String[] getRequiredFieldNames() {
				return new String[] {NAME, ID, PARENT_ID};
			}

			@Override
			public TestSet processEntity(Map<String, String> fieldValues) {
				return new TestSet(
						fieldValues.get(NAME),
						Integer.parseInt(fieldValues.get(ID)),
						Integer.parseInt(fieldValues.get(PARENT_ID)));
			}
		};
		
		Map<Integer, TestSet> directoryMap = new HashMap<Integer, TestSet>();
		List<TestSet> directories = folderGetter.getResult(restClient, logger);
		
		for (TestSet directory: directories) {
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
	private TestSet getActualTestSet(String[] pathParts, List<TestSet> possibleTestSets) {
		
		// load test set folder data if it hasn't been done already
		if (almFolders == null) {
			almFolders = getAlmFolders();
		}
		
		// check each test set to see if it matches the provide path
		for (TestSet testSet: possibleTestSets) {
			TestSet currentFolder = almFolders.get(testSet.getParentId());
			int i = pathParts.length - 2;
			
			/*
			 * Begin with the name of the desired test set's parent directory
			 * and the current test set's parent directory.
			 * As long as we have not arrived at the root directory (pathParts[0])
			 * and the current directory's name == the desired directory's name,
			 * move references to parent directory.
			 */
			while (i > 0 && currentFolder != null 
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
	
	/**
	 * Helper function to get test set name from path
	 * @param testSetPath
	 * @return Returns test set name
	 */
	private String getTestSetName(String testSetPath) {
		String[] parts = testSetPath.split(DIRECTORY_SPLIT_REGEX);
		return toHumanReadable(parts[parts.length - 1]);
	}
	
	private String toHumanReadable(String s) {
		return s.replaceAll("_", " ");
	}
	
}