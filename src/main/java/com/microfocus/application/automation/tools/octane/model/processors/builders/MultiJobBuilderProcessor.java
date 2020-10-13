/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2019 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors ("Micro Focus") are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.octane.model.processors.builders;

import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import com.microfocus.application.automation.tools.octane.model.ModelFactory;
import com.tikal.jenkins.plugins.multijob.MultiJobBuilder;
import com.tikal.jenkins.plugins.multijob.PhaseJobsConfig;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Job;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Implementation for discovery/provisioning of an internal phases/steps of the specific Job in context of MultiJob Plugin
 */
class MultiJobBuilderProcessor extends AbstractBuilderProcessor {
    private static final Logger logger = SDKBasedLoggerProvider.getLogger(MultiJobBuilderProcessor.class);

    MultiJobBuilderProcessor(Builder builder, Job job, Set<Job> processedJobs) {
        MultiJobBuilder b = (MultiJobBuilder) builder;
        super.phases = new ArrayList<>();
        List<AbstractProject> items = new ArrayList<>();
        AbstractProject tmpProject;
        for (PhaseJobsConfig config : b.getPhaseJobs()) {
            if(StringUtils.isEmpty(config.getJobName())){
                continue;
            }
            Item item = Jenkins.get().getItemByFullName(config.getJobName());
            if (item == null) {
                logger.warn(job.getFullName() + "' contains phase job  '" + config.getJobName() + "' that is not found.");
            } else if (item instanceof AbstractProject) {
                tmpProject = (AbstractProject) item;
                if (processedJobs.contains(tmpProject)) {
                    logger.warn(String.format("encountered circular reference from %s to %s", job.getFullName(), tmpProject.getFullName()));
                } else {
                    items.add(tmpProject);
                }
            } else {
                logger.warn(job.getFullName() + "' contains phase job '" + config.getJobName() + "' that is not AbstractProject");
            }
        }
        super.phases.add(ModelFactory.createStructurePhase(b.getPhaseName(), true, items, processedJobs));
    }
}
