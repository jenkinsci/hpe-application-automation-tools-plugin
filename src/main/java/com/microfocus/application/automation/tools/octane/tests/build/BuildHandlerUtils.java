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

package com.microfocus.application.automation.tools.octane.tests.build;

import com.hp.octane.integrations.dto.causes.CIEventCause;
import com.hp.octane.integrations.dto.causes.CIEventCauseType;
import com.hp.octane.integrations.dto.snapshots.CIBuildResult;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import com.hp.octane.integrations.utils.SdkConstants;
import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import com.microfocus.application.automation.tools.octane.model.CIEventCausesFactory;
import com.microfocus.application.automation.tools.octane.model.processors.projects.JobProcessorFactory;
import hudson.FilePath;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.Run;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.actions.ThreadNameAction;
import org.jenkinsci.plugins.workflow.actions.WorkspaceAction;
import org.jenkinsci.plugins.workflow.cps.nodes.StepEndNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.graph.FlowEndNode;
import org.jenkinsci.plugins.workflow.graph.FlowGraphWalker;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graph.FlowStartNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Generic utilities handling Job/Run metadata extraction/transformation/processing
 */

public class BuildHandlerUtils {
	private static final Logger logger = SDKBasedLoggerProvider.getLogger(BuildHandlerUtils.class);
	public static final String JOB_LEVEL_SEPARATOR = "/job/";

	public static BuildDescriptor getBuildType(Run<?, ?> run) {
		for (BuildHandlerExtension ext : BuildHandlerExtension.all()) {
			if (ext.supports(run)) {
				return ext.getBuildType(run);
			}
		}
		return new BuildDescriptor(
				BuildHandlerUtils.getJobCiId(run),
				run.getParent().getName(),
				BuildHandlerUtils.getBuildCiId(run),
				String.valueOf(run.getNumber()),
				"");
	}

	@Deprecated
	public static String getProjectFullName(Run<?, ?> run) {
		for (BuildHandlerExtension ext : BuildHandlerExtension.all()) {
			if (ext.supports(run)) {
				return ext.getProjectFullName(run);
			}
		}
		return run.getParent().getFullName();
	}

	public static FilePath getWorkspace(Run<?, ?> run) {
		if (run.getExecutor() != null && run.getExecutor().getCurrentWorkspace() != null) {
			return run.getExecutor().getCurrentWorkspace();
		}
		if (run instanceof AbstractBuild) {
			return ((AbstractBuild) run).getWorkspace();
		}
		if (run instanceof WorkflowRun) {
			FlowExecution fe = ((WorkflowRun) run).getExecution();
			if (fe != null) {
				FlowGraphWalker w = new FlowGraphWalker(fe);
				for (FlowNode n : w) {
					WorkspaceAction action = n.getAction(WorkspaceAction.class);
					if (action != null) {
						FilePath workspace = action.getWorkspace();
						if (workspace == null) {
							workspace = handleWorkspaceActionWithoutWorkspace(action);
						}
						return workspace;
					}
				}
			}
		}

		logger.error("BuildHandlerUtils.getWorkspace - run is not handled. Run type : " + run.getClass());
		return null;
	}
	public static Set<FilePath> getWorkspaces(Run<?, ?> run) {
		if (run.getExecutor() != null && run.getExecutor().getCurrentWorkspace() != null) {
			return Collections.singleton(run.getExecutor().getCurrentWorkspace());
		}
		if (run instanceof AbstractBuild) {
			return Collections.singleton(((AbstractBuild) run).getWorkspace());
		}
		Set<FilePath> workspaces = new HashSet<>();
		if (run instanceof WorkflowRun) {
			FlowExecution fe = ((WorkflowRun) run).getExecution();
			if (fe != null) {
				FlowGraphWalker w = new FlowGraphWalker(fe);
				for (FlowNode n : w) {
					WorkspaceAction action = n.getAction(WorkspaceAction.class);
					if (action != null) {
						FilePath workspace = action.getWorkspace();
						if (workspace == null) {
							workspace = handleWorkspaceActionWithoutWorkspace(action);
						}
						workspaces.add(workspace);
					}
				}
			}
		}

		if(workspaces.isEmpty()) {
			logger.error("BuildHandlerUtils.getWorkspaces - run is not handled. Run type : " + run.getClass());
		}

		return workspaces;
	}

