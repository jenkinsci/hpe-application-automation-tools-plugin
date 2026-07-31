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

import com.hp.octane.integrations.executor.TestsToRunConverter;
import com.microfocus.application.automation.tools.mi.MIAgentBuildAction;
import com.microfocus.application.automation.tools.mi.MIAgentResultPublisher;
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
import jenkins.tasks.SimpleBuildStep;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Build step that executes MI Agent runs from the converted tests payload.
 *
 * <p>The converter ({@code TestsToRunConverterBuilder} with {@code MF_MI_AGENT}) provides a
 * manifest where each item under {@code data[]} is a manual run payload including its
 * {@code run_steps}. This builder materializes one run-step file per run, executes the
 * configured MI Agent runner, and writes a manifest consumed by {@link MIAgentResultPublisher}.</p>
 */
public class RunFromMiAgentBuilder extends Builder implements SimpleBuildStep {

    public static final String DEFAULT_RUNNER_EXECUTABLE = "mi-agent.exe";
    public static final String DEFAULT_RESULT_FOLDER = MIAgentResultPublisher.DEFAULT_RESULT_FOLDER;
    private static final String RUN_STEPS_FILE_NAME = "run_steps.json";
    private static final String RUN_STEPS_RESULT_FILE_NAME = "run_steps_result.json";
    private static final String MANIFEST_FILE_NAME = "manifest.json";

    private String executorId;
    private String executorLogicalName;
    private String configurationId;
    private String workspaceId;
    private String runnerExecutable = DEFAULT_RUNNER_EXECUTABLE;
    private String resultFolder = DEFAULT_RESULT_FOLDER;
    private String browserChannel = "chrome";
    private boolean recordingEnabled = true;
    private boolean skipExecution;

    @DataBoundConstructor
    public RunFromMiAgentBuilder() {
    }

    public String getExecutorId() {
        return executorId;
    }

    @DataBoundSetter
    public void setExecutorId(String executorId) {
        this.executorId = executorId;
    }

    public String getExecutorLogicalName() {
        return executorLogicalName;
    }

    @DataBoundSetter
    public void setExecutorLogicalName(String executorLogicalName) {
        this.executorLogicalName = executorLogicalName;
    }

    public String getConfigurationId() {
        return configurationId;
    }

    @DataBoundSetter
    public void setConfigurationId(String configurationId) {
        this.configurationId = configurationId;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    @DataBoundSetter
    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getRunnerExecutable() {
        return runnerExecutable;
    }

    @DataBoundSetter
    public void setRunnerExecutable(String runnerExecutable) {
        this.runnerExecutable = StringUtils.isBlank(runnerExecutable) ? DEFAULT_RUNNER_EXECUTABLE : runnerExecutable.trim();
    }

    public String getResultFolder() {
        return resultFolder;
    }

    @DataBoundSetter
    public void setResultFolder(String resultFolder) {
        this.resultFolder = StringUtils.isBlank(resultFolder) ? DEFAULT_RESULT_FOLDER : resultFolder.trim();
    }

    public String getBrowserChannel() {
        return browserChannel;
    }

    @DataBoundSetter
    public void setBrowserChannel(String browserChannel) {
        this.browserChannel = StringUtils.isBlank(browserChannel) ? "chrome" : browserChannel.trim();
    }

    public boolean isRecordingEnabled() {
        return recordingEnabled;
    }

    @DataBoundSetter
    public void setRecordingEnabled(boolean recordingEnabled) {
        this.recordingEnabled = recordingEnabled;
    }

    public boolean isSkipExecution() {
        return skipExecution;
    }

    @DataBoundSetter
    public void setSkipExecution(boolean skipExecution) {
        this.skipExecution = skipExecution;
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

        FilePath resultRoot = workspace.child(resultFolder);
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

            int exitCode = executeRunner(runFolder, runStepsFile, workspace, launcher, log);
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
        normalized.put("type", "run_manual_test");
        normalized.put("id", runData.get("id"));
        if (runData.get("name") != null) {
            normalized.put("name", runData.get("name"));
        }
        Object runSteps = runData.get("run_steps");
        normalized.put("run_steps", runSteps != null ? runSteps : new JSONObject());
        return normalized;
    }

    private int executeRunner(FilePath runFolder,
                              FilePath runStepsFile,
                              FilePath workspace,
                              Launcher launcher,
                              PrintStream log) throws IOException, InterruptedException {
        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add(runnerExecutable);
        args.add("--run_step_file_path=" + runStepsFile.getRemote());
        args.add("--output_base_dir=" + runFolder.getRemote());
        args.add("--browser_channel=" + browserChannel);
        args.add("--execution_recording_enabled=" + (recordingEnabled ? "true" : "false"));
        if (skipExecution) {
            args.add("--skip_execution=true");
        }
        log.println("[MI Agent] Executing: " + args.toStringWithQuote());
        int exitCode = launcher.launch().cmds(args).stdout(log).pwd(workspace).join();
        log.println("[MI Agent] Exit code: " + exitCode);
        return exitCode;
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

    /**
     * Convenience check used by other extensions.
     */
    public static boolean isMiAgentRun(Run<?, ?> run) {
        return run != null && run.getAction(MIAgentBuildAction.class) != null;
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
