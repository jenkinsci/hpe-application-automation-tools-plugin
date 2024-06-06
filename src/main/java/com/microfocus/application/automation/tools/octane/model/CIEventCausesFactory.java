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

package com.microfocus.application.automation.tools.octane.model;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.causes.CIEventCause;
import com.hp.octane.integrations.dto.causes.CIEventCauseType;
import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import com.microfocus.application.automation.tools.octane.model.processors.projects.JobProcessorFactory;
import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import hudson.model.Cause;
import hudson.model.InvisibleAction;
import hudson.model.Run;
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.cps.nodes.StepAtomNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepEndNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;

import java.io.IOException;
import java.util.*;

/**
 * Causes Factory is a collection of static stateless methods to extract/traverse/transform causes chains of the runs
 * User: gullery
 * Date: 20/10/14
 */

public final class CIEventCausesFactory {
	private static final Logger logger = SDKBasedLoggerProvider.getLogger(CIEventCausesFactory.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	private CIEventCausesFactory() {
	}

	public static List<CIEventCause> processCauses(Run<?, ?> run) {
		if (run == null) {
			throw new IllegalArgumentException("run MUST NOT be null");
		}

		Map<String, CIEventCause> result = new LinkedHashMap();//LinkedHashMap - save order of insertion
		List<Cause> causes = run.getCauses();
		CIEventCause tmpResultCause;
		Cause.UserIdCause tmpUserCause;
		Cause.UpstreamCause tmpUpstreamCause;

		for (Cause cause : causes) {
			tmpResultCause = dtoFactory.newDTO(CIEventCause.class);
			if (cause instanceof SCMTrigger.SCMTriggerCause) {
				tmpResultCause.setType(CIEventCauseType.SCM);
				result.put(tmpResultCause.generateKey(), tmpResultCause);
			} else if (cause instanceof TimerTrigger.TimerTriggerCause) {
				tmpResultCause.setType(CIEventCauseType.TIMER);
				result.put(tmpResultCause.generateKey(), tmpResultCause);
			} else if (cause instanceof Cause.UserIdCause) {
				tmpUserCause = (Cause.UserIdCause) cause;
				tmpResultCause.setType(CIEventCauseType.USER);
				tmpResultCause.setUser(tmpUserCause.getUserId());
				result.put(tmpResultCause.generateKey(), tmpResultCause);
			} else if (cause instanceof Cause.UpstreamCause) {
				if("RebuildCause".equals(cause.getClass().getSimpleName())){
					continue;//rebuild reason have upstream of previous build - its confusing octane.(Part of rebuild plugin)
				}
				tmpUpstreamCause = (Cause.UpstreamCause) cause;

				boolean succeededToBuildFlowCauses = false;
				Run upstreamRun = tmpUpstreamCause.getUpstreamRun();
				if (upstreamRun != null && JobProcessorFactory.WORKFLOW_RUN_NAME.equals(upstreamRun.getClass().getName())) {

					//  for the child of the Workflow - break aside and calculate the causes chain of the stages
					WorkflowRun rootWFRun = (WorkflowRun) upstreamRun;
					if (rootWFRun.getExecution() != null && rootWFRun.getExecution().getCurrentHeads() != null) {
						FlowNode enclosingNode = lookupJobEnclosingNode(run, rootWFRun);
						if (enclosingNode != null) {
							List<CIEventCause> flowCauses = processCauses(enclosingNode);
							flowCauses.forEach(fc -> result.put(fc.generateKey(), fc));
							succeededToBuildFlowCauses = true;
						}
					}
				}

				if (upstreamRun != null && !succeededToBuildFlowCauses) {
					//  proceed with regular UPSTREAM calculation logic as usual
					tmpResultCause.setType(CIEventCauseType.UPSTREAM);
					tmpResultCause.setProject(resolveJobCiId(tmpUpstreamCause.getUpstreamProject()));
					tmpResultCause.setBuildCiId(String.valueOf(tmpUpstreamCause.getUpstreamBuild()));
					tmpResultCause.setCauses(processCauses(upstreamRun));
					result.put(tmpResultCause.generateKey(), tmpResultCause);
				}
			} else { //  TODO: add support to Cause.RemoteCause execution in SDK/DTOs/Octane
				tmpResultCause.setType(CIEventCauseType.UNDEFINED);
				result.put(tmpResultCause.generateKey(), tmpResultCause);
			}
		}
		return new ArrayList<>(result.values());
	}

	public static List<CIEventCause> processCauses(FlowNode flowNode) {
		List<CIEventCause> causes = new LinkedList<>();
		processCauses(flowNode, causes, new LinkedHashSet<>(),new HashSet<>());
		return causes;
	}

	private static void processCauses(FlowNode flowNode, List<CIEventCause> causes, Set<FlowNode> startStagesToSkip,Set<FlowNode> visitedParents) {
		//  we reached the start of the flow - add WorkflowRun as an initial UPSTREAM cause
		if (flowNode.getParents().isEmpty() && !visitedParents.contains(flowNode)) {
			WorkflowRun parentRun = BuildHandlerUtils.extractParentRun(flowNode);
			CIEventCause cause = dtoFactory.newDTO(CIEventCause.class)
					.setType(CIEventCauseType.UPSTREAM)
					.setProject(BuildHandlerUtils.getJobCiId(parentRun))
					.setBuildCiId(BuildHandlerUtils.getBuildCiId(parentRun))
					.setCauses(CIEventCausesFactory.processCauses((parentRun)));
				visitedParents.add(flowNode);
				causes.add(cause);
		}

		//  if we are calculating causes for the END STEP - exclude it's own START STEP from calculation
		if (BuildHandlerUtils.isStageEndNode(flowNode)) {
			startStagesToSkip.add(((StepEndNode) flowNode).getStartNode());
		}

		for (FlowNode parent : flowNode.getParents()) {
			if (BuildHandlerUtils.isStageEndNode(parent)) {
				startStagesToSkip.add(((StepEndNode) parent).getStartNode());
				processCauses(parent, causes, startStagesToSkip,visitedParents);
			} else if (BuildHandlerUtils.isStageStartNode(parent)) {
				if (!startStagesToSkip.contains(parent) && !visitedParents.contains(parent)) {
					visitedParents.add(parent);
					CIEventCause cause = dtoFactory.newDTO(CIEventCause.class)
							.setType(CIEventCauseType.UPSTREAM)
							.setProject(parent.getDisplayName())
							.setBuildCiId(String.valueOf(BuildHandlerUtils.extractParentRun(parent).getNumber()));
					causes.add(cause);
					processCauses(parent, cause.getCauses(), startStagesToSkip, visitedParents);
				} else if (startStagesToSkip.contains(parent)){
					startStagesToSkip.remove(parent);
					processCauses(parent, causes, startStagesToSkip, visitedParents);
				}
			} else {
				processCauses(parent, causes, startStagesToSkip, visitedParents);
			}
		}
	}

	private static String resolveJobCiId(String jobPlainName) {
		if (!jobPlainName.contains(",")) {
			return BuildHandlerUtils.translateFolderJobName(jobPlainName);
		}
		return jobPlainName;
	}

	private static FlowNode lookupJobEnclosingNode(Run targetRun, WorkflowRun parentRun) {
		if (parentRun.getExecution() == null) {
			return null;
		}

		FlowNode result = null;

		OctaneParentNodeAction octaneParentNodeAction = targetRun.getAction(OctaneParentNodeAction.class);
		if (octaneParentNodeAction != null) {
			//  finished event case - we do expect an action OctaneParentNodeAction to be present with the relevant info
			try {
				result = parentRun.getExecution().getNode(octaneParentNodeAction.parentFlowNodeId);
			} catch (IOException ioe) {
				logger.error("failed to extract parent flow node for " + targetRun, ioe);
			}
		} else {
			//  started event case - we expect a strict bond here since the parent FlowNode MUST be among the current heads
			//  the only case for potential break here is if the same JOB will be running concurrently by 2 distinct FlowNodes
			List<FlowNode> potentialAncestors = parentRun.getExecution().getCurrentHeads();
			if (potentialAncestors != null) {
				for (FlowNode head : potentialAncestors) {
					if (head instanceof StepAtomNode && head.getAction(LabelAction.class) != null) {
						StepDescriptor descriptor = ((StepAtomNode) head).getDescriptor();
						LabelAction labelAction = head.getAction(LabelAction.class);
						String label = labelAction != null ? labelAction.getDisplayName() : null;
						if (descriptor != null && descriptor.getId().endsWith("BuildTriggerStep") &&
								label != null && label.endsWith(targetRun.getParent().getFullDisplayName())) {
							result = head;
							targetRun.addAction(new OctaneParentNodeAction(result.getId()));
							break;
						}
					}
				}
			}
		}

		return result;
	}

	private final static class OctaneParentNodeAction extends InvisibleAction {
		private final String parentFlowNodeId;

		private OctaneParentNodeAction(String parentFlowNodeId) {
			this.parentFlowNodeId = parentFlowNodeId;
		}
	}
}
