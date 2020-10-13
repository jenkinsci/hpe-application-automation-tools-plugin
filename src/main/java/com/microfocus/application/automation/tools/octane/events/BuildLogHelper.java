/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2019 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors ("Micro Focus") are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.octane.events;

import com.hp.octane.integrations.OctaneSDK;
import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import hudson.model.Run;
import org.apache.logging.log4j.Logger;

public class BuildLogHelper {
	private static Logger logger = SDKBasedLoggerProvider.getLogger(BuildLogHelper.class);

	private BuildLogHelper(){
		//for code climate
	}

	public static void enqueueBuildLog(Run run) {
		if(!OctaneSDK.hasClients()){
			return;
		}
		try {
			String jobCiId = BuildHandlerUtils.getJobCiId(run);
			String buildCiId = BuildHandlerUtils.getBuildCiId(run);
			String parents = BuildHandlerUtils.getRootJobCiIds(run);

			logger.debug("enqueued build '" + jobCiId + " #" + buildCiId + "' for log submission");
			OctaneSDK.getClients().forEach(octaneClient -> {
				octaneClient.getLogsService().enqueuePushBuildLog(jobCiId, buildCiId, parents);
			});
		} catch (Exception t) {
			logger.error("failed to enqueue " + run + " for logs push to Octane", t);
		}
	}
}
