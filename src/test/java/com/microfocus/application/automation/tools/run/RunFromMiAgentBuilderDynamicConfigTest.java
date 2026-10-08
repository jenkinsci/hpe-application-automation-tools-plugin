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
package com.microfocus.application.automation.tools.run;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.octane.integrations.executor.TestsToRunConverter;
import com.microfocus.application.automation.tools.mi.AuTeLlmCredentials;
import com.microfocus.application.automation.tools.model.LoggedJenkinsRule;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import hudson.util.Secret;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the per-run dynamic configuration flow: each run's LLM credential (looked up by
 * {@code ENDPOINT_LOGICAL_NAME} against an {@link AuTeLlmCredentials}) is the only thing piped over
 * stdin, while its adapted {@code llm_configuration} (vendor fan-out, MODEL_PARAMETERS
 * array-to-object) goes to the run folder's {@code run_steps.json} and its {@code OUTPUT_BASE_DIR}
 * to that folder's {@code conf.json}.
 */
public class RunFromMiAgentBuilderDynamicConfigTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CREDENTIAL_ID = "gemini-config";

    @Rule
    public LoggedJenkinsRule jenkins = new LoggedJenkinsRule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void registerLlmCredential() {
        SystemCredentialsProvider.getInstance().getCredentials().add(new AuTeLlmCredentials(
                CredentialsScope.GLOBAL, CREDENTIAL_ID, "desc", Secret.fromString("{\"apiKey\":\"secret-value\"}")));
    }

    @Test
    public void perform_pipesOnlyCredentialsOverStdinAndAdaptsConfigurationInRunStepFile() throws Exception {
        File sharedWorkspace = tempFolder.newFolder("shared-workspace-dynamic-config");
        assertTrue(new File(sharedWorkspace, "mi-agent.exe").createNewFile());
        File buildWorkspace = new File(sharedWorkspace, "build-workspace");
        assertTrue(buildWorkspace.mkdir());

        String convertedTests = "{\"data\":["
                + "{\"id\":\"2001\",\"au_tester_configuration\":{"
                + "\"llm_configuration\":{\"ENDPOINT_LOGICAL_NAME\":\"" + CREDENTIAL_ID + "\",\"VENDOR\":\"GEMINI\","
                + "\"executor_model\":{\"MODEL_PARAMETERS\":[{\"Name\":\"temperature\",\"Value\":\"0.2\"}]}}"
                + "}}"
                + "]}";

        Launcher.ProcStarter procStarter = mockProcStarter();
        Launcher launcher = mock(Launcher.class);
        when(launcher.launch()).thenReturn(procStarter);

        RunFromMiAgentBuilder builder = new RunFromMiAgentBuilder();
        builder.perform(mockBuild(convertedTests), new FilePath(buildWorkspace), launcher, mockListener());

        ArgumentCaptor<InputStream> stdinCaptor = ArgumentCaptor.forClass(InputStream.class);
        verify(procStarter).stdin(stdinCaptor.capture());
        JsonNode document = MAPPER.readTree(stdinCaptor.getValue());
        assertEquals("secret-value", document.path("credentials").path("apiKey").asText());
        assertTrue(document.path("run_step").isMissingNode());

        File runFolder = runFolder(buildWorkspace, "2001");
        JsonNode llmConfiguration = MAPPER.readTree(new File(runFolder, "run_steps.json"))
                .path("au_tester_configuration").path("llm_configuration");
        assertEquals("GEMINI", llmConfiguration.path("executor_model").path("LLM_EXECUTOR_VENDOR").asText());
        assertEquals("0.2", llmConfiguration.path("executor_model").path("MODEL_PARAMETERS").path("temperature").asText());

        JsonNode conf = MAPPER.readTree(new File(runFolder, "conf.json"));
        assertEquals(runFolder.getAbsolutePath(), conf.path("OUTPUT_BASE_DIR").asText());
    }

    @Test
    public void perform_eachRunGetsItsOwnRunStepFileAndOutputBaseDir() throws Exception {
        File sharedWorkspace = tempFolder.newFolder("shared-workspace-perform");
        assertTrue(new File(sharedWorkspace, "mi-agent.exe").createNewFile());
        File buildWorkspace = new File(sharedWorkspace, "build-workspace");
        assertTrue(buildWorkspace.mkdir());

        String convertedTests = "{\"data\":["
                + "{\"id\":\"2001\",\"au_tester_configuration\":{\"llm_configuration\":{\"ENDPOINT_LOGICAL_NAME\":\"" + CREDENTIAL_ID + "\"}}},"
                + "{\"id\":\"2002\",\"au_tester_configuration\":{\"llm_configuration\":{\"ENDPOINT_LOGICAL_NAME\":\"" + CREDENTIAL_ID + "\"}}}"
                + "]}";

        Launcher.ProcStarter procStarter = mockProcStarter();
        Launcher launcher = mock(Launcher.class);
        when(launcher.launch()).thenReturn(procStarter);

        RunFromMiAgentBuilder builder = new RunFromMiAgentBuilder();
        builder.perform(mockBuild(convertedTests), new FilePath(buildWorkspace), launcher, mockListener());

        verify(procStarter, times(2)).stdin(any(InputStream.class));

        for (String runId : new String[]{"2001", "2002"}) {
            File runFolder = runFolder(buildWorkspace, runId);
            assertEquals(runId, MAPPER.readTree(new File(runFolder, "run_steps.json")).path("id").asText());
            assertEquals(runFolder.getAbsolutePath(),
                    MAPPER.readTree(new File(runFolder, "conf.json")).path("OUTPUT_BASE_DIR").asText());
        }
    }

    /** Mirrors ai-executor: no result file means every step is reported skipped with the error on the run. */
    @Test
    public void perform_whenAgentProducesNoResult_synthesizesSkippedResult() throws Exception {
        File sharedWorkspace = tempFolder.newFolder("shared-workspace-skipped");
        assertTrue(new File(sharedWorkspace, "mi-agent.exe").createNewFile());
        File buildWorkspace = new File(sharedWorkspace, "build-workspace");
        assertTrue(buildWorkspace.mkdir());

        String convertedTests = "{\"data\":[{\"id\":\"4001\",\"au_tester_configuration\":"
                + "{\"llm_configuration\":{\"ENDPOINT_LOGICAL_NAME\":\"" + CREDENTIAL_ID + "\"}},"
                + "\"run_steps\":{\"data\":[{\"type\":\"run_step\",\"id\":\"s1\"}]}}]}";

        Launcher.ProcStarter procStarter = mockProcStarter();
        Launcher launcher = mock(Launcher.class);
        when(launcher.launch()).thenReturn(procStarter);

        RunFromMiAgentBuilder builder = new RunFromMiAgentBuilder();
        builder.perform(mockBuild(convertedTests), new FilePath(buildWorkspace), launcher, mockListener());

        JsonNode result = MAPPER.readTree(new File(runFolder(buildWorkspace, "4001"), "run_steps_result.json"));
        assertEquals("list_node.run_native_status.skipped", result.path("native_status").path("id").asText());
        assertTrue(result.path("description").asText().contains("did not produce"));
        assertEquals("list_node.run_native_status.skipped",
                result.path("run_steps").path("data").get(0).path("result").path("id").asText());
        assertEquals("This run step was skipped",
                result.path("run_steps").path("data").get(0).path("actual").asText());
    }

    @Test
    public void perform_writesConfFileInIndividualRunFolder() throws Exception {
        File sharedWorkspace = tempFolder.newFolder("shared-workspace-no-conf-file");
        assertTrue(new File(sharedWorkspace, "mi-agent.exe").createNewFile());
        File buildWorkspace = new File(sharedWorkspace, "build-workspace");
        assertTrue(buildWorkspace.mkdir());

        String convertedTests = "{\"data\":[{\"id\":\"3001\",\"au_tester_configuration\":"
                + "{\"llm_configuration\":{\"ENDPOINT_LOGICAL_NAME\":\"" + CREDENTIAL_ID + "\"}}}]}";

        Launcher.ProcStarter procStarter = mockProcStarter();
        Launcher launcher = mock(Launcher.class);
        when(launcher.launch()).thenReturn(procStarter);

        RunFromMiAgentBuilder builder = new RunFromMiAgentBuilder();
        builder.perform(mockBuild(convertedTests), new FilePath(buildWorkspace), launcher, mockListener());

        assertFalse(new File(buildWorkspace, "conf.json").exists());

        File runFolder = runFolder(buildWorkspace, "3001");
        File confFile = new File(runFolder, "conf.json");
        assertTrue(confFile.exists());

        JsonNode conf = MAPPER.readTree(confFile);
        assertEquals(new File(runFolder, "run_steps.json").getAbsolutePath(), conf.path("RUN_STEP_FILE_PATH").asText());
        assertEquals(runFolder.getAbsolutePath(), conf.path("OUTPUT_BASE_DIR").asText());
    }

    /**
     * A real {@link FreeStyleProject} as the mocked build's parent, so
     * {@code CredentialsProvider.findCredentialById(..., Run, ...)} has a genuine Job/ACL context.
     */
    private Run<?, ?> mockBuild(String convertedTests) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        Run<?, ?> build = mock(FreeStyleBuild.class);
        when(build.getParent()).thenReturn((Job) project);

        EnvVars env = new EnvVars();
        env.put(TestsToRunConverter.DEFAULT_TESTS_TO_RUN_CONVERTED_PARAMETER, convertedTests);
        when(build.getEnvironment(any(TaskListener.class))).thenReturn(env);
        return build;
    }

    private TaskListener mockListener() {
        TaskListener listener = mock(TaskListener.class);
        when(listener.getLogger()).thenReturn(new PrintStream(System.out));
        return listener;
    }

    /** {@code <workspace>/mi-agent-results/<buildNumber>/<runId>}, where the mocked build number is 0. */
    private File runFolder(File buildWorkspace, String runId) {
        return new File(new File(new File(buildWorkspace, "mi-agent-results"), "0"), runId);
    }

    private Launcher.ProcStarter mockProcStarter() throws Exception {
        Launcher.ProcStarter procStarter = mock(Launcher.ProcStarter.class);
        when(procStarter.cmds(any(ArgumentListBuilder.class))).thenReturn(procStarter);
        when(procStarter.envs(anyMap())).thenReturn(procStarter);
        when(procStarter.stdin(any(InputStream.class))).thenReturn(procStarter);
        when(procStarter.stdout(any(PrintStream.class))).thenReturn(procStarter);
        when(procStarter.pwd(any(FilePath.class))).thenReturn(procStarter);
        when(procStarter.join()).thenReturn(0);
        return procStarter;
    }
}

