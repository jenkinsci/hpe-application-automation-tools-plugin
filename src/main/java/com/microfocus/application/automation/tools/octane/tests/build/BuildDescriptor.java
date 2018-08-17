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

public final class BuildDescriptor {

	private final String jobId;
	private final String jobName;
	private final String buildId;
	private final String buildName;
	private final String subType;

	public BuildDescriptor(String jobId, String jobName, String buildId, String buildName, String subType) {
		this.jobId = jobId;
		this.jobName = jobName;
		this.buildId = buildId;
		this.buildName = buildName;
		this.subType = subType;
	}

	public String getJobId() {
		return jobId;
	}

	public String getJobName() {
		return jobName;
	}

	public String getBuildId() {
		return buildId;
	}

	public String getBuildName() {
		return buildName;
	}

	public String getSubType() {
		return subType;
	}
}
