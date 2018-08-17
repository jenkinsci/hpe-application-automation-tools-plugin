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

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.util.Secret;

/**
 * This model is for sse build step's proxy setting.
 * It's different from the class ProxySettings. Here we use credentials instead of name/password.
 * @author llu4
 *
 */
public class SseProxySettings {
    private String fsProxyAddress;
    private String fsProxyCredentialsId;
    
    /**
     * To store the user name which get from the credentials.
     * Is set in sseBuilder while performing.
     */
    private String fsProxyUserName;
    
    /**
     * To store the password which get from the credentials.
     * Is set in sseBuilder while performing.
     */
    private Secret fsProxyPassword;

    /**
     * These two variables are set directly by the jelly form.
     */
    @DataBoundConstructor
    public SseProxySettings(String fsProxyAddress, String fsProxyCredentialsId) {
        this.fsProxyAddress = fsProxyAddress;
        this.fsProxyCredentialsId = fsProxyCredentialsId;
    }

    public String getFsProxyAddress() {
        return fsProxyAddress;
    }

    public String getFsProxyCredentialsId() {
        return fsProxyCredentialsId;
    }

	public String getFsProxyUserName() {
		return fsProxyUserName;
	}

	public void setFsProxyUserName(String fsProxyUserName) {
		this.fsProxyUserName = fsProxyUserName;
	}

	public Secret getFsProxyPassword() {
		return fsProxyPassword;
	}

	public void setFsProxyPassword(Secret fsProxyPassword) {
		this.fsProxyPassword = fsProxyPassword;
	}

}
