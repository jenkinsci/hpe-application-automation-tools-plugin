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
package com.microfocus.application.automation.tools.mi;

import com.hp.octane.integrations.OctaneClient;
import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.services.rest.OctaneRestClient;
import com.hp.octane.integrations.utils.SdkStringUtils;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import jenkins.tasks.SimpleBuildStep;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.Serial;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Post-build publisher for MI Agent (Autonomous-Tester / AuTe) results.
 *
 * <p>Publishes the run status and per-step results back to Software Delivery Management, then
 * uploads recording/screenshot artifacts as attachments.</p>
 */
public class MIAgentResultPublisher extends Recorder implements SimpleBuildStep, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_RESULT_FOLDER = "mi-agent-results";
    public static final String DEFAULT_MANIFEST_NAME = "manifest.json";
    private static final String RUN_STEPS_RESULT_FILE = "run_steps_result.json";
    private static final Pattern SCREENSHOT_RE = Pattern.compile("^screenshot_(?<stepId>[^_]+)_");
    private static final List<String> SUPPORTED_MANIFEST_VERSIONS = List.of("1.0");
    private static final String ACCEPT_JSON = "application/json";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String RUN_NATIVE_STATUS_PREFIX = "list_node.run_native_status.";
    private static final int MAX_EXCEPTION_STACK_FRAMES = 7;
    private static final String WARN_PREFIX = "[WARN]";
    private static final String ERROR_PREFIX = "[ERROR]";
    private static final String CONTENT_PART_NAME = "content";
    private static final String ENTITY_PART_NAME = "entity";
    private static final String CLIENT_TYPE_HEADER = "HPECLIENTTYPE";
    private static final String CLIENT_TYPE_VALUE = "HPE_CI_CLIENT";
    private static final String RETURN_RESPONSE_IMMEDIATELY_HEADER = "RETURN_RESPONSE_IMMEDIATELY";
    private static final String RETURN_RESPONSE_IMMEDIATELY_VALUE = "true";
    // Keep bulk requests in practical limits similar to execution-service defaults.
    private static final int BULK_ATTACHMENTS_LIMIT = 50;
    private static final long BULK_ATTACHMENT_SIZE_ESTIMATE_BYTES = 1_048_576L;
    private static final long BULK_REQUEST_MAX_SIZE_BYTES = BULK_ATTACHMENTS_LIMIT * BULK_ATTACHMENT_SIZE_ESTIMATE_BYTES;
    private static final long MULTIPART_PER_ATTACHMENT_OVERHEAD_BYTES = 512L;
    private static final Map<String, String> BASE_HEADERS = Map.of("accept", ACCEPT_JSON, OctaneRestClient.CLIENT_TYPE_HEADER, OctaneRestClient.CLIENT_TYPE_VALUE);
    private static final Map<String, String> JSON_HEADERS = headersWithContentType(CONTENT_TYPE_JSON);

    private String resultFolder;
    private String manifestName;
    private String configurationId;
    private String workspaceId;
    private boolean uploadAttachments = true;
    private boolean failBuildOnPublishError = true;
    private transient OctaneClientProvider octaneClientProvider;
    private transient OctaneRequestExecutor octaneRequestExecutor;

    @DataBoundConstructor
    public MIAgentResultPublisher() {
        this.resultFolder = DEFAULT_RESULT_FOLDER;
        this.manifestName = DEFAULT_MANIFEST_NAME;
        initTransientCollaborators();
    }

    @DataBoundSetter
    public void setResultFolder(String resultFolder) {
        this.resultFolder = StringUtils.isBlank(resultFolder) ? DEFAULT_RESULT_FOLDER : resultFolder.trim();
    }

    @DataBoundSetter
    public void setManifestName(String manifestName) {
        this.manifestName = StringUtils.isBlank(manifestName) ? DEFAULT_MANIFEST_NAME : manifestName.trim();
    }

    public String getConfigurationId() {
        return configurationId;
    }

    @DataBoundSetter
    public void setConfigurationId(String configurationId) {
        this.configurationId = configurationId;
    }

    @DataBoundSetter
    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    @DataBoundSetter
    public void setUploadAttachments(boolean uploadAttachments) {
        this.uploadAttachments = uploadAttachments;
    }

    @DataBoundSetter
    public void setFailBuildOnPublishError(boolean failBuildOnPublishError) {
        this.failBuildOnPublishError = failBuildOnPublishError;
    }

    void setOctaneClientProvider(OctaneClientProvider octaneClientProvider) {
        this.octaneClientProvider = octaneClientProvider != null ? octaneClientProvider : OctaneSDK::getClientByInstanceId;
    }

    void setOctaneRequestExecutor(OctaneRequestExecutor octaneRequestExecutor) {
        this.octaneRequestExecutor = octaneRequestExecutor != null
                ? octaneRequestExecutor
                : (client, request) -> client.getRestService().obtainOctaneRestClient().execute(request);
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run,
                        @Nonnull FilePath workspace,
                        @Nonnull Launcher launcher,
                        @Nonnull TaskListener listener) throws IOException, InterruptedException {

        PrintStream log = listener.getLogger();
        log.println("Autonomous-Tester result publisher started.");

        MIAgentPublishSummary summary = new MIAgentPublishSummary();
        try {
            FilePath resultRoot = workspace.child(resultFolder);
            if (!resultRoot.exists()) {
                summary.setStatus(MIAgentPublishSummary.Status.NO_RESULTS);
                summary.setMessage("Result folder '" + resultFolder + "' was not found under workspace.");
                return;
            }

            FilePath manifestPath = resultRoot.child(manifestName);
            if (!manifestPath.exists()) {
                summary.setStatus(MIAgentPublishSummary.Status.NO_RESULTS);
                summary.setMessage("Manifest '" + manifestName + "' not found under '" + resultFolder + "'.");
                return;
            }

            JSONObject manifest = readManifest(manifestPath);
            validateManifest(manifest);

            PublishContext ctx = createPublishContext();
            JSONArray runs = (JSONArray) manifest.get("runs");
            List<String> failures = new ArrayList<>();
            int publishedSteps = 0;
            int totalSteps = 0;
            int consumedResultFiles = 1; // manifest.json

            for (Object item : runs) {
                if (!(item instanceof JSONObject runItem)) {
                    continue;
                }
                RunPublishData runData = parseRunPublishData(resultRoot, runItem, log);
                consumedResultFiles++; // per-run run_steps_result.json
                RunPublishResult runPublishResult = publishSingleRun(runData, ctx, log, failures);
                publishedSteps += runPublishResult.publishedSteps();
                totalSteps += runPublishResult.totalSteps();
            }

            summary.setTotalTests(totalSteps);
            summary.setPublishedFiles(consumedResultFiles);
            summary.setPublishedSteps(publishedSteps);
            if (failures.isEmpty()) {
                summary.setStatus(MIAgentPublishSummary.Status.PUBLISHED);
                summary.setMessage("Published " + runs.size() + " MI Agent run(s), " + publishedSteps + " step result(s).");
            } else {
                handlePublishFailures(run, failures, summary);
            }
        } catch (MIAgentValidationException e) {
            summary.setStatus(MIAgentPublishSummary.Status.INVALID);
            summary.setMessage("Validation failed: " + e.getMessage());
            run.setResult(Result.FAILURE);
        } catch (Exception e) {
            summary.setStatus(MIAgentPublishSummary.Status.ERROR);
            summary.setMessage("Unexpected error: " + e.getMessage());
            log.println(ERROR_PREFIX + " " + summary.getMessage());
            log.println(ERROR_PREFIX + " " + formatExceptionDetails(e));
            run.setResult(failBuildOnPublishError ? Result.FAILURE : Result.UNSTABLE);
        } finally {
            run.addAction(new MIAgentPublishSummaryAction(summary));
            log.println(summary.getMessage());
        }
    }

    private JSONObject readManifest(FilePath manifest) throws IOException, InterruptedException, MIAgentValidationException {
        if (!manifest.exists()) {
            throw new MIAgentValidationException("Manifest '" + manifest.getRemote() + "' not found.");
        }
        Object parsed = JSONValue.parse(manifest.readToString());
        if (!(parsed instanceof JSONObject)) {
            throw new MIAgentValidationException("Manifest must be a JSON object.");
        }
        return (JSONObject) parsed;
    }

    void validateManifest(JSONObject manifest) throws MIAgentValidationException {
        String schemaVersion = manifest.getAsString("schemaVersion");
        if (StringUtils.isBlank(schemaVersion) || !SUPPORTED_MANIFEST_VERSIONS.contains(schemaVersion)) {
            throw new MIAgentValidationException("Unsupported manifest version: " + schemaVersion
                    + ". Supported: " + SUPPORTED_MANIFEST_VERSIONS);
        }
        JSONArray runs = (JSONArray) manifest.get("runs");
        if (runs == null || runs.isEmpty()) {
            throw new MIAgentValidationException("Manifest contains no runs.");
        }
    }

    private PublishContext createPublishContext() throws MIAgentValidationException {
        initTransientCollaborators();
        if (StringUtils.isBlank(configurationId)) {
            throw new MIAgentValidationException("Missing configurationId.");
        }
        if (StringUtils.isBlank(workspaceId)) {
            throw new MIAgentValidationException("Missing workspaceId.");
        }

        OctaneClient client = octaneClientProvider.getClientByInstanceId(configurationId);
        if (client == null) {
            throw new MIAgentValidationException("Octane client not found for configurationId=" + configurationId);
        }

        OctaneConfiguration conf = client.getConfigurationService().getConfiguration();
        if (conf == null || SdkStringUtils.isEmpty(conf.getUrl()) || SdkStringUtils.isEmpty(conf.getSharedSpace())) {
            throw new MIAgentValidationException("Invalid Octane configuration.");
        }

        return new PublishContext(client, conf.getUrl(), conf.getSharedSpace(), workspaceId);
    }

    private RunPublishData parseRunPublishData(FilePath resultRoot, JSONObject runItem, PrintStream log) throws IOException, InterruptedException, MIAgentValidationException {
        String runId = runItem.getAsString("runId");
        String runFolderPath = runItem.getAsString("runFolder");
        if (StringUtils.isBlank(runId) || StringUtils.isBlank(runFolderPath)) {
            throw new MIAgentValidationException("Manifest run entry is missing runId/runFolder.");
        }
        log.println("parseRunPublishData: runId=" + runId + ", runFolder=" + runFolderPath);

        FilePath runFolder = resolveRunFolder(resultRoot, runFolderPath, runId);
        FilePath resultFile = runFolder.child(RUN_STEPS_RESULT_FILE);
        if (!resultFile.exists()) {
            throw new MIAgentValidationException("Missing run_steps_result.json for run " + runId);
        }

        Object parsedResult = JSONValue.parse(resultFile.readToString());
        if (!(parsedResult instanceof JSONObject)) {
            throw new MIAgentValidationException("run_steps_result.json is not a JSON object for run " + runId);
        }

        return new RunPublishData(runId, runFolder, (JSONObject) parsedResult);
    }

    private FilePath resolveRunFolder(FilePath resultRoot, String runFolderPath, String runId)
            throws IOException, InterruptedException, MIAgentValidationException {
        FilePath normalizedRoot = resultRoot.absolutize();
        FilePath runFolder = resultRoot.child(runFolderPath).absolutize();
        if (!isPathUnderRoot(normalizedRoot.getRemote(), runFolder.getRemote())) {
            throw new MIAgentValidationException("Run folder for run " + runId
                    + " points outside result root: " + runFolderPath);
        }
        return runFolder;
    }

    private boolean isPathUnderRoot(String rootPath, String candidatePath) {
        String normalizedRoot = normalizePathForComparison(rootPath);
        String normalizedCandidate = normalizePathForComparison(candidatePath);
        if (isWindowsStylePath(normalizedRoot) || isWindowsStylePath(normalizedCandidate)) {
            normalizedRoot = normalizedRoot.toLowerCase(Locale.ROOT);
            normalizedCandidate = normalizedCandidate.toLowerCase(Locale.ROOT);
        }
        return normalizedCandidate.equals(normalizedRoot)
                || normalizedCandidate.startsWith(normalizedRoot + "/");
    }

    private String normalizePathForComparison(String path) {
        String normalized = path.replace('\\', '/');
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private boolean isWindowsStylePath(String path) {
        return path.length() > 1 && Character.isLetter(path.charAt(0)) && path.charAt(1) == ':';
    }

    private RunPublishResult publishSingleRun(RunPublishData runData, PublishContext ctx, PrintStream log, List<String> failures)
            throws InterruptedException, IOException {
        String overallStatusId = toListNodeStatusId((JSONObject) runData.runResult().get("native_status"));
        if (StringUtils.isBlank(overallStatusId)) {
            overallStatusId = RUN_NATIVE_STATUS_PREFIX + "failed";
        }

        try {
            updateRunStatus(runData.runId(), overallStatusId, ctx, log);
        } catch (Exception e) {
            String details = StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getName());
            failures.add("Run " + runData.runId() + " status update failed: " + details);
            log.println(WARN_PREFIX + " Run " + runData.runId() + " status update failed: " + details);
            log.println(WARN_PREFIX + " " + formatExceptionDetails(e));
        }

        int publishedSteps = 0;
        int totalSteps = 0;
        JSONObject runSteps = (JSONObject) runData.runResult().get("run_steps");
        JSONArray steps = runSteps == null ? null : (JSONArray) runSteps.get("data");
        if (steps != null) {
            for (Object item : steps) {
                if (!(item instanceof JSONObject step)) {
                    continue;
                }
                totalSteps++;
                String stepId = String.valueOf(step.get("id"));
                String stepStatusId = toListNodeStatusId((JSONObject) step.get("result"));
                if (StringUtils.isBlank(stepId) || StringUtils.isBlank(stepStatusId)) {
                    continue;
                }
                String actual = step.get("actual") == null ? null : String.valueOf(step.get("actual"));
                try {
                    updateRunStep(stepId, stepStatusId, actual, ctx);
                    publishedSteps++;
                } catch (Exception e) {
                    String details = StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getName());
                    failures.add("Run " + runData.runId() + " step " + stepId + " update failed: " + details);
                    log.println(WARN_PREFIX + " Run " + runData.runId() + " step " + stepId + " update failed: " + details);
                    log.println(WARN_PREFIX + " " + formatExceptionDetails(e));
                }
            }
        }

        if (uploadAttachments) {
            uploadAttachments(runData, ctx, log, failures);
        }
        log.println("Published run " + runData.runId() + " (steps " + publishedSteps + "/" + totalSteps + ").");
        return new RunPublishResult(publishedSteps, totalSteps);
    }

    private void updateRunStatus(String runId, String statusId, PublishContext ctx, PrintStream log) throws IOException {
        JSONObject payload = new JSONObject();
        JSONObject status = new JSONObject();
        status.put("type", "list_node");
        status.put("id", statusId);
        payload.put("native_status", status);

        log.println("updateRunStatus: statusId=" + statusId);
        String url = String.format("%s/api/shared_spaces/%s/workspaces/%s/runs/%s", ctx.baseUrl(), ctx.sharedSpaceId(), ctx.workspaceId(), runId);
        OctaneResponse response = executeJsonRequest(HttpMethod.PUT, url, payload.toJSONString(), ctx.client());
        assertSuccess(response, "Update run status failed for run " + runId);
    }

    private void updateRunStep(String stepId, String statusId, String actual, PublishContext ctx) throws IOException {
        JSONObject payload = new JSONObject();
        JSONObject status = new JSONObject();
        status.put("type", "list_node");
        status.put("id", statusId);
        payload.put("result", status);
        if (actual != null) {
            payload.put("actual", actual);
        }

        String url = String.format("%s/api/shared_spaces/%s/workspaces/%s/run_steps/%s", ctx.baseUrl(), ctx.sharedSpaceId(), ctx.workspaceId(), stepId);
        OctaneResponse response = executeJsonRequest(HttpMethod.PUT, url, payload.toJSONString(), ctx.client());
        assertSuccess(response, "Update run step failed for step " + stepId);
    }

    private void uploadAttachments(RunPublishData runData, PublishContext ctx, PrintStream log, List<String> failures)
            throws InterruptedException, IOException {
        List<AttachmentUploadData> attachmentUploads = new ArrayList<>();
        FilePath recording = runData.runFolder().child("recording.mp4");
        if (recording.exists()) {
            attachmentUploads.add(new AttachmentUploadData(recording, "recording.mp4", "owner_run", "run", runData.runId()));
        }

        FilePath images = runData.runFolder().child("images");
        if (images.exists()) {
            FilePath[] imgs = images.list("screenshot_*.jpg");
            for (FilePath shot : imgs) {
                String name = shot.getName();
                Matcher m = SCREENSHOT_RE.matcher(name);
                if (!m.find()) {
                    continue;
                }
                String stepId = m.group("stepId");
                attachmentUploads.add(new AttachmentUploadData(shot, name, "owner_run_step", "run_step", stepId));
            }
        }

        if (attachmentUploads.isEmpty()) {
            return;
        }

        List<List<AttachmentUploadData>> batches = createAttachmentBatches(attachmentUploads);
        log.println("Uploading " + attachmentUploads.size() + " attachment(s) in " + batches.size() + " bulk chunk(s) ...");
        for (int i = 0; i < batches.size(); i++) {
            List<AttachmentUploadData> batch = batches.get(i);
            try {
                uploadAttachmentsBulk(ctx, batch);
            } catch (IOException e) {
                String details = summarizeExceptionMessage(e.getMessage());
                log.println(WARN_PREFIX + " Bulk attachment upload failed for run " + runData.runId()
                        + " (chunk " + (i + 1) + "/" + batches.size() + "): " + details);
                log.println(WARN_PREFIX + " " + formatExceptionDetails(e));
                failures.add("Run " + runData.runId() + " attachment bulk upload failed (chunk "
                        + (i + 1) + "/" + batches.size() + "): " + details);
            }
        }
    }

    private List<List<AttachmentUploadData>> createAttachmentBatches(List<AttachmentUploadData> uploads)
            throws IOException, InterruptedException {
        List<List<AttachmentUploadData>> batches = new ArrayList<>();
        List<AttachmentUploadData> currentBatch = new ArrayList<>();
        long currentBatchSize = 0L;

        for (AttachmentUploadData upload : uploads) {
            long fileSize = upload.file().length();
            long estimatedPartSize = fileSize + MULTIPART_PER_ATTACHMENT_OVERHEAD_BYTES;

            if (!currentBatch.isEmpty()
                    && (currentBatch.size() >= BULK_ATTACHMENTS_LIMIT
                    || currentBatchSize + estimatedPartSize > BULK_REQUEST_MAX_SIZE_BYTES)) {
                batches.add(currentBatch);
                currentBatch = new ArrayList<>();
                currentBatchSize = 0L;
            }

            currentBatch.add(upload);
            currentBatchSize += estimatedPartSize;
        }

        if (!currentBatch.isEmpty()) {
            batches.add(currentBatch);
        }

        return batches;
    }

    private void uploadAttachmentsBulk(PublishContext ctx, List<AttachmentUploadData> uploads)
            throws IOException, InterruptedException {
        String boundary = "----MIAgentBoundary" + UUID.randomUUID();
        String url = String.format("%s/api/shared_spaces/%s/workspaces/%s/attachments/bulk", ctx.baseUrl(), ctx.sharedSpaceId(), ctx.workspaceId());
        try (InputStream body = buildBulkMultipartBodyStream(boundary, uploads)) {
            Map<String, String> headers = headersWithContentType("multipart/form-data; boundary=" + boundary);
            headers.put(CLIENT_TYPE_HEADER, CLIENT_TYPE_VALUE);
            headers.put(RETURN_RESPONSE_IMMEDIATELY_HEADER, RETURN_RESPONSE_IMMEDIATELY_VALUE);
            OctaneRequest request = buildOctaneRequest(
                    HttpMethod.POST,
                    url,
                    headers,
                    body);
            OctaneResponse response = octaneRequestExecutor.execute(ctx.client(), request);
            assertSuccess(response, "Bulk attachment upload failed");
        }
    }

    private InputStream buildBulkMultipartBodyStream(String boundary, List<AttachmentUploadData> uploads)
            throws IOException, InterruptedException {
        List<InputStream> parts = new ArrayList<>(uploads.size() * 4 + 1);
        for (AttachmentUploadData upload : uploads) {
            String entityJson = buildAttachmentEntityJson(upload.fileName(), upload.ownerField(), upload.ownerType(), upload.ownerId());
            String mime = resolveMime(upload.fileName());
            parts.add(new ByteArrayInputStream(createFormField(ENTITY_PART_NAME, "blob", CONTENT_TYPE_JSON, entityJson, boundary)));
            parts.add(new ByteArrayInputStream(createFilePart(CONTENT_PART_NAME, upload.fileName(), mime, boundary)));
            parts.add(upload.file().read());
            parts.add(new ByteArrayInputStream("\r\n".getBytes(StandardCharsets.UTF_8)));
        }
        parts.add(new ByteArrayInputStream(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8)));
        return new SequenceInputStream(Collections.enumeration(parts));
    }

    private String buildAttachmentEntityJson(String fileName, String ownerField, String ownerType, String ownerId) {
        JSONObject entity = new JSONObject();
        entity.put("name", fileName);
        JSONObject owner = new JSONObject();
        owner.put("type", ownerType);
        owner.put("id", ownerId);
        entity.put(ownerField, owner);
        return entity.toJSONString();
    }

    private byte[] createFormField(String name, String filename, String contentType, String value, String boundary) {
        String part = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n"
                + value + "\r\n";
        return part.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] createFilePart(String name, String filename, String contentType, String boundary) {
        String partHeader = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n";
        return partHeader.getBytes(StandardCharsets.UTF_8);
    }

    private String resolveMime(String fileName) {
        String ext = FilenameUtils.getExtension(fileName);
        return switch (ext.toLowerCase(Locale.ROOT)) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "mp4" -> "video/mp4";
            default -> "application/octet-stream";
        };
    }

    private static Map<String, String> headersWithContentType(String contentType) {
        Map<String, String> headers = new LinkedHashMap<>(BASE_HEADERS);
        headers.put("Content-Type", contentType);
        return headers;
    }

    private OctaneResponse executeJsonRequest(HttpMethod method, String url, String jsonBody, OctaneClient client) throws IOException {
        OctaneRequest request = buildOctaneRequest(method, url, JSON_HEADERS, jsonBody);
        initTransientCollaborators();
        return octaneRequestExecutor.execute(client, request);
    }

    private OctaneRequest buildOctaneRequest(HttpMethod method, String url, Map<String, String> headers, String body) {
        return DTOFactory.getInstance()
                .newDTO(OctaneRequest.class)
                .setMethod(method)
                .setHeaders(headers)
                .setUrl(url)
                .setBody(body);
    }

    private OctaneRequest buildOctaneRequest(HttpMethod method, String url, Map<String, String> headers, InputStream body) {
        return DTOFactory.getInstance()
                .newDTO(OctaneRequest.class)
                .setMethod(method)
                .setHeaders(headers)
                .setUrl(url)
                .setBody(body);
    }

    private void assertSuccess(OctaneResponse response, String message) throws IOException {
        if (response == null) {
            throw new IOException(message + ": empty response");
        }
        if (response.getStatus() >= HttpStatus.SC_OK && response.getStatus() < HttpStatus.SC_MULTIPLE_CHOICES) {
            return;
        }
        throw new IOException(message + ". HTTP " + response.getStatus() + ", body: " + summarizeResponseBody(response.getBody()));
    }

    private String toListNodeStatusId(JSONObject statusObject) {
        if (statusObject != null) {
            String id = firstNonBlank(statusObject.getAsString("id"), statusObject.getAsString("logical_name"));
            if (StringUtils.isNotBlank(id) && id.startsWith("list_node.")) {
                return id;
            }
            String byName = mapStatusName(statusObject.getAsString("name"));
            if (StringUtils.isNotBlank(byName)) {
                return byName;
            }
        }
        return null;
    }

    private String mapStatusName(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        String s = raw.trim().toLowerCase(Locale.ROOT).replace("-", "").replace("_", "").replace(" ", "");
        String suffix = switch (s) {
            case "passed", "pass", "success" -> "passed";
            case "failed", "fail", "failure" -> "failed";
            case "skipped", "skip" -> "skipped";
            case "notcompleted" -> "not_completed";
            case "needsattention" -> "needs_attention";
            default -> null;
        };
        return suffix == null ? null : RUN_NATIVE_STATUS_PREFIX + suffix;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String formatExceptionDetails(Throwable t) {
        if (t == null) {
            return "null exception";
        }
        StringBuilder details = new StringBuilder();
        details.append(t.getClass().getName()).append(": ").append(summarizeExceptionMessage(t.getMessage()));

        StackTraceElement[] stack = t.getStackTrace();
        if (stack != null && stack.length > 0) {
            String nl = System.lineSeparator();
            int framesToPrint = Math.min(MAX_EXCEPTION_STACK_FRAMES, stack.length);
            details.append("; stack:");
            for (int i = 0; i < framesToPrint; i++) {
                details.append(nl).append("\tat ").append(stack[i]);
            }
            if (stack.length > framesToPrint) {
                details.append(nl).append("\t... (").append(stack.length - framesToPrint).append(" more)");
            }
        }

        Throwable cause = t.getCause();
        if (cause != null) {
            details.append(System.lineSeparator())
                    .append("caused by: ")
                    .append(cause.getClass().getName())
                    .append(": ")
                    .append(summarizeExceptionMessage(cause.getMessage()));
        }
        return details.toString();
    }

    private String summarizeExceptionMessage(String message) {
        if (StringUtils.isBlank(message)) {
            return message;
        }
        return truncateServerStackTraceField(message);
    }

    private String summarizeResponseBody(String body) {
        if (StringUtils.isBlank(body)) {
            return body;
        }
        return truncateServerStackTraceField(body);
    }

    private String truncateServerStackTraceField(String text) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        Object parsed = JSONValue.parse(text);
        if (parsed instanceof JSONObject obj && obj.containsKey("stack_trace")) {
            JSONObject copy = new JSONObject();
            copy.putAll(obj);
            Object stackTraceObj = obj.get("stack_trace");
            if (stackTraceObj instanceof String stackTrace) {
                copy.put("stack_trace", truncateByLines(stackTrace, MAX_EXCEPTION_STACK_FRAMES));
            }
            return copy.toJSONString();
        }
        return text;
    }

    private String truncateByLines(String text, int maxLines) {
        if (StringUtils.isBlank(text) || maxLines <= 0) {
            return text;
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n");
        if (lines.length <= maxLines) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxLines; i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(lines[i]);
        }
        sb.append('\n').append("... (").append(lines.length - maxLines).append(" more lines)");
        return sb.toString();
    }

    void handlePublishFailures(Run<?, ?> run, List<String> failures, MIAgentPublishSummary summary) {
        summary.setStatus(MIAgentPublishSummary.Status.PARTIAL_FAILURE);
        summary.setFailures(failures);
        summary.setMessage("Publish completed with " + failures.size() + " failure(s).");
        run.setResult(failBuildOnPublishError ? Result.FAILURE : Result.UNSTABLE);
    }

    public static class MIAgentValidationException extends Exception {
        @Serial
        private static final long serialVersionUID = 1L;
        public MIAgentValidationException(String message) { super(message); }
    }

    public static class MIAgentPublishSummary implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        public enum Status { PUBLISHED, PARTIAL_FAILURE, NO_RESULTS, INVALID, ERROR }

        private Status status = Status.NO_RESULTS;
        private String message = "";
        private int totalTests;
        private int publishedFiles;
        private int publishedSteps;
        private List<String> failures = new ArrayList<>();

        public Status getStatus() { return status; }
        public void setStatus(Status status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public int getTotalTests() { return totalTests; }
        public void setTotalTests(int totalTests) { this.totalTests = totalTests; }
        public int getPublishedFiles() { return publishedFiles; }
        public void setPublishedFiles(int publishedFiles) { this.publishedFiles = publishedFiles; }
        public int getPublishedSteps() { return publishedSteps; }
        public void setPublishedSteps(int publishedSteps) { this.publishedSteps = publishedSteps; }
        public List<String> getFailures() { return failures; }
        public void setFailures(List<String> failures) { this.failures = new ArrayList<>(failures); }
    }

    public static class MIAgentPublishSummaryAction extends hudson.model.InvisibleAction implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private final MIAgentPublishSummary summary;
        public MIAgentPublishSummaryAction(MIAgentPublishSummary summary) { this.summary = summary; }
        public MIAgentPublishSummary getSummary() { return summary; }
    }

    @Extension
    @Symbol("miAgentPublisher")
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Publish MI Agent (Autonomous-Tester) results to Software Delivery Management";
        }
    }

    private void initTransientCollaborators() {
        if (octaneClientProvider == null) {
            octaneClientProvider = OctaneSDK::getClientByInstanceId;
        }
        if (octaneRequestExecutor == null) {
            octaneRequestExecutor = (client, request) -> client.getRestService().obtainOctaneRestClient().execute(request);
        }
    }

    @Serial
    private Object readResolve() {
        initTransientCollaborators();
        return this;
    }

    interface OctaneClientProvider {
        OctaneClient getClientByInstanceId(String instanceId);
    }

    interface OctaneRequestExecutor {
        OctaneResponse execute(OctaneClient client, OctaneRequest request) throws IOException;
    }

    private record PublishContext(OctaneClient client, String baseUrl, String sharedSpaceId, String workspaceId) {}

    private record RunPublishData(String runId, FilePath runFolder, JSONObject runResult) {}

    private record RunPublishResult(int publishedSteps, int totalSteps) {}

    private record AttachmentUploadData(FilePath file, String fileName, String ownerField, String ownerType, String ownerId) {}
}
