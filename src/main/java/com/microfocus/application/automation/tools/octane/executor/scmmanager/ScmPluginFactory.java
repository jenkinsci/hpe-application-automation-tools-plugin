/*
 *  Certain versions of software accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 *  This software was acquired by Micro Focus on September 1, 2017, and is now
 *  offered by OpenText.
 *  Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical
 *  in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the
 *  property of their respective owners.
 *  OpenText is a trademark of Open Text.
 *  __________________________________________________________________
 *  MIT License
 *
 *  Copyright 2012-2025 Open Text.
 *
 *  The only warranties for products and services of Open Text and
 *  its affiliates and licensors ("Open Text") are as may be set forth
 *  in the express warranty statements accompanying such products and services.
 *  Nothing herein should be construed as constituting an additional warranty.
 *  Open Text shall not be liable for technical or editorial errors or
 *  omissions contained herein. The information contained herein is subject
 *  to change without notice.
 *
 *  Except as specifically indicated otherwise, this document contains
 *  confidential information and a valid license is required for possession,
 *  use or copying. If this work is provided to the U.S. Government,
 *  consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 *  Computer Software Documentation, and Technical Data for Commercial Items are
 *  licensed to the U.S. Government under vendor's standard commercial license.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ___________________________________________________________________
 */
package com.microfocus.application.automation.tools.octane.executor.scmmanager;

import com.hp.octane.integrations.dto.scm.SCMType;
import hudson.PluginWrapper;
import jenkins.model.Jenkins;

public class ScmPluginFactory {

    private static ScmPluginHandler gitPluginHandler = null;
    private static ScmPluginHandler svnPluginHandler = null;


    public static ScmPluginHandler getScmHandler(SCMType scmType) {
        if (SCMType.GIT.equals(scmType)) {
            if (gitPluginHandler == null) {
                gitPluginHandler = new GitPluginHandler();
            }
            return gitPluginHandler;
        } else if (SCMType.SVN.equals(scmType)) {
            if (svnPluginHandler == null) {
                svnPluginHandler = new SvnPluginHandler();
            }
            return svnPluginHandler;
        }
        throw new IllegalArgumentException("SCM repository " + scmType + " isn't supported.");
    }

    public static ScmPluginHandler getScmHandlerByScmPluginName(String pluginName) {
        SCMType scmType = null;

        if ("hudson.plugins.git.GitSCM".equals(pluginName)) {
            scmType = SCMType.GIT;
        } else if ("hudson.scm.SubversionSCM".equals(pluginName)) {
            scmType = SCMType.SVN;
        } else {
            return null;
        }
        return getScmHandler(scmType);
    }

    public static boolean isPluginInstalled(SCMType scmType) {
        String shortName;
        if (SCMType.GIT.equals(scmType)) {
            shortName = "git";
        } else if (SCMType.SVN.equals(scmType)) {
            shortName = "subversion";
        } else {
            throw new IllegalArgumentException("SCM repository " + scmType + " isn't supported.");
        }

        PluginWrapper plugin = Jenkins.getInstanceOrNull().pluginManager.getPlugin(shortName);
        return plugin != null;
    }
}
