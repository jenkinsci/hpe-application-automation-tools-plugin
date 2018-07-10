/*
 * © Copyright 2013 EntIT Software LLC
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright (c) 2018 Micro Focus Company, L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ___________________________________________________________________
 *
 */

package com.hpe.application.automation.tools.octane.executor;

import com.google.inject.Inject;
import com.hpe.application.automation.tools.common.HttpStatus;
import com.hpe.application.automation.tools.octane.ResultQueue;
import com.hpe.application.automation.tools.octane.actions.UftTestType;
import com.hpe.application.automation.tools.octane.actions.dto.*;
import com.hpe.application.automation.tools.octane.actions.dto.AutomatedTests;
import com.hpe.application.automation.tools.octane.actions.dto.ListNodeEntity;
import com.hpe.application.automation.tools.octane.actions.dto.ListNodeEntityCollection;
import com.hpe.application.automation.tools.octane.actions.dto.ScmResourceFile;
import com.hpe.application.automation.tools.octane.configuration.ConfigurationService;
import com.hpe.application.automation.tools.octane.configuration.ServerConfiguration;
import com.hpe.application.automation.tools.octane.tests.AbstractSafeLoggingAsyncPeriodWork;
import com.hp.mqm.client.MqmRestClient;
import com.hp.mqm.client.QueryHelper;
import com.hp.mqm.client.exception.RequestErrorException;
import com.hp.mqm.client.model.Entity;
import com.hp.mqm.client.model.ListItem;
import com.hp.mqm.client.model.PagedList;
import com.hpe.application.automation.tools.octane.actions.dto.AutomatedTest;
import com.hpe.application.automation.tools.octane.actions.dto.BaseRefEntity;
import com.hpe.application.automation.tools.octane.actions.dto.ScmResources;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.TimeUnit2;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.processors.PropertyNameProcessor;
import net.sf.json.util.PropertyFilter;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;


/**
 * This class is responsible to send discovered uft tests to Octane.
 * Class uses file-based queue so if octane or jenkins will be down before sending,
 * after connection is up - this dispatcher will send tests to Octane.
 * <p>
 * Actually list of discovered tests are persisted in job run directory. Queue contains only reference to that job run.
 */
@Extension
public class UftTestDiscoveryDispatcher extends AbstractSafeLoggingAsyncPeriodWork {

    private final static Logger logger = LogManager.getLogger(UftTestDiscoveryDispatcher.class);
    private final static String DUPLICATE_ERROR_CODE = "platform.duplicate_entity_error";
    private final static int POST_BULK_SIZE = 100;
    private final static int MAX_DISPATCH_TRIALS = 5;

    private UftTestDiscoveryQueue queue;

    public UftTestDiscoveryDispatcher() {
        super("Uft Test Discovery Dispatcher");
    }


    @Override
    protected void doExecute(TaskListener listener) throws IOException, InterruptedException {
        if (queue.peekFirst() == null) {
            return;
        }

        logger.warn("Queue size  " + queue.size());
        ServerConfiguration serverConfiguration = ConfigurationService.getServerConfiguration();
        MqmRestClient client = ConfigurationService.createClient(serverConfiguration);

        if (client == null) {
            logger.warn("There are pending discovered UFT tests, but MQM server configuration is not valid, results can't be submitted");
            return;
        }

        ResultQueue.QueueItem item = null;
        try {
            while ((item = queue.peekFirst()) != null) {

                Job project = (Job) Jenkins.getInstance().getItemByFullName(item.getProjectName());
                if (project == null) {
                    logger.warn("Project [" + item.getProjectName() + "] no longer exists, pending discovered tests can't be submitted");
                    queue.remove();
                    continue;
                }

                Run build = project.getBuildByNumber(item.getBuildNumber());
                if (build == null) {
                    logger.warn("Build [" + item.getProjectName() + "#" + item.getBuildNumber() + "] no longer exists, pending discovered tests can't be submitted");
                    queue.remove();
                    continue;
                }

                UFTTestDetectionResult result = UFTTestDetectionService.readDetectionResults(build);
                if (result == null) {
                    logger.warn("Build [" + item.getProjectName() + "#" + item.getBuildNumber() + "] no longer contains valid detection result file");
                    queue.remove();
                    continue;
                }

                logger.warn("Persistence [" + item.getProjectName() + "#" + item.getBuildNumber() + "]");
                dispatchDetectionResults(item, client, result);
                queue.remove();
            }
        } catch (Exception e) {
            if (item != null) {
                item.incrementFailCount();
                if (item.incrementFailCount() > MAX_DISPATCH_TRIALS) {
                    queue.remove();
                    logger.warn("Failed to  persist discovery of [" + item.getProjectName() + "#" + item.getBuildNumber() + "]  after " + MAX_DISPATCH_TRIALS + " trials");
                }
            }
        }
    }

