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
import com.microfocus.application.automation.tools.octane.model.ModelFactory;
import hudson.model.*;

import java.util.ArrayList;

/**
 * Created by gullery on 19/02/2015.
 */

public class InherentParameterProcessor extends AbstractParametersProcessor {
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	InherentParameterProcessor() {
	}

	@Override
	public CIParameter createParameterConfig(ParameterDefinition pd) {
		CIParameter result;
		if (pd instanceof BooleanParameterDefinition) {
			result = ModelFactory.createParameterConfig(pd, CIParameterType.BOOLEAN);
		} else if (pd instanceof TextParameterDefinition) {
			result = ModelFactory.createParameterConfig(pd, CIParameterType.STRING);
		} else if (pd instanceof StringParameterDefinition) {
			result = ModelFactory.createParameterConfig(pd, CIParameterType.STRING);
		} else if (pd instanceof ChoiceParameterDefinition) {
			ChoiceParameterDefinition choicePd = (ChoiceParameterDefinition) pd;
			result = ModelFactory.createParameterConfig(pd, CIParameterType.STRING, null, new ArrayList<>(choicePd.getChoices()));
		} else if (pd instanceof PasswordParameterDefinition) {
			PasswordParameterDefinition passPd = (PasswordParameterDefinition) pd;
			result = ModelFactory.createParameterConfig(pd, CIParameterType.PASSWORD, passPd.getDefaultValue());
		} else if (pd instanceof FileParameterDefinition) {
			result = ModelFactory.createParameterConfig(pd, CIParameterType.FILE);
		} else {
			result = new UnsupportedParameterProcessor().createParameterConfig(pd);
		}
		return result;
	}

	@Override
	public CIParameter createParameterInstance(ParameterDefinition pd, ParameterValue pv) {
		CIParameter result;
		CIParameter pc = createParameterConfig(pd);
		Object value = pv == null ? null : pv.getValue();
		if (pd instanceof BooleanParameterDefinition) {
			result = ModelFactory.createParameterInstance(pc, value);
		} else if (pd instanceof TextParameterDefinition) {
			result = ModelFactory.createParameterInstance(pc, value);
		} else if (pd instanceof StringParameterDefinition) {
			result = ModelFactory.createParameterInstance(pc, value);
		} else if (pd instanceof ChoiceParameterDefinition) {
			result = ModelFactory.createParameterInstance(pc, value);
		} else if (pd instanceof PasswordParameterDefinition) {
			result = dtoFactory.newDTO(CIParameter.class)
					.setType(pc.getType())
					.setName(pc.getName())
					.setDescription(pc.getDescription())
					.setChoices(pc.getChoices())
					.setDefaultValue(pc.getDefaultValue())
					.setValue(null);
		} else if (pd instanceof FileParameterDefinition) {
			FileParameterValue filePv = (FileParameterValue) pv;
			result = dtoFactory.newDTO(CIParameter.class)
					.setType(pc.getType())
					.setName(pc.getName())
					.setDescription(pc.getDescription())
					.setChoices(pc.getChoices())
					.setDefaultValue(pc.getDefaultValue())
					.setValue(filePv != null ? filePv.getOriginalFileName() : null);
		} else {
			result = new UnsupportedParameterProcessor().createParameterInstance(pd, pv);
		}
		return result;
	}
}
