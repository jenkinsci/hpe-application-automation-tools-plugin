/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2019 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors ("Micro Focus") are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.octane.actions;

import com.hp.octane.integrations.OctaneClient;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.dto.entities.EntityConstants;
import com.hp.octane.integrations.dto.entities.impl.EntityImpl;
import com.hp.octane.integrations.dto.scm.SCMType;
import com.hp.octane.integrations.services.configuration.ConfigurationService;
import com.hp.octane.integrations.services.entities.EntitiesService;
import com.hp.octane.integrations.services.entities.QueryHelper;
import com.hp.octane.integrations.uft.items.UftTestDiscoveryResult;
import com.microfocus.application.automation.tools.octane.JellyUtils;
import com.microfocus.application.automation.tools.octane.Messages;
import com.microfocus.application.automation.tools.octane.executor.*;
import com.microfocus.application.automation.tools.octane.executor.scmmanager.ScmPluginFactory;
import com.microfocus.application.automation.tools.octane.executor.scmmanager.ScmPluginHandler;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.Launcher;
import hudson.model.*;
import hudson.scm.NullSCM;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Post-build action of Uft test detection
 */

public class UFTTestDetectionPublisher extends Recorder {
    private String configurationId;
    private String workspaceName;
    private String scmRepositoryId;

    public String getWorkspaceName() {
        return workspaceName;
    }

    public String getScmRepositoryId() {
        return scmRepositoryId;
    }

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public UFTTestDetectionPublisher(String configurationId, String workspaceName, String scmRepositoryId) {
        this.configurationId = configurationId;
        this.workspaceName = workspaceName;
        this.scmRepositoryId = scmRepositoryId;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        UFTTestDetectionService.printToConsole(listener, "UFTTestDetectionPublisher is started.");

        try {
            //validate configuration id
            if (configurationId == null || JellyUtils.NONE.equals(configurationId)) {
                throw new IllegalArgumentException("ALM Octane configuration is missing.");
            }

            if (workspaceName == null || JellyUtils.NONE.equals(workspaceName)) {
                throw new IllegalArgumentException("ALM Octane workspace is missing.");
            }

            //validate scm repository id
            if (StringUtils.isEmpty(scmRepositoryId)) {
                generateScmRepository(build, listener);
            }
        } catch (IllegalArgumentException e) {
            UFTTestDetectionService.printToConsole(listener, e.getMessage());
            build.setResult(Result.FAILURE);
            return false;
        }

        try {
            UftTestDiscoveryResult results = build.getWorkspace().act(new UFTTestDetectionCallable(build, configurationId, workspaceName, getScmRepositoryId(), listener));
            UFTTestDetectionBuildAction buildAction = new UFTTestDetectionBuildAction(build, results);
            build.addAction(buildAction);

            if (results.hasChanges()) {
                UFTTestDetectionService.publishDetectionResults(build, listener, results);
                UftTestDiscoveryDispatcher dispatcher = getExtension(UftTestDiscoveryDispatcher.class);
                dispatcher.enqueueResult(configurationId, build.getProject().getFullName(), build.getNumber(), workspaceName);

            }
            if (!results.getDeletedFolders().isEmpty()) {
                UFTTestDetectionService.printToConsole(listener, String.format("Found %s deleted folders", results.getDeletedFolders().size()));
                UFTTestDetectionService.printToConsole(listener, "To sync deleted items - full sync required. Triggering job with full sync parameter.");
                handleDeletedFolders(build);
            }
        } catch (Exception e) {
            UFTTestDetectionService.printToConsole(listener, "UFTTestDetectionPublisher.perform is failed : " + e.getMessage());
            build.setResult(Result.FAILURE);
        }

        return true;
    }

