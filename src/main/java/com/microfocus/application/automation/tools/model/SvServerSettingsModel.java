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

import java.net.MalformedURLException;
import java.net.URL;

import com.hp.sv.jsvconfigurator.core.impl.processor.Credentials;
import hudson.util.Secret;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

public class SvServerSettingsModel {

    private final String name;
    private final String url;
    private final String username;
    private final Secret password;

    @DataBoundConstructor
    public SvServerSettingsModel(String name, String url, String username, Secret password) {
        this.name = StringUtils.trim(name);
        this.url = StringUtils.trim(url);
        this.username = username;
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public URL getUrlObject() throws MalformedURLException {
        return new URL(url);
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password.getPlainText();
    }

    public Credentials getCredentials() {
        if (StringUtils.isBlank(username) || password == null) {
            return null;
        }
        return new Credentials(username, password.getPlainText());
    }
}