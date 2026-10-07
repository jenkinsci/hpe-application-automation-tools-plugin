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

import com.microfocus.application.automation.tools.pc.PcTestBase;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PcBuilderTest {

    @Test
    public void earlyAbortClearsPreviousRunId() throws Exception {
        PcBuilder builder = new PcBuilder(
                PcTestBase.SERVER_AND_PORT, PcTestBase.PC_SERVER_NAME, null,
                PcTestBase.ALM_DOMAIN, PcTestBase.ALM_PROJECT, PcTestBase.TEST_ID,
                PcTestBase.TEST_INSTANCE_ID, PcTestBase.TESTINSTANCEID,
                PcTestBase.TIMESLOT_DURATION_HOURS, PcTestBase.TIMESLOT_DURATION_MINUTES,
                PcTestBase.POST_RUN_ACTION, PcTestBase.VUDS_MODE, false, PcTestBase.DESCRIPTION,
                "NO_TREND", null, PcTestBase.IS_HTTPS, null, null,
                PcTestBase.RETRY, PcTestBase.RETRYDELAY, PcTestBase.RETRYOCCURRENCES,
                PcTestBase.AUTHENTICATE_WITH_TOKEN);
        Field runId = PcBuilder.class.getDeclaredField("runId");
        runId.setAccessible(true);
        runId.setInt(builder, Integer.parseInt(PcTestBase.RUN_ID));
        assertEquals(Integer.parseInt(PcTestBase.RUN_ID), builder.getRunId());

        FilePath workspace = mock(FilePath.class);
        InterruptedException interruption = new InterruptedException("Aborted before authentication");
        when(workspace.toURI()).thenThrow(interruption);

        try {
            builder.perform(mock(Run.class), workspace, mock(Launcher.class), TaskListener.NULL);
            fail("Expected the early interruption to propagate");
        } catch (InterruptedException e) {
            assertSame(interruption, e);
        }

        assertEquals(0, builder.getRunId());
    }
}
