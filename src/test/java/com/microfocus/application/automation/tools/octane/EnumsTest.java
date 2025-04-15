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
 *  Copyright 2012-2025 Open Text.
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
package com.microfocus.application.automation.tools.octane;

import com.hp.octane.integrations.dto.causes.CIEventCauseType;
import com.hp.octane.integrations.dto.events.CIEventType;
import com.hp.octane.integrations.dto.parameters.CIParameterType;
import com.hp.octane.integrations.dto.scm.SCMType;
import com.hp.octane.integrations.dto.snapshots.CIBuildResult;
import com.hp.octane.integrations.dto.snapshots.CIBuildStatus;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created with IntelliJ IDEA.
 * User: gullery
 * Date: 13/01/15
 * Time: 21:32
 * To change this template use File | Settings | File Templates.
 */
@SuppressWarnings({"squid:S2699","squid:S3658","squid:S2259","squid:S1872","squid:S2925","squid:S109","squid:S1607","squid:S2701","squid:S2698"})
public class EnumsTest {

	@Test
	public void testCIEventCauseType() {
		assertEquals(CIEventCauseType.values().length, 5);
		assertEquals(CIEventCauseType.SCM.value(), "scm");
		assertEquals(CIEventCauseType.USER.value(), "user");
		assertEquals(CIEventCauseType.TIMER.value(), "timer");
		assertEquals(CIEventCauseType.UPSTREAM.value(), "upstream");
		assertEquals(CIEventCauseType.UNDEFINED.value(), "undefined");
		assertEquals(CIEventCauseType.fromValue("scm"), CIEventCauseType.SCM);
	}

	@Test
	public void testCIEventType() {
		assertEquals(CIEventType.values().length, 9);
		assertEquals(CIEventType.QUEUED.value(), "queued");
		assertEquals(CIEventType.SCM.value(), "scm");
		assertEquals(CIEventType.STARTED.value(), "started");
		assertEquals(CIEventType.FINISHED.value(), "finished");
		assertEquals(CIEventType.fromValue("queued"), CIEventType.QUEUED);
		assertEquals(CIEventType.DELETED.value(),"deleted" );
		assertEquals(CIEventType.RENAMED.value(),"renamed" );
		assertEquals(CIEventType.REMOVED_FROM_QUEUE.value(),"removed_from_queue" );
		assertEquals(CIEventType.CHANGE_EXEC_STATE.value(),"change_exec_state" );

	}

	@Test
	public void testParameterType() {
		assertEquals(CIParameterType.values().length, 7);
		assertEquals(CIParameterType.UNKNOWN.value(), "unknown");
		assertEquals(CIParameterType.PASSWORD.value(), "password");
		assertEquals(CIParameterType.BOOLEAN.value(), "boolean");
		assertEquals(CIParameterType.STRING.value(), "string");
		assertEquals(CIParameterType.NUMBER.value(), "number");
		assertEquals(CIParameterType.FILE.value(), "file");
    assertEquals(CIParameterType.AXIS.value(), "axis");
		assertEquals(CIParameterType.fromValue("unavailable"), CIParameterType.UNKNOWN);
	}

	@Test
	public void testSnapshotResult() {
		assertEquals(CIBuildResult.values().length, 5);
		assertEquals(CIBuildResult.UNAVAILABLE.value(), "unavailable");
		assertEquals(CIBuildResult.UNSTABLE.value(), "unstable");
		assertEquals(CIBuildResult.ABORTED.value(), "aborted");
		assertEquals(CIBuildResult.FAILURE.value(), "failure");
		assertEquals(CIBuildResult.SUCCESS.value(), "success");
		assertEquals(CIBuildResult.fromValue("unavailable"), CIBuildResult.UNAVAILABLE);
	}

	@Test
	public void testSnapshotStatus() {
		assertEquals(CIBuildStatus.values().length, 4);
		assertEquals(CIBuildStatus.UNAVAILABLE.value(), "unavailable");
		assertEquals(CIBuildStatus.QUEUED.value(), "queued");
		assertEquals(CIBuildStatus.RUNNING.value(), "running");
		assertEquals(CIBuildStatus.FINISHED.value(), "finished");
		assertEquals(CIBuildStatus.fromValue("unavailable"), CIBuildStatus.UNAVAILABLE);
	}

	@Test
	public void testSCMType() {
		assertEquals(SCMType.values().length, 7);
		assertEquals(SCMType.UNKNOWN.value(), "unknown");
		assertEquals(SCMType.GIT.value(), "git");
		assertEquals(SCMType.SVN.value(), "svn");
		assertEquals(SCMType.STARTEAM.value(), "starteam");
		assertEquals(SCMType.ACCUREV.value(), "accurev");
		assertEquals(SCMType.DIMENSIONS_CM.value(), "dimensions_cm");
		assertEquals(SCMType.fromValue("unknown"), SCMType.UNKNOWN);
	}
}
