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

package com.microfocus.application.automation.tools.octane.model.processors.scm;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.scm.*;
import com.hp.octane.integrations.dto.scm.impl.LineRange;
import com.hp.octane.integrations.dto.scm.impl.RevisionsMap;
import com.hp.octane.integrations.dto.scm.impl.SCMFileBlameImpl;
import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import hudson.FilePath;
import hudson.model.*;
import hudson.plugins.git.Branch;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitChangeSet;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.extensions.GitSCMExtension;
import hudson.plugins.git.extensions.GitSCMExtensionDescriptor;
import hudson.plugins.git.extensions.impl.RelativeTargetDirectory;
import hudson.plugins.git.util.BuildData;
import hudson.remoting.VirtualChannel;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCM;
import hudson.tasks.Mailer;
import hudson.util.DescribableList;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.*;
import org.eclipse.jgit.errors.NoMergeBaseException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

/**
 * Created by gullery on 31/03/2015.
 */

class GitSCMProcessor implements SCMProcessor {
	private static final Logger logger = SDKBasedLoggerProvider.getLogger(GitSCMProcessor.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();
	private static final String MASTER = "refs/remotes/origin/master";

	@Override
	public SCMData getSCMData(AbstractBuild build, SCM scm) {
		List<ChangeLogSet<? extends ChangeLogSet.Entry>> changes = new ArrayList<>();
		changes.add(build.getChangeSet());
		SCMData scmData = extractSCMData(build, scm, changes);
		scmData = enrichLinesOnSCMData(scmData, build);
		return scmData;
	}

	/**
	 * this method go over each of the changed files and enrich line changes
	 * into existing scm events, so that the new enriched events will have line ranges.
	 * in addition, for each renamed file, we enrich inside delete event the 'renamed to' file
	 *
	 * @param scmData SCM data as an input
	 * @param build   build context
	 */
	private SCMData enrichLinesOnSCMData(SCMData scmData, AbstractBuild build) {
		long startTime = System.currentTimeMillis();
		try {
			FilePath workspace = build.getWorkspace();
			if (workspace != null) {
				scmData = workspace.act(new LineEnricherCallable(getCheckoutDir(build), scmData));
				logger.debug("Line enricher: process took: " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds");
			} else {
				logger.warn("Line enricher: workspace is null");
			}
		} catch (Exception e1) {
			logger.error("Line enricher: FAILED. could not enrich lines on SCM Data : " + e1.getMessage());
		}
		return scmData;
	}

	@Override
	public SCMData getSCMData(WorkflowRun run, SCM scm) {
		return extractSCMData(run, scm, run.getChangeSets());
	}

	@Override
	public CommonOriginRevision getCommonOriginRevision(final Run run) {
		//for phase 1 this is hard coded since its not possible to calculate it, and configuration from outside will complicate the feature
		//so for this phase we keep it hardcoded.
		CommonOriginRevision commonOriginRevision = new CommonOriginRevision();
		commonOriginRevision.branch = getBranchName(run);

		try {
			final AbstractBuild abstractBuild = (AbstractBuild) run;
			FilePath workspace = ((AbstractBuild) run).getWorkspace();
			if (workspace != null) {
				commonOriginRevision.revision = workspace.act(new FileContentCallable(getCheckoutDir(abstractBuild)));

			}
			logger.debug("most recent common revision resolved to " + commonOriginRevision.revision + " (branch: " + commonOriginRevision.branch + ")");
		} catch (Exception e) {
			logger.error("failed to resolve most recent common revision : " + e.getClass().getName() + " - " + e.getMessage());
			return commonOriginRevision;
		}
		return commonOriginRevision;
	}

	private SCMData extractSCMData(Run run, SCM scm, List<ChangeLogSet<? extends ChangeLogSet.Entry>> changes) {
		if (!(scm instanceof GitSCM)) {
			throw new IllegalArgumentException("GitSCM type of SCM was expected here, found '" + scm.getClass().getName() + "'");
		}

		GitSCM gitData = (GitSCM) scm;
		SCMRepository repository;
		List<SCMCommit> tmpCommits;
		String builtRevId = null;

		repository = getRepository(run, gitData);

		BuildData buildData = gitData.getBuildData(run);
		if (buildData != null && buildData.getLastBuiltRevision() != null) {
			builtRevId = buildData.getLastBuiltRevision().getSha1String();
		}

		tmpCommits = extractCommits(changes);
		return dtoFactory.newDTO(SCMData.class)
				.setRepository(repository)
				.setBuiltRevId(builtRevId)
				.setCommits(tmpCommits);
	}

	private String getBranchName(Run r) {
		try {
			SCM scm = ((AbstractBuild) r).getProject().getScm();
			GitSCM git = (GitSCM) scm;
			List<BranchSpec> branches = git.getBranches();
			String rawBranchName = branches.get(0).toString();
			if (rawBranchName != null && rawBranchName.startsWith("${") && rawBranchName.endsWith("}")) {
				String param = rawBranchName.substring(2, rawBranchName.length() - 1);
				if (((AbstractBuild) r).getBuildVariables().get(param) != null) {
					return ((AbstractBuild) r).getBuildVariables().get(param).toString();
				} else {
					return param;
				}
			}
			if (rawBranchName != null && rawBranchName.startsWith("*/")) {
				return rawBranchName.substring(2);
			}
			return rawBranchName; //trunk the '*/' from the '*/<branch name>' in order to get clean branch name
		} catch (Exception e) {
			logger.error("failed to extract branch name", e);
		}
		return null;
	}

    private static String getCheckoutDir(AbstractBuild r) {
        final DescribableList<GitSCMExtension, GitSCMExtensionDescriptor> extensions = ((GitSCM) (r.getProject()).getScm()).getExtensions();
        if (extensions != null) {
            final RelativeTargetDirectory relativeTargetDirectory = extensions.get(RelativeTargetDirectory.class);
            if (relativeTargetDirectory != null && relativeTargetDirectory.getRelativeTargetDir() != null) {
                return relativeTargetDirectory.getRelativeTargetDir();
            }
        }
        return "";
	}

	private SCMRepository getRepository(Run run, GitSCM gitData) {
		SCMRepository result = null;
		String url = null;
		String branch = null;
		if (gitData != null && gitData.getBuildData(run) != null) {
			BuildData buildData = gitData.getBuildData(run);
			if (buildData != null) {
				if (buildData.getRemoteUrls() != null && !buildData.getRemoteUrls().isEmpty()) {
					url = (String) buildData.getRemoteUrls().toArray()[0];
				}
				if (buildData.getLastBuiltRevision() != null && !buildData.getLastBuiltRevision().getBranches().isEmpty()) {
					branch = ((Branch) buildData.getLastBuiltRevision().getBranches().toArray()[0]).getName();
				}
				result = dtoFactory.newDTO(SCMRepository.class)
						.setType(SCMType.GIT)
						.setUrl(url)
						.setBranch(branch);
			} else {
				logger.warn("failed to obtain BuildData; no SCM repository info will be available");
			}
		}
		return result;
	}

	private List<SCMCommit> extractCommits(List<ChangeLogSet<? extends ChangeLogSet.Entry>> changes) {
		List<SCMCommit> commits = new LinkedList<>();
		for (ChangeLogSet<? extends ChangeLogSet.Entry> set : changes) {
			for (ChangeLogSet.Entry change : set) {
				if (change instanceof GitChangeSet) {
					GitChangeSet commit = (GitChangeSet) change;
					List<SCMChange> tmpChanges = new ArrayList<>();
					for (GitChangeSet.Path item : commit.getAffectedFiles()) {
						SCMChange tmpChange = dtoFactory.newDTO(SCMChange.class)
								.setType(item.getEditType().getName())
								.setFile(item.getPath());
						tmpChanges.add(tmpChange);
					}

					SCMCommit tmpCommit = dtoFactory.newDTO(SCMCommit.class)
							.setTime(commit.getTimestamp())
							.setRevId(commit.getCommitId())
							.setParentRevId(commit.getParentCommit())
							.setComment(commit.getComment().trim())
							.setChanges(tmpChanges);

					setUserInCommit(commit,tmpCommit);
					commits.add(tmpCommit);
				}
			}
		}
		return commits;
	}

	private void setUserInCommit(GitChangeSet commit, SCMCommit dtoCommit) {
		User user = commit.getAuthor();
		String userName = user.getId();
		String userEmail = null;
		for (UserProperty property : user.getAllProperties()) {
			if (property instanceof Mailer.UserProperty) {
				userEmail = ((Mailer.UserProperty) property).getAddress();
			}
		}

		try {
			//commits in github UI - returns with user "noreply"
			if ("noreply".equals(userName)) {
				String authorEmail = (String) FieldUtils.readField(commit, "authorEmail", true);
				if (StringUtils.isNotEmpty(authorEmail) && authorEmail.contains("@")) {
					userEmail = authorEmail;
					userName = authorEmail.substring(0, authorEmail.indexOf('@'));
				}
			}
		} catch (Exception e) {
			logger.info("Failed to extract authorEmail : " + e.getMessage());
		}

		dtoCommit
				.setUser(userName)
				.setUserEmail(userEmail);
	}

	private static final class FileContentCallable extends MasterToSlaveFileCallable<String> {
		private final String checkoutDir;

		private FileContentCallable(String checkoutDir) {
			this.checkoutDir = checkoutDir;
		}

		@Override
		public String invoke(File rootDir, VirtualChannel channel) throws IOException {
			File repoDir = new File(rootDir, checkoutDir + File.separator + ".git");
			try (Git git = Git.open(repoDir);
			     Repository repo = git.getRepository()) {
				if (repo == null) {
					return "";
				}

				try (RevWalk walk = new RevWalk(repo)) {
					ObjectId resolveForCurrentBranch = repo.resolve(Constants.HEAD);
					if (resolveForCurrentBranch == null) {
						return "";
					}

					RevCommit currentBranchCommit = walk.parseCommit(resolveForCurrentBranch);
					if (currentBranchCommit == null) {
						return "";
					}

					ObjectId resolveForMaster = repo.resolve(MASTER);
					if (resolveForMaster == null) {
						return "";
					}

					RevCommit masterCommit = walk.parseCommit(resolveForMaster);
					walk.reset();
					walk.setRevFilter(RevFilter.MERGE_BASE);
					walk.markStart(currentBranchCommit);
					walk.markStart(masterCommit);
					RevCommit base = walk.next();
					if (base == null) {
						return "";
					}
					final RevCommit base2 = walk.next();
					if (base2 != null) {
						throw new NoMergeBaseException(NoMergeBaseException.MergeBaseFailureReason.MULTIPLE_MERGE_BASES_NOT_SUPPORTED,
								MessageFormat.format(JGitText.get().multipleMergeBasesFor, currentBranchCommit.name(), masterCommit.name(), base.name(), base2.name()));
					}

					//in order to return actual revision and not merge commit
					while (base.getParents().length > 1) {
						RevCommit base_1 = base.getParent(0);
						RevCommit base_2 = base.getParent(1);
						if (base_1.getParents().length == 1) {
							base = base_1;
						} else {
							base = base_2;
						}
					}
					return base.getId().getName();
				}
			}
		}
	}

	/*line enricher running on the same jenkins node that the job is running in it*/
	private static final class LineEnricherCallable extends MasterToSlaveFileCallable<SCMData> {
		private final String checkoutDir;
		private final SCMData scmData;

		private LineEnricherCallable(String checkoutDir, SCMData scmData) {
			this.checkoutDir = checkoutDir;
			this.scmData = scmData;
		}

		@Override
		public SCMData invoke(File rootDir, VirtualChannel channel) throws IOException {
			File repoDir = new File(rootDir, checkoutDir + File.separator + ".git");
			try (Git git = Git.open(repoDir);
			     Repository repo = git.getRepository()) {
				if (repo == null) {
					return null;
				}

				try (RevWalk rw = new RevWalk(repo);
				     DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
					df.setDiffComparator(RawTextComparator.DEFAULT);
					df.setRepository(repo);
					df.setDetectRenames(true);

					//add blame data to scm data
					Set<String> committedFiles = getAddedOrEditedFiles(scmData);
					List<SCMFileBlame> fileBlameList = getBlameData(repo, committedFiles);
					scmData.setFileBlameList(fileBlameList);

					for (SCMCommit curCommit : scmData.getCommits()) {
						Map<String, SCMChange> fileChanges = new HashMap<>();
						curCommit.getChanges().forEach(change -> fileChanges.put(change.getFile(), change));
						RevCommit commit = rw.parseCommit(repo.resolve(curCommit.getRevId())); // Any ref will work here (HEAD, a sha1, tag, branch)
						RevCommit parent = rw.parseCommit(commit.getParent(0).getId());

						List<DiffEntry> diffs = df.scan(parent.getTree(), commit.getTree());
						// FOR EACH FILE
						for (DiffEntry diff : diffs) { // each file change will be in seperate diff
							EditList fileEdits = df.toFileHeader(diff).toEditList();
							switch (diff.getChangeType()) {
								case ADD:
									// old path == null, need to use new path
									handleAddLinesDiff(fileEdits, fileChanges.get(diff.getNewPath()));
									break;
								case COPY:
									// need to validate this type
									handleModifyDiff(fileEdits, fileChanges.get(diff.getNewPath()));
									break;
								case DELETE:
									// new path == null, need to use old path
									handleDeleteLinesDiff(fileEdits, fileChanges.get(diff.getOldPath()));
									break;
								case MODIFY:
									handleModifyDiff(fileEdits, fileChanges.get(diff.getNewPath()));
									break;
								case RENAME:
									// enrich delete event with 'rename to' data
									SCMChange deletedChange = fileChanges.get(diff.getOldPath());
									SCMChange newRenamedFile = fileChanges.get(diff.getNewPath());
									deletedChange.setRenamedToFile(newRenamedFile.getFile());
									// handle changes
									handleModifyDiff(fileEdits, fileChanges.get(diff.getNewPath()));
									break;
								default:
									break;
							}
						}
					}
					return scmData;
				}
			}
		}
	}

	private static Set<String> getAddedOrEditedFiles(SCMData scmData) {
		Set<String> filesCommittedInPPR = new HashSet<>();
		for (SCMCommit curCommit : scmData.getCommits()) {
			curCommit.getChanges().stream().filter(change -> !change.getType().equals("delete")).forEach(change -> filesCommittedInPPR.add(change.getFile()));
		}
		return filesCommittedInPPR;
	}

	private static List<SCMFileBlame> getBlameData(Repository repo, Set<String> files) {
		BlameCommand blamer = new BlameCommand(repo);
		List<SCMFileBlame> fileBlameList = new ArrayList<>();
		ObjectId commitID;
		try {
			commitID = repo.resolve(Constants.HEAD);
			for (String filePath : files) {
				blamer.setStartCommit(commitID);
				blamer.setFilePath(filePath);
				BlameResult blameResult = blamer.call();
				if (blameResult == null) {
					continue;
				}
				RawText rawText = blameResult.getResultContents();
				int fileSize = rawText.size();

				RevisionsMap revisionsMap = new RevisionsMap();

				if (fileSize > 0) {
					String startRangeRevision = blameResult.getSourceCommit(0).getName();
					int startRange = 1;
					for (int i = 1; i < fileSize; i++) {
						String currentRevision = blameResult.getSourceCommit(i).getName();
						if (!currentRevision.equals(startRangeRevision)) {
							LineRange range = new LineRange(startRange, i);//line numbers starting from 1 not from 0.
							revisionsMap.addRangeToRevision(startRangeRevision, range);
							startRange = i + 1;
							startRangeRevision = currentRevision;
						}
					}
				}
				fileBlameList.add(new SCMFileBlameImpl(filePath, revisionsMap));
			}
		} catch (IOException e) {
			logger.error("failed to resolve repo head", e);
		} catch (GitAPIException e) {
			logger.error("failed to get blame result from git", e);
		}
		return fileBlameList;
	}

	private static void handleModifyDiff(EditList fileEdits, SCMChange scmChange) {
		if (scmChange != null) {
			for (Edit edit : fileEdits) {
				switch (edit.getType()) {
					case INSERT:
						scmChange.insertAddedLines(new LineRange(edit.getBeginB() + 1, edit.getEndB()));
						break;
					case DELETE:
						scmChange.insertDeletedLines(new LineRange(edit.getBeginA() + 1, edit.getEndA()));
						break;
					case REPLACE:
						scmChange.insertDeletedLines(new LineRange(edit.getBeginA() + 1, edit.getEndA()));
						scmChange.insertAddedLines(new LineRange(edit.getBeginB() + 1, edit.getEndB()));
						break;
					default:
						break;
				}
			}
		}
	}

	// probably it's useless to track deleted lines (inside scm change), consider removing it later.
	private static void handleDeleteLinesDiff(EditList fileEdits, SCMChange scmChange) {
		if (scmChange != null) {
			for (Edit edit : fileEdits) {
				scmChange.insertDeletedLines(new LineRange(edit.getBeginA() + 1, edit.getEndA()));
			}
		}
	}

	private static void handleAddLinesDiff(EditList fileEdits, SCMChange scmChange) {
		if (scmChange != null) {
			for (Edit edit : fileEdits) {
				scmChange.insertAddedLines(new LineRange(edit.getBeginB() + 1, edit.getEndB()));
			}
		}
	}
}
