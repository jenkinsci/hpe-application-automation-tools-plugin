/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.hpe.application.automation.tools.octane.model.processors.projects;

import com.hpe.application.automation.tools.octane.model.processors.builders.AbstractBuilderProcessor;
import com.hpe.application.automation.tools.octane.model.processors.builders.BuildTriggerProcessor;
import com.hpe.application.automation.tools.octane.model.processors.builders.MultiJobBuilderProcessor;
import com.hpe.application.automation.tools.octane.model.processors.builders.ParameterizedTriggerProcessor;
import com.hp.octane.integrations.dto.pipelines.PipelinePhase;
import com.hpe.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.tasks.BuildStep;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.plugins.conditionalbuildstep.ConditionalBuilder;
import org.jenkinsci.plugins.conditionalbuildstep.singlestep.SingleConditionalBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: gullery
 * Date: 09/01/15
 * Time: 00:59
 * To change this template use File | Settings | File Templates.
 */

@SuppressWarnings({"squid:S1132", "squid:S1872"})
public abstract class AbstractProjectProcessor<T extends Job> {
    private static final Logger logger = LogManager.getLogger(AbstractProjectProcessor.class);
    private final List<PipelinePhase> internals = new ArrayList<>();
    private final List<PipelinePhase> postBuilds = new ArrayList<>();

    T job;

    AbstractProjectProcessor(T job) {
        this.job = job;
    }

    /**
     * Attempt to retrieve an [internal] build phases of the Job
     *
     * @return
     */
    public abstract List<Builder> tryGetBuilders();

    /**
     * Enqueue Job's run with the specified parameters
     *
     * @param parametersBody parameters for the Job execution in a RAW JSON format
     */
    public abstract void scheduleBuild(String parametersBody);

    /**
     * Retrieve Job's CI ID
     * return the job name, in case of a folder job, this method returns the refactored
     * name that matches the required pattern.
     * @return Job's CI ID
     */
    public String getTranslateJobName() {
        if (job.getParent().getClass().getName().equals("com.cloudbees.hudson.plugins.folder.Folder")) {
            String jobPlainName = job.getFullName();    // e.g: myFolder/myJob
            return BuildHandlerUtils.translateFolderJobName(jobPlainName);
        } else {
            return job.getName();
        }
    }

    /**
     * Discover an internal phases of the Job
     *
     * @return list of phases
     */
    public List<PipelinePhase> getInternals() {
        return internals;
    }

    /**
     * Discover a post build phases of the Job
     *
     * @return list of phases
     */
    public List<PipelinePhase> getPostBuilds() {
        return postBuilds;
    }

    /**
     * Internal API
     * Processes and prepares Job's children for future use - internal flow
     *
     * @param builders      Job's builders
     * @param job           Job to process
     * @param processedJobs previously processed Jobs in this Job's hierarchical chain in order to break the recursive flows
     */
    void processBuilders(List<Builder> builders, Job job, Set<Job> processedJobs) {
        this.processBuilders(builders, job, "", processedJobs);
    }

    /**
     * Internal API
     * Processes and prepares Job's children for future use - internal flow
     *
     * @param builders      Job's builders
     * @param job           Job to process
     * @param phasesName    Targeted phase name in case of available one
     * @param processedJobs previously processed Jobs in this Job's hierarchical chain in order to break the recursive flows
     */
    void processBuilders(List<Builder> builders, Job job, String phasesName, Set<Job> processedJobs) {
        for (Builder builder : builders) {
            builderClassValidator(builder, job, phasesName, processedJobs);
        }
    }

    /**
     * Internal API
     * Processes and prepares Job's children for future use - post build flow
     *
     * @param job           Job to process
     * @param processedJobs previously processed Jobs in this Job's hierarchical chain in order to break the recursive flows
     */
    @SuppressWarnings("unchecked")
    void processPublishers(Job job, Set<Job> processedJobs) {
        if (job instanceof AbstractProject) {
            AbstractProject project = (AbstractProject) job;
            processedJobs.add(job);
            AbstractBuilderProcessor builderProcessor;
            List<Publisher> publishers = project.getPublishersList();
            for (Publisher publisher : publishers) {
                builderProcessor = null;
                if (publisher.getClass().getName().equals("hudson.tasks.BuildTrigger")) {
                    builderProcessor = new BuildTriggerProcessor(publisher, project, processedJobs);
                } else if (publisher.getClass().getName().equals("hudson.plugins.parameterizedtrigger.BuildTrigger")) {
                    builderProcessor = new ParameterizedTriggerProcessor(publisher, project, "", processedJobs);
                }
                if (builderProcessor != null) {
                    postBuilds.addAll(builderProcessor.getPhases());
                } else {
                    logger.debug("not yet supported publisher (post build) action: " + publisher.getClass().getName());
                }
            }
        }
    }

    private void builderClassValidator(Builder builder, Job job, String phasesName, Set<Job> processedJobs) {
        processedJobs.add(job);
        AbstractBuilderProcessor builderProcessor = null;
        if (builder.getClass().getName().equals("org.jenkinsci.plugins.conditionalbuildstep.ConditionalBuilder")) {
            ConditionalBuilder conditionalBuilder = (ConditionalBuilder) builder;
            for (BuildStep currentBuildStep : conditionalBuilder.getConditionalbuilders()) {
                builderClassValidator((Builder) currentBuildStep, job, phasesName, processedJobs);
            }
        } else if (builder.getClass().getName().equals("org.jenkinsci.plugins.conditionalbuildstep.singlestep.SingleConditionalBuilder")) {
            SingleConditionalBuilder singleConditionalBuilder = (SingleConditionalBuilder) builder;
            builderClassValidator((Builder) singleConditionalBuilder.getBuildStep(), job, phasesName, processedJobs);
        } else if (builder.getClass().getName().equals("hudson.plugins.parameterizedtrigger.TriggerBuilder")) {
            builderProcessor = new ParameterizedTriggerProcessor(builder, job, phasesName, processedJobs);
        } else if (builder.getClass().getName().equals("com.tikal.jenkins.plugins.multijob.MultiJobBuilder")) {
            builderProcessor = new MultiJobBuilderProcessor(builder, processedJobs);
        }

        if (builderProcessor != null) {
            internals.addAll(builderProcessor.getPhases());
        } else {
            logger.debug("not yet supported build (internal) action: " + builder.getClass().getName());
        }
    }
}