    private void generateScmRepository(AbstractBuild build, BuildListener listener) {
        SCM scm = build.getProject().getScm();
        if (scm instanceof NullSCM) {
            throw new IllegalArgumentException("SCM definition is missing in the job");
        }
        ScmPluginHandler scmPluginHandler = ScmPluginFactory.getScmHandlerByScmPluginName(scm.getType());
        String url = ScmPluginFactory.getScmHandlerByScmPluginName(scm.getType()).getScmRepositoryUrl(scm);
        if (StringUtils.isEmpty(url)) {
            throw new IllegalArgumentException("SCM url is not defined in the job");
        }
        OctaneClient octaneClient = OctaneSDK.getClientByInstanceId(configurationId);
        EntitiesService entitiesService = octaneClient.getEntitiesService();
        ConfigurationService configurationService = octaneClient.getConfigurationService();
        long workspaceId = Long.parseLong(workspaceName);

        if (configurationService.isOctaneVersionGreaterOrEqual("15.1.28")) {
            scmRepositoryId = getScmRepositoriesWithRoots(listener, scmPluginHandler, url, entitiesService, workspaceId);
        } else {
            scmRepositoryId = getScmRepositoriesLegacy(listener, scmPluginHandler, url, entitiesService, workspaceId);
        }

        try {
            build.getProject().save();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to save job with updated SCM repository. Update manually SCM repository to " + scmRepositoryId + ". Error message : " + e.getMessage());
        }

        UFTTestDetectionService.printToConsole(listener, "SCM repository field value is updated to " + scmRepositoryId);
    }


    private static String getScmRepositoriesLegacy(BuildListener listener, ScmPluginHandler scmPluginHandler, String url, EntitiesService entitiesService, long workspaceId) {
        String scmRepoId;
        String collectionName = "scm_repositories";
        List<String> conditions = Collections.singletonList(QueryHelper.condition("url", url));
        List<Entity> foundEntities = entitiesService.getEntities(workspaceId, collectionName, conditions, Collections.singletonList(EntityConstants.Base.ID_FIELD));
        if (!foundEntities.isEmpty()) {
            scmRepoId = foundEntities.get(0).getId();
            UFTTestDetectionService.printToConsole(listener, "SCM repository " + url + " is already exist in ALM Octane with id=" + scmRepoId);
        } else {
            //create a new scm repository
            Entity newScmRepository = buildNewRepoEntity(scmPluginHandler, url, "scm_repository");
            String name = scmPluginHandler.tryExtractUrlShortName(url) + (SCMType.GIT.equals(scmPluginHandler.getScmType()) ? ":master" : "");
            newScmRepository.setName(name);

            List<Entity> createEntities = entitiesService.postEntities(workspaceId, collectionName, Collections.singletonList(newScmRepository));
            scmRepoId = createEntities.get(0).getId();
            UFTTestDetectionService.printToConsole(listener, "SCM repository " + url + " is created in ALM Octane with id=" + scmRepoId);
        }
        return scmRepoId;
    }

    private static String getScmRepositoriesWithRoots(BuildListener listener, ScmPluginHandler scmPluginHandler, String url, EntitiesService entitiesService, long workspaceId) {
        String scmBranchId;
        String scmRootId;
        String branchCollectionName = EntityConstants.ScmRepository.COLLECTION_NAME;
        String rootCollectionName = EntityConstants.ScmRepositoryRoot.COLLECTION_NAME;

        boolean isGit = SCMType.GIT.equals(scmPluginHandler.getScmType());

        //find repository root
        List<String> conditions = Collections.singletonList(QueryHelper.condition("url", url));
        List<Entity> foundEntities = entitiesService.getEntities(workspaceId, rootCollectionName, conditions, Collections.singletonList(EntityConstants.Base.ID_FIELD));
        if (!foundEntities.isEmpty()) {
            scmRootId = foundEntities.get(0).getId();
            UFTTestDetectionService.printToConsole(listener, String.format("SCM repository root %s is already exist in ALM Octane with id=%s", url, scmRootId));
        } else {
            //create a new scm repository root
            Entity newScmRepositoryRoot = buildNewRepoEntity(scmPluginHandler, url, EntityConstants.ScmRepositoryRoot.ENTITY_NAME);
            List<Entity> createEntities = entitiesService.postEntities(workspaceId, rootCollectionName, Collections.singletonList(newScmRepositoryRoot));
            scmRootId = createEntities.get(0).getId();
            UFTTestDetectionService.printToConsole(listener, String.format("SCM repository root %s  is created in ALM Octane with id=%s", url, scmRootId));
        }

        //find branch
        String name = scmPluginHandler.tryExtractUrlShortName(url) + (isGit ? ":master" : "");
        String branchCondition = QueryHelper.orConditions(QueryHelper.condition(EntityConstants.ScmRepository.BRANCH, "master"),
                QueryHelper.conditionEmpty(EntityConstants.ScmRepository.BRANCH));
        conditions = Collections.singletonList(branchCondition);
        foundEntities = entitiesService.getEntities(workspaceId, branchCollectionName, conditions,
                Arrays.asList(EntityConstants.ScmRepository.ID_FIELD, EntityConstants.ScmRepository.BRANCH));
        if (!foundEntities.isEmpty()) {
            scmBranchId = foundEntities.get(0).getId();
            UFTTestDetectionService.printToConsole(listener, String.format("SCM branch %s is already exist in ALM Octane with id=%s", name, scmBranchId));
        } else {
            //create a new branch
            Entity newBranch = buildNewRepoEntity(scmPluginHandler, url, "scm_repository");
            newBranch.setName(name);

            if (isGit) {
                newBranch.setField(EntityConstants.ScmRepository.BRANCH, "master");
            }


            Entity scmRoot = new EntityImpl().setType(EntityConstants.ScmRepositoryRoot.ENTITY_NAME).setId(scmRootId);
            newBranch.setField(EntityConstants.ScmRepository.PARENT, scmRoot);
            List<Entity> createEntities = entitiesService.postEntities(workspaceId, branchCollectionName, Collections.singletonList(newBranch));
            scmBranchId = createEntities.get(0).getId();
            UFTTestDetectionService.printToConsole(listener, String.format("SCM branch %s is created in ALM Octane with id=%s", name, scmBranchId));
        }

        return scmBranchId;
    }

