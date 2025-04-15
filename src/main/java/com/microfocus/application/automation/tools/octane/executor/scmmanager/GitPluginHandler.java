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

import com.cloudbees.hudson.plugins.folder.Folder;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.executor.TestConnectivityInfo;
import com.hp.octane.integrations.dto.scm.SCMRepository;
import com.hp.octane.integrations.dto.scm.SCMType;
import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import hudson.EnvVars;
import hudson.model.*;
import hudson.plugins.git.*;
import hudson.plugins.git.extensions.GitSCMExtension;
import hudson.plugins.git.extensions.impl.RelativeTargetDirectory;
import hudson.scm.SCM;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.plugins.gitclient.Git;
import org.jenkinsci.plugins.gitclient.GitClient;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitPluginHandler implements ScmPluginHandler {
    private static final Logger logger = SDKBasedLoggerProvider.getLogger(GitPluginHandler.class);

    @Override
    public void setScmRepositoryInJob(SCMRepository scmRepository, String scmRepositoryCredentialsId, FreeStyleProject proj, boolean executorJob) throws IOException {

        List<UserRemoteConfig> repoLists = Collections.singletonList(new UserRemoteConfig(scmRepository.getUrl(), null, null, scmRepositoryCredentialsId));
        List<GitSCMExtension> extensions = null;

        if (executorJob) {
            String folder = buildRepoFolder(proj);
            String repoName = buildRepoName(scmRepository.getUrl());
            String relativeCheckOut = folder + repoName;
            RelativeTargetDirectory targetDirectory = new RelativeTargetDirectory(relativeCheckOut);
            extensions = Collections.singletonList(targetDirectory);
        }

        String branch = "*/master";
        if (proj.getScm() != null && proj.getScm() instanceof GitSCM) {
            List<BranchSpec> branches = ((GitSCM) proj.getScm()).getBranches();
            if (branches.size() > 0) {
                String existingBrName = branches.get(0).getName();
                boolean emptyBrName = (existingBrName == null || existingBrName.isEmpty() || existingBrName.equals("**"));
                if (!emptyBrName) {
                    branch = existingBrName;
                }
            }
        }

        GitSCM scm = new GitSCM(repoLists, Collections.singletonList(new BranchSpec(branch)), false, Collections.emptyList(), null, null, extensions);
        proj.setScm(scm);
    }

    private String buildRepoFolder(FreeStyleProject proj) {
        String pathPrefix = "";//its required if job is created in some folder
        ItemGroup parent = proj.getParent();
        while (Jenkins.get() != null && !parent.equals(Jenkins.get())) {
            pathPrefix += "..\\";
            if (parent instanceof Folder) {
                parent = ((Folder) parent).getParent();
            } else {
                break;
            }
        }
        return pathPrefix + "..\\..\\_test_sources\\";
    }

    private String buildRepoName(String scmRepoUrl) {
        String temp = scmRepoUrl.replaceAll("[<>:\"/\\|?*]", "_").trim();
        String GIT_SUFFIX = ".git";
        String HTTP_PREFIX = "http_";
        String HTTPS_PREFIX = "https_";
        String GIT_PREFIX = "git@";

        //remove .git from end
        if (temp.toLowerCase().endsWith(GIT_SUFFIX)) {
            temp = temp.substring(0, temp.length() - GIT_SUFFIX.length());
        }
        //remove git_,https_,http_ from beginning
        if (temp.toLowerCase().startsWith(GIT_PREFIX)) {
            temp = temp.substring(GIT_PREFIX.length());
        } else if (temp.toLowerCase().startsWith(HTTPS_PREFIX)) {
            temp = temp.substring(HTTPS_PREFIX.length());
        } else if (temp.toLowerCase().startsWith(HTTP_PREFIX)) {
            temp = temp.substring(HTTP_PREFIX.length());
        }
        temp = StringUtils.strip(temp, "_");

        //restrict size by
        int CHECK_SIZE = 40;
        int SPLIT_SIZE = CHECK_SIZE - 4;
        if (temp.length() > CHECK_SIZE) {
            String split1 = temp.substring(0, temp.length() - SPLIT_SIZE);
            String split2 = temp.substring(temp.length() - SPLIT_SIZE);
            temp = "_" + Math.abs(split1.hashCode() % 1000) + split2;
        }
        return temp;
    }

    @Override
    public String getSharedCheckOutDirectory(Job j) {
        SCM scm = ((AbstractProject) j).getScm();

        GitSCM gitScm = (GitSCM) scm;
        RelativeTargetDirectory sharedCheckOutDirectory = gitScm.getExtensions().get(RelativeTargetDirectory.class);
        if (sharedCheckOutDirectory != null) {
            return sharedCheckOutDirectory.getRelativeTargetDir();
        }
        return null;
    }

    @Override
    public void checkRepositoryConnectivity(TestConnectivityInfo testConnectivityInfo, StandardCredentials credentials, OctaneResponse result) {

        try {
            EnvVars environment = new EnvVars(System.getenv());
            GitClient git = Git.with(TaskListener.NULL, environment).using(GitTool.getDefaultInstallation().getGitExe()).getClient();
            git.addDefaultCredentials(credentials);
            git.getHeadRev(testConnectivityInfo.getScmRepository().getUrl(), "HEAD");

            result.setStatus(HttpStatus.SC_OK);

        } catch (IOException | InterruptedException e) {
            logger.error("Failed to connect to git : " + e.getMessage());
            result.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            result.setBody(e.getMessage());
        } catch (GitException e) {
            logger.error("Failed to execute getHeadRev : " + e.getMessage());
            result.setStatus(HttpStatus.SC_NOT_FOUND);
            result.setBody(e.getMessage());
        }

    }

    @Override
    public String getScmRepositoryUrl(SCM scm) {
        return ((GitSCM) scm).getUserRemoteConfigs().get(0).getUrl();
    }

    @Override
    public String getScmRepositoryCredentialsId(SCM scm) {
        return ((GitSCM) scm).getUserRemoteConfigs().get(0).getCredentialsId();
    }

    @Override
    public SCMType getScmType() {
        return SCMType.GIT;
    }

    @Override
    public String tryExtractUrlShortName(String url) {
		/*
        Use Reg-ex pattern which covers both formats:
        git@github.com:MicroFocus/hpaa-octane-dev.git
        https://github.houston.softwaregrp.net/Octane/syncx.git
        */

        String patternStr = "^.*[/:](.*/.*)$";
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(url);
        if (matcher.find() && matcher.groupCount() == 1) {
            return matcher.group(1);
        } else {
            return url;
        }
    }
}
