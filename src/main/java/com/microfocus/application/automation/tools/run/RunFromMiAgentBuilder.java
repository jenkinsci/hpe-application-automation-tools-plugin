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

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.hp.octane.integrations.executor.TestsToRunConverter;
import com.microfocus.application.automation.tools.mi.AuTeConfigurationStore;
import com.microfocus.application.automation.tools.mi.AuTeLlmCredentials;
import com.microfocus.application.automation.tools.mi.MIAgentConstants;
import com.microfocus.application.automation.tools.mi.MIAgentBuildAction;
import com.microfocus.application.automation.tools.mi.MIAgentResultPublisher;
import com.microfocus.application.automation.tools.settings.MiAgentGlobalConfiguration;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.Secret;
import jenkins.tasks.SimpleBuildStep;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Build step that executes Autonomous Tester (AuTe) runs from the converted tests payload.
 *
 * <p>The converter ({@code TestsToRunConverterBuilder} with {@code MF_MI_AGENT}) provides a
 * manifest where each item under {@code data[]} is a manual run payload including its
 * {@code run_steps}. For each run, this builder pipes one JSON document to mi-agent.exe over stdin:
 * {@code CONFIGURATION} holds the shared space default configuration, the LLM endpoint resolved from Jenkins
 * credentials and the run output directory; {@code RUN_TEST} holds the run payload unchanged.
 * A manifest consumed by {@link MIAgentResultPublisher} is written at the end.</p>
 */
public class RunFromMiAgentBuilder extends Builder implements SimpleBuildStep {

    private static final String MI_AGENT_EXE = "mi-agent.exe";
    private static final String RUN_STEPS_RESULT_FILE_NAME = MIAgentConstants.RUN_STEPS_RESULT_FILE_NAME;
    private static final String MANIFEST_FILE_NAME = MIAgentConstants.MANIFEST_FILE_NAME;

    private static final String AU_TESTER_CONFIGURATION_FIELD = "au_tester_configuration";
    private static final String LLM_CONFIGURATION_FIELD = "llm_configuration";
    private static final String LLM_LOGICAL_NAME_FIELD = "ENDPOINT_LOGICAL_NAME";

    private static final String CONFIGURATION_BLOCK = "CONFIGURATION";
    private static final String RUN_TEST_BLOCK = "RUN_TEST";
    private static final String LLM_ENDPOINT_BLOCK = "LLM_ENDPOINT";
    private static final String ENVIRONMENT_BLOCK = "ENVIRONMENT";
    private static final String OUTPUT_DIR_FIELD = "OUTPUT_DIR";

    private static final String SKIPPED_NATIVE_STATUS_ID = "list_node.run_native_status.skipped";
    private static final String SKIPPED_STEP_MESSAGE = "This run step was skipped";
    private static final String ERROR_PREFIX = "ErrorCode: ";

    private String executorId;
    private String executorLogicalName;
    private String configurationId;
    private String workspaceId;

    @DataBoundConstructor
    public RunFromMiAgentBuilder() {
    }

    @DataBoundSetter
    public void setExecutorId(String executorId) {
        this.executorId = executorId;
    }

    @DataBoundSetter
    public void setExecutorLogicalName(String executorLogicalName) {
        this.executorLogicalName = executorLogicalName;
    }

    @DataBoundSetter
    public void setConfigurationId(String configurationId) {
        this.configurationId = configurationId;
    }

