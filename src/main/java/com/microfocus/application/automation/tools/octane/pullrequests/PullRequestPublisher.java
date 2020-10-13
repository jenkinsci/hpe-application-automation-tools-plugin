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

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.scm.PullRequest;
import com.hp.octane.integrations.services.pullrequests.factory.CommitUserIdPicker;
import com.hp.octane.integrations.services.pullrequests.factory.FetchParameters;
import com.hp.octane.integrations.services.pullrequests.factory.PullRequestFetchFactory;
import com.hp.octane.integrations.services.pullrequests.factory.PullRequestFetchHandler;
import com.hp.octane.integrations.services.pullrequests.rest.ScmTool;
import com.hp.octane.integrations.services.pullrequests.rest.authentication.AuthenticationStrategy;
import com.hp.octane.integrations.services.pullrequests.rest.authentication.BasicAuthenticationStrategy;
import com.hp.octane.integrations.services.pullrequests.rest.authentication.NoCredentialsStrategy;
import com.hp.octane.integrations.services.pullrequests.rest.authentication.PATStrategy;
import com.microfocus.application.automation.tools.octane.JellyUtils;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Post-build action of Uft test detection
 */

public class PullRequestPublisher extends Recorder implements SimpleBuildStep, CommitUserIdPicker {
    private String configurationId;
    private String workspaceId;
    private String repositoryUrl;
    private String credentialsId;
    private String sourceBranchFilter;
    private String targetBranchFilter;
    private String scmTool;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public PullRequestPublisher(String configurationId, String workspaceId, String scmTool, String repositoryUrl, String credentialsId, String sourceBranchFilter, String targetBranchFilter) {
        this.configurationId = JellyUtils.NONE.equalsIgnoreCase(configurationId) ? null : configurationId;
        this.workspaceId = JellyUtils.NONE.equalsIgnoreCase(workspaceId) ? null : workspaceId;
        this.repositoryUrl = repositoryUrl;
        this.credentialsId = credentialsId;
        this.sourceBranchFilter = sourceBranchFilter;
        this.targetBranchFilter = targetBranchFilter;
        this.scmTool = JellyUtils.NONE.equalsIgnoreCase(scmTool) ? null : scmTool;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener taskListener) throws InterruptedException, IOException {
        performInternal(run, taskListener);
    }

    @Override
    public boolean perform(AbstractBuild build, @Nonnull Launcher launcher, BuildListener listener) {
        performInternal(build, listener);
        return build.getResult() == Result.SUCCESS;
    }

    public void performInternal(@Nonnull Run<?, ?> run, @Nonnull TaskListener taskListener) {
        LogConsumer logConsumer = new LogConsumer(taskListener.getLogger());
        logConsumer.printLog("PullRequestPublisher is started.");
        if (configurationId == null) {
            throw new IllegalArgumentException("ALM Octane configuration is not defined.");
        }
        if (workspaceId == null) {
            throw new IllegalArgumentException("ALM Octane workspace is not defined.");
        }
        if (scmTool == null) {
            throw new IllegalArgumentException("SCM Tool is not defined.");
        }

        String myCredentialsId = credentialsId;
        String myConfigurationId = configurationId;
        String myWorkspaceId = workspaceId;
        String myScmTool = scmTool;
        try {
            EnvVars env = run.getEnvironment(taskListener);
            myCredentialsId = env.expand(credentialsId);
            myConfigurationId = env.expand(configurationId);
            myWorkspaceId = env.expand(workspaceId);
            myScmTool = env.expand(scmTool);
        } catch (IOException | InterruptedException e) {
            taskListener.error("Failed loading build environment " + e);
        }

        FetchParameters fp = createFetchParameters(run, taskListener, myConfigurationId, myWorkspaceId, logConsumer::printLog);

        StandardCredentials credentials = getCredentialsById(myCredentialsId, run, taskListener.getLogger());
        AuthenticationStrategy authenticationStrategy = getAuthenticationStrategy(credentials);

        PullRequestFetchHandler fetchHandler = PullRequestFetchFactory.getHandler(ScmTool.fromValue(myScmTool), authenticationStrategy);
        try {
            List<PullRequest> pullRequests = fetchHandler.fetchPullRequests(fp, this, logConsumer::printLog);
            PullRequestBuildAction buildAction = new PullRequestBuildAction(run, pullRequests, fp.getRepoUrl(), fp.getMinUpdateTime(),
                    fp.getSourceBranchFilter(), fp.getTargetBranchFilter());
            run.addAction(buildAction);

            if (!pullRequests.isEmpty()) {
                OctaneSDK.getClientByInstanceId(myConfigurationId).getPullRequestService().sendPullRequests(pullRequests, myWorkspaceId, fp, logConsumer::printLog);
            }
        } catch (Exception e) {
            logConsumer.printLog("Failed to fetch pull requests : " + e.getMessage());
            e.printStackTrace(taskListener.getLogger());
            run.setResult(Result.FAILURE);
        }
    }

