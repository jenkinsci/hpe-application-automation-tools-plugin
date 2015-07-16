package com.ge.applications.automation.testStepRetriever;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import qc.rest.examples.infrastructure.Entities;
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

	private static final String ITERATION_PREFIX = "Start .* Iteration ";
	private static final String ITERATION_REGEX = ITERATION_PREFIX + "(\\d)";
	
	public static final String NAME_FIELD = "name";
	public static final String STATUS_FIELD = "status";
	public static final String DESCRIPTION_FIELD = "description";
	public static final String TEST_ID = "test-id";
	public static final String PARENT_ID = "parent-id";
	public static final String ID = "id";
	
	private static final String ALM_DATE_STRING = "yyyy-MM-dd hh:mm:ss";
	
	private SimpleDateFormat almDateFormat;
	private RestConnector con;
	private PrintStream logger;
	private Map<Integer, TestSetFolder> almFolders;
	
	/**
	 * Create a new instance of ALMTestStepRetriever
	 * @param logger Logger to use
	 */
	public AlmTestStepRetriever(RestConnector con, PrintStream logger) {
		this.con = con;
		this.logger = logger;
		
		almDateFormat = new SimpleDateFormat(ALM_DATE_STRING);
	}
	
	public Testsuites getTestSetResults(String[] testSetPaths) {
        List<Integer> testSetIds = new ArrayList<Integer>();
		for (String testSetPath: testSetPaths) {
        	testSetIds.add(getTestSetIdFromPath(testSetPath));
        }
		almFolders = null;
		
		return getTestSetResults(testSetIds);
	}
	
	/**
	 * Get test results corresponding to a group of test sets
	 * @param con
	 * @param testSetIds 
	 * @return
	 */
	public Testsuites getTestSetResults(List<Integer>testSetIds) {
		Testsuites testsuites = new Testsuites();
		List<Integer> testIds = new ArrayList<Integer>();
		 
		// get test ids connected to test set ids
		for (Integer testSetId: testSetIds) {
			testIds.addAll(getTestIds(con, testSetId));
		}
		
		// get run steps for each test id
		for (int testId: testIds) {
			Testsuite testsuite = new Testsuite();
			testsuite.setPackage("All tests");
			testsuite.setName(getTestNameById(testId));
			testsuites.getTestsuite().add(testsuite);
			
			int runId = getMostRecentRunIdFromTestId(testId);
			testsuite.getTestcase().addAll(getTestRunResults(runId));
		}
		
		testsuites.setName("All tests");
		testsuites.updateCounts();
		return testsuites;
	}
	
	/**
	 * Make rest call to get test ids linked to a test set id
	 * @param con
	 * @param testSetId
	 * @return Returns List of test ids
	 */
	private List<Integer> getTestIds(RestConnector con, final int testSetId) {
		AlmRestHandler<List<Integer>> testIdGetter = new AlmRestHandler<List<Integer>>() {
			
			@Override
			public List<Integer> parseXml(String xml) throws JAXBException {
				List<Integer> testIds = new ArrayList<Integer>();
				Entities entitySet = EntityMarshallingUtils.marshal(Entities.class, xml);
				List<Entity> entities = entitySet.getEntities();
		        for (Entity entity: entities) {
		        	List<Field> fields = entity.getFields().getField();
		        	for (Field field : fields) {
			        	if (field.getName().equals(TEST_ID)) {
			        		testIds.add(Integer.parseInt(field.getValue().get(0)));
			        	}
			        }
		        }
		        return testIds;
			}
			
			@Override
			public String getRequest() {
				return "test-instances?query={cycle-id[" + testSetId + "]}";
			}
		};
		return testIdGetter.getResult(con, logger);
	}
	
	/**
	 * Make rest call to get the name associated with a test id
	 * @param con
	 * @param testId
	 * @return Returns test name
	 */
	private String getTestNameById(final int testId) {
		AlmRestHandler<String> testNameGetter = new AlmRestHandler<String>() {

			@Override
			public String parseXml(String xml) throws JAXBException {
				Entity testEntity = EntityMarshallingUtils.marshal(Entity.class, xml);
				List<Field> fields = testEntity.getFields().getField();
	        	for (Field field : fields) {
		        	if (field.getName().equals(NAME_FIELD)) {
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
	 * Get the id of the most recent run for the given test id
	 * @param con
	 * @param testId
	 * @return Returns run id of most recent run of a given test
	 */
	private int getMostRecentRunIdFromTestId(final int testId) {
		
		AlmRestHandler<Integer> runIdGetter = new AlmRestHandler<Integer>() {

			@Override
			public Integer parseXml(String xml) throws JAXBException {
				Entities entitySet = EntityMarshallingUtils.marshal(Entities.class, xml);
				List<Entity> entities = entitySet.getEntities();
		        for (Entity entity: entities) {
		        	List<Field> fields = entity.getFields().getField();
		        	for (Field field : fields) {
			        	if (field.getName().equals(ID)) {
			        		return Integer.parseInt(field.getValue().get(0));
			        	}
			        }
		        }
		        return null;
			}

			@Override
			public String getRequest() {
				return "runs?query={test-id[" + testId + "]}"
						+ "&page-size=1"
						+ "&fields=id";
			}
			
		};
		
		Integer runId = runIdGetter.getResult(con, logger);
		return runId != null? runId: -1;
	}
	
	/**
	 * Get test results for a give test run.
	 * Each Testcase corresponds to one call to Reporter.Report in UFT
	 * @param con
	 * @param logger
	 * @param runId
	 * @return Returns a list of Testcases associated with a run
	 */
	private List<Testcase> getTestRunResults(final int runId) {
		AlmRestHandler<List<Testcase>> resultGetter = new AlmRestHandler<List<Testcase>>() {
			
			@Override
			public List<Testcase> parseXml(String xml) throws JAXBException {
				List<Testcase> results = new ArrayList<Testcase>();
				Entities entitySet = EntityMarshallingUtils.marshal(Entities.class, xml);
				
				List<Entity> entities = entitySet.getEntities();
				
				// Current iteration must be kept track of.
	        	// One test step with no status will have a description
	        	// in the form "Start Global Iteration x".
	        	// All subsequent steps belong to that iteration
	        	int currentIteration = 0;
		        for (Entity entity: entities) {
		        	List<Field> fields = entity.getFields().getField();
		        	List<String> stepName = null;
		        	List<String> stepStatus = null;
		        	List<String> stepDescription = null;
		        	
			        for (Field field : fields) {
			        	
			        	// get needed fields
			        	if (field.getName().equals(NAME_FIELD)) {
			        		stepName = field.getValue();
			        	} else if (field.getName().equals(STATUS_FIELD)) {
			        		stepStatus = field.getValue();
			        	} else if (field.getName().equals(DESCRIPTION_FIELD)) {
			        		stepDescription = field.getValue();
			        	}
			        }
			        if (stepStatus != null) {
			        	// for each status, add a test case
			        	for (int i = 0; i < stepStatus.size(); i++) {
			        		String status = stepStatus.get(i);
			        		
			        		// if status is non-empty, add the step
			        		if (!status.isEmpty()) {
			        			String testName = String.format("[%d]%s", currentIteration, stepName.get(i));
			        			Testcase testcase = new Testcase(
			        					testName,
			        					stepStatus.get(i));
			        			results.add(testcase);
			        			
			        			// add failure info if needed
			        			if (testcase.isFailure()) {
			        				testcase.getFailure().add(new Failure(stepDescription.get(i)));
			        			}
			        		}
			        		
			        		// if no status, check if we need to update current iteration
			        		else {
			        			String description = stepDescription.get(i);
			        			if (description.matches(ITERATION_REGEX)) {
			        				currentIteration = Integer.parseInt(
			        						description.replaceAll(ITERATION_PREFIX, ""));
			        			}
			        		}
			        	}
			        }
		        }
		        
		        return results;
			}
			
			@Override
			public String getRequest() {
				return "runs/" + runId + "/run-steps";
			}
		};
		
		return resultGetter.getResult(con, logger);
	}
	
	private int getTestSetIdFromPath(String testSetPath) {
		// split path at slashes to separate directory names and the test set name
		final String[] parts = testSetPath.split("[/\\\\]");
		
		// get all test sets with the given test set name
		List<TestSetFolder> possibleTestSets = getPossibleTestSets(parts[parts.length - 1]); 
		
		int testSetId = -1;
		
		/*if (possibleTestSets.size() == 1) {
			testSetId = possibleTestSets.get(0).getId();
		} else */{
			//for (TestSetFolder testSet: possibleTestSets) {
				//logger.println("Test set id: " + testSet.getName());
				testSetId = getActualTestSet(parts, possibleTestSets).getId();
			//}
		}
		
		// TODO return testSetId
		return testSetId;
		
	}
	
	private List<TestSetFolder> getPossibleTestSets(final String testSetName) {
		// get possible test sets based on test set name alone
		AlmRestHandler<List<TestSetFolder>> testSetGetter = new AlmRestHandler<List<TestSetFolder>>() {

			@Override
			public String getRequest() {
				return "test-sets?"
						+ "query={name[" + testSetName + "]}"
						+ "&fields=name,id,parent-id";
			}

			@Override
			public List<TestSetFolder> parseXml(String xml) throws JAXBException {
				List<TestSetFolder> possibleTestSets = new ArrayList<TestSetFolder>();
				Entities entitySet = EntityMarshallingUtils.marshal(Entities.class, xml);
				
				List<Entity> entities = entitySet.getEntities();
		        for (Entity entity: entities) {
		        	List<Field> fields = entity.getFields().getField();
		        	List<String> testSetName = null;
		        	List<String> testSetId = null;
		        	List<String> testSetParent = null;
			        for (Field field : fields) {
			        	
			        	// get needed fields
			        	if (field.getName().equals(NAME_FIELD)) {
			        		testSetName = field.getValue();
			        	} else if (field.getName().equals(ID)) {
			        		testSetId = field.getValue();
			        	} else if (field.getName().equals(PARENT_ID)) {
			        		testSetParent = field.getValue();
			        	}
			        }
			        if (testSetName != null) {
			        	// for each name, check if it matches the desired name
			        	for (int i = 0; i < testSetName.size(); i++) {
			        		possibleTestSets.add(new TestSetFolder(testSetName.get(i),
			        				Integer.parseInt(testSetId.get(i)),
			        				Integer.parseInt(testSetParent.get(i))));
			        	}
			        }
		        }
				
				return possibleTestSets;
			}
		
		};
		return testSetGetter.getResult(con, logger);
	}
	
	private Map<Integer, TestSetFolder> getAlmFolders(RestConnector con, final PrintStream logger) {
		AlmRestHandler<Map<Integer, TestSetFolder>> folderGetter = new AlmRestHandler<Map<Integer, TestSetFolder>>() {

			@Override
			public String getRequest() {
				return "test-set-folders?fields=name,id,parent-id";
			}

			@Override
			public Map<Integer, TestSetFolder> parseXml(String xml) throws JAXBException {
				Map<Integer, TestSetFolder> possibleTestSets = new HashMap<Integer, TestSetFolder>();
				Entities entitySet = EntityMarshallingUtils.marshal(Entities.class, xml);
				
				List<Entity> entities = entitySet.getEntities();
		        for (Entity entity: entities) {
		        	List<Field> fields = entity.getFields().getField();
		        	List<String> testSetName = null;
		        	List<String> testSetId = null;
		        	List<String> testSetParent = null;
			        for (Field field : fields) {
			        	
			        	// get needed fields
			        	if (field.getName().equals(NAME_FIELD)) {
			        		testSetName = field.getValue();
			        	} else if (field.getName().equals(ID)) {
			        		testSetId = field.getValue();
			        	} else if (field.getName().equals(PARENT_ID)) {
			        		testSetParent = field.getValue();
			        	}
			        }
			        if (testSetName != null) {
			        	// for each name, check if it matches the desired name
			        	for (int i = 0; i < testSetName.size(); i++) {
			        		int id = Integer.parseInt(testSetId.get(i));
			        		TestSetFolder testSet = new TestSetFolder(testSetName.get(i), id,
	        						Integer.parseInt(testSetParent.get(i)));
			        		possibleTestSets.put(id, testSet);
			        	}
			        }
		        }
				
				return possibleTestSets;
			}
		
		};
		return folderGetter.getResult(con, logger);
		
	}
	
	private TestSetFolder getActualTestSet(String[] pathParts, List<TestSetFolder> possibleTestSets) {
		
		// load test set folder data if it hasn't been done already
		if (almFolders == null) {
			almFolders = getAlmFolders(con, logger);
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
