/*
 * Certain versions of software accessible here may contain branding from Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 * This software was acquired by Micro Focus on September 1, 2017, and is now offered by OpenText.
 * Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright 2012-2024 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ___________________________________________________________________
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
	public final static EnumDescription CreateHtmlReportResults = new EnumDescription("PUBLISH_HTML_REPORT", "Always archive and publish test reports (LRP only)");
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



