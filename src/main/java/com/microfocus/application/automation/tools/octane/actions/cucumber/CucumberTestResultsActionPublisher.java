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

package com.microfocus.application.automation.tools.octane.actions.cucumber;

import com.microfocus.application.automation.tools.octane.Messages;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Created by franksha on 07/12/2016.
 */
public class CucumberTestResultsActionPublisher extends Recorder {

    private final String glob;

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @DataBoundConstructor
    public CucumberTestResultsActionPublisher(String cucumberResultsGlob) {
        this.glob = cucumberResultsGlob;
    }

    public String getCucumberResultsGlob() {
        return glob;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        CucumberTestResultsAction action = new CucumberTestResultsAction(build, glob, listener);
        build.addAction(action);
        boolean resultsFound = action.copyResultsToBuildFolder();
        return resultsFound;
    }

    @Override
    public CucumberTestResultsActionPublisher.Descriptor getDescriptor() {
        return (CucumberTestResultsActionPublisher.Descriptor) super.getDescriptor();
    }

    @Extension
    public static final class Descriptor extends BuildStepDescriptor<Publisher> {

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return Messages.CucumberReporterName();
        }

        public FormValidation doCheckCucumberResultsGlob(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException, ServletException {
            if (value == null || value.isEmpty()) {
                return FormValidation.warning(Messages.CucumberResultsActionEmptyConfigurationWarning(), CucumberResultsService.DEFAULT_GLOB);
            } else if (project == null) {
                return FormValidation.ok();
            }
            return FilePath.validateFileMask(project.getSomeWorkspace(), value);
        }
    }
}
