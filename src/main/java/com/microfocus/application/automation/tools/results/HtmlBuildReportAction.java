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

import hudson.model.Action;
import hudson.model.Run;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
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
    private Run build;
    private List<ReportMetaData> reportMetaDataList;
    private Integer index;

    //NOTE: if parameter has BuildListener, the build cannot be serialize normally.
    public HtmlBuildReportAction(Run<?, ?> build, String reportName, Integer index) throws IOException, SAXException, ParserConfigurationException {
        this.build = build;

        File reportMetaData_XML = new File(build.getRootDir(), reportName);
        if (reportMetaData_XML.exists()) {
            this.reportMetaDataList = readReportFromXMLFile(reportMetaData_XML.getAbsolutePath());
        }
        this.index = index;
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
        return "uft-report" + (this.index != null ? "-" + this.index : "");
    }

    @Override
    public String getIconFileName() {
		return "/plugin/hp-application-automation-tools-plugin/icons/24x24/uft_report.png";
    }

    // other property of the report
    public List<ReportMetaData> getAllReports() {
        return reportMetaDataList;
    }

    public List<ReportMetaData> getReportMetaDataList(){
        return this.reportMetaDataList;
    }


   private List<ReportMetaData> readReportFromXMLFile(String filename) throws ParserConfigurationException, IOException, SAXException {
        List<ReportMetaData> listReport = new ArrayList<>();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(filename);

        Element root = doc.getDocumentElement();
        NodeList reportList = root.getElementsByTagName("report");
        for (int i = 0; i < reportList.getLength(); i++) {
            ReportMetaData reportmetadata = new ReportMetaData();
            Node nNode = reportList.item(i);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element report = (Element) nNode;
                String disPlayName = report.getAttribute("disPlayName");
                String urlName = report.getAttribute("urlName");
                String resourceURL = report.getAttribute("resourceURL");
                String dateTime = report.getAttribute("dateTime");
                String status = report.getAttribute("status");
                String isHtmlreport = report.getAttribute("isHtmlreport");
                String isParallelRunnerReport = report.getAttribute("isParallelRunnerReport");
                String archiveUrl = report.getAttribute("archiveUrl");

                reportmetadata.setDisPlayName(disPlayName);
                reportmetadata.setUrlName(urlName);
                reportmetadata.setResourceURL(resourceURL);
                reportmetadata.setDateTime(dateTime);
                reportmetadata.setStatus(status);
                reportmetadata.setIsHtmlReport("true".equals(isHtmlreport));
                reportmetadata.setIsParallelRunnerReport("true".equals(isParallelRunnerReport));
                reportmetadata.setArchiveUrl(archiveUrl);
                listReport.add(reportmetadata);
            }
        }

        return listReport;
    }



}
