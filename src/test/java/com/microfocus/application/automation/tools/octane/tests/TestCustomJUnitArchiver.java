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

package com.microfocus.application.automation.tools.octane.tests;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;
import hudson.tasks.test.AbstractTestResultAction;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class TestCustomJUnitArchiver extends Recorder {

	private String resultFile;

	public TestCustomJUnitArchiver(String resultFile) {
		this.resultFile = resultFile;
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		build.addAction(new TestResultAction());
		InputStream is = build.getWorkspace().child(resultFile).read();
		String junitResults = IOUtils.toString(is, "UTF-8");
		junitResults = junitResults
				.replaceAll("%%%WORKSPACE%%%", build.getWorkspace().getRemote())
				.replaceAll("%%%SEPARATOR%%%", File.separator.equals("\\") ? "\\\\" : File.separator);
		IOUtils.closeQuietly(is);
		new FilePath(build.getRootDir()).child("junitResult.xml").write(junitResults, "UTF-8");
		return true;
	}

	private static class TestResultAction extends AbstractTestResultAction {

		@Override
		public int getFailCount() {
			return 0;
		}

		@Override
		public int getTotalCount() {
			return 0;
		}

		@Override
		public Object getResult() {
			return new Object();
		}
	}
}
