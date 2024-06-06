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

package com.microfocus.application.automation.tools.lr.model;

import com.microfocus.application.automation.tools.lr.Messages;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.EnvVars;
import org.kohsuke.stapler.DataBoundConstructor;
import org.apache.commons.lang.StringUtils;
import javax.annotation.Nonnull;
import java.util.List;
import java.util.Properties;

/**
 * Use case: users can add runtime settings to scripts from jenkins (currently only additional
 * attributes)
 * This model will be sent to HpToolsLauncher (by saving it in the props.txt file) which parses
 * the scripts and performs api calls on controller
 *
 * Describes a container for scripts and their associated runtime settings
 */
public class ScriptRTSSetModel extends AbstractDescribableImpl<ScriptRTSSetModel> {
    private List<ScriptRTSModel> scripts;

    @DataBoundConstructor
    public ScriptRTSSetModel(List<ScriptRTSModel> scripts) {
        this.scripts = scripts;
    }

    public List<ScriptRTSModel> getScripts() {
        return scripts;
    }

    /**
     * Adds scripts to the props file containing script name
     *
     * @param props
     */
    public void addScriptsToProps(Properties props, EnvVars envVars) {
        int scriptCounter = 1;

        ScriptRTSModel.additionalAttributeCounter = 1;
        for (ScriptRTSModel script: this.scripts) {
            if (!StringUtils.isEmpty(script.getScriptName())) {
                props.put("ScriptRTS" + scriptCounter, script.getScriptName());
                script.addAdditionalAttributesToPropsFile(props, script.getScriptName(), envVars);
                scriptCounter++;
            }
        }
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ScriptRTSSetModel>
    {
        @Nonnull
        public String getDisplayName() { return Messages.ScriptRTSSetModel(); }
    }
}
