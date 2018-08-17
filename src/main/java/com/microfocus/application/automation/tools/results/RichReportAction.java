
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


public class RichReportAction implements Action, SimpleBuildStep.LastBuildAction {

    private static final String RICH_REPORT_FOLDER = "RichReport";
    private static final String REPORT_INDEX = "report.index";

    private Map<String, DetailReport> richReportMap = new LinkedHashMap<String, DetailReport>();
    private final List<TestResultProjectAction> projectActionList;


    private final Run<?,?> build;

    public RichReportAction(Run<?,?> build) throws IOException {
        this.build = build;
        File reportFolder = new File(build.getRootDir(), RICH_REPORT_FOLDER);
        if (reportFolder.exists()) {
            File indexFile = new File(reportFolder, REPORT_INDEX);
            if (indexFile.exists()) {
                File file = new File(build.getRootDir(), RICH_REPORT_FOLDER);
                DirectoryBrowserSupport dbs = new DirectoryBrowserSupport(this, new FilePath(file), "report", "graph.gif", false);


                createRichIndexFile(build, indexFile, dbs);
            }
        }
        projectActionList = new ArrayList<TestResultProjectAction>();
    }

    /**
     * Reads data from report.index and displays a table with:
     * scenario name, duration, transactions passed and failed
     */
    private void createRichIndexFile(Run<?, ?> build, File indexFile, DirectoryBrowserSupport dbs) throws IOException {
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
            //Set detailed report information
            switch (values.length) {
                case 4:
                    report.updateReport(values[1], values[2], values[3]);
                    break;
                case 3:
                    report.updateReport(values[1], values[2], "##");
                    break;
                case 2:
                    report.updateReport(values[1], "##", "##");
            } 
            richReportMap.put(values[0], report);
        }
        br.close();
    }

    @Override
    public String getIconFileName() {
        return "/plugin/hp-application-automation-tools-plugin/PerformanceReport/LoadRunner.png";
    }

    @Override
    public String getDisplayName() {
        return "Rich Report";
    }

    @Override
    public String getUrlName() {
        return "RichReport";
    }

    @SuppressWarnings("squid:S1452")
    public Run<?,?> getBuild() {
        return build;
    }

    public Map<String, DetailReport> getRichReportMap() {
        return richReportMap;
    }


    public Object getDynamic(String name, StaplerRequest req, StaplerResponse rsp) {
        if (richReportMap.containsKey(name))
            return richReportMap.get(name);
        return null;
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        return Collections.emptySet();

    }
}