    private static void dispatchDetectionResults(ResultQueue.QueueItem item, MqmRestClient client, UFTTestDetectionResult result) {
        //Check if there is diff in discovery and server status
        //for example : discovery found new test , but it already exist in server , instead of create new tests we will do update test
        if (result.isFullScan()) {
            validateTestDiscoveryForFullDetection(client, result);
            validateDataTablesDiscoveryForFullDetection(client, result);
        } else {
            validateTestDiscoveryAndCompleteTestIdsForScmChangeDetection(client, result);
            //no need to add validation for dataTables, because there is no DTs update and there is no special delete strategy
        }

        //publish final results
        FreeStyleProject project = (FreeStyleProject) Jenkins.getInstance().getItemByFullName(item.getProjectName());
        FilePath subWorkspace = project.getWorkspace().child("_Final_Detection_Results");
        try {
            if (!subWorkspace.exists()) {
                subWorkspace.mkdirs();
            }
            File reportXmlFile = new File(subWorkspace.getRemote(), "final_detection_result_build_" + item.getBuildNumber() + ".xml");
            UFTTestDetectionService.publishDetectionResults(reportXmlFile, null, result);
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to write final_detection_result file :" + e.getMessage());
        }

        //post new tests
        if (!result.getNewTests().isEmpty()) {
            boolean posted = postTests(client, result.getNewTests(), result.getWorkspaceId(), result.getScmRepositoryId());
            logger.warn("Persistence [" + item.getProjectName() + "#" + item.getBuildNumber() + "] : " + result.getNewTests().size() + "  new tests posted successfully = " + posted);
        }

        //post test updated
        if (!result.getUpdatedTests().isEmpty()) {
            boolean updated = updateTests(client, result.getUpdatedTests(), result.getWorkspaceId());
            logger.warn("Persistence [" + item.getProjectName() + "#" + item.getBuildNumber() + "] : " + result.getUpdatedTests().size() + "  updated tests posted successfully = " + updated);
        }

        //post test deleted
        if (!result.getDeletedTests().isEmpty()) {
            boolean updated = updateTests(client, result.getDeletedTests(), result.getWorkspaceId());
            logger.warn("Persistence [" + item.getProjectName() + "#" + item.getBuildNumber() + "] : " + result.getDeletedTests().size() + "  deleted tests set as not executable successfully = " + updated);
        }

        //post scm resources
        if (!result.getNewScmResourceFiles().isEmpty()) {
            boolean posted = postScmResources(client, result.getNewScmResourceFiles(), result.getWorkspaceId(), result.getScmRepositoryId());
            logger.warn("Persistence [" + item.getProjectName() + "#" + item.getBuildNumber() + "] : " + result.getNewScmResourceFiles().size() + "  new scmResources posted successfully = " + posted);
        }

        //delete scm resources
        if (!result.getDeletedScmResourceFiles().isEmpty()) {
            boolean posted = deleteScmResources(client, result.getDeletedScmResourceFiles(), result.getWorkspaceId(), result.getScmRepositoryId());
            logger.warn("Persistence [" + item.getProjectName() + "#" + item.getBuildNumber() + "] : " + result.getDeletedScmResourceFiles().size() + "  scmResources deleted successfully = " + posted);
        }
    }

