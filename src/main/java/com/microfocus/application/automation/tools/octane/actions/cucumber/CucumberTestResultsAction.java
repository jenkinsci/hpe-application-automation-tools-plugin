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
package com.microfocus.application.automation.tools.octane.actions.cucumber;

import com.microfocus.application.automation.tools.octane.Messages;
import hudson.FilePath;
import hudson.model.Action;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by franksha on 07/12/2016.
 */
public class CucumberTestResultsAction implements Action {
    private final String glob;
    private final Run<?, ?> build;

    CucumberTestResultsAction(Run<?, ?> run, String glob, TaskListener listener) {
        this.build = run;
        this.glob = glob;
        CucumberResultsService.setListener(listener);
    }

    public boolean copyResultsToBuildFolder(Run<?, ?> run, FilePath workspace) {
        try {
            CucumberResultsService.log(Messages.CucumberResultsActionCollecting());
            String[] files = CucumberResultsService.getCucumberResultFiles(workspace, glob);
            boolean found = files.length > 0;

            for (String fileName : files) {
                File resultFile = new File(workspace.child(fileName).toURI());
                if (resultFile.lastModified() == 0 || run.getStartTimeInMillis() < resultFile.lastModified()) {
                    // for some reason , on some linux machines last modified time for newly create gherkin result file is 0 - lets consider it as valid
                    CucumberResultsService.copyResultFile(resultFile, build.getRootDir(), workspace);
                } else {
                    String pattern = "yyyy-MM-dd HH:mm:ss";
                    SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);

                    CucumberResultsService.log("Found outdated file %s, build started at %s (%s), while file last update time is %s (%s) ",
                            resultFile.getPath(), dateFormat.format(new Date(run.getStartTimeInMillis())), String.valueOf(run.getStartTimeInMillis()),
                            dateFormat.format(new Date(resultFile.lastModified())), String.valueOf(resultFile.lastModified()));
                }
            }

            if (!found && build.getResult() != Result.FAILURE) {
                // most likely a configuration error in the job - e.g. false pattern to match the cucumber result files
                CucumberResultsService.log(Messages.CucumberResultsActionNotFound());
            }  // else , if results weren't found but build result is failure - most likely a build failed before us. don't report confusing error message.

            return found;

        } catch (Exception e) {
            CucumberResultsService.log(Messages.CucumberResultsActionError(), e.toString());
            return false;
        }
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
