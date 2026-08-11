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
import com.microfocus.application.automation.tools.mi.MIAgentBuildAction;
import com.microfocus.application.automation.tools.mi.MIAgentResultPublisher;
import hudson.*;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import jenkins.tasks.SimpleBuildStep;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;

/**
 * Build step that executes MI Agent runs from the converted tests payload.
 *
 * <p>The converter ({@code TestsToRunConverterBuilder} with {@code MF_MI_AGENT}) provides a
 * manifest where each item under {@code data[]} is a manual run payload including its
 * {@code run_steps}. This builder materializes one run-step file per run, executes the
 * configured MI Agent runner, and writes a manifest consumed by {@link MIAgentResultPublisher}.</p>
 */
public class RunFromMiAgentBuilder extends Builder implements SimpleBuildStep {

    public static final String DEFAULT_RESULT_FOLDER = MIAgentResultPublisher.DEFAULT_RESULT_FOLDER;
    private static final String MI_AGENT_EXE =  "mi-agent.exe";
    private static final String RUN_STEPS_FILE_NAME = "run_steps.json";
    private static final String RUN_STEPS_RESULT_FILE_NAME = "run_steps_result.json";
    private static final String MANIFEST_FILE_NAME = "manifest.json";
    private static final String CONF_FILE_NAME = "conf.json";
    private static final String[] RUN_STEP_SCALAR_FIELDS = {
            "type", "workspace_id", "name", "test_name", "order_in_suite_run",
            "duration", "id", "subtype", "has_attachments", "manual_run_source"
    };
    private static final String[] RUN_STEP_OBJECT_FIELDS = {
            "au_tester_configuration", "parent_suite", "run_steps", "test", "native_status", "run_by"
    };

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
        log.println("[MI Agent] Tagged build as MI Agent run.");

        String converted = resolveConvertedTests(build, listener);
        if (StringUtils.isBlank(converted)) {
            log.println("[MI Agent] No MI Agent tests were found.");
            return;
        }

        Object parsed = JSONValue.parse(converted);
        if (!(parsed instanceof JSONObject)) {
            log.println("[MI Agent][ERROR] Converted tests payload is not a JSON object.");
            build.setResult(Result.FAILURE);
            return;
        }

        JSONArray data = (JSONArray) ((JSONObject) parsed).get("data");
        if (data == null || data.isEmpty()) {
            log.println("[MI Agent] Converted tests payload contains no runs.");
            return;
        }

