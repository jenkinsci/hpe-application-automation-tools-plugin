/*
 * © Copyright 2013 EntIT Software LLC
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright (c) 2018 Micro Focus Company, L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
