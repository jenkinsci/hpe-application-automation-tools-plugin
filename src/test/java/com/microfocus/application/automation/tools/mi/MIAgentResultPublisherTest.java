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
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Action;
import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MIAgentResultPublisherTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void perform_noResultFolder_addsNoResultsSummary() throws Exception {
        Run<?, ?> run = mock(FreeStyleBuild.class);
        TaskListener listener = mockListener();

        MIAgentResultPublisher publisher = new MIAgentResultPublisher();
        publisher.perform(run, new FilePath(tempFolder.getRoot()), mock(Launcher.class), listener);

        MIAgentResultPublisher.MIAgentPublishSummary summary = captureSummary(run);
        assertEquals(MIAgentResultPublisher.MIAgentPublishSummary.Status.NO_RESULTS, summary.getStatus());
        assertTrue(summary.getMessage().contains("Result folder"));
        verify(run, never()).setResult(any());
    }

    @Test
    public void perform_invalidManifest_setsFailureAndInvalidSummary() throws Exception {
        Run<?, ?> run = mock(FreeStyleBuild.class);
        TaskListener listener = mockListener();
        FilePath workspace = new FilePath(tempFolder.getRoot());
        FilePath resultRoot = workspace.child(MIAgentResultPublisher.DEFAULT_RESULT_FOLDER);
        resultRoot.mkdirs();

        JSONObject manifest = new JSONObject();
        manifest.put("schemaVersion", "2.0");
        manifest.put("runs", new JSONArray());
        resultRoot.child(MIAgentResultPublisher.DEFAULT_MANIFEST_NAME)
                .write(manifest.toJSONString(), StandardCharsets.UTF_8.name());

        MIAgentResultPublisher publisher = new MIAgentResultPublisher();
        publisher.perform(run, workspace, mock(Launcher.class), listener);

        MIAgentResultPublisher.MIAgentPublishSummary summary = captureSummary(run);
        assertEquals(MIAgentResultPublisher.MIAgentPublishSummary.Status.INVALID, summary.getStatus());
        verify(run).setResult(Result.FAILURE);
    }

    @Test
    public void perform_validManifest_publishesRunAndSteps() throws Exception {
        Run<?, ?> run = mock(FreeStyleBuild.class);
        TaskListener listener = mockListener();
        FilePath workspace = new FilePath(tempFolder.getRoot());
        FilePath resultRoot = workspace.child(MIAgentResultPublisher.DEFAULT_RESULT_FOLDER);
        FilePath runFolder = resultRoot.child("1042");
        runFolder.mkdirs();

        JSONObject runResult = new JSONObject();
        JSONObject nativeStatus = new JSONObject();
        nativeStatus.put("name", "passed");
        runResult.put("native_status", nativeStatus);
        JSONObject stepResult = new JSONObject();
        stepResult.put("name", "failed");
        JSONObject step = new JSONObject();
        step.put("id", "s1");
        step.put("result", stepResult);
        step.put("actual", "failed on assert");
        JSONArray steps = new JSONArray();
        steps.add(step);
        JSONObject runSteps = new JSONObject();
        runSteps.put("data", steps);
        runResult.put("run_steps", runSteps);
        runFolder.child("run_steps_result.json").write(runResult.toJSONString(), StandardCharsets.UTF_8.name());

        JSONObject runEntry = new JSONObject();
        runEntry.put("runId", "1042");
        runEntry.put("runFolder", runFolder.getRemote());
        JSONArray runs = new JSONArray();
        runs.add(runEntry);
        JSONObject manifest = new JSONObject();
        manifest.put("schemaVersion", "1.0");
        manifest.put("runs", runs);
        resultRoot.child(MIAgentResultPublisher.DEFAULT_MANIFEST_NAME)
                .write(manifest.toJSONString(), StandardCharsets.UTF_8.name());

        OctaneClient client = mockOctaneClient("http://octane.example", "1001");
        List<OctaneRequest> requests = new ArrayList<>();
        MIAgentResultPublisher publisher = new MIAgentResultPublisher();
        publisher.setConfigurationId("cfg");
        publisher.setWorkspaceId("2001");
        publisher.setUploadAttachments(false);
        publisher.setOctaneClientProvider(instanceId -> client);
        publisher.setOctaneRequestExecutor((ignored, request) -> {
            requests.add(request);
            return mockResponse(200);
        });

        publisher.perform(run, workspace, mock(Launcher.class), listener);

        MIAgentResultPublisher.MIAgentPublishSummary summary = captureSummary(run);
        assertEquals(MIAgentResultPublisher.MIAgentPublishSummary.Status.PUBLISHED, summary.getStatus());
        assertEquals(1, summary.getPublishedSteps());
        assertEquals(1, summary.getTotalTests());
        assertEquals(2, requests.size());
        verify(run, never()).setResult(any());
    }

    @Test
    public void perform_nativeStatusByLogicalName_andInvalidSteps_publishOnlyValidStep() throws Exception {
        Run<?, ?> run = mock(FreeStyleBuild.class);
        TaskListener listener = mockListener();
        FilePath workspace = new FilePath(tempFolder.getRoot());
        FilePath resultRoot = workspace.child(MIAgentResultPublisher.DEFAULT_RESULT_FOLDER);
        FilePath runFolder = resultRoot.child("1142");
        runFolder.mkdirs();

        JSONObject runResult = new JSONObject();
        JSONObject nativeStatus = new JSONObject();
        nativeStatus.put("logical_name", "list_node.run_native_status.skipped");
        runResult.put("native_status", nativeStatus);

        JSONArray steps = new JSONArray();
        JSONObject validStep = new JSONObject();
        validStep.put("id", "s1");
        JSONObject validResult = new JSONObject();
        validResult.put("name", "passed");
        validStep.put("result", validResult);
        validStep.put("actual", "ok");
        steps.add(validStep);

        JSONObject missingResult = new JSONObject();
        missingResult.put("id", "s2");
        steps.add(missingResult);

        JSONObject blankId = new JSONObject();
        blankId.put("id", " ");
        JSONObject blankIdResult = new JSONObject();
        blankIdResult.put("name", "failed");
        blankId.put("result", blankIdResult);
        steps.add(blankId);

        JSONObject runSteps = new JSONObject();
        runSteps.put("data", steps);
        runResult.put("run_steps", runSteps);
        runFolder.child("run_steps_result.json").write(runResult.toJSONString(), StandardCharsets.UTF_8.name());

        JSONObject runEntry = new JSONObject();
        runEntry.put("runId", "1142");
        runEntry.put("runFolder", runFolder.getRemote());
        JSONArray runs = new JSONArray();
        runs.add(runEntry);
        JSONObject manifest = new JSONObject();
        manifest.put("schemaVersion", "1.0");
        manifest.put("runs", runs);
        resultRoot.child(MIAgentResultPublisher.DEFAULT_MANIFEST_NAME)
                .write(manifest.toJSONString(), StandardCharsets.UTF_8.name());

        OctaneClient client = mockOctaneClient("http://octane.example", "1001");
        List<OctaneRequest> requests = new ArrayList<>();
        MIAgentResultPublisher publisher = new MIAgentResultPublisher();
        publisher.setConfigurationId("cfg");
        publisher.setWorkspaceId("2001");
        publisher.setUploadAttachments(false);
        publisher.setOctaneClientProvider(instanceId -> client);
        publisher.setOctaneRequestExecutor((ignored, request) -> {
            requests.add(request);
            return mockResponse(200);
        });

        publisher.perform(run, workspace, mock(Launcher.class), listener);

        MIAgentResultPublisher.MIAgentPublishSummary summary = captureSummary(run);
        assertEquals(MIAgentResultPublisher.MIAgentPublishSummary.Status.PUBLISHED, summary.getStatus());
        assertEquals(3, summary.getTotalTests());
        assertEquals(1, summary.getPublishedSteps());
        assertEquals(2, requests.size());

        JSONObject runUpdateBody = (JSONObject) JSONValue.parse(extractRequestBody(requests.get(0)));
        assertEquals("list_node.run_native_status.skipped",
                ((JSONObject) runUpdateBody.get("native_status")).getAsString("id"));
        JSONObject stepUpdateBody = (JSONObject) JSONValue.parse(extractRequestBody(requests.get(1)));
        assertEquals("list_node.run_native_status.passed",
                ((JSONObject) stepUpdateBody.get("result")).getAsString("id"));
        assertEquals("ok", stepUpdateBody.getAsString("actual"));
    }

    @Test
    public void perform_unknownNativeStatus_fallsBackToFailed() throws Exception {
        Run<?, ?> run = mock(FreeStyleBuild.class);
        TaskListener listener = mockListener();
        FilePath workspace = new FilePath(tempFolder.getRoot());
        FilePath resultRoot = workspace.child(MIAgentResultPublisher.DEFAULT_RESULT_FOLDER);
        FilePath runFolder = resultRoot.child("1242");
        runFolder.mkdirs();

        JSONObject runResult = new JSONObject();
        JSONObject nativeStatus = new JSONObject();
        nativeStatus.put("name", "unknown-status");
        runResult.put("native_status", nativeStatus);
        JSONObject runSteps = new JSONObject();
        runSteps.put("data", new JSONArray());
        runResult.put("run_steps", runSteps);
        runFolder.child("run_steps_result.json").write(runResult.toJSONString(), StandardCharsets.UTF_8.name());

        JSONObject runEntry = new JSONObject();
        runEntry.put("runId", "1242");
        runEntry.put("runFolder", runFolder.getRemote());
        JSONArray runs = new JSONArray();
        runs.add(runEntry);
        JSONObject manifest = new JSONObject();
        manifest.put("schemaVersion", "1.0");
        manifest.put("runs", runs);
        resultRoot.child(MIAgentResultPublisher.DEFAULT_MANIFEST_NAME)
                .write(manifest.toJSONString(), StandardCharsets.UTF_8.name());

        OctaneClient client = mockOctaneClient("http://octane.example", "1001");
        List<OctaneRequest> requests = new ArrayList<>();
        MIAgentResultPublisher publisher = new MIAgentResultPublisher();
        publisher.setConfigurationId("cfg");
        publisher.setWorkspaceId("2001");
        publisher.setUploadAttachments(false);
        publisher.setOctaneClientProvider(instanceId -> client);
        publisher.setOctaneRequestExecutor((ignored, request) -> {
            requests.add(request);
            return mockResponse(200);
        });

        publisher.perform(run, workspace, mock(Launcher.class), listener);

        assertEquals(1, requests.size());
        JSONObject runUpdateBody = (JSONObject) JSONValue.parse(extractRequestBody(requests.get(0)));
        assertEquals("list_node.run_native_status.failed",
                ((JSONObject) runUpdateBody.get("native_status")).getAsString("id"));
    }

    @Test
    public void perform_publishErrorWithFailBuildDisabled_setsUnstableAndPartialFailure() throws Exception {
        Run<?, ?> run = mock(FreeStyleBuild.class);
        TaskListener listener = mockListener();
        FilePath workspace = new FilePath(tempFolder.getRoot());
        FilePath resultRoot = workspace.child(MIAgentResultPublisher.DEFAULT_RESULT_FOLDER);
        FilePath runFolder = resultRoot.child("2042");
        runFolder.mkdirs();

        JSONObject runResult = new JSONObject();
        JSONObject nativeStatus = new JSONObject();
        nativeStatus.put("name", "passed");
        runResult.put("native_status", nativeStatus);
        runFolder.child("run_steps_result.json").write(runResult.toJSONString(), StandardCharsets.UTF_8.name());

        JSONObject runEntry = new JSONObject();
        runEntry.put("runId", "2042");
        runEntry.put("runFolder", runFolder.getRemote());
        JSONArray runs = new JSONArray();
        runs.add(runEntry);
        JSONObject manifest = new JSONObject();
        manifest.put("schemaVersion", "1.0");
        manifest.put("runs", runs);
        resultRoot.child(MIAgentResultPublisher.DEFAULT_MANIFEST_NAME)
                .write(manifest.toJSONString(), StandardCharsets.UTF_8.name());

        OctaneClient client = mockOctaneClient("http://octane.example", "1001");
        MIAgentResultPublisher publisher = new MIAgentResultPublisher();
        publisher.setConfigurationId("cfg");
        publisher.setWorkspaceId("2001");
        publisher.setFailBuildOnPublishError(false);
        publisher.setUploadAttachments(false);
        publisher.setOctaneClientProvider(instanceId -> client);
        publisher.setOctaneRequestExecutor((ignored, request) -> mockResponse(500));

        publisher.perform(run, workspace, mock(Launcher.class), listener);

        MIAgentResultPublisher.MIAgentPublishSummary summary = captureSummary(run);
        assertEquals(MIAgentResultPublisher.MIAgentPublishSummary.Status.PARTIAL_FAILURE, summary.getStatus());
        assertEquals(1, summary.getFailures().size());
        verify(run).setResult(Result.UNSTABLE);
    }

    @Test
    public void perform_stepUpdateFailure_continuesOtherStepsAndReportsPartialFailure() throws Exception {
        Run<?, ?> run = mock(FreeStyleBuild.class);
        TaskListener listener = mockListener();
        FilePath workspace = new FilePath(tempFolder.getRoot());
        FilePath resultRoot = workspace.child(MIAgentResultPublisher.DEFAULT_RESULT_FOLDER);
        FilePath runFolder = resultRoot.child("2099");
        runFolder.mkdirs();

        JSONObject runResult = new JSONObject();
        JSONObject nativeStatus = new JSONObject();
        nativeStatus.put("name", "passed");
        runResult.put("native_status", nativeStatus);

        JSONArray steps = new JSONArray();
        JSONObject step1 = new JSONObject();
        step1.put("id", "s1");
        JSONObject step1Status = new JSONObject();
        step1Status.put("name", "passed");
        step1.put("result", step1Status);
        step1.put("actual", "first");
        steps.add(step1);

        JSONObject step2 = new JSONObject();
        step2.put("id", "s2");
        JSONObject step2Status = new JSONObject();
        step2Status.put("name", "passed");
        step2.put("result", step2Status);
        step2.put("actual", "second");
        steps.add(step2);

        JSONObject runSteps = new JSONObject();
        runSteps.put("data", steps);
        runResult.put("run_steps", runSteps);
        runFolder.child("run_steps_result.json").write(runResult.toJSONString(), StandardCharsets.UTF_8.name());

        JSONObject runEntry = new JSONObject();
        runEntry.put("runId", "2099");
        runEntry.put("runFolder", runFolder.getRemote());
        JSONArray runs = new JSONArray();
        runs.add(runEntry);
        JSONObject manifest = new JSONObject();
        manifest.put("schemaVersion", "1.0");
        manifest.put("runs", runs);
        resultRoot.child(MIAgentResultPublisher.DEFAULT_MANIFEST_NAME)
                .write(manifest.toJSONString(), StandardCharsets.UTF_8.name());

        OctaneClient client = mockOctaneClient("http://octane.example", "1001");
        List<OctaneRequest> requests = new ArrayList<>();
        MIAgentResultPublisher publisher = new MIAgentResultPublisher();
        publisher.setConfigurationId("cfg");
        publisher.setWorkspaceId("2001");
        publisher.setFailBuildOnPublishError(false);
        publisher.setUploadAttachments(false);
        publisher.setOctaneClientProvider(instanceId -> client);
        publisher.setOctaneRequestExecutor((ignored, request) -> {
            requests.add(request);
            String url = extractRequestUrl(request);
            if (url != null && url.endsWith("/run_steps/s1")) {
                return mockResponse(500);
            }
            return mockResponse(200);
        });

        publisher.perform(run, workspace, mock(Launcher.class), listener);

        MIAgentResultPublisher.MIAgentPublishSummary summary = captureSummary(run);
        assertEquals(MIAgentResultPublisher.MIAgentPublishSummary.Status.PARTIAL_FAILURE, summary.getStatus());
        assertEquals(2, summary.getTotalTests());
        assertEquals(1, summary.getPublishedSteps());
        assertEquals(1, summary.getFailures().size());
        assertTrue(summary.getFailures().get(0).contains("step s1 update failed"));
        assertEquals(3, requests.size());
        verify(run).setResult(Result.UNSTABLE);
    }

    @Test
    public void perform_attachmentUploadContentLengthAlreadySet_setsPartialFailure() throws Exception {
        Run<?, ?> run = mock(FreeStyleBuild.class);
        TaskListener listener = mockListener();
        FilePath workspace = new FilePath(tempFolder.getRoot());
        FilePath resultRoot = workspace.child(MIAgentResultPublisher.DEFAULT_RESULT_FOLDER);
        FilePath runFolder = resultRoot.child("2142");
        runFolder.mkdirs();

        JSONObject runResult = new JSONObject();
        JSONObject nativeStatus = new JSONObject();
        nativeStatus.put("name", "passed");
        runResult.put("native_status", nativeStatus);
        runFolder.child("run_steps_result.json").write(runResult.toJSONString(), StandardCharsets.UTF_8.name());
        runFolder.child("recording.mp4").write("dummy", StandardCharsets.UTF_8.name());

        JSONObject runEntry = new JSONObject();
        runEntry.put("runId", "2142");
        runEntry.put("runFolder", runFolder.getRemote());
        JSONArray runs = new JSONArray();
        runs.add(runEntry);
        JSONObject manifest = new JSONObject();
        manifest.put("schemaVersion", "1.0");
        manifest.put("runs", runs);
        resultRoot.child(MIAgentResultPublisher.DEFAULT_MANIFEST_NAME)
                .write(manifest.toJSONString(), StandardCharsets.UTF_8.name());

        OctaneClient client = mockOctaneClient("http://octane.example", "1001");
        MIAgentResultPublisher publisher = new MIAgentResultPublisher();
        publisher.setConfigurationId("cfg");
        publisher.setWorkspaceId("2001");
        publisher.setFailBuildOnPublishError(false);
        publisher.setUploadAttachments(true);
        publisher.setOctaneClientProvider(instanceId -> client);
        final int[] requestCount = {0};
        publisher.setOctaneRequestExecutor((ignored, request) -> {
            requestCount[0]++;
            if (requestCount[0] == 2) {
                throw new IOException("Content-Length header already present");
            }
            return mockResponse(200);
        });

        publisher.perform(run, workspace, mock(Launcher.class), listener);

        MIAgentResultPublisher.MIAgentPublishSummary summary = captureSummary(run);
        assertEquals(MIAgentResultPublisher.MIAgentPublishSummary.Status.PARTIAL_FAILURE, summary.getStatus());
        assertEquals(1, summary.getFailures().size());
        assertTrue(summary.getFailures().get(0).contains("recording.mp4"));
        verify(run).setResult(Result.UNSTABLE);
    }

    @Test
    public void perform_multipleAttachments_uploadsInParallel() throws Exception {
        Run<?, ?> run = mock(FreeStyleBuild.class);
        TaskListener listener = mockListener();
        FilePath workspace = new FilePath(tempFolder.getRoot());
        FilePath resultRoot = workspace.child(MIAgentResultPublisher.DEFAULT_RESULT_FOLDER);
        FilePath runFolder = resultRoot.child("2242");
        FilePath imagesFolder = runFolder.child("images");
        imagesFolder.mkdirs();

        JSONObject runResult = new JSONObject();
        JSONObject nativeStatus = new JSONObject();
        nativeStatus.put("name", "passed");
        runResult.put("native_status", nativeStatus);
        runFolder.child("run_steps_result.json").write(runResult.toJSONString(), StandardCharsets.UTF_8.name());
        runFolder.child("recording.mp4").write("dummy-video", StandardCharsets.UTF_8.name());
        imagesFolder.child("screenshot_s11_1.jpg").write("img-1", StandardCharsets.UTF_8.name());
        imagesFolder.child("screenshot_s12_2.jpg").write("img-2", StandardCharsets.UTF_8.name());

        JSONObject runEntry = new JSONObject();
        runEntry.put("runId", "2242");
        runEntry.put("runFolder", runFolder.getRemote());
        JSONArray runs = new JSONArray();
        runs.add(runEntry);
        JSONObject manifest = new JSONObject();
        manifest.put("schemaVersion", "1.0");
        manifest.put("runs", runs);
        resultRoot.child(MIAgentResultPublisher.DEFAULT_MANIFEST_NAME)
                .write(manifest.toJSONString(), StandardCharsets.UTF_8.name());

        OctaneClient client = mockOctaneClient("http://octane.example", "1001");
        MIAgentResultPublisher publisher = new MIAgentResultPublisher();
        publisher.setConfigurationId("cfg");
        publisher.setWorkspaceId("2001");
        publisher.setUploadAttachments(true);
        publisher.setOctaneClientProvider(instanceId -> client);

        CountDownLatch attachmentsStarted = new CountDownLatch(2);
        AtomicInteger inFlightAttachments = new AtomicInteger(0);
        AtomicInteger maxInFlightAttachments = new AtomicInteger(0);
        publisher.setOctaneRequestExecutor((ignored, request) -> {
            String url = extractRequestUrl(request);
            if (url != null && url.endsWith("/attachments")) {
                int currentInFlight = inFlightAttachments.incrementAndGet();
                maxInFlightAttachments.updateAndGet(previous -> Math.max(previous, currentInFlight));
                try {
                    attachmentsStarted.countDown();
                    if (!attachmentsStarted.await(2, TimeUnit.SECONDS)) {
                        throw new IOException("Attachment uploads did not overlap.");
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    inFlightAttachments.decrementAndGet();
                }
            }
            return mockResponse(200);
        });

        publisher.perform(run, workspace, mock(Launcher.class), listener);

        assertTrue("Expected at least two concurrent attachment uploads.", maxInFlightAttachments.get() >= 2);
        MIAgentResultPublisher.MIAgentPublishSummary summary = captureSummary(run);
        assertEquals(MIAgentResultPublisher.MIAgentPublishSummary.Status.PUBLISHED, summary.getStatus());
    }

    @Test
    public void perform_twoAttachmentUploadsFail_setsPartialFailureAndReportsBothFiles() throws Exception {
        Run<?, ?> run = mock(FreeStyleBuild.class);
        TaskListener listener = mockListener();
        FilePath workspace = new FilePath(tempFolder.getRoot());
        FilePath resultRoot = workspace.child(MIAgentResultPublisher.DEFAULT_RESULT_FOLDER);
        FilePath runFolder = resultRoot.child("2342");
        FilePath imagesFolder = runFolder.child("images");
        imagesFolder.mkdirs();

        JSONObject runResult = new JSONObject();
        JSONObject nativeStatus = new JSONObject();
        nativeStatus.put("name", "passed");
        runResult.put("native_status", nativeStatus);
        runFolder.child("run_steps_result.json").write(runResult.toJSONString(), StandardCharsets.UTF_8.name());
        runFolder.child("recording.mp4").write("dummy-video", StandardCharsets.UTF_8.name());
        imagesFolder.child("screenshot_s11_1.jpg").write("img-1", StandardCharsets.UTF_8.name());

        JSONObject runEntry = new JSONObject();
        runEntry.put("runId", "2342");
        runEntry.put("runFolder", runFolder.getRemote());
        JSONArray runs = new JSONArray();
        runs.add(runEntry);
        JSONObject manifest = new JSONObject();
        manifest.put("schemaVersion", "1.0");
        manifest.put("runs", runs);
        resultRoot.child(MIAgentResultPublisher.DEFAULT_MANIFEST_NAME)
                .write(manifest.toJSONString(), StandardCharsets.UTF_8.name());

        OctaneClient client = mockOctaneClient("http://octane.example", "1001");
        MIAgentResultPublisher publisher = new MIAgentResultPublisher();
        publisher.setConfigurationId("cfg");
        publisher.setWorkspaceId("2001");
        publisher.setFailBuildOnPublishError(false);
        publisher.setUploadAttachments(true);
        publisher.setOctaneClientProvider(instanceId -> client);

        AtomicInteger attachmentRequests = new AtomicInteger(0);
        publisher.setOctaneRequestExecutor((ignored, request) -> {
            String url = extractRequestUrl(request);
            if (url != null && url.endsWith("/attachments")) {
                int idx = attachmentRequests.incrementAndGet();
                throw new IOException("simulated upload failure " + idx);
            }
            return mockResponse(200);
        });

        publisher.perform(run, workspace, mock(Launcher.class), listener);

        MIAgentResultPublisher.MIAgentPublishSummary summary = captureSummary(run);
        assertEquals(MIAgentResultPublisher.MIAgentPublishSummary.Status.PARTIAL_FAILURE, summary.getStatus());
        assertEquals(2, summary.getFailures().size());
        assertTrue(summary.getFailures().stream().anyMatch(f -> f.contains("recording.mp4")));
        assertTrue(summary.getFailures().stream().anyMatch(f -> f.contains("screenshot_s11_1.jpg")));
        verify(run).setResult(Result.UNSTABLE);
    }

    @Test
    public void perform_attachmentUploadFailureWithServerStackTrace_truncatesStackTraceByLinesInFailureSummary() throws Exception {
        Run<?, ?> run = mock(FreeStyleBuild.class);
        TaskListener listener = mockListener();
        FilePath workspace = new FilePath(tempFolder.getRoot());
        FilePath resultRoot = workspace.child(MIAgentResultPublisher.DEFAULT_RESULT_FOLDER);
        FilePath runFolder = resultRoot.child("2442");
        runFolder.mkdirs();

        JSONObject runResult = new JSONObject();
        JSONObject nativeStatus = new JSONObject();
        nativeStatus.put("name", "passed");
        runResult.put("native_status", nativeStatus);
        runFolder.child("run_steps_result.json").write(runResult.toJSONString(), StandardCharsets.UTF_8.name());
        runFolder.child("recording.mp4").write("dummy-video", StandardCharsets.UTF_8.name());

        JSONObject runEntry = new JSONObject();
        runEntry.put("runId", "2442");
        runEntry.put("runFolder", runFolder.getRemote());
        JSONArray runs = new JSONArray();
        runs.add(runEntry);
        JSONObject manifest = new JSONObject();
        manifest.put("schemaVersion", "1.0");
        manifest.put("runs", runs);
        resultRoot.child(MIAgentResultPublisher.DEFAULT_MANIFEST_NAME)
                .write(manifest.toJSONString(), StandardCharsets.UTF_8.name());

        OctaneClient client = mockOctaneClient("http://octane.example", "1001");
        MIAgentResultPublisher publisher = new MIAgentResultPublisher();
        publisher.setConfigurationId("cfg");
        publisher.setWorkspaceId("2001");
        publisher.setFailBuildOnPublishError(false);
        publisher.setUploadAttachments(true);
        publisher.setOctaneClientProvider(instanceId -> client);
        publisher.setOctaneRequestExecutor((ignored, request) -> {
            String url = extractRequestUrl(request);
            if (url != null && url.endsWith("/attachments")) {
                return mockResponse(500,
                        "{\"error_code\":\"platform.general_error\",\"stack_trace\":\"l1\\nl2\\nl3\\nl4\\nl5\\nl6\\nl7\\nl8\\nl9\",\"description\":\"attachment failed\"}");
            }
            return mockResponse(200);
        });

        publisher.perform(run, workspace, mock(Launcher.class), listener);

        MIAgentResultPublisher.MIAgentPublishSummary summary = captureSummary(run);
        assertEquals(MIAgentResultPublisher.MIAgentPublishSummary.Status.PARTIAL_FAILURE, summary.getStatus());
        assertEquals(1, summary.getFailures().size());
        String failure = summary.getFailures().get(0);
        assertTrue(failure.contains("recording.mp4"));
        assertTrue(failure.contains("\"stack_trace\":\"l1\\nl2\\nl3\\nl4\\nl5\\nl6\\nl7\\n... (2 more lines)\""));
        assertTrue(!failure.contains("l8\\n"));
        assertTrue(!failure.contains("\\nl9"));
        verify(run).setResult(Result.UNSTABLE);
    }

    @Test
    public void perform_manifestRunFolderOutsideResultRoot_setsInvalidAndFailure() throws Exception {
        Run<?, ?> run = mock(FreeStyleBuild.class);
        TaskListener listener = mockListener();
        FilePath workspace = new FilePath(tempFolder.getRoot());
        FilePath resultRoot = workspace.child(MIAgentResultPublisher.DEFAULT_RESULT_FOLDER);
        resultRoot.mkdirs();

        JSONObject runEntry = new JSONObject();
        runEntry.put("runId", "3042");
        runEntry.put("runFolder", "../outside-3042");
        JSONArray runs = new JSONArray();
        runs.add(runEntry);
        JSONObject manifest = new JSONObject();
        manifest.put("schemaVersion", "1.0");
        manifest.put("runs", runs);
        resultRoot.child(MIAgentResultPublisher.DEFAULT_MANIFEST_NAME)
                .write(manifest.toJSONString(), StandardCharsets.UTF_8.name());

        MIAgentResultPublisher publisher = new MIAgentResultPublisher();
        publisher.setConfigurationId("cfg");
        publisher.setWorkspaceId("2001");
        publisher.setOctaneClientProvider(instanceId -> mockOctaneClient("http://octane.example", "1001"));

        publisher.perform(run, workspace, mock(Launcher.class), listener);

        MIAgentResultPublisher.MIAgentPublishSummary summary = captureSummary(run);
        assertEquals(MIAgentResultPublisher.MIAgentPublishSummary.Status.INVALID, summary.getStatus());
        assertTrue(summary.getMessage().contains("outside result root"));
        verify(run).setResult(Result.FAILURE);
    }

    private MIAgentResultPublisher.MIAgentPublishSummary captureSummary(Run<?, ?> run) {
        ArgumentCaptor<Action> actionCaptor = ArgumentCaptor.forClass(Action.class);
        verify(run).addAction(actionCaptor.capture());
        MIAgentResultPublisher.MIAgentPublishSummaryAction action =
                (MIAgentResultPublisher.MIAgentPublishSummaryAction) actionCaptor.getValue();
        return action.getSummary();
    }

    private OctaneClient mockOctaneClient(String url, String sharedSpace) {
        OctaneClient client = mock(OctaneClient.class, RETURNS_DEEP_STUBS);
        OctaneConfiguration conf = mock(OctaneConfiguration.class);
        when(client.getConfigurationService().getConfiguration()).thenReturn(conf);
        when(conf.getUrl()).thenReturn(url);
        when(conf.getSharedSpace()).thenReturn(sharedSpace);
        return client;
    }

    private OctaneResponse mockResponse(int statusCode) {
        return mockResponse(statusCode, "{}");
    }

    private OctaneResponse mockResponse(int statusCode, String body) {
        OctaneResponse response = mock(OctaneResponse.class);
        when(response.getStatus()).thenReturn(statusCode);
        when(response.getBody()).thenReturn(body);
        return response;
    }

    private String extractRequestBody(OctaneRequest request) throws Exception {
        Method getter = request.getClass().getDeclaredMethod("getBody");
        getter.setAccessible(true);
        Object body = getter.invoke(request);
        if (body == null) {
            return null;
        }
        if (body instanceof String) {
            return (String) body;
        }
        if (body instanceof InputStream) {
            return new String(((InputStream) body).readAllBytes(), StandardCharsets.UTF_8);
        }
        return String.valueOf(body);
    }

    private String extractRequestUrl(OctaneRequest request) throws IOException {
        try {
            Method getter = request.getClass().getDeclaredMethod("getUrl");
            getter.setAccessible(true);
            Object url = getter.invoke(request);
            return url == null ? null : String.valueOf(url);
        } catch (Exception e) {
            throw new IOException("Failed reading request URL.", e);
        }
    }

    private TaskListener mockListener() {
        TaskListener listener = mock(TaskListener.class);
        when(listener.getLogger()).thenReturn(new PrintStream(System.out));
        return listener;
    }
}
