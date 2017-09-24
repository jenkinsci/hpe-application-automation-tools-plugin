
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


public class PerformanceReportAction implements Action, SimpleBuildStep.LastBuildAction {

    private static final String PERFORMANCE_REPORT_FOLDER = "PerformanceReport";
    private static final String REPORT_INDEX = "report.index";

    private Map<String, DetailReport> detailReportMap = new LinkedHashMap<String, DetailReport>();
    private final List<TestResultProjectAction> projectActionList;


    private final Run<?,?> build;

    public PerformanceReportAction(Run<?,?> build) throws IOException {
        this.build = build;
        File reportFolder = new File(build.getRootDir(), PERFORMANCE_REPORT_FOLDER);
        if (reportFolder.exists()) {
            File indexFile = new File(reportFolder, REPORT_INDEX);
            if (indexFile.exists()) {
                File file = new File(build.getRootDir(), PERFORMANCE_REPORT_FOLDER);
                DirectoryBrowserSupport dbs = new DirectoryBrowserSupport(this, new FilePath(file), "report", "graph.gif", false);


                createPreformanceIndexFile(build, indexFile, dbs);
            }
        }
        projectActionList = new ArrayList<TestResultProjectAction>();
    }

    private void createPreformanceIndexFile(Run<?, ?> build, File indexFile, DirectoryBrowserSupport dbs) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(indexFile));
        String line;
        boolean rolling = true;
        while ((line = br.readLine()) != null) {
            String[] values = line.split("\t");
            if (values.length < 1)
                continue;
            DetailReport report = new DetailReport(build, values[0], dbs);
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
            detailReportMap.put(values[0], report);
        }
        br.close();
    }

    @Override
    public String getIconFileName() {
        return "/plugin/hp-application-automation-tools-plugin/PerformanceReport/LoadRunner.png";
    }

    @Override
    public String getDisplayName() {
        return "Performance Report";
    }

    @Override
    public String getUrlName() {
        return "PerformanceReport";
    }

    @SuppressWarnings("squid:S1452")
    public Run<?,?> getBuild() {
        return build;
    }

    public Map<String, DetailReport> getDetailReportMap() {
        return detailReportMap;
    }


    public Object getDynamic(String name, StaplerRequest req, StaplerResponse rsp) {
        if (detailReportMap.containsKey(name))
            return detailReportMap.get(name);
        return null;
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
//        List<Action> projectActions = new ArrayList<>();
//        projectActions.add(new PerformanceProjectAction(build.getParent()));
//        projectActions.add(new TestResultProjectAction(build.getParent()));
//        return projectActions;
        return Collections.emptySet();

    }
}
