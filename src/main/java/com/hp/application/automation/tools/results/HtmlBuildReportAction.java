package com.hp.application.automation.tools.results;


import hudson.model.Action;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
//import hudson.model.ProminentProjectAction;
import hudson.remoting.Channel;

import java.io.File;
import java.util.List;


/**
 * Created by betzalel on 28/06/2015.
 */

public class HtmlBuildReportAction implements Action {
    //private HtmlPublisherTarget actualHtmlPublisherTarget;

    private AbstractBuild build;
    private BuildListener listener;

    //private String dateTime;

    private List<ReportMetaData> reportMetaData;

    public HtmlBuildReportAction(AbstractBuild<?, ?> build, BuildListener listener, List<ReportMetaData> reportMetaData) {
        //super(actualHtmlPublisherTarget);
        this.build = build;
        this.listener = listener;
        this.reportMetaData = reportMetaData;
    }

    public HtmlBuildReportAction(AbstractBuild<?, ?> build, BuildListener listener) {
        //super(actualHtmlPublisherTarget);
        this.build = build;
        this.listener = listener;
        //this.dateTime = dateTime;
    }

    public final AbstractBuild<?, ?> getOwner() {
        return build;
    }

//    protected String getTitle() {
//        return this.build.getDisplayName() + " UFTReport";
//    }

    protected File reportFile() {
        return getBuildHtmlReport(this.build);
    }

    private File getBuildHtmlReport(Run run) {

        File reportFile = new File(new File(run.getArtifactsDir(), "UFTReport"), "index.html");

        listener.getLogger().println("HtmlReportFile: " + reportFile.toString());
        return reportFile;
    }

    public String getDisplayName() {
        String displayName = "UFT Report";//this.build.getDisplayName() +
        //return reportFile().exists() ? displayName : null;
        return displayName;
    }

    public String getUrlName() {


        //test
        return "uft-report";
        //testend
//        //String url = this.build.getUrl() + "/UFTReport";
//        String url = "artifact/UFTReport/index.html";
//        return reportFile().exists() ? url : null;
//        //return url;
    }

    public String getIconFileName() {

        //return reportFile().exists() ? "graph.gif" : null;
        return "graph.gif";
    }

    // other property of the report
    public List<ReportMetaData> getAllReports(){
        return this.reportMetaData;
    }
//
//    /**
//     * Serves HTML reports.
//     */
//    public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
//        DirectoryBrowserSupport dbs = new DirectoryBrowserSupport(this, new FilePath(this.dir()), this.getTitle(), "graph.gif", false);
//        dbs.setIndexFileName(HtmlPublisherTarget.this.wrapperName); // Hudson >= 1.312
//        dbs.generateResponse(req, rsp, this);
//    }
}