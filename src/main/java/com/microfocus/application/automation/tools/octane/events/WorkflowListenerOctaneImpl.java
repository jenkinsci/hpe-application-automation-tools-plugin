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
import com.microfocus.application.automation.tools.octane.tests.TestListener;
import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import hudson.Extension;
import hudson.model.Result;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.actions.WarningAction;
import org.jenkinsci.plugins.workflow.cps.nodes.StepEndNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.flow.GraphListener;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
		CIJenkinsServicesImpl.publishEventToRelevantClients(event);
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