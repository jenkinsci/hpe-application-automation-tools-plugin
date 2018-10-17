/*
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
 */

package com.microfocus.application.automation.tools.octane.tests.detection;

import hudson.Extension;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import hudson.tasks.Builder;

@SuppressWarnings("squid:S1872")
@Extension
public class UFTExtension extends ResultFieldsDetectionExtension {

	public static final String UFT = "UFT";

	public static final String RUN_FROM_FILE_BUILDER = "RunFromFileBuilder";
	public static final String RUN_FROM_ALM_BUILDER = "RunFromAlmBuilder";

	@Override
	public ResultFields detect(final Run build) {
		if (build.getParent() instanceof FreeStyleProject) {
			for (Builder builder : ((FreeStyleProject) build.getParent()).getBuilders()) {
				if (RUN_FROM_FILE_BUILDER.equals(builder.getClass().getSimpleName()) || RUN_FROM_ALM_BUILDER.equals(builder.getClass().getSimpleName())) {
					return new ResultFields(UFT, UFT, null);
				}
			}
		}
		return null;
	}
}
