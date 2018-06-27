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

package com.microfocus.application.automation.tools.model;

import hudson.util.Secret;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jingwei
 * Date: 5/17/16
 * Time: 4:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class UploadAppModel {
    private String mcServerName;
    private String mcUserName;
    private Secret mcPassword;
    private String mcTenantId;
    private ProxySettings proxySettings;
    private List<UploadAppPathModel> applicationPaths;

    @DataBoundConstructor
    public UploadAppModel(String mcServerName, String mcUserName, String mcPassword, String mcTenantId, ProxySettings proxySettings, List<UploadAppPathModel> applicationPaths) {
        this.mcServerName = mcServerName;
        this.mcUserName = mcUserName;
        this.mcPassword = Secret.fromString(mcPassword);
        this.mcTenantId = mcTenantId;
        this.proxySettings = proxySettings;
        this.applicationPaths = applicationPaths;
    }

    public String getMcServerName() {
        return mcServerName;
    }

    public String getMcUserName() {
        return mcUserName;
    }

    public String getMcPassword() {
        return mcPassword.getPlainText();
    }

    public String getMcTenantId() {
        return mcTenantId;
    }

    public ProxySettings getProxySettings() {
        return proxySettings;
    }

    public boolean isUseProxy() {
        return proxySettings != null;
    }

    public boolean isUseAuthentication() {
        return proxySettings != null && StringUtils.isNotBlank(proxySettings.getFsProxyUserName());
    }

    public List<UploadAppPathModel> getApplicationPaths() {
        return applicationPaths;
    }
}
