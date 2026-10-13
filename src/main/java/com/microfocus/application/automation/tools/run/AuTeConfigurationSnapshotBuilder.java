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

import com.microfocus.application.automation.tools.mi.AuTeConfigurationStore;
import com.microfocus.application.automation.tools.mi.AuTeSetupFailure;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** First AuTe build step: copies the shared space default configuration from the controller into the build workspace. */
public class AuTeConfigurationSnapshotBuilder extends Builder implements SimpleBuildStep {
    static final String COPY_FAILED_REASON = "Failed to copy the shared space default configuration to the build workspace.";
    static final String INVALID_JSON_REASON = "The shared space default configuration must be a valid JSON object.";

    private final String configurationId;
    private final String spaceId;

    @DataBoundConstructor
    public AuTeConfigurationSnapshotBuilder(String configurationId, String spaceId) {
        this.configurationId = configurationId;
        this.spaceId = spaceId;
    }

    public String getConfigurationId() {
        return configurationId;
    }

    public String getSpaceId() {
        return spaceId;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher,
                        @Nonnull TaskListener listener) throws IOException, InterruptedException {
        byte[] template;
        try {
            template = AuTeConfigurationStore.readTemplate(configurationId, spaceId);
        } catch (IOException e) {
            throw setupFailure(run, listener, COPY_FAILED_REASON, e);
        }

        try {
            validateJsonObject(template);
        } catch (IOException e) {
            throw setupFailure(run, listener, INVALID_JSON_REASON, e);
        }

        FilePath target = workspace.child(AuTeConfigurationStore.SNAPSHOT_PATH);
        try {
            workspace.mkdirs();
            try (OutputStream output = target.write()) {
                output.write(template);
            }
        } catch (IOException e) {
            throw setupFailure(run, listener, COPY_FAILED_REASON, e);
        }
        listener.getLogger().println("[AuTe] Copied shared space configuration to: " + target.getRemote());
    }

    static void validateJsonObject(byte[] contents) throws IOException {
        Object parsed;
        try {
            parsed = JSONValue.parseStrict(new String(contents, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IOException(INVALID_JSON_REASON, e);
        }
        if (!(parsed instanceof JSONObject)) {
            throw new IOException(INVALID_JSON_REASON);
        }
    }

    private static AbortException setupFailure(Run<?, ?> run, TaskListener listener, String reason, IOException cause) {
        listener.getLogger().println("[AuTe] " + reason + " " + cause);
        run.addAction(new AuTeSetupFailure(reason));
        return new AbortException(reason);
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return FreeStyleProject.class.isAssignableFrom(jobType);
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "AuTe: Copy shared-space configuration";
        }
    }
}