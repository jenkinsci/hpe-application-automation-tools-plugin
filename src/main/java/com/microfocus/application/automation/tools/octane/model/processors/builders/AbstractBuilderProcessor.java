/*
 *  Certain versions of software accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 *  This software was acquired by Micro Focus on September 1, 2017, and is now
 *  offered by OpenText.
 *  Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical
 *  in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the
 *  property of their respective owners.
 *  OpenText is a trademark of Open Text.
 *  __________________________________________________________________
 *  MIT License
 *
 *  Copyright 2012-2025 Open Text.
 *
 *  The only warranties for products and services of Open Text and
 *  its affiliates and licensors ("Open Text") are as may be set forth
 *  in the express warranty statements accompanying such products and services.
 *  Nothing herein should be construed as constituting an additional warranty.
 *  Open Text shall not be liable for technical or editorial errors or
 *  omissions contained herein. The information contained herein is subject
 *  to change without notice.
 *
 *  Except as specifically indicated otherwise, this document contains
 *  confidential information and a valid license is required for possession,
 *  use or copying. If this work is provided to the U.S. Government,
 *  consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 *  Computer Software Documentation, and Technical Data for Commercial Items are
 *  licensed to the U.S. Government under vendor's standard commercial license.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ___________________________________________________________________
 */
package com.microfocus.application.automation.tools.octane.model.processors.builders;

import com.hp.octane.integrations.dto.pipelines.PipelinePhase;
import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import com.microfocus.application.automation.tools.octane.model.processors.projects.JobProcessorFactory;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.tasks.Builder;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Base class for discovery/provisioning of an internal phases/steps of the specific Job
 */
public abstract class AbstractBuilderProcessor {
	private static final Logger logger = SDKBasedLoggerProvider.getLogger(AbstractBuilderProcessor.class);
	protected ArrayList<PipelinePhase> phases = new ArrayList<>();

	/**
	 * Retrieves previously processed and prepared phases of the specific Builder (Jenkins' internal Job invoker)
	 *
	 * @return list of phases
	 */
	public List<PipelinePhase> getPhases() {
		return phases;
	}

	public static void processInternalBuilders(Builder builder, Job job, String phasesName, List<PipelinePhase> internalPhases, Set<Job> processedJobs) {
		processedJobs.add(job);
		AbstractBuilderProcessor builderProcessor = null;
		try {
			switch (builder.getClass().getName()) {
				case JobProcessorFactory.CONDITIONAL_BUILDER_NAME:
					builderProcessor = new ConditionalBuilderProcessor(builder, job, phasesName, internalPhases, processedJobs);
					break;
				case JobProcessorFactory.SINGLE_CONDITIONAL_BUILDER_NAME:
					builderProcessor = new SingleConditionalBuilderProcessor(builder, job, phasesName, internalPhases, processedJobs);
					break;
				case JobProcessorFactory.PARAMETRIZED_TRIGGER_BUILDER:
					builderProcessor = new ParameterizedTriggerProcessor(builder, job, phasesName, processedJobs);
					break;
				case JobProcessorFactory.MULTIJOB_BUILDER:
					builderProcessor = new MultiJobBuilderProcessor(builder, job, processedJobs);
					break;
				default:
					logger.debug("not yet supported build (internal) action: " + builder.getClass().getName());
					break;
			}
		} catch (Throwable e) {
			logger.info("Failed to load build processor for build " + builder.getClass().getName() + ",  " + e.getClass().getName() + " : " + e.getMessage());
		}

		if (builderProcessor != null) {
			internalPhases.addAll(builderProcessor.getPhases());
		}
		processedJobs.remove(job);
	}

	protected void eliminateIllegalItems(Job job, Set<Job> processedJobs, List<AbstractProject> items) {
		for (Iterator<AbstractProject> iterator = items.iterator(); iterator.hasNext(); ) {
			AbstractProject next = iterator.next();
			if (next == null) {
				iterator.remove();
				logger.warn("encountered null project reference; considering it as corrupted configuration and skipping");
			} else if (processedJobs.contains(next)) {
				iterator.remove();
				logger.warn(String.format("encountered circular reference from %s to %s", job.getFullName(), next.getFullName()));
			}
		}
	}
}
