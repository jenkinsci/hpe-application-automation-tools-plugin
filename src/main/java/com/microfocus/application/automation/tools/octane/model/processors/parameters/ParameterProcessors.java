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

package com.microfocus.application.automation.tools.octane.model.processors.parameters;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.parameters.CIParameter;
import com.hp.octane.integrations.dto.parameters.CIParameterType;
import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import com.microfocus.application.automation.tools.octane.model.ModelFactory;
import hudson.matrix.*;
import hudson.model.*;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by gullery on 26/03/2015.
 *
 * This class incorporates helper methods for handling Jenkins job parameters.
 */

public enum ParameterProcessors {
	EXTENDED("com.cwctravel.hudson.plugins.extended_choice_parameter.ExtendedChoiceParameterDefinition", ExtendedChoiceParameterProcessor.class),
	INHERENT("hudson.model", InherentParameterProcessor.class),
	NODE_LABEL("org.jvnet.jenkins.plugins.nodelabelparameter", NodeLabelParameterProcessor.class),
	RANDOM_STRING("hudson.plugins.random_string_parameter.RandomStringParameterDefinition", RandomStringParameterProcessor.class),
	ACTIVE_CHOICE("org.biouno.unochoice", ActiveChoiceParameterProcessor.class);

	private static final Logger logger = SDKBasedLoggerProvider.getLogger(ParameterProcessors.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();
	private String targetPluginClassName;
	private Class<? extends AbstractParametersProcessor> processorClass;

	ParameterProcessors(String targetPluginClassName, Class<? extends AbstractParametersProcessor> processorClass) {
		this.targetPluginClassName = targetPluginClassName;
		this.processorClass = processorClass;
	}

	public static List<CIParameter> getConfigs(Job job) {
		List<CIParameter> result = new ArrayList<>();
		List<ParameterDefinition> paramDefinitions;
		ParameterDefinition pd;
		String className;
		AbstractParametersProcessor processor;
		if (job.getProperty(ParametersDefinitionProperty.class) != null) {
			paramDefinitions = ((ParametersDefinitionProperty) job.getProperty(ParametersDefinitionProperty.class)).getParameterDefinitions();
			for (int i = 0; paramDefinitions != null && i < paramDefinitions.size(); i++) {
				pd = paramDefinitions.get(i);
				if (pd instanceof PasswordParameterDefinition) {
					continue;
				}
				className = pd.getClass().getName();
				processor = getAppropriate(className);
				result.add(processor.createParameterConfig(pd));
			}
		}

		if (job instanceof MatrixProject) {
			AxisList axisList = ((MatrixProject) job).getAxes();
			for (Axis axis : axisList) {
				result.add(ModelFactory.createParameterConfig(axis.getName(), CIParameterType.AXIS, new ArrayList<>(axis.getValues())));
			}
		}

		return result;
	}

	//  TODO: the below mapping between param configs and values based on param name uniqueness, beware!
	public static List<CIParameter> getInstances(Run run) {
		List<CIParameter> result = new ArrayList<>();

		try {
			CIParameter tmp;
			Job job = run.getParent();
			List<ParameterDefinition> paramDefinitions;
			String className;
			Map<String, ParameterValue> parametersValues;
			ParametersAction parametersAction = run.getAction(ParametersAction.class);
			if (parametersAction != null) {
				parametersValues = parametersAction.getAllParameters().stream().collect(
						Collectors.toMap(ParameterValue::getName, Function.identity(), (v1, v2) -> v1));
			} else {
				parametersValues = new HashMap<>();
			}
			ParameterValue pv;

			//  TODO: should be moved to the Matrix Project processor
			if (job instanceof MatrixConfiguration) {
				Combination combination = ((MatrixConfiguration) job).getCombination();
				for (Map.Entry<String, String> entry : combination.entrySet()) {
					tmp = dtoFactory.newDTO(CIParameter.class)
							.setType(CIParameterType.AXIS)
							.setName(entry.getKey())
							.setValue(entry.getValue());
					result.add(tmp);
				}
			}

			if (job.getProperty(ParametersDefinitionProperty.class) != null) {
				paramDefinitions = ((ParametersDefinitionProperty) job.getProperty(ParametersDefinitionProperty.class)).getParameterDefinitions();
				for (ParameterDefinition pd : paramDefinitions) {
					className = pd.getClass().getName();
					pv = parametersValues.remove(pd.getName());

					try {
						AbstractParametersProcessor processor = getAppropriate(className);
						result.add(processor.createParameterInstance(pd, pv));
					} catch (Exception e) {
						logger.error("failed to process instance of parameter or type '" + className + "', adding as unsupported", e);
						result.add(new UnsupportedParameterProcessor().createParameterInstance(pd, pv));
					}
				}
			}
			//go over parameters that are not defined in definitions
			for (ParameterValue notDefinedParameter : parametersValues.values()) {
				if (notDefinedParameter.getValue() != null) {
					CIParameter param = dtoFactory.newDTO(CIParameter.class)
							.setType(CIParameterType.STRING)
							.setName(notDefinedParameter.getName())
							.setValue(notDefinedParameter.getValue());
					result.add(param);
				}
			}
		} catch (Exception e) {
			logger.error("failed to process parameters of " + run, e);
		}

		return result;
	}

	public static List<CIParameter> getInstances(ParametersAction parametersAction) {
		List<CIParameter> result = new ArrayList<>();

		try {
			Map<String, ParameterValue> parametersValues = parametersAction.getAllParameters().stream().collect(
					Collectors.toMap(ParameterValue::getName, Function.identity(), (v1, v2) -> v1));

			//go over parameters that are not defined in definitions
			for (ParameterValue notDefinedParameter : parametersValues.values()) {
				if (notDefinedParameter.getValue() != null) {
					CIParameter param = dtoFactory.newDTO(CIParameter.class)
							.setType(CIParameterType.STRING)
							.setName(notDefinedParameter.getName())
							.setValue(notDefinedParameter.getValue());
					result.add(param);
				}
			}
		} catch (Exception e) {
			logger.error("failed to process parameters :" + e.getMessage(), e);
		}

		return result;
	}

	private static AbstractParametersProcessor getAppropriate(String className) {
		for (ParameterProcessors p : values()) {
			if (className.startsWith(p.targetPluginClassName)) {
				try {
					return p.processorClass.newInstance();
				} catch (InstantiationException | IllegalAccessException ie) {
					logger.error("failed to instantiate instance of parameters processor of type " + className, ie);
				}
			}
		}
		return new UnsupportedParameterProcessor();
	}
}
