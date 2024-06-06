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

package com.microfocus.application.automation.tools.run;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.microfocus.application.automation.tools.model.*;
import com.microfocus.application.automation.tools.octane.executor.UftConstants;
import com.microfocus.application.automation.tools.settings.AlmServerSettingsGlobalConfiguration;
import com.microfocus.application.automation.tools.sse.common.StringUtils;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.Secret;
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

        List<AlmServerSettingsModel> models = Arrays.asList(AlmServerSettingsGlobalConfiguration.getInstance().getInstallations());

        Multimap<String , String> serverUsernames = ArrayListMultimap.create();
        Multimap<String , String> serverClientIds = ArrayListMultimap.create();

        for (Project job : jobs) {
            List<Builder> builders = job.getBuilders();
            if (builders != null) {
                for (Builder builder : builders) {
                    if (builder instanceof RunFromAlmBuilder) {
                        RunFromAlmBuilder almBuilder = (RunFromAlmBuilder) builder;
                        String almUsername = almBuilder.runFromAlmModel.getAlmUserName();
                        String almClientID = almBuilder.runFromAlmModel.getAlmClientID();
                        if(!StringUtils.isNullOrEmpty(almUsername) || !StringUtils.isNullOrEmpty(almClientID)) {
                            listener.getLogger().println("Migrating credentials from task " + job.getDisplayName());

                            for (AlmServerSettingsModel model : models) {
                                if (model.getAlmServerName().equals(almBuilder.getAlmServerName())) {
                                    if (!StringUtils.isNullOrEmpty(almUsername) && !serverUsernames.get(model.getAlmServerName()).contains(almUsername) &&
                                        !almUsername.equals(UftConstants.NO_USERNAME_DEFINED) && almBuilder.runFromAlmModel.getAlmPassword() != null) {
                                        String almPassword = almBuilder.runFromAlmModel.getAlmPassword().getPlainText();
                                        serverUsernames.put(model.getAlmServerName(), almUsername);
                                        model.set_almCredentials(Arrays.asList(new CredentialsModel(almUsername, almPassword)));
                                        listener.getLogger().println("Migrating username '" + almUsername + "' 'for server: " + model.getAlmServerName() + ", " + model.getAlmServerUrl());
                                    }

                                    if (!StringUtils.isNullOrEmpty(almClientID) && !serverClientIds.get(model.getAlmServerName()).contains(almClientID) &&
                                        !almClientID.equals(UftConstants.NO_CLIENT_ID_DEFINED) && almBuilder.runFromAlmModel.getAlmApiKey() != null) {
                                        String almApiKeySecret = almBuilder.runFromAlmModel.getAlmApiKey().getPlainText();
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
        List<AlmServerSettingsModel> models = Arrays.asList(AlmServerSettingsGlobalConfiguration.getInstance().getInstallations());
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
