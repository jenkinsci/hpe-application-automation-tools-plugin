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

package com.microfocus.application.automation.tools.octane.buildLogs;

import com.hp.octane.integrations.OctaneSDK;
import com.microfocus.application.automation.tools.octane.configuration.ConfigurationService;
import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.listeners.RunListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by benmeior on 11/16/2016
 * Jenkins events listener to dispatch build logs to Octane server
 */

@Extension
public class RunListenerForLogs extends RunListener<Run> {
	private static Logger logger = LogManager.getLogger(RunListenerForLogs.class);

	@Override
	public void onFinalized(Run run) {
		if (ConfigurationService.getServerConfiguration() == null ||
				!ConfigurationService.getServerConfiguration().isValid() ||
				ConfigurationService.getModel().isSuspend()) {
			return;
		}

		try {
			String jobCiId = BuildHandlerUtils.getJobCiId(run);
			String buildCiId = BuildHandlerUtils.getBuildCiId(run);
			logger.info("enqueued build '" + jobCiId + " #" + buildCiId + "' for log submission");
			OctaneSDK.getInstance().getLogsService().enqueuePushBuildLog(jobCiId, buildCiId);
		} catch (Throwable t) {
			logger.error("failed to enqueue " + run + " for logs push to Octane", t);
		}
	}
}
