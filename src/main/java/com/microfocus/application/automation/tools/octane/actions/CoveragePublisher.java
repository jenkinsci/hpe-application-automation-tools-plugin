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

package com.microfocus.application.automation.tools.octane.actions;
import com.microfocus.application.automation.tools.octane.Messages;
import com.microfocus.application.automation.tools.octane.actions.coverage.CoveragePublisherAction;
import com.microfocus.application.automation.tools.octane.actions.coverage.CoverageService;
import com.microfocus.application.automation.tools.octane.tests.CoverageReportsDispatcher;
import hudson.Extension;
import hudson.ExtensionList;
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
import jenkins.model.Jenkins;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Post-build action that collects the coverage reports from workspace
 * the reports that matches a specified regular expression path, are copied to
 * the build folder for future upload.
 */
public class CoveragePublisher extends Recorder {
	private final String jacocoPathPattern;
	private final String lcovPathPattern;
	/**
	 * this ctor is being called from configuration page.
	 * the jacocoPathPattern is being injected from the web page text box
	 * @param jacocoPathPattern regular expression path for coverage reports
	 */
	@DataBoundConstructor
	public CoveragePublisher(String jacocoPathPattern, String lcovPathPattern) {
		// Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
		this.jacocoPathPattern = jacocoPathPattern == null || jacocoPathPattern.isEmpty() ? CoverageService.Jacoco.JACOCO_DEFAULT_PATH : jacocoPathPattern;
		this.lcovPathPattern = lcovPathPattern == null || lcovPathPattern.isEmpty() ? CoverageService.Lcov.LCOV_DEFAULT_PATH : lcovPathPattern;
	}

	/**
	 * this method used for serialization & deserialization of path
	 * @return jacoco path
	 */
	public String getJacocoPathPattern() {
		return jacocoPathPattern;
	}

	/**
	 * this method used for serialization & deserialization of path
	 * @return lcov path
	 */
	public String getLcovPathPattern() {
		return lcovPathPattern;
	}

	/**
	 * this is where we build the project. this method is being called when we run the build
	 * @param build instance
	 * @param launcher instance
	 * @param listener for action attachment
	 * @return status
	 */
	@Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
		boolean copyReportsToBuildFolderStatus = false;
		ExtensionList<CoverageReportsDispatcher> extensionList = Jenkins.getInstance().getExtensionList(CoverageReportsDispatcher.class);
		if (extensionList == null || extensionList.size() == 0) {
			return false;
		}
		// copy coverage reports
		CoveragePublisherAction action = new CoveragePublisherAction(build, listener);
		build.addAction(action);
		if (action.copyCoverageReportsToBuildFolder(jacocoPathPattern, CoverageService.Jacoco.JACOCO_DEFAULT_FILE_NAME)) {
			extensionList.get(0).enqueueTask(build.getProject().getFullName(), build.getNumber(), CoverageService.Jacoco.JACOCO_TYPE);
			copyReportsToBuildFolderStatus = true;
		}
		if (action.copyCoverageReportsToBuildFolder(lcovPathPattern, CoverageService.Lcov.LCOV_DEFAULT_FILE_NAME)) {
			extensionList.get(0).enqueueTask(build.getProject().getFullName(), build.getNumber(), CoverageService.Lcov.LCOV_TYPE);
			copyReportsToBuildFolderStatus |= true;
		}
		// add upload task to queue
		return copyReportsToBuildFolderStatus;
	}

	/**
	 * bound between descriptor to publisher
	 * @return descriptor
	 */
	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	/**
	 * Returns BuildStepMonitor.NONE by default, as Builders normally don't depend on its previous result
	 * @return monitor
	 */
	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	/**
	 * The Publisher object or Recorder is the base.
	 * It needs a BuildStepDescriptor to provide certain information to Jenkins
	 */
	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		public DescriptorImpl() {
			load();
		}

		/**
		 * Indicates that this builder can be used with all kinds of project types
		 * @param aClass that describe the job
		 * @return always true, indicate that this post build action suitable for all jenkins jobs
		 */
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true; // so that it will also be available for maven & other projects
		}

		public String getDisplayName() {
			return "ALM Octane code coverage publisher";
		}

		public FormValidation doCheckJacocoPathPattern(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException, ServletException {
			if (value == null || value.isEmpty()) {
				return FormValidation.warning(Messages.CoverageResultsActionEmptyConfigurationWarning(), CoverageService.Jacoco.JACOCO_DEFAULT_PATH);
			} else if (project == null) {
				return FormValidation.ok();
			}
			return FilePath.validateFileMask(project.getSomeWorkspace(), value);
		}
		public FormValidation doCheckLcovPathPattern(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException, ServletException {
			if (value == null || value.isEmpty()) {
				return FormValidation.warning(Messages.CoverageResultsActionEmptyConfigurationWarning(), CoverageService.Lcov.LCOV_DEFAULT_PATH);
			} else if (project == null) {
				return FormValidation.ok();
			}
			return FilePath.validateFileMask(project.getSomeWorkspace(), value);
		}
	}
}