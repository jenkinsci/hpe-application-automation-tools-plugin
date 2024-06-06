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

package com.microfocus.application.automation.tools.octane.events;

import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import com.microfocus.application.automation.tools.settings.OutputEnvironmentVariablesBuildWrapper;
import com.microfocus.application.automation.tools.settings.RunnerMiscSettingsGlobalConfiguration;
import com.microfocus.application.automation.tools.sse.common.StringUtils;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Job;
import hudson.model.Run;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OutputEnvironmentParametersHelper {

	private static final String SPLIT_SYMBOL = "\\s++";

	private OutputEnvironmentParametersHelper(){}

	private static Logger logger = SDKBasedLoggerProvider.getLogger(OutputEnvironmentParametersHelper.class);

	public static Map<String, String> getOutputEnvironmentParams(Run run) {
		EnvVars environment = getEnvironment(run);
		if (environment == null) {
			return Collections.emptyMap();
		} else {
			List<String> paramKeysList = new ArrayList<>();
			paramKeysList.addAll(getGlobalParamsList());
			paramKeysList.addAll(getJobParamsList(run));

			if (paramKeysList.isEmpty()) return Collections.emptyMap();

			Map<String, String> outputEnvParams = new HashMap<>();
			Set<String> sensitiveBuildVariables =null;
			if (run instanceof AbstractBuild) {
				sensitiveBuildVariables = ((AbstractBuild) run).getSensitiveBuildVariables();
			}

			String value;
			for (String key : paramKeysList) {
				if (sensitiveBuildVariables != null && sensitiveBuildVariables.contains(key)) continue;
				value = environment.get(key);
				if (value != null) {
					outputEnvParams.put(key, value);
				}
			}
			return outputEnvParams;
		}
	}

	private static EnvVars getEnvironment(Run run) {
		EnvVars environment = null;
		try {
			environment = run.getEnvironment(null);
		} catch (IOException | InterruptedException e) {
			logger.error("Can not get Run(id: " + run.getId() + ") Environment: " + e.getMessage());
		}
		return environment;
	}

	private static List<String> getGlobalParamsList() {
		try {
			String paramsStr = RunnerMiscSettingsGlobalConfiguration.getInstance().getOutputEnvironmentParameters();
			return createParamsListFromString(paramsStr);
		} catch (NullPointerException ignored) {
			return Collections.emptyList();
		}
	}

	private static List<String> getJobParamsList(Run run) {
		Job<?, ?> job = run.getParent();
		if (job instanceof BuildableItemWithBuildWrappers) {
			OutputEnvironmentVariablesBuildWrapper outputEnvVarsBuildWrapper = ((BuildableItemWithBuildWrappers) job)
					.getBuildWrappersList().get(OutputEnvironmentVariablesBuildWrapper.class);
			if (outputEnvVarsBuildWrapper != null) {
				String paramsStr = outputEnvVarsBuildWrapper.getOutputEnvironmentParameters();
				if (!StringUtils.isNullOrEmpty(paramsStr)) {
					return createParamsListFromString(paramsStr);
				}
			}
		}
		return Collections.emptyList();
	}

	private static List<String> createParamsListFromString(String params) {
		return Stream.of(params.split(SPLIT_SYMBOL)).filter(p -> !StringUtils.isNullOrEmpty(p)).collect(Collectors.toList());
	}
}
