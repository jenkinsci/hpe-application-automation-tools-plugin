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

package com.hpe.application.automation.tools.octane.events;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.events.CIEvent;
import com.hp.octane.integrations.dto.events.CIEventType;
import com.hp.octane.integrations.dto.events.PhaseType;
import com.hp.octane.integrations.dto.pipelines.PipelineNode;
import com.hp.octane.integrations.dto.pipelines.PipelinePhase;
import com.hp.octane.integrations.dto.snapshots.CIBuildResult;
import com.hpe.application.automation.tools.octane.configuration.ConfigurationService;
import com.hpe.application.automation.tools.octane.executor.UftJobRecognizer;
import com.hpe.application.automation.tools.octane.model.CIEventCausesFactory;
import com.hpe.application.automation.tools.octane.model.processors.builders.WorkFlowRunProcessor;
import com.hpe.application.automation.tools.octane.model.processors.parameters.ParameterProcessors;
import com.hpe.application.automation.tools.octane.model.processors.projects.JobProcessorFactory;
import com.hpe.application.automation.tools.octane.tests.HPRunnerType;
import com.hpe.application.automation.tools.octane.tests.MqmTestsExtension;
import com.hpe.application.automation.tools.octane.tests.TestResultContainer;
import com.hpe.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import hudson.Extension;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixRun;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import jenkins.model.Jenkins;
import org.apache.logging.log4j.LogManager;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: gullery
 * Date: 24/08/14
 * Time: 17:21
 */

@Extension
@SuppressWarnings({"squid:S2259","squid:S1872","squid:S1698","squid:S1132"})
public final class RunListenerImpl extends RunListener<Run> {
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();
	private ExecutorService executor = new ThreadPoolExecutor(0, 5, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	private static final Logger logger = LogManager.getLogger(RunListenerImpl.class);
	@Override
	public void onStarted(final Run r, TaskListener listener) {
		if(!ConfigurationService.getServerConfiguration().isValid()){
			return;
		}

		CIEvent event;
		if (r.getClass().getName().equals("org.jenkinsci.plugins.workflow.job.WorkflowRun")) {
			event = dtoFactory.newDTO(CIEvent.class)
					.setEventType(CIEventType.STARTED)
					.setProject(BuildHandlerUtils.getJobCiId(r))
					.setBuildCiId(String.valueOf(r.getNumber()))
					.setNumber(String.valueOf(r.getNumber()))
					.setStartTime(r.getStartTimeInMillis())
					.setPhaseType(PhaseType.POST)
					.setEstimatedDuration(r.getEstimatedDuration())
					.setCauses(CIEventCausesFactory.processCauses(extractCauses(r)));
			EventsService.getExtensionInstance().dispatchEvent(event);
			WorkFlowRunProcessor workFlowRunProcessor = new WorkFlowRunProcessor(r);
			workFlowRunProcessor.registerEvents(executor);
		} else {
			if (r.getParent() instanceof MatrixConfiguration) {
				AbstractBuild build = (AbstractBuild) r;
				event = dtoFactory.newDTO(CIEvent.class)
						.setEventType(CIEventType.STARTED)
						.setProject(BuildHandlerUtils.getJobCiId(r))
						.setProjectDisplayName(BuildHandlerUtils.getJobCiId(r))
						.setBuildCiId(String.valueOf(build.getNumber()))
						.setNumber(String.valueOf(build.getNumber()))
						.setStartTime(build.getStartTimeInMillis())
						.setEstimatedDuration(build.getEstimatedDuration())
						.setCauses(CIEventCausesFactory.processCauses(extractCauses(build)))
						.setParameters(ParameterProcessors.getInstances(build));
				if (isInternal(r)) {
					event.setPhaseType(PhaseType.INTERNAL);
				} else {
					event.setPhaseType(PhaseType.POST);
				}
				EventsService.getExtensionInstance().dispatchEvent(event);
			} else if (r instanceof AbstractBuild) {
				AbstractBuild build = (AbstractBuild) r;
				event = dtoFactory.newDTO(CIEvent.class)
						.setEventType(CIEventType.STARTED)
						.setProject(BuildHandlerUtils.getJobCiId(r))
						.setProjectDisplayName(BuildHandlerUtils.getJobCiId(r))
						.setBuildCiId(String.valueOf(build.getNumber()))
						.setNumber(String.valueOf(build.getNumber()))
						.setStartTime(build.getStartTimeInMillis())
						.setEstimatedDuration(build.getEstimatedDuration())
						.setCauses(CIEventCausesFactory.processCauses(extractCauses(build)))
						.setParameters(ParameterProcessors.getInstances(build));
				if (isInternal(r)) {
					event.setPhaseType(PhaseType.INTERNAL);
				} else {
					event.setPhaseType(PhaseType.POST);
				}
				EventsService.getExtensionInstance().dispatchEvent(event);
			}
		}
	}

	@Override
	public void onFinalized(Run r)
	{
		if(!ConfigurationService.getServerConfiguration().isValid()){
			return;
		}

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
		CIEvent	event = dtoFactory.newDTO(CIEvent.class)
			.setEventType(CIEventType.FINISHED)
			.setBuildCiId(String.valueOf(r.getNumber()))
			.setNumber(String.valueOf(r.getNumber()))
			.setProject(BuildHandlerUtils.getJobCiId(r))
			.setStartTime(r.getStartTimeInMillis())
			.setEstimatedDuration(r.getEstimatedDuration())
			.setCauses(CIEventCausesFactory.processCauses(extractCauses(r)))
			.setResult(result)
			.setDuration(r.getDuration());

		try {
			if (r.getResult() == Result.FAILURE) {
				Boolean hasTests = hasUftTests(r);
				if (hasTests != null) {
					event.setTestResultExpected(hasTests);
				}
			}
		} catch (Exception e) {
			logger.log(Level.WARN,"hasUftTests error",e);
		}

		if(r instanceof AbstractBuild){
			event.setParameters(ParameterProcessors.getInstances(r))
				.setProjectDisplayName(BuildHandlerUtils.getJobCiId(r));
		}
		EventsService.getExtensionInstance().dispatchEvent(event);
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
				if (parent.getClass().getName().equals("org.jenkinsci.plugins.workflow.job.WorkflowJob")) {
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

    private static Boolean hasUftTests(Run build) {
        if (build.getParent() instanceof FreeStyleProject && UftJobRecognizer.isExecutorJob((FreeStyleProject) build.getParent())) {
            try {
                boolean hasTests = false;
                for (MqmTestsExtension ext : MqmTestsExtension.all()) {
                    if (ext.supports(build)) {
                        String jenkinsRootUrl = Jenkins.getInstance().getRootUrl();
                        List<Run> buildsList = BuildHandlerUtils.getBuildPerWorkspaces(build);

                        for (Run buildX : buildsList) {
                            TestResultContainer testResultContainer = ext.getTestResults(buildX, HPRunnerType.UFT, jenkinsRootUrl);
                            if (testResultContainer != null && testResultContainer.getIterator().hasNext()) {
                                hasTests = true;
                            }
                        }
                    }
                }
                return hasTests;
            } catch (Exception e) {
                logger.log(Level.WARN,"Could not check uft tests exists",e);
            }
        }

        return null;
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

	private static List<Cause> extractCauses(Run r) {
		if (r.getParent() instanceof MatrixConfiguration) {
			return ((MatrixRun) r).getParentBuild().getCauses();
		}

		return r.getCauses();
	}
}
