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
 *  Copyright 2012-2026 Open Text.
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
package com.microfocus.application.automation.tools.settings;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import jenkins.model.GlobalConfiguration;

import java.io.Serializable;

@Extension
public class MiAgentGlobalConfiguration extends GlobalConfiguration implements Serializable {

    private int maxBuildsToKeep = 50;
    private int maxDaysToKeep = 7;

    public MiAgentGlobalConfiguration() {
        load();
    }

    public static MiAgentGlobalConfiguration getInstance() throws NullPointerException {
        MiAgentGlobalConfiguration configuration = GlobalConfiguration.all().get(MiAgentGlobalConfiguration.class);
        if (configuration == null) {
            throw new NullPointerException();
        }
        return configuration;
    }

    @NonNull
    @Override
    public String getDisplayName() {
        return "Autonomous Tester settings";
    }

    public int getMaxBuildsToKeep() {
        return maxBuildsToKeep;
    }

    public void setMaxBuildsToKeep(int maxBuildsToKeep) {
        this.maxBuildsToKeep = maxBuildsToKeep;
        save();
    }

    public int getMaxDaysToKeep() {
        return maxDaysToKeep;
    }

    public void setMaxDaysToKeep(int maxDaysToKeep) {
        this.maxDaysToKeep = maxDaysToKeep;
        save();
    }
}