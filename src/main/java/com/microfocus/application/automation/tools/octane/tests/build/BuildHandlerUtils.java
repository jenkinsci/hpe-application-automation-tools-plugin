/*
 *
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
 *
 */

package com.microfocus.application.automation.tools.octane.tests.build;

import com.hp.octane.integrations.dto.snapshots.CIBuildResult;
import com.microfocus.application.automation.tools.octane.model.processors.projects.JobProcessorFactory;
import hudson.FilePath;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.Run;
import org.apache.logging.log4j.LogManager;
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

import java.io.IOException;

/**
 * Generic utilities handling Job/Run metadata extraction/transformation/processing
 */

public class BuildHandlerUtils {
	private static final Logger logger = LogManager.getLogger(BuildHandlerUtils.class);

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
					if (n instanceof StepStartNode) {
						WorkspaceAction action = n.getAction(WorkspaceAction.class);
						if (action != null) {
							return action.getWorkspace();
						}
					}
				}
			}
		}
		return null;
	}

	public static String getBuildCiId(Run run) {
		return String.valueOf(run.getNumber());
		//  YG  temporary disabling the support for fluid build number until Octane supports it
		//return run.getNumber() + "_" + run.getStartTimeInMillis();
	}

	public static String getJobCiId(Run run) {
		if (run.getParent() instanceof MatrixConfiguration) {
			return JobProcessorFactory.getFlowProcessor(((MatrixRun) run).getParentBuild().getParent()).getTranslateJobName();
		}
		if (run.getParent().getClass().getName().equals(JobProcessorFactory.WORKFLOW_JOB_NAME)) {
			return JobProcessorFactory.getFlowProcessor(run.getParent()).getTranslateJobName();
		}
		return JobProcessorFactory.getFlowProcessor(((AbstractBuild) run).getProject()).getTranslateJobName();
	}

	public static String translateFolderJobName(String jobPlainName) {
		String newSplitterCharacters = "/job/";
		return jobPlainName.replaceAll("/", newSplitterCharacters);
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
}
