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
                logger.debug(job.getFullName() + "' contains phase job  '" + config.getJobName() + "' that is not found.");
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
