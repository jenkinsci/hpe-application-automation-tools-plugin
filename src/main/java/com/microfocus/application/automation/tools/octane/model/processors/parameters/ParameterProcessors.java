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

package com.microfocus.application.automation.tools.octane.model.processors.parameters;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.parameters.CIParameter;
import com.hp.octane.integrations.dto.parameters.CIParameterType;
import com.microfocus.application.automation.tools.octane.model.ModelFactory;
import hudson.matrix.*;
import hudson.model.*;
import org.apache.logging.log4j.LogManager;
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
	RANDOM_STRING("hudson.plugins.random_string_parameter.RandomStringParameterDefinition", RandomStringParameterProcessor.class);

	private static final Logger logger = LogManager.getLogger(ParameterProcessors.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();
	private String targetPluginClassName;
	private Class<? extends AbstractParametersProcessor> processorClass;

	ParameterProcessors(String targetPluginClassName, Class<? extends AbstractParametersProcessor> processorClass) {
		this.targetPluginClassName = targetPluginClassName;
		this.processorClass = processorClass;
	}

	public static List<CIParameter> getConfigs(Job job) {
		ArrayList<CIParameter> result = new ArrayList<>();
		List<ParameterDefinition> paramDefinitions;
		ParameterDefinition pd;
		String className;
		AbstractParametersProcessor processor;
		if (job.getProperty(ParametersDefinitionProperty.class) != null) {
			paramDefinitions = ((ParametersDefinitionProperty) job.getProperty(ParametersDefinitionProperty.class)).getParameterDefinitions();
			for (int i = 0; paramDefinitions != null && i < paramDefinitions.size(); i++) {
				pd = paramDefinitions.get(i);
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
		CIParameter tmp;
		Job job = run.getParent();
		List<ParameterDefinition> paramDefinitions;
		String className;


		Map<String, ParameterValue> parametersValues;
		ParametersAction parametersAction = run.getAction(ParametersAction.class);
		if (parametersAction != null) {
			parametersValues = parametersAction.getAllParameters().stream().collect(
					Collectors.toMap(ParameterValue::getName, Function.identity()));
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