    /**
     * This method try to find ids of updated and deleted tests for scm change detection
     * if test is found on server - update id of discovered test
     * if test is not found and test is marked for update - move it to new tests (possibly test was deleted on server)
     *
     * @return true if there were changes comparing to discoverede results
     */
    private static boolean validateTestDiscoveryAndCompleteTestIdsForScmChangeDetection(MqmRestClient client, UFTTestDetectionResult result) {
        boolean hasDiff = false;
        List<AutomatedTest> allTests = new ArrayList<>();
        allTests.addAll(result.getUpdatedTests());
        allTests.addAll(result.getDeletedTests());
        Set<String> allTestNames = new HashSet<>();
        for (AutomatedTest test : allTests) {
            allTestNames.add(test.getName());
        }

        //GET TESTS FROM OCTANE
        Map<String, Entity> octaneTestsMapByKey = getTestsFromServer(client, Long.parseLong(result.getWorkspaceId()), Long.parseLong(result.getScmRepositoryId()), allTestNames);


        //MATCHING
        for (AutomatedTest test : allTests) {
            String key = createKey(test.getPackage(), test.getName());
            Entity octaneTest = octaneTestsMapByKey.get(key);
            if (octaneTest != null) {
                test.setId(octaneTest.getId());
            } else {//no match{
                hasDiff = true;
                if (result.getUpdatedTests().remove(test)) {
                    result.getNewTests().add(test);
                } else {
                    //test that is marked to be deleted - doesn't exist in Octane - do nothing
                    result.getDeletedTests().remove(test);
                }
            }
        }

        return hasDiff;
    }

    /**
     * This method check whether discovered test are already exist on server, and instead of creation - those tests will be updated
     * Go over discovered and octane tests
     * 1.if test doesn't exist on octane - this is new test
     * 2.if test exist
     * 2.1 if test different from discovered - this is test for update
     * 2.2 if tests are equal - skip test
     * 3. all tests that are found in Octane but not discovered - those deleted tests and they will be turned to not executable
     *
     * @return true if there were changes comparing to discoverede results
     */
    private static boolean validateTestDiscoveryForFullDetection(MqmRestClient client, UFTTestDetectionResult detectionResult) {
        boolean hasDiff = false;
        Map<String, Entity> octaneTestsMap = getTestsFromServer(client, Long.parseLong(detectionResult.getWorkspaceId()), Long.parseLong(detectionResult.getScmRepositoryId()), null);

        List<AutomatedTest> discoveredTests = new ArrayList(detectionResult.getNewTests());
        detectionResult.getNewTests().clear();
        for (AutomatedTest discoveredTest : discoveredTests) {
            String key = createKey(discoveredTest.getPackage(), discoveredTest.getName());
            Entity octaneTest = octaneTestsMap.remove(key);

            if (octaneTest == null) {
                detectionResult.getNewTests().add(discoveredTest);
            } else {
                hasDiff = true;//if we get here - there is diff with discovered tests
                //the only fields that might be different is description and executable
                boolean octaneExecutable = octaneTest.getBooleanValue(OctaneConstants.Tests.EXECUTABLE_FIELD);
                String octaneDescription = octaneTest.getStringValue(OctaneConstants.Tests.DESCRIPTION_FIELD);
                boolean descriptionEquals = ((StringUtils.isEmpty(octaneDescription) || "null".equals(octaneDescription)) && discoveredTest.getDescription() == null) ||
                        octaneDescription.contains(discoveredTest.getDescription());
                boolean testsEqual = (octaneExecutable && descriptionEquals);
                if (!testsEqual) { //if equal - skip
                    discoveredTest.setId(octaneTest.getId());
                    detectionResult.getUpdatedTests().add(discoveredTest);
                }
            }
        }

        //go over executable tests that exist in Octane but not discovered and disable them
        for (Entity octaneTest : octaneTestsMap.values()) {
            hasDiff = true;//if some test exist - there is diff with discovered tests
            boolean octaneExecutable = octaneTest.getBooleanValue(OctaneConstants.Tests.EXECUTABLE_FIELD);
            if (octaneExecutable) {
                AutomatedTest test = new AutomatedTest();
                test.setId(octaneTest.getId());
                test.setExecutable(false);
                test.setName(octaneTest.getName());
                test.setPackage(octaneTest.getStringValue(OctaneConstants.Tests.PACKAGE_FIELD));
                detectionResult.getDeletedTests().add(test);
            }
        }

        return hasDiff;
    }

