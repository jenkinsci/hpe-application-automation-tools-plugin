/*
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
 */

package com.microfocus.application.automation.tools.octane.events;

import com.google.inject.Inject;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.events.CIEvent;
import com.hp.octane.integrations.dto.events.CIEventType;
import com.hp.octane.integrations.dto.events.PhaseType;
import com.hp.octane.integrations.dto.pipelines.PipelineNode;
import com.hp.octane.integrations.dto.pipelines.PipelinePhase;
import com.microfocus.application.automation.tools.octane.configuration.ConfigurationService;
import com.microfocus.application.automation.tools.octane.model.CIEventCausesFactory;
import com.microfocus.application.automation.tools.octane.model.processors.parameters.ParameterProcessors;
import com.microfocus.application.automation.tools.octane.model.processors.projects.JobProcessorFactory;
import com.microfocus.application.automation.tools.octane.model.processors.scm.CommonOriginRevision;
import com.microfocus.application.automation.tools.octane.model.processors.scm.SCMProcessor;
import com.microfocus.application.automation.tools.octane.model.processors.scm.SCMProcessors;
import com.microfocus.application.automation.tools.octane.tests.TestListener;
import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import hudson.Extension;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import hudson.scm.SCM;
import jenkins.model.Jenkins;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.List;

/**
 * Run Listener that handles basic CI events and dispatches notifications to the Octane server
 * User: gullery
 * Date: 24/08/14
 * Time: 17:21
 */

@Extension
@SuppressWarnings({"squid:S2259", "squid:S1872", "squid:S1698", "squid:S1132"})
public final class AbstractBuildListenerOctaneImpl extends RunListener<AbstractBuild> {
	private static final Logger logger = LogManager.getLogger(AbstractBuildListenerOctaneImpl.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	@Inject
	private TestListener testListener;

	@Override
	public void onStarted(AbstractBuild build, TaskListener listener) {
		if (noGoConfiguration()) {
			return;
		}

		try {
			CIEvent event = dtoFactory.newDTO(CIEvent.class)
					.setEventType(CIEventType.STARTED)
					.setProject(BuildHandlerUtils.getJobCiId(build))
					.setProjectDisplayName(BuildHandlerUtils.getJobCiId(build))
					.setBuildCiId(BuildHandlerUtils.getBuildCiId(build))
					.setNumber(String.valueOf(build.getNumber()))
					.setStartTime(build.getStartTimeInMillis())
					.setEstimatedDuration(build.getEstimatedDuration())
					.setCauses(CIEventCausesFactory.processCauses(build))
					.setParameters(ParameterProcessors.getInstances(build));

			if (isInternal(build)) {
				event.setPhaseType(PhaseType.INTERNAL);
			} else {
				event.setPhaseType(PhaseType.POST);
			}
			OctaneSDK.getInstance().getEventsService().publishEvent(event);
		} catch (Throwable throwable) {
			logger.error("failed to build and/or dispatch STARTED event for " + build, throwable);
		}
	}

	@Override
	public void onFinalized(AbstractBuild build) {
		if (noGoConfiguration()) {
			return;
		}

		try {
			boolean hasTests = testListener.processBuild(build);

			CIEvent event = dtoFactory.newDTO(CIEvent.class)
					.setEventType(CIEventType.FINISHED)
					.setProject(BuildHandlerUtils.getJobCiId(build))
					.setProjectDisplayName(BuildHandlerUtils.getJobCiId(build))
					.setBuildCiId(BuildHandlerUtils.getBuildCiId(build))
					.setNumber(String.valueOf(build.getNumber()))
					.setStartTime(build.getStartTimeInMillis())
					.setEstimatedDuration(build.getEstimatedDuration())
					.setCauses(CIEventCausesFactory.processCauses(build))
					.setParameters(ParameterProcessors.getInstances(build))
					.setResult(BuildHandlerUtils.translateRunResult(build))
					.setDuration(build.getDuration())
					.setTestResultExpected(hasTests);

			CommonOriginRevision commonOriginRevision = getCommonOriginRevision(build);
			if (commonOriginRevision != null) {
				event
						.setCommonHashId(commonOriginRevision.revision)
						.setBranchName(commonOriginRevision.branch);
			}

			OctaneSDK.getInstance().getEventsService().publishEvent(event);
		} catch (Throwable throwable) {
			logger.error("failed to build and/or dispatch FINISHED event for " + build, throwable);
		}
	}

	private boolean noGoConfiguration() {
		return ConfigurationService.getServerConfiguration() == null ||
				!ConfigurationService.getServerConfiguration().isValid() ||
				ConfigurationService.getModel() == null ||
				ConfigurationService.getModel().isSuspend();
	}

	private CommonOriginRevision getCommonOriginRevision(AbstractBuild build) {
		CommonOriginRevision commonOriginRevision = null;
		SCM scm = build.getProject().getScm();
		if (scm != null) {
			SCMProcessor scmProcessor = SCMProcessors.getAppropriate(scm.getClass().getName());
			if (scmProcessor != null) {
				commonOriginRevision = scmProcessor.getCommonOriginRevision(build);
			}
		}
		return commonOriginRevision;
	}

	//  TODO: https://issues.jenkins-ci.org/browse/JENKINS-53410
	private boolean isInternal(Run r) {
		boolean result = false;

		//  get upstream cause, if any
		Cause.UpstreamCause upstreamCause = null;
		for (Object cause : r.getCauses()) {
			if (cause instanceof Cause.UpstreamCause) {
				upstreamCause = (Cause.UpstreamCause) cause;
				break;
			}
		}

		if (upstreamCause != null) {
			String causeJobName = upstreamCause.getUpstreamProject();
			TopLevelItem parent = Jenkins.getInstance().getItem(causeJobName);
			if (parent == null) {
				if (causeJobName.contains("/") && !causeJobName.contains(",")) {
					parent = getJobFromFolder(causeJobName);
					if (parent == null) {
						result = false;
					}
				}
			} else {
				if (parent.getClass().getName().equals(JobProcessorFactory.WORKFLOW_JOB_NAME)) {
					result = true;
				} else {
					List<PipelinePhase> phases = JobProcessorFactory.getFlowProcessor((Job) parent).getInternals();
					for (PipelinePhase p : phases) {
						for (PipelineNode n : p.getJobs()) {
							if (n != null && n.getName().equals(r.getParent().getName())) {
								return true;
							}
						}
					}
					return false;
				}
			}
		}
		return result;
	}

	private static TopLevelItem getJobFromFolder(String causeJobName) {
		String newJobRefId = causeJobName.substring(0, causeJobName.indexOf('/'));
		TopLevelItem item = Jenkins.getInstance().getItem(newJobRefId);
		if (item != null) {
			Collection<? extends Job> allJobs = item.getAllJobs();
			for (Job job : allJobs) {
				if (causeJobName.endsWith(job.getName())) {
					return (TopLevelItem) job;
				}
			}
			return null;
		}
		return null;
	}
}
