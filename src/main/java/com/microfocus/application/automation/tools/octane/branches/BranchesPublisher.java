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

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.hp.octane.integrations.OctaneClient;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.exceptions.OctaneBulkException;
import com.hp.octane.integrations.exceptions.OctaneValidationException;
import com.hp.octane.integrations.services.pullrequestsandbranches.BranchSyncResult;
import com.hp.octane.integrations.services.pullrequestsandbranches.PullRequestAndBranchService;
import com.hp.octane.integrations.services.pullrequestsandbranches.factory.BranchFetchParameters;
import com.hp.octane.integrations.services.pullrequestsandbranches.factory.FetchFactory;
import com.hp.octane.integrations.services.pullrequestsandbranches.factory.FetchHandler;
import com.hp.octane.integrations.services.pullrequestsandbranches.rest.ScmTool;
import com.hp.octane.integrations.services.pullrequestsandbranches.rest.authentication.AuthenticationStrategy;
import com.microfocus.application.automation.tools.octane.GitFetchUtils;
import com.microfocus.application.automation.tools.octane.JellyUtils;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Post-build action of Uft test detection
 */

public class BranchesPublisher extends Recorder implements SimpleBuildStep {
    private String configurationId;
    private String workspaceId;
    private String repositoryUrl;
    private String credentialsId;
    private String filter;
    private String scmTool;
    private String useSSHFormat;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public BranchesPublisher(String configurationId, String workspaceId, String scmTool, String repositoryUrl, String credentialsId, String filter) {
        this.configurationId = JellyUtils.NONE.equalsIgnoreCase(configurationId) ? null : configurationId;
        this.workspaceId = JellyUtils.NONE.equalsIgnoreCase(workspaceId) ? null : workspaceId;
        this.repositoryUrl = repositoryUrl;
        this.credentialsId = credentialsId;
        this.filter = filter;
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
        logConsumer.printLog("BranchPublisher is started ***********************************************************************");
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
        String myUseSshFormat = useSSHFormat;
        try {
            EnvVars env = run.getEnvironment(taskListener);
            myCredentialsId = env.expand(credentialsId);
            myConfigurationId = env.expand(configurationId);
            myWorkspaceId = env.expand(workspaceId);
            myScmTool = env.expand(scmTool);
            myUseSshFormat = env.expand(useSSHFormat);
        } catch (IOException | InterruptedException e) {
            taskListener.error("Failed loading build environment " + e);
        }

        BranchFetchParameters fp = createFetchParameters(run, taskListener, myUseSshFormat, logConsumer::printLog);

        StandardCredentials credentials = GitFetchUtils.getCredentialsById(myCredentialsId, run, taskListener.getLogger());
        AuthenticationStrategy authenticationStrategy = GitFetchUtils.getAuthenticationStrategy(credentials);

        try {
            //GET BRANCHES FROM CI SERVER
            FetchHandler fetchHandler = FetchFactory.getHandler(ScmTool.fromValue(myScmTool), authenticationStrategy);

            OctaneClient octaneClient = OctaneSDK.getClientByInstanceId(myConfigurationId);
            logConsumer.printLog("ALM Octane " + octaneClient.getConfigurationService().getConfiguration().getLocationForLog() + ", workspace - " + myWorkspaceId);
            octaneClient.validateOctaneIsActiveAndSupportVersion(PullRequestAndBranchService.BRANCH_COLLECTION_SUPPORTED_VERSION);
            PullRequestAndBranchService service = OctaneSDK.getClientByInstanceId(myConfigurationId).getPullRequestAndBranchService();
            BranchSyncResult result = service.syncBranchesToOctane(fetchHandler, fp, Long.parseLong(myWorkspaceId), GitFetchUtils::getUserIdForCommit, logConsumer::printLog);

            String repoUrlForOctane =  fp.isUseSSHFormat() ? fp.getRepoUrlSsh() : fp.getRepoUrl();
            GitFetchUtils.updateRepoTemplates(service, fetchHandler, fp.getRepoUrl(), repoUrlForOctane, Long.parseLong(myWorkspaceId), logConsumer::printLog);

            synchronized (BranchesBuildAction.class) {
                long index = run.getActions(BranchesBuildAction.class).size();
                BranchesBuildAction buildAction = new BranchesBuildAction(run, result, fp.getRepoUrl(), fp.getFilter(), index);
                run.addAction(buildAction);
            }

        } catch (OctaneValidationException e) {
            logConsumer.printLog("ALM Octane branch collector failed on validation : " + e.getMessage());
            run.setResult(Result.FAILURE);
        } catch (OctaneBulkException e) {
            //grouping error messages in format : "exception message (count)
            String exceptions = e.getData().getErrors().stream().map(m -> m.getDescriptionTranslated()).collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                    .entrySet().stream().map(entry -> entry.getKey() + "(" + entry.getValue() + ")")
                    .collect(Collectors.joining(System.lineSeparator() + "  - ", " Exceptions are : " + System.lineSeparator() + "  - ", ""));
            logConsumer.printLog("ALM Octane branch collector failed : " + e.getMessage() + exceptions);
            run.setResult(Result.FAILURE);
        } catch (Exception e) {
            logConsumer.printLog("ALM Octane branch collector failed : " + e.getMessage());
            e.printStackTrace(taskListener.getLogger());
            run.setResult(Result.FAILURE);
        }
    }