    /**
     * Go over discovered and octane data tables
     * 1.if DT doesn't exist on octane - this is new DT
     * 2. all DTs that are found in Octane but not discovered - delete those DTs from server
     */
    private static boolean validateDataTablesDiscoveryForFullDetection(MqmRestClient client, UFTTestDetectionResult detectionResult) {
        boolean hasDiff = false;
        List<ScmResourceFile> discoveredDataTables = new ArrayList(detectionResult.getNewScmResourceFiles());
        detectionResult.getNewScmResourceFiles().clear();

        Map<String, Entity> octaneDataTablesMap = getDataTablesFromServer(client, Long.parseLong(detectionResult.getWorkspaceId()), Long.parseLong(detectionResult.getScmRepositoryId()));
        for (ScmResourceFile dataTable : discoveredDataTables) {
            Entity octaneDataTable = octaneDataTablesMap.remove(dataTable.getRelativePath());
            if (octaneDataTable == null) {
                detectionResult.getNewScmResourceFiles().add(dataTable);
            } else {
                hasDiff = true;
            }
        }

        //go over DT that exist in Octane but not discovered
        for (Entity octaneDataTable : octaneDataTablesMap.values()) {
            hasDiff = true;
            ScmResourceFile dt = new ScmResourceFile();
            dt.setId(octaneDataTable.getId());
            dt.setName(octaneDataTable.getName());
            dt.setRelativePath(octaneDataTable.getStringValue(OctaneConstants.DataTables.RELATIVE_PATH_FIELD));
            detectionResult.getDeletedScmResourceFiles().add(dt);
        }

        return hasDiff;
    }

    private static Map<String, Entity> getTestsFromServer(MqmRestClient client, long workspaceId, long scmRepositoryId, Set<String> allTestNames) {
        List<String> conditions = new ArrayList<>();
        if (allTestNames != null && !allTestNames.isEmpty()) {
            String byNameCondition = QueryHelper.conditionIn(OctaneConstants.Tests.NAME_FIELD, allTestNames, false);
            int byNameConditionSizeThreshold = 3000;
            //Query string is part of UR, some servers limit request size by 4K,
            //Here we limit nameCondition by 3K, if it exceed, we will fetch all tests
            if (byNameCondition.length() < byNameConditionSizeThreshold) {
                conditions.add(byNameCondition);
            }
        }

        conditions.add(QueryHelper.conditionRef(OctaneConstants.Tests.SCM_REPOSITORY_FIELD, scmRepositoryId));
        Collection<String> fields = Arrays.asList(OctaneConstants.Tests.ID_FIELD, OctaneConstants.Tests.NAME_FIELD, OctaneConstants.Tests.PACKAGE_FIELD, OctaneConstants.Tests.EXECUTABLE_FIELD, OctaneConstants.Tests.DESCRIPTION_FIELD);
        List<Entity> octaneTests = client.getEntities(workspaceId, OctaneConstants.Tests.COLLECTION_NAME, conditions, fields);
        Map<String, Entity> octaneTestsMapByKey = new HashedMap();
        for (Entity octaneTest : octaneTests) {
            String key = createKey(octaneTest.getStringValue(OctaneConstants.Tests.PACKAGE_FIELD), octaneTest.getName());
            octaneTestsMapByKey.put(key, octaneTest);
        }
        return octaneTestsMapByKey;
    }

    private static Map<String, Entity> getDataTablesFromServer(MqmRestClient client, long workspaceId, long scmRepositoryId) {
        List<String> conditionByScmRepository = Arrays.asList(QueryHelper.conditionRef(OctaneConstants.DataTables.SCM_REPOSITORY_FIELD, scmRepositoryId));

        List<String> dataTablesFields = Arrays.asList(OctaneConstants.DataTables.ID_FIELD, OctaneConstants.DataTables.NAME_FIELD, OctaneConstants.DataTables.RELATIVE_PATH_FIELD);
        List<Entity> octaneDataTables = client.getEntities(workspaceId, OctaneConstants.DataTables.COLLECTION_NAME, conditionByScmRepository, dataTablesFields);

        Map<String, Entity> octaneDataTablesMap = new HashedMap();
        for (Entity dataTable : octaneDataTables) {
            octaneDataTablesMap.put(dataTable.getStringValue(OctaneConstants.DataTables.RELATIVE_PATH_FIELD), dataTable);
        }

        return octaneDataTablesMap;
    }

