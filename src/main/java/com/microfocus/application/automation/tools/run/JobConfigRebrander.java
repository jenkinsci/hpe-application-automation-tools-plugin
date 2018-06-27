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
                convertHpToHpe(listener, xmlFile);
            }

            final File buildsFolder = new File(subdir, "builds");
            File[] builds = buildsFolder.listFiles();
            for(final File buildDir : builds){
                XmlFile buildXmlFile = new XmlFile(new File(buildDir,"build.xml"));
                if (buildXmlFile.exists()) {
                    convertHpToHpe(listener, buildXmlFile);
                }
            }

        }

    }

    private void convertHpToHpe(@Nonnull TaskListener listener, XmlFile confXmlFile) {
        try {
            String configuration = FileUtils.readFileToString(confXmlFile.getFile());
            String newConfiguration = StringUtils.replace(configuration, ".hp.", ".hpe.");
            FileUtils.writeStringToFile(confXmlFile.getFile(), newConfiguration);
        } catch (IOException e) {
            listener.error(
                    "failed to convert configuration 5.1 to new 5.2 format (hp to HPE): " + e.getMessage());
        }
    }


    @Extension
    public static class Descriptor extends BuildStepDescriptor<Builder> {

        public Descriptor() {
            load();
        }

        @Override
        public String getDisplayName() {
            return "Fix old HPE Jenkins builds";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

    }

}
