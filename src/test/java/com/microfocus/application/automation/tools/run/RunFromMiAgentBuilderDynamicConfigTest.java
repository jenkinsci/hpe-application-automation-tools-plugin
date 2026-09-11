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
import com.microfocus.application.automation.tools.mi.CommonConstants;
import com.microfocus.application.automation.tools.model.LoggedJenkinsRule;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.FreeStyleBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import hudson.util.Secret;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

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
 * Verifies the dynamic per-run configuration flow: Octane's resolved {@code au_tester_configuration}
 * only takes effect if {@code RUN_STEP_FILE_PATH} reaches mi-agent as a real process env var, since
 * mi_agent/config.py reads it via argparse before {@code --config_file_path} (conf.json) is loaded.
 */
public class RunFromMiAgentBuilderDynamicConfigTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Rule
    public LoggedJenkinsRule jenkins = new LoggedJenkinsRule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void registerLlmCredentials() {
        SystemCredentialsProvider.getInstance().getCredentials().add(
                new StringCredentialsImpl(CredentialsScope.GLOBAL, "LLM_ANALYZER_KEY", "desc", Secret.fromString("secret")));
        SystemCredentialsProvider.getInstance().getCredentials().add(
                new StringCredentialsImpl(CredentialsScope.GLOBAL, "LLM_EXECUTOR_KEY", "desc", Secret.fromString("secret")));
    }

    @Test
    public void executeRunner_passesRunStepFilePathAsEnvVar() throws Exception {
        File sharedWorkspace = tempFolder.newFolder("shared-workspace");
        File runnerWorkspace = new File(sharedWorkspace, "runner");
        assertTrue(runnerWorkspace.mkdir());
        assertTrue(new File(sharedWorkspace, "mi-agent.exe").createNewFile());

        File runFolder = new File(runnerWorkspace, CommonConstants.RESULT_FOLDER + "\\1042");
        assertTrue(runFolder.mkdirs());
        File runStepsFile = new File(runFolder, CommonConstants.RUN_STEPS_FILE_NAME);
        Files.writeString(runStepsFile.toPath(), "{\"id\":\"1042\"}", StandardCharsets.UTF_8);

        Launcher.ProcStarter procStarter = mockProcStarter();
        Launcher launcher = mock(Launcher.class);
        when(launcher.launch()).thenReturn(procStarter);

        invokeExecuteRunner(runFolder, runStepsFile, runnerWorkspace, launcher);

        // The fix: RUN_STEP_FILE_PATH must be a real process env var, not just a value inside conf.json.
        ArgumentCaptor<Map> envs = ArgumentCaptor.forClass(Map.class);
        verify(procStarter).envs(envs.capture());
        assertEquals(runStepsFile.getAbsolutePath(), envs.getValue().get("RUN_STEP_FILE_PATH"));

        String confJson = Files.readString(new File(runnerWorkspace, CommonConstants.CONFIG_FILE_NAME).toPath(), StandardCharsets.UTF_8);
        assertFalse("conf.json must not carry RUN_STEP_FILE_PATH; mi-agent reads it too late from there.",
                confJson.contains("RUN_STEP_FILE_PATH"));
    }

    @Test
    public void perform_writesEachRunsOwnAuTesterConfiguration() throws Exception {
        File sharedWorkspace = tempFolder.newFolder("shared-workspace-perform");
        assertTrue(new File(sharedWorkspace, "mi-agent.exe").createNewFile());
        File buildWorkspace = new File(sharedWorkspace, "build-workspace");
        assertTrue(buildWorkspace.mkdir());

        String convertedTests = "{\"data\":["
                + "{\"id\":\"2001\",\"au_tester_configuration\":{\"browser\":{\"BROWSER_NAME\":\"Google Chrome\"}}},"
                + "{\"id\":\"2002\",\"au_tester_configuration\":{\"browser\":{\"BROWSER_NAME\":\"Firefox\"}}}"
                + "]}";

        Launcher.ProcStarter procStarter = mockProcStarter();
        Launcher launcher = mock(Launcher.class);
        when(launcher.launch()).thenReturn(procStarter);

        RunFromMiAgentBuilder builder = new RunFromMiAgentBuilder();
        builder.perform(mockBuild(convertedTests), new FilePath(buildWorkspace), launcher, mockListener());

        assertEquals("Google Chrome", readRunSteps(buildWorkspace, "2001")
                .path("au_tester_configuration").path("browser").path("BROWSER_NAME").asText());
        assertEquals("Firefox", readRunSteps(buildWorkspace, "2002")
                .path("au_tester_configuration").path("browser").path("BROWSER_NAME").asText());

        // Each run must launch mi-agent with its own RUN_STEP_FILE_PATH, not a shared/last-wins value.
        ArgumentCaptor<Map> envs = ArgumentCaptor.forClass(Map.class);
        verify(procStarter, times(2)).envs(envs.capture());
        assertTrue(((String) envs.getAllValues().get(0).get("RUN_STEP_FILE_PATH")).contains("2001"));
        assertTrue(((String) envs.getAllValues().get(1).get("RUN_STEP_FILE_PATH")).contains("2002"));
    }

    private void invokeExecuteRunner(File runFolder, File runStepsFile, File workspace, Launcher launcher) throws Exception {
        Method executeRunner = RunFromMiAgentBuilder.class.getDeclaredMethod(
                "executeRunner", FilePath.class, FilePath.class, FilePath.class, Launcher.class, PrintStream.class, Run.class);
        executeRunner.setAccessible(true);
        executeRunner.invoke(new RunFromMiAgentBuilder(),
                new FilePath(runFolder), new FilePath(runStepsFile), new FilePath(workspace),
                launcher, new PrintStream(System.out), mock(FreeStyleBuild.class));
    }

    private JsonNode readRunSteps(File buildWorkspace, String runId) throws Exception {
        File file = new File(buildWorkspace, CommonConstants.RESULT_FOLDER + "\\" + runId + "\\" + CommonConstants.RUN_STEPS_FILE_NAME);
        return MAPPER.readTree(file);
    }

    private Run<?, ?> mockBuild(String convertedTests) throws Exception {
        Run<?, ?> build = mock(FreeStyleBuild.class);
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

    private Launcher.ProcStarter mockProcStarter() throws Exception {
        Launcher.ProcStarter procStarter = mock(Launcher.ProcStarter.class);
        when(procStarter.cmds(any(ArgumentListBuilder.class))).thenReturn(procStarter);
        when(procStarter.envs(anyMap())).thenReturn(procStarter);
        when(procStarter.stdout(any(PrintStream.class))).thenReturn(procStarter);
        when(procStarter.pwd(any(FilePath.class))).thenReturn(procStarter);
        when(procStarter.join()).thenReturn(0);
        return procStarter;
    }
}