    private FetchParameters createFetchParameters(@Nonnull Run<?, ?> run, @Nonnull TaskListener taskListener, String myConfigurationId, String myWorkspaceId, Consumer<String> logConsumer) {

        FetchParameters fp;
        try {
            EnvVars env = run.getEnvironment(taskListener);
            fp = new FetchParameters()
                    .setRepoUrl(env.expand(repositoryUrl))
                    .setSourceBranchFilter(env.expand(sourceBranchFilter))
                    .setTargetBranchFilter(env.expand(targetBranchFilter));
        } catch (IOException | InterruptedException e) {
            taskListener.error("Failed loading build environment " + e);
            fp = new FetchParameters()
                    .setRepoUrl(repositoryUrl)
                    .setSourceBranchFilter(sourceBranchFilter)
                    .setTargetBranchFilter(targetBranchFilter);
        }

        ParametersAction parameterAction = run.getAction(ParametersAction.class);
        if (parameterAction != null) {
            fp.setPageSize(getIntegerValueParameter(parameterAction, "pullrequests_page_size"));
            fp.setMaxPRsToFetch(getIntegerValueParameter(parameterAction, "pullrequests_max_pr_to_collect"));
            fp.setMaxCommitsToFetch(getIntegerValueParameter(parameterAction, "pullrequests_max_commits_to_collect"));
            fp.setMinUpdateTime(getLongValueParameter(parameterAction, "pullrequests_min_update_time"));
        }
        if (fp.getMinUpdateTime() == FetchParameters.DEFAULT_MIN_UPDATE_DATE) {
            long lastUpdateTime = OctaneSDK.getClientByInstanceId(myConfigurationId).getPullRequestService().getLastUpdateTime(myWorkspaceId, fp.getRepoUrl());
            fp.setMinUpdateTime(lastUpdateTime);
        }

        logConsumer.accept("Repository URL        : " + fp.getRepoUrl());
        logConsumer.accept("Min update date       : " + fp.getMinUpdateTime());
        logConsumer.accept("Source branch filter  : " + fp.getSourceBranchFilter());
        logConsumer.accept("Target branch filter  : " + fp.getTargetBranchFilter());
        logConsumer.accept("Max PRs to collect    : " + fp.getMaxPRsToFetch());
        logConsumer.accept("Max commits to collect: " + fp.getMaxCommitsToFetch());
        return fp;
    }

