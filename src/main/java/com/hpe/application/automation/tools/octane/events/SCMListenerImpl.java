/*
 * © Copyright 2013 EntIT Software LLC
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright (c) 2018 Micro Focus Company, L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ___________________________________________________________________
 *
 */

package com.hpe.application.automation.tools.octane.events;

import com.hp.octane.integrations.OctaneSDK;
import com.hpe.application.automation.tools.octane.configuration.ConfigurationService;
import com.hpe.application.automation.tools.octane.model.processors.scm.SCMProcessor;
import com.hpe.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.events.CIEvent;
import com.hp.octane.integrations.dto.events.CIEventType;
import com.hp.octane.integrations.dto.scm.SCMData;
import com.hpe.application.automation.tools.octane.model.CIEventCausesFactory;
import com.hpe.application.automation.tools.octane.model.processors.scm.SCMProcessors;
import hudson.Extension;
import hudson.FilePath;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.SCMListener;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCM;
import hudson.scm.SCMRevisionState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.io.File;
import java.util.List;

/**
 * Run Listener that handles SCM CI events and dispatches notifications to the Octane server
 * Created by gullery on 10/07/2016.
 */

@Extension
@SuppressWarnings("squid:S1872")
public class SCMListenerImpl extends SCMListener {
	private static final Logger logger = LogManager.getLogger(SCMListenerImpl.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();


	@Override
	public void onCheckout(Run<?, ?> build, SCM scm, FilePath workspace, TaskListener listener, File changelogFile, SCMRevisionState pollingBaseline) throws Exception {
		super.onCheckout(build, scm, workspace, listener, changelogFile, pollingBaseline);
	}

	@Override
	public void onChangeLogParsed(Run<?, ?> r, SCM scm, TaskListener listener, ChangeLogSet<?> changelog) throws Exception {
		super.onChangeLogParsed(r, scm, listener, changelog);

		if (ConfigurationService.getServerConfiguration() != null && !ConfigurationService.getServerConfiguration().isValid()) {
			return;
		}
		if (ConfigurationService.getModel() != null && ConfigurationService.getModel().isSuspend()) {
			return;
		}

		CIEvent event;
		if (r.getParent() instanceof MatrixConfiguration || r instanceof AbstractBuild) {
			AbstractBuild build = (AbstractBuild) r;
			if (changelog != null && !changelog.isEmptySet()) {        // if there are any commiters
				SCMProcessor scmProcessor = SCMProcessors.getAppropriate(scm.getClass().getName());
				if (scmProcessor != null) {
					event = createSCMEvent(r, build, scmProcessor);
					OctaneSDK.getInstance().getEventsService().publishEvent(event);
				} else {
					logger.info("SCM changes detected, but no processors found for SCM provider of type " + scm.getClass().getName());
				}
			}
		} else if (r.getParent() instanceof WorkflowJob) {
			WorkflowRun wRun = (WorkflowRun) r;
			if (changelog != null && !changelog.isEmptySet() || !wRun.getChangeSets().isEmpty()) {
				SCMProcessor scmProcessor = SCMProcessors.getAppropriate(scm.getClass().getName());
				if (scmProcessor != null) {
					List<SCMData> scmDataList = scmProcessor.getSCMData(wRun);
					for (SCMData scmData : scmDataList) {
						event = dtoFactory.newDTO(CIEvent.class)
								.setEventType(CIEventType.SCM)
								.setProject(BuildHandlerUtils.getJobCiId(r))
								.setBuildCiId(BuildHandlerUtils.getBuildCiId(r))
								.setCauses(CIEventCausesFactory.processCauses(extractCauses(r)))
								.setNumber(String.valueOf(r.getNumber()))
								.setScmData(scmData);
						OctaneSDK.getInstance().getEventsService().publishEvent(event);
					}
				} else {
					logger.info("SCM changes detected, but no processors found for SCM provider of type " + scm.getClass().getName());
				}
			}
		}
	}

	private CIEvent createSCMEvent(Run<?, ?> run, AbstractBuild build, SCMProcessor scmProcessor) {
		CIEvent event;
		SCMData scmData = scmProcessor.getSCMData(build);
		event = dtoFactory.newDTO(CIEvent.class)
				.setEventType(CIEventType.SCM)
				.setProject(BuildHandlerUtils.getJobCiId(run))
				.setBuildCiId(BuildHandlerUtils.getBuildCiId(run))
				.setCauses(CIEventCausesFactory.processCauses(extractCauses(run)))
				.setNumber(String.valueOf(run.getNumber()))
				.setScmData(scmData);
		return event;
	}

	private List<Cause> extractCauses(Run<?, ?> r) {
		if (r.getParent() instanceof MatrixConfiguration) {
			return ((MatrixRun) r).getParentBuild().getCauses();
		} else {
			return r.getCauses();
		}
	}
}
