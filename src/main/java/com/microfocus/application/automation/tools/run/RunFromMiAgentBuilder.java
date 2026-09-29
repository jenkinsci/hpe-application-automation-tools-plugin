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
import com.microfocus.application.automation.tools.mi.AuTeLlmCredentials;
import com.microfocus.application.automation.tools.mi.MIAgentConstants;
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
 * {@code run_steps}. For each run, this builder writes the normalized run data to a
 * {@code run_steps.json} file, writes a {@code conf.json} engine configuration in the run folder
 * (static defaults plus the per-run {@code RUN_STEP_FILE_PATH}/{@code OUTPUT_BASE_DIR}), and
 * invokes mi-agent.exe with {@code --config_file_path=<conf.json>}. Only the LLM
 * credentials resolved from Jenkins credentials are piped over stdin. A manifest consumed by
 * {@link MIAgentResultPublisher} is written at the end.</p>
 */
public class RunFromMiAgentBuilder extends Builder implements SimpleBuildStep {

    private static final String MI_AGENT_EXE = "mi-agent.exe";
    private static final String RUN_STEPS_RESULT_FILE_NAME = MIAgentConstants.RUN_STEPS_RESULT_FILE_NAME;
    private static final String MANIFEST_FILE_NAME = MIAgentConstants.MANIFEST_FILE_NAME;
    private static final String RUN_STEPS_FILE_NAME = "run_steps.json";
    private static final String CONF_FILE_NAME = "conf.json";

    private static final String AU_TESTER_CONFIGURATION_FIELD = "au_tester_configuration";
    private static final String LLM_CONFIGURATION_FIELD = "llm_configuration";
    private static final String LLM_LOGICAL_NAME_FIELD = "ENDPOINT_LOGICAL_NAME";
    private static final String VENDOR_FIELD = "VENDOR";
    private static final String EXECUTOR_MODEL_FIELD = "executor_model";
    private static final String REPORT_BUILDER_MODEL_FIELD = "report-builder-model";
    private static final String LLM_EXECUTOR_VENDOR_FIELD = "LLM_EXECUTOR_VENDOR";
    private static final String LLM_ANALYZER_VENDOR_FIELD = "LLM_ANALYZER_VENDOR";
    private static final String MODEL_PARAMETERS_FIELD = "MODEL_PARAMETERS";
    private static final String PARAMETER_NAME_FIELD = "Name";
    private static final String PARAMETER_VALUE_FIELD = "Value";
    private static final String CREDENTIALS_FIELD = "credentials";
    private static final String CONFIG_FILE_PATH_ARG_PREFIX = "--config_file_path=";

    private static final String DEFAULT_LLM_VENDOR = "GEMINI";
    private static final String DEFAULT_LLM_EXECUTOR_MODEL = "gemini-2.5-flash";
    private static final String DEFAULT_LLM_ANALYZER_MODEL = "gemini-2.5-pro";
    private static final double DEFAULT_LLM_TEMPERATURE = 0.2;
    private static final int DEFAULT_STEP_MULTIPLIER = 3;
    private static final int DEFAULT_MAX_FAILURES = 1;
    private static final int DEFAULT_LOG_TO_CONSOLE = 2;
    private static final int DEFAULT_DISABLE_LOG_REDIRECT = 2;

    private static final String[] RUN_STEP_SCALAR_FIELDS = {
            "type", "workspace_id", "name", "test_name", "order_in_suite_run", "duration", "id", "subtype", "has_attachments"
    };
    private static final String[] RUN_STEP_OBJECT_FIELDS = {
            AU_TESTER_CONFIGURATION_FIELD, "parent_suite", "run_steps", "test", "native_status", "run_by"
    };

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

        FilePath resultRoot = MIAgentConstants.resultRootForBuild(workspace, build);
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
            JSONObject runStep = toRunStep(runData);
            FilePath runStepsFile = runFolder.child(RUN_STEPS_FILE_NAME);
            runStepsFile.write(runStep.toJSONString(), "UTF-8");

            RunOutcome outcome;
            try {
                JSONObject credentials = resolveLlmCredentials(build, log, extractLlmLogicalName(runData), runId);
                FilePath confFile = writeConfFile(runFolder, runStepsFile.getRemote(), runFolder.getRemote());
                JSONObject credentialsDocument = buildCredentialsDocument(credentials);
                outcome = executeRunner(confFile, credentialsDocument, build, workspace, launcher, listener, log);
            } catch (IOException e) {
                log.println(e.getMessage());
                synthesizeSkippedResult(runFolder, runStep, e.getMessage());
                failures++;
                manifestRuns.add(buildRunManifestEntry(runId, runFolder));
                continue;
            }

