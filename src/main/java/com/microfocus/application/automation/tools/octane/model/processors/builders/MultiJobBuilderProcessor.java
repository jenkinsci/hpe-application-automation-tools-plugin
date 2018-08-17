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

package com.microfocus.application.automation.tools.octane.model.processors.builders;

import com.microfocus.application.automation.tools.octane.model.ModelFactory;
import com.tikal.jenkins.plugins.multijob.MultiJobBuilder;
import com.tikal.jenkins.plugins.multijob.PhaseJobsConfig;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Implementation for discovery/provisioning of an internal phases/steps of the specific Job in context of MultiJob Plugin
 */
public class MultiJobBuilderProcessor extends AbstractBuilderProcessor {
	private static final Logger logger = LogManager.getLogger(MultiJobBuilderProcessor.class);

	public MultiJobBuilderProcessor(Builder builder, Set<Job> processedJobs) {
		MultiJobBuilder b = (MultiJobBuilder) builder;
		super.phases = new ArrayList<>();
		List<AbstractProject> items = new ArrayList<>();
		AbstractProject tmpProject;
		for (PhaseJobsConfig config : b.getPhaseJobs()) {
			tmpProject = (AbstractProject) Jenkins.getInstance().getItem(config.getJobName());
			if (tmpProject == null) {
				logger.warn("project named '" + config.getJobName() + "' not found; considering this as corrupted configuration and skipping the project");
			} else if (processedJobs.contains(tmpProject)) {
				logger.warn("project named '" + config.getJobName() + "' is duplicated");
			} else {
				items.add(tmpProject);
			}
		}
		super.phases.add(ModelFactory.createStructurePhase(b.getPhaseName(), true, items, processedJobs));
	}
}
