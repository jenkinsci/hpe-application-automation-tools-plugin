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

import com.cwctravel.hudson.plugins.extended_choice_parameter.ExtendedChoiceParameterDefinition;
import com.hp.octane.integrations.dto.parameters.CIParameter;
import com.hp.octane.integrations.dto.parameters.CIParameterType;
import com.microfocus.application.automation.tools.octane.model.ModelFactory;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by gullery on 19/02/2015.
 */

public class ExtendedChoiceParameterProcessor extends AbstractParametersProcessor {
	ExtendedChoiceParameterProcessor() {
	}

	@Override
	public CIParameter createParameterConfig(ParameterDefinition pd) {
		ExtendedChoiceParameterDefinition extChoice = (ExtendedChoiceParameterDefinition) pd;
		Map<String, String> choicesMap;
		List<Object> choices = new ArrayList<Object>();
		try {
			choicesMap = extChoice.getChoicesByDropdownId();
		} catch (Exception e) {
			choicesMap = null;
		}
		if (choicesMap != null) {
			choices = new ArrayList<Object>(choicesMap.values());
		}
		return ModelFactory.createParameterConfig(pd, CIParameterType.STRING, null, choices);
	}

	@Override
	public CIParameter createParameterInstance(ParameterDefinition pd, ParameterValue pv) {
        Object value = pv == null ? null : pv.getValue();
		return ModelFactory.createParameterInstance(createParameterConfig(pd), value);
	}
}
