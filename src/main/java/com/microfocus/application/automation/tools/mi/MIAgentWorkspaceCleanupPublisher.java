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

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serial;
import java.io.Serializable;

/**
 * Post-build step for MI Agent (Autonomous-Tester / AuTe) jobs that deletes this build's
 * {@code mi-agent-results/<build-number>} folder from the agent workspace.
 * <p>
 * Must run AFTER {@link MIAgentResultPublisher} and the artifact archiver, since both need the
 * folder's contents to still be on disk. The result folder is confined to a single build number
 * ({@link MIAgentConstants#resultRootForBuild(FilePath, Run)}), so this never touches other builds'
 * data or any other part of the workspace.
 * </p>
 */
public class MIAgentWorkspaceCleanupPublisher extends Recorder implements SimpleBuildStep, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @DataBoundConstructor
    public MIAgentWorkspaceCleanupPublisher() {
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
        FilePath resultRoot = MIAgentConstants.resultRootForBuild(workspace, run);
        if (resultRoot.exists()) {
            resultRoot.deleteRecursive();
            log.println("[AuTe] Deleted workspace results folder: " + resultRoot.getRemote());
        }
    }

    @Extension
    @Symbol("miAgentWorkspaceCleanup")
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "AuTe: Delete workspace results folder";
        }
    }
}
