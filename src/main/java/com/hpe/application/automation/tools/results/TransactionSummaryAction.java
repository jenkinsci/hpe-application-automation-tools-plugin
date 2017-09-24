
/*
 * MIT License
 * Copyright (c) 2016 Hewlett-Packard Development Company, L.P.
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.hpe.application.automation.tools.results;

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
