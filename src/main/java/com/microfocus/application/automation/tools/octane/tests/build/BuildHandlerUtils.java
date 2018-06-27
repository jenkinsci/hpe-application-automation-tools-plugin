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

package com.microfocus.application.automation.tools.octane.tests.build;

import com.microfocus.application.automation.tools.octane.model.processors.projects.JobProcessorFactory;
import com.microfocus.application.automation.tools.octane.workflow.WorkflowBuildAdapter;
import com.microfocus.application.automation.tools.octane.workflow.WorkflowGraphListener;
import hudson.FilePath;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic utilities handling Job/Run metadata extraction/transformation/processing
 */

public class BuildHandlerUtils {

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
		if (run instanceof WorkflowBuildAdapter) {
			return ((WorkflowBuildAdapter) run).getWorkspace();
		}
		return null;
	}

	public static List<Run> getBuildPerWorkspaces(Run run) {
		if (run instanceof WorkflowRun) {
			return WorkflowGraphListener.FlowNodeContainer.getFlowNode(run);
		} else {
			List<Run> runsList = new ArrayList<>();
			runsList.add(run);
			return runsList;
		}
	}

	public static String getBuildCiId(Run run) {
		return String.valueOf(run.getNumber());
		//  YG  temportarty disabling the support for fluid build number until Octane supports it
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

	/**
	 * Retrieve Job's CI ID
	 *
	 * @return Job's CI ID
	 */
	public static String translateFolderJobName(String jobPlainName) {
		String newSplitterCharacters = "/job/";
		return jobPlainName.replaceAll("/", newSplitterCharacters);
	}
}
