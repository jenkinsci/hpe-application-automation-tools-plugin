/*
 * © Copyright 2013 EntIT Software LLC
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright (c) 2018 Micro Focus Company, L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ___________________________________________________________________
 *
 */

package com.hpe.application.automation.tools.octane.executor.scmmanager;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.executor.TestConnectivityInfo;
import com.hp.octane.integrations.dto.scm.SCMRepository;
import com.hpe.application.automation.tools.common.HttpStatus;
import hudson.EnvVars;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.TaskListener;
import hudson.plugins.git.*;
import hudson.plugins.git.extensions.GitSCMExtension;
import hudson.plugins.git.extensions.impl.RelativeTargetDirectory;
import hudson.scm.SCM;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.plugins.gitclient.Git;
import org.jenkinsci.plugins.gitclient.GitClient;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GitPluginHandler implements ScmPluginHandler {

    private static final Logger logger = LogManager.getLogger(GitPluginHandler.class);

    @Override
    public void setScmRepositoryInJob(SCMRepository scmRepository, String scmRepositoryCredentialsId, FreeStyleProject proj, boolean executorJob) throws IOException {

        List<UserRemoteConfig> repoLists = Arrays.asList(new UserRemoteConfig(scmRepository.getUrl(), null, null, scmRepositoryCredentialsId));
        List<GitSCMExtension> extensions = null;

        if (executorJob) {
            String relativeCheckOut = "..\\..\\_test_sources\\" + scmRepository.getUrl().replaceAll("[<>:\"/\\|?*]", "_");
            RelativeTargetDirectory targetDirectory = new RelativeTargetDirectory(relativeCheckOut);
            extensions = Arrays.<GitSCMExtension>asList(targetDirectory);
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

        GitSCM scm = new GitSCM(repoLists, Collections.singletonList(new BranchSpec(branch)), false, Collections.<SubmoduleConfig>emptyList(), null, null, extensions);
        proj.setScm(scm);
    }

    @Override
    public String getSharedCheckOutDirectory(Job j) {
        SCM scm = ((FreeStyleProject) j).getScm();

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

            result.setStatus(HttpStatus.OK.getCode());

        } catch (IOException | InterruptedException e) {
            logger.error("Failed to connect to git : " + e.getMessage());
            result.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
            result.setBody(e.getMessage());
        } catch (GitException e) {
            logger.error("Failed to execute getHeadRev : " + e.getMessage());
            result.setStatus(HttpStatus.NOT_FOUND.getCode());
            result.setBody(e.getMessage());
        }

    }
}
