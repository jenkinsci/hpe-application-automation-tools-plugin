/*
 *  Certain versions of software accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 *  This software was acquired by Micro Focus on September 1, 2017, and is now
 *  offered by OpenText.
 *  Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical
 *  in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the
 *  property of their respective owners.
 *  OpenText is a trademark of Open Text.
 *  __________________________________________________________________
 *  MIT License
 *
 *  Copyright 2012-2025 Open Text.
 *
 *  The only warranties for products and services of Open Text and
 *  its affiliates and licensors ("Open Text") are as may be set forth
 *  in the express warranty statements accompanying such products and services.
 *  Nothing herein should be construed as constituting an additional warranty.
 *  Open Text shall not be liable for technical or editorial errors or
 *  omissions contained herein. The information contained herein is subject
 *  to change without notice.
 *
 *  Except as specifically indicated otherwise, this document contains
 *  confidential information and a valid license is required for possession,
 *  use or copying. If this work is provided to the U.S. Government,
 *  consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 *  Computer Software Documentation, and Technical Data for Commercial Items are
 *  licensed to the U.S. Government under vendor's standard commercial license.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ___________________________________________________________________
 */
package com.microfocus.application.automation.tools.octane.events;

import com.google.inject.Inject;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.causes.CIEventCause;
import com.hp.octane.integrations.dto.causes.CIEventCauseType;
import com.hp.octane.integrations.dto.events.CIEvent;
import com.hp.octane.integrations.dto.events.CIEventType;
import com.hp.octane.integrations.dto.events.MultiBranchType;
import com.hp.octane.integrations.dto.events.PhaseType;
import com.hp.octane.integrations.dto.snapshots.CIBuildResult;
import com.microfocus.application.automation.tools.octane.CIJenkinsServicesImpl;
import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import com.microfocus.application.automation.tools.octane.model.CIEventCausesFactory;
import com.microfocus.application.automation.tools.octane.model.processors.parameters.ParameterProcessors;
import com.microfocus.application.automation.tools.octane.model.processors.projects.JobProcessorFactory;
import com.microfocus.application.automation.tools.octane.model.processors.scm.CommonOriginRevision;
import com.microfocus.application.automation.tools.octane.model.processors.scm.SCMProcessor;
import com.microfocus.application.automation.tools.octane.model.processors.scm.SCMProcessors;
import com.microfocus.application.automation.tools.octane.tests.TestListener;
import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import hudson.Extension;
import hudson.model.Result;
import hudson.scm.SCM;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.actions.WarningAction;
import org.jenkinsci.plugins.workflow.cps.nodes.StepEndNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.flow.GraphListener;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import java.lang.reflect.Method;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Collection;

/**
 * Octane's listener for WorkflowRun events
 * - this listener should handle Pipeline's STARTED and FINISHED events
 * - this listener should handle each stage's STARTED and FINISHED events
 *
 * User: gadiel
 * Date: 07/06/2016
 * Time: 17:21
 */

