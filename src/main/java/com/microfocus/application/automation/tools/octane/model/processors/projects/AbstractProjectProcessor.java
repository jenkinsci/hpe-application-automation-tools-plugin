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

package com.microfocus.application.automation.tools.octane.model.processors.projects;

import com.hp.octane.integrations.dto.pipelines.PipelinePhase;
import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import com.microfocus.application.automation.tools.octane.executor.UftConstants;
import com.microfocus.application.automation.tools.octane.model.processors.builders.AbstractBuilderProcessor;
import com.microfocus.application.automation.tools.octane.model.processors.builders.BuildTriggerProcessor;
import com.microfocus.application.automation.tools.octane.model.processors.builders.ParameterizedTriggerProcessor;
import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import hudson.model.*;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import jenkins.model.Jenkins;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: gullery
 * Date: 09/01/15
 * Time: 00:59
 * To change this template use File | Settings | File Templates.
 */

@SuppressWarnings({"squid:S1132", "squid:S1872"})
public abstract class AbstractProjectProcessor<T extends Job> {
	private static final Logger logger = SDKBasedLoggerProvider.getLogger(AbstractProjectProcessor.class);
	private final List<PipelinePhase> internals = new ArrayList<>();
	private final List<PipelinePhase> postBuilds = new ArrayList<>();
	private boolean isProcessed = false;
	T job;

	AbstractProjectProcessor(T job) {
		this.job = job;
	}

	/**
	 * Attempt to retrieve an [internal] build phases of the Job
	 *
	 * @return list of builders
	 */
	public List<Builder> tryGetBuilders() {
		return new ArrayList<>();
	}

	/**
	 * Enqueue Job's run with the specified parameters
	 */
	public void scheduleBuild(Cause cause, ParametersAction parametersAction) {
		if (job instanceof AbstractProject) {
			AbstractProject project = (AbstractProject) job;
			int delay = project.getQuietPeriod();
			project.scheduleBuild(delay, cause, parametersAction);
		} else {
			throw new IllegalStateException("unsupported job CAN NOT be run");
		}
	}

	public void cancelBuild(Cause cause, ParametersAction parametersAction) {
		String suiteId = (String) parametersAction.getParameter(UftConstants.SUITE_ID_PARAMETER_NAME).getValue();
		String suiteRunId = (String) parametersAction.getParameter(UftConstants.SUITE_RUN_ID_PARAMETER_NAME).getValue();
		logger.info("cancelBuild for suiteId=" + suiteId +", suiteRunId=" + suiteRunId);
		if (job instanceof AbstractProject) {
			AbstractProject project = (AbstractProject) job;
			Queue queue = Jenkins.get().getQueue();
			queue.getItems(project).forEach(item -> {
				item.getActions(ParametersAction.class).forEach(action -> {
					if (checkSuiteIdParamsExistAndEqual(action, suiteId, suiteRunId)) {
						try {
							logger.info("canceling item in queue : " + item.toString());
							queue.cancel(item);
							logger.info("Item in queue is cancelled item : " + item.toString());
						} catch (Exception e) {
							logger.warn("Failed to cancel '" + item.toString() + "' in queue : " + e.getMessage(), e);
						}
					}
				});
			});

			project.getBuilds().forEach(build -> {
				if (build instanceof AbstractBuild) {
					AbstractBuild aBuild = (AbstractBuild) build;
					aBuild.getActions(ParametersAction.class).forEach(action -> {
						if (checkSuiteIdParamsExistAndEqual(action, suiteId, suiteRunId)) {
							try {
								aBuild.doStop();
								logger.info("Build is stopped : " + aBuild.getProject().getDisplayName() + aBuild.getDisplayName());
							} catch (Exception e) {
								logger.warn("Failed to stop build '" + aBuild.getDisplayName() + "' :" + e.getMessage(), e);
							}
						}
					});
				}
			});
		} else {
			throw new IllegalStateException("unsupported job CAN NOT be stopped");
		}
	}

