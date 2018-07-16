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

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by shepshel on 20/07/2016.
 */
public class SrfServerSettingsModel {
    private final String srfTunnelPath;
    private final String srfServerName;
    private final String srfProxyName;
    private  final String srfAppName;
    private  final String srfSecretName;

    @DataBoundConstructor
    public SrfServerSettingsModel(String srfTunnelPath, String srfServerName, String srfProxyName,String srfAppName, String srfSecretName ) {
        this.srfTunnelPath = srfTunnelPath;
        this.srfServerName = srfServerName;
        this.srfProxyName = srfProxyName;
        this.srfAppName = srfAppName;
        this.srfSecretName = srfSecretName;
    }

    public String getSrfServerName() {
        return srfServerName;
    }
    public String getSrfTunnelPath(){
        return srfTunnelPath;
    }
    public String getSrfAppName() {
        return srfAppName;
    }
    public String getSrfSecretName() {
        return srfSecretName;
    }
    public String getSrfProxyName() {
        return srfProxyName;
    }

}
