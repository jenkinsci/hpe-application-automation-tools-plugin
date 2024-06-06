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

package com.microfocus.application.automation.tools.results;

import hudson.FilePath;
import hudson.model.Action;
import hudson.model.DirectoryBrowserSupport;
import hudson.model.Run;
import hudson.tasks.test.TestResultProjectAction;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates the transaction summary reports and adds them to the run
 */
public class TransactionSummaryAction implements Action, SimpleBuildStep.LastBuildAction {

    private static final String TRANSACTION_SUMMARY_FOLDER = "TransactionSummary";
    private static final String REPORT_INDEX = "report.index";

    private Map<String, SummaryReport> summaryReportMap = new LinkedHashMap<String, SummaryReport>();
    private final List<TestResultProjectAction> projectActionList;

    private Run<?,?> build;

  /**
   * Instantiates a new Transaction summary action.
   *
   * @param build the build
   */
  public TransactionSummaryAction(Run<?,?> build) throws IOException {
        this.build = build;
        File reportFolder = new File(build.getRootDir(), TRANSACTION_SUMMARY_FOLDER);
        if (reportFolder.exists()) {
            File indexFile = new File(reportFolder, REPORT_INDEX);
            if (indexFile.exists()) {
                File file = new File(build.getRootDir(), TRANSACTION_SUMMARY_FOLDER);
                DirectoryBrowserSupport dbs = new DirectoryBrowserSupport(this, new FilePath(file), "report", "graph.gif", false);


                createTransactionIndex(build, indexFile, dbs);


            }
        }
        projectActionList = new ArrayList<TestResultProjectAction>();
    }

    private void createTransactionIndex(Run<?, ?> build, File indexFile, DirectoryBrowserSupport dbs) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(indexFile));
        String line;
        boolean rolling = true;
        while ((line = br.readLine()) != null) {
            String[] values = line.split("\t");
            if (values.length < 1)
                continue;
            SummaryReport report = new SummaryReport(build, values[0], dbs);
            if (rolling) {
                report.setColor("#FFF");
                rolling = false;
            } else {
                report.setColor("#F1F1F1");
                rolling = true;
            }
            if (values.length >= 2)
                report.setDuration(values[1]);
            else
                report.setDuration("##");
            if (values.length >= 3)
                report.setPass(values[2]);
            else
                report.setPass("##");
            if (values.length >= 4)
                report.setFail(values[3]);
            else
                report.setFail("##");
            summaryReportMap.put(values[0], report);
        }
        br.close();
    }

    @Override
    public String getIconFileName() {
        return "/plugin/hp-application-automation-tools-plugin/PerformanceReport/LoadRunner.png";
    }

    @Override
    public String getDisplayName() {
        return "Transaction Summary";
    }

    @Override
    public String getUrlName() {
        return "TransactionSummary";
    }

  /**
   * Gets build.
   *
   * @return the build
   */
  public Run<?, ?> getBuild() {
        return build;
    }

  /**
   * Gets summary report map.
   *
   * @return the summary report map
   */
  public Map<String, SummaryReport> getSummaryReportMap() {
        return summaryReportMap;
    }

  /**
   * Gets dynamic.
   *
   * @param name the name
   * @param req the req
   * @param rsp the rsp
   * @return the dynamic
   */
  public Object getDynamic(String name, StaplerRequest req, StaplerResponse rsp) {
        if (summaryReportMap.containsKey(name))
            return summaryReportMap.get(name);
        return null;
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        return Collections.emptySet();
    }
}
