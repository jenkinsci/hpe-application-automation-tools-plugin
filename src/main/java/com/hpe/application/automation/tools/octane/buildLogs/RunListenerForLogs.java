/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.hpe.application.automation.tools.octane.buildLogs;

import com.google.inject.Inject;
import com.hpe.application.automation.tools.octane.configuration.ConfigurationService;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;

/**
 * Created by benmeior on 11/16/2016
 * Jenkins events listener to dispatch build logs to BDI server via Octane server as its proxy
 */

@Extension
public class RunListenerForLogs extends RunListener<Run> {
	private static Logger logger = LogManager.getLogger(RunListenerForLogs.class);

	@Inject
	private LogDispatcher logDispatcher;

	@Override
	public void onCompleted(Run r, @Nonnull TaskListener listener) {
		if (r instanceof AbstractBuild && ConfigurationService.getServerConfiguration().isValid()) {
			AbstractBuild build = (AbstractBuild) r;
			logger.info(String.format("Enqueued job [%s#%d]", build.getProject().getFullName(), build.getNumber()));
			logDispatcher.enqueueLog(build.getProject().getFullName(), build.getNumber());
		} else {
			logger.warn("Octane configuration is not valid");
		}
	}
}