	private boolean checkSuiteIdParamsExistAndEqual(ParametersAction parametersAction, String suiteId, String suiteRunId) {
		ParameterValue suiteIdPV = parametersAction.getParameter(UftConstants.SUITE_ID_PARAMETER_NAME);
		ParameterValue suiteRunIdPV = parametersAction.getParameter(UftConstants.SUITE_RUN_ID_PARAMETER_NAME);
		return (suiteIdPV != null && suiteRunIdPV != null && suiteIdPV.getValue().equals(suiteId)
				&& suiteRunIdPV.getValue().equals(suiteRunId));
	}

	/**
	 * Retrieve Job's CI ID
	 * return the job name, in case of a folder job, this method returns the refactored
	 * name that matches the required pattern.
	 *
	 * @return Job's CI ID
	 */
	public String getTranslatedJobName() {
		if (JobProcessorFactory.FOLDER_JOB_NAME.equals(job.getParent().getClass().getName())) {
			String jobPlainName = job.getFullName();    // e.g: myFolder/myJob
			return BuildHandlerUtils.translateFolderJobName(jobPlainName);
		} else {
			return job.getName();
		}
	}

	public void buildStructure(Set<Job> processedJobs) {
		processedJobs.add(job);
		buildStructureInternal(processedJobs);
		processedJobs.remove(job);
		isProcessed = true;
	}

	protected void buildStructureInternal(Set<Job> processedJobs){
	}

	/**
	 * Discover an internal phases of the Job
	 *
	 * @return list of phases
	 */
	public List<PipelinePhase> getInternals() {
		if (!isProcessed) {
			buildStructure(new HashSet<>());
		}
		return internals;
	}

	/**
	 * Discover a post build phases of the Job
	 *
	 * @return list of phases
	 */
	public List<PipelinePhase> getPostBuilds() {
		if (!isProcessed) {
			buildStructure(new HashSet<>());
		}
		return postBuilds;
	}

	/**
	 * Internal API
	 * Processes and prepares Job's children for future use - internal flow
	 *
	 * @param builders      Job's builders
	 * @param job           Job to process
	 * @param processedJobs previously processed Jobs in this Job's hierarchical chain in order to break the recursive flows
	 */
	void processBuilders(List<Builder> builders, Job job, Set<Job> processedJobs) {
		this.processBuilders(builders, job, "", processedJobs);
	}

	/**
	 * Internal API
	 * Processes and prepares Job's children for future use - internal flow
	 *
	 * @param builders      Job's builders
	 * @param job           Job to process
	 * @param phasesName    Targeted phase name in case of available one
	 * @param processedJobs previously processed Jobs in this Job's hierarchical chain in order to break the recursive flows
	 */
	void processBuilders(List<Builder> builders, Job job, String phasesName, Set<Job> processedJobs) {
		for (Builder builder : builders) {
			AbstractBuilderProcessor.processInternalBuilders(builder, job, phasesName, internals, processedJobs);
		}
	}

	/**
	 * Internal API
	 * Processes and prepares Job's children for future use - post build flow
	 *
	 * @param job           Job to process
	 * @param processedJobs previously processed Jobs in this Job's hierarchical chain in order to break the recursive flows
	 */
	@SuppressWarnings("unchecked")
	void processPublishers(Job job, Set<Job> processedJobs) {
		if (job instanceof AbstractProject) {
			AbstractProject project = (AbstractProject) job;
			processedJobs.add(job);
			AbstractBuilderProcessor builderProcessor;
			List<Publisher> publishers = project.getPublishersList();
			for (Publisher publisher : publishers) {
				builderProcessor = null;
				if (publisher.getClass().getName().equals(JobProcessorFactory.SIMPLE_BUILD_TRIGGER)) {
					builderProcessor = new BuildTriggerProcessor(publisher, project, processedJobs);
				} else if (publisher.getClass().getName().equals(JobProcessorFactory.PARAMETRIZED_BUILD_TRIGGER)) {
					builderProcessor = new ParameterizedTriggerProcessor(publisher, project, "", processedJobs);
				}
				if (builderProcessor != null) {
					postBuilds.addAll(builderProcessor.getPhases());
				} else {
					logger.debug("not yet supported publisher (post build) action: " + publisher.getClass().getName());
				}
			}
			processedJobs.remove(job);
		}
	}
}
