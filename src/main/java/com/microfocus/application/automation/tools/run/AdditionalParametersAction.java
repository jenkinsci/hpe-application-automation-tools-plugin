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

package com.microfocus.application.automation.tools.run;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentContributor;
import hudson.model.ParameterValue;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.ParametersAction;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Fix for supporting SECURITY-170 changes
 * https://jenkins.io/blog/2016/05/11/security-update/
 * https://issues.jenkins-ci.org/browse/JENKINS-39654
 *
 */
public class AdditionalParametersAction  extends ParametersAction{

    private List<ParameterValue> parameters;

    public AdditionalParametersAction(List<ParameterValue> cparameters){
        this.parameters = Collections.unmodifiableList(cparameters);
    }

    @Override
    public List<ParameterValue> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    @Override
    public List<ParameterValue> getAllParameters() {
        return Collections.unmodifiableList(parameters);
    }

    @Override
    public ParameterValue getParameter(String name){
        for (ParameterValue p : parameters) {
            if (p == null)
                continue;
            if (p.getName().equals(name))
                return p;
        }
        return null;
    }

    @Extension
    public static final class AdditionalParametersActionEnvironmentContributor extends EnvironmentContributor {
        @Override
        public void buildEnvironmentFor(Run r, EnvVars envs, TaskListener listener)
            throws IOException, InterruptedException {
            AdditionalParametersAction action = r.getAction(AdditionalParametersAction.class);
            if (action != null) {
                for (ParameterValue p : action.getParameters()) {
                    envs.putIfNotNull(p.getName(), String.valueOf(p.getValue()));
                }
            }
        }
    }
}
