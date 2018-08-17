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

package com.microfocus.application.automation.tools.srf.model;

import net.sf.json.JSONObject;

/**
 * This class represents SRF's script run type interface
 */
public class SrfScriptRunModel {

    public String id;
    public String name;
    public String start;
    public String yac;
    public JSONObject parent;
    public int durationMs;
    public SrfStatus status;
    public Environment environment;

    public SrfScriptRunModel(String id, String name, int durationMs, SrfStatus status, JSONObject environment, String yac, JSONObject parent){
        this.id = id;
        this.name = name;
        this.durationMs = durationMs;
        this.yac = yac;
        this.parent = parent;
        this.status = status;
        JSONObject os = environment.getJSONObject("os");
        JSONObject browser = environment.getJSONObject("browser");
        this.environment = new Environment(os.getString("name"), os.getString("version"), browser.getString("name"), browser.getString("version"));
    }

    public int getDuration() {
        return durationMs / 1000;
    }

    public SrfStatus getStatus() {
        return status;
    }

    public String getFullName() {
        return String.format("%s.%s", this.parent.getString("name"), this.name);
    }

    public String getLinkName() {
        String normalizedScriptRunName = SrfScriptRunModel.normalizeName(this.name);
        return String.format("%s_%s", normalizedScriptRunName, this.yac);
    }

    public static String normalizeName(String name) {
        return name.replaceAll("[\\.|\\-|\\s|\\+]","_");
    }

    public class NameVersion {
        public String name;
        public String version;

        public NameVersion(String name, String version){
            this.name = name;
            this.version = version;
        }
    }

    public class Environment {
        public NameVersion os;
        public NameVersion browser;
        public String resolution;
        public NameVersion platform;
        public String deviceName;
        public String deviceType;
        public String envType;
        
        public Environment(NameVersion os, NameVersion browser){
            this.browser = browser;
            this.os = os;
        }
        public Environment(String osName, String osVersion, String browserName, String browserVersion){
            this.browser = new NameVersion(browserName, browserVersion);
            this.os = new NameVersion(osName, osVersion);
        }

        @Override
        public String toString() {
            return String.format("%1s %1s %1s %1s", this.os.name, this.os.version, this.browser.name, this.browser.version);
        }
    }

    public enum SrfStatus {
        success("Success", "result-passed"),
        errored("Error", "result-failed"),
        failed("Failed", "result-failed"),
        discarded("Discarded", "result-skipped"),
        cancelled("Cancelled", "result-skipped"),
        completed("Completed", "result-passed"),
        pending("Pending", "result-skipped");

        private final String text;
        private final String cssClass;

        private SrfStatus(final String text, String cssClass) {
            this.text = text;
            this.cssClass = cssClass;
        }

        @Override
        public String toString() {
            return text;
        }
        public String getCssClass() {
            return this.cssClass;
        }
    }

}
