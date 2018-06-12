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

import com.google.inject.Inject;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.events.CIEvent;
import com.hp.octane.integrations.dto.events.CIEventType;
import com.hp.octane.integrations.dto.events.MultiBranchType;
import com.hp.octane.integrations.dto.events.PhaseType;
import com.hp.octane.integrations.dto.pipelines.PipelineNode;
import com.hp.octane.integrations.dto.pipelines.PipelinePhase;
import com.hp.octane.integrations.dto.snapshots.CIBuildResult;
import com.hpe.application.automation.tools.octane.configuration.ConfigurationService;
import com.hpe.application.automation.tools.octane.model.CIEventCausesFactory;
import com.hpe.application.automation.tools.octane.model.processors.parameters.ParameterProcessors;
import com.hpe.application.automation.tools.octane.model.processors.projects.JobProcessorFactory;
import com.hpe.application.automation.tools.octane.model.processors.scm.SCMProcessor;
import com.hpe.application.automation.tools.octane.model.processors.scm.SCMProcessors;
import com.hpe.application.automation.tools.octane.tests.TestListener;
import com.hpe.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import hudson.Extension;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixRun;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import hudson.scm.SCM;
import jenkins.model.Jenkins;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Run Listener that handles basic CI events and dispatches notifications to the Octane server
 * User: gullery
 * Date: 24/08/14
 * Time: 17:21
 */

