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

import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MiAgentPreflightBuilderTest {

    private final Run<?, ?> build = mock(Run.class);
    private final FilePath workspace = mock(FilePath.class);
    private final FilePath sharedWorkspace = mock(FilePath.class);
    private final FilePath miAgentExe = mock(FilePath.class);
    private final Launcher launcher = mock(Launcher.class);
    private final TaskListener listener = mock(TaskListener.class);

    private final MiAgentPreflightBuilder builder = new MiAgentPreflightBuilder();

    @Test
    public void checkMiAgentExecutable_throwsWhenParentIsNull() throws Exception {
        when(workspace.getParent()).thenReturn(null);

        try {
            builder.perform(build, workspace, launcher, listener);
        } catch (AbortException e) {
            assertTrue(e.getMessage().contains("shared workspace root"));
            return;
        }
        throw new AssertionError("Expected AbortException");
    }

    @Test
    public void checkMiAgentExecutable_throwsWhenExeNotFound() throws Exception {
        when(workspace.getParent()).thenReturn(sharedWorkspace);
        when(sharedWorkspace.child(MiAgentPreflightBuilder.MI_AGENT_EXE)).thenReturn(miAgentExe);
        when(miAgentExe.exists()).thenReturn(false);
        when(miAgentExe.getRemote()).thenReturn("C:\\Jenkins\\workspace\\mi-agent.exe");

        try {
            builder.perform(build, workspace, launcher, listener);
        } catch (AbortException e) {
            assertTrue(e.getMessage().contains("C:\\Jenkins\\workspace\\mi-agent.exe"));
            return;
        }
        throw new AssertionError("Expected AbortException");
    }

    @Test
    public void checkMiAgentExecutable_throwsWhenExeIsDirectory() throws Exception {
        when(workspace.getParent()).thenReturn(sharedWorkspace);
        when(sharedWorkspace.child(MiAgentPreflightBuilder.MI_AGENT_EXE)).thenReturn(miAgentExe);
        when(miAgentExe.exists()).thenReturn(true);
        when(miAgentExe.isDirectory()).thenReturn(true);

        try {
            builder.perform(build, workspace, launcher, listener);
        } catch (AbortException e) {
            assertTrue(e.getMessage().contains("mi-agent.exe not found"));
            return;
        }
        throw new AssertionError("Expected AbortException");
    }

    @Test
    public void checkMiAgentExecutable_proceedsToSignerValidationWhenExePresent() throws Exception {
        when(workspace.getParent()).thenReturn(sharedWorkspace);
        when(sharedWorkspace.child(MiAgentPreflightBuilder.MI_AGENT_EXE)).thenReturn(miAgentExe);
        when(miAgentExe.exists()).thenReturn(true);
        when(miAgentExe.isDirectory()).thenReturn(false);
        // Unix launcher causes signer step to fail — proves file check passed
        when(launcher.isUnix()).thenReturn(true);

        try {
            builder.perform(build, workspace, launcher, listener);
        } catch (AbortException e) {
            assertTrue(e.getMessage().contains("Windows agent"));
            return;
        }
        throw new AssertionError("Expected AbortException");
    }

    @Test
    public void validateSignerName_throwsWhenPowerShellExitCodeNonZero() throws Exception {
        setupExeMock("C:\\Jenkins\\workspace\\mi-agent.exe");
        setupLauncherMock(1, "Some error");

        try {
            builder.perform(build, workspace, launcher, listener);
        } catch (AbortException e) {
            assertTrue(e.getMessage().contains("PowerShell exit code=1"));
            assertTrue(e.getMessage().contains("Some error"));
            return;
        }
        throw new AssertionError("Expected AbortException");
    }

    @Test
    public void validateSignerName_throwsWhenExeNotSigned() throws Exception {
        setupExeMock("C:\\Jenkins\\workspace\\mi-agent.exe");
        setupLauncherMock(0, "SIGNER=");

        try {
            builder.perform(build, workspace, launcher, listener);
        } catch (AbortException e) {
            assertTrue(e.getMessage().contains("not digitally signed"));
            return;
        }
        throw new AssertionError("Expected AbortException");
    }

    @Test
    public void validateSignerName_throwsWhenSignerNameDoesNotMatch() throws Exception {
        setupExeMock("C:\\Jenkins\\workspace\\mi-agent.exe");
        setupLauncherMock(0, "SIGNER=Some Other Signer");

        try {
            builder.perform(build, workspace, launcher, listener);
        } catch (AbortException e) {
            assertTrue(e.getMessage().contains("Some Other Signer"));
            assertTrue(e.getMessage().contains(MiAgentPreflightBuilder.EXPECTED_SIGNER_NAME));
            return;
        }
        throw new AssertionError("Expected AbortException");
    }

    @Test
    public void perform_succeedsWithValidSigner() throws Exception {
        setupExeMock("C:\\Jenkins\\workspace\\mi-agent.exe");
        setupLauncherMock(0, "SIGNER=" + MiAgentPreflightBuilder.EXPECTED_SIGNER_NAME);
        ByteArrayOutputStream logs = new ByteArrayOutputStream();
        PrintStream logger = new PrintStream(logs, true, StandardCharsets.UTF_8.name());
        when(listener.getLogger()).thenReturn(logger);

        builder.perform(build, workspace, launcher, listener);

        String output = logs.toString(StandardCharsets.UTF_8.name());
        assertTrue(output.contains("MiAgentPreflightBuilder : mi-agent.exe found at: C:\\Jenkins\\workspace\\mi-agent.exe"));
        assertTrue(output.contains("MiAgentPreflightBuilder : mi-agent.exe signer validated"));
    }

    @Test
    public void validateSignerName_scriptContainsEscapedPathWithSingleQuotes() throws Exception {
        setupExeMock("C:\\path with 'quotes'\\mi-agent.exe");
        Launcher.ProcStarter procStarter = setupLauncherMock(0, "SIGNER=" + MiAgentPreflightBuilder.EXPECTED_SIGNER_NAME);
        when(listener.getLogger()).thenReturn(mock(PrintStream.class));

        builder.perform(build, workspace, launcher, listener);

        ArgumentCaptor<String> encodedCaptor = ArgumentCaptor.forClass(String.class);
        verify(procStarter).cmds(
                eq("powershell.exe"), eq("-NoProfile"), eq("-ExecutionPolicy"),
                eq("Bypass"), eq("-EncodedCommand"), encodedCaptor.capture());
        String decoded = new String(Base64.getDecoder().decode(encodedCaptor.getValue()), StandardCharsets.UTF_16LE);
        assertTrue(decoded.contains("C:\\path with ''quotes''\\mi-agent.exe"));
    }

    @Test
    public void extractOutputValue_returnsSignerValue() {
        String output = "STATUS=Valid\nSIGNER=OpenText Internal Development Code Signing\n";

        String signer = MiAgentPreflightBuilder.extractOutputValue(output, "SIGNER=");

        assertEquals("OpenText Internal Development Code Signing", signer);
    }

    @Test
    public void extractOutputValue_returnsNullWhenPrefixMissing() {
        String output = "STATUS=Valid\n";

        String signer = MiAgentPreflightBuilder.extractOutputValue(output, "SIGNER=");

        assertNull(signer);
    }

    private void setupExeMock(String exePath) throws Exception {
        when(workspace.getParent()).thenReturn(sharedWorkspace);
        when(sharedWorkspace.child(MiAgentPreflightBuilder.MI_AGENT_EXE)).thenReturn(miAgentExe);
        when(miAgentExe.exists()).thenReturn(true);
        when(miAgentExe.isDirectory()).thenReturn(false);
        when(miAgentExe.getRemote()).thenReturn(exePath);
    }

    private Launcher.ProcStarter setupLauncherMock(int exitCode, String psOutput) throws Exception {
        Launcher.ProcStarter procStarter = mock(Launcher.ProcStarter.class);
        when(launcher.isUnix()).thenReturn(false);
        when(launcher.launch()).thenReturn(procStarter);
        when(procStarter.cmds(eq("powershell.exe"), eq("-NoProfile"), eq("-ExecutionPolicy"),
                eq("Bypass"), eq("-EncodedCommand"), anyString())).thenReturn(procStarter);
        when(procStarter.quiet(anyBoolean())).thenReturn(procStarter);
        doAnswer(inv -> {
            OutputStream os = inv.getArgument(0);
            os.write(psOutput.getBytes(StandardCharsets.UTF_8));
            return procStarter;
        }).when(procStarter).stdout(any(OutputStream.class));
        when(procStarter.stderr(any(OutputStream.class))).thenReturn(procStarter);
        when(procStarter.join()).thenReturn(exitCode);
        return procStarter;
    }
}