    private static String createKey(String... values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null || "null".equals(values[i])) {
                values[i] = "";
            }
        }
        return StringUtils.join(values, "#");
    }

    private static boolean postTests(MqmRestClient client, List<AutomatedTest> tests, String workspaceId, String scmRepositoryId) {

        if (!tests.isEmpty()) {
            try {
                completeTestProperties(client, Long.parseLong(workspaceId), tests, scmRepositoryId);
            } catch (RequestErrorException e) {
                logger.error("Failed to completeTestProperties : " + e.getMessage());
                return false;
            }

            for (int i = 0; i < tests.size(); i += POST_BULK_SIZE) {
                try {
                    AutomatedTests
                            data = AutomatedTests.createWithTests(tests.subList(i, Math.min(i + POST_BULK_SIZE, tests.size())));
                    String uftTestJson = convertToJsonString(data);
                    client.postEntities(Long.parseLong(workspaceId), OctaneConstants.Tests.COLLECTION_NAME, uftTestJson);
                } catch (RequestErrorException e) {
                    return checkIfExceptionCanBeIgnoredInPOST(e, "Failed to post tests");
                }
            }
        }
        return true;
    }

    private static boolean postScmResources(MqmRestClient client, List<ScmResourceFile> resources, String workspaceId, String scmResourceId) {

        if (!resources.isEmpty()) {
            try {
                completeScmResourceProperties(resources, scmResourceId);
            } catch (RequestErrorException e) {
                logger.error("Failed to completeScmResourceProperties : " + e.getMessage());
                return false;
            }

            for (int i = 0; i < resources.size(); i += POST_BULK_SIZE)
                try {
                    ScmResources data = ScmResources.createWithItems(resources.subList(i, Math.min(i + POST_BULK_SIZE, resources.size())));
                    String uftTestJson = convertToJsonString(data);
                    client.postEntities(Long.parseLong(workspaceId), OctaneConstants.DataTables.COLLECTION_NAME, uftTestJson);
                } catch (RequestErrorException e) {
                    return checkIfExceptionCanBeIgnoredInPOST(e, "Failed to post scm resource files");
                }
        }
        return true;
    }

    /**
     * Entities might be posted while they already exist in Octane, such POST request will fail with general error code will be 409.
     * The same error code might be received on other validation error.
     * In this method we check whether exist other exception than duplicate
     *
     * @param e
     * @param errorPrefix
     * @return
     */
    private static boolean checkIfExceptionCanBeIgnoredInPOST(RequestErrorException e, String errorPrefix) {
        if (e.getStatusCode() == HttpStatus.CONFLICT.getCode() && e.getJsonObject() != null && e.getJsonObject().containsKey("errors")) {
            JSONObject error = findFirstErrorDifferThan(e.getJsonObject().getJSONArray("errors"), DUPLICATE_ERROR_CODE);
            String errorMessage = null;
            if (error != null) {
                errorMessage = error.getString("description");
                logger.error(errorPrefix + " : " + errorMessage);
            }
            return errorMessage == null;
        }

        logger.error(errorPrefix + "  :  " + e.getMessage());
        return false;
    }

    /**
     * Search for error code that differ from supplied errorCode.
     */
    private static JSONObject findFirstErrorDifferThan(JSONArray errors, String excludeErrorCode) {
        for (int errorIndex = 0; errorIndex < errors.size(); errorIndex++) {
            JSONObject error = errors.getJSONObject(errorIndex);
            String errorCode = error.getString("error_code");
            if (errorCode.equals(excludeErrorCode)) {
                continue;
            } else {
                return error;
            }
        }
        return null;
    }

    private static String convertToJsonString(Object data) {
        JsonConfig config = getJsonConfig();
        return JSONObject.fromObject(data, config).toString();
    }

    private static JsonConfig getJsonConfig() {
        JsonConfig config = new JsonConfig();
        //override field names
        config.registerJsonPropertyNameProcessor(AutomatedTest.class, new PropertyNameProcessor() {

            @Override
            public String processPropertyName(Class className, String fieldName) {
                String result = fieldName;
                switch (fieldName) {
                    case "scmRepository":
                        result = OctaneConstants.Tests.SCM_REPOSITORY_FIELD;
                        break;
                    case "testingToolType":
                        result = OctaneConstants.Tests.TESTING_TOOL_TYPE_FIELD;
                        break;
                    case "testTypes":
                        result = OctaneConstants.Tests.TEST_TYPE_FIELD;
                        break;
                    default:
                        break;
                }
                return result;
            }
        });

        config.registerJsonPropertyNameProcessor(ScmResourceFile.class, new PropertyNameProcessor() {

            @Override
            public String processPropertyName(Class className, String fieldName) {
                String result = fieldName;
                switch (fieldName) {
                    case "relativePath":
                        result = OctaneConstants.DataTables.RELATIVE_PATH_FIELD;
                        break;
                    case "scmRepository":
                        result = OctaneConstants.DataTables.SCM_REPOSITORY_FIELD;
                        break;
                    default:
                        break;
                }
                return result;
            }
        });

        //filter empty fields
        PropertyFilter pf = new PropertyFilter() {
            public boolean apply(Object source, String name, Object value) {
                if (value != null) {
                    return false;
                }
                return true;
            }
        };
        config.setJsonPropertyFilter(pf);

        //skip fields
        config.registerPropertyExclusion(AutomatedTest.class, "uftTestType");
        return config;
    }

    private static boolean updateTests(MqmRestClient client, Collection<AutomatedTest> tests, String workspaceId) {

        try {
            //build  testsForUpdate
            List<AutomatedTest> testsForUpdate = new ArrayList<>();
            for (AutomatedTest test : tests) {
                AutomatedTest testForUpdate = new AutomatedTest();
                if (test.getDescription() != null) {
                    testForUpdate.setDescription(test.getDescription());
                }
                testForUpdate.setExecutable(test.getExecutable());
                testForUpdate.setId(test.getId());
                testsForUpdate.add(testForUpdate);
            }

            if (!testsForUpdate.isEmpty()) {
                for (int i = 0; i < tests.size(); i += POST_BULK_SIZE) {
                    AutomatedTests data = AutomatedTests.createWithTests(testsForUpdate.subList(i, Math.min(i + POST_BULK_SIZE, tests.size())));
                    String uftTestJson = convertToJsonString(data);
                    client.updateEntities(Long.parseLong(workspaceId), OctaneConstants.Tests.COLLECTION_NAME, uftTestJson);
                }
            }

            return true;
        } catch (Exception e) {
            logger.error("Failed to update tests : " + e.getMessage());
            return false;
        }
    }

    private static Entity fetchDataTableFromOctane(MqmRestClient client, long workspaceIdAsLong, long scmRepositoryId, ScmResourceFile scmResource) {
        List<String> conditions = new ArrayList<>();
        conditions.add(QueryHelper.condition(OctaneConstants.DataTables.RELATIVE_PATH_FIELD, scmResource.getRelativePath()));
        conditions.add(QueryHelper.conditionRef(OctaneConstants.DataTables.SCM_REPOSITORY_FIELD, scmRepositoryId));
        List<Entity> entities = client.getEntities(workspaceIdAsLong, OctaneConstants.DataTables.COLLECTION_NAME, conditions, Arrays.asList("id, name"));

        return entities.size() == 1 ? entities.get(0) : null;
    }

    private static boolean deleteScmResources(MqmRestClient client, List<ScmResourceFile> deletedResourceFiles, String workspaceId, String scmRepositoryId) {

        long workspaceIdAsLong = Long.parseLong(workspaceId);
        long scmRepositoryIdAsLong = Long.parseLong(scmRepositoryId);
        Set<Long> deletedIds = new HashSet<>();
        try {
            for (ScmResourceFile scmResource : deletedResourceFiles) {
                Entity found = fetchDataTableFromOctane(client, workspaceIdAsLong, scmRepositoryIdAsLong, scmResource);
                if (found != null) {
                    deletedIds.add(found.getId());
                }
            }

            client.deleteEntities(Long.parseLong(workspaceId), OctaneConstants.DataTables.COLLECTION_NAME, deletedIds);
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private static void completeTestProperties(MqmRestClient client, long workspaceId, Collection<AutomatedTest> tests, String scmRepositoryId) {
        ListNodeEntity uftTestingTool = getUftTestingTool(client, workspaceId);
        ListNodeEntity uftFramework = getUftFramework(client, workspaceId);
        ListNodeEntity guiTestType = hasTestsByType(tests, UftTestType.GUI) ? getGuiTestType(client, workspaceId) : null;
        ListNodeEntity apiTestType = hasTestsByType(tests, UftTestType.API) ? getApiTestType(client, workspaceId) : null;

        BaseRefEntity scmRepository = StringUtils.isEmpty(scmRepositoryId) ? null : BaseRefEntity.create(OctaneConstants.Tests.SCM_REPOSITORY_FIELD, scmRepositoryId);
        for (AutomatedTest test : tests) {
            test.setTestingToolType(uftTestingTool);
            test.setFramework(uftFramework);
            test.setScmRepository(scmRepository);

            ListNodeEntity testType = guiTestType;
            if (UftTestType.API.equals(test.getUftTestType())) {
                testType = apiTestType;
            }
            test.setTestTypes(ListNodeEntityCollection.create(testType));
        }
    }

    private static void completeScmResourceProperties(List<ScmResourceFile> resources, String scmResourceId) {
        BaseRefEntity scmRepository = StringUtils.isEmpty(scmResourceId) ? null : BaseRefEntity.create(OctaneConstants.DataTables.SCM_REPOSITORY_FIELD, scmResourceId);
        for (ScmResourceFile resource : resources) {
            resource.setScmRepository(scmRepository);
        }
    }

    private static ListNodeEntity getUftTestingTool(MqmRestClient client, long workspaceId) {
        PagedList<ListItem> testingTools = client.queryListItems("list_node.testing_tool_type", null, workspaceId, 0, POST_BULK_SIZE);
        String uftTestingToolLogicalName = "list_node.testing_tool_type.uft";

        for (ListItem item : testingTools.getItems()) {
            if (uftTestingToolLogicalName.equals(item.getLogicalName())) {
                return ListNodeEntity.create(item.getId());
            }
        }
        return null;
    }

    private static ListNodeEntity getUftFramework(MqmRestClient client, long workspaceId) {
        PagedList<ListItem> testingTools = client.queryListItems("list_node.je.framework", null, workspaceId, 0, POST_BULK_SIZE);
        String uftTestingToolLogicalName = "list_node.je.framework.uft";

        for (ListItem item : testingTools.getItems()) {
            if (uftTestingToolLogicalName.equals(item.getLogicalName())) {
                return ListNodeEntity.create(item.getId());
            }
        }
        return null;
    }

    private static ListNodeEntity getGuiTestType(MqmRestClient client, long workspaceId) {
        PagedList<ListItem> testingTools = client.queryListItems("list_node.test_type", null, workspaceId, 0, POST_BULK_SIZE);
        String guiLogicalName = "list_node.test_type.gui";

        for (ListItem item : testingTools.getItems()) {
            if (guiLogicalName.equals(item.getLogicalName())) {
                return ListNodeEntity.create(item.getId());
            }
        }
        return null;
    }

    private static ListNodeEntity getApiTestType(MqmRestClient client, long workspaceId) {
        PagedList<ListItem> testingTools = client.queryListItems("list_node.test_type", null, workspaceId, 0, POST_BULK_SIZE);
        String guiLogicalName = "list_node.test_type.api";

        for (ListItem item : testingTools.getItems()) {
            if (guiLogicalName.equals(item.getLogicalName())) {
                return ListNodeEntity.create(item.getId());
            }
        }
        return null;
    }

    @Override
    public long getRecurrencePeriod() {
        String value = System.getProperty("UftTestDiscoveryDispatcher.Period"); // let's us config the recurrence period. default is 60 seconds.
        if (!StringUtils.isEmpty(value)) {
            return Long.valueOf(value);
        }
        return TimeUnit2.SECONDS.toMillis(30);
    }

    @Inject
    public void setTestResultQueue(UftTestDiscoveryQueue queue) {
        this.queue = queue;
    }

    private static boolean hasTestsByType(Collection<AutomatedTest> tests, UftTestType uftTestType) {
        for (AutomatedTest test : tests) {
            if (uftTestType.equals(test.getUftTestType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Queue that current run contains discovered tests
     *
     * @param projectName
     * @param buildNumber
     */
    public void enqueueResult(String projectName, int buildNumber) {
        queue.add(projectName, buildNumber);
    }
}
