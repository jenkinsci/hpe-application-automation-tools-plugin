/*
 *  Certain versions of software accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 *  This software was acquired by Micro Focus on September 1, 2017, and is now
 *  offered by OpenText.
 *  Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical
 *  in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the
 *  property of their respective owners.
 *  OpenText is a trademark of Open Text.
 *  __________________________________________________________________
 *  MIT License
 *
 *  Copyright 2012-2025 Open Text.
 *
 *  The only warranties for products and services of Open Text and
 *  its affiliates and licensors ("Open Text") are as may be set forth
 *  in the express warranty statements accompanying such products and services.
 *  Nothing herein should be construed as constituting an additional warranty.
 *  Open Text shall not be liable for technical or editorial errors or
 *  omissions contained herein. The information contained herein is subject
 *  to change without notice.
 *
 *  Except as specifically indicated otherwise, this document contains
 *  confidential information and a valid license is required for possession,
 *  use or copying. If this work is provided to the U.S. Government,
 *  consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 *  Computer Software Documentation, and Technical Data for Commercial Items are
 *  licensed to the U.S. Government under vendor's standard commercial license.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ___________________________________________________________________
 */
package com.microfocus.application.automation.tools.octane.actions.coverage;

import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import hudson.FilePath;
import hudson.model.*;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * this action initiate a copy of all coverage reports from workspace to build folder.
 * the files are calculated by a pattern that the user enters in job configuration page
 */
public class CoveragePublisherAction implements Action {
	private final Run build;

	public CoveragePublisherAction(Run build, TaskListener listener) {
		this.build = build;
		CoverageService.setListener(listener);
	}

	/**
	 * this method copy all reports from specified path pattern
	 *
	 * @return list of file names that were copied; NEVER NULL; if an empty list returned - no coverage reports found
	 */
	public List<String> copyCoverageReportsToBuildFolder(String filePattern, String defaultFileName) {
		List<String> result = new LinkedList<>();
		FilePath workspace = BuildHandlerUtils.getWorkspace(build);
		if (workspace != null) {
			try {
				CoverageService.log("start copying coverage report to build folder, using file patten of " + filePattern);
				String[] files = CoverageService.getCoverageFiles(workspace, filePattern);
				List<String> matchingReportFiles = filterFilesByFileExtension(files);
				int index = 0;

				for (String fileName : matchingReportFiles) {
					File resultFile = new File(workspace.child(fileName).toURI());
					String nextOutputFilename = CoverageService.getCoverageReportFileName(index++, defaultFileName);
					result.add(nextOutputFilename);
					File targetReportFile = new File(build.getRootDir(), nextOutputFilename);
					CoverageService.copyCoverageFile(resultFile, targetReportFile, workspace);
				}

				if (result.isEmpty()) {
					// most likely a configuration error in the job - e.g. false pattern to match the cucumber result files
					CoverageService.log("No coverage file that matched the specified pattern was found in workspace");
				}
			} catch (Exception e) {
				CoverageService.log("Copying coverage files to build folder failed because of " + e.toString());
			}
		}
		return result;
	}

	/**
	 * pre validation of coverage files by file extension.
	 *
	 * @param files to validate
	 * @return filtered list of files
	 */
	private ArrayList<String> filterFilesByFileExtension(String[] files) {
		ArrayList<String> filteredList = new ArrayList<>();
		for (String fileFullPath : files) {
			if (fileFullPath.endsWith(CoverageService.Lcov.LCOV_FILE_EXTENSION) || fileFullPath.endsWith(CoverageService.Jacoco.JACOCO_FILE_EXTENSION)) {
				filteredList.add(fileFullPath);
			}
		}
		return filteredList;
	}

	@Override
	public String getIconFileName() {
		return null;
	}

	@Override
	public String getDisplayName() {
		return null;
	}

	@Override
	public String getUrlName() {
		return null;
	}
}
