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

/**
 * Created by kazaky on 28/03/2017.
 */

/**
 * Stores the information on html report for specific report
 */
@SuppressWarnings("squid:S1068")
public class LrScriptHtmlReport {
    public static final String LR_REPORT_FOLDER = "LRReport";
    public static final  String BASE_LR_REPORT_URL = "artifact/LRReport/";
    private final String scriptLocalPath; //Created for future use and allow backward compatibility
    private String scriptName;
    private String htmlUrl;
    private String scriptFolderPath;

    /**
     * Instantiates a new Lr script html report.
     *
     * @param scriptName the script name
     * @param htmlUrl    the html url
     */
    public LrScriptHtmlReport(String scriptName, String htmlUrl, String scriptLocalPath) {
        this.scriptName = scriptName;
        this.scriptFolderPath = BASE_LR_REPORT_URL + scriptName;
        this.htmlUrl = scriptFolderPath + htmlUrl;
        this.scriptLocalPath = scriptLocalPath;
    }

    /**
     * Gets script name.
     *
     * @return the script name
     */
    public String getScriptName() {
        return scriptName;
    }

    /**
     * Sets script name.
     *
     * @param scriptName the script name
     */
    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    /**
     * Gets html url.
     *
     * @return the html url
     */
    public String getHtmlUrl() {
        return htmlUrl;
    }

    /**
     * Sets html url.
     *
     * @param htmlUrl the html url
     */
    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    /**
     * Gets script folder path.
     *
     * @return the script folder path
     */
    public String getScriptFolderPath() {
        return this.scriptFolderPath;
    }

    /**
     * Sets script folder path.
     *
     * @param scriptFolderPath the script folder path
     */
    public void setScriptFolderPath(String scriptFolderPath) {
        this.scriptFolderPath = scriptFolderPath;
    }
}