    @DataBoundSetter
    public void setUseSSHFormat(boolean useSSHFormat) {
        this.useSSHFormat = Boolean.toString(useSSHFormat);
    }


    public boolean getUseSSHFormat() {
        return Boolean.parseBoolean(useSSHFormat);
    }

    private BranchFetchParameters createFetchParameters(@Nonnull Run<?, ?> run, @Nonnull TaskListener taskListener, String myUseSshFormat, Consumer<String> logConsumer) {

        BranchFetchParameters fp;
        try {
            EnvVars env = run.getEnvironment(taskListener);
            fp = new BranchFetchParameters()
                    .setRepoUrl(env.expand(repositoryUrl))
                    .setFilter(env.expand(filter))
                    .setUseSSHFormat(Boolean.parseBoolean(env.expand(myUseSshFormat)));
        } catch (IOException | InterruptedException e) {
            taskListener.error("Failed loading build environment " + e);
            fp = new BranchFetchParameters()
                    .setRepoUrl(repositoryUrl)
                    .setFilter(filter)
                    .setUseSSHFormat(Boolean.parseBoolean(myUseSshFormat));
        }

        ParametersAction parameterAction = run.getAction(ParametersAction.class);
        if (parameterAction != null) {
            fp.setPageSize(getIntegerValueParameter(parameterAction, "branches_page_size"));
            fp.setActiveBranchDays(getIntegerValueParameter(parameterAction, "branches_active_branch_days"));
            fp.setMaxBranchesToFill(getIntegerValueParameter(parameterAction, "branches_max_branches_to_fill"));
        }

        logConsumer.accept("Repository URL       : " + fp.getRepoUrl());
        logConsumer.accept("Filter               : " + fp.getFilter());
        logConsumer.accept("Page size            : " + fp.getPageSize());
        logConsumer.accept("Max branches to fill : " + fp.getMaxBranchesToFill());
        logConsumer.accept("Branch active days   : " + fp.getActiveBranchDays());
        logConsumer.accept("Use ssh format       : " + fp.isUseSSHFormat());

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

    public String getConfigurationId() {
        return configurationId;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    private static class LogConsumer {

        private final PrintStream ps;

        public LogConsumer(PrintStream ps) {
            this.ps = ps;
        }

        public void printLog(String msg) {
            ps.println("BranchPublisher : " + msg);
        }
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

    public String getFilter() {
        return filter;
    }

    public String getScmTool() {
        return scmTool;
    }

    @Symbol("collectBranchesToAlmOctane")
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        static final CredentialsMatcher CREDENTIALS_MATCHER = CredentialsMatchers.anyOf(new CredentialsMatcher[]{
                CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class),
                CredentialsMatchers.instanceOf(StringCredentials.class)});

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item project,
                                                     @QueryParameter String credentialsId) {

            return JellyUtils.fillCredentialsIdItems(project, credentialsId, CREDENTIALS_MATCHER);
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
            return "ALM Octane branch collector";
        }
    }
}
