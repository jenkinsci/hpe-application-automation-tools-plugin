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
import com.hp.mqm.client.MqmRestClient;
import com.hp.mqm.client.QueryHelper;
import com.hp.mqm.client.exception.RequestErrorException;
import com.hp.mqm.client.model.Entity;
import com.hp.mqm.client.model.ListItem;
import com.hp.mqm.client.model.PagedList;
import com.hpe.application.automation.tools.common.HttpStatus;
import com.hpe.application.automation.tools.octane.ResultQueue;
import com.hpe.application.automation.tools.octane.actions.UftTestType;
import com.hpe.application.automation.tools.octane.actions.dto.*;
import com.hpe.application.automation.tools.octane.configuration.ConfigurationListener;
import com.hpe.application.automation.tools.octane.configuration.ConfigurationService;
import com.hpe.application.automation.tools.octane.configuration.ServerConfiguration;
import com.hpe.application.automation.tools.octane.tests.AbstractSafeLoggingAsyncPeriodWork;
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
public class UftTestDiscoveryDispatcher extends AbstractSafeLoggingAsyncPeriodWork implements ConfigurationListener {

    private final static Logger logger = LogManager.getLogger(UftTestDiscoveryDispatcher.class);
    private final static String DUPLICATE_ERROR_CODE = "platform.duplicate_entity_error";
    private final static int POST_BULK_SIZE = 100;
    private final static int MAX_DISPATCH_TRIALS = 5;
    private final static int QUERY_CONDITION_SIZE_THRESHOLD = 3000;
    private static final String OCTANE_VERSION_SUPPORTING_TEST_RENAME = "12.60.3";
    private static String OCTANE_VERSION = null;

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
            if (isOctaneSupportTestRename(client)) {
                handleMovedTests(result);
                handleMovedDataTables(result);
            }