            boolean hasResult = runFolder.child(RUN_STEPS_RESULT_FILE_NAME).exists();
            if (!hasResult) {
                String detailedError = extractMiAgentError(outcome.consoleOutput());
                String message = detailedError != null ? detailedError
                        : "Autonomous Tester exited with code " + outcome.exitCode() + " and did not produce " + RUN_STEPS_RESULT_FILE_NAME;
                log.println("[AuTe][WARN] " + message);
                synthesizeSkippedResult(runFolder, runStep, message);
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
            log.println("[AuTe][WARN] Interrupted while reading build environment: " + e.getMessage());
        } catch (IOException e) {
            log.println("[AuTe][WARN] Failed to read build environment: " + e.getMessage());
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
            throw new IOException("[AuTe][ERROR] Run " + runId + " has no '" + LLM_LOGICAL_NAME_FIELD
                    + "' in '" + LLM_CONFIGURATION_FIELD + "'. It must name an Autonomous Tester LLM Configuration credential.");
        }

        AuTeLlmCredentials llmConfig = CredentialsProvider.findCredentialById(
                logicalName, AuTeLlmCredentials.class, build, Collections.emptyList());
        if (llmConfig == null) {
            throw new IOException("[AuTe][ERROR] No Autonomous Tester LLM Configuration with id '"
                    + logicalName + "' is available to this job.");
        }

        Object parsedConfig = JSONValue.parse(Secret.toString(llmConfig.getConfigurationJson()));
        if (!(parsedConfig instanceof JSONObject)) {
            throw new IOException("[AuTe][ERROR] LLM configuration '" + logicalName
                    + "' must contain a JSON object.");
        }

