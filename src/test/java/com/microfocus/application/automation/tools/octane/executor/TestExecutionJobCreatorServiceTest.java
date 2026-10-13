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
package com.microfocus.application.automation.tools.octane.executor;

import com.microfocus.application.automation.tools.mi.MIAgentConstants;
import com.microfocus.application.automation.tools.mi.AuTeConfigurationStore;
import com.microfocus.application.automation.tools.mi.MIAgentResultPublisher;
import com.microfocus.application.automation.tools.run.AuTeConfigurationSnapshotBuilder;
import com.microfocus.application.automation.tools.run.MiAgentPreflightBuilder;
import com.hp.octane.integrations.OctaneClient;
import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.executor.DiscoveryInfo;
import com.hp.octane.integrations.dto.executor.impl.TestingToolType;
import com.microfocus.application.automation.tools.mi.MIAgentWorkspaceCleanupPublisher;
import com.microfocus.application.automation.tools.model.LoggedJenkinsRule;
import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.tasks.ArtifactArchiver;
import org.junit.Before;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.TestBuilder;

import java.lang.reflect.Field;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.microfocus.application.automation.tools.octane.executor.UftConstants.AUTONOMOUS_TESTER_LABEL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

/**
 * Verifies that Autonomous Tester jobs are restricted to nodes with the dedicated Autonomous Tester
 * label, whatever its casing.
 */
public class TestExecutionJobCreatorServiceTest {

    @Rule
    public LoggedJenkinsRule jenkins = new LoggedJenkinsRule();

    private Map<OctaneConfiguration, OctaneClient> clients;
    private OctaneConfiguration connection;

    @Before
    public void registerOctaneConnection() throws Exception {
        Field field = OctaneSDK.class.getDeclaredField("clients");
        field.setAccessible(true);
        clients = (Map<OctaneConfiguration, OctaneClient>) field.get(null);
        connection = OctaneConfiguration.create("aute-lifecycle", "http://octane.example", "1001");
        OctaneClient client = mock(OctaneClient.class, RETURNS_DEEP_STUBS);
        when(client.getConfigurationService().getConfiguration()).thenReturn(connection);
        clients.put(connection, client);
    }

    @After
    public void removeOctaneConnection() {
        if (clients != null) {
            clients.remove(connection);
        }
    }

    @Test
    public void createExecutor_createsAndPersistsTemplateAndLifecycleBeforeAnyBuild() throws Exception {
        FreeStyleProject first = createAuTeJob("1");
        assertNotNull(first);
        assertTrue(first.getBuilds().isEmpty());
        assertTrue(Files.isRegularFile(jenkins.jenkins.getRootDir().toPath()
            .resolve("aute-config/aute-lifecycle/1001/default-configuration.json")));
        var template = AuTeConfigurationStore.ensureTemplate("aute-lifecycle", "1001");
        assertEquals(5, ((net.minidev.json.JSONObject) net.minidev.json.JSONValue.parse(Files.readString(template))).size());
        Files.writeString(template, "{\"administrator\":true}");
        assertNotNull(createAuTeJob("2"));
        assertEquals("{\"administrator\":true}", Files.readString(template));
        first.doReload();
        assertTrue(first.getBuildersList().get(0) instanceof AuTeConfigurationSnapshotBuilder);
        assertTrue(first.getBuildersList().get(1) instanceof MiAgentPreflightBuilder);
        assertNotNull(first.getPublishersList().get(MIAgentWorkspaceCleanupPublisher.class));
    }

