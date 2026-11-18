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

import hudson.EnvVars;
import hudson.FilePath;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public final class MIAgentConstants {
    public static final String RESULT_FOLDER = "mi-agent-results";
    public static final String MANIFEST_FILE_NAME = "manifest.json";
    public static final String RUN_STEPS_RESULT_FILE_NAME = "run_steps_result.json";
    public static final String AUTE_HOME_ENV_VAR = "UFT_AUTE_HOME";

    private MIAgentConstants() {
        // Prevent instantiation
    }

    public static FilePath resultRoot(FilePath workspace) {
        return workspace.child(RESULT_FOLDER);
    }

    /**
     * Resolves the installed runner using the agent process environment, not job overrides.
     * Restart the Jenkins agent after the installer sets {@code UFT_AUTE_HOME}.
     */
    public static FilePath resolveRunnerExecutable(FilePath workspace) throws IOException, InterruptedException {
        EnvVars agentEnvironment = EnvVars.getRemote(workspace.getChannel());
        String auteHome = StringUtils.trimToNull(agentEnvironment.get(AUTE_HOME_ENV_VAR));
        if (auteHome == null) {
            throw new IOException("[AuTe][ERROR] UFT_AUTE_HOME is not set in the Jenkins agent environment. "
                    + "Install Autonomous Tester and restart the Jenkins agent.");
        }

        FilePath runner = new FilePath(workspace.getChannel(), auteHome)
                .child("mi-agent")
                .child("mi-agent.exe");
        if (!runner.exists() || runner.isDirectory()) {
            throw new IOException("[AuTe][ERROR] mi-agent.exe not found at installer location: %s"
                    .formatted(runner.getRemote()));
        }
        return runner;
    }
}
