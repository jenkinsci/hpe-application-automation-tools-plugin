/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.microfocus.application.automation.tools.octane.actions.coverage;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.BuildListener;

import java.io.File;
import java.util.ArrayList;

/**
 * this action initiate a copy of all coverage reports from workspace to build folder.
 * the files are calculated by a pattern that the user enters in job configuration page
 */
public class CoveragePublisherAction implements Action {
    private final AbstractBuild build;

    public CoveragePublisherAction(AbstractBuild build, BuildListener listener) {
        this.build = build;
        CoverageService.setListener(listener);
    }

    /**
     * this method copy all reports from specified path pattern
     * @return true if files have been copied, otherwise return false
     */
    public boolean copyCoverageReportsToBuildFolder(String filePattern, String defaultFileName) {
        try {
            CoverageService.log("start copying coverage report to build folder, using file patten of " + filePattern);
            String[] files = CoverageService.getCoverageFiles(build.getWorkspace(), filePattern);
            ArrayList<String> filteredFiles = filterFilesByFileExtension(files);
            boolean found = filteredFiles.size() > 0;
            int index = 0;

            for (String fileName : filteredFiles) {
                File resultFile = new File(build.getWorkspace().child(fileName).toURI());
                File targetReportFile = new File(build.getRootDir(), CoverageService.getCoverageReportFileName(index++, defaultFileName));
                CoverageService.copyCoverageFile(resultFile, targetReportFile, build.getWorkspace());
            }

            if (!found) {
                // most likely a configuration error in the job - e.g. false pattern to match the cucumber result files
                CoverageService.log("No coverage file that matched the specified pattern was found in workspace");
            }
            return found;

        } catch (Exception e) {
            CoverageService.log("Copying coverage files to build folder failed because of " + e.toString());
            return false;
        }
    }

    /**
     * pre validation of coverage files by file extension.
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