@Extension
@SuppressWarnings({"squid:S2259", "squid:S1872", "squid:S1698", "squid:S1132"})
public final class RunListenerImpl extends RunListener<Run> {
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();
	private ExecutorService executor = new ThreadPoolExecutor(0, 5, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

	@Inject
	private TestListener testListener;

	@Override
	public void onStarted(final Run r, TaskListener listener) {
		if (!ConfigurationService.getServerConfiguration().isValid()) {
			return;
		}
		if (ConfigurationService.getModel().isSuspend()) {
			return;
		}

		CIEvent event;
		if (r.getClass().getName().equals("org.jenkinsci.plugins.workflow.job.WorkflowRun")) {
			event = dtoFactory.newDTO(CIEvent.class)
					.setEventType(CIEventType.STARTED)
					.setProject(BuildHandlerUtils.getJobCiId(r))
					.setBuildCiId(BuildHandlerUtils.getBuildCiId(r))
					.setNumber(String.valueOf(r.getNumber()))
					.setStartTime(r.getStartTimeInMillis())
					.setPhaseType(PhaseType.POST)
					.setEstimatedDuration(r.getEstimatedDuration())
					.setCauses(CIEventCausesFactory.processCauses(extractCauses(r)));

			if (r.getParent().getParent() != null && r.getParent().getParent().getClass().getName().equals(JobProcessorFactory.WORKFLOW_MULTI_BRANCH_JOB_NAME)) {
				event
						.setParentCiId(r.getParent().getParent().getFullName())
						.setMultiBranchType(MultiBranchType.MULTI_BRANCH_CHILD)
						.setProjectDisplayName(r.getParent().getFullName());
			}

			OctaneSDK.getInstance().getEventsService().publishEvent(event);
			//events on the internal stages of the workflowRun are handled in this place:
			// com.hpe.application.automation.tools.octane.workflow.WorkflowGraphListener

		} else {
			if (r.getParent() instanceof MatrixConfiguration) {
				event = dtoFactory.newDTO(CIEvent.class)
						.setEventType(CIEventType.STARTED)
						.setProject(BuildHandlerUtils.getJobCiId(r))
						.setProjectDisplayName(BuildHandlerUtils.getJobCiId(r))
						.setBuildCiId(BuildHandlerUtils.getBuildCiId(r))
						.setNumber(String.valueOf(r.getNumber()))
						.setStartTime(r.getStartTimeInMillis())
						.setEstimatedDuration(r.getEstimatedDuration())
						.setCauses(CIEventCausesFactory.processCauses(extractCauses(r)))
						.setParameters(ParameterProcessors.getInstances(r));
				if (isInternal(r)) {
					event.setPhaseType(PhaseType.INTERNAL);
				} else {
					event.setPhaseType(PhaseType.POST);
				}
				OctaneSDK.getInstance().getEventsService().publishEvent(event);
			} else if (r instanceof AbstractBuild) {
				event = dtoFactory.newDTO(CIEvent.class)
						.setEventType(CIEventType.STARTED)
						.setProject(BuildHandlerUtils.getJobCiId(r))
						.setProjectDisplayName(BuildHandlerUtils.getJobCiId(r))
						.setBuildCiId(BuildHandlerUtils.getBuildCiId(r))
						.setNumber(String.valueOf(r.getNumber()))
						.setStartTime(r.getStartTimeInMillis())
						.setEstimatedDuration(r.getEstimatedDuration())
						.setCauses(CIEventCausesFactory.processCauses(extractCauses(r)))
						.setParameters(ParameterProcessors.getInstances(r));
				if (isInternal(r)) {
					event.setPhaseType(PhaseType.INTERNAL);
				} else {
					event.setPhaseType(PhaseType.POST);
				}
				OctaneSDK.getInstance().getEventsService().publishEvent(event);
			}
		}
	}

	@Override
	public void onFinalized(Run r) {
		if (onFinelizedValidations()) return;

		SCMProcessor.CommonOriginRevision commonOriginRevision=null;// = getCommonOriginRevision(r);

		boolean hasTests = testListener.processBuild(r);

		CIBuildResult result;
		result = getCiBuildResult(r);
		CIEvent event = getCiEvent(r, commonOriginRevision, hasTests, result);

		if (r instanceof AbstractBuild) {
			event.setParameters(ParameterProcessors.getInstances(r))
					.setProjectDisplayName(BuildHandlerUtils.getJobCiId(r));
		}
		OctaneSDK.getInstance().getEventsService().publishEvent(event);
	}



	private CIEvent getCiEvent(Run r, SCMProcessor.CommonOriginRevision commonOriginRevision, boolean hasTests, CIBuildResult result) {
		return dtoFactory.newDTO(CIEvent.class)
				.setEventType(CIEventType.FINISHED)
				.setBuildCiId(BuildHandlerUtils.getBuildCiId(r))
				.setNumber(String.valueOf(r.getNumber()))
				.setProject(BuildHandlerUtils.getJobCiId(r))
				.setStartTime(r.getStartTimeInMillis())
				.setEstimatedDuration(r.getEstimatedDuration())
				.setCauses(CIEventCausesFactory.processCauses(extractCauses(r)))
				.setResult(result)
				.setDuration(r.getDuration())
				.setCommonHashId(commonOriginRevision != null ? commonOriginRevision.revision : null)
				.setBranchName(commonOriginRevision != null ? commonOriginRevision.branch : null)
				.setTestResultExpected(hasTests);
	}

	private boolean onFinelizedValidations() {
		return (!ConfigurationService.getServerConfiguration().isValid() ||
				ConfigurationService.getModel().isSuspend());
	}

	private CIBuildResult getCiBuildResult(Run r) {
		CIBuildResult result;
		if (r.getResult() == Result.SUCCESS) {
			result = CIBuildResult.SUCCESS;
		} else if (r.getResult() == Result.ABORTED) {
			result = CIBuildResult.ABORTED;
		} else if (r.getResult() == Result.FAILURE) {
			result = CIBuildResult.FAILURE;
		} else if (r.getResult() == Result.UNSTABLE) {
			result = CIBuildResult.UNSTABLE;
		} else {
			result = CIBuildResult.UNAVAILABLE;
		}
		return result;
	}

	private SCMProcessor.CommonOriginRevision getCommonOriginRevision(Run r) {
		SCMProcessor.CommonOriginRevision commonOriginRevision=null;
		if(r instanceof AbstractBuild) {
			final SCM scm = ((AbstractBuild) r).getProject().getScm();
			if (scm != null) {
				SCMProcessor scmProcessor = SCMProcessors.getAppropriate(scm.getClass().getName());
				if(scmProcessor!=null) {
					commonOriginRevision = scmProcessor.getCommonOriginRevision(r);
				}
			}
		}
		return commonOriginRevision;
	}

	//  TODO: [YG] this method should be part of causes factory or something like this, it is not suitable for merged build as well
	private boolean isInternal(Run r) {
		boolean result = false;

		//  get upstream cause, if any
		Cause.UpstreamCause upstreamCause = null;
		for (Cause cause : (List<Cause>) r.getCauses()) {
			if (cause instanceof Cause.UpstreamCause) {
				upstreamCause = (Cause.UpstreamCause) cause;
				break;          //  TODO: here we are breaking the merged build support
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

	private static List<Cause> extractCauses(Run<?, ?> r) {
		if (r.getParent() instanceof MatrixConfiguration) {
			return ((MatrixRun) r).getParentBuild().getCauses();
		}
		return r.getCauses();
	}
}
