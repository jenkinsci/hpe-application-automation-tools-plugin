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

import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import hudson.model.*;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

/**
 * Created with IntelliJ IDEA.
 * User: gullery
 * Date: 24/12/14
 * Time: 13:40
 * To change this template use File | Settings | File Templates.
 */

public class WorkFlowJobProcessor extends AbstractProjectProcessor<WorkflowJob> {
	private static final Logger logger = SDKBasedLoggerProvider.getLogger(WorkFlowJobProcessor.class);
	WorkFlowJobProcessor(Job job) {
		super((WorkflowJob) job);
	}

	public void scheduleBuild(Cause cause, ParametersAction parametersAction) {
		int delay = this.job.getQuietPeriod();
		CauseAction causeAction = new CauseAction(cause);
		this.job.scheduleBuild2(delay, parametersAction, causeAction);
	}

	@Override
	public String getTranslatedJobName() {
		if (JobProcessorFactory.WORKFLOW_MULTI_BRANCH_JOB_NAME.equals(job.getParent().getClass().getName())) {
			return BuildHandlerUtils.translateFolderJobName(job.getFullName());
		} else {
			return super.getTranslatedJobName();
		}
	}

	protected void stopBuild(Run run) {
		WorkflowRun aBuild = (WorkflowRun)run;
		try {
			aBuild.doStop();
			logger.info("Build is stopped : " + aBuild.getParent().getDisplayName() + aBuild.getDisplayName());
		} catch (Exception e) {
			logger.warn("Failed to stop build '" + aBuild.getDisplayName() + "' :" + e.getMessage(), e);
		}
	}
}
