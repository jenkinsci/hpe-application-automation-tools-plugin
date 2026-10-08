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
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
    public void publisherDefaultsToFailingBuildOnPublishError() {
        MIAgentResultPublisher publisher = new MIAgentResultPublisher();
        assertTrue(publisher.isFailBuildOnPublishError());
    }

    @Test
    public void perform_twoRuns_firstStatusConnectionReset_recoversWithFreshRequestBody() throws Exception {
        Run<?, ?> run = mock(FreeStyleBuild.class);
        FilePath workspace = new FilePath(tempFolder.getRoot());
        FilePath resultRoot = MIAgentConstants.resultRootForBuild(workspace, run);
        FilePath runFolder = resultRoot.child("6024");
        runFolder.mkdirs();

        JSONObject status = new JSONObject();
        status.put("name", "passed");
        JSONObject runResult = new JSONObject();
        runResult.put("native_status", status);
        runFolder.child(MIAgentConstants.RUN_STEPS_RESULT_FILE_NAME)
                .write(runResult.toJSONString(), StandardCharsets.UTF_8.name());
        JSONObject runEntry = new JSONObject();
        runEntry.put("runId", "6024");
        runEntry.put("runFolder", runFolder.getRemote());
        JSONArray runs = new JSONArray();
        runs.add(runEntry);
        FilePath secondRunFolder = resultRoot.child("6025");
        secondRunFolder.mkdirs();
        secondRunFolder.child(MIAgentConstants.RUN_STEPS_RESULT_FILE_NAME)
            .write(runResult.toJSONString(), StandardCharsets.UTF_8.name());
        JSONObject secondRunEntry = new JSONObject();
        secondRunEntry.put("runId", "6025");
        secondRunEntry.put("runFolder", secondRunFolder.getRemote());
        runs.add(secondRunEntry);
        JSONObject manifest = new JSONObject();
        manifest.put("schemaVersion", "1.0");
        manifest.put("runs", runs);
        resultRoot.child(MIAgentConstants.MANIFEST_FILE_NAME)
                .write(manifest.toJSONString(), StandardCharsets.UTF_8.name());

        List<Long> delays = new ArrayList<>();
        MIAgentResultPublisher publisher = publisherWithoutWaiting(delays);
        publisher.setConfigurationId("cfg");
        publisher.setWorkspaceId("2001");
        publisher.setOctaneClientProvider(instanceId -> mockOctaneClient("http://octane.example", "1001"));
        AtomicInteger attempts = new AtomicInteger();
        List<String> bodies = new ArrayList<>();
        List<String> urls = new ArrayList<>();
        publisher.setOctaneRequestExecutor((ignored, request) -> {
            bodies.add(new String(request.getBody().readAllBytes(), StandardCharsets.UTF_8));
            urls.add(extractRequestUrl(request));
            if (attempts.incrementAndGet() == 1) {
                throw new SocketException("Connection reset");
            }
            return mockResponse(200);
        });

        publisher.perform(run, workspace, mock(Launcher.class), mockListener());

        assertEquals(3, attempts.get());
        assertEquals(1, delays.size());
        assertEquals(Long.valueOf(1000L), delays.get(0));
        assertEquals(bodies.get(0), bodies.get(1));
        assertTrue(bodies.get(1).contains("list_node.run_native_status.passed"));
        assertEquals(urls.get(0), urls.get(1));
        assertTrue(urls.get(1).endsWith("/runs/6024"));
        assertTrue(urls.get(2).endsWith("/runs/6025"));
        MIAgentResultPublisher.MIAgentPublishSummary summary = captureSummary(run);
        assertEquals(MIAgentResultPublisher.MIAgentPublishSummary.Status.PUBLISHED, summary.getStatus());
        assertTrue(summary.getFailures().isEmpty());
        verify(run, never()).setResult(any());
    }

    @Test
    public void perform_connectionResetExhaustsRetries_reportsOneFailure() throws Exception {
        RetryFixture fixture = retryFixture(false);
        AtomicInteger attempts = new AtomicInteger();
        fixture.publisher().setOctaneRequestExecutor((ignored, request) -> {
            attempts.incrementAndGet();
            throw new SocketException("Connection reset");
        });

        fixture.publish();

        assertEquals(3, attempts.get());
        assertEquals(2, fixture.delays().size());
        assertEquals(List.of(1000L, 1000L), fixture.delays());
        MIAgentResultPublisher.MIAgentPublishSummary summary = captureSummary(fixture.run());
        assertEquals(MIAgentResultPublisher.MIAgentPublishSummary.Status.PARTIAL_FAILURE, summary.getStatus());
        assertEquals(1, summary.getFailures().size());
        verify(fixture.run()).setResult(Result.FAILURE);
    }

    @Test
    public void perform_httpErrors_areNotRetried() throws Exception {
        for (int status : List.of(400, 401, 403, 404, 422, 429, 500, 503)) {
            RetryFixture fixture = retryFixture(false);
            AtomicInteger attempts = new AtomicInteger();
            fixture.publisher().setOctaneRequestExecutor((ignored, request) -> {
                attempts.incrementAndGet();
                return mockResponse(status);
            });

            fixture.publish();

            assertEquals(1, attempts.get());
            assertTrue(fixture.delays().isEmpty());
            verify(fixture.run()).setResult(Result.FAILURE);
        }
    }

    @Test
    public void perform_nonTransientIOException_isNotRetried() throws Exception {
        RetryFixture fixture = retryFixture(false);
        AtomicInteger attempts = new AtomicInteger();
        fixture.publisher().setOctaneRequestExecutor((ignored, request) -> {
            attempts.incrementAndGet();
            throw new IOException("Invalid request body");
        });

        fixture.publish();

        assertEquals(1, attempts.get());
        assertTrue(fixture.delays().isEmpty());
        verify(fixture.run()).setResult(Result.FAILURE);
    }

    @Test
    public void perform_stepTimeout_recoversWithoutRepeatingRunUpdate() throws Exception {
        RetryFixture fixture = retryFixture(true);
        AtomicInteger runAttempts = new AtomicInteger();
        AtomicInteger stepAttempts = new AtomicInteger();
        fixture.publisher().setOctaneRequestExecutor((ignored, request) -> {
            if (extractRequestUrl(request).endsWith("/runs/6024")) {
                runAttempts.incrementAndGet();
            } else if (stepAttempts.incrementAndGet() == 1) {
                throw new SocketTimeoutException("Read timed out");
            }
            return mockResponse(200);
        });

        fixture.publish();

        assertEquals(1, runAttempts.get());
        assertEquals(2, stepAttempts.get());
        assertEquals(1, captureSummary(fixture.run()).getPublishedSteps());
        assertEquals(MIAgentResultPublisher.MIAgentPublishSummary.Status.PUBLISHED, captureSummary(fixture.run()).getStatus());
        verify(fixture.run(), never()).setResult(any());
    }

    @Test
    public void perform_interruptedRetry_stopsPublication() throws Exception {
        RetryFixture fixture = retryFixture(true);
        AtomicInteger attempts = new AtomicInteger();
        fixture.publisher().setOctaneRequestExecutor((ignored, request) -> {
            attempts.incrementAndGet();
            Thread.currentThread().interrupt();
            throw new SocketException("Connection reset");
        });

        try {
            fixture.publish();
            org.junit.Assert.fail("Expected InterruptedException");
        } catch (InterruptedException expected) {
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
        assertEquals(1, attempts.get());
        assertEquals(MIAgentResultPublisher.MIAgentPublishSummary.Status.ERROR, captureSummary(fixture.run()).getStatus());
    }

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
        FilePath resultRoot = MIAgentConstants.resultRootForBuild(workspace, run);
        resultRoot.mkdirs();

        JSONObject manifest = new JSONObject();
        manifest.put("schemaVersion", "2.0");
        manifest.put("runs", new JSONArray());
        resultRoot.child(MIAgentConstants.MANIFEST_FILE_NAME)
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
        FilePath resultRoot = MIAgentConstants.resultRootForBuild(workspace, run);
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
        resultRoot.child(MIAgentConstants.MANIFEST_FILE_NAME)
                .write(manifest.toJSONString(), StandardCharsets.UTF_8.name());

        OctaneClient client = mockOctaneClient("http://octane.example", "1001");
        List<OctaneRequest> requests = new ArrayList<>();
        MIAgentResultPublisher publisher = new MIAgentResultPublisher();
        publisher.setConfigurationId("cfg");
        publisher.setWorkspaceId("2001");
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
        assertTrue(resultRoot.exists());
        verify(run, never()).setResult(any());
    }

    @Test
    public void perform_nativeStatusByLogicalName_andInvalidSteps_publishOnlyValidStep() throws Exception {
        Run<?, ?> run = mock(FreeStyleBuild.class);
        TaskListener listener = mockListener();
        FilePath workspace = new FilePath(tempFolder.getRoot());
        FilePath resultRoot = MIAgentConstants.resultRootForBuild(workspace, run);
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
        resultRoot.child(MIAgentConstants.MANIFEST_FILE_NAME)
                .write(manifest.toJSONString(), StandardCharsets.UTF_8.name());

        OctaneClient client = mockOctaneClient("http://octane.example", "1001");
        List<OctaneRequest> requests = new ArrayList<>();
        MIAgentResultPublisher publisher = new MIAgentResultPublisher();
        publisher.setConfigurationId("cfg");
        publisher.setWorkspaceId("2001");
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
        FilePath resultRoot = MIAgentConstants.resultRootForBuild(workspace, run);
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
        resultRoot.child(MIAgentConstants.MANIFEST_FILE_NAME)
                .write(manifest.toJSONString(), StandardCharsets.UTF_8.name());

        OctaneClient client = mockOctaneClient("http://octane.example", "1001");
        List<OctaneRequest> requests = new ArrayList<>();
        MIAgentResultPublisher publisher = new MIAgentResultPublisher();
        publisher.setConfigurationId("cfg");
        publisher.setWorkspaceId("2001");
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
        FilePath resultRoot = MIAgentConstants.resultRootForBuild(workspace, run);
        FilePath runFolder = resultRoot.child("2042");
        runFolder.mkdirs();

        JSONObject runResult = new JSONObject();
        JSONObject nativeStatus = new JSONObject();
        nativeStatus.put("name", "passed");
        runResult.put("native_status", nativeStatus);
        runFolder.child(MIAgentConstants.RUN_STEPS_RESULT_FILE_NAME).write(runResult.toJSONString(), StandardCharsets.UTF_8.name());

        JSONObject runEntry = new JSONObject();
        runEntry.put("runId", "2042");
        runEntry.put("runFolder", runFolder.getRemote());
        JSONArray runs = new JSONArray();
        runs.add(runEntry);
        JSONObject manifest = new JSONObject();
        manifest.put("schemaVersion", "1.0");
        manifest.put("runs", runs);
        resultRoot.child(MIAgentConstants.MANIFEST_FILE_NAME)
                .write(manifest.toJSONString(), StandardCharsets.UTF_8.name());

        OctaneClient client = mockOctaneClient("http://octane.example", "1001");
        MIAgentResultPublisher publisher = new MIAgentResultPublisher();
        publisher.setConfigurationId("cfg");
        publisher.setWorkspaceId("2001");
        publisher.setFailBuildOnPublishError(false);
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
        FilePath resultRoot = MIAgentConstants.resultRootForBuild(workspace, run);
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
        resultRoot.child(MIAgentConstants.MANIFEST_FILE_NAME)
                .write(manifest.toJSONString(), StandardCharsets.UTF_8.name());

        OctaneClient client = mockOctaneClient("http://octane.example", "1001");
        List<OctaneRequest> requests = new ArrayList<>();
        MIAgentResultPublisher publisher = new MIAgentResultPublisher();
        publisher.setConfigurationId("cfg");
        publisher.setWorkspaceId("2001");
        publisher.setFailBuildOnPublishError(false);
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
        FilePath resultRoot = MIAgentConstants.resultRootForBuild(workspace, run);
        FilePath runFolder = resultRoot.child("2142");
        FilePath imagesFolder = runFolder.child("images");
        runFolder.mkdirs();
        imagesFolder.mkdirs();

        JSONObject runResult = new JSONObject();
        JSONObject nativeStatus = new JSONObject();
        nativeStatus.put("name", "passed");
        runResult.put("native_status", nativeStatus);
        runFolder.child(MIAgentConstants.RUN_STEPS_RESULT_FILE_NAME).write(runResult.toJSONString(), StandardCharsets.UTF_8.name());
        imagesFolder.child("screenshot_s11_1.jpg").write("dummy", StandardCharsets.UTF_8.name());

        JSONObject runEntry = new JSONObject();
        runEntry.put("runId", "2142");
        runEntry.put("runFolder", runFolder.getRemote());
        JSONArray runs = new JSONArray();
        runs.add(runEntry);
        JSONObject manifest = new JSONObject();
        manifest.put("schemaVersion", "1.0");
        manifest.put("runs", runs);
        resultRoot.child(MIAgentConstants.MANIFEST_FILE_NAME)
                .write(manifest.toJSONString(), StandardCharsets.UTF_8.name());

        OctaneClient client = mockOctaneClient("http://octane.example", "1001");
        MIAgentResultPublisher publisher = new MIAgentResultPublisher();
        publisher.setConfigurationId("cfg");
        publisher.setWorkspaceId("2001");
        publisher.setFailBuildOnPublishError(false);
        publisher.setOctaneClientProvider(instanceId -> client);
        publisher.setOctaneRequestExecutor((ignored, request) -> {
            String url = extractRequestUrl(request);
            if (url != null && url.endsWith("/attachments/bulk")) {
                throw new IOException("Content-Length header already present");
            }
            return mockResponse(200);
        });

        publisher.perform(run, workspace, mock(Launcher.class), listener);

        MIAgentResultPublisher.MIAgentPublishSummary summary = captureSummary(run);
        assertEquals(MIAgentResultPublisher.MIAgentPublishSummary.Status.PARTIAL_FAILURE, summary.getStatus());
        assertEquals(1, summary.getFailures().size());
        assertTrue(summary.getFailures().get(0).contains("Content-Length header already present"));
        verify(run).setResult(Result.UNSTABLE);
    }

    @Test
    public void perform_multipleAttachments_uploadsInSingleBulkRequest() throws Exception {
        Run<?, ?> run = mock(FreeStyleBuild.class);
        TaskListener listener = mockListener();
        FilePath workspace = new FilePath(tempFolder.getRoot());
        FilePath resultRoot = MIAgentConstants.resultRootForBuild(workspace, run);
        FilePath runFolder = resultRoot.child("2242");
        FilePath imagesFolder = runFolder.child("images");
        imagesFolder.mkdirs();

        JSONObject runResult = new JSONObject();
        JSONObject nativeStatus = new JSONObject();
        nativeStatus.put("name", "passed");
        runResult.put("native_status", nativeStatus);
        runFolder.child(MIAgentConstants.RUN_STEPS_RESULT_FILE_NAME).write(runResult.toJSONString(), StandardCharsets.UTF_8.name());
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
        resultRoot.child(MIAgentConstants.MANIFEST_FILE_NAME)
                .write(manifest.toJSONString(), StandardCharsets.UTF_8.name());

        OctaneClient client = mockOctaneClient("http://octane.example", "1001");
        MIAgentResultPublisher publisher = new MIAgentResultPublisher();
        publisher.setConfigurationId("cfg");
        publisher.setWorkspaceId("2001");
        publisher.setOctaneClientProvider(instanceId -> client);

        final int[] bulkAttachmentRequests = {0};
        publisher.setOctaneRequestExecutor((ignored, request) -> {
            String url = extractRequestUrl(request);
            if (url != null && url.endsWith("/attachments/bulk")) {
                bulkAttachmentRequests[0]++;
            }
            return mockResponse(200);
        });

        publisher.perform(run, workspace, mock(Launcher.class), listener);

        assertEquals("Expected a single bulk attachment upload request.", 1, bulkAttachmentRequests[0]);
        MIAgentResultPublisher.MIAgentPublishSummary summary = captureSummary(run);
        assertEquals(MIAgentResultPublisher.MIAgentPublishSummary.Status.PUBLISHED, summary.getStatus());
    }

    @Test
    public void perform_twoAttachmentUploadsFail_setsPartialFailureAndReportsBothFiles() throws Exception {
        Run<?, ?> run = mock(FreeStyleBuild.class);
        TaskListener listener = mockListener();
        FilePath workspace = new FilePath(tempFolder.getRoot());
        FilePath resultRoot = MIAgentConstants.resultRootForBuild(workspace, run);
        FilePath runFolder = resultRoot.child("2342");
        FilePath imagesFolder = runFolder.child("images");
        imagesFolder.mkdirs();

        JSONObject runResult = new JSONObject();
        JSONObject nativeStatus = new JSONObject();
        nativeStatus.put("name", "passed");
        runResult.put("native_status", nativeStatus);
        runFolder.child(MIAgentConstants.RUN_STEPS_RESULT_FILE_NAME).write(runResult.toJSONString(), StandardCharsets.UTF_8.name());
        imagesFolder.child("screenshot_s11_1.jpg").write("img-1", StandardCharsets.UTF_8.name());
        imagesFolder.child("screenshot_s11_2.jpg").write("img-2", StandardCharsets.UTF_8.name());

        JSONObject runEntry = new JSONObject();
        runEntry.put("runId", "2342");
        runEntry.put("runFolder", runFolder.getRemote());
        JSONArray runs = new JSONArray();
        runs.add(runEntry);
        JSONObject manifest = new JSONObject();
        manifest.put("schemaVersion", "1.0");
        manifest.put("runs", runs);
        resultRoot.child(MIAgentConstants.MANIFEST_FILE_NAME)
                .write(manifest.toJSONString(), StandardCharsets.UTF_8.name());

        OctaneClient client = mockOctaneClient("http://octane.example", "1001");
        MIAgentResultPublisher publisher = new MIAgentResultPublisher();
        publisher.setConfigurationId("cfg");
        publisher.setWorkspaceId("2001");
        publisher.setFailBuildOnPublishError(false);
        publisher.setOctaneClientProvider(instanceId -> client);

        publisher.setOctaneRequestExecutor((ignored, request) -> {
            String url = extractRequestUrl(request);
            if (url != null && url.endsWith("/attachments/bulk")) {
                throw new IOException("simulated bulk upload failure");
            }
            return mockResponse(200);
        });

        publisher.perform(run, workspace, mock(Launcher.class), listener);

        MIAgentResultPublisher.MIAgentPublishSummary summary = captureSummary(run);
        assertEquals(MIAgentResultPublisher.MIAgentPublishSummary.Status.PARTIAL_FAILURE, summary.getStatus());
        assertEquals(1, summary.getFailures().size());
        String failure = summary.getFailures().get(0);
        assertTrue(failure.contains("bulk upload failed"));
        assertTrue(failure.contains("screenshot_s11_1.jpg"));
        assertTrue(failure.contains("screenshot_s11_2.jpg"));
        verify(run).setResult(Result.UNSTABLE);
    }

    @Test
    public void perform_attachmentUploadFailureWithServerStackTrace_truncatesStackTraceByLinesInFailureSummary() throws Exception {
        Run<?, ?> run = mock(FreeStyleBuild.class);
        TaskListener listener = mockListener();
        FilePath workspace = new FilePath(tempFolder.getRoot());
        FilePath resultRoot = MIAgentConstants.resultRootForBuild(workspace, run);
        FilePath runFolder = resultRoot.child("2442");
        FilePath imagesFolder = runFolder.child("images");
        runFolder.mkdirs();
        imagesFolder.mkdirs();

        JSONObject runResult = new JSONObject();
        JSONObject nativeStatus = new JSONObject();
        nativeStatus.put("name", "passed");
        runResult.put("native_status", nativeStatus);
        runFolder.child(MIAgentConstants.RUN_STEPS_RESULT_FILE_NAME).write(runResult.toJSONString(), StandardCharsets.UTF_8.name());
        imagesFolder.child("screenshot_s11_1.jpg").write("img-1", StandardCharsets.UTF_8.name());

        JSONObject runEntry = new JSONObject();
        runEntry.put("runId", "2442");
        runEntry.put("runFolder", runFolder.getRemote());
        JSONArray runs = new JSONArray();
        runs.add(runEntry);
        JSONObject manifest = new JSONObject();
        manifest.put("schemaVersion", "1.0");
        manifest.put("runs", runs);
        resultRoot.child(MIAgentConstants.MANIFEST_FILE_NAME)
                .write(manifest.toJSONString(), StandardCharsets.UTF_8.name());

        OctaneClient client = mockOctaneClient("http://octane.example", "1001");
        MIAgentResultPublisher publisher = new MIAgentResultPublisher();
        publisher.setConfigurationId("cfg");
        publisher.setWorkspaceId("2001");
        publisher.setFailBuildOnPublishError(false);
        publisher.setOctaneClientProvider(instanceId -> client);
        publisher.setOctaneRequestExecutor((ignored, request) -> {
            String url = extractRequestUrl(request);
            if (url != null && url.endsWith("/attachments/bulk")) {
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
        FilePath resultRoot = MIAgentConstants.resultRootForBuild(workspace, run);
        resultRoot.mkdirs();

        JSONObject runEntry = new JSONObject();
        runEntry.put("runId", "3042");
        runEntry.put("runFolder", "../outside-3042");
        JSONArray runs = new JSONArray();
        runs.add(runEntry);
        JSONObject manifest = new JSONObject();
        manifest.put("schemaVersion", "1.0");
        manifest.put("runs", runs);
        resultRoot.child(MIAgentConstants.MANIFEST_FILE_NAME)
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

    private MIAgentResultPublisher publisherWithoutWaiting(List<Long> delays) {
        return new MIAgentResultPublisher() {
            @Override
            void waitBeforeRetry(long delayMs) throws InterruptedException {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Result publication interrupted.");
                }
                delays.add(delayMs);
            }
        };
    }

    private RetryFixture retryFixture(boolean includeStep) throws Exception {
        Run<?, ?> run = mock(FreeStyleBuild.class);
        FilePath workspace = new FilePath(tempFolder.newFolder());
        FilePath resultRoot = MIAgentConstants.resultRootForBuild(workspace, run);
        FilePath runFolder = resultRoot.child("6024");
        runFolder.mkdirs();
        JSONObject status = new JSONObject();
        status.put("name", "passed");
        JSONObject result = new JSONObject();
        result.put("native_status", status);
        if (includeStep) {
            JSONObject step = new JSONObject();
            step.put("id", "s1");
            step.put("result", status);
            JSONArray steps = new JSONArray();
            steps.add(step);
            JSONObject runSteps = new JSONObject();
            runSteps.put("data", steps);
            result.put("run_steps", runSteps);
        }
        runFolder.child(MIAgentConstants.RUN_STEPS_RESULT_FILE_NAME).write(result.toJSONString(), StandardCharsets.UTF_8.name());
        JSONObject entry = new JSONObject();
        entry.put("runId", "6024");
        entry.put("runFolder", runFolder.getRemote());
        JSONArray runs = new JSONArray();
        runs.add(entry);
        JSONObject manifest = new JSONObject();
        manifest.put("schemaVersion", "1.0");
        manifest.put("runs", runs);
        resultRoot.child(MIAgentConstants.MANIFEST_FILE_NAME).write(manifest.toJSONString(), StandardCharsets.UTF_8.name());
        List<Long> delays = new ArrayList<>();
        MIAgentResultPublisher publisher = publisherWithoutWaiting(delays);
        publisher.setConfigurationId("cfg");
        publisher.setWorkspaceId("2001");
        publisher.setOctaneClientProvider(instanceId -> mockOctaneClient("http://octane.example", "1001"));
        return new RetryFixture(run, workspace, publisher, delays);
    }

    private record RetryFixture(Run<?, ?> run, FilePath workspace, MIAgentResultPublisher publisher, List<Long> delays) {
        void publish() throws Exception {
            TaskListener listener = mock(TaskListener.class);
            when(listener.getLogger()).thenReturn(new PrintStream(System.out));
            publisher.perform(run, workspace, mock(Launcher.class), listener);
        }
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
