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
package com.microfocus.application.automation.tools.octane.actions;

import com.hp.octane.integrations.uft.items.OctaneStatus;
import com.hp.octane.integrations.uft.items.SupportsOctaneStatus;
import com.hp.octane.integrations.uft.items.UftTestDiscoveryResult;
import com.microfocus.application.automation.tools.octane.Messages;
import com.microfocus.application.automation.tools.octane.executor.UFTTestDetectionService;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Run;

import java.util.List;

/**
 * Class responsible to show report of  {@link UFTTestDetectionService}
 */
public class UFTTestDetectionBuildAction implements Action {
    private AbstractBuild<?, ?> build;


    private UftTestDiscoveryResult results;

    @Override
    public String getIconFileName() {
        return "notepad.png";
    }

    @Override
    public String getDisplayName() {
        return Messages.UFTTestDetectionBuildActionConfigurationLabel();
    }

    @Override
    public String getUrlName() {
        return "uft_report";
    }

    @SuppressWarnings("squid:S1452")
    public final Run<?, ?> getBuild() {
        return build;
    }

    public UFTTestDetectionBuildAction(final AbstractBuild<?, ?> build, UftTestDiscoveryResult results) {
        this.build = build;
        this.results = results == null ? new UftTestDiscoveryResult() : results;
    }

    public UftTestDiscoveryResult getResults() {
        return results;
    }

    /**
     * used by ~\src\main\resources\com\hp\application\automation\tools\octane\actions\UFTTestDetectionBuildAction\index.jelly
     *
     * @return
     */
    public boolean getHasNewTests() {
        return countItemsWithStatus(OctaneStatus.NEW, results.getAllTests()) > 0;
    }

    /**
     * used by ~\src\main\resources\com\hp\application\automation\tools\octane\actions\UFTTestDetectionBuildAction\index.jelly
     *
     * @return
     */
    public boolean getHasDeletedTests() {
        return countItemsWithStatus(OctaneStatus.DELETED, results.getAllTests()) > 0;
    }

    /**
     * used by ~\src\main\resources\com\hp\application\automation\tools\octane\actions\UFTTestDetectionBuildAction\index.jelly
     *
     * @return
     */
    public boolean getHasUpdatedTests() {
        return countItemsWithStatus(OctaneStatus.MODIFIED, results.getAllTests()) > 0;
    }

    public boolean getHasQuotedPaths() {
        return results.isHasQuotedPaths();
    }

    /**
     * used by ~\src\main\resources\com\hp\application\automation\tools\octane\actions\UFTTestDetectionBuildAction\index.jelly
     *
     * @return
     */
    public boolean getHasNewScmResources() {
        return countItemsWithStatus(OctaneStatus.NEW, results.getAllScmResourceFiles()) > 0;
    }

    /**
     * used by ~\src\main\resources\com\hp\application\automation\tools\octane\actions\UFTTestDetectionBuildAction\index.jelly
     *
     * @return
     */
    public boolean getHasDeletedScmResources() {
        return countItemsWithStatus(OctaneStatus.DELETED, results.getAllScmResourceFiles()) > 0;
    }

    /**
     * used by ~\src\main\resources\com\hp\application\automation\tools\octane\actions\UFTTestDetectionBuildAction\index.jelly
     *
     * @return
     */
    public boolean getHasUpdatedScmResources() {
        return countItemsWithStatus(OctaneStatus.MODIFIED, results.getAllScmResourceFiles()) > 0;
    }

    /**
     * used by ~\src\main\resources\com\hp\application\automation\tools\octane\actions\UFTTestDetectionBuildAction\index.jelly
     *
     * @return
     */
    public boolean getHasDeletedFolders() {
        return results.getDeletedFolders().size() > 0;
    }


    public void setResults(UftTestDiscoveryResult results) {
        this.results = results;
    }

    private static int countItemsWithStatus(OctaneStatus status, List<? extends SupportsOctaneStatus> items) {

        int count = 0;
        for (SupportsOctaneStatus item : items) {
            if (item.getOctaneStatus().equals(status)) {
                count++;
            }
        }
        return count;
    }
}
