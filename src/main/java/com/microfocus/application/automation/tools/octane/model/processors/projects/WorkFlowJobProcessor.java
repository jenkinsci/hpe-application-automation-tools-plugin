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

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.configuration.OctaneConfiguration;
import com.microfocus.application.automation.tools.octane.configuration.ConfigurationService;
import com.microfocus.application.automation.tools.octane.configuration.ServerConfiguration;
import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import hudson.model.Cause;
import hudson.model.Job;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: gullery
 * Date: 24/12/14
 * Time: 13:40
 * To change this template use File | Settings | File Templates.
 */

public class WorkFlowJobProcessor extends AbstractProjectProcessor<WorkflowJob> {
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	WorkFlowJobProcessor(Job job) {
		super((WorkflowJob) job);
	}

	public List<Builder> tryGetBuilders() {
		return new ArrayList<>();
	}

	public void scheduleBuild(String parametersBody) {
		int delay = this.job.getQuietPeriod();

		if (parametersBody != null && !parametersBody.isEmpty()) {
			JSONObject bodyJSON = JSONObject.fromObject(parametersBody);

			//  delay
			if (bodyJSON.has("delay") && bodyJSON.get("delay") != null) {
				delay = bodyJSON.getInt("delay");
			}

			//  TODO: support parameters
		}
		this.job.scheduleBuild(delay, new Cause.RemoteCause(getOctaneConfiguration() == null ? "non available URL" : getOctaneConfiguration().getUrl(), "octane driven execution"));
	}

	private OctaneConfiguration getOctaneConfiguration() {
		OctaneConfiguration result = null;
		ServerConfiguration serverConfiguration = ConfigurationService.getServerConfiguration();
		if (serverConfiguration.location != null && !serverConfiguration.location.isEmpty() &&
				serverConfiguration.sharedSpace != null && !serverConfiguration.sharedSpace.isEmpty()) {
			result = dtoFactory.newDTO(OctaneConfiguration.class)
					.setUrl(serverConfiguration.location)
					.setSharedSpace(serverConfiguration.sharedSpace)
					.setApiKey(serverConfiguration.username)
					.setSecret(serverConfiguration.password.getPlainText());
		}
		return result;
	}

	@Override
	public String getTranslateJobName() {
		if (this.job.getParent() != null && this.job.getParent().getClass().getName().equals(JobProcessorFactory.WORKFLOW_MULTI_BRANCH_JOB_NAME)) {
			return BuildHandlerUtils.translateFolderJobName(job.getFullName());
		} else {
			return super.getTranslateJobName();
		}
	}
}
