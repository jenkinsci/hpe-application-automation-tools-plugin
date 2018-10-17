/*
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
 */

package com.microfocus.application.automation.tools.octane.events;

import com.hp.octane.integrations.OctaneSDK;
import com.microfocus.application.automation.tools.octane.configuration.ConfigurationService;
import com.microfocus.application.automation.tools.octane.model.processors.scm.SCMProcessor;
import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.events.CIEvent;
import com.hp.octane.integrations.dto.events.CIEventType;
import com.hp.octane.integrations.dto.scm.SCMData;
import com.microfocus.application.automation.tools.octane.model.CIEventCausesFactory;
import com.microfocus.application.automation.tools.octane.model.processors.scm.SCMProcessors;
import hudson.Extension;
import hudson.FilePath;
import hudson.matrix.MatrixConfiguration;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.SCMListener;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCM;
import hudson.scm.SCMRevisionState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.io.File;

/**
 * Run Listener that handles SCM CI events and dispatches notifications to the Octane server
 * Created by gullery on 10/07/2016.
 */

@Extension
public class SCMListenerOctaneImpl extends SCMListener {
	private static final Logger logger = LogManager.getLogger(SCMListenerOctaneImpl.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	@Override
	public void onChangeLogParsed(Run<?, ?> run, SCM scm, TaskListener listener, ChangeLogSet<?> changelog) throws Exception {
		super.onChangeLogParsed(run, scm, listener, changelog);

		if (ConfigurationService.getServerConfiguration() != null && !ConfigurationService.getServerConfiguration().isValid()) {
			return;
		}
		if (ConfigurationService.getModel() != null && ConfigurationService.getModel().isSuspend()) {
			return;
		}

		SCMProcessor scmProcessor = SCMProcessors.getAppropriate(scm.getClass().getName());
		if (scmProcessor == null) {
			logger.debug("no processors found for SCM provider of type '" + scm.getType() + "', SCM data won't be extracted");
			return;
		}

		try {
			SCMData scmData = extractSCMData(run, scm, scmProcessor);
			if (scmData != null) {
				CIEvent event = createSCMEvent(run, scmData);
				OctaneSDK.getInstance().getEventsService().publishEvent(event);
			}
		} catch (Throwable throwable) {
			logger.error("failed to build and/or dispatch SCM event for " + run, throwable);
		}
	}

	private SCMData extractSCMData(Run run, SCM scm, SCMProcessor scmProcessor) {
		SCMData result = null;
		if (run.getParent() instanceof MatrixConfiguration || run instanceof AbstractBuild) {
			AbstractBuild build = (AbstractBuild) run;
			if (!build.getChangeSet().isEmptySet()) {
				result = scmProcessor.getSCMData(build, scm);
			}
		} else if (run instanceof WorkflowRun) {
			WorkflowRun wRun = (WorkflowRun) run;
			if (!wRun.getChangeSets().isEmpty()) {
				result = scmProcessor.getSCMData(wRun, scm);
			}
		}
		return result;
	}

	private CIEvent createSCMEvent(Run<?, ?> run, SCMData scmData) {
		return dtoFactory.newDTO(CIEvent.class)
				.setEventType(CIEventType.SCM)
				.setProject(BuildHandlerUtils.getJobCiId(run))
				.setBuildCiId(BuildHandlerUtils.getBuildCiId(run))
				.setCauses(CIEventCausesFactory.processCauses(run))
				.setNumber(String.valueOf(run.getNumber()))
				.setScmData(scmData);
	}
}