@Extension
public class WorkflowListenerOctaneImpl implements GraphListener {
	private static final Logger logger = SDKBasedLoggerProvider.getLogger(WorkflowListenerOctaneImpl.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	//After upgrading Pipeline:Groovy plugin to Version 2.64: receive two start events, therefore
	// pipeline job shows 2 bars for a single pipeline run.
	// Here we add job key during start event and remove key in finished event
	private static Set<String> workflowJobStarted = new HashSet<>();
	@Inject
	private TestListener testListener;

	@Override
	public void onNewHead(FlowNode flowNode) {
		if(!OctaneSDK.hasClients()){
			return;
		}
		try {
			if (BuildHandlerUtils.isWorkflowStartNode(flowNode)) {
				sendPipelineStartedEvent(flowNode);
			} else if (BuildHandlerUtils.isWorkflowEndNode(flowNode)) {
				WorkflowRun parentRun = BuildHandlerUtils.extractParentRun(flowNode);
				sendPipelineFinishedEvent(parentRun);
				BuildLogHelper.enqueueBuildLog(parentRun);
			} else if (BuildHandlerUtils.isStageStartNode(flowNode)) {
				sendStageStartedEvent((StepStartNode) flowNode);
			} else if (BuildHandlerUtils.isStageEndNode(flowNode)) {
				sendStageFinishedEvent((StepEndNode) flowNode);
			}
		} catch (Throwable throwable) {
			logger.error("failed to build and/or dispatch STARTED/FINISHED event for " + flowNode, throwable);
		}
	}

	private void sendPipelineStartedEvent(FlowNode flowNode) {
		WorkflowRun parentRun = BuildHandlerUtils.extractParentRun(flowNode);

		//Avoid duplicate start events
		String buildKey = getBuildKey(parentRun);
		if (workflowJobStarted.contains(buildKey)) {
			return;
		} else {
			workflowJobStarted.add(buildKey);
		}

		CIEvent event = dtoFactory.newDTO(CIEvent.class)
				.setEventType(CIEventType.STARTED)
				.setProjectDisplayName(BuildHandlerUtils.translateFullDisplayName(parentRun.getParent().getFullDisplayName()))
				.setProject(BuildHandlerUtils.getJobCiId(parentRun))
				.setBuildCiId(BuildHandlerUtils.getBuildCiId(parentRun))
				.setNumber(String.valueOf(parentRun.getNumber()))
				.setParameters(ParameterProcessors.getInstances(parentRun))
				.setStartTime(parentRun.getStartTimeInMillis())
				.setEstimatedDuration(parentRun.getEstimatedDuration())
				.setCauses(CIEventCausesFactory.processCauses(parentRun));

		if(isInternal(event.getCauses())){
			event.setPhaseType(PhaseType.INTERNAL);
		}
		if (parentRun.getParent().getParent().getClass().getName().equals(JobProcessorFactory.WORKFLOW_MULTI_BRANCH_JOB_NAME)) {
			event
					.setParentCiId(BuildHandlerUtils.translateFolderJobName(parentRun.getParent().getParent().getFullName()))
					.setMultiBranchType(MultiBranchType.MULTI_BRANCH_CHILD)
					.setProjectDisplayName(BuildHandlerUtils.translateFullDisplayName(parentRun.getParent().getFullDisplayName()));
		}

		CIJenkinsServicesImpl.publishEventToRelevantClients(event);
	}

	private boolean isInternal(List<CIEventCause> causes) {
		if (causes != null) {
			for (CIEventCause cause : causes) {
				if (CIEventCauseType.UPSTREAM.equals(cause.getType())) {
					return true;
				}
			}
		}
		return false;
	}

	private String getBuildKey(WorkflowRun run){
		return run.getFullDisplayName();
	}

	// Reflection method names
	private static final String METHOD_GET_DEFINITION = "getDefinition";
	private static final String METHOD_GET_SCM = "getScm";
	private static final String METHOD_GET_SCMS = "getSCMs";

	/**
	 * Extracts the common origin revision (merge-base) for a WorkflowRun.
	 * This is used for code coverage comparison between branches in Octane.
	 *
	 * @param run the WorkflowRun to process
	 * @return CommonOriginRevision containing branch and revision info, or null if unable to extract
	 */
	private CommonOriginRevision getCommonOriginRevision(WorkflowRun run) {
		try {
			Object jobParent = run.getParent();
			if (jobParent == null) {
				logger.warn("Job parent is null for run: {}", run.getFullDisplayName());
				return null;
			}

			SCM scm = extractScmFromJob(jobParent);
			if (scm != null) {
				return processScmForCommonOrigin(run, scm);
			}
		} catch (Exception e) {
			logger.error("Failed to resolve common origin revision for pipeline run: {}", e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Processes the SCM to extract common origin revision information.
	 * @param run the WorkflowRun being processed
	 * @param scm the SCM object associated with the run
	 * @return CommonOriginRevision containing branch and revision info, or null if unable to extract
	 */
	private CommonOriginRevision processScmForCommonOrigin(WorkflowRun run, SCM scm) {
		logger.debug("SCM found: {}, getting SCMProcessor", scm.getClass().getName());
		SCMProcessor scmProcessor = SCMProcessors.getAppropriate(scm.getClass().getName());

		if (scmProcessor != null) {
			logger.debug("SCMProcessor found: {}, calling getCommonOriginRevision", scmProcessor.getClass().getName());
			CommonOriginRevision commonOriginRevision = scmProcessor.getCommonOriginRevision(run);
			logger.info("Common Origin revision is {}", commonOriginRevision);

			return commonOriginRevision;
		}

		return null;
	}

	/**
	 * Extracts SCM from a job object using reflection.
	 * For both regular Pipeline jobs and MultiBranch Pipeline children, the parent is always
	 * a WorkflowJob, so the same extraction strategies apply to both.
	 *
	 * @param job the job object (WorkflowJob, AbstractProject, etc.)
	 * @return the SCM object, or null if not found
	 */
	private SCM extractScmFromJob(Object job) {
		if (job == null) {
			return null;
		}
		return extractScmDirectly(job);
	}

	/**
	 * Directly extracts SCM from a job using various reflection strategies.
	 *
	 * @param job the job object to inspect
	 * @return the SCM object, or null if not found
	 */
	private SCM extractScmDirectly(Object job) {
		try {
			SCM scm = tryExtractScmViaGetScm(job);
			if (scm != null) {
				return scm;
			}
		} catch (ReflectiveOperationException e) {
			logger.debug("Job {} does not support getScm(), trying getDefinition().getScm()", job.getClass().getSimpleName());
		}

		try {
			SCM scm = tryExtractScmViaDefinition(job);
			if (scm != null) {
				return scm;
			}
		} catch (ReflectiveOperationException e) {
			logger.debug("Job {} does not support getDefinition().getScm(), trying getSCMs()", job.getClass().getSimpleName());
		}

		try {
			return tryExtractScmViaScmsCollection(job);
		} catch (ReflectiveOperationException e) {
			logger.debug("Could not extract SCM from job {} using fallback strategies: {}", job.getClass().getSimpleName(), e.getMessage());
		}
		return null;
	}

	/**
	 * Attempts to extract SCM by invoking {@code getScm()} directly on the job.
	 *
	 * @param job the job instance to inspect
	 * @return extracted SCM, or null when the returned object is not an SCM
	 * @throws ReflectiveOperationException when {@code getScm()} cannot be resolved or invoked
	 */
	private SCM tryExtractScmViaGetScm(Object job) throws ReflectiveOperationException {
		Method getScmMethod = job.getClass().getMethod(METHOD_GET_SCM);
		Object scmObj = getScmMethod.invoke(job);
		if (scmObj instanceof SCM scm) {
			return scm;
		}
		return null;
	}

	/**
	 * Attempts to extract SCM from a pipeline definition via {@code getDefinition().getScm()}.
	 *
	 * @param job the job instance that may expose a pipeline definition
	 * @return extracted SCM, or null when definition/scm is unavailable
	 * @throws ReflectiveOperationException when reflection calls cannot be resolved or invoked
	 */
	private SCM tryExtractScmViaDefinition(Object job) throws ReflectiveOperationException {
		Method getDefinitionMethod = job.getClass().getMethod(METHOD_GET_DEFINITION);
		Object definition = getDefinitionMethod.invoke(job);
		if (definition == null) {
			return null;
		}

		Method getScmFromDefinitionMethod = definition.getClass().getMethod(METHOD_GET_SCM);
		Object scmObj = getScmFromDefinitionMethod.invoke(definition);
		if (scmObj instanceof SCM scm) {
			return scm;
		}
		return null;
	}

	/**
	 * Attempts to extract SCM from jobs exposing multiple SCMs via {@code getSCMs()}.
	 *
	 * @param job the job instance to inspect
	 * @return first SCM found in the returned collection, or null when none is present
	 * @throws ReflectiveOperationException when {@code getSCMs()} cannot be resolved or invoked
	 */
	private SCM tryExtractScmViaScmsCollection(Object job) throws ReflectiveOperationException {
		Method getScmsMethod = job.getClass().getMethod(METHOD_GET_SCMS);
		Object scmsObj = getScmsMethod.invoke(job);
		if (scmsObj instanceof Collection<?> scmCollection) {
			for (Object scmObj : scmCollection) {
				if (scmObj instanceof SCM scm) {
					return scm;
				}
			}
		}
		return null;
	}

	private void sendPipelineFinishedEvent(WorkflowRun parentRun) {
		workflowJobStarted.remove(getBuildKey(parentRun));
		boolean hasTests = testListener.processBuild(parentRun);

		CIEvent event = dtoFactory.newDTO(CIEvent.class)
				.setEventType(CIEventType.FINISHED)
				.setProject(BuildHandlerUtils.getJobCiId(parentRun))
				.setBuildCiId(BuildHandlerUtils.getBuildCiId(parentRun))
				.setNumber(String.valueOf(parentRun.getNumber()))
				.setParameters(ParameterProcessors.getInstances(parentRun))
				.setStartTime(parentRun.getStartTimeInMillis())
				.setEstimatedDuration(parentRun.getEstimatedDuration())
				.setDuration(parentRun.getDuration())
				.setResult(BuildHandlerUtils.translateRunResult(parentRun))
				.setCauses(CIEventCausesFactory.processCauses(parentRun))
				.setTestResultExpected(hasTests)
				.setEnvironmentOutputtedParameters(OutputEnvironmentParametersHelper.getOutputEnvironmentParams(parentRun));

		// Set commonHashId and branchName for pipeline run comparison
		setCommonHashOnEvent(event, getCommonOriginRevision(parentRun), parentRun.getFullDisplayName());

		CIJenkinsServicesImpl.publishEventToRelevantClients(event);
	}

	/**
	 * Sets the common hash ID and branch name on a CI event if valid.
	 * Logs appropriate warnings if the common origin revision is invalid.
	 *
	 * @param event the CI event to update
	 * @param commonOriginRevision the common origin revision data
	 * @param runDisplayName display name of the run for logging
	 */
	private void setCommonHashOnEvent(CIEvent event, CommonOriginRevision commonOriginRevision, String runDisplayName) {
		if (commonOriginRevision != null) {
			String revision = commonOriginRevision.revision;
			if (revision != null && !revision.isEmpty()) {
				event
						.setCommonHashId(revision)
						.setBranchName(commonOriginRevision.branch);
				logger.debug("Common hash set for pipeline run: {} (branch: {})",
					revision, commonOriginRevision.branch);
			} else {
				logger.warn("Common origin revision object exists but revision is null/empty for pipeline run: {}", runDisplayName);
			}
		} else {
			logger.warn("Common origin revision is null for pipeline run: {} - common hash will not be sent to Octane", runDisplayName);
		}
	}

	private void sendStageStartedEvent(StepStartNode stepStartNode) {
		logger.debug("node " + stepStartNode + " detected as Stage Start node");
		CIEvent event = prepareStageEvent(stepStartNode).setEventType(CIEventType.STARTED);
		CIJenkinsServicesImpl.publishEventToRelevantClients(event);
	}

	private void sendStageFinishedEvent(StepEndNode stepEndNode) {
		logger.debug("node " + stepEndNode + " detected as Stage End node");
		StepStartNode stepStartNode = stepEndNode.getStartNode();
		CIEvent event = prepareStageEvent(stepStartNode)
				.setEventType(CIEventType.FINISHED)
				.setDuration(TimingAction.getStartTime(stepEndNode) - TimingAction.getStartTime(stepStartNode))
				.setResult(extractFlowNodeResult(stepEndNode));

		CIJenkinsServicesImpl.publishEventToRelevantClients(event);
	}

	private CIEvent prepareStageEvent(StepStartNode stepStartNode) {
		WorkflowRun parentRun = BuildHandlerUtils.extractParentRun(stepStartNode);
		return dtoFactory.newDTO(CIEvent.class)
				.setPhaseType(PhaseType.INTERNAL)
				.setIsVirtualProject(true)
				.setProject(stepStartNode.getDisplayName())
				.setBuildCiId(BuildHandlerUtils.getBuildCiId(parentRun))
				.setNumber(String.valueOf(parentRun.getNumber()))
				.setStartTime(TimingAction.getStartTime(stepStartNode))
				.setCauses(CIEventCausesFactory.processCauses(stepStartNode));
	}

	private CIBuildResult extractFlowNodeResult(FlowNode node) {
		CIBuildResult result = node.getError() != null ? CIBuildResult.FAILURE : CIBuildResult.SUCCESS;
		if (CIBuildResult.SUCCESS.equals(result) && isChildNodeFailed(node, 0)) {
			result = CIBuildResult.FAILURE;
		}
		return result;
	}

	/**
	 * example of script : in this case second stage is failing but in octane its successful;in third case - error converted to warning
	 * 		node {
	 * 			stage('Build') {}
	 * 			stage('Results') {
	 * 				uftScenarioLoad archiveTestResultsMode: 'ALWAYS_ARCHIVE_TEST_REPORT',testPaths: '''c:\\dev\\plugins\\_uft\\UftTests\\GeneratedResult\\GUITestWithFail'''
	 * 				catchError(stageResult: 'FAILURE') {error 'error message 123'}
	 *           }
	 *           stage('Post Results') {
	 * 	           warnError('Script failed!') {//convert error to warning
	 * 	              error 'err 1'
	 * 	           }
	 * 	    }
	 * @param node
	 * @param iteration
	 * @return
	 */
    private boolean isChildNodeFailed(FlowNode node, int iteration) {
        if (iteration >= 2) { // drill down upto 2 levels
            return false;
        }
        try {
            for (FlowNode temp : node.getParents()) {
                if (temp instanceof StepEndNode) {
                    boolean isFailed = temp.getError() != null;
                    if (isFailed) {//if failed - validate that maybe error converted to warning of unstable
                        WarningAction warning = temp.getAction(WarningAction.class);
                        if (warning != null) {
                            return warning.getResult().isWorseThan(Result.UNSTABLE);
                        }
                        return true;
                    } else if (isChildNodeFailed(temp, iteration + 1)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
			logger.error("failed in isChildNodeFailed " + e.getMessage());
        }
		return false;
    }
}