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

package com.microfocus.application.automation.tools.octane.executor;

import com.hp.octane.integrations.dto.executor.impl.TestingToolType;
import com.hp.octane.integrations.dto.scm.SCMType;
import com.hp.octane.integrations.uft.UftTestDiscoveryUtils;
import com.hp.octane.integrations.uft.items.UftTestDiscoveryResult;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.remoting.VirtualChannel;
import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.lang.reflect.FieldUtils;
import org.jenkinsci.remoting.RoleChecker;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * used to start UFTTestDetectionService.startScanning on slave machine
 */
public class UFTTestDetectionCallable extends MasterToSlaveFileCallable<UftTestDiscoveryResult> {
    private String configurationId;
    private String workspaceId;
    private String scmRepositoryId;
    private BuildListener buildListener;
    private TestingToolType testingToolType;
    private String testRunnerId;
    private boolean fullScan = false;
    private ScmChangesWrapper scmChangesWrapper;

    public UFTTestDetectionCallable(AbstractBuild<?, ?> build, String configurationId, String workspaceId, String scmRepositoryId, BuildListener buildListener, TestingToolType testingToolType) {
        this.configurationId = configurationId;
        this.workspaceId = workspaceId;
        this.scmRepositoryId = scmRepositoryId;
        this.buildListener = buildListener;
        this.testingToolType = testingToolType == null || TestingToolType.UNKNOWN.equals(testingToolType) ? TestingToolType.UFT : testingToolType;

        extractParameterValues(build);
        wrapScmChanges(build);

    }

    private void extractParameterValues(AbstractBuild<?, ?> build) {
        ParametersAction parameterAction = build.getAction(ParametersAction.class);
        if (parameterAction != null) {
            ParameterValue testRunnerParameter = parameterAction.getParameter(UftConstants.TEST_RUNNER_ID_PARAMETER_NAME);
            if (testRunnerParameter != null && testRunnerParameter.getValue() instanceof String) {
                testRunnerId = ((String) testRunnerParameter.getValue());
            }


            ParameterValue parameterValue = parameterAction.getParameter(UftConstants.FULL_SCAN_PARAMETER_NAME);
            if (parameterValue != null) {
                fullScan = (Boolean) parameterValue.getValue();
            }
            if (!fullScan) {
                fullScan = build.getId().equals("1");
            }
        }
    }

    private void wrapScmChanges(AbstractBuild<?, ?> build) {
        ChangeLogSet<? extends ChangeLogSet.Entry> buildChangeSet = build.getChangeSet();
        switch (buildChangeSet.getClass().getName()) {
            case "hudson.plugins.git.GitChangeSetList":
                scmChangesWrapper = new ScmChangesWrapper(SCMType.GIT);
                break;
            case "hudson.scm.SubversionChangeLogSet":
                scmChangesWrapper = new ScmChangesWrapper(SCMType.SVN);
                break;
            default:
        }
        if (scmChangesWrapper != null) {
            Object[] changeSetItems = build.getChangeSet().getItems();
            for (int i = 0; i < changeSetItems.length; i++) {
                ChangeLogSet.Entry changeSet = (ChangeLogSet.Entry) changeSetItems[i];
                for (ChangeLogSet.AffectedFile affectedFileChange : changeSet.getAffectedFiles()) {

                    //compute edit type
                    ScmChangeEditTypeWrapper editType;
                    if (affectedFileChange.getEditType().equals(EditType.ADD)) {
                        editType = ScmChangeEditTypeWrapper.ADD;
                    } else if (affectedFileChange.getEditType().equals(EditType.DELETE)) {
                        editType = ScmChangeEditTypeWrapper.DELETE;
                    } else if (affectedFileChange.getEditType().equals(EditType.EDIT)) {
                        editType = ScmChangeEditTypeWrapper.EDIT;
                    } else {
                        throw new IllegalArgumentException("Not expected value : " + affectedFileChange.getEditType());
                    }

                    ScmChangeAffectedFileWrapper fileWrapper = new ScmChangeAffectedFileWrapper(affectedFileChange.getPath(), editType);

                    //compute fields related to scm type
                    if (scmChangesWrapper.getScmType().equals(SCMType.GIT)) {
                        fileWrapper.setGitDst(getGitDestination(affectedFileChange));
                        fileWrapper.setGitSrc(getGitSource(affectedFileChange));
                    } else if (scmChangesWrapper.getScmType().equals(SCMType.SVN)) {
                        fileWrapper.setSvnDirType(isSvnDir(affectedFileChange));
                    }

                    if(fileWrapper.isSvnDirType()||
                            UftTestDiscoveryUtils.isTestMainFilePath(fileWrapper.getPath())||
                            (TestingToolType.UFT.equals(testingToolType) && UftTestDiscoveryUtils.isUftDataTableFile(fileWrapper.getPath())) ||
                            (TestingToolType.MBT.equals(testingToolType) && UftTestDiscoveryUtils.isUftActionFile(fileWrapper.getPath()))){
                        //add to list
                        scmChangesWrapper.getAffectedFiles().add(fileWrapper);
                    }
                }
            }
        }
    }

