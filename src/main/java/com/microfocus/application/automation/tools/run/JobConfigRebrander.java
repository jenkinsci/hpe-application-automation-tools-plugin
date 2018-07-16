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

package com.microfocus.application.automation.tools.run;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.XmlFile;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;


import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;

/**
 * Created in order to fix previous builds that we're build with HP convention plugin and move them to HPE
 */
public class JobConfigRebrander  extends Builder implements SimpleBuildStep {

    @DataBoundConstructor
    public JobConfigRebrander() {
    }

    /**
     * Run this step.
     *
     * @param run       a build this is running as a part of
     * @param workspace a workspace to use for any file operations
     * @param launcher  a way to start processes
     * @param listener  a place to send output
     * @throws InterruptedException if the step is interrupted
     */
    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher,
                        @Nonnull TaskListener listener) throws InterruptedException, IOException {
        File root = Jenkins.getInstance().getRootDir();
        File projectsDir = new File(root,"jobs");
        File[] subdirs = projectsDir.listFiles();
        for (final File subdir : subdirs) {
            XmlFile xmlFile = new XmlFile(new File(subdir,"config.xml"));
            if (xmlFile.exists()) {
                convertOldNameToNewName(listener, xmlFile, ".hp.");
                convertOldNameToNewName(listener, xmlFile, ".hpe.");
            }

            final File buildsFolder = new File(subdir, "builds");
            File[] builds = buildsFolder.listFiles();
            for(final File buildDir : builds){
                XmlFile buildXmlFile = new XmlFile(new File(buildDir,"build.xml"));
                if (buildXmlFile.exists()) {
                    convertOldNameToNewName(listener, buildXmlFile, ".hp.");
                    convertOldNameToNewName(listener, buildXmlFile, ".hpe.");
                }
            }

        }

    }

    private void convertOldNameToNewName(@Nonnull TaskListener listener, XmlFile confXmlFile, String oldName) {
        try {
            String configuration = FileUtils.readFileToString(confXmlFile.getFile());
            String newConfiguration = StringUtils.replace(configuration, oldName, ".microfocus.");
            FileUtils.writeStringToFile(confXmlFile.getFile(), newConfiguration);
        } catch (IOException e) {
            listener.error(
                    String.format("failed to convert configuration format (%s to microfocus): %s", oldName, e.getMessage()));
        }
    }


    @Extension
    public static class Descriptor extends BuildStepDescriptor<Builder> {

        public Descriptor() {
            load();
        }

        @Override
        public String getDisplayName() {
            return "Fix old Micro Focus Jenkins builds";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

    }

}
