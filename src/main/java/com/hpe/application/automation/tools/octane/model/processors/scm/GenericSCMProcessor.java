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
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.scm.ChangeLogSet;
import hudson.tasks.Mailer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by benmeior on 9/8/2016.
 */

public class GenericSCMProcessor implements SCMProcessor {
	private static final Logger logger = LogManager.getLogger(GenericSCMProcessor.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	GenericSCMProcessor() {
	}

	@Override
	public SCMData getSCMData(AbstractBuild build) {
		SCMData result;
		SCMRepository repository = buildScmRepository();

		ChangeLogSet<ChangeLogSet.Entry> changes = build.getChangeSet();
		ArrayList<SCMCommit> tmpCommits = buildScmCommits(changes);

		result = dtoFactory.newDTO(SCMData.class)
				.setCommits(tmpCommits)
				.setRepository(repository);

		return result;
	}

	@Override
	public List<SCMData> getSCMData(WorkflowRun run) {
		// todo: implement default - yanivl
		return null;
	}

	@Override
	public CommonOriginRevision getCommonOriginRevision(Run run) {
		return null;
	}

	private ArrayList<SCMCommit> buildScmCommits(ChangeLogSet<ChangeLogSet.Entry> changes) {
		ArrayList<SCMCommit> tmpCommits = new ArrayList<>();
		ArrayList<SCMChange> tmpChanges;
		SCMChange tmpChange;

		for (ChangeLogSet.Entry c : changes) {
			User user = c.getAuthor();
			String userEmail = null;

			tmpChanges = new ArrayList<>();

			for (ChangeLogSet.AffectedFile item : c.getAffectedFiles()) {
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
			SCMCommit tmpCommit = buildScmCommit(tmpChanges, c, userEmail);
			tmpCommits.add(tmpCommit);
		}
		return tmpCommits;
	}

	private SCMCommit buildScmCommit(ArrayList<SCMChange> tmpChanges, ChangeLogSet.Entry commit, String userEmail) {
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
