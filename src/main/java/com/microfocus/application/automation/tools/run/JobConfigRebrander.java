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

package com.microfocus.application.automation.tools.run;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.XmlFile;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created in order to fix previous builds that we're build with HP/HPE convention plugin and move them to Micro Focus
 */
public class JobConfigRebrander  extends Builder implements SimpleBuildStep {
    private static final String MICROFOCUS = ".microfocus.";
    private static final String HPE_HP_REGEX = "\\.hp\\.|\\.hpe\\.";

    private Run<?, ?> build;

    @DataBoundConstructor
    public JobConfigRebrander() {
        // A class which extends Builder should supply an empty constructor.
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
                        @Nonnull TaskListener listener) throws InterruptedException, IOException{
        build = run;
        File root = Jenkins.getInstance().getRootDir();
        convertXmlFilesAtRootDir(listener, root);
        File projectsDir = new File(root,"jobs");
        File[] subdirs = projectsDir.listFiles();

        for (final File subdir : subdirs) {
            convertSpecifiedXmlFile(listener, subdir, "config.xml");
            final File buildsFolder = new File(subdir, "builds");
            File[] builds = buildsFolder.listFiles();

            for(final File buildDir : builds){
                convertSpecifiedXmlFile(listener, buildDir, "build.xml");
            }
        }
    }

    /**
     * Creates XML file of the file we want to convert.
     * And calls {@link JobConfigRebrander#convertOldNameToNewName(TaskListener, XmlFile)}
     * to convert from all the old package names.
     * @param listener      A place to send log output
     * @param dir           The directory which we create the file
     * @param xmlFileName   The XML file name
     */
    private void convertSpecifiedXmlFile(@Nonnull TaskListener listener, File dir, String xmlFileName) {
        XmlFile xmlFile = new XmlFile(new File(dir, xmlFileName));
        if (xmlFile.exists()) {
            convertOldNameToNewName(listener, xmlFile);
        }
    }

    /**
     * Converts xml files which is located in the root directory
     * That is responsible for the configuration files located at the "Manage Jenkins -> Configure System".
     * @param listener  a place to send log output
     * @param root      the root directory
     */
    private void convertXmlFilesAtRootDir(@Nonnull TaskListener listener, File root) {
        try {
            File[] files = root.listFiles(pathname -> {
                String name = pathname.getName().toLowerCase();
                return (name.startsWith("com.hpe") || name.startsWith("com.hp")) && name.endsWith(".xml") && pathname.isFile();
            });

            for (File file: files) {
                String newFileName = file.toString().replaceAll(HPE_HP_REGEX, MICROFOCUS);
                File replacedFile  = new File(newFileName);

                removeXmlFileIfExists(listener, newFileName, replacedFile);
                convertXmlFileIfNotExists(listener, file, replacedFile);
            }
        } catch (SecurityException | NullPointerException e) {
            listener.error("Failed to convert Global Settings configurations to microfocus: %s", e.getMessage());
            build.setResult(Result.FAILURE);
        }
    }

    private void convertXmlFileIfNotExists(@Nonnull TaskListener listener, File file, File replacedFile) {
        if (!replacedFile.exists() && file.renameTo(replacedFile)) {
            XmlFile xmlFile = new XmlFile(replacedFile);
            convertOldNameToNewName(listener, xmlFile);
        }
    }

    /**
     * There are files which can be auto-generated as empty after the package name change.
     * We need to remove them in order to let the
     * {@link JobConfigRebrander#convertXmlFileIfNotExists(TaskListener, File, File)} do the convert.
     * @param listener      a place to send log output
     * @param newFileName   the new file absolute path
     * @param replacedFile  the new file name which we need to remove
     */
    private void removeXmlFileIfExists(@Nonnull TaskListener listener, String newFileName, File replacedFile) {
        if (replacedFile.exists()) {
            try {
                Files.delete(Paths.get(replacedFile.getAbsolutePath()));
            } catch (IOException | SecurityException e) {
                listener.error("Failed to delete %s when doing Global Settings configurations convert: %s",
                        newFileName, e.getMessage());
                build.setResult(Result.UNSTABLE);
            }
        }
    }

    /**
     * Replace all occurrences of {@code oldName} of a given XML file.
     * @param listener    A place to send log output
     * @param confXmlFile The XML file which we convert
     * @see XmlFile
     */
    private void convertOldNameToNewName(@Nonnull TaskListener listener, XmlFile confXmlFile) {
        try {
            String configuration = FileUtils.readFileToString(confXmlFile.getFile());
            String newConfiguration = configuration.replaceAll(HPE_HP_REGEX, MICROFOCUS);
            FileUtils.writeStringToFile(confXmlFile.getFile(), newConfiguration);
        } catch (IOException e) {
            listener.error("Failed to convert job configuration format to microfocus: %s", e.getMessage());
            build.setResult(Result.FAILURE);
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