	private static FilePath handleWorkspaceActionWithoutWorkspace(WorkspaceAction action) {
		logger.error("Found WorkspaceAction without workspace");
		logger.warn("Node getPath = " + action.getPath());
		logger.warn("Node getNode = " + action.getNode());
		FilePath workspace = null;

		if (StringUtils.isNotEmpty(action.getPath())) {
			logger.warn("Node getPath is not empty, return getPath as workspace");
			workspace = new FilePath(new File(action.getPath()));
		} else {
			logger.warn("Node getPath is empty, return workspace = null");
		}
		return workspace;
	}

	public static String getBuildCiId(Run run) {
		return String.valueOf(run.getNumber());
		//  YG  temporary disabling the support for fluid build number until Octane supports it
		//return run.getNumber() + "_" + run.getStartTimeInMillis();
	}

	public static String getJobCiId(Run run) {
		if (run.getParent() instanceof MatrixConfiguration) {
			return JobProcessorFactory.getFlowProcessor(((MatrixRun) run).getProject()).getTranslatedJobName();
		}
		if (run.getParent().getClass().getName().equals(JobProcessorFactory.WORKFLOW_JOB_NAME)) {
			return JobProcessorFactory.getFlowProcessor(run.getParent()).getTranslatedJobName();
		}
		return JobProcessorFactory.getFlowProcessor(((AbstractBuild) run).getProject()).getTranslatedJobName();
	}

	public static String translateFolderJobName(String jobPlainName) {
		return jobPlainName.replaceAll("/", JOB_LEVEL_SEPARATOR);
	}

	public static String revertTranslateFolderJobName(String translatedJobName) {
		return translatedJobName.replaceAll(JOB_LEVEL_SEPARATOR, "/");
	}

	public static String translateFullDisplayName(String fullDisplayName) {
		return fullDisplayName.replaceAll(" » ", "/");
	}

	public static CIBuildResult translateRunResult(Run run) {
		CIBuildResult result;
		if (run.getResult() == Result.SUCCESS) {
			result = CIBuildResult.SUCCESS;
		} else if (run.getResult() == Result.ABORTED) {
			result = CIBuildResult.ABORTED;
		} else if (run.getResult() == Result.FAILURE) {
			result = CIBuildResult.FAILURE;
		} else if (run.getResult() == Result.UNSTABLE) {
			result = CIBuildResult.UNSTABLE;
		} else {
			result = CIBuildResult.UNAVAILABLE;
		}
		return result;
	}

	public static boolean isWorkflowStartNode(FlowNode node) {
		return node.getParents().isEmpty() ||
				node.getParents().stream().anyMatch(fn -> fn instanceof FlowStartNode);
	}

	public static boolean isWorkflowEndNode(FlowNode node) {
		return node instanceof FlowEndNode;
	}

	public static boolean isStageStartNode(FlowNode node) {
		return node instanceof StepStartNode && node.getAction(LabelAction.class) != null && node.getAction(ThreadNameAction.class) == null;
	}

	public static boolean isStageEndNode(FlowNode node) {
		return node instanceof StepEndNode && isStageStartNode(((StepEndNode) node).getStartNode());
	}

	public static WorkflowRun extractParentRun(FlowNode flowNode) {
		try {
			return (WorkflowRun) flowNode.getExecution().getOwner().getExecutable();
		} catch (IOException ioe) {
			logger.error("failed to extract parent workflow run from " + flowNode, ioe);
			throw new IllegalStateException("failed to extract parent workflow run from " + flowNode);
		}
	}

	public static String getRootJobCiIds(Run<?, ?> run) {
		Set<String> parents = new HashSet<>();
		CIPluginSDKUtils.getRootJobCiIds(BuildHandlerUtils.getJobCiId(run), CIEventCausesFactory.processCauses(run), parents);
		return String.join(SdkConstants.General.JOB_PARENT_DELIMITER, parents);
	}
}
