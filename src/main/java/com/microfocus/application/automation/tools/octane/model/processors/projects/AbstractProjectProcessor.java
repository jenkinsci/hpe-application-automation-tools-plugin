/*
 * Certain versions of software accessible here may contain branding from Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 * This software was acquired by Micro Focus on September 1, 2017, and is now offered by OpenText.
 * Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright 2012-2024 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.octane.model.processors.projects;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.general.CIBuildStatusInfo;
import com.hp.octane.integrations.dto.pipelines.PipelinePhase;
import com.hp.octane.integrations.dto.snapshots.CIBuildStatus;
import com.hp.octane.integrations.utils.SdkConstants;
import com.hp.octane.integrations.utils.SdkStringUtils;
import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import com.microfocus.application.automation.tools.octane.events.OutputEnvironmentParametersHelper;
import com.microfocus.application.automation.tools.octane.executor.UftConstants;
import com.microfocus.application.automation.tools.octane.model.processors.builders.AbstractBuilderProcessor;
import com.microfocus.application.automation.tools.octane.model.processors.builders.BuildTriggerProcessor;
import com.microfocus.application.automation.tools.octane.model.processors.builders.ParameterizedTriggerProcessor;
import com.microfocus.application.automation.tools.octane.model.processors.parameters.ParameterProcessors;
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
		String buildId = getParameterValueIfExist(parametersAction, UftConstants.BUILD_ID_PARAMETER_NAME);

		String suiteId = getParameterValueIfExist(parametersAction, SdkConstants.JobParameters.SUITE_ID_PARAMETER_NAME);
		String suiteRunId = getParameterValueIfExist(parametersAction, SdkConstants.JobParameters.SUITE_RUN_ID_PARAMETER_NAME);

		String releaseExecutionId = getParameterValueIfExist(parametersAction, SdkConstants.JobParameters.OCTANE_AUTO_ACTION_EXECUTION_ID_PARAMETER_NAME);

		if (buildId != null) {
			logger.info(String.format("cancelBuild for %s, buildId=%s", job.getFullName(), buildId));
			Run aBuild = (job).getBuild(buildId);
			logger.info(String.format("cancelBuild for %s, buildId=%s - is done", job.getFullName(), buildId));
			if (aBuild == null) {
				logger.warn(String.format("Cannot stop : build %s is not found", buildId));
				return;
			}
			stopBuild(aBuild);
		} else {
			FoundInfo foundInfo = new FoundInfo();
			String paramToSearch;
			String paramValueToSearch;
			if (SdkStringUtils.isNotEmpty(releaseExecutionId)) {
				paramToSearch = SdkConstants.JobParameters.OCTANE_AUTO_ACTION_EXECUTION_ID_PARAMETER_NAME;
				paramValueToSearch = releaseExecutionId;
			} else if (SdkStringUtils.isNotEmpty(suiteRunId)) {
				paramToSearch = SdkConstants.JobParameters.SUITE_RUN_ID_PARAMETER_NAME;
				paramValueToSearch = suiteRunId;
			} else {
				throw new IllegalArgumentException("Cannot cancel job as no identification parameters was passed");
			}

			logger.info(String.format("cancelBuild for %s, %s=%s", job.getFullName(), paramToSearch, paramValueToSearch));
			Queue queue = Jenkins.get().getQueue();
			Queue.Task queueTaskJob = (Queue.Task) job;
			queue.getItems(queueTaskJob).forEach(item -> {
				item.getActions(ParametersAction.class).forEach(action -> {
					if (!foundInfo.found && checkIfParamExistAndEqual(action, paramToSearch, paramValueToSearch)) {
						try {
							logger.info("canceling item in queue : " + item);
							queue.cancel(item);
							logger.info("Item in queue is cancelled item : " + item);
							foundInfo.found = true;
						} catch (Exception e) {
							logger.warn("Failed to cancel '" + item + "' in queue : " + e.getMessage(), e);
						}
					}
				});
			});

			job.getBuilds().forEach(build -> {
				if (!foundInfo.found) {
					Run run = (Run)build;
					run.getActions(ParametersAction.class).forEach(action -> {
						if (checkIfParamExistAndEqual(action, paramToSearch, paramValueToSearch)) {
							stopBuild(run);
							foundInfo.found = true;
						}
					});
				}
			});
		}
	}

	public CIBuildStatusInfo getBuildStatus(String paramName, String paramValue) {
		CIBuildStatusInfo status = DTOFactory.getInstance().newDTO(CIBuildStatusInfo.class)
				.setBuildStatus(CIBuildStatus.UNAVAILABLE)
				.setJobCiId(this.getTranslatedJobName())
				.setParamName(paramName)
				.setParamValue(paramValue);
		String buildId = UftConstants.BUILD_ID_PARAMETER_NAME.equals(paramName) ? paramValue : null;

		if (buildId != null) {
			try {
				int buildNum = Integer.parseInt(buildId);
				Run aBuild = job.getBuildByNumber(buildNum);
				if (aBuild == null) {
					status.setBuildStatus(CIBuildStatus.UNAVAILABLE);
				} else {
					status.setBuildCiId(BuildHandlerUtils.getBuildCiId(aBuild));
					status.setAllBuildParams(ParameterProcessors.getInstances(aBuild));
					if (aBuild.isBuilding()) {
						status.setBuildStatus(CIBuildStatus.RUNNING);
					} else {
						status.setBuildStatus(CIBuildStatus.FINISHED);
						status.setResult(BuildHandlerUtils.translateRunResult(aBuild));
					}
				}
			} catch (NumberFormatException e) {
				throw new RuntimeException("Failed to parse build id " + buildId);
			}
		} else {
			FoundInfo foundInfo = new FoundInfo();
			if (job instanceof Queue.Task) {
				Queue.Task queueTaskJob = (Queue.Task) job;
				Queue queue = Jenkins.get().getQueue();
				queue.getItems(queueTaskJob).forEach(item -> {
					item.getActions(ParametersAction.class).forEach(action -> {
						if (!foundInfo.found && checkIfParamExistAndEqual(action, paramName, paramValue)) {
							status.setBuildStatus(CIBuildStatus.QUEUED);
							foundInfo.found = true;
						}
					});
				});
			}

			job.getBuilds().forEach(build -> {
				if (!foundInfo.found) {
					Run aBuild = (Run) build;
					aBuild.getActions(ParametersAction.class).forEach(action -> {
						if (checkIfParamExistAndEqual(action, paramName, paramValue)) {
							if (aBuild.isBuilding()) {
								status.setBuildStatus(CIBuildStatus.RUNNING);
							} else {
								status.setBuildStatus(CIBuildStatus.FINISHED);
								status.setResult(BuildHandlerUtils.translateRunResult(aBuild));
								status.setEnvironmentOutputtedParameters(OutputEnvironmentParametersHelper.getOutputEnvironmentParams(aBuild));
							}
							status.setAllBuildParams(ParameterProcessors.getInstances(aBuild));
							status.setBuildCiId(BuildHandlerUtils.getBuildCiId(aBuild));
							foundInfo.found = true;
						}
					});
				}
			});
		}

		return status;
	}

	private String getParameterValueIfExist(ParametersAction parametersAction, String paramName) {
		ParameterValue pv = parametersAction.getParameter(paramName);
		if (pv != null) {
			return (String) pv.getValue();
		} else {
			return null;
		}
	}

	protected void stopBuild(Run run) {
		AbstractBuild aBuild = (AbstractBuild)run;
		try {
			aBuild.doStop();
			logger.info("Build is stopped : " + aBuild.getProject().getDisplayName() + aBuild.getDisplayName());
		} catch (Exception e) {
			logger.warn("Failed to stop build '" + aBuild.getDisplayName() + "' :" + e.getMessage(), e);
		}
	}

	private boolean checkIfParamExistAndEqual(ParametersAction parametersAction, String paramName, String expectedValue) {
		ParameterValue pv = parametersAction.getParameter(paramName);
		return (SdkStringUtils.isNotEmpty(expectedValue) && pv != null && pv.getValue().equals(expectedValue));
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
	private static class FoundInfo{
		public boolean found;
	}
}
