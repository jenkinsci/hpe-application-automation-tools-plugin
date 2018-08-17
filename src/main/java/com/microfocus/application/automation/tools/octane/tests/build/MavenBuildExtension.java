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

package com.microfocus.application.automation.tools.octane.tests.build;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Run;

/**
 * Run/Build metadata factory for Maven projects
 */

@Extension
public class MavenBuildExtension extends BuildHandlerExtension {

	@Override
	public boolean supports(Run<?, ?> run) {
		return "hudson.maven.MavenBuild".equals(run.getClass().getName()) ||
				"hudson.maven.MavenModuleSetBuild".equals(run.getClass().getName());
	}

	@Override
	public BuildDescriptor getBuildType(Run<?, ?> run) {
		return new BuildDescriptor(
				BuildHandlerUtils.getJobCiId(run),
				((AbstractBuild) run).getProject().getName(),
				BuildHandlerUtils.getBuildCiId(run),
				String.valueOf(run.getNumber()),
				"");
	}

	@Override
	public String getProjectFullName(Run<?, ?> run) {
		if ("hudson.maven.MavenBuild".equals(run.getClass().getName())) {
			// we don't push individual maven module results (although we create the file)
			return null;
		} else {
			return ((AbstractBuild) run).getProject().getFullName();
		}
	}
}
