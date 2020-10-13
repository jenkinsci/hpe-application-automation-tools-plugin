package com.microfocus.application.automation.tools.run;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.microfocus.application.automation.tools.model.*;
import com.microfocus.application.automation.tools.octane.executor.UftConstants;
import com.microfocus.application.automation.tools.settings.AlmServerSettingsBuilder;
import com.microfocus.application.automation.tools.sse.common.StringUtils;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;

import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class MigrateAlmCredentialsBuilder extends Recorder implements Serializable, SimpleBuildStep {



    @DataBoundConstructor
    public MigrateAlmCredentialsBuilder() {}

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener taskListener) throws InterruptedException, IOException {
        if(!isMigrationDone()) {
            scanJobs(taskListener);
        } else {
            taskListener.getLogger().println("ALM credentials have been already migrated to Jenkins Configure page.");
        }
    }

    public static void scanJobs(TaskListener listener) {
        List<Project> jobs = Jenkins.getInstanceOrNull().getAllItems(Project.class);

        List<AlmServerSettingsModel> models = Arrays.asList(Jenkins.getInstanceOrNull().getDescriptorByType(AlmServerSettingsBuilder.DescriptorImpl.class).getInstallations());

        Multimap<String , String> serverUsernames = ArrayListMultimap.create();
        Multimap<String , String> serverClientIds = ArrayListMultimap.create();

        for (Project job : jobs) {
            List<Builder> builders = job.getBuilders();
            if (builders != null) {
                for (Builder builder : builders) {
                    if (builder instanceof RunFromAlmBuilder) {
                        RunFromAlmBuilder almBuilder = (RunFromAlmBuilder) builder;
                        String almUsername = almBuilder.getAlmUserName();
                        String almPassword = almBuilder.getAlmPassword();
                        String almClientID = almBuilder.getAlmClientID();
                        String almApiKeySecret = almBuilder.getAlmApiKey();
                        if(!StringUtils.isNullOrEmpty(almUsername) || !StringUtils.isNullOrEmpty(almClientID)) {
                            listener.getLogger().println("Migrating credentials from task " + job.getDisplayName());

                            for (AlmServerSettingsModel model : models) {
                                if (model.getAlmServerName().equals(almBuilder.getAlmServerName())) {
                                    if (!StringUtils.isNullOrEmpty(almUsername) && !serverUsernames.get(model.getAlmServerName()).contains(almUsername) &&
                                        !almUsername.equals(UftConstants.NO_USERNAME_DEFINED)) {
                                        serverUsernames.put(model.getAlmServerName(), almUsername);
                                        model.set_almCredentials(Arrays.asList(new CredentialsModel(almUsername, almPassword)));
                                        listener.getLogger().println("Migrating username '" + almUsername + "' 'for server: " + model.getAlmServerName() + ", " + model.getAlmServerUrl());
                                    }

                                    if (!StringUtils.isNullOrEmpty(almClientID) && !serverClientIds.get(model.getAlmServerName()).contains(almClientID) &&
                                        !almClientID.equals(UftConstants.NO_CLIENT_ID_DEFINED)) {
                                        serverClientIds.put(model.getAlmServerName(), almClientID);
                                        model.set_almSSOCredentials(Arrays.asList(new SSOCredentialsModel(almClientID, almApiKeySecret)));
                                        listener.getLogger().println("Migrating client ID '" + almClientID + "' for server: " + model.getAlmServerName() + ", " + model.getAlmServerUrl());
                                    }


                                    try {
                                        job.save();
                                    } catch (IOException e) {
                                        listener.getLogger().println("Job not saved.");
                                    }

                                    listener.getLogger().println("------------------------------");
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public Boolean isMigrationDone(){
        List<AlmServerSettingsModel> models = Arrays.asList(Jenkins.getInstanceOrNull().getDescriptorByType(AlmServerSettingsBuilder.DescriptorImpl.class).getInstallations());
        for(AlmServerSettingsModel model : models){
            if (model != null && (!model.getAlmCredentials().isEmpty() || !model.getAlmSSOCredentials().isEmpty())) {
                return true;
            }
        }

        return false;
    }


    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        /**
         * Instantiates a new Descriptor.
         */
        public DescriptorImpl() {

            load();
        }

        @Override
        public String getDisplayName() {

            return "Migrate ALM Credentials";
        }

        @Override
        public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {

            return true;
        }

    }
}
