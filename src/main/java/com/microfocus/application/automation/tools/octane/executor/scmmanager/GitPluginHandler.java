/*
 *
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * © Copyright 2012-2018 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors (“Micro Focus”) are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 *
 */

package com.microfocus.application.automation.tools.octane.executor.scmmanager;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.executor.TestConnectivityInfo;
import com.hp.octane.integrations.dto.scm.SCMRepository;
import hudson.EnvVars;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.TaskListener;
import hudson.plugins.git.*;
import hudson.plugins.git.extensions.GitSCMExtension;
import hudson.plugins.git.extensions.impl.RelativeTargetDirectory;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCM;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.plugins.gitclient.Git;
import org.jenkinsci.plugins.gitclient.GitClient;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class GitPluginHandler implements ScmPluginHandler {

	private static final Logger logger = LogManager.getLogger(GitPluginHandler.class);

	@Override
	public void setScmRepositoryInJob(SCMRepository scmRepository, String scmRepositoryCredentialsId, FreeStyleProject proj, boolean executorJob) throws IOException {

		List<UserRemoteConfig> repoLists = Collections.singletonList(new UserRemoteConfig(scmRepository.getUrl(), null, null, scmRepositoryCredentialsId));
		List<GitSCMExtension> extensions = null;

		if (executorJob) {
			String relativeCheckOut = "..\\..\\_test_sources\\" + scmRepository.getUrl().replaceAll("[<>:\"/\\|?*]", "_");
			RelativeTargetDirectory targetDirectory = new RelativeTargetDirectory(relativeCheckOut);
			extensions = Collections.<GitSCMExtension>singletonList(targetDirectory);
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
	public String getChangeSetSrc(ChangeLogSet.AffectedFile affectedFile) {
		return ((GitChangeSet.Path) affectedFile).getSrc();
	}

	@Override
	public String getChangeSetDst(ChangeLogSet.AffectedFile affectedFile) {
		return ((GitChangeSet.Path) affectedFile).getDst();
	}
}