    @Test
    public void lifecycle_copiesToRemoteAgentArchivesAndDeletesOnlyAllocatedWorkspace() throws Exception {
        FreeStyleProject project = lifecycleJob("remote");
        var agent = jenkins.createOnlineSlave();
        project.setAssignedLabel(agent.getSelfLabel());
        FilePath expectedWorkspace = agent.getWorkspaceFor(project);
        FilePath executable = expectedWorkspace.getParent().child("mi-agent.exe");
        executable.getParent().mkdirs();
        executable.write("shared executable", "UTF-8");
        FilePath sibling = expectedWorkspace.withSuffix("@2");
        sibling.mkdirs();
        sibling.child("active.txt").write("other build", "UTF-8");
        byte[] edited = "{\"BROWSER\":{\"BROWSER_LOCALE\":\"en-US\"}}".getBytes(StandardCharsets.UTF_8);
        var template = AuTeConfigurationStore.ensureTemplate("aute-lifecycle", "1001");
        Files.write(template, edited);
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(hudson.model.AbstractBuild<?, ?> build, hudson.Launcher launcher,
                                   hudson.model.BuildListener listener) throws IOException, InterruptedException {
                FilePath result = MIAgentConstants.resultRoot(build.getWorkspace());
                result.mkdirs();
                result.child("result.json").write("{}", "UTF-8");
                hudson.slaves.WorkspaceList.tempDir(build.getWorkspace()).child("scratch.txt").write("tmp", "UTF-8");
                return true;
            }
        });
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        assertSnapshot(build, edited);
        assertFalse(hudson.slaves.WorkspaceList.tempDir(expectedWorkspace).exists());
        assertTrue(build.getArtifactManager().root().child("mi-agent-results/result.json").isFile());
        assertFalse(expectedWorkspace.exists());
        assertTrue(sibling.child("active.txt").exists());
        assertTrue(executable.exists());
        assertArrayEquals(edited, Files.readAllBytes(template));
    }

    @Test
    public void lifecycle_failedPreflightStillArchivesSnapshotAndCleansWorkspace() throws Exception {
        FreeStyleProject project = lifecycleJob("preflight");
        project.getBuildersList().add(new MiAgentPreflightBuilder());
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.FAILURE, build);
        assertSnapshot(build, Files.readAllBytes(AuTeConfigurationStore.ensureTemplate("aute-lifecycle", "1001")));
        assertFalse(build.getWorkspace().exists());
    }

    @Test
    public void lifecycle_failedConfigurationCopyIsRecordedAsSetupFailure() throws Exception {
        FreeStyleProject project = lifecycleJob("copy-failure");
        project.getBuildersList().clear();
        project.getBuildersList().add(new AuTeConfigurationSnapshotBuilder("../outside", "1001"));
        project.getBuildersList().add(new MiAgentPreflightBuilder());
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.FAILURE, build);
        var failure = build.getAction(com.microfocus.application.automation.tools.mi.AuTeSetupFailure.class);
        assertNotNull(failure);
        assertTrue(failure.getReason().contains("Failed to copy the shared space default configuration"));
        jenkins.assertLogNotContains("AuTe Preflight Check", build);
    }

    @Test
    public void lifecycle_failedArchiveStillDeletesWorkspace() throws Exception {
        FreeStyleProject project = lifecycleJob("archive-failure");
        project.getPublishersList().remove(ArtifactArchiver.class);
        project.getPublishersList().remove(MIAgentWorkspaceCleanupPublisher.class);
        // Matches nothing and does not allow an empty archive, so archiving fails.
        project.getPublishersList().add(new ArtifactArchiver("missing/**"));
        invokeAddMiAgentWorkspaceCleanupIfNeeded(project);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.FAILURE, build);
        assertFalse(build.getWorkspace().exists());
    }

    @Test
    public void lifecycle_concurrentBuildsKeepSeparateSnapshotsAndWorkspaceLeases() throws Exception {
        jenkins.jenkins.setNumExecutors(2);
        FreeStyleProject project = lifecycleJob("concurrent");
        project.setConcurrentBuild(true);
        project.addProperty(new hudson.model.ParametersDefinitionProperty(
            new hudson.model.StringParameterDefinition("buildToken", "")));
        CountDownLatch staged = new CountDownLatch(2);
        Map<Integer, FilePath> workspaces = new ConcurrentHashMap<>();
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(hudson.model.AbstractBuild<?, ?> build, hudson.Launcher launcher,
                                   hudson.model.BuildListener listener) throws IOException, InterruptedException {
                workspaces.put(build.getNumber(), build.getWorkspace());
                staged.countDown();
                assertTrue(staged.await(60, TimeUnit.SECONDS));
                assertTrue(build.getWorkspace().child("default-configuration.json").exists());
                return true;
            }
        });
        var firstFuture = project.scheduleBuild2(0, new hudson.model.ParametersAction(
            new hudson.model.StringParameterValue("buildToken", "first")));
        var secondFuture = project.scheduleBuild2(0, new hudson.model.ParametersAction(
            new hudson.model.StringParameterValue("buildToken", "second")));
        FreeStyleBuild first = firstFuture.get(90, TimeUnit.SECONDS);
        FreeStyleBuild second = secondFuture.get(90, TimeUnit.SECONDS);
        jenkins.assertBuildStatusSuccess(first);
        jenkins.assertBuildStatusSuccess(second);
        assertFalse(workspaces.get(first.getNumber()).equals(workspaces.get(second.getNumber())));
        byte[] template = Files.readAllBytes(AuTeConfigurationStore.ensureTemplate("aute-lifecycle", "1001"));
        assertSnapshot(first, template);
        assertSnapshot(second, template);
        assertFalse(workspaces.get(first.getNumber()).exists());
        assertFalse(workspaces.get(second.getNumber()).exists());
    }

    private FreeStyleProject createAuTeJob(String id) {
        DiscoveryInfo info = mock(DiscoveryInfo.class);
        when(info.getConfigurationId()).thenReturn("aute-lifecycle");
        when(info.getTestingToolType()).thenReturn(TestingToolType.MI_AGENT);
        when(info.getExecutorId()).thenReturn(id);
        when(info.getExecutorLogicalName()).thenReturn("logical-" + id);
        when(info.getWorkspaceId()).thenReturn("2001");
        return TestExecutionJobCreatorService.createExecutor(info);
    }

    @Test
    public void configurationRoundTrip_preservesSnapshotIds() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("configuration-roundtrip");
        project.getBuildersList().add(new AuTeConfigurationSnapshotBuilder("aute-lifecycle", "1001"));
        jenkins.configRoundtrip(project);
        AuTeConfigurationSnapshotBuilder snapshot = project.getBuildersList().get(AuTeConfigurationSnapshotBuilder.class);
        assertEquals("aute-lifecycle", snapshot.getConfigurationId());
        assertEquals("1001", snapshot.getSpaceId());
    }

    private FreeStyleProject lifecycleJob(String id) throws Exception {
        FreeStyleProject project = createAuTeJob(id);
        assertNotNull(project);
        project.setAssignedLabel(null);
        project.getBuildersList().clear();
        project.getBuildersList().add(new AuTeConfigurationSnapshotBuilder("aute-lifecycle", "1001"));
        project.getPublishersList().remove(MIAgentResultPublisher.class);
        return project;
    }

    private void assertSnapshot(FreeStyleBuild build, byte[] expected) throws Exception {
        try (var input = build.getArtifactManager().root().child(AuTeConfigurationStore.SNAPSHOT_PATH).open()) {
            assertArrayEquals(expected, input.readAllBytes());
        }
    }

    @Test
    public void addAutonomousTesterAssignedNode_matchesOnlyNodesWithTheLabel() throws Exception {
        FreeStyleProject proj = jenkins.createFreeStyleProject("aute-job");
        invokeAddAutonomousTesterAssignedNode(proj);

        assertEquals(AUTONOMOUS_TESTER_LABEL, proj.getAssignedLabelString());

        assertTrue(matches(proj, jenkins.createSlave("at-node-1", "AUTONOMOUS_TESTER", null)));
        assertTrue(matches(proj, jenkins.createSlave("at-node-2", "AT", null)));
        assertFalse(matches(proj, jenkins.createSlave("uft-node", "UFT", null)));

    }

    private static boolean matches(FreeStyleProject proj, Node node) {
        return proj.getAssignedLabel().matches(node);
    }

    @Test
    public void addMiAgentArtifactArchiverIfNeeded_archivesOnlyThisBuildsResultsAndIsIdempotent() throws Exception {
        FreeStyleProject proj = jenkins.createFreeStyleProject("mi-agent-archiver-job");

        invokeAddMiAgentArtifactArchiverIfNeeded(proj);
        invokeAddMiAgentArtifactArchiverIfNeeded(proj);

        List<ArtifactArchiver> archivers = proj.getPublishersList().getAll(ArtifactArchiver.class);
        assertEquals(1, archivers.size());
        ArtifactArchiver archiver = archivers.get(0);
        assertEquals(MIAgentConstants.RESULT_FOLDER + "/**,"
            + com.microfocus.application.automation.tools.mi.AuTeConfigurationStore.SNAPSHOT_PATH, archiver.getArtifacts());
        assertTrue("Failed runs are the ones most worth debugging, so they must still be archived",
                archiver.getAllowEmptyArchive() && !archiver.isOnlyIfSuccessful());
    }

    @Test
    public void addMiAgentWorkspaceCleanupIfNeeded_addsCleanupPublisherOnceAndIsIdempotent() throws Exception {
        FreeStyleProject proj = jenkins.createFreeStyleProject("mi-agent-cleanup-job");

        invokeAddMiAgentWorkspaceCleanupIfNeeded(proj);
        invokeAddMiAgentWorkspaceCleanupIfNeeded(proj);

        assertEquals(1, proj.getPublishersList().getAll(MIAgentWorkspaceCleanupPublisher.class).size());
    }

    private static void invokeAddAutonomousTesterAssignedNode(FreeStyleProject proj) throws Exception {
        Method method = TestExecutionJobCreatorService.class
                .getDeclaredMethod("addAutonomousTesterAssignedNode", FreeStyleProject.class);
        method.setAccessible(true);
        method.invoke(null, proj);
    }

    private static void invokeAddMiAgentArtifactArchiverIfNeeded(FreeStyleProject proj) throws Exception {
        Method method = TestExecutionJobCreatorService.class
                .getDeclaredMethod("addMiAgentArtifactArchiverIfNeeded", List.class);
        method.setAccessible(true);
        method.invoke(null, proj.getPublishersList());
    }

    private static void invokeAddMiAgentWorkspaceCleanupIfNeeded(FreeStyleProject proj) throws Exception {
        Method method = TestExecutionJobCreatorService.class
                .getDeclaredMethod("addMiAgentWorkspaceCleanupIfNeeded", List.class);
        method.setAccessible(true);
        method.invoke(null, proj.getPublishersList());
    }
}