        FilePath resultRoot = workspace.child(DEFAULT_RESULT_FOLDER);
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
                log.println("[MI Agent][WARN] Skipping run with missing id.");
                continue;
            }

            FilePath runFolder = resultRoot.child(runId);
            runFolder.mkdirs();
            FilePath runStepsFile = runFolder.child(RUN_STEPS_FILE_NAME);
            JSONObject runStepsInput = normalizeRunStepsInput(runData);
            runStepsFile.write(runStepsInput.toJSONString(), "UTF-8");

            int exitCode = executeRunner(runFolder, runStepsFile, workspace, launcher, log, build);
            boolean hasResult = runFolder.child(RUN_STEPS_RESULT_FILE_NAME).exists();
            if (exitCode != 0 || !hasResult) {
                failures++;
                if (!hasResult) {
                    synthesizeFailureResult(runFolder, runStepsInput,
                            "MI Agent exited with code " + exitCode + " and did not produce run_steps_result.json");
                }
            }

            JSONObject runManifest = new JSONObject();
            runManifest.put("runId", runId);
            runManifest.put("runFolder", runFolder.getRemote());
            runManifest.put("runStepsFile", RUN_STEPS_FILE_NAME);
            runManifest.put("runStepsResultFile", RUN_STEPS_RESULT_FILE_NAME);
            runManifest.put("exitCode", exitCode);
            runManifest.put("hasResult", runFolder.child(RUN_STEPS_RESULT_FILE_NAME).exists());
            manifestRuns.add(runManifest);
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

    private String resolveConvertedTests(Run<?, ?> build, TaskListener listener) {
        try {
            EnvVars env = build.getEnvironment(listener);
            if (env != null) {
                return env.get(TestsToRunConverter.DEFAULT_TESTS_TO_RUN_CONVERTED_PARAMETER);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            listener.getLogger().println("[MI Agent][WARN] Interrupted while reading build environment: " + e.getMessage());
        } catch (IOException e) {
            listener.getLogger().println("[MI Agent][WARN] Failed to read build environment: " + e.getMessage());
        }
        return null;
    }

    private JSONObject normalizeRunStepsInput(JSONObject runData) {
        JSONObject normalized = new JSONObject();

        copyFields(normalized, runData, RUN_STEP_SCALAR_FIELDS, false);
        // Keep nested structures as JSON objects exactly as received (deep copied).
        copyFields(normalized, runData, RUN_STEP_OBJECT_FIELDS, true);
        normalized.putIfAbsent("run_steps", new JSONObject());

        return normalized;
    }

    private void copyFields(JSONObject target, JSONObject source, String[] fields, boolean deepCopy) {
        for (String field : fields) {
            Object value = source.get(field);
            if (value == null) {
                continue;
            }
            target.put(field, deepCopy ? deepCopyObject(value) : value);
        }
    }

    private Object deepCopyObject(Object value) {
        Object deepCopy = JSONValue.parse(JSONValue.toJSONString(value));
        return deepCopy instanceof JSONObject ? deepCopy : value;
    }

    private int executeRunner(FilePath runFolder,
                              FilePath runStepsFile,
                              FilePath workspace,
                              Launcher launcher,
                              PrintStream log,
                              Run<?, ?> build) throws IOException, InterruptedException {
        FilePath sharedRunner = resolveRunnerExecutable(workspace);
        if (sharedRunner == null) {
            throw new IOException("[MI Agent][ERROR] MI Agent executable not found at required shared location: ${WORKSPACE}/../"
                    + MI_AGENT_EXE);
        }

        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add(sharedRunner.getRemote());
        FilePath confFile = generateConfFile(workspace, runStepsFile.getRemote(), runFolder.getRemote(), build);
        args.add("--config_file_path=" + confFile.getRemote());
        log.println("[MI Agent] Resolved executable: " + sharedRunner.getRemote());
        int exitCode = launcher.launch().cmds(args).stdout(log).pwd(workspace).join();
        log.println("[MI Agent] Exit code: " + exitCode);
        return exitCode;
    }

    private FilePath resolveRunnerExecutable(FilePath workspace) throws IOException, InterruptedException {
        FilePath sharedWorkspace = workspace.getParent();
        if (sharedWorkspace == null) {
            return null;
        }

        FilePath sharedRunner = sharedWorkspace.child(MI_AGENT_EXE);
        if (sharedRunner.exists() && !sharedRunner.isDirectory()) {
            return sharedRunner;
        }

        return null;
    }

    private FilePath generateConfFile(FilePath workspace, String runStepFilePath, String outputBaseDir, Run<?, ?> build)
            throws IOException, InterruptedException {
        String llmAnalyzerKey = resolveCredentialSecret(build, "LLM_ANALYZER_KEY");
        String llmExecutorKey = resolveCredentialSecret(build, "LLM_EXECUTOR_KEY");

        JSONObject conf = new JSONObject();
        conf.put("LLM_EXECUTOR_VENDOR", "GEMINI");
        conf.put("LLM_EXECUTOR_MODEL", "gemini-2.5-flash");
        conf.put("LLM_EXECUTOR_TEMPERATURE", 0.2);
        conf.put("LLM_ANALYZER_VENDOR", "GEMINI");
        conf.put("LLM_ANALYZER_MODEL", "gemini-2.5-pro");
        conf.put("LLM_ANALYZER_KEY", llmAnalyzerKey);
        conf.put("LLM_EXECUTOR_KEY", llmExecutorKey);
        conf.put("STEP_MULTIPLIER", 3);
        conf.put("BROWSER_USE_LOGGING_LEVEL", "debug");
        conf.put("ANONYMIZED_TELEMETRY", false);
        conf.put("CONVERSATION", true);
        conf.put("COST_CALCULATION", true);
        conf.put("EXECUTION_RECORDING_ENABLED", false);
        conf.put("NO_IMAGES", false);
        conf.put("MI_DOM_STABILITY_WAIT", 2.0);
        conf.put("MI_DOM_STABILITY_MIN_WAIT", 0.3);
        conf.put("MI_AGENT_MODE", "dev");
        conf.put("VALIDATE_SCHEMA", true);
        conf.put("RUN_STEP_FILE_PATH", runStepFilePath);
        conf.put("OUTPUT_BASE_DIR", outputBaseDir);
        conf.put("LOG_TO_CONSOLE", 2);
        conf.put("DISABLE_LOG_REDIRECT", 2);
        conf.put("AWS_CLUSTER_NAME", "jenkins-mi-agent"); // TODO ask Idan

        FilePath confFile = workspace.child(CONF_FILE_NAME);
        confFile.write(conf.toJSONString(), "UTF-8");
        return confFile;
    }

    private String resolveCredentialSecret(Run<?, ?> build, String credentialId) throws IOException {
        StringCredentials credentials = CredentialsProvider.findCredentialById(
                credentialId, StringCredentials.class, build, Collections.emptyList());
        if (credentials == null) {
            throw new IOException("[MI Agent][ERROR] Jenkins credential not found for " + credentialId + ".");
        }
        return credentials.getSecret().getPlainText();
    }

    private void synthesizeFailureResult(FilePath runFolder, JSONObject runStepsInput, String message) throws IOException, InterruptedException {
        JSONObject failedStatus = new JSONObject();
        failedStatus.put("type", "list_node");
        failedStatus.put("id", "list_node.run_native_status.failed");
        failedStatus.put("logical_name", "list_node.run_native_status.failed");
        failedStatus.put("name", "Failed");

        JSONArray failedSteps = new JSONArray();
        JSONObject runSteps = (JSONObject) runStepsInput.get("run_steps");
        JSONArray data = runSteps == null ? null : (JSONArray) runSteps.get("data");
        if (data != null) {
            for (Object o : data) {
                if (!(o instanceof JSONObject)) {
                    continue;
                }
                JSONObject source = (JSONObject) o;
                JSONObject step = new JSONObject();
                step.put("type", "run_step");
                step.put("id", source.get("id"));
                step.put("result", failedStatus);
                step.put("actual", message);
                failedSteps.add(step);
            }
        }

        JSONObject result = new JSONObject();
        result.put("type", "run_manual_test");
        result.put("id", runStepsInput.get("id"));
        result.put("description", message);
        result.put("duration", 0);
        result.put("native_status", failedStatus);
        JSONObject steps = new JSONObject();
        steps.put("data", failedSteps);
        result.put("run_steps", steps);
        runFolder.child(RUN_STEPS_RESULT_FILE_NAME).write(result.toJSONString(), "UTF-8");
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
            return "Run MI Agent (Autonomous-Tester)";
        }
    }
}