            validateTestDiscoveryAndCompleteTestIdsForScmChangeDetection(client, result);
            validateTestDiscoveryAndCompleteDataTableIdsForScmChangeDetection(client, result);
            //no need to add validation for dataTables, because there is no DTs update and there is no special delete strategy
        }
        removeItemsWithStatusNone(result.getAllTests());
        removeItemsWithStatusNone(result.getAllScmResourceFiles());

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
        List<AutomatedTest> tests = result.getNewTests();
        if (!tests.isEmpty()) {
            boolean posted = postTests(client, tests, result.getWorkspaceId(), result.getScmRepositoryId());
            logger.warn("Persistence [" + item.getProjectName() + "#" + item.getBuildNumber() + "] : " + tests + "  new tests posted successfully = " + posted);
        }

        //post test updated
        tests = result.getUpdatedTests();
        if (!tests.isEmpty()) {
            boolean updated = updateTests(client, tests, result.getWorkspaceId());
            logger.warn("Persistence [" + item.getProjectName() + "#" + item.getBuildNumber() + "] : " + tests.size() + "  updated tests posted successfully = " + updated);
        }

        //post test deleted
        tests = result.getDeletedTests();
        if (!tests.isEmpty()) {
            boolean updated = updateTests(client, tests, result.getWorkspaceId());
            logger.warn("Persistence [" + item.getProjectName() + "#" + item.getBuildNumber() + "] : " + tests.size() + "  deleted tests set as not executable successfully = " + updated);
        }

        //post scm resources
        List<ScmResourceFile> resources = result.getNewScmResourceFiles();
        if (!resources.isEmpty()) {
            boolean posted = postScmResources(client, resources, result.getWorkspaceId(), result.getScmRepositoryId());
            logger.warn("Persistence [" + item.getProjectName() + "#" + item.getBuildNumber() + "] : " + resources.size() + "  new scmResources posted successfully = " + posted);
        }

        //update scm resources
        resources = result.getUpdatedScmResourceFiles();
        if (!resources.isEmpty()) {
            boolean posted = updateScmResources(client, resources, result.getWorkspaceId());
            logger.warn("Persistence [" + item.getProjectName() + "#" + item.getBuildNumber() + "] : " + resources.size() + "  updated scmResources posted successfully = " + posted);
        }

        //delete scm resources
        resources = result.getDeletedScmResourceFiles();
        if (!resources.isEmpty()) {
            boolean posted = deleteScmResources(client, resources, result.getWorkspaceId(), result.getScmRepositoryId());
            logger.warn("Persistence [" + item.getProjectName() + "#" + item.getBuildNumber() + "] : " + resources.size() + "  scmResources deleted successfully = " + posted);
        }
    }

    private static void removeItemsWithStatusNone(List<? extends SupportsOctaneStatus> list) {
        for (int i = list.size(); i > 0; i--) {
            if (list.get(i - 1).getOctaneStatus().equals(OctaneStatus.NONE)) {
                list.remove(i - 1);
            }
        }
    }

    private static boolean validateTestDiscoveryAndCompleteDataTableIdsForScmChangeDetection(MqmRestClient client, UFTTestDetectionResult result) {
        boolean hasDiff = false;
        Set<String> allNames = new HashSet<>();
        for (ScmResourceFile file : result.getAllScmResourceFiles()) {
            if (file.getIsMoved()) {
                allNames.add(file.getOldName());
            } else {
                allNames.add(file.getName());
            }
        }

        //GET DataTables FROM OCTANE
        Map<String, Entity> octaneEntityMapByRelativePath = getDataTablesFromServer(client, Long.parseLong(result.getWorkspaceId()), Long.parseLong(result.getScmRepositoryId()), allNames);


        //MATCHING
        for (ScmResourceFile file : result.getAllScmResourceFiles()) {

            String key = file.getIsMoved() ? file.getOldRelativePath() : file.getRelativePath();
            Entity octaneFile = octaneEntityMapByRelativePath.get(key);

            boolean octaneFileFound = (octaneFile != null);
            if (octaneFileFound) {
                file.setId(octaneFile.getId());
            }

            switch (file.getOctaneStatus()) {
                case DELETED:
                    if (!octaneFileFound) {
                        //file that is marked to be deleted - doesn't exist in Octane - do nothing
                        hasDiff = true;
                        file.setOctaneStatus(OctaneStatus.NONE);
                    }
                    break;
                case MODIFIED:
                    if (!octaneFileFound) {
                        //updated file that has no matching in Octane, possibly was remove from Octane. So we move it to new
                        hasDiff = true;
                        file.setOctaneStatus(OctaneStatus.NEW);
                    }
                    break;
                case NEW:
                    if (octaneFileFound) {
                        //new file was found in Octane - do nothing(there is nothing to update)
                        hasDiff = true;
                        file.setOctaneStatus(OctaneStatus.NONE);
                    }
                    break;
                default:
                    //do nothing
            }
        }

        return hasDiff;
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

        Set<String> allTestNames = new HashSet<>();
        for (AutomatedTest test : result.getAllTests()) {
            if (test.getIsMoved()) {
                allTestNames.add(test.getOldName());
            } else {
                allTestNames.add(test.getName());
            }
        }

        //GET TESTS FROM OCTANE
        Map<String, Entity> octaneTestsMapByKey = getTestsFromServer(client, Long.parseLong(result.getWorkspaceId()), Long.parseLong(result.getScmRepositoryId()), allTestNames);


        //MATCHING
        for (AutomatedTest discoveredTest : result.getAllTests()) {
            String key = discoveredTest.getIsMoved() ? createKey(discoveredTest.getOldPackage(), discoveredTest.getOldName()) : createKey(discoveredTest.getPackage(), discoveredTest.getName());
            Entity octaneTest = octaneTestsMapByKey.get(key);
            boolean octaneTestFound = (octaneTest != null);
            if (octaneTestFound) {
                discoveredTest.setId(octaneTest.getId());
            }
            switch (discoveredTest.getOctaneStatus()) {
                case DELETED:
                    if (!octaneTestFound) {
                        //discoveredTest that is marked to be deleted - doesn't exist in Octane - do nothing
                        hasDiff = true;
                        discoveredTest.setOctaneStatus(OctaneStatus.NONE);
                    }
                    break;
                case MODIFIED:
                    if (!octaneTestFound) {
                        //updated discoveredTest that has no matching in Octane, possibly was remove from Octane. So we move it to new tests
                        hasDiff = true;
                        discoveredTest.setOctaneStatus(OctaneStatus.NEW);
                    } else {
                        boolean testsEqual = checkTestEquals(discoveredTest, octaneTest);
                        if (testsEqual) { //if equal - skip
                            discoveredTest.setOctaneStatus(OctaneStatus.NONE);
                        }
                    }
                    break;
                case NEW:
                    if (octaneTestFound) {
                        //new discoveredTest was found in Octane - move it to update
                        hasDiff = true;
                        discoveredTest.setOctaneStatus(OctaneStatus.MODIFIED);
                    }
                    break;
                default:
                    //do nothing
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
     * @return true if there were changes comparing to discovered results
     */
    private static boolean validateTestDiscoveryForFullDetection(MqmRestClient client, UFTTestDetectionResult detectionResult) {
        boolean hasDiff = false;
        Map<String, Entity> octaneTestsMap = getTestsFromServer(client, Long.parseLong(detectionResult.getWorkspaceId()), Long.parseLong(detectionResult.getScmRepositoryId()), null);

        for (AutomatedTest discoveredTest : detectionResult.getAllTests()) {
            String key = createKey(discoveredTest.getPackage(), discoveredTest.getName());
            Entity octaneTest = octaneTestsMap.remove(key);

            if (octaneTest == null) {
                //do nothing, status of test should remain NEW
            } else {
                hasDiff = true;//if we get here - there is diff with discovered tests
                //the only fields that might be different is description and executable
                boolean testsEqual = checkTestEquals(discoveredTest, octaneTest);
                if (!testsEqual) { //if equal - skip
                    discoveredTest.setId(octaneTest.getId());
                    discoveredTest.setOctaneStatus(OctaneStatus.MODIFIED);
                } else {
                    discoveredTest.setOctaneStatus(OctaneStatus.NONE);
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
                test.setOctaneStatus(OctaneStatus.DELETED);
            }
        }

        return hasDiff;
    }

    private static boolean checkTestEquals(AutomatedTest discoveredTest, Entity octaneTest) {
        boolean octaneExecutable = octaneTest.getBooleanValue(OctaneConstants.Tests.EXECUTABLE_FIELD);
        String octaneDescription = octaneTest.getStringValue(OctaneConstants.Tests.DESCRIPTION_FIELD);
        boolean descriptionEquals = ((StringUtils.isEmpty(octaneDescription) || "null".equals(octaneDescription)) && discoveredTest.getDescription() == null) ||
                octaneDescription.contains(discoveredTest.getDescription());
        boolean testsEqual = (octaneExecutable && descriptionEquals && !discoveredTest.getIsMoved());
        return testsEqual;
    }

    /**
     * Go over discovered and octane data tables
     * 1.if DT doesn't exist on octane - this is new DT
     * 2. all DTs that are found in Octane but not discovered - delete those DTs from server
     */
    private static boolean validateDataTablesDiscoveryForFullDetection(MqmRestClient client, UFTTestDetectionResult detectionResult) {
        boolean hasDiff = false;


        Map<String, Entity> octaneDataTablesMap = getDataTablesFromServer(client, Long.parseLong(detectionResult.getWorkspaceId()), Long.parseLong(detectionResult.getScmRepositoryId()), null);
        for (ScmResourceFile dataTable : detectionResult.getAllScmResourceFiles()) {
            Entity octaneDataTable = octaneDataTablesMap.remove(dataTable.getRelativePath());
            if (octaneDataTable != null) {//found in Octnat - skip
                dataTable.setOctaneStatus(OctaneStatus.NONE);
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
            dt.setOctaneStatus(OctaneStatus.DELETED);
            detectionResult.getAllScmResourceFiles().add(dt);
        }

        return hasDiff;
    }

    private static Map<String, Entity> getTestsFromServer(MqmRestClient client, long workspaceId, long scmRepositoryId, Set<String> allTestNames) {
        List<String> conditions = new ArrayList<>();
        if (allTestNames != null && !allTestNames.isEmpty()) {
            String byNameCondition = QueryHelper.conditionIn(OctaneConstants.Tests.NAME_FIELD, allTestNames, false);
            //Query string is part of UR, some servers limit request size by 4K,
            //Here we limit nameCondition by 3K, if it exceed, we will fetch all tests
            if (byNameCondition.length() < QUERY_CONDITION_SIZE_THRESHOLD) {
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

    private static Map<String, Entity> getDataTablesFromServer(MqmRestClient client, long workspaceId, long scmRepositoryId, Set<String> allNames) {
        List<String> conditions = new ArrayList<>();
        if (allNames != null && !allNames.isEmpty()) {
            String byPathCondition = QueryHelper.conditionIn(OctaneConstants.DataTables.NAME_FIELD, allNames, false);

            //Query string is part of UR, some servers limit request size by 4K,
            //Here we limit nameCondition by 3K, if it exceed, we will fetch all
            if (byPathCondition.length() < QUERY_CONDITION_SIZE_THRESHOLD) {
                conditions.add(byPathCondition);
            }
        }

        String conditionByScmRepository = QueryHelper.conditionRef(OctaneConstants.DataTables.SCM_REPOSITORY_FIELD, scmRepositoryId);
        conditions.add(conditionByScmRepository);

        List<String> dataTablesFields = Arrays.asList(OctaneConstants.DataTables.ID_FIELD, OctaneConstants.DataTables.NAME_FIELD, OctaneConstants.DataTables.RELATIVE_PATH_FIELD);
        List<Entity> octaneDataTables = client.getEntities(workspaceId, OctaneConstants.DataTables.COLLECTION_NAME, conditions, dataTablesFields);

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
                    AutomatedTests data = AutomatedTests.createWithTests(tests.subList(i, Math.min(i + POST_BULK_SIZE, tests.size())));
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
                    String json = convertToJsonString(data);
                    client.postEntities(Long.parseLong(workspaceId), OctaneConstants.DataTables.COLLECTION_NAME, json);
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
        config.registerPropertyExclusions(AutomatedTest.class, new String[]{"uftTestType", "changeSetSrc", "changeSetDst", "oldName", "oldPackage", "isMoved", "octaneStatus"});
        config.registerPropertyExclusions(ScmResourceFile.class, new String[]{"changeSetSrc", "changeSetDst", "oldName", "oldRelativePath", "isMoved", "octaneStatus"});
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

                if (test.getIsMoved()) {
                    testForUpdate.setName(test.getName());
                    testForUpdate.setPackage(test.getPackage());
                }
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

    private static Entity fetchDataTableFromOctane(MqmRestClient client, long workspaceIdAsLong, long scmRepositoryId, String relativePath) {
        List<String> conditions = new ArrayList<>();
        conditions.add(QueryHelper.condition(OctaneConstants.DataTables.RELATIVE_PATH_FIELD, relativePath));
        conditions.add(QueryHelper.conditionRef(OctaneConstants.DataTables.SCM_REPOSITORY_FIELD, scmRepositoryId));
        List<Entity> entities = client.getEntities(workspaceIdAsLong, OctaneConstants.DataTables.COLLECTION_NAME, conditions, Arrays.asList("id, name"));

        return entities.size() == 1 ? entities.get(0) : null;
    }

    private static boolean updateScmResources(MqmRestClient client, List<ScmResourceFile> updatedResourceFiles, String workspaceId) {
        try {

            if (!updatedResourceFiles.isEmpty()) {
                for (int i = 0; i < updatedResourceFiles.size(); i += POST_BULK_SIZE) {
                    ScmResources data = ScmResources.createWithItems(updatedResourceFiles.subList(i, Math.min(i + POST_BULK_SIZE, updatedResourceFiles.size())));
                    String json = convertToJsonString(data);
                    client.updateEntities(Long.parseLong(workspaceId), OctaneConstants.DataTables.COLLECTION_NAME, json);
                }
            }

            return true;
        } catch (Exception e) {
            logger.error("Failed to update data tables : " + e.getMessage());
            return false;
        }
    }

    private static boolean deleteScmResources(MqmRestClient client, List<ScmResourceFile> deletedResourceFiles, String workspaceId, String scmRepositoryId) {

        long workspaceIdAsLong = Long.parseLong(workspaceId);
        long scmRepositoryIdAsLong = Long.parseLong(scmRepositoryId);
        Set<Long> deletedIds = new HashSet<>();
        try {
            for (ScmResourceFile scmResource : deletedResourceFiles) {
                Entity found = fetchDataTableFromOctane(client, workspaceIdAsLong, scmRepositoryIdAsLong, scmResource.getRelativePath());
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


    private static void handleMovedTests(UFTTestDetectionResult result) {
        List<AutomatedTest> newTests = result.getNewTests();
        List<AutomatedTest> deletedTests = result.getDeletedTests();
        if (!newTests.isEmpty() && !deletedTests.isEmpty()) {
            Map<String, AutomatedTest> dst2Test = new HashMap<>();
            Map<AutomatedTest, AutomatedTest> deleted2newMovedTests = new HashMap<>();
            for (AutomatedTest newTest : newTests) {
                if (StringUtils.isNotEmpty(newTest.getChangeSetDst())) {
                    dst2Test.put(newTest.getChangeSetDst(), newTest);
                }
            }
            for (AutomatedTest deletedTest : deletedTests) {
                if (StringUtils.isNotEmpty(deletedTest.getChangeSetDst()) && dst2Test.containsKey(deletedTest.getChangeSetDst())) {
                    AutomatedTest newTest = dst2Test.get(deletedTest.getChangeSetDst());
                    deleted2newMovedTests.put(deletedTest, newTest);
                }
            }

            for (Map.Entry<AutomatedTest, AutomatedTest> entry : deleted2newMovedTests.entrySet()) {
                AutomatedTest deletedTest = entry.getKey();
                AutomatedTest newTest = entry.getValue();

                newTest.setIsMoved(true);
                newTest.setOldName(deletedTest.getName());
                newTest.setOldPackage(deletedTest.getPackage());
                newTest.setOctaneStatus(OctaneStatus.MODIFIED);

                result.getAllTests().remove(deletedTest);
            }
        }
    }

    private static void handleMovedDataTables(UFTTestDetectionResult result) {
        List<ScmResourceFile> newItems = result.getNewScmResourceFiles();
        List<ScmResourceFile> deletedItems = result.getDeletedScmResourceFiles();
        if (!newItems.isEmpty() && !deletedItems.isEmpty()) {
            Map<String, ScmResourceFile> dst2File = new HashMap<>();
            Map<ScmResourceFile, ScmResourceFile> deleted2newMovedFiles = new HashMap<>();
            for (ScmResourceFile newFile : newItems) {
                if (StringUtils.isNotEmpty(newFile.getChangeSetDst())) {
                    dst2File.put(newFile.getChangeSetDst(), newFile);
                }
            }
            for (ScmResourceFile deletedFile : deletedItems) {
                if (StringUtils.isNotEmpty(deletedFile.getChangeSetDst()) && dst2File.containsKey(deletedFile.getChangeSetDst())) {
                    ScmResourceFile newFile = dst2File.get(deletedFile.getChangeSetDst());
                    deleted2newMovedFiles.put(deletedFile, newFile);
                }
            }

            for (Map.Entry<ScmResourceFile, ScmResourceFile> entry : deleted2newMovedFiles.entrySet()) {
                ScmResourceFile deletedFile = entry.getKey();
                ScmResourceFile newFile = entry.getValue();

                newFile.setIsMoved(true);
                newFile.setOldName(deletedFile.getName());
                newFile.setOldRelativePath(deletedFile.getRelativePath());
                newFile.setOctaneStatus(OctaneStatus.MODIFIED);

                result.getAllScmResourceFiles().remove(deletedFile);
            }
        }
    }


    private static boolean isOctaneSupportTestRename(MqmRestClient client) {
        String octane_version = getOctaneVersion(client);
        boolean supportTestRename = (octane_version != null && versionCompare(OCTANE_VERSION_SUPPORTING_TEST_RENAME, octane_version) <= 0);
        logger.warn("Support test rename = " + supportTestRename);
        return supportTestRename;
    }

    private static String getOctaneVersion(MqmRestClient client) {

        if (OCTANE_VERSION == null) {
            List<Entity> entities = client.getEntities(null, "server_version", null, null);
            if (entities.size() == 1) {
                Entity entity = entities.get(0);
                OCTANE_VERSION = entity.getStringValue("version");
                logger.warn("Received Octane version - " + OCTANE_VERSION);

            } else {
                logger.error(String.format("Request for Octane version returned %s items. return version is not defined.", entities.size()));
            }
        }

        return OCTANE_VERSION;
    }

    @Override
    public void onChanged(ServerConfiguration conf, ServerConfiguration oldConf) {
        OCTANE_VERSION = null;
    }

    /**
     * Compares two version strings.
     * <p>
     * Use this instead of String.compareTo() for a non-lexicographical
     * comparison that works for version strings. e.g. "1.10".compareTo("1.6").
     *
     * @param str1 a string of ordinal numbers separated by decimal points.
     * @param str2 a string of ordinal numbers separated by decimal points.
     * @return The result is a negative integer if str1 is _numerically_ less than str2.
     * The result is a positive integer if str1 is _numerically_ greater than str2.
     * The result is zero if the strings are _numerically_ equal.
     * @note It does not work if "1.10" is supposed to be equal to "1.10.0".
     */
    private static Integer versionCompare(String str1, String str2) {
        String[] vals1 = str1.split("\\.");
        String[] vals2 = str2.split("\\.");
        int i = 0;
        // set index to first non-equal ordinal or length of shortest version string
        while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
            i++;
        }
        // compare first non-equal ordinal number
        if (i < vals1.length && i < vals2.length) {
            int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
            return Integer.signum(diff);
        }
        // the strings are equal or one string is a substring of the other
        // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
        else {
            return Integer.signum(vals1.length - vals2.length);
        }
    }


}
