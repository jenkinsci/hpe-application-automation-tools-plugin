/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2019 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors ("Micro Focus") are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.octane.pullrequests;

import com.hp.octane.integrations.dto.scm.PullRequest;
import com.hp.octane.integrations.dto.scm.SCMCommit;
import com.microfocus.application.automation.tools.octane.Messages;
import hudson.model.Action;
import hudson.model.Run;

import javax.annotation.CheckForNull;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class PullRequestBuildAction implements Action {

    private final Run<?, ?> build;

    private final List<PullRequest> pullRequests;
    private final long minUpdateTime;
    private final String sourceBranchFilter;
    private final String targetBranchFilter;
    private final String repositoryUrl;

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm";
    private SimpleDateFormat updatedDateFormat = null;

    @CheckForNull
    @Override
    public String getIconFileName() {
        return "notepad.png";
    }

    public PullRequestBuildAction(final Run<?, ?> build, List<PullRequest> pullRequests, String repositoryUrl, long minUpdateTime,
                                  String sourceBranchFilter, String targetBranchFilter) {
        this.build = build;
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
        return "pull-request-report";
    }

    public List<PullRequest> getPullRequests() {
        return pullRequests;
    }

    @SuppressWarnings("squid:S1452")
    public final Run<?, ?> getBuild() {
        return build;
    }

    public String getFormattedDate(long longTime) {
       if (updatedDateFormat == null) {
            updatedDateFormat = new SimpleDateFormat(DATE_TIME_FORMAT);
            TimeZone utc = TimeZone.getTimeZone("UTC");
            updatedDateFormat.setTimeZone(utc);
        }
        return updatedDateFormat.format(new Date(longTime));
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
