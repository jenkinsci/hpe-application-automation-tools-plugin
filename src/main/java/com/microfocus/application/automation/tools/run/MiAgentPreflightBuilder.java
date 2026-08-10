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
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * First build step in every MI Agent job — verifies that mi-agent.exe is present in the
 * shared workspace root before conversion or execution begins.
 */
public class MiAgentPreflightBuilder extends Builder implements SimpleBuildStep {

    static final String MI_AGENT_EXE = "mi-agent.exe";
    static final String EXPECTED_SIGNER_NAME = "OpenText Internal Development Code Signing";
    private static final String SIGNER_PREFIX = "SIGNER=";

    @DataBoundConstructor
    public MiAgentPreflightBuilder() {
    }

    @Override
    public void perform(@Nonnull Run<?, ?> build,
                        @Nonnull FilePath workspace,
                        @Nonnull Launcher launcher,
                        @Nonnull TaskListener listener) throws IOException, InterruptedException {
        FilePath miAgentExe = checkMiAgentExecutable(workspace);
        validateSignerName(miAgentExe, launcher);
        printToConsole(listener, "mi-agent.exe found at: " + miAgentExe.getRemote());
        printToConsole(listener, "mi-agent.exe signer validated");
    }

    private static void printToConsole(TaskListener listener, String msg) {
        listener.getLogger().println(formatMessage(msg));
    }

    private static String formatMessage(String msg) {
        return MiAgentPreflightBuilder.class.getSimpleName() + " : " + msg;
    }

    private FilePath checkMiAgentExecutable(FilePath workspace) throws IOException, InterruptedException {
        // mi-agent.exe lives in the shared workspace root, one level above the job workspace
        FilePath sharedWorkspace = workspace.getParent();
        if (sharedWorkspace == null) {
            throw new AbortException(formatMessage("Cannot resolve shared workspace root from: " + workspace.getRemote()));
        }

        FilePath miAgentExe = sharedWorkspace.child(MI_AGENT_EXE);
        if (!miAgentExe.exists() || miAgentExe.isDirectory()) {
            throw new AbortException(formatMessage("mi-agent.exe not found at: " + miAgentExe.getRemote()));
        }

        return miAgentExe;
    }

    private void validateSignerName(FilePath miAgentExe, Launcher launcher) throws IOException, InterruptedException {
        if (launcher.isUnix()) {
            throw new AbortException(formatMessage("Signer validation requires a Windows agent."));
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        String signerScript = buildSignerScript(miAgentExe.getRemote());
        String encoded = Base64.getEncoder().encodeToString(signerScript.getBytes(StandardCharsets.UTF_16LE));
        int exitCode = launcher.launch()
                .cmds("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-EncodedCommand", encoded)
                .quiet(true)
                .stdout(output)
                .stderr(output)
                .join();

        String outputText = output.toString(StandardCharsets.UTF_8).trim();
        if (exitCode != 0) {
            throw new AbortException(formatMessage("Failed to validate mi-agent.exe signer. "
                    + "PowerShell exit code=" + exitCode + ". Output: " + outputText));
        }

        String signerName = extractOutputValue(outputText, SIGNER_PREFIX);
        if (signerName == null || signerName.trim().isEmpty()) {
            throw new AbortException(formatMessage("mi-agent.exe is not digitally signed."));
        }
        if (!EXPECTED_SIGNER_NAME.equals(signerName.trim())) {
            throw new AbortException(formatMessage("Invalid mi-agent.exe signer: '" + signerName + "'. "
                    + "Expected signer: '" + EXPECTED_SIGNER_NAME + "'."));
        }
    }

    private String buildSignerScript(String exePath) {
        String escaped = exePath.replace("'", "''");
        return """
                $sig = Get-AuthenticodeSignature -LiteralPath '%s'
                $n = if ($sig.SignerCertificate) {
                    $sig.SignerCertificate.GetNameInfo([System.Security.Cryptography.X509Certificates.X509NameType]::SimpleName, $false)
                } else { '' }
                Write-Output ('%s' + $n)
                """.formatted(escaped, SIGNER_PREFIX);
    }

    static String extractOutputValue(String outputText, String prefix) {
        String[] lines = outputText.split("\\R");
        for (String line : lines) {
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length());
            }
        }
        return null;
    }

    @Extension
    @Symbol("miAgentPreflight")
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "MI Agent Preflight Check";
        }
    }
}