    @Override
    public UftTestDiscoveryResult invoke(File file, VirtualChannel virtualChannel) {
        UftTestDiscoveryResult results = UFTTestDetectionService.startScanning(file, buildListener, configurationId, workspaceId, scmRepositoryId, testRunnerId, scmChangesWrapper, fullScan, testingToolType);
        return results;
    }

    @Override
    public void checkRoles(RoleChecker roleChecker) throws SecurityException {
        //no need to check roles as this can be run on master and on slave
    }

    private static boolean isSvnDir(ChangeLogSet.AffectedFile path) {
        //ONLY for SVN plugin : Check if path is directory
        try {
            String value = (String) FieldUtils.readDeclaredField(path, "kind", true);
            return "dir".equals(value);
        } catch (Exception e) {
            return false;
        }
    }

    private static String getGitDestination(ChangeLogSet.AffectedFile path) {
        try {
            return (String) FieldUtils.readDeclaredField(path, "dst", true);
        } catch (Exception e) {
            return null;
        }
    }

    private static String getGitSource(ChangeLogSet.AffectedFile path) {
        try {
            return (String) FieldUtils.readDeclaredField(path, "src", true);
        } catch (Exception e) {
            return null;
        }
    }

    public static class ScmChangesWrapper implements Serializable {
        private SCMType scmType;
        private List<ScmChangeAffectedFileWrapper> affectedFiles = new ArrayList<>();

        public ScmChangesWrapper(SCMType scmType) {
            this.scmType = scmType;
        }

        public SCMType getScmType() {
            return scmType;
        }

        public List<ScmChangeAffectedFileWrapper> getAffectedFiles() {
            return affectedFiles;
        }
    }

    public enum ScmChangeEditTypeWrapper implements Serializable {
        ADD, EDIT, DELETE
    }

    public static class ScmChangeAffectedFileWrapper implements Serializable {

        private String path;

        private ScmChangeEditTypeWrapper editType;
        private String gitSrc;
        private String gitDst;
        private boolean svnDirType;


        public ScmChangeAffectedFileWrapper(String path, ScmChangeEditTypeWrapper editType) {
            this.path = path;
            this.editType = editType;
        }

        public String getPath() {
            return path;
        }

        public ScmChangeEditTypeWrapper getEditType() {
            return editType;
        }

        public String getGitDst() {
            return gitDst;
        }

        public String getGitSrc() {
            return gitSrc;
        }

        public boolean isSvnDirType() {
            return svnDirType;
        }

        public void setGitSrc(String gitSrc) {
            this.gitSrc = gitSrc;
        }

        public void setGitDst(String gitDst) {
            this.gitDst = gitDst;
        }

        public void setSvnDirType(boolean svnDirType) {
            this.svnDirType = svnDirType;
        }
    }
}
