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

package com.microfocus.application.automation.tools.octane.model.processors.projects;

import hudson.model.Job;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by gadiel on 30/11/2016.
 *
 * Job processors factory - should be used as a 'static' class, no instantiation, only static method/s
 */

public class JobProcessorFactory {

	public static String WORKFLOW_JOB_NAME = "org.jenkinsci.plugins.workflow.job.WorkflowJob";

	public static String FOLDER_JOB_NAME = "com.cloudbees.hudson.plugins.folder.Folder";

	public static String WORKFLOW_MULTI_BRANCH_JOB_NAME = "org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject";

	private static String MULTIJOB_JOB_NAME = "com.tikal.jenkins.plugins.multijob.MultiJobProject";

	private static String MAVEN_JOB_NAME = "hudson.maven.MavenModuleSet";

	private static String MATRIX_JOB_NAME = "hudson.matrix.MatrixProject";

	private static String FREE_STYLE_JOB_NAME = "hudson.model.FreeStyleProject";

	public static String GITHUB_ORGANIZATION_FOLDER = "jenkins.branch.OrganizationFolder";


	private JobProcessorFactory() {
	}

	public static <T extends Job> AbstractProjectProcessor<T> getFlowProcessor(T job){
		Set<Job> processedJobs = new HashSet<>();
		return getFlowProcessor(job, processedJobs);
	}

	public static <T extends Job> AbstractProjectProcessor<T> getFlowProcessor(T job, Set<Job> processedJobs) {
		AbstractProjectProcessor flowProcessor;
		processedJobs.add(job);

		if (job.getClass().getName().equals(FREE_STYLE_JOB_NAME)) {
			flowProcessor = new FreeStyleProjectProcessor(job, processedJobs);
		} else if (job.getClass().getName().equals(MATRIX_JOB_NAME)) {
			flowProcessor = new MatrixProjectProcessor(job, processedJobs);
		} else if (job.getClass().getName().equals(MAVEN_JOB_NAME)) {
			flowProcessor = new MavenProjectProcessor(job, processedJobs);
		} else if (job.getClass().getName().equals(MULTIJOB_JOB_NAME)) {
			flowProcessor = new MultiJobProjectProcessor(job, processedJobs);
		} else if (job.getClass().getName().equals(WORKFLOW_JOB_NAME)) {
			flowProcessor = new WorkFlowJobProcessor(job);
		} else {
			flowProcessor = new UnsupportedProjectProcessor(job);
		}
		processedJobs.remove(job);
		return flowProcessor;
	}
}
