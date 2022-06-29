/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2021 Micro Focus or one of its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.results;

import com.microfocus.application.automation.tools.commonResultUpload.xmlreader.XpathReader;
import com.microfocus.application.automation.tools.settings.RunnerMiscSettingsGlobalConfiguration;
import hudson.FilePath;
import hudson.model.TaskListener;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.HashSet;

import static com.microfocus.application.automation.tools.settings.RunnerMiscSettingsGlobalConfiguration.*;

public class ReportMetaData {

    private String folderPath;  //slave path of report folder(only for html report format)
    private String disPlayName;
    private String urlName;
    private String resourceURL;
    private String dateTime;
    private String status;
    private Boolean isHtmlReport;
    private Boolean isParallelRunnerReport;
    private String  archiveUrl;
    private final Set<String> stResFolders = new HashSet<>();
    private static final String RUN_API_TEST_XPATH_EXPRESSION = "//Data[Name='RunAPITest']/Extension/StepCustomData";
    private static final String FAILED_TO_PROCESS_XML_REPORT = "Failed to process run_results.xml report: ";
    private static final String ST_RES = "..\\StRes";

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public String getDisPlayName() {
        return disPlayName;
    }

    public void setDisPlayName(String disPlayName) {
        this.disPlayName = disPlayName;
    }

    public String getUrlName() {
        return urlName;
    }

    public void setUrlName(String urlName) {
        this.urlName = urlName;
    }

    public String getResourceURL() {
        return resourceURL;
    }

    public void setResourceURL(String resourceURL) {
        this.resourceURL = resourceURL;
    }

    private static LocalDateTime tryParseDate(String date) {
        for (String pattern : DEFAULT_UFT_DATE_PATTERNS) {
            try {
                return LocalDateTime.parse(date, DateTimeFormatter.ofPattern(pattern));
            } catch (DateTimeException | IllegalArgumentException ignored) {
                // ignoring, trying to find appropriate date pattern for date string
            }
        }

        return null;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getFormattedDateTime() {
        // there is a global configuration option to set the date format for RunResults, we format the received date at the last second
        // because this way we can keep the default UFT formatting, nonetheless how users specify their own format
        LocalDateTime dt = tryParseDate(dateTime);
        if (dt == null) return dateTime;

        try {
            return dt.format(RunnerMiscSettingsGlobalConfiguration.getInstance().getDateFormatter());
        } catch (NullPointerException ignored) {
            return dateTime;
        }
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getIsHtmlReport() {
        return isHtmlReport;
    }

    public void setIsHtmlReport(Boolean isHtmlReport) {
        this.isHtmlReport = isHtmlReport;
    }

    public Boolean getIsParallelRunnerReport() {
        return isParallelRunnerReport;
    }

    public void setIsParallelRunnerReport(Boolean parallelRunnerReport) { isParallelRunnerReport = parallelRunnerReport; }

    public String getArchiveUrl() {
        return archiveUrl;
    }

    public void setArchiveUrl(String archiveUrl) {
        this.archiveUrl = archiveUrl;
    }

    public boolean hasArchiveUrl() {
        return archiveUrl != null && !archiveUrl.equals("");
    }

    public Set<String> getStResFolders() {
        return stResFolders;
    }

    public void computeStResFolders(FilePath xmlReport, TaskListener listener) throws InterruptedException {
        try {
            if (xmlReport.exists()) {
                XpathReader xr = new XpathReader(xmlReport);
                NodeList nodes = xr.getNodeListFromNode(RUN_API_TEST_XPATH_EXPRESSION, xr.getDoc());
                for (int x = 0; x < nodes.getLength(); x++) {
                    String val = nodes.item(x).getTextContent();
                    if (val.startsWith(ST_RES)) {
                        stResFolders.add(val.substring(3));
                    }
                }
            }
        } catch(NullPointerException | XPathExpressionException | IOException | SAXException | ParserConfigurationException e) {
            listener.error(FAILED_TO_PROCESS_XML_REPORT + e);
        }
    }
}
