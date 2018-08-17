
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


import hudson.model.Action;
import hudson.model.Run;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by betzalel on 28/06/2015.
 */

public class HtmlBuildReportAction implements Action {
    private static final String REPORTMETADATE_XML = "report_metadata.xml";
    private Run build;
    private List<ReportMetaData> reportMetaDataList = new ArrayList<ReportMetaData>();


    //public HtmlBuildReportAction(AbstractBuild<?, ?> build, BuildListener listener, List<ReportMetaData> reportMetaData)
    //NOTE: if parameter has BuildListener, the build cannot be serilize normally.

    public HtmlBuildReportAction(Run<?, ?> build) throws IOException, SAXException, ParserConfigurationException {
        this.build = build;

        File reportMetaData_XML = new File(build.getRootDir(), REPORTMETADATE_XML);
        if (reportMetaData_XML.exists()) {
            readReportFromXMLFile(reportMetaData_XML.getAbsolutePath(), this.reportMetaDataList);
        }

    }

	@SuppressWarnings("squid:S1452")
    public final Run<?, ?> getBuild() {
        return build;
    }

    protected File reportFile() {
        return getBuildHtmlReport(this.build);
    }

    private File getBuildHtmlReport(Run run) {
		return new File(new File(new File(run.getRootDir(),"archive"), "UFTReport"), "index.html");
    }

    @Override
    public String getDisplayName() {
		return "UFT Report";
    }

    @Override
    public String getUrlName() {
        return "uft-report";
    }

    @Override
    public String getIconFileName() {
		return "/plugin/hp-application-automation-tools-plugin/icons/24x24/uft_report.png";
    }

    // other property of the report
    public List<ReportMetaData> getAllReports() {
        return this.reportMetaDataList;
    }


    private void readReportFromXMLFile(String filename, List<ReportMetaData> listReport) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        Document doc;
		builder = dbf.newDocumentBuilder();
		doc = builder.parse(filename);

        Element root = doc.getDocumentElement();
        NodeList reportList = root.getElementsByTagName("report");
        for (int i = 0; i < reportList.getLength(); i++) {
            ReportMetaData reportmetadata = new ReportMetaData();
            Element report = (Element) reportList.item(i);
            String disPlayName = report.getAttribute("disPlayName");
            String urlName = report.getAttribute("urlName");
            String resourceURL = report.getAttribute("resourceURL");
            String dateTime = report.getAttribute("dateTime");
            String status = report.getAttribute("status");
            String isHtmlreport = report.getAttribute("isHtmlreport");
            String isParallelRunnerReport = report.getAttribute("isParallelRunnerReport");

            reportmetadata.setDisPlayName(disPlayName);
            reportmetadata.setUrlName(urlName);
            reportmetadata.setResourceURL(resourceURL);
            reportmetadata.setDateTime(dateTime);
            reportmetadata.setStatus(status);
            reportmetadata.setIsHtmlReport("true".equals(isHtmlreport));
            reportmetadata.setIsParallelRunnerReport("true".equals(isParallelRunnerReport));
            listReport.add(reportmetadata);

        }
    }
}
