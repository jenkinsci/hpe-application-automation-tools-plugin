/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2019 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors ("Micro Focus") are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;


public class AlmServerSettingsModel extends AbstractDescribableImpl<AlmServerSettingsModel> {
    
    private final String _almServerName;
    private final String _almServerUrl;
    private List<CredentialsModel> _almCredentials;
    private List<SSOCredentialsModel> _almSSOCredentials;

    @DataBoundConstructor
    public AlmServerSettingsModel(String almServerName, String almServerUrl,
                                  List<CredentialsModel> almCredentials,
                                  List<SSOCredentialsModel> almSSOCredentials) {
        
        this._almServerName = almServerName.trim();
        this._almServerUrl = almServerUrl.trim();
        this._almCredentials = almCredentials;
        this._almSSOCredentials = almSSOCredentials;
    }

    /**
     * @return the almServerName
     */
    public String getAlmServerName() {
        
        return _almServerName;
    }
    
    /**
     * @return the almServerUrl
     */
    public String getAlmServerUrl() {
        
        return _almServerUrl;
    }

    public List<CredentialsModel> getAlmCredentials(){
        if(_almCredentials != null) {
            return _almCredentials;
        }

        return new ArrayList<>();
    }

    public void set_almCredentials(List<CredentialsModel> almCredentials){
        this._almCredentials = almCredentials;
    }

    public List<SSOCredentialsModel> getAlmSSOCredentials() {
        if (_almSSOCredentials != null) {
            return _almSSOCredentials;
        }

        return new ArrayList<>();
    }

    public void set_almSSOCredentials(List<SSOCredentialsModel> almSSOCredentials){
        this._almSSOCredentials = almSSOCredentials;
    }

    public Properties getProperties() {
        
        Properties prop = new Properties();

        if (!StringUtils.isEmpty(_almServerUrl)) {
            prop.put("almServerUrl", _almServerUrl.trim());
        } else {
            prop.put("almServerUrl", "");
        }
        return prop;
    }


    @Extension
    public static class DescriptorImpl extends Descriptor<AlmServerSettingsModel> {
        @Nonnull
        @Override
        public String getDisplayName() {
            return "Alm Server Settings Model";
        }
    }
}