    @DataBoundSetter
    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> build,
                        @Nonnull FilePath workspace,
                        @Nonnull Launcher launcher,
                        @Nonnull TaskListener listener) throws IOException, InterruptedException {

        PrintStream log = listener.getLogger();
        if (build.getAction(MIAgentBuildAction.class) == null) {
            build.addAction(new MIAgentBuildAction(executorId, executorLogicalName, configurationId, workspaceId));
        }
        log.println("[AuTe] Tagged build as Autonomous Tester run.");

        JSONArray data = resolveConvertedRuns(build, listener);
        if (data.isEmpty()) {
            return;
        }

        String defaultConfiguration = readDefaultConfiguration(workspace);

        FilePath resultRoot = MIAgentConstants.resultRoot(workspace);
        if (resultRoot.exists()) {
            resultRoot.deleteRecursive();
        }
        resultRoot.mkdirs();

        JSONArray manifestRuns = new JSONArray();
        int failures = 0;
        for (Object o : data) {
            if (!(o instanceof JSONObject)) {
                continue;
            }
            JSONObject runData = (JSONObject) o;
            String runId = String.valueOf(runData.get("id"));
            if (StringUtils.isBlank(runId) || "null".equalsIgnoreCase(runId)) {
                log.println("[AuTe][WARN] Skipping run with missing id.");
                continue;
            }

            FilePath runFolder = resultRoot.child(runId);
            runFolder.mkdirs();

            RunOutcome outcome;
            try {
                String llmLogicalName = extractLlmLogicalName(runData);
                JSONObject llmEndpoint = resolveLlmCredentials(build, log, llmLogicalName, runId);
                JSONObject stdinDocument = buildStdinDocument(defaultConfiguration, llmEndpoint, runData, runFolder);
                outcome = executeRunner(stdinDocument, build, workspace, launcher, listener, log);
            } catch (IOException e) {
                log.println(e.getMessage());
                synthesizeSkippedResult(runFolder, runData, e.getMessage());
                failures++;
                manifestRuns.add(buildRunManifestEntry(runId, runFolder));
                continue;
            }

            boolean hasResult = runFolder.child(RUN_STEPS_RESULT_FILE_NAME).exists();
            if (!hasResult) {
                String detailedError = extractMiAgentError(outcome.consoleOutput());
                String runErrorMessage = detailedError != null ? detailedError
                        : "Autonomous Tester exited with code %s and did not produce %s"
                        .formatted(outcome.exitCode(), RUN_STEPS_RESULT_FILE_NAME);
                log.println("[AuTe][WARN] %s".formatted(runErrorMessage));
                synthesizeSkippedResult(runFolder, runData, runErrorMessage);
                failures++;
            }

            manifestRuns.add(buildRunManifestEntry(runId, runFolder));
        }

        JSONObject manifest = new JSONObject();
        manifest.put("schemaVersion", "1.0");
        manifest.put("generatedAtEpochMillis", System.currentTimeMillis());
        manifest.put("executorId", executorId);
        manifest.put("executorLogicalName", executorLogicalName);
        manifest.put("configurationId", configurationId);
        manifest.put("workspaceId", workspaceId);
        manifest.put("runs", manifestRuns);
        manifest.put("totalRuns", manifestRuns.size());
        manifest.put("failedRuns", failures);
        resultRoot.child(MANIFEST_FILE_NAME).write(manifest.toJSONString(), "UTF-8");

        if (failures > 0) {
            build.setResult(Result.FAILURE);
        }
    }

    /**
     * Reads the converted-tests payload from the build environment and returns its {@code data[]} runs.
     *
     * @return the runs to execute, or an empty array when there is nothing to run
     * @throws IOException when the payload is present but malformed
     */
    private JSONArray resolveConvertedRuns(Run<?, ?> build, TaskListener listener) throws IOException {
        PrintStream log = listener.getLogger();
        String converted = null;
        try {
            EnvVars env = build.getEnvironment(listener);
            if (env != null) {
                converted = env.get(TestsToRunConverter.DEFAULT_TESTS_TO_RUN_CONVERTED_PARAMETER);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.println("[AuTe][WARN] Interrupted while reading build environment: %s".formatted(e.getMessage()));
        } catch (IOException e) {
            log.println("[AuTe][WARN] Failed to read build environment: %s".formatted(e.getMessage()));
        }

        if (StringUtils.isBlank(converted)) {
            log.println("[AuTe] No Autonomous Tester tests were found.");
            return new JSONArray();
        }

        Object parsed = JSONValue.parse(converted);
        if (!(parsed instanceof JSONObject)) {
            throw new IOException("[AuTe][ERROR] Converted tests payload is not a JSON object.");
        }

        Object data = ((JSONObject) parsed).get("data");
        if (data != null && !(data instanceof JSONArray)) {
            throw new IOException("[AuTe][ERROR] Converted tests payload 'data' is not a JSON array.");
        }
        if (data == null || ((JSONArray) data).isEmpty()) {
            log.println("[AuTe] Converted tests payload contains no runs.");
            return new JSONArray();
        }
        return (JSONArray) data;
    }

    private JSONObject resolveLlmCredentials(Run<?, ?> build,
                                             PrintStream log,
                                             String logicalName,
                                             String runId) throws IOException {
        if (StringUtils.isBlank(logicalName)) {
            throw new IOException("[AuTe][ERROR] Run %s has no '%s' in '%s' and no default is set in the Autonomous Tester settings. It must name an Autonomous Tester LLM Configuration credential."
                .formatted(runId, LLM_LOGICAL_NAME_FIELD, LLM_CONFIGURATION_FIELD));
        }

        AuTeLlmCredentials llmConfig = CredentialsProvider.findCredentialById(
                logicalName, AuTeLlmCredentials.class, build, Collections.emptyList());
        if (llmConfig == null) {
            throw new IOException("[AuTe][ERROR] No Autonomous Tester LLM Configuration with id '%s' is available to this job."
                .formatted(logicalName));
        }

        Object parsedConfig = JSONValue.parse(Secret.toString(llmConfig.getConfigurationJson()));
        if (!(parsedConfig instanceof JSONObject)) {
            throw new IOException("[AuTe][ERROR] LLM configuration '%s' must contain a JSON object."
                .formatted(logicalName));
        }

        log.println("[AuTe] Run %s: LLM configuration '%s' selected.".formatted(runId, logicalName));
        return (JSONObject) parsedConfig;
    }

    /** Octane value first, then the Jenkins global default; null when neither is set. */
    private String extractLlmLogicalName(JSONObject runData) {
        // Null when no default is configured in the Jenkins global settings.
        String defaultName = MiAgentGlobalConfiguration.getInstance().getDefaultLlmLogicalName();

        Object configuration = runData.get(AU_TESTER_CONFIGURATION_FIELD);
        if (!(configuration instanceof JSONObject)) {
            return defaultName;
        }
        Object llmConfiguration = ((JSONObject) configuration).get(LLM_CONFIGURATION_FIELD);
        if (!(llmConfiguration instanceof JSONObject)) {
            return defaultName;
        }
        Object logicalName = ((JSONObject) llmConfiguration).get(LLM_LOGICAL_NAME_FIELD);
        if (logicalName == null || StringUtils.isBlank(String.valueOf(logicalName))) {
            return defaultName;
        }
        return String.valueOf(logicalName).trim();
    }

    /** Reads the shared space default configuration copied into the workspace by {@link AuTeConfigurationSnapshotBuilder}. */
    private String readDefaultConfiguration(FilePath workspace) throws IOException, InterruptedException {
        FilePath file = workspace.child(AuTeConfigurationStore.SNAPSHOT_PATH);
        if (!file.exists()) {
            throw new IOException("[AuTe][ERROR] Shared space default configuration not found at %s"
                .formatted(file.getRemote()));
        }
        String content = file.readToString();
        if (!(JSONValue.parse(content) instanceof JSONObject)) {
            throw new IOException("[AuTe][ERROR] Shared space default configuration at %s must contain a JSON object."
                .formatted(file.getRemote()));
        }
        return content;
    }

    private JSONObject buildStdinDocument(String defaultConfiguration,
                                          JSONObject llmEndpoint,
                                          JSONObject runData,
                                          FilePath runFolder) {
        // Parsed per run so each run gets its own copy of the default configuration
        JSONObject config = (JSONObject) JSONValue.parse(defaultConfiguration);
        config.put(LLM_ENDPOINT_BLOCK, llmEndpoint);
        JSONObject environment = (JSONObject) config.get(ENVIRONMENT_BLOCK);
        environment.put(OUTPUT_DIR_FIELD, runFolder.getRemote());

        JSONObject document = new JSONObject();
        document.put(CONFIGURATION_BLOCK, config);
        document.put(RUN_TEST_BLOCK, runData);
        return document;
    }

    private RunOutcome executeRunner(JSONObject stdinDocument,
                              Run<?, ?> build,
                              FilePath workspace,
                              Launcher launcher,
                              TaskListener listener,
                              PrintStream log) throws IOException, InterruptedException {
        FilePath sharedRunner = resolveRunnerExecutable(workspace);
        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add(sharedRunner.getRemote());
        log.println("[AuTe] Resolved executable: %s".formatted(sharedRunner.getRemote()));

        EnvVars environment = new EnvVars(build.getEnvironment(listener));

        byte[] stdinBytes = stdinDocument.toJSONString().getBytes(StandardCharsets.UTF_8);

        // Tee stdout so its error diagnostic can be recovered after the process exits.
        ByteArrayOutputStream consoleCapture = new ByteArrayOutputStream();
        PrintStream teeStream = new PrintStream(new TeeOutputStream(log, consoleCapture), true, StandardCharsets.UTF_8);

        int exitCode = launcher.launch().cmds(args)
                .envs(environment)
                .stdin(new ByteArrayInputStream(stdinBytes))
                .stdout(teeStream).pwd(workspace).join();

        log.println("[AuTe] Exit code: %s".formatted(exitCode));
        return new RunOutcome(exitCode, consoleCapture.toString(StandardCharsets.UTF_8));
    }

    /** Exit code paired with captured mi-agent console output. */
    private record RunOutcome(int exitCode, String consoleOutput) {
    }

    private FilePath resolveRunnerExecutable(FilePath workspace) throws IOException, InterruptedException {
        FilePath sharedWorkspace = workspace.getParent();
        if (sharedWorkspace != null) {
            FilePath sharedRunner = sharedWorkspace.child(MI_AGENT_EXE);
            if (sharedRunner.exists() && !sharedRunner.isDirectory()) {
                return sharedRunner;
            }
        }

        throw new IOException("[AuTe][ERROR] mi-agent.exe not found at required shared location: ${WORKSPACE}/../%s"
            .formatted(MI_AGENT_EXE));
    }

    /** Returns the first console line containing the {@value #ERROR_PREFIX} marker, or {@code null} if none. */
    private String extractMiAgentError(String output) {
        String trimmed = StringUtils.trimToNull(output);
        if (trimmed == null) {
            return null;
        }
        return trimmed.lines()
                .filter(line -> line.contains(ERROR_PREFIX))
                .findFirst()
                .orElse(null);
    }

    private void synthesizeSkippedResult(FilePath runFolder, JSONObject runStepsInput, String message) throws IOException, InterruptedException {
        JSONObject skippedStatus = buildSkippedStatus();

        JSONArray skippedSteps = new JSONArray();
        JSONObject runSteps = (JSONObject) runStepsInput.get("run_steps");
        JSONArray data = runSteps == null ? null : (JSONArray) runSteps.get("data");
        if (data != null) {
            for (Object o : data) {
                if (!(o instanceof JSONObject)) {
                    continue;
                }
                JSONObject source = (JSONObject) o;
                JSONObject step = new JSONObject();
                step.put("type", source.get("type"));
                step.put("id", source.get("id"));
                step.put("result", skippedStatus);
                step.put("actual", SKIPPED_STEP_MESSAGE);
                skippedSteps.add(step);
            }
        }

        JSONObject result = new JSONObject();
        result.put("type", runStepsInput.get("type"));
        result.put("id", runStepsInput.get("id"));
        result.put("native_status", skippedStatus);
        result.put("description", message);
        JSONObject steps = new JSONObject();
        steps.put("data", skippedSteps);
        result.put("run_steps", steps);
        runFolder.child(RUN_STEPS_RESULT_FILE_NAME).write(result.toJSONString(), "UTF-8");
    }

    private JSONObject buildSkippedStatus() {
        JSONObject status = new JSONObject();
        status.put("type", "list_node");
        status.put("id", SKIPPED_NATIVE_STATUS_ID);
        status.put("logical_name", SKIPPED_NATIVE_STATUS_ID);
        status.put("name", "Skipped");
        return status;
    }

    private JSONObject buildRunManifestEntry(String runId, FilePath runFolder) {
        JSONObject runManifest = new JSONObject();
        runManifest.put("runId", runId);
        runManifest.put("runFolder", runFolder.getRemote());
        runManifest.put("runStepsResultFile", RUN_STEPS_RESULT_FILE_NAME);
        return runManifest;
    }

    @Extension
    @Symbol("runFromMiAgent")
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Run Autonomous Tester (AuTe)";
        }
    }
}
