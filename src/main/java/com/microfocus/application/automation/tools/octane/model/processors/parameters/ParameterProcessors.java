/*
 * © Copyright 2013 EntIT Software LLC
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright (c) 2018 Micro Focus Company, L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
import java.util.List;
import java.util.Map;

/**
 * Created by gullery on 26/03/2015.
 *
 * This class incorporates helper methods for handling Jenkins job parameters.
 */

public enum ParameterProcessors {
	//	DYNAMIC("com.seitenbau.jenkins.plugins.dynamicparameter", DynamicParameterProcessor.class),
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
				result.add(ModelFactory.createParameterConfig(axis.getName(), CIParameterType.AXIS, new ArrayList<Object>(axis.getValues())));
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

		AbstractParametersProcessor processor;
		List<ParameterValue> parametersValues;
		ParametersAction parametersAction = run.getAction(ParametersAction.class);
		if (parametersAction != null) {
			parametersValues = new ArrayList<>(parametersAction.getParameters());
		} else {
			parametersValues = new ArrayList<>();
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
				pv = null;

				try {
					for (int j = 0; j < parametersValues.size(); j++) {
						if (parametersValues.get(j) != null && parametersValues.get(j).getName().equals(pd.getName())) {
							pv = parametersValues.get(j);
							parametersValues.remove(j);
							break;
						}
					}
					processor = getAppropriate(className);
					result.add(processor.createParameterInstance(pd, pv));
				} catch (Exception e) {
					logger.error("failed to process instance of parameter or type '" + className + "', adding as unsupported", e);
					result.add(new UnsupportedParameterProcessor().createParameterInstance(pd, pv));
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
				} catch (InstantiationException ie) {
					logger.error("failed to instantiate instance of parameters processor of type " + className, ie);
				} catch (IllegalAccessException iae) {
					logger.error("failed to instantiate instance of parameters processor of type " + className, iae);
				}
			}
		}
		return new UnsupportedParameterProcessor();
	}
}
