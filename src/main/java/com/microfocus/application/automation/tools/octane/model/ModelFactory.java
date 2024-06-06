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

package com.microfocus.application.automation.tools.octane.model;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.parameters.CIParameter;
import com.hp.octane.integrations.dto.parameters.CIParameterType;
import com.hp.octane.integrations.dto.pipelines.PipelineNode;
import com.hp.octane.integrations.dto.pipelines.PipelinePhase;
import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import com.microfocus.application.automation.tools.octane.model.processors.parameters.ParameterProcessors;
import com.microfocus.application.automation.tools.octane.model.processors.projects.AbstractProjectProcessor;
import com.microfocus.application.automation.tools.octane.model.processors.projects.JobProcessorFactory;
import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import hudson.model.*;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Created by lazara on 26/01/2016.
 */
public class ModelFactory {
	private static final Logger logger = SDKBasedLoggerProvider.getLogger(ModelFactory.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	public static PipelineNode createStructureItem(Job job) {
		return createStructureItem(job, new HashSet<>());
	}

	public static PipelinePhase createStructurePhase(String name, boolean blocking, List<AbstractProject> items, Set<Job> processedJobs) {
		PipelinePhase pipelinePhase = dtoFactory.newDTO(PipelinePhase.class);
		pipelinePhase.setName(name);
		pipelinePhase.setBlocking(blocking);

		PipelineNode[] tmp = new PipelineNode[items.size()];
		for (int i = 0; i < tmp.length; i++) {
			if (items.get(i) != null) {
				tmp[i] = ModelFactory.createStructureItem(items.get(i), processedJobs);

			} else {
				logger.warn("One of referenced jobs is null, your Jenkins config probably broken, skipping this job...");
			}
		}

		pipelinePhase.setJobs(Arrays.asList(tmp));

		return pipelinePhase;
	}

	private static PipelineNode createStructureItem(Job job, Set<Job> processedJobs) {
		AbstractProjectProcessor projectProcessor = JobProcessorFactory.getFlowProcessor(job);
		projectProcessor.buildStructure(processedJobs);
		PipelineNode pipelineNode = dtoFactory.newDTO(PipelineNode.class);
		pipelineNode.setJobCiId(projectProcessor.getTranslatedJobName());
		pipelineNode.setName(BuildHandlerUtils.translateFullDisplayName(job.getFullDisplayName()));
		pipelineNode.setParameters(ParameterProcessors.getConfigs(job));
		pipelineNode.setPhasesInternal(projectProcessor.getInternals());
		pipelineNode.setPhasesPostBuild(projectProcessor.getPostBuilds());

		return pipelineNode;
	}

	public static CIParameter createParameterConfig(ParameterDefinition pd) {
		return createParameterConfig(pd, CIParameterType.UNKNOWN, null, null);
	}

	public static CIParameter createParameterConfig(ParameterDefinition pd, CIParameterType type) {
		return createParameterConfig(
				pd,
				type,
				pd.getDefaultParameterValue() == null ? null : pd.getDefaultParameterValue().getValue(),
				null);
	}

	public static CIParameter createParameterConfig(ParameterDefinition pd, CIParameterType type, Object defaultValue) {
		return createParameterConfig(pd, type, defaultValue, null);
	}

	public static CIParameter createParameterConfig(String name, CIParameterType type, List<Object> choices) {
		CIParameter ciParameter = dtoFactory.newDTO(CIParameter.class);
		ciParameter.setName(name);
		ciParameter.setType(type);
		ciParameter.setDescription("");
		ciParameter.setChoices(choices.toArray());
		return ciParameter;
	}

	public static CIParameter createParameterConfig(ParameterDefinition pd, CIParameterType type, Object defaultValue, List<Object> choices) {
		CIParameter ciParameter = dtoFactory.newDTO(CIParameter.class);
		ciParameter.setName(pd.getName());
		ciParameter.setType(type);
		ciParameter.setDescription(pd.getDescription());
		ParameterValue tmp;
		if (type != CIParameterType.UNKNOWN && type != CIParameterType.PASSWORD) {
			if (defaultValue != null) {
				ciParameter.setDefaultValue(defaultValue);
			} else {
				try { //computing getDefaultParameterValue may throw exception(for example ChoiceParameterDefinition.getDefaultParameterValue may throw exception of ArrayIndexOutOfBoundsException)
					tmp = pd.getDefaultParameterValue();
				} catch (Throwable e) {
					tmp = null;
				}
				ciParameter.setDefaultValue(tmp == null ? "" : tmp.getValue());
			}
			if (choices != null) {
				ciParameter.setChoices(choices.toArray());
			}
		}

		return ciParameter;
	}

	public static CIParameter createParameterInstance(CIParameter pc, Object rawValue) {
		String value = rawValue == null ? null : rawValue.toString();
		return dtoFactory.newDTO(CIParameter.class)
				.setName(pc.getName())
				.setType(pc.getType())
				.setDescription(pc.getDescription())
				.setChoices(pc.getChoices())
				.setDescription(pc.getDescription())
				.setDefaultValue(pc.getDefaultValue())
				.setValue(value);
	}

	public static String generateSubBuildName(List<CIParameter> parameters) {
		List<CIParameter> sortedList = new ArrayList<>();
		for (CIParameter p : parameters) {
			if (p.getType() == CIParameterType.AXIS) {
				sortedList.add(p);
			}
		}

		sortedList.sort(Comparator.comparing(CIParameter::getName));

		StringBuilder subBuildName = new StringBuilder();
		if (sortedList.size() > 0) {
			int i = 0;
			for (; i < sortedList.size() - 1; i++) {
				subBuildName
						.append(sortedList.get(i).getName())
						.append("=")
						.append(sortedList.get(i).getValue().toString())
						.append(",");
			}
			subBuildName
					.append(sortedList.get(i).getName())
					.append("=")
					.append(sortedList.get(i).getValue().toString());
		}
		return subBuildName.toString();
	}
}
