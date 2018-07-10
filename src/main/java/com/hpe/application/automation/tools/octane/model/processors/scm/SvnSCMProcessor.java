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

package com.hpe.application.automation.tools.octane.model.processors.scm;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.scm.SCMChange;
import com.hp.octane.integrations.dto.scm.SCMCommit;
import com.hp.octane.integrations.dto.scm.SCMData;
import com.hp.octane.integrations.dto.scm.SCMRepository;
import com.hp.octane.integrations.dto.scm.SCMType;
import hudson.model.*;
import hudson.scm.ChangeLogSet;
import hudson.scm.SubversionChangeLogSet;
import hudson.scm.SubversionSCM;
import hudson.tasks.Mailer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by benmeior on 5/15/2016.
 */

@SuppressWarnings({"squid:S2221", "squid:S00105"})
public class SvnSCMProcessor implements SCMProcessor {
	private static final Logger logger = LogManager.getLogger(SvnSCMProcessor.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();
	private static final int PARENT_COMMIT_INDEX = 1;

	@Override
	public SCMData getSCMData(AbstractBuild build) {
		AbstractProject project = build.getProject();
		SCMData result = null;
		SubversionSCM svnData;
		SCMRepository scmRepository;
		ArrayList<SCMCommit> tmpCommits;
		ChangeLogSet<ChangeLogSet.Entry> changes = build.getChangeSet();
		String builtRevId;

		if (project.getScm() instanceof SubversionSCM) {
			svnData = (SubversionSCM) project.getScm();
			scmRepository = getSCMRepository(svnData);

			builtRevId = getBuiltRevId(build, svnData, scmRepository.getUrl());

			tmpCommits = buildScmCommits(changes);

			result = dtoFactory.newDTO(SCMData.class)
					.setRepository(scmRepository)
					.setBuiltRevId(builtRevId)
					.setCommits(tmpCommits);
		}
		return result;
	}

	@Override
	public List<SCMData> getSCMData(WorkflowRun run) {
		// todo: add implementatiohn  - yanivl
		return null;
	}

	@Override
	public CommonOriginRevision getCommonOriginRevision(Run run) {
		return null;
	}

	private ArrayList<SCMCommit> buildScmCommits(ChangeLogSet<ChangeLogSet.Entry> changes) {
		ArrayList<SCMCommit> tmpCommits;
		List<SCMChange> tmpChanges;
		SCMChange tmpChange;
		tmpCommits = new ArrayList<>();
		for (ChangeLogSet.Entry c : changes) {
			if (c instanceof SubversionChangeLogSet.LogEntry) {
				SubversionChangeLogSet.LogEntry commit = (SubversionChangeLogSet.LogEntry) c;
				User user = commit.getAuthor();
				String userEmail = null;

				tmpChanges = new ArrayList<>();
				for (SubversionChangeLogSet.Path item : commit.getAffectedFiles()) {
					tmpChange = dtoFactory.newDTO(SCMChange.class)
							.setType(item.getEditType().getName())
							.setFile(item.getPath());
					tmpChanges.add(tmpChange);
				}

				for (UserProperty property : user.getAllProperties()) {
					if (property instanceof Mailer.UserProperty) {
						userEmail = ((Mailer.UserProperty) property).getAddress();
					}
				}

				String parentRevId = getParentRevId(commit);

				SCMCommit tmpCommit = dtoFactory.newDTO(SCMCommit.class)
						.setTime(commit.getTimestamp())
						.setUser(commit.getAuthor().getId())
						.setUserEmail(userEmail)
						.setRevId(commit.getCommitId())
						.setParentRevId(parentRevId)
						.setComment(commit.getMsg().trim())
						.setChanges(tmpChanges);
				tmpCommits.add(tmpCommit);
			}
		}
		return tmpCommits;
	}

	private String getBuiltRevId(AbstractBuild build, SubversionSCM svnData, String scmRepositoryUrl) {
		String builtRevId = null;

		try {
			String revisionStateStr = String.valueOf(svnData.calcRevisionsFromBuild(build, null, null));
			builtRevId = getRevisionIdFromBuild(revisionStateStr, scmRepositoryUrl);
		} catch (IOException | InterruptedException e) {
			logger.error("failed to get revision state", e);
		}
		return builtRevId;
	}

	private static String getParentRevId(SubversionChangeLogSet.LogEntry commit) {
		String parentRevId = null;

		try {
			if (commit.getParent() != null && commit.getParent().getLogs() != null && commit.getParent().getLogs().size() > PARENT_COMMIT_INDEX) {
				parentRevId = commit.getParent().getLogs().get(PARENT_COMMIT_INDEX).getCommitId();
			} else {
				logger.info("parent rev ID is not available in the commit's logs");
			}
		} catch (Exception e) {
			logger.error("failed to retrieve parentRevId", e);
		}

		return parentRevId;
	}

	private static String getRevisionIdFromBuild(String revisionStateStr, String repositoryUrl) {
		Matcher m = Pattern.compile("(\\d+)").matcher(
				revisionStateStr.substring(revisionStateStr.indexOf(repositoryUrl) + repositoryUrl.length() + 1)
		);
		if (!m.find()) {
			return null;
		}
		return m.group(1);
	}

	private static SCMRepository getSCMRepository(SubversionSCM svnData) {
		SCMRepository result;
		String url = null;
		if (svnData.getLocations().length == 1) {
			url = svnData.getLocations()[0].getURL();
		}
		result = dtoFactory.newDTO(SCMRepository.class)
				.setType(SCMType.SVN)
				.setUrl(url);
		return result;
	}
}
