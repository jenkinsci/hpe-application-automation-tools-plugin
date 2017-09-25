package com.hpe.application.automation.tools.model;

import com.hpe.application.automation.tools.settings.SrfServerSettingsBuilder;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Properties;

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

        SrfServerSettingsBuilder mdl = new SrfServerSettingsBuilder();
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

    private Properties getProperties() {

        Properties prop = new Properties();
        if (!StringUtils.isEmpty(srfProxyName)) {
            prop.put("srfProxyName", srfProxyName);
        } else {
            prop.put("srfProxyName", "");
        }

        return prop;
    }
}
