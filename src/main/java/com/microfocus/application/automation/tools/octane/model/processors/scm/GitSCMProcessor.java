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
 *  Copyright 2012-2026 Open Text.
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
package com.microfocus.application.automation.tools.octane.model.processors.scm;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.scm.*;
import com.hp.octane.integrations.dto.scm.impl.LineRange;
import com.hp.octane.integrations.dto.scm.impl.RevisionsMap;
import com.hp.octane.integrations.dto.scm.impl.SCMFileBlameImpl;
import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import com.microfocus.application.automation.tools.octane.model.processors.projects.JobProcessorFactory;
import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
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
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.*;

/**
 * Created by gullery on 31/03/2015.
 */

class GitSCMProcessor implements SCMProcessor {
	private static final Logger logger = SDKBasedLoggerProvider.getLogger(GitSCMProcessor.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();
	private static final String DEFAULT_REMOTE_BRANCH = "refs/remotes/origin/master";

	// Reflection method names used for SCM extraction
	private static final String METHOD_GET_DEFINITION = "getDefinition";
	private static final String METHOD_GET_SCM = "getScm";
	private static final String METHOD_GET_SCMS = "getSCMs";

	// Git ref path constants
	private static final String REFS_REMOTES_ORIGIN_PREFIX = "refs/remotes/origin/";

	// Parameter name for user-specified default branch (used for merge-base calculation)
	private static final String DEFAULT_BRANCH_PARAMETER = "DEFAULT_PROJECT_BRANCH";

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

	/**
	 * Calculates the common origin revision (merge-base) between the current branch and the default remote branch.
	 *
	 * @param run the Jenkins run (supports both AbstractBuild and WorkflowRun)
	 * @return CommonOriginRevision containing the branch name and common hash ID
	 */
	@Override
	public CommonOriginRevision getCommonOriginRevision(final Run run) {
		CommonOriginRevision commonOriginRevision = new CommonOriginRevision();
		commonOriginRevision.branch = getBranchName(run);

		try {
			FilePath workspace = getWorkspaceForRun(run);

			if (workspace != null) {
				String checkoutDirValue = getCheckoutDirForRun(run);
				String defaultBranch = extractDefaultBranchParameter(run);
				commonOriginRevision.revision = workspace.act(new FileContentCallable(checkoutDirValue, defaultBranch));
				logger.debug("most recent common revision resolved to {} (branch: {})", commonOriginRevision.revision, commonOriginRevision.branch);
			} else {
				logger.warn("Workspace is null for run {}, cannot calculate common origin revision", run.getFullDisplayName());
			}
		} catch (Exception e) {
			logger.error("failed to resolve most recent common revision : " + e.getClass().getName() + " - " + e.getMessage());
		}
		return commonOriginRevision;
	}

	/**
	 * Gets the workspace for a given run, handling both AbstractBuild and WorkflowRun types.
	 */
	private FilePath getWorkspaceForRun(Run run) {
		if (run instanceof AbstractBuild abstractBuild) {
			return abstractBuild.getWorkspace();
		} else if (run instanceof WorkflowRun workflowRun) {
			return BuildHandlerUtils.getWorkspace(workflowRun);
		}
		return null;
	}

	/**
	 * Gets the checkout directory for a given run, handling both AbstractBuild and WorkflowRun types.
	 */
	private String getCheckoutDirForRun(Run run) {
		String checkoutDir = StringUtils.EMPTY;

		if (run instanceof AbstractBuild abstractBuild) {
			checkoutDir = getCheckoutDir(abstractBuild);
			logger.debug("AbstractBuild checkoutDir: '{}'", checkoutDir);
		} else if (run instanceof WorkflowRun workflowRun) {
			SCM scm = extractScmFromRun(run);
			checkoutDir = scm != null ? getCheckoutDirForWorkflowRun(workflowRun, scm) : StringUtils.EMPTY;
			logger.debug("WorkflowRun checkoutDir: '{}', SCM: {}", checkoutDir, scm != null ? scm.getClass().getSimpleName() : "null");
		}

		return checkoutDir;
	}

	private SCMData extractSCMData(Run run, SCM scm, List<ChangeLogSet<? extends ChangeLogSet.Entry>> changes) {
		if (!(scm instanceof GitSCM gitData)) {
			throw new IllegalArgumentException("GitSCM type of SCM was expected here, found '" + scm.getClass().getName() + "'");
		}

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

	/**
	 * Extracts the branch name from the run's SCM configuration.
	 *
	 * @param r the Jenkins run
	 * @return the branch name, or null if unable to extract
	 */
	private String getBranchName(Run r) {
		try {
			SCM scm = extractScmFromRun(r);

			if (scm instanceof GitSCM git) {
				return extractBranchFromGitSCM(git, r);
			}
		} catch (Exception e) {
			logger.error("failed to extract branch name", e);
		}
		return null;
	}

	/**
	 * Extracts the user-specified default branch parameter from the run.
	 * This branch is used as the base for merge-base calculation (common ancestor).
	 * Works for both AbstractBuild and WorkflowRun.
	 *
	 * @param run the Jenkins run
	 * @return the default branch name, or null if not specified
	 */
	private String extractDefaultBranchParameter(Run run) {
		try {
			ParametersAction parameterAction = run.getAction(ParametersAction.class);
			if (parameterAction != null) {
				ParameterValue pv = parameterAction.getParameter(DEFAULT_BRANCH_PARAMETER);
				if (pv != null && pv.getValue() instanceof String branch) {
					if (StringUtils.isNotBlank(branch)) {
						logger.debug("Found {} parameter with value: {}", DEFAULT_BRANCH_PARAMETER, branch);
						return branch;
					}
				}
			}
		} catch (Exception e) {
			logger.debug("Failed to extract {} parameter: {}", DEFAULT_BRANCH_PARAMETER, e.getMessage());
		}
		return null;
	}

	/**
	 * Extracts SCM from a Run object, supporting both AbstractBuild and WorkflowRun.
	 *
	 * @param run the Jenkins run
	 * @return the SCM object, or null if not found
	 */
	private SCM extractScmFromRun(Run run) {
		if (run instanceof AbstractBuild abstractBuild) {
			return abstractBuild.getProject().getScm();
		} else if (run instanceof WorkflowRun workflowRun) {
			return extractScmFromWorkflowRun(workflowRun);
		}
		return null;
	}

	/**
	 * Extracts SCM from a WorkflowRun using reflection, trying multiple strategies.
	 *
	 * @param workflowRun the WorkflowRun instance
	 * @return the SCM object if found, null otherwise
	 */
	private SCM extractScmFromWorkflowRun(WorkflowRun workflowRun) {
		Object jobParent = workflowRun.getParent();

		SCM scm = tryExtractScmViaDefinition(jobParent);
		if (scm != null) {
			return scm;
		}

		scm = tryExtractScmViaScmsCollection(jobParent);
		if (scm != null) {
			return scm;
		}

		if (!JobProcessorFactory.WORKFLOW_MULTI_BRANCH_JOB_NAME.equals(jobParent.getClass().getName())) {
			scm = tryExtractScmViaDirectGetScm(jobParent);
		}

		return scm;
	}

	/**
	 * Attempts to extract SCM via job definition (getDefinition().getScm()).
	 *
	 * @param jobParent the job parent object
	 * @return the SCM object if found, null otherwise
	 */
	private SCM tryExtractScmViaDefinition(Object jobParent) {
		try {
			Method getDefinitionMethod = jobParent.getClass().getMethod(METHOD_GET_DEFINITION);
			Object definition = getDefinitionMethod.invoke(jobParent);
			if (definition == null) {
				return null;
			}

			Method getSCMMethod = definition.getClass().getMethod(METHOD_GET_SCM);
			Object scmObj = getSCMMethod.invoke(definition);
			if (scmObj instanceof SCM scm) {
				return scm;
			}
		} catch (ReflectiveOperationException e) {
			logger.debug("Could not extract SCM from WorkflowRun definition: {}", e.getMessage());
		}
		return null;
	}

	/**
	 * Attempts to extract SCM from SCMs collection (getSCMs()).
	 *
	 * @param jobParent the job parent object
	 * @return the first SCM object found in the collection, null otherwise
	 */
	private SCM tryExtractScmViaScmsCollection(Object jobParent) {
		try {
			Method getSCMsMethod = jobParent.getClass().getMethod(METHOD_GET_SCMS);
			Object scmsObj = getSCMsMethod.invoke(jobParent);
			if (scmsObj instanceof Collection<?> scmCollection) {
				for (Object scmObj : scmCollection) {
					if (scmObj instanceof SCM scm) {
						return scm;
					}
				}
			}
		} catch (ReflectiveOperationException e) {
			logger.debug("Could not extract SCM collection from WorkflowRun parent: {}", e.getMessage());
		}
		return null;
	}

	/**
	 * Attempts to extract SCM directly via getScm() method.
	 *
	 * @param jobParent the job parent object
	 * @return the SCM object if found, null otherwise
	 */
	private SCM tryExtractScmViaDirectGetScm(Object jobParent) {
		try {
			Method getSCMMethod = jobParent.getClass().getMethod(METHOD_GET_SCM);
			Object scmObj = getSCMMethod.invoke(jobParent);
			if (scmObj instanceof SCM scm) {
				return scm;
			}
		} catch (ReflectiveOperationException e) {
			logger.debug("Could not extract SCM using direct getScm fallback: {}", e.getMessage());
		}
		return null;
	}

	/**
	 * Extracts the branch name from a GitSCM configuration, resolving parameters if needed.
	 */
	private String extractBranchFromGitSCM(GitSCM git, Run run) {
		List<BranchSpec> branches = git.getBranches();
		if (branches == null || branches.isEmpty()) {
			return null;
		}

		String rawBranchName = branches.stream().findFirst().toString();

        // Handle parameterized branch names like ${BRANCH_NAME}
		if (rawBranchName.startsWith("${") && rawBranchName.endsWith("}")) {
			String param = rawBranchName.substring(2, rawBranchName.length() - 1);
			if (run instanceof AbstractBuild runObj) {
				Object value = runObj.getBuildVariables().get(param);
				return value != null ? value.toString() : param;
			}
			return param;
		}

		// Remove '*/' prefix from branch names like '*/master'
		if (rawBranchName.startsWith("*/")) {
			return rawBranchName.substring(2);
		}

		return rawBranchName;
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

	/**
	 * Extracts the checkout directory for WorkflowRun (Pipeline jobs).
	 * For Pipeline jobs, we need to get the SCM from the job definition.
	 *
	 * @param run the WorkflowRun instance
	 * @param scm the SCM configuration
	 * @return the checkout directory path, or empty string if not configured
	 */
	private static String getCheckoutDirForWorkflowRun(WorkflowRun run, SCM scm) {
		try {
			if (scm instanceof GitSCM gitSCM) {
				DescribableList<GitSCMExtension, GitSCMExtensionDescriptor> extensions = gitSCM.getExtensions();
				if (extensions != null) {
					RelativeTargetDirectory relativeTargetDirectory = extensions.get(RelativeTargetDirectory.class);
					if (relativeTargetDirectory != null && relativeTargetDirectory.getRelativeTargetDir() != null) {
						String checkoutDir = relativeTargetDirectory.getRelativeTargetDir();
						logger.debug("Checkout directory found for WorkflowRun: {}", checkoutDir);
						return checkoutDir;
					}
				}
			}
		} catch (Exception e) {
			logger.debug("Could not extract checkout directory for WorkflowRun: {}", e.getMessage());
		}
		return StringUtils.EMPTY;
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
		private final String userDefaultBranch;

		private FileContentCallable(String checkoutDir, String userDefaultBranch) {
			this.checkoutDir = checkoutDir;
			this.userDefaultBranch = userDefaultBranch;
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

					// Try to find the default remote branch
					String defaultRemoteBranch = findDefaultRemoteBranch(repo);
					if (defaultRemoteBranch == null) {
						return "";
					}

					ObjectId resolveForMaster = repo.resolve(defaultRemoteBranch);
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

		/**
		 * Tries to find the default remote branch in the repository.
		 * Strategy:
		 * 0. User-specified default branch parameter (DEFAULT_PROJECT_BRANCH job parameter)
		 * 1. Fallback to refs/remotes/origin/master
		 *
		 * @param repo the Git repository
		 * @return the ref name of the default remote branch, or null if not found
		 */
		private String findDefaultRemoteBranch(Repository repo) {
			// Strategy 0: User-specified default branch parameter (HIGHEST PRIORITY)
			if (StringUtils.isNotEmpty(userDefaultBranch)) {
				String resolvedBranch = resolveUserDefaultBranch(repo, userDefaultBranch);
				if (resolvedBranch != null) {
					return resolvedBranch;
				}
				logger.warn("User-specified default branch '{}' not found, falling back to master", userDefaultBranch);
			}

			// Strategy 1: Fallback to master
			if (refExists(repo, DEFAULT_REMOTE_BRANCH)) {
				logger.debug("Using default remote branch for merge-base: {}", DEFAULT_REMOTE_BRANCH);
				return DEFAULT_REMOTE_BRANCH;
			}

			return null;
		}

		/**
		 * Resolves user-specified default branch to a full Git ref.
		 *
		 * @param repo the Git repository
		 * @param userBranch the user-specified branch name
		 * @return the resolved full ref name, or null if not found
		 */
		private String resolveUserDefaultBranch(Repository repo, String userBranch) {
			// Try as-is first
			if (refExists(repo, userBranch)) {
				logger.debug("Using user-specified default branch (exact match): {}", userBranch);
				return userBranch;
			}

			// Only try with prefix if it's a plain branch name (not already a full ref path)
			if (!userBranch.startsWith("refs/")) {
				String withPrefix = REFS_REMOTES_ORIGIN_PREFIX + userBranch;
				if (refExists(repo, withPrefix)) {
					logger.debug("Using user-specified default branch (resolved to): {}", withPrefix);
					return withPrefix;
				}
			}

			return null;
		}

		private boolean refExists(Repository repo, String refName) {
			try {
				return repo.resolve(refName) != null;
			} catch (Exception e) {
				logger.debug("Failed to resolve ref {}: {}", refName, e.getMessage());
			}
			return false;
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
