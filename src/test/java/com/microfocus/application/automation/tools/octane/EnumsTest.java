/*
 *
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * © Copyright 2012-2018 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors (“Micro Focus”) are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 *
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
		assertEquals(CIEventType.values().length, 6);
		assertEquals(CIEventType.QUEUED.value(), "queued");
		assertEquals(CIEventType.SCM.value(), "scm");
		assertEquals(CIEventType.STARTED.value(), "started");
		assertEquals(CIEventType.FINISHED.value(), "finished");
		assertEquals(CIEventType.fromValue("queued"), CIEventType.QUEUED);
		assertEquals(CIEventType.DELETED.value(),"deleted" );
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
		assertEquals(SCMType.values().length, 3);
		assertEquals(SCMType.UNKNOWN.value(), "unknown");
		assertEquals(SCMType.GIT.value(), "git");
		assertEquals(SCMType.SVN.value(), "svn");
		assertEquals(SCMType.fromValue("unknown"), SCMType.UNKNOWN);
	}
}
