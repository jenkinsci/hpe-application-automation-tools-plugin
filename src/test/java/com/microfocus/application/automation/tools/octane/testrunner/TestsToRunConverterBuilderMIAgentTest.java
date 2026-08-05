/*
 *  Certain versions of software accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 *  This software was acquired by Micro Focus on September 1, 2017, and is now
 *  offered by OpenText.
 *  Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical
 *  in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the
 *  property of their respective owners.
 *  OpenText is a trademark of Open Text.
 *  __________________________________________________________________
 *  MIT License
 *
 *  Copyright 2012-2026 Open Text.
 *
 *  The only warranties for products and services of Open Text and
 *  its affiliates and licensors ("Open Text") are as may be set forth
 *  in the express warranty statements accompanying such products and services.
 *  Nothing herein should be construed as constituting an additional warranty.
 *  Open Text shall not be liable for technical or editorial errors or
 *  omissions contained herein. The information contained herein is subject
 *  to change without notice.
 *
 *  Except as specifically indicated otherwise, this document contains
 *  confidential information and a valid license is required for possession,
 *  use or copying. If this work is provided to the U.S. Government,
 *  consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 *  Computer Software Documentation, and Technical Data for Commercial Items are
 *  licensed to the U.S. Government under vendor's standard commercial license.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ___________________________________________________________________
 */
package com.microfocus.application.automation.tools.octane.testrunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.octane.integrations.OctaneClient;
import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.services.configuration.ConfigurationService;
import com.hp.octane.integrations.services.rest.OctaneRestClient;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.utils.SdkConstants;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;

public class TestsToRunConverterBuilderMIAgentTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String METADATA_WITH_AU = "{\"data\":[{\"name\":\"au_tester_configuration\",\"entity_name\":\"run\"}]}";

    private static final String RUNS_RESPONSE = "{\"data\":[{" +
            "\"type\":\"run\"," +
            "\"subtype\":\"run_manual\"," +
            "\"order_in_suite_run\":1," +
            "\"duration\":null," +
            "\"name\":\"tsMIAgent\"," +
            "\"workspace_id\":2001," +
            "\"id\":\"1042\"," +
            "\"au_tester_configuration\":{\"browser\":{\"BROWSER_NAME\":\"Google Chrome\",\"BROWSER_LOCALE\":\"en-US\"},\"agent\":{\"MAX_FAILURES\":3,\"MAX_NUMBER_OF_STEPS\":100,\"BROWSER_USE_RUN_TIMEOUT\":2700}}," +
            "\"test_name\":\"Login flow\"," +
            "\"has_attachments\":false," +
            "\"parent_suite\":{\"type\":\"run_suite\",\"id\":\"1041\",\"name\":\"tsMIAgent\"}," +
            "\"run_steps\":{\"total_count\":1,\"data\":[{" +
            "\"type\":\"run_step\",\"id\":\"s1\",\"result\":null,\"attachments\":{\"total_count\":0,\"data\":[]},\"index_in_script\":0,\"index_in_report\":\"1\",\"from_call_to_test\":false,\"activity_level\":0,\"actual\":null,\"description\":\"Open login page\",\"step_type\":{\"type\":\"list_node\",\"id\":\"list_node.manual_test_run_step_type.normal\",\"activity_level\":0,\"logical_name\":\"list_node.manual_test_run_step_type.normal\",\"index\":1,\"name\":\"Normal\"},\"run\":{\"type\":\"run_manual\",\"id\":\"1042\",\"name\":\"tsMIAgent\",\"activity_level\":0}}]}," +
            "\"test\":{\"type\":\"test_manual\",\"id\":\"1001\",\"name\":\"Login flow\",\"activity_level\":0}," +
            "\"native_status\":{\"type\":\"list_node\",\"id\":\"list_node.run_native_status.not_completed\",\"activity_level\":0,\"logical_name\":\"list_node.run_native_status.not_completed\",\"index\":4,\"name\":\"In Progress\"}," +
            "\"run_by\":{\"type\":\"workspace_user\",\"id\":\"1001\",\"workspace_id\":2001,\"full_name\":\"sa@nga\",\"activity_level\":0}},{" +
            "\"type\":\"run\"," +
            "\"subtype\":\"run_manual\"," +
            "\"order_in_suite_run\":2," +
            "\"duration\":null," +
            "\"name\":\"tsMIAgent\"," +
            "\"workspace_id\":2001," +
            "\"id\":\"1043\"," +
            "\"au_tester_configuration\":{\"browser\":{\"BROWSER_NAME\":\"Google Chrome\",\"BROWSER_LOCALE\":\"en-US\"},\"agent\":{\"MAX_FAILURES\":3,\"MAX_NUMBER_OF_STEPS\":100,\"BROWSER_USE_RUN_TIMEOUT\":2700}}," +
            "\"test_name\":\"Checkout flow\"," +
            "\"has_attachments\":false," +
            "\"parent_suite\":{\"type\":\"run_suite\",\"id\":\"1041\",\"name\":\"tsMIAgent\"}," +
            "\"run_steps\":{\"total_count\":2,\"data\":[{" +
            "\"type\":\"run_step\",\"id\":\"s2\",\"result\":null,\"attachments\":{\"total_count\":0,\"data\":[]},\"index_in_script\":0,\"index_in_report\":\"1\",\"from_call_to_test\":false,\"activity_level\":0,\"actual\":null,\"description\":\"Add item\",\"step_type\":{\"type\":\"list_node\",\"id\":\"list_node.manual_test_run_step_type.normal\",\"activity_level\":0,\"logical_name\":\"list_node.manual_test_run_step_type.normal\",\"index\":1,\"name\":\"Normal\"},\"run\":{\"type\":\"run_manual\",\"id\":\"1043\",\"name\":\"tsMIAgent\",\"activity_level\":0}} ,{" +
            "\"type\":\"run_step\",\"id\":\"s3\",\"result\":null,\"attachments\":{\"total_count\":0,\"data\":[]},\"index_in_script\":1,\"index_in_report\":\"2\",\"from_call_to_test\":false,\"activity_level\":0,\"actual\":null,\"description\":\"Verify total\",\"step_type\":{\"type\":\"list_node\",\"id\":\"list_node.manual_test_run_step_type.validate\",\"activity_level\":0,\"logical_name\":\"list_node.manual_test_run_step_type.validate\",\"index\":0,\"name\":\"Validate\"},\"run\":{\"type\":\"run_manual\",\"id\":\"1043\",\"name\":\"tsMIAgent\",\"activity_level\":0}}]}," +
            "\"test\":{\"type\":\"test_manual\",\"id\":\"1002\",\"name\":\"Checkout flow\",\"activity_level\":0}," +
            "\"native_status\":{\"type\":\"list_node\",\"id\":\"list_node.run_native_status.not_completed\",\"activity_level\":0,\"logical_name\":\"list_node.run_native_status.not_completed\",\"index\":4,\"name\":\"In Progress\"}," +
            "\"run_by\":{\"type\":\"workspace_user\",\"id\":\"1001\",\"workspace_id\":2001,\"full_name\":\"sa@nga\",\"activity_level\":0}}],\"total_count\":2}";

    // v1 string format: package|class|testName|key=value
    private static final String RAW_TESTS = "v1:|Login flow|Login flow|runId=1042;|Checkout flow|Checkout flow|runId=1043";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private OctaneClient mockClient;
    private OctaneRestClient mockRestClient;
    private RestService mockRestService;
    private ConfigurationService mockConfigService;
    private OctaneConfiguration realConfig;
    private Map<OctaneConfiguration, OctaneClient> sdkClientsMap;

    @Before
    public void setUp() throws Exception {
        mockClient = mock(OctaneClient.class);
        mockRestClient = mock(OctaneRestClient.class);
        mockRestService = mock(RestService.class);
        mockConfigService = mock(ConfigurationService.class);

        Field f = OctaneSDK.class.getDeclaredField("clients");
        f.setAccessible(true);
        sdkClientsMap = (Map<OctaneConfiguration, OctaneClient>) f.get(null);
    }

    @After
    public void tearDown() {
        if (realConfig != null) {
            sdkClientsMap.remove(realConfig);
        }
    }

    @Test
    public void perform_miAgent_enrichesAndConverts() throws Exception {
        registerOctaneMock();

        Run build = mockBuild(RAW_TESTS);
        TaskListener listener = mockListener();

        TestsToRunConverterBuilder builder = new TestsToRunConverterBuilder("mi_agent", "");
        builder.perform(build, new FilePath(tempFolder.getRoot()), mock(Launcher.class), listener);

        // capture the action added to the build — it carries the converted JSON
        ArgumentCaptor<Action> captor = ArgumentCaptor.forClass(Action.class);
        verify(build).addAction(captor.capture());

        VariableInjectionAction action = (VariableInjectionAction) captor.getValue();
        assertNotNull(action);

        String converted = extractVariable(action, "testsToRunConverted");
        assertNotNull("testsToRunConverted should be set", converted);

        JsonNode manifest = MAPPER.readTree(converted);
        assertEquals(2, manifest.path("total_count").asInt());

        JsonNode runs = manifest.path("data");
        assertTrue(runs.isArray());
        assertEquals(2, runs.size());

        assertEquals("1042", runs.get(0).path("id").asText());
        assertEquals("run_manual", runs.get(0).path("subtype").asText());
        assertEquals("Login flow", runs.get(0).path("test_name").asText());
        assertEquals("Google Chrome", runs.get(0).path("au_tester_configuration").path("browser").path("BROWSER_NAME").asText());
        assertEquals("list_node.run_native_status.not_completed", runs.get(0).path("native_status").path("id").asText());
        assertEquals("s1", runs.get(0).path("run_steps").path("data").get(0).path("id").asText());

        assertEquals("1043", runs.get(1).path("id").asText());
        assertEquals("Checkout flow", runs.get(1).path("test").path("name").asText());
        assertEquals(2, runs.get(1).path("run_steps").path("data").size());
        assertEquals("list_node.manual_test_run_step_type.validate", runs.get(1).path("run_steps").path("data").get(1).path("step_type").path("id").asText());
    }

    @Test
    public void perform_miAgent_missingRunId_fails() throws Exception {
        registerOctaneMock();

        String rawTestsMissingRunId = "v1:|Bad test|Bad test";
        Run build = mockBuild(rawTestsMissingRunId);
        TaskListener listener = mockListener();

        TestsToRunConverterBuilder builder = new TestsToRunConverterBuilder("mi_agent", "");
        builder.perform(build, new FilePath(tempFolder.getRoot()), mock(Launcher.class), listener);

        verify(build).setResult(Result.FAILURE);
        verify(build, never()).addAction(any());
    }

    @Test
    public void perform_miAgent_emptyTests_skips() throws Exception {
        Run build = mockBuild("");
        TaskListener listener = mockListener();

        TestsToRunConverterBuilder builder = new TestsToRunConverterBuilder("mi_agent", "");
        builder.perform(build, new FilePath(tempFolder.getRoot()), mock(Launcher.class), listener);

        verify(build, never()).addAction(any());
        verify(build, never()).setResult(any());
    }

    @Test
    public void perform_miAgent_jsonFormat_enrichesAndConverts() throws Exception {
        registerOctaneMock();

        String jsonTests = "{\"testsToRun\":[" +
            "{\"testName\":\"Login flow\",\"packageName\":\"\",\"className\":\"Login flow\",\"parameters\":{\"runId\":\"1042\"}}," +
            "{\"testName\":\"Checkout flow\",\"packageName\":\"\",\"className\":\"Checkout flow\",\"parameters\":{\"runId\":\"1043\"}}" +
                "]}";

        Run build = mockBuild(jsonTests);
        TaskListener listener = mockListener();

        TestsToRunConverterBuilder builder = new TestsToRunConverterBuilder("mi_agent", "");
        builder.perform(build, new FilePath(tempFolder.getRoot()), mock(Launcher.class), listener);

        ArgumentCaptor<Action> captor = ArgumentCaptor.forClass(Action.class);
        verify(build).addAction(captor.capture());

        VariableInjectionAction action = (VariableInjectionAction) captor.getValue();
        JsonNode manifest = MAPPER.readTree(extractVariable(action, "testsToRunConverted"));
        assertEquals(2, manifest.path("total_count").asInt());
        assertEquals("1042", manifest.path("data").get(0).path("id").asText());
        assertEquals("Login flow", manifest.path("data").get(0).path("test_name").asText());
        assertEquals("Google Chrome", manifest.path("data").get(0).path("au_tester_configuration").path("browser").path("BROWSER_NAME").asText());
    }

    // --- helpers ---

    private void registerOctaneMock() throws Exception {
        realConfig = OctaneConfiguration.create("test-config-id", "https://octane.example.com", "1000");
        when(mockClient.getConfigurationService()).thenReturn(mockConfigService);
        when(mockClient.getRestService()).thenReturn(mockRestService);
        when(mockConfigService.getConfiguration()).thenReturn(realConfig);
        when(mockRestService.obtainOctaneRestClient()).thenReturn(mockRestClient);

        OctaneResponse metadataResp = mockResponse(200, METADATA_WITH_AU);
        OctaneResponse runsResp = mockResponse(200, RUNS_RESPONSE);
        when(mockRestClient.execute(any()))
                .thenReturn(metadataResp)
                .thenReturn(runsResp);

        sdkClientsMap.put(realConfig, mockClient);
    }

    private Run mockBuild(String testsToRun) {
        Run build = mock(FreeStyleBuild.class);

        List<ParameterValue> params = Arrays.asList(
                new StringParameterValue("testsToRun", testsToRun),
                new StringParameterValue(SdkConstants.JobParameters.SUITE_ID_PARAMETER_NAME, "100"),
                new StringParameterValue(SdkConstants.JobParameters.SUITE_RUN_ID_PARAMETER_NAME, "5001"),
                new StringParameterValue(SdkConstants.JobParameters.OCTANE_CONFIG_ID_PARAMETER_NAME, "test-config-id"),
                new StringParameterValue(SdkConstants.JobParameters.OCTANE_WORKSPACE_PARAMETER_NAME, "1001"),
                new StringParameterValue(SdkConstants.JobParameters.OCTANE_SPACE_PARAMETER_NAME, "1000"),
                new StringParameterValue(SdkConstants.JobParameters.OCTANE_URL_PARAMETER_NAME, "https://octane.example.com")
        );

        ParametersAction parametersAction = new ParametersAction(params);
        when(build.getAction(ParametersAction.class)).thenReturn(parametersAction);

        File rootDir = new File(tempFolder.getRoot(), "build");
        rootDir.mkdirs();
        when(build.getRootDir()).thenReturn(rootDir);

        return build;
    }

    private TaskListener mockListener() {
        TaskListener listener = mock(TaskListener.class);
        when(listener.getLogger()).thenReturn(new PrintStream(System.out));
        return listener;
    }

    private String extractVariable(VariableInjectionAction action, String key) {
        EnvVars env = new EnvVars();
        action.buildEnvVars(null, env);
        return env.get(key);
    }

    private OctaneResponse mockResponse(int status, String body) {
        OctaneResponse r = mock(OctaneResponse.class);
        when(r.getStatus()).thenReturn(status);
        when(r.getBody()).thenReturn(body);
        return r;
    }
}
