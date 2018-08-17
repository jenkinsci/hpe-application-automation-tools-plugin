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

package com.microfocus.application.automation.tools.octane.model.processors.scm;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.scm.SCMChange;
import com.hp.octane.integrations.dto.scm.SCMCommit;
import com.hp.octane.integrations.dto.scm.SCMData;
import com.hp.octane.integrations.dto.scm.SCMRepository;
import com.hp.octane.integrations.dto.scm.SCMType;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCM;
import hudson.tasks.Mailer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by benmeior on 9/8/2016.
 */

class GenericSCMProcessor implements SCMProcessor {
	private static final Logger logger = LogManager.getLogger(GenericSCMProcessor.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	@Override
	public SCMData getSCMData(AbstractBuild build, SCM scm) {
		List<ChangeLogSet<? extends ChangeLogSet.Entry>> changes = new ArrayList<>();
		changes.add(build.getChangeSet());
		return extractSCMData(scm, changes);
	}

	@Override
	public SCMData getSCMData(WorkflowRun run, SCM scm) {
		return extractSCMData(scm, run.getChangeSets());
	}

	@Override
	public CommonOriginRevision getCommonOriginRevision(Run run) {
		return null;
	}

	private SCMData extractSCMData(SCM scm, List<ChangeLogSet<? extends ChangeLogSet.Entry>> changes) {
		logger.info("building generic scm data for SCM of type " + scm.getType());

		SCMRepository repository = buildScmRepository();
		List<SCMCommit> tmpCommits = extractCommits(changes);

		return dtoFactory.newDTO(SCMData.class)
				.setRepository(repository)
				.setCommits(tmpCommits);
	}

	private List<SCMCommit> extractCommits(List<ChangeLogSet<? extends ChangeLogSet.Entry>> changes) {
		List<SCMCommit> tmpCommits = new ArrayList<>();
		List<SCMChange> tmpChanges;
		SCMChange tmpChange;

		for (ChangeLogSet<? extends ChangeLogSet.Entry> set : changes) {
			for (ChangeLogSet.Entry change : set) {
				User user = change.getAuthor();
				String userEmail = null;

				tmpChanges = new ArrayList<>();

				for (ChangeLogSet.AffectedFile item : change.getAffectedFiles()) {
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
				SCMCommit tmpCommit = buildScmCommit(tmpChanges, change, userEmail);
				tmpCommits.add(tmpCommit);
			}
		}
		return tmpCommits;
	}

	private SCMCommit buildScmCommit(List<SCMChange> tmpChanges, ChangeLogSet.Entry commit, String userEmail) {
		return dtoFactory.newDTO(SCMCommit.class)
				.setTime(commit.getTimestamp())
				.setUser(commit.getAuthor().getId())
				.setUserEmail(userEmail)
				.setRevId(commit.getCommitId())
				.setComment(commit.getMsg().trim())
				.setChanges(tmpChanges);
	}

	private SCMRepository buildScmRepository() {
		return dtoFactory.newDTO(SCMRepository.class)
				.setType(SCMType.UNKNOWN);
	}
}
