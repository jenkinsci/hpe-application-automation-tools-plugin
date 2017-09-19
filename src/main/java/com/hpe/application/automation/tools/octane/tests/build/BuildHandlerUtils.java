// (C) Copyright 2003-2015 Hewlett-Packard Development Company, L.P.

package com.hpe.application.automation.tools.octane.tests.build;

import com.hpe.application.automation.tools.octane.model.processors.projects.JobProcessorFactory;
import com.hpe.application.automation.tools.octane.workflow.WorkflowBuildAdapter;
import com.hpe.application.automation.tools.octane.workflow.WorkflowGraphListener;
import hudson.FilePath;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.ArrayList;
import java.util.List;

public class BuildHandlerUtils {

	public static BuildDescriptor getBuildType(Run<?, ?> build) {
		for (BuildHandlerExtension ext : BuildHandlerExtension.all()) {
			if (ext.supports(build)) {
				return ext.getBuildType(build);
			}
		}
		return new BuildDescriptor(
				BuildHandlerUtils.getJobCiId(build),
				build.getParent().getName(),
				String.valueOf(build.getNumber()),
				String.valueOf(build.getNumber()),
				"");
	}

	public static String getProjectFullName(Run<?, ?> build) {
		for (BuildHandlerExtension ext : BuildHandlerExtension.all()) {
			if (ext.supports(build)) {
				return ext.getProjectFullName(build);
			}
		}
		return build.getParent().getFullName();
	}

	public static FilePath getWorkspace(Run<?,?> build){
		//this.buildId =/*build.getProject()*/((AbstractProject)build.getParent()).getBuilds().getLastBuild().getId();
			if(build.getExecutor()!=null && build.getExecutor().getCurrentWorkspace()!=null){
				return build.getExecutor().getCurrentWorkspace();
			}
			if (build instanceof AbstractBuild){
				return ((AbstractBuild) build).getWorkspace();
			}
			if(build instanceof WorkflowBuildAdapter){
				return ((WorkflowBuildAdapter)build).getWorkspace();
//				FilePath filePath = new FilePath(new File(((WorkflowRun) build).getParent().getRootDir().
//						getAbsolutePath()+File.separator +"workspace"));
//				return filePath;
			}

			return null;
	}

	public static String getBuildId(Run<?,?> build){
//		if(build instanceof AbstractBuild){
//			return ((AbstractProject)build.getParent()).getBuilds().getLastBuild().getId();
//		}else{
//			return build.getParent().getLastBuild().getId();
//		}
		return build.getParent().getLastBuild().getId();
	}

	public static List<Run> getBuildPerWorkspaces(Run build) {

		if(build instanceof WorkflowRun){
			return  WorkflowGraphListener.FlowNodeContainer.getFlowNode(build);

		}else {
			List<Run> runsList = new ArrayList<>();
			runsList.add(build);
			return runsList;
		}
	}

	public static String getJobCiId(Run r) {
		if (r.getParent() instanceof MatrixConfiguration) {
			return JobProcessorFactory.getFlowProcessor(((MatrixRun) r).getParentBuild().getParent()).getTranslateJobName();
		}
		if (r.getParent().getClass().getName().equals("org.jenkinsci.plugins.workflow.job.WorkflowJob")) {
			return JobProcessorFactory.getFlowProcessor(r.getParent()).getTranslateJobName();
		}
		return JobProcessorFactory.getFlowProcessor(((AbstractBuild) r).getProject()).getTranslateJobName();
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
