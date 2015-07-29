package com.hp.application.automation.tools.results;

/**
 * Created by xueli on 7/23/2015.
 */
public class ReportMetaData {



    private String folderPath;  //slave path of report folder(only for html report format)
    private String disPlayName;
    private String urlName;
    private String resourceURL;
    private String dateTime;
    private String status;
    private Boolean isHtmlReport;

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

    public String getDateTime() {
        return dateTime;
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
}