    private Integer getIntegerValueParameter(ParametersAction parameterAction, String paramValue) {
        ParameterValue pv = parameterAction.getParameter(paramValue);
        if (pv != null && pv.getValue() instanceof String) {
            try {
                return Integer.valueOf((String) pv.getValue());
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private Long getLongValueParameter(ParametersAction parameterAction, String paramValue) {
        ParameterValue pv = parameterAction.getParameter(paramValue);
        if (pv != null && pv.getValue() instanceof String) {
            try {
                return Long.valueOf((String) pv.getValue());
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public String getConfigurationId() {
        return configurationId;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    @Override
    public String getUserIdForCommit(String email, String login) {
        if (login != null) {
            User user = User.get(login, false, Collections.emptyMap());
            if (user != null) {
                return user.getId();
            }
        }
        if (email != null && email.contains("@")) {
            String[] emailParts = email.split("@");
            return emailParts[0];

        }
        return login;
    }

    private static class LogConsumer {

        private final PrintStream ps;

        public LogConsumer(PrintStream ps) {
            this.ps = ps;
        }

        public void printLog(String msg) {
            ps.println("PullRequestPublisher : " + msg);
        }
    }

    private AuthenticationStrategy getAuthenticationStrategy(StandardCredentials credentials) {
        AuthenticationStrategy authenticationStrategy;
        if (credentials == null) {
            authenticationStrategy = new NoCredentialsStrategy();
        } else if (credentials instanceof StringCredentials) {
            Secret secret = ((StringCredentials) credentials).getSecret();
            authenticationStrategy = new PATStrategy(secret.getPlainText());
        } else if (credentials instanceof StandardUsernamePasswordCredentials) {
            StandardUsernamePasswordCredentials cr = (StandardUsernamePasswordCredentials) credentials;
            authenticationStrategy = new BasicAuthenticationStrategy(cr.getUsername(), cr.getPassword().getPlainText());
        } else {
            throw new IllegalArgumentException("Credentials type is not supported : " + credentials.getClass().getCanonicalName());
        }

        return authenticationStrategy;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public String getSourceBranchFilter() {
        return sourceBranchFilter;
    }

    public String getTargetBranchFilter() {
        return targetBranchFilter;
    }

    public String getScmTool() {
        return scmTool;
    }

    /**
     * Get user name password credentials by id.
     */
    private StandardCredentials getCredentialsById(String credentialsId, Run<?, ?> run, PrintStream logger) {

        StandardCredentials credentials = null;
        if (!StringUtils.isEmpty(credentialsId)) {
            credentials = CredentialsProvider.findCredentialById(credentialsId,
                    StandardCredentials.class,
                    run,
                    URIRequirementBuilder.create().build());
            if (credentials == null) {
                logger.println("Can not find credentials with the credentialsId:" + credentialsId);
            }
        }

        return credentials;
    }

    @Symbol("collectPullRequestsToAlmOctane")
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        static final CredentialsMatcher CREDENTIALS_MATCHER = CredentialsMatchers.anyOf(new CredentialsMatcher[]{
                CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class),
                CredentialsMatchers.instanceOf(StringCredentials.class)});

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item project,
                                                     @QueryParameter String credentialsId) {

            if (project == null || !project.hasPermission(Item.CONFIGURE)) {
                return new StandardUsernameListBoxModel().includeCurrentValue(credentialsId);
            }

            return new StandardListBoxModel()
                    .includeEmptyValue()
                    .includeMatchingAs(
                            project instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task) project) : ACL.SYSTEM,
                            project,
                            StandardCredentials.class,
                            URIRequirementBuilder.create().build(),
                            CREDENTIALS_MATCHER)
                    .includeCurrentValue(credentialsId);
        }

        public ListBoxModel doFillScmToolItems() {
            ListBoxModel m = JellyUtils.createComboModelWithNoneValue();
            for (ScmTool tool : ScmTool.values()) {
                m.add(tool.getDesc(), tool.getValue());
            }

            return m;
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public ListBoxModel doFillConfigurationIdItems() {
            return JellyUtils.fillConfigurationIdModel();
        }

        public ListBoxModel doFillWorkspaceIdItems(@QueryParameter String configurationId, @QueryParameter(value = "workspaceId") String workspaceId) {
            return JellyUtils.fillWorkspaceModel(configurationId, workspaceId);
        }

        public String getDisplayName() {
            return "ALM Octane pull-request collector";
        }
    }
}
