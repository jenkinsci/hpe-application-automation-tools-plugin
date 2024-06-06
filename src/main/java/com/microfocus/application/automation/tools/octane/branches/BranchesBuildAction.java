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

package com.microfocus.application.automation.tools.octane.branches;

import com.hp.octane.integrations.services.pullrequestsandbranches.BranchSyncResult;
import com.microfocus.application.automation.tools.octane.GitFetchUtils;
import com.microfocus.application.automation.tools.octane.Messages;
import hudson.model.Action;
import hudson.model.Run;

import javax.annotation.CheckForNull;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BranchesBuildAction implements Action {

    private final Run<?, ?> build;

    private final BranchSyncResult branchSyncResult;
    private final String filter;
    private final String repositoryUrl;
    private final Long index;

    private SimpleDateFormat dateFormat = null;

    @CheckForNull
    @Override
    public String getIconFileName() {
        return "notepad.png";
    }

    public BranchesBuildAction(final Run<?, ?> build, BranchSyncResult branchSyncResult, String repositoryUrl, String filter,  long index) {
        this.index = index;
        this.build = build;
        this.branchSyncResult = branchSyncResult;
        this.filter = filter;
        this.repositoryUrl = repositoryUrl;
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return Messages.BranchActionConfigurationLabel();
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return "branch-report" + (index == null || index.equals(0l) ? "" : "-" + index);
    }

    public BranchSyncResult getBranchSyncResult() {
        return branchSyncResult;
    }

    @SuppressWarnings("squid:S1452")
    public final Run<?, ?> getBuild() {
        return build;
    }

    public String getFormattedDate(long longTime) {
        if (dateFormat == null) {
            dateFormat = GitFetchUtils.generateDateFormat();
        }
        return dateFormat.format(new Date(longTime));
    }

    public String getFilter() {
        return filter;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }
}