        log.println("[AuTe] Run " + runId + ": LLM configuration '" + logicalName + "' selected.");
        return (JSONObject) parsedConfig;
    }

    private String extractLlmLogicalName(JSONObject runData) {
        Object configuration = runData.get(AU_TESTER_CONFIGURATION_FIELD);
        if (!(configuration instanceof JSONObject)) {
            return null;
        }
        Object llmConfiguration = ((JSONObject) configuration).get(LLM_CONFIGURATION_FIELD);
        if (!(llmConfiguration instanceof JSONObject)) {
            return null;
        }
        Object logicalName = ((JSONObject) llmConfiguration).get(LLM_LOGICAL_NAME_FIELD);
        return logicalName == null ? null : StringUtils.trimToNull(String.valueOf(logicalName));
    }

    private JSONObject toRunStep(JSONObject runData) {
        JSONObject normalized = new JSONObject();

        copyFields(normalized, runData, RUN_STEP_SCALAR_FIELDS, false);
        // Keep nested structures as JSON objects exactly as received (deep copied).
        copyFields(normalized, runData, RUN_STEP_OBJECT_FIELDS, true);

        adaptLlmConfiguration(normalized);
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

    /**
     * Adapts the Octane {@code llm_configuration} block in place: fans the shared {@code VENDOR} out to
     * the role-specific vendor keys mi-agent reads, and converts each {@code MODEL_PARAMETERS} name/value
     * list into a JSON object so it sits on the same leaf-key path {@code browser}/{@code agent} already take.
     */
    private void adaptLlmConfiguration(JSONObject runStep) {
        Object configuration = runStep.get(AU_TESTER_CONFIGURATION_FIELD);
        if (!(configuration instanceof JSONObject)) {
            return;
        }
        Object llmConfiguration = ((JSONObject) configuration).get(LLM_CONFIGURATION_FIELD);
        if (!(llmConfiguration instanceof JSONObject)) {
            return;
        }

        JSONObject llm = (JSONObject) llmConfiguration;
        Object vendor = llm.get(VENDOR_FIELD);

        adaptModel(llm, EXECUTOR_MODEL_FIELD, LLM_EXECUTOR_VENDOR_FIELD, vendor);
        adaptModel(llm, REPORT_BUILDER_MODEL_FIELD, LLM_ANALYZER_VENDOR_FIELD, vendor);
    }

    private void adaptModel(JSONObject llmConfiguration, String modelField, String vendorField, Object vendor) {
        Object model = llmConfiguration.get(modelField);
        if (!(model instanceof JSONObject)) {
            return;
        }

        JSONObject modelObject = (JSONObject) model;
        if (vendor != null) {
            modelObject.put(vendorField, vendor);
        }

        Object parameters = modelObject.get(MODEL_PARAMETERS_FIELD);
        if (parameters instanceof JSONArray) {
            modelObject.put(MODEL_PARAMETERS_FIELD, toModelParametersObject((JSONArray) parameters));
        }
    }

    /**
     * Converts a Name/Value entry list to a flat JSON object. Entries with a blank name are skipped;
     * a duplicate name keeps the last value. Values are copied unchanged, never cast.
     */
    private JSONObject toModelParametersObject(JSONArray parameters) {
        JSONObject result = new JSONObject();
        for (Object entry : parameters) {
            if (!(entry instanceof JSONObject)) {
                continue;
            }
            JSONObject parameter = (JSONObject) entry;
            String name = StringUtils.trimToNull(String.valueOf(parameter.get(PARAMETER_NAME_FIELD)));
            if (name == null) {
                continue;
            }
            result.put(name, parameter.get(PARAMETER_VALUE_FIELD));
        }
        return result;
    }

    /**
     * Wraps the resolved LLM credentials in the single-key document mi-agent reads from stdin.
     */
    private JSONObject buildCredentialsDocument(JSONObject credentials) {
        JSONObject document = new JSONObject();
        document.put(CREDENTIALS_FIELD, credentials);
        return document;
    }

    /**
    * Writes the run-specific {@code conf.json}: static engine defaults plus the per-run
     * {@code RUN_STEP_FILE_PATH}/{@code OUTPUT_BASE_DIR}. Any LLM vendor/model default here is
     * overwritten once mi-agent loads the run_step file's {@code au_tester_configuration}.
     */
    private FilePath writeConfFile(FilePath runFolder, String runStepFilePath, String outputBaseDir)
            throws IOException, InterruptedException {
        JSONObject conf = new JSONObject();
        conf.put(LLM_EXECUTOR_VENDOR_FIELD, DEFAULT_LLM_VENDOR);
        conf.put("LLM_EXECUTOR_MODEL", DEFAULT_LLM_EXECUTOR_MODEL);
        conf.put("LLM_EXECUTOR_TEMPERATURE", DEFAULT_LLM_TEMPERATURE);
        conf.put(LLM_ANALYZER_VENDOR_FIELD, DEFAULT_LLM_VENDOR);
        conf.put("LLM_ANALYZER_MODEL", DEFAULT_LLM_ANALYZER_MODEL);
        conf.put("LLM_ANALYZER_TEMPERATURE", DEFAULT_LLM_TEMPERATURE);
        conf.put("STEP_MULTIPLIER", DEFAULT_STEP_MULTIPLIER);
        conf.put("MAX_FAILURES", DEFAULT_MAX_FAILURES);
        conf.put("LOG_TO_CONSOLE", DEFAULT_LOG_TO_CONSOLE);
        conf.put("DISABLE_LOG_REDIRECT", DEFAULT_DISABLE_LOG_REDIRECT);
        conf.put("EXECUTION_RECORDING_ENABLED", false);
        conf.put("RUN_STEP_FILE_PATH", runStepFilePath);
        conf.put("OUTPUT_BASE_DIR", outputBaseDir);

        FilePath confFile = runFolder.child(CONF_FILE_NAME);
        confFile.write(conf.toJSONString(), "UTF-8");
        return confFile;
    }

    private RunOutcome executeRunner(FilePath confFile,
                              JSONObject credentialsDocument,
                              Run<?, ?> build,
                              FilePath workspace,
                              Launcher launcher,
                              TaskListener listener,
                              PrintStream log) throws IOException, InterruptedException {
        FilePath sharedRunner = resolveRunnerExecutable(workspace);
        if (sharedRunner == null) {
            throw new IOException("[AuTe][ERROR] mi-agent.exe not found at required shared location: ${WORKSPACE}/../"
                    + MI_AGENT_EXE);
        }

        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add(sharedRunner.getRemote());
        args.add(CONFIG_FILE_PATH_ARG_PREFIX + confFile.getRemote());
        log.println("[AuTe] Resolved executable: " + sharedRunner.getRemote());

        EnvVars environment = new EnvVars(build.getEnvironment(listener));

        byte[] stdinBytes = credentialsDocument.toJSONString().getBytes(StandardCharsets.UTF_8);

        // Tee stdout so its error diagnostic can be recovered after the process exits.
        ByteArrayOutputStream consoleCapture = new ByteArrayOutputStream();
        PrintStream teeStream = new PrintStream(new TeeOutputStream(log, consoleCapture), true, StandardCharsets.UTF_8);

        int exitCode = launcher.launch().cmds(args)
                .envs(environment)
                .stdin(new ByteArrayInputStream(stdinBytes))
                .stdout(teeStream).pwd(workspace).join();

        log.println("[AuTe] Exit code: " + exitCode);
        return new RunOutcome(exitCode, consoleCapture.toString(StandardCharsets.UTF_8));
    }

    /** Exit code paired with captured mi-agent console output. */
    private record RunOutcome(int exitCode, String consoleOutput) {
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
