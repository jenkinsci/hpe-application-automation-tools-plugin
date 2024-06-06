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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.scm.SCMChange;
import com.hp.octane.integrations.dto.scm.SCMCommit;
import com.hp.octane.integrations.dto.scm.SCMData;
import com.hp.octane.integrations.dto.scm.SCMRepository;
import com.hp.octane.integrations.dto.scm.SCMType;
import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;
import hudson.scm.SCM;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.Mailer;

class StarTeamSCMProcessor implements SCMProcessor {
  private static final Logger logger = SDKBasedLoggerProvider.getLogger(StarTeamSCMProcessor.class);
  private static final DTOFactory dtoFactory = DTOFactory.getInstance();

  @Override
  public SCMData getSCMData(AbstractBuild build, SCM scm) {
    List<ChangeLogSet<? extends ChangeLogSet.Entry>> changes = new ArrayList<>();
    changes.add(build.getChangeSet());
    return extractSCMData(build, scm, changes);
  }

  @Override
  public SCMData getSCMData(WorkflowRun run, SCM scm) {
    return extractSCMData(run, scm, run.getChangeSets());
  }

  @Override
  public CommonOriginRevision getCommonOriginRevision(final Run run) {
    return null;
  }

  private SCMData extractSCMData(Run run, SCM scm, List<ChangeLogSet<? extends ChangeLogSet.Entry>> changes) {
    if (!(scm.getClass().getName().equals("hudson.plugins.starteam.StarTeamSCM"))) {    
      throw new IllegalArgumentException("SCM type of StarTeamSCM was expected here, found '" + scm.getClass().getName() + "'");
    }

    SCMRepository repository = getRepository(scm);
    List<SCMCommit> commits = extractCommits(changes);

    return dtoFactory.newDTO(SCMData.class)
        .setRepository(repository)
        .setCommits(commits);
  }

  private SCMRepository getRepository(SCM starTeamSCM) {
    SCMRepository result = null;
    if (starTeamSCM != null) {
      try {
        String url = getSCMRepositoryURL(starTeamSCM);
        String branch = getSCMRepositoryBranch(starTeamSCM);
        result = dtoFactory.newDTO(SCMRepository.class).setType(SCMType.STARTEAM).setUrl(url).setBranch(branch);
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
        logger.warn(e.getClass().getSimpleName() + " unable to extract data from starTeamSCM, no SCM repository info will be available.", e);
      }
    } else {
      logger.warn("starTeamSCM is null, no SCM repository info will be available.");
    }
    return result;
  }
  
  private List<SCMCommit> extractCommits(List<ChangeLogSet<? extends ChangeLogSet.Entry>> changes) {
    LinkedList<SCMCommit> tmpCommits = new LinkedList<>();
  
    for (ChangeLogSet<? extends ChangeLogSet.Entry> set : changes) {
      for (ChangeLogSet.Entry change : set) {
        if (change.getClass().getName().equals("hudson.plugins.starteam.changelog.StarTeamChangeLogEntry")) {
          List<SCMChange> tmpChanges = new ArrayList<>();
          User user = change.getAuthor();
          String userEmail = null;
  
          SCMChange tmpChange;
          try {
            tmpChange = dtoFactory.newDTO(SCMChange.class)
                .setType(getCommitType(change))
                .setFile(getFileName(change));
            tmpChanges.add(tmpChange);
  
            for (UserProperty property : user.getAllProperties()) {
              if (property instanceof Mailer.UserProperty) {
                userEmail = ((Mailer.UserProperty) property).getAddress();
              }
            }
  
            SCMCommit tmpCommit = dtoFactory.newDTO(SCMCommit.class)
                .setTime(getDate(change).getTime())
                .setUserEmail(userEmail)
                .setUser(change.getAuthor().getId())
                .setRevId(getRevision(change))
                .setComment(getComment(change))
                .setChanges(tmpChanges);
  
            tmpCommits.add(tmpCommit);
          } catch (Exception e) {
            logger.warn("failed to obtain commit information", e);
          }
        }
      }
    }
    return tmpCommits;
  }

