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

package com.microfocus.application.automation.tools.results.lrscriptresultparser;

import hudson.model.Action;
import hudson.model.Run;
import jenkins.util.VirtualFile;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by kazaky on 27/03/2017.
 */

/**
 * Presents the LR script HTML report
 */
public class LrScriptHtmlReportAction implements Action {
    private final VirtualFile basePath;
    private Run build;
    private HashSet<String> scripts = new HashSet<String>();
    private List<LrScriptHtmlReport> reportMetaDataList = new ArrayList<LrScriptHtmlReport>();

    public LrScriptHtmlReportAction(Run<?, ?> build) {
        this.build = build;
        this.basePath = build.getArtifactManager().root().child(LrScriptHtmlReport.LR_REPORT_FOLDER);
    }

    @SuppressWarnings("squid:S1452")
    public final Run<?, ?> getBuild() {
        return build;
    }

    protected File reportFile() {
        return getBuildHtmlReport();
    }

    private File getBuildHtmlReport() {
        return new File(basePath.child("index.html").toURI());
    }

    @Override
    public String getIconFileName() {
        return "/plugin/hp-application-automation-tools-plugin/PerformanceReport/VuGen.png";
    }

    @Override
    public String getDisplayName() {
        return "LR script report";
    }

    @Override
    public String getUrlName() {
        return "LRReport";
    }

    // other property of the report
    public List<LrScriptHtmlReport> getAllReports() {
        return this.reportMetaDataList;
    }

    public void mergeResult(Run<?, ?> build, String scriptName) {
            if (this.scripts == null) {
                this.reportMetaDataList = new ArrayList<LrScriptHtmlReport>();
                this.scripts = new HashSet<String>();
            }
            this.scripts.add(scriptName);
            String scriptResultPath = build.getArtifactManager().root().child("LRReport").child(scriptName).toString();
            LrScriptHtmlReport lrScriptHtmlReport = new LrScriptHtmlReport(scriptName,"/result.html", scriptResultPath);
            this.reportMetaDataList.add(lrScriptHtmlReport);
    }


}
