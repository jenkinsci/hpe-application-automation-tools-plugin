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
package com.microfocus.application.automation.tools.pipelineSteps;

import com.microfocus.application.automation.tools.run.PcBuilder;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.logging.log4j.LogManager;
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.After;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PcBuildStepExecutionTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @After
    public void closeLogFiles() {
        // Plugin log appenders otherwise keep JenkinsRule's temporary files locked on Windows.
        LogManager.shutdown();
    }

    private StepContext context() throws Exception {
        StepContext context = mock(StepContext.class);
        when(context.get(Run.class)).thenReturn(mock(Run.class));
        when(context.get(FilePath.class)).thenReturn(jenkins.jenkins.getRootPath());
        when(context.get(Launcher.class)).thenReturn(mock(Launcher.class));
        when(context.get(TaskListener.class)).thenReturn(TaskListener.NULL);
        return context;
    }

    @Test
    public void abortInterruptsWorkerAndWaitsForCleanup() throws Exception {
        PcBuilder builder = mock(PcBuilder.class);
        StepContext context = context();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        CountDownLatch cleanup = new CountDownLatch(1);
        doAnswer(invocation -> {
            entered.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException e) {
                interrupted.countDown();
                assertTrue(cleanup.await(10, TimeUnit.SECONDS));
                throw e;
            }
            return null;
        }).when(builder).perform(any(Run.class), any(FilePath.class), any(Launcher.class), any(TaskListener.class));

        PcBuildStepExecution execution = new PcBuildStepExecution(builder, context);
        FlowInterruptedException cause = new FlowInterruptedException(Result.ABORTED, true);
        try {
            assertFalse(execution.start());
            assertTrue(entered.await(10, TimeUnit.SECONDS));
            execution.stop(cause);
            assertTrue(interrupted.await(10, TimeUnit.SECONDS));
            verify(context, never()).onFailure(any(Throwable.class));
            verify(context, never()).onSuccess(isNull());
            execution.stop(cause);
        } finally {
            cleanup.countDown();
        }
        verify(context, timeout(10000)).onFailure(cause);
        verify(context, never()).onSuccess(isNull());
        assertArrayEquals(new Throwable[0], cause.getSuppressed());
    }

    @Test
    public void abortPreservesUnexpectedCleanupFailure() throws Exception {
        PcBuilder builder = mock(PcBuilder.class);
        StepContext context = context();
        CountDownLatch entered = new CountDownLatch(1);
        IOException cleanupFailure = new IOException("Cleanup failed");
        doAnswer(invocation -> {
            entered.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException e) {
                throw cleanupFailure;
            }
            return null;
        }).when(builder).perform(any(Run.class), any(FilePath.class), any(Launcher.class), any(TaskListener.class));

        PcBuildStepExecution execution = new PcBuildStepExecution(builder, context);
        FlowInterruptedException cause = new FlowInterruptedException(Result.ABORTED, true);
        assertFalse(execution.start());
        try {
            assertTrue(entered.await(10, TimeUnit.SECONDS));
        } finally {
            execution.stop(cause);
        }

        verify(context, timeout(10000)).onFailure(cause);
        verify(context, never()).onSuccess(isNull());
        assertArrayEquals(new Throwable[] {cleanupFailure}, cause.getSuppressed());
    }

    @Test
    public void normalCompletionReportsSuccess() throws Exception {
        PcBuilder builder = mock(PcBuilder.class);
        StepContext context = context();
        PcBuildStepExecution execution = new PcBuildStepExecution(builder, context);

        assertFalse(execution.start());

        verify(context, timeout(10000)).onSuccess(null);
        verify(context, never()).onFailure(any(Throwable.class));
        verify(builder).perform(any(Run.class), any(FilePath.class), any(Launcher.class), any(TaskListener.class));
    }

    @Test
    public void abortBeforeWorkerStartsDoesNotRunBuilder() throws Exception {
        PcBuilder builder = mock(PcBuilder.class);
        StepContext context = context();
        PcBuildStepExecution execution = new PcBuildStepExecution(builder, context);
        FlowInterruptedException cause = new FlowInterruptedException(Result.ABORTED, true);

        execution.stop(cause);
        assertFalse(execution.start());

        verify(context, timeout(10000)).onFailure(cause);
        verify(builder, never()).perform(any(Run.class), any(FilePath.class), any(Launcher.class), any(TaskListener.class));
        verify(context, never()).onSuccess(isNull());
        assertArrayEquals(new Throwable[0], cause.getSuppressed());
    }
}
