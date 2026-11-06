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

import com.microfocus.application.automation.tools.mi.AuTeSetupFailure;
import com.microfocus.application.automation.tools.mi.MIAgentConstants;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

public class MiAgentPreflightBuilderTest {

    private final Run<?, ?> build = mock(Run.class);
    private final Launcher launcher = mock(Launcher.class);
    private final TaskListener listener = mock(TaskListener.class);

    private final MiAgentPreflightBuilder builder = new MiAgentPreflightBuilder();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private String originalAuteHome;
    private FilePath workspace;
    private File miAgentExe;

    @Before
    public void setUpInstallation() throws Exception {
        originalAuteHome = EnvVars.masterEnvVars.remove(MIAgentConstants.AUTE_HOME_ENV_VAR);
        File installation = tempFolder.newFolder("Autonomous Tester");
        EnvVars.masterEnvVars.put(MIAgentConstants.AUTE_HOME_ENV_VAR, installation.getAbsolutePath());
        File agentFolder = new File(installation, "mi-agent");
        assertTrue(agentFolder.mkdir());
        miAgentExe = new File(agentFolder, "mi-agent.exe");
        workspace = new FilePath(tempFolder.newFolder("workspace"));
    }

    @After
    public void restoreAuteHome() {
        if (originalAuteHome == null) {
            EnvVars.masterEnvVars.remove(MIAgentConstants.AUTE_HOME_ENV_VAR);
        } else {
            EnvVars.masterEnvVars.put(MIAgentConstants.AUTE_HOME_ENV_VAR, originalAuteHome);
        }
    }

    @Test
    public void checkMiAgentExecutable_throwsWhenVariableMissing() throws Exception {
        EnvVars.masterEnvVars.remove(MIAgentConstants.AUTE_HOME_ENV_VAR);

        try {
            builder.perform(build, workspace, launcher, listener);
        } catch (AbortException e) {
            assertTrue(e.getMessage().contains("UFT_AUTE_HOME is not set"));
            verify(build).addAction(any(AuTeSetupFailure.class));
            return;
        }
        throw new AssertionError("Expected AbortException");
    }

    @Test
    public void checkMiAgentExecutable_throwsWhenExeNotFound() throws Exception {
        try {
            builder.perform(build, workspace, launcher, listener);
        } catch (AbortException e) {
            assertTrue(e.getMessage().contains(miAgentExe.getAbsolutePath()));
            verify(build).addAction(any(AuTeSetupFailure.class));
            return;
        }
        throw new AssertionError("Expected AbortException");
    }

    @Test
    public void checkMiAgentExecutable_throwsWhenExeIsDirectory() throws Exception {
        assertTrue(miAgentExe.mkdir());

        try {
            builder.perform(build, workspace, launcher, listener);
        } catch (AbortException e) {
            assertTrue(e.getMessage().contains("mi-agent.exe not found"));
            return;
        }
        throw new AssertionError("Expected AbortException");
    }

    @Test
    public void perform_succeedsWhenExePresent() throws Exception {
        assertTrue(miAgentExe.createNewFile());
        ByteArrayOutputStream logs = new ByteArrayOutputStream();
        PrintStream logger = new PrintStream(logs, true, StandardCharsets.UTF_8.name());
        when(listener.getLogger()).thenReturn(logger);

        builder.perform(build, workspace, launcher, listener);

        String output = logs.toString(StandardCharsets.UTF_8.name());
        assertTrue(output.contains("AuTe Preflight Check : mi-agent.exe found at: " + miAgentExe.getAbsolutePath()));
    }
}
