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

package com.microfocus.application.automation.tools.octane.executor;

import com.hp.octane.integrations.OctaneSDK;
import com.microfocus.application.automation.tools.octane.executor.scmmanager.ScmPluginFactory;
import com.microfocus.application.automation.tools.octane.executor.scmmanager.ScmPluginHandler;
import com.microfocus.application.automation.tools.run.RunFromFileBuilder;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentContributor;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.TaskListener;
import hudson.scm.NullSCM;
import hudson.scm.SCM;
import hudson.tasks.Builder;

import java.util.List;

/**
 * Add job environment value for CHECKOUT_SUBDIR
 */
@Extension
public class CheckOutSubDirEnvContributor extends EnvironmentContributor {

    public static final String CHECKOUT_SUBDIR_ENV_NAME = "CHECKOUT_SUBDIR";

    @Override
    public void buildEnvironmentFor(Job j, EnvVars envs, TaskListener listener) {
        if(!OctaneSDK.hasClients()){
            return;
        }
        String dir = getSharedCheckOutDirectory(j);
        if (dir != null) {
            envs.put(CHECKOUT_SUBDIR_ENV_NAME, dir);
        }
    }

    public static String getSharedCheckOutDirectory(Job j) {
        if (j instanceof FreeStyleProject) {
            FreeStyleProject proj = (FreeStyleProject) j;
            SCM scm = proj.getScm();
            List<Builder> builders = proj.getBuilders();
            if (scm != null && !(scm instanceof NullSCM) && builders != null) {
                for (Builder builder : builders) {
                    if (builder instanceof RunFromFileBuilder) {
                        ScmPluginHandler scmPluginHandler = ScmPluginFactory.getScmHandlerByScmPluginName(scm.getClass().getName());
                        if (scmPluginHandler != null) {
                            return scmPluginHandler.getSharedCheckOutDirectory(j);
                        }
                    }
                }

            }
        }

        return null;
    }

}

