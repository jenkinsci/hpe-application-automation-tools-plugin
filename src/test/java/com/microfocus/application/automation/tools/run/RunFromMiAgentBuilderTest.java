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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.octane.integrations.executor.TestsToRunConverter;
import com.microfocus.application.automation.tools.mi.MIAgentBuildAction;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Action;
import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RunFromMiAgentBuilderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void perform_emptyConvertedPayload_addsMiActionAndSkips() throws Exception {
        Run<?, ?> build = mockBuild(null, null);
        TaskListener listener = mockListener();

        RunFromMiAgentBuilder builder = new RunFromMiAgentBuilder();
        builder.setExecutorId("1001");
        builder.setExecutorLogicalName("exec-logical");
        builder.setConfigurationId("2001");
        builder.setWorkspaceId("1001");
        builder.perform(build, new FilePath(tempFolder.getRoot()), mock(Launcher.class), listener);

        ArgumentCaptor<Action> actionCaptor = ArgumentCaptor.forClass(Action.class);
        verify(build).addAction(actionCaptor.capture());
        assertTrue(actionCaptor.getValue() instanceof MIAgentBuildAction);
        MIAgentBuildAction action = (MIAgentBuildAction) actionCaptor.getValue();
        assertEquals("1001", action.getExecutorId());
        assertEquals("exec-logical", action.getExecutorLogicalName());
        assertEquals("2001", action.getConfigurationId());
        assertEquals("1001", action.getWorkspaceId());
        verify(build, never()).setResult(any());
        assertTrue(!new File(tempFolder.getRoot(), RunFromMiAgentBuilder.DEFAULT_RESULT_FOLDER).exists());
    }

    @Test
    public void perform_invalidConvertedPayload_setsFailure() throws Exception {
        Run<?, ?> build = mockBuild("[]", null);
        TaskListener listener = mockListener();

        RunFromMiAgentBuilder builder = new RunFromMiAgentBuilder();
        builder.perform(build, new FilePath(tempFolder.getRoot()), mock(Launcher.class), listener);

        verify(build).setResult(Result.FAILURE);
    }

    @Test
    public void perform_payloadWithoutValidRunIds_writesEmptyManifest() throws Exception {
        String converted = "{\"data\":[{\"name\":\"first\"},{\"id\":null},{\"id\":\"  \"}]}";
        Run<?, ?> build = mockBuild(converted, null);
        TaskListener listener = mockListener();

        RunFromMiAgentBuilder builder = new RunFromMiAgentBuilder();
        builder.setExecutorId("1002");
        builder.setExecutorLogicalName("mi-agent-executor");
        builder.setConfigurationId("2002");
        builder.setWorkspaceId("2001");

        builder.perform(build, new FilePath(tempFolder.getRoot()), mock(Launcher.class), listener);

        File manifestFile = new File(tempFolder.getRoot(), RunFromMiAgentBuilder.DEFAULT_RESULT_FOLDER + "\\manifest.json");
        assertTrue(manifestFile.exists());

        JsonNode manifest = MAPPER.readTree(Files.readString(manifestFile.toPath(), StandardCharsets.UTF_8));
        assertEquals("1.0", manifest.path("schemaVersion").asText());
        assertEquals(0, manifest.path("totalRuns").asInt());
        assertEquals(0, manifest.path("failedRuns").asInt());
        assertEquals("1002", manifest.path("executorId").asText());
        assertEquals("mi-agent-executor", manifest.path("executorLogicalName").asText());
        assertEquals("2002", manifest.path("configurationId").asText());
        assertEquals("2001", manifest.path("workspaceId").asText());
        verify(build, never()).setResult(any());
    }

    @Test
    public void perform_existingMiAction_doesNotAddDuplicateAction() throws Exception {
        MIAgentBuildAction existingAction = new MIAgentBuildAction("1003", "logical", "2003", "3003");
        Run<?, ?> build = mockBuild(null, existingAction);
        TaskListener listener = mockListener();

        RunFromMiAgentBuilder builder = new RunFromMiAgentBuilder();
        builder.perform(build, new FilePath(tempFolder.getRoot()), mock(Launcher.class), listener);

        verify(build, never()).addAction(any());
    }

    @Test
    public void normalizeRunStepsInput_deepCopiesNestedObjects() throws Exception {
        RunFromMiAgentBuilder builder = new RunFromMiAgentBuilder();

        JSONObject runData = new JSONObject();
        runData.put("id", "1042");
        runData.put("test_name", "Login flow");

        JSONObject runSteps = new JSONObject();
        JSONArray steps = new JSONArray();
        JSONObject step = new JSONObject();
        step.put("id", "s1");
        steps.add(step);
        runSteps.put("data", steps);
        runData.put("run_steps", runSteps);

        Method normalizeMethod = RunFromMiAgentBuilder.class.getDeclaredMethod("normalizeRunStepsInput", JSONObject.class);
        normalizeMethod.setAccessible(true);
        JSONObject normalized = (JSONObject) normalizeMethod.invoke(builder, runData);

        ((JSONObject) ((JSONArray) ((JSONObject) runData.get("run_steps")).get("data")).get(0)).put("id", "changed");

        assertEquals("1042", normalized.get("id"));
        assertNotNull(normalized.get("run_steps"));
        assertEquals("s1", ((JSONObject) ((JSONArray) ((JSONObject) normalized.get("run_steps")).get("data")).get(0)).get("id"));

        JSONObject runDataWithoutRunSteps = new JSONObject();
        runDataWithoutRunSteps.put("id", "2001");
        JSONObject normalizedWithoutRunSteps = (JSONObject) normalizeMethod.invoke(builder, runDataWithoutRunSteps);
        assertNotNull(normalizedWithoutRunSteps.get("run_steps"));
    }

    @Test
    public void synthesizeFailureResult_createsFailedResultFile() throws Exception {
        RunFromMiAgentBuilder builder = new RunFromMiAgentBuilder();

        FilePath runFolder = new FilePath(new File(tempFolder.getRoot(), "mi-agent-results\\1042"));
        runFolder.mkdirs();

        JSONObject runStepsInput = new JSONObject();
        runStepsInput.put("id", "1042");
        JSONObject runSteps = new JSONObject();
        JSONArray data = new JSONArray();
        JSONObject s1 = new JSONObject();
        s1.put("id", "s1");
        data.add(s1);
        JSONObject s2 = new JSONObject();
        s2.put("id", "s2");
        data.add(s2);
        runSteps.put("data", data);
        runStepsInput.put("run_steps", runSteps);

        Method synthesizeMethod = RunFromMiAgentBuilder.class.getDeclaredMethod(
                "synthesizeFailureResult", FilePath.class, JSONObject.class, String.class);
        synthesizeMethod.setAccessible(true);
        synthesizeMethod.invoke(builder, runFolder, runStepsInput, "runner failed");

        File resultFile = new File(runFolder.child("run_steps_result.json").getRemote());
        assertTrue(resultFile.exists());

        JsonNode json = MAPPER.readTree(Files.readString(resultFile.toPath(), StandardCharsets.UTF_8));
        assertEquals("1042", json.path("id").asText());
        assertEquals("list_node.run_native_status.failed", json.path("native_status").path("id").asText());
        assertEquals(2, json.path("run_steps").path("data").size());
        assertEquals("s1", json.path("run_steps").path("data").get(0).path("id").asText());
        assertEquals("runner failed", json.path("run_steps").path("data").get(1).path("actual").asText());
    }

    @Test
    public void resolveRunnerExecutable_usesSharedWorkspaceParent() throws Exception {
        RunFromMiAgentBuilder builder = new RunFromMiAgentBuilder();
        File sharedWorkspace = tempFolder.newFolder("shared-workspace");
        File runnerWorkspace = new File(sharedWorkspace, "runner-1");
        assertTrue(runnerWorkspace.mkdir());
        File miAgentExe = new File(sharedWorkspace, "mi-agent.exe");
        assertTrue(miAgentExe.createNewFile());

        Method resolveMethod = RunFromMiAgentBuilder.class.getDeclaredMethod("resolveRunnerExecutable", FilePath.class);
        resolveMethod.setAccessible(true);

        FilePath resolved = (FilePath) resolveMethod.invoke(builder, new FilePath(runnerWorkspace));
        assertNotNull(resolved);
        assertEquals(miAgentExe.getAbsolutePath(), resolved.getRemote());
    }

    @Test
    public void resolveRunnerExecutable_returnsNullWhenExeExistsOnlyInRunnerWorkspace() throws Exception {
        RunFromMiAgentBuilder builder = new RunFromMiAgentBuilder();
        File sharedWorkspace = tempFolder.newFolder("shared-workspace-only-runner-has-exe");
        File runnerWorkspace = new File(sharedWorkspace, "runner-2");
        assertTrue(runnerWorkspace.mkdir());
        File miAgentExeInRunner = new File(runnerWorkspace, "mi-agent.exe");
        assertTrue(miAgentExeInRunner.createNewFile());

        Method resolveMethod = RunFromMiAgentBuilder.class.getDeclaredMethod("resolveRunnerExecutable", FilePath.class);
        resolveMethod.setAccessible(true);

        FilePath resolved = (FilePath) resolveMethod.invoke(builder, new FilePath(runnerWorkspace));
        assertNull(resolved);
    }

    @Test
    public void executeRunner_throwsWhenSharedExecutableMissing() throws Exception {
        RunFromMiAgentBuilder builder = new RunFromMiAgentBuilder();
        File sharedWorkspace = tempFolder.newFolder("shared-workspace-missing-exe");
        File runnerWorkspace = new File(sharedWorkspace, "runner-3");
        assertTrue(runnerWorkspace.mkdir());
        File runFolderDir = new File(runnerWorkspace, "mi-agent-results\\1042");
        assertTrue(runFolderDir.mkdirs());
        File runSteps = new File(runFolderDir, "run_steps.json");
        Files.writeString(runSteps.toPath(), "{}", StandardCharsets.UTF_8);

        Method executeMethod = RunFromMiAgentBuilder.class.getDeclaredMethod(
                "executeRunner", FilePath.class, FilePath.class, FilePath.class,
                Launcher.class, PrintStream.class, Run.class);
        executeMethod.setAccessible(true);

        try {
            executeMethod.invoke(
                    builder,
                    new FilePath(runFolderDir),
                    new FilePath(runSteps),
                    new FilePath(runnerWorkspace),
                    mock(Launcher.class),
                    new PrintStream(System.out),
                    mock(FreeStyleBuild.class));
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertTrue(e.getCause() instanceof java.io.IOException);
            assertTrue(e.getCause().getMessage().contains("${WORKSPACE}/../mi-agent.exe"));
            return;
        }
        throw new AssertionError("Expected IOException");
    }

    private Run<?, ?> mockBuild(String convertedTests, MIAgentBuildAction existingAction) throws Exception {
        Run<?, ?> build = mock(FreeStyleBuild.class);
        if (existingAction != null) {
            when(build.getAction(MIAgentBuildAction.class)).thenReturn(existingAction);
        }

        EnvVars env = new EnvVars();
        if (convertedTests != null) {
            env.put(TestsToRunConverter.DEFAULT_TESTS_TO_RUN_CONVERTED_PARAMETER, convertedTests);
        }
        when(build.getEnvironment(any(TaskListener.class))).thenReturn(env);
        return build;
    }

    private TaskListener mockListener() {
        TaskListener listener = mock(TaskListener.class);
        when(listener.getLogger()).thenReturn(new PrintStream(System.out));
        return listener;
    }
}