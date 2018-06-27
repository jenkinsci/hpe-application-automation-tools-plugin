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
        String normalizedScriptRunName = this.name.replaceAll("[\\.|\\-|\\s|\\+]","_");
        return String.format("%s_%s", normalizedScriptRunName, this.yac);
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
