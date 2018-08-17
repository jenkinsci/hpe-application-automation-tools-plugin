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

package com.microfocus.application.automation.tools.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

public class ResultsPublisherModel implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public final static EnumDescription dontArchiveResults = new EnumDescription("DONT_ARCHIVE_TEST_REPORT", "Do not archive test reports");
	public final static EnumDescription alwaysArchiveResults = new EnumDescription("ALWAYS_ARCHIVE_TEST_REPORT", "Always archive test reports");
	public final static EnumDescription ArchiveFailedTestsResults = new EnumDescription("ONLY_ARCHIVE_FAILED_TESTS_REPORT", "Archive test report for failed tests ");
	public final static EnumDescription CreateHtmlReportResults = new EnumDescription("PUBLISH_HTML_REPORT", "Always archive and publish test reports (LR only)");
    public final static List<EnumDescription> archiveModes =
            Arrays.asList(ArchiveFailedTestsResults, alwaysArchiveResults,
                    CreateHtmlReportResults, dontArchiveResults);

	private String archiveTestResultsMode;

	@DataBoundConstructor
	public ResultsPublisherModel(String archiveTestResultsMode) {
	
		this.archiveTestResultsMode=archiveTestResultsMode;
		
		if (this.archiveTestResultsMode.isEmpty()){
			this.archiveTestResultsMode=dontArchiveResults.getValue();
		}
	}

	public String getArchiveTestResultsMode() {
		return archiveTestResultsMode;
	}
	
}



