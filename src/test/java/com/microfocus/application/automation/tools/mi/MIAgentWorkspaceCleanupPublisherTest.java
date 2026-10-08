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

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.FreeStyleBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.PrintStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MIAgentWorkspaceCleanupPublisherTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void perform_deletesOnlyThisBuildsResultFolder() throws Exception {
        File thisBuildFolder = new File(tempFolder.getRoot(), MIAgentConstants.RESULT_FOLDER + "\\7");
        File otherBuildFolder = new File(tempFolder.getRoot(), MIAgentConstants.RESULT_FOLDER + "\\8");
        assertTrue(thisBuildFolder.mkdirs());
        assertTrue(otherBuildFolder.mkdirs());

        Run<?, ?> build = mock(FreeStyleBuild.class);
        when(build.getNumber()).thenReturn(7);

        new MIAgentWorkspaceCleanupPublisher()
                .perform(build, new FilePath(tempFolder.getRoot()), mock(Launcher.class), mockListener());

        assertFalse(thisBuildFolder.exists());
        assertTrue(otherBuildFolder.exists());
    }

    @Test
    public void perform_noResultFolderForBuild_doesNothing() throws Exception {
        Run<?, ?> build = mock(FreeStyleBuild.class);
        when(build.getNumber()).thenReturn(1);

        new MIAgentWorkspaceCleanupPublisher()
                .perform(build, new FilePath(tempFolder.getRoot()), mock(Launcher.class), mockListener());

        assertFalse(new File(tempFolder.getRoot(), MIAgentConstants.RESULT_FOLDER).exists());
    }

    private TaskListener mockListener() {
        TaskListener listener = mock(TaskListener.class);
        when(listener.getLogger()).thenReturn(new PrintStream(System.out));
        return listener;
    }
}
