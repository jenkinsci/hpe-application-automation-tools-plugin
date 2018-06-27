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
