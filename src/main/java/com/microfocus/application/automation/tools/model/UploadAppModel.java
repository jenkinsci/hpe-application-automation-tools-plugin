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