    private static Entity buildNewRepoEntity(ScmPluginHandler scmPluginHandler, String url, String entityType) {
        Entity newScmRepositoryRoot = new EntityImpl();
        newScmRepositoryRoot.setType(entityType);
        String name = scmPluginHandler.tryExtractUrlShortName(url);
        newScmRepositoryRoot.setName(name);
        newScmRepositoryRoot.setField("url", url);
        newScmRepositoryRoot.setField("scm_type", scmPluginHandler.getScmType().getOctaneId());
        return newScmRepositoryRoot;
    }

    private void handleDeletedFolders(AbstractBuild build) {
        //This situation is relevant for SVN only.
        //Deleting folder - SCM event doesn't supply information about deleted items in deleted folder - only top-level directory.
        //In this case need to do for each deleted folder - need to check with Octane what tests and data tables were under this folder.
        //so for each deleted folder - need to do at least 2 requests. In this situation - decided to activate full sync as it already tested scenario.
        //Full sync wil be triggered with delay of 60 secs to give the dispatcher possibility to sync other found changes

        //triggering full sync


        FreeStyleProject proj = (FreeStyleProject) build.getParent();
        List<ParameterValue> newParameters = new ArrayList<>();
        for (ParameterValue param : build.getAction(ParametersAction.class).getParameters()) {
            ParameterValue paramForSet;
            if (param.getName().equals(UftConstants.FULL_SCAN_PARAMETER_NAME)) {
                paramForSet = new BooleanParameterValue(UftConstants.FULL_SCAN_PARAMETER_NAME, true);
            } else {
                paramForSet = param;
            }
            newParameters.add(paramForSet);
        }

        ParametersAction parameters = new ParametersAction(newParameters);
        CauseAction causeAction = new CauseAction(new FullSyncRequiredCause(build.getId()));
        proj.scheduleBuild2(60, parameters, causeAction);

    }

    private static <T> T getExtension(Class<T> clazz) {
        ExtensionList<T> items = Jenkins.get().getExtensionList(clazz);
        return items.get(0);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public String getConfigurationId() {
        return configurationId;
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public ListBoxModel doFillConfigurationIdItems() {
            return JellyUtils.fillConfigurationIdModel();
        }

        public ListBoxModel doFillWorkspaceNameItems(@QueryParameter String configurationId, @QueryParameter(value = "workspaceName") String workspaceName) {
            return JellyUtils.fillWorkspaceModel(configurationId, workspaceName);
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return aClass.equals(FreeStyleProject.class);
        }

        public String getDisplayName() {
            return Messages.UFTTestDetectionPublisherConfigurationLabel();
        }
    }
}