  private Object callMethodViaReflection(Object object, String methodName) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Class<?> cls = object.getClass();
    Method methodTocall = cls.getDeclaredMethod(methodName); 
    Object returnValue = methodTocall.invoke(object); 
    return returnValue;
  }

  private String getSCMRepositoryBranch(SCM starTeamSCM) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
    String branch;
    if (usingStarTeamURL(starTeamSCM))
      branch = "";
    else {
      branch = getViewName(starTeamSCM);
    }
    return branch;
  }

  private String getViewName(SCM starTeamSCM) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Object returnValue = callMethodViaReflection(starTeamSCM, "getViewName");
    return (String) returnValue;
  }

  private String getSCMRepositoryURL(SCM starTeamSCM) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    String url;
    if (usingStarTeamURL(starTeamSCM)) {
      Object returnValue = callMethodViaReflection(starTeamSCM, "getStFolderUrl"); 
      url = (String) returnValue;
    }
    else {
      String hostname = getHostName(starTeamSCM);
      String port = getPort(starTeamSCM);
      String projectName = getProjectNamer(starTeamSCM);
      url = hostname + ":" + port + "/" + projectName;
    }
    return url;
  }

  private String getProjectNamer(SCM starTeamSCM) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Object returnValue = callMethodViaReflection(starTeamSCM, ("getProjectName"));
    return (String) returnValue;
  }

  private String getPort(SCM starTeamSCM) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Object returnValue = callMethodViaReflection(starTeamSCM, ("getPort"));
    return (String) returnValue;
  }

  private String getHostName(SCM starTeamSCM) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Object returnValue = callMethodViaReflection(starTeamSCM, ("getHostName"));
    return (String) returnValue;
  }

  private boolean usingStarTeamURL(SCM starTeamSCM) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    Object returnValue = callMethodViaReflection(starTeamSCM, ("getStFolderUrl"));
    String getStFolderUrl = (String) returnValue;
    return getStFolderUrl != null && !getStFolderUrl.isEmpty();
  }

  private String getFileName(Entry change) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
    Object returnValue = callMethodViaReflection(change, ("getFileName"));
    return (String) returnValue;
  }

  private Date getDate(Entry change) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
    Object returnValue = callMethodViaReflection(change, ("getDate"));
    return (Date) returnValue;
  }

  private String getRevision(ChangeLogSet.Entry commit) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException {
    Integer viewMemberID = getViewMemberID(commit);
    Integer revisionNumber = getRevisionNumber(commit);
    String changeType = getChangeType(commit);
    String revID = "VMID: " + viewMemberID + " " + "Rev: " + revisionNumber;

    if (changeType.equals("DELETE")) {
      revID = revID + " (deleted)";
    }
    return revID;
  }

  private String getChangeType(ChangeLogSet.Entry commit) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException {
    Object changeType = callMethodViaReflection(commit, ("getChangeType"));
    return changeType.toString();
  }

  private Integer getRevisionNumber(ChangeLogSet.Entry commit) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Object returnValue = callMethodViaReflection(commit, ("getRevisionNumber"));
    return (Integer) returnValue;
  }

  private Integer getViewMemberID(ChangeLogSet.Entry commit) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Object returnValue = callMethodViaReflection(commit, ("getViewMemberId"));
    return (Integer) returnValue;
  }

  private String getComment(ChangeLogSet.Entry commit) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException {
    String changeType = getChangeType(commit);
    if (changeType.equals("DELETE")) {
      return "File deleted";
    } else
      return commit.getMsg();
  }

  private String getCommitType(ChangeLogSet.Entry commit) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException {
    String changeType = getChangeType(commit);
    if ("ADD".equals(changeType)) {
      return EditType.ADD.getName();
    } else if ("DELETE".equals(changeType)) {
      return EditType.DELETE.getName();
    } else if ("MODIFIED".equals(changeType)) {
      return EditType.EDIT.getName();
    }
    return null;
  }

}
