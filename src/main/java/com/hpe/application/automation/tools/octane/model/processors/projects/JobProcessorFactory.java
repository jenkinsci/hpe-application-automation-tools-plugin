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

package com.hpe.application.automation.tools.octane.model.processors.projects;

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
