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

import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

public class AlmServerSettingsModel {
    
    private final String _almServerName;
    private final String _almServerUrl;
    
    @DataBoundConstructor
    public AlmServerSettingsModel(String almServerName, String almServerUrl) {
        
        _almServerName = almServerName;
        _almServerUrl = almServerUrl;
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
    
    public Properties getProperties() {
        
        Properties prop = new Properties();
        if (!StringUtils.isEmpty(_almServerUrl)) {
            prop.put("almServerUrl", _almServerUrl);
        } else {
            prop.put("almServerUrl", "");
        }
        
        return prop;
    }
}
