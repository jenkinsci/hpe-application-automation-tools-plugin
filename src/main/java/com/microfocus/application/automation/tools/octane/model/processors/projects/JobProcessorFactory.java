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

import hudson.model.Item;
import hudson.model.Job;

/**
 * Created by gadiel on 30/11/2016.
 * <p>
 * Job processors factory - should be used as a 'static' class, no instantiation, only static method/s
 */

public class JobProcessorFactory {
	//  native
	public static final String FREE_STYLE_JOB_NAME = "hudson.model.FreeStyleProject";
	public static final String SIMPLE_BUILD_TRIGGER = "hudson.tasks.BuildTrigger";
	public static final String PARAMETRIZED_BUILD_TRIGGER = "hudson.plugins.parameterizedtrigger.BuildTrigger";
	public static final String PARAMETRIZED_TRIGGER_BUILDER = "hudson.plugins.parameterizedtrigger.TriggerBuilder";

	//  workflow
	public static final String WORKFLOW_JOB_NAME = "org.jenkinsci.plugins.workflow.job.WorkflowJob";
	public static final String WORKFLOW_RUN_NAME = "org.jenkinsci.plugins.workflow.job.WorkflowRun";
	public static final String WORKFLOW_MULTI_BRANCH_JOB_NAME = "org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject";

	//  multijob
	public static final String MULTIJOB_JOB_NAME = "com.tikal.jenkins.plugins.multijob.MultiJobProject";
	public static final String MULTIJOB_BUILDER = "com.tikal.jenkins.plugins.multijob.MultiJobBuilder";

	//  matrix
	public static final String MATRIX_JOB_NAME = "hudson.matrix.MatrixProject";
	public static final String MATRIX_CONFIGURATION_NAME = "hudson.matrix.MatrixConfiguration";

	//  conditional
	public static final String CONDITIONAL_BUILDER_NAME = "org.jenkinsci.plugins.conditionalbuildstep.ConditionalBuilder";
	public static final String SINGLE_CONDITIONAL_BUILDER_NAME = "org.jenkinsci.plugins.conditionalbuildstep.singlestep.SingleConditionalBuilder";

	//  maven
	public static final String MAVEN_JOB_NAME = "hudson.maven.MavenModuleSet";
	public static final String MAVEN_MODULE_NAME = "hudson.maven.MavenModule";

	//  folders
	public static final String FOLDER_JOB_NAME = "com.cloudbees.hudson.plugins.folder.Folder";
	public static final String GITHUB_ORGANIZATION_FOLDER = "jenkins.branch.OrganizationFolder";

	private JobProcessorFactory() {
	}

	public static <T extends Job> AbstractProjectProcessor<T> getFlowProcessor(T job) {
		AbstractProjectProcessor flowProcessor;

		switch (job.getClass().getName()) {
			case FREE_STYLE_JOB_NAME:
				flowProcessor = new FreeStyleProjectProcessor(job);
				break;
			case MATRIX_JOB_NAME:
				flowProcessor = new MatrixProjectProcessor(job);
				break;
			case MATRIX_CONFIGURATION_NAME:
				flowProcessor = new MatrixConfigurationProcessor(job);
				break;
			case MAVEN_JOB_NAME:
				flowProcessor = new MavenProjectProcessor(job);
				break;
			case MULTIJOB_JOB_NAME:
				flowProcessor = new MultiJobProjectProcessor(job);
				break;
			case WORKFLOW_JOB_NAME:
				flowProcessor = new WorkFlowJobProcessor(job);
				break;
			default:
				flowProcessor = new UnsupportedProjectProcessor(job);
				break;
		}

		return flowProcessor;
	}

	public static  boolean isFolder(Item item) {
		return JobProcessorFactory.FOLDER_JOB_NAME.equals(item.getClass().getName());
	}

	public static boolean isMultibranch(Item item) {
		return JobProcessorFactory.WORKFLOW_MULTI_BRANCH_JOB_NAME.equals(item.getClass().getName());
	}

	public static boolean isJob(Item item) {
		return item instanceof Job;
	}

	public static boolean isMultibranchChild(Item item) {
		return JobProcessorFactory.WORKFLOW_JOB_NAME.equals(item.getClass().getName()) &&
				JobProcessorFactory.WORKFLOW_MULTI_BRANCH_JOB_NAME.equals(item.getParent().getClass().getName());
	}
}
