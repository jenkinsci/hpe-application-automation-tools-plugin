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

package com.microfocus.application.automation.tools.octane.pullrequests;

import com.hp.octane.integrations.dto.scm.PullRequest;
import com.hp.octane.integrations.dto.scm.SCMCommit;
import com.microfocus.application.automation.tools.octane.GitFetchUtils;
import com.microfocus.application.automation.tools.octane.Messages;
import hudson.model.Action;
import hudson.model.Run;

import javax.annotation.CheckForNull;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class PullRequestBuildAction implements Action {

    private final Run<?, ?> build;

    private final List<PullRequest> pullRequests;
    private final long minUpdateTime;
    private final String sourceBranchFilter;
    private final String targetBranchFilter;
    private final String repositoryUrl;
    private final Long index;

    private SimpleDateFormat dateFormat = null;

    @CheckForNull
    @Override
    public String getIconFileName() {
        return "notepad.png";
    }

    public PullRequestBuildAction(final Run<?, ?> build, List<PullRequest> pullRequests, String repositoryUrl, long minUpdateTime,
                                  String sourceBranchFilter, String targetBranchFilter, long index) {
        this.build = build;
        this.index = index;
        this.pullRequests = pullRequests;
        this.minUpdateTime = minUpdateTime;
        this.sourceBranchFilter = sourceBranchFilter;
        this.targetBranchFilter = targetBranchFilter;
        this.repositoryUrl = repositoryUrl;
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return Messages.PullRequestActionConfigurationLabel();
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return "pull-request-report" + (index == null || index.equals(0l) ? "" : "-" + index);
    }

    public List<PullRequest> getPullRequests() {
        return pullRequests;
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

    public String getTopCommits(PullRequest p) {
        StringBuilder sb = new StringBuilder();
        int counter = 0;
        int max_detailed_count = 10;
        int max_commit_message_size = 50;
        for (SCMCommit commit : p.getCommits()) {
            sb.append(commit.getComment(), 0, Math.min(max_commit_message_size, commit.getComment().length()));
            if (commit.getComment().length() > max_commit_message_size) {
                sb.append("...");
            }
            sb.append("\n");
            counter++;
            if (counter >= max_detailed_count) {
                sb.append("And other " + (p.getCommits().size() - max_detailed_count) + " commits");
                break;
            }
        }
        return sb.toString();
    }

    public long getMinUpdateTime() {
        return minUpdateTime;
    }

    public String getSourceBranchFilter() {
        return sourceBranchFilter;
    }

    public String getTargetBranchFilter() {
        return targetBranchFilter;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }
}
