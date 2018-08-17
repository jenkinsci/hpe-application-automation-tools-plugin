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

package com.microfocus.application.automation.tools.octane.actions.project;

import hudson.Extension;
import hudson.model.*;

/**
 * Created with IntelliJ IDEA.
 * User: gullery
 * Date: 12/01/15
 * Time: 17:55
 * To change this template use File | Settings | File Templates.
 */

@Extension
final public class CustomProject extends Project<FreeStyleProject, FreeStyleBuild> implements TopLevelItem {

	@Extension(ordinal = 1000)
	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

	public static final class DescriptorImpl extends AbstractProject.AbstractProjectDescriptor {
		public String getDisplayName() {
			return "Custom Project";
		}

		public CustomProject newInstance(ItemGroup itemGroup, String name) {
			return new CustomProject(itemGroup, name);
		}
	}

	public CustomProject() {
		super(null, null);
	}

	public CustomProject(ItemGroup group, String name) {
		super(group, name);
	}

	@Override
	public TopLevelItemDescriptor getDescriptor() {
		return DESCRIPTOR;
	}

	@Override
	protected Class<FreeStyleBuild> getBuildClass() {
		return FreeStyleBuild.class;
	}
}