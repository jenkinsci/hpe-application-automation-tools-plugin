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

import com.microfocus.application.automation.tools.EncryptionUtils;
import com.microfocus.application.automation.tools.mc.JobConfigurationProxy;
import hudson.EnvVars;
import hudson.util.Secret;
import hudson.util.VariableResolver;
import net.minidev.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Holds the data for RunFromFile build type.
 */
public class RunFromFileSystemModel {

    public static final String MOBILE_PROXY_SETTING_PASSWORD_FIELD = "MobileProxySetting_Password";
    public static final String MOBILE_PROXY_SETTING_USER_NAME = "MobileProxySetting_UserName";
    public static final String MOBILE_PROXY_SETTING_AUTHENTICATION = "MobileProxySetting_Authentication";
    public static final String MOBILE_USE_SSL = "MobileUseSSL";

    public final static EnumDescription FAST_RUN_MODE = new EnumDescription("Fast", "Fast");
    public final static EnumDescription NORMAL_RUN_MODE = new EnumDescription("Normal", "Normal");
    public final static List<EnumDescription> fsUftRunModes = Arrays.asList(FAST_RUN_MODE, NORMAL_RUN_MODE);

    private String fsTests;
    private String fsTimeout;
    private String fsUftRunMode;
    private String controllerPollingInterval;
    private String perScenarioTimeOut;
    private String ignoreErrorStrings;
    private String analysisTemplate;
    private String displayController;
    private String mcServerName;
    private String fsUserName;
    private Secret fsPassword;
    private String mcTenantId;

    private String fsDeviceId;
    private String fsOs;
    private String fsManufacturerAndModel;
    private String fsTargetLab;
    private String fsAutActions;
    private String fsLaunchAppName;
    private String fsInstrumented;
    private String fsDevicesMetrics;
    private String fsExtraApps;
    private String fsJobId;
    private ProxySettings proxySettings;
    private boolean useSSL;

    /**
     * Instantiates a new Run from file system model.
     *
     * @param fsTests                   the fs tests path
     * @param fsTimeout                 the fs timeout in minutes for tests in seconds
     * @param controllerPollingInterval the controller polling interval in minutes
     * @param perScenarioTimeOut        the per scenario time out in minutes
     * @param ignoreErrorStrings        the ignore error strings
     * @param analysisTemplate          the analysis template
     * @param displayController         the display controller
     * @param mcServerName              the mc server name
     * @param fsUserName                the fs user name
     * @param fsPassword                the fs password
     * @param fsDeviceId                the fs device id
     * @param fsTargetLab               the fs target lab
     * @param fsManufacturerAndModel    the fs manufacturer and model
     * @param fsOs                      the fs os
     * @param fsAutActions              the fs aut actions
     * @param fsLaunchAppName           the fs launch app name
     * @param fsDevicesMetrics          the fs devices metrics
     * @param fsInstrumented            the fs instrumented
     * @param fsExtraApps               the fs extra apps
     * @param fsJobId                   the fs job id
     * @param proxySettings             the proxy settings
     * @param useSSL                    the use ssl
     */
    @SuppressWarnings("squid:S00107")
    public RunFromFileSystemModel(String fsTests, String fsTimeout, String fsUftRunMode, String controllerPollingInterval,String perScenarioTimeOut,
                                  String ignoreErrorStrings, String analysisTemplate, String displayController, String mcServerName, String fsUserName, String fsPassword, String mcTenantId,
                                  String fsDeviceId, String fsTargetLab, String fsManufacturerAndModel, String fsOs,
                                  String fsAutActions, String fsLaunchAppName, String fsDevicesMetrics, String fsInstrumented,
                                  String fsExtraApps, String fsJobId, ProxySettings proxySettings, boolean useSSL) {

        this.setFsTests(fsTests);

        this.fsTimeout = fsTimeout;
        this.fsUftRunMode = fsUftRunMode;

        this.perScenarioTimeOut = perScenarioTimeOut;
        this.controllerPollingInterval = controllerPollingInterval;
        this.ignoreErrorStrings = ignoreErrorStrings;
        this.analysisTemplate = analysisTemplate;
        this.displayController = displayController;

        this.mcServerName = mcServerName;
        this.fsUserName = fsUserName;
        this.fsPassword = Secret.fromString(fsPassword);
        this.mcTenantId = mcTenantId;

        this.fsDeviceId = fsDeviceId;
        this.fsOs = fsOs;
        this.fsManufacturerAndModel = fsManufacturerAndModel;
        this.fsTargetLab = fsTargetLab;
        this.fsAutActions = fsAutActions;
        this.fsLaunchAppName = fsLaunchAppName;
        this.fsAutActions = fsAutActions;
        this.fsDevicesMetrics = fsDevicesMetrics;
        this.fsInstrumented = fsInstrumented;
        this.fsExtraApps = fsExtraApps;
        this.fsJobId = fsJobId;
        this.proxySettings = proxySettings;
        this.useSSL = useSSL;

    }


    /**
     * Instantiates a new file system model.
     *
     * @param fsTests the fs tests
     */
    @DataBoundConstructor
    public RunFromFileSystemModel(String fsTests) {
        this.setFsTests(fsTests);

        //Init default vals
        this.fsTimeout = "";
        this.fsUftRunMode = "Fast";
        this.controllerPollingInterval = "30";
        this.perScenarioTimeOut = "10";
        this.ignoreErrorStrings = "";
        this.displayController = "false";
        this.analysisTemplate = "";
    }

    /**
     * Sets fs tests.
     *
     * @param fsTests the fs tests
     */
    public void setFsTests(String fsTests) {
        this.fsTests = fsTests.trim();

        if (!this.fsTests.contains("\n")) {
            this.fsTests += "\n";
        }
    }

    /**
     * Sets fs timeout.
     *
     * @param fsTimeout the fs timeout
     */
    public void setFsTimeout(String fsTimeout) {
        this.fsTimeout = fsTimeout;
    }

    /**
     * Sets fs runMode.
     *
     * @param fsUftRunMode the fs runMode
     */
    public void setFsUftRunMode(String fsUftRunMode) {
        this.fsUftRunMode = fsUftRunMode;
    }

    /**
     * Sets mc server name.
     *
     * @param mcServerName the mc server name
     */
    public void setMcServerName(String mcServerName) {
        this.mcServerName = mcServerName;
    }

    /**
     * Sets fs user name.
     *
     * @param fsUserName the fs user name
     */
    public void setFsUserName(String fsUserName) {
        this.fsUserName = fsUserName;
    }

    /**
     * Sets fs password.
     *
     * @param fsPassword the fs password
     */
    public void setFsPassword(String fsPassword) {
        this.fsPassword = Secret.fromString(fsPassword);
    }

    /**
     * Sets fs device id.
     *
     * @param fsDeviceId the fs device id
     */
    public void setFsDeviceId(String fsDeviceId) {
        this.fsDeviceId = fsDeviceId;
    }

    /**
     * Sets fs os.
     *
     * @param fsOs the fs os
     */
    public void setFsOs(String fsOs) {
        this.fsOs = fsOs;
    }

    /**
     * Sets fs manufacturer and model.
     *
     * @param fsManufacturerAndModel the fs manufacturer and model
     */
    public void setFsManufacturerAndModel(String fsManufacturerAndModel) {
        this.fsManufacturerAndModel = fsManufacturerAndModel;
    }

    /**
     * Sets fs target lab.
     *
     * @param fsTargetLab the fs target lab
     */
    public void setFsTargetLab(String fsTargetLab) {
        this.fsTargetLab = fsTargetLab;
    }

    /**
     * Sets fs aut actions.
     *
     * @param fsAutActions the fs aut actions
     */
    public void setFsAutActions(String fsAutActions) {
        this.fsAutActions = fsAutActions;
    }

    /**
     * Sets fs launch app name.
     *
     * @param fsLaunchAppName the fs launch app name
     */
    public void setFsLaunchAppName(String fsLaunchAppName) {
        this.fsLaunchAppName = fsLaunchAppName;
    }

    /**
     * Sets fs instrumented.
     *
     * @param fsInstrumented the fs instrumented
     */
    public void setFsInstrumented(String fsInstrumented) {
        this.fsInstrumented = fsInstrumented;
    }

    /**
     * Sets fs devices metrics.
     *
     * @param fsDevicesMetrics the fs devices metrics
     */
    public void setFsDevicesMetrics(String fsDevicesMetrics) {
        this.fsDevicesMetrics = fsDevicesMetrics;
    }

    /**
     * Sets fs extra apps.
     *
     * @param fsExtraApps the fs extra apps
     */
    public void setFsExtraApps(String fsExtraApps) {
        this.fsExtraApps = fsExtraApps;
    }

    /**
     * Sets fs job id.
     *
     * @param fsJobId the fs job id
     */
    public void setFsJobId(String fsJobId) {
        this.fsJobId = fsJobId;
    }

    /**
     * Sets proxy settings.
     *
     * @param proxySettings the proxy settings
     */
    public void setProxySettings(ProxySettings proxySettings) {
        this.proxySettings = proxySettings;
    }

    /**
     * Sets use ssl.
     *
     * @param useSSL the use ssl
     */
    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    /**
     * Gets fs tests.
     *
     * @return the fs tests
     */
    public String getFsTests() {
        return fsTests;
    }

    /**
     * Gets fs timeout.
     *
     * @return the fs timeout
     */
    public String getFsTimeout() {
        return fsTimeout;
    }

    /**
     * Gets fs runMode.
     *
     * @return the fs runMode
     */
    public String getFsUftRunMode() {
        return fsUftRunMode;
    }

    /**
     * Gets fs runModes
     *
     * @return the fs runModes
     */
    public List<EnumDescription> getFsUftRunModes() { return fsUftRunModes; }

    /**
     * Gets mc server name.
     *
     * @return the mc server name
     */
    public String getMcServerName() {
        return mcServerName;
    }

    /**
     * Gets fs user name.
     *
     * @return the fs user name
     */
    public String getFsUserName() {
        return fsUserName;
    }

    /**
     * Gets fs password.
     *
     * @return the fs password
     */
    public String getFsPassword() {
        //Temp fix till supported in pipeline module in LR
        if(fsPassword == null)
        {
            return null;
        }
        return fsPassword.getPlainText();
    }

    public String getMcTenantId() {
        return mcTenantId;
    }

    public void setMcTenantId(String mcTenantId) {
        this.mcTenantId = mcTenantId;
    }

    /**
     * Gets fs device id.
     *
     * @return the fs device id
     */
    public String getFsDeviceId() {
        return fsDeviceId;
    }

    /**
     * Gets fs os.
     *
     * @return the fs os
     */
    public String getFsOs() {
        return fsOs;
    }

    /**
     * Gets fs manufacturer and model.
     *
     * @return the fs manufacturer and model
     */
    public String getFsManufacturerAndModel() {
        return fsManufacturerAndModel;
    }

    /**
     * Gets fs target lab.
     *
     * @return the fs target lab
     */
    public String getFsTargetLab() {
        return fsTargetLab;
    }

    /**
     * Gets fs aut actions.
     *
     * @return the fs aut actions
     */
    public String getFsAutActions() {
        return fsAutActions;
    }

    /**
     * Gets fs launch app name.
     *
     * @return the fs launch app name
     */
    public String getFsLaunchAppName() {
        return fsLaunchAppName;
    }

    /**
     * Gets fs instrumented.
     *
     * @return the fs instrumented
     */
    public String getFsInstrumented() {
        return fsInstrumented;
    }

    /**
     * Gets fs devices metrics.
     *
     * @return the fs devices metrics
     */
    public String getFsDevicesMetrics() {
        return fsDevicesMetrics;
    }

    /**
     * Gets fs extra apps.
     *
     * @return the fs extra apps
     */
    public String getFsExtraApps() {
        return fsExtraApps;
    }

    /**
     * Gets fs job id.
     *
     * @return the fs job id
     */
    public String getFsJobId() {
        return fsJobId;
    }

    /**
     * Is use proxy boolean.
     *
     * @return the boolean
     */
    public boolean isUseProxy() {
        return proxySettings != null;
    }

    /**
     * Is use authentication boolean.
     *
     * @return the boolean
     */
    public boolean isUseAuthentication() {
        return proxySettings != null && StringUtils.isNotBlank(proxySettings.getFsProxyUserName());
    }

    /**
     * Gets proxy settings.
     *
     * @return the proxy settings
     */
    public ProxySettings getProxySettings() {
        return proxySettings;
    }

    /**
     * Is use ssl boolean.
     *
     * @return the boolean
     */
    public boolean isUseSSL() {
        return useSSL;
    }


    /**
     * Gets controller polling interval.
     *
     * @return the controllerPollingInterval
     */
    public String getControllerPollingInterval() {
        return controllerPollingInterval;
    }

    /**
     * Sets controller polling interval.
     *
     * @param controllerPollingInterval the controllerPollingInterval to set
     */
    public void setControllerPollingInterval(String controllerPollingInterval) {
        this.controllerPollingInterval = controllerPollingInterval;
    }

    /**
     * Gets analysis template.
     *
     * @return the analysis template
     */
    public String getAnalysisTemplate() {
        return analysisTemplate;
    }

    /**
     * Sets analysis template.
     *
     * @param analysisTemplate the analysis template
     */
    public void setAnalysisTemplate(String analysisTemplate) {
        this.analysisTemplate = analysisTemplate;
    }

    /**
     * Gets display controller.
     *
     * @return the displayController
     */
    public String getDisplayController() {
        return displayController;
    }

    /**
     * Sets display controller.
     *
     * @param displayController the displayController to set
     */
    public void setDisplayController(String displayController) {
        this.displayController = displayController;
    }

    /**
     * Gets ignore error strings.
     *
     * @return the ignoreErrorStrings
     */
    public String getIgnoreErrorStrings() {
        return ignoreErrorStrings;
    }


    /**
     * Sets ignore error strings.
     *
     * @param ignoreErrorStrings the ignoreErrorStrings to set
     */
    public void setIgnoreErrorStrings(String ignoreErrorStrings) {
        this.ignoreErrorStrings = ignoreErrorStrings;
    }


    /**
     * Gets per scenario time out.
     *
     * @return the perScenarioTimeOut
     */
    public String getPerScenarioTimeOut() {
        return perScenarioTimeOut;
    }

    /**
     * Sets per scenario time out.
     *
     * @param perScenarioTimeOut the perScenarioTimeOut to set
     */
    public void setPerScenarioTimeOut(String perScenarioTimeOut) {
        this.perScenarioTimeOut = perScenarioTimeOut;
    }


	/**
	 * Gets properties.
	 *
	 * @param envVars     the env vars
	 * @return the properties
	 */
	@Nullable
	public Properties getProperties(EnvVars envVars,
									VariableResolver<String> varResolver) {
		return createProperties(envVars, varResolver);
	}

	private Properties createProperties(EnvVars envVars,
										VariableResolver<String> varResolver) {

		return createProperties(envVars);
	}

    /**
     * Gets properties.
     *
     * @param envVars     the env vars
     * @return the properties
     */
	@Nullable
	public Properties getProperties(EnvVars envVars) {
        return createProperties(envVars);
    }

    /**
     * Gets properties.
     *
     * @return the properties
     */
    public Properties getProperties() {
        return createProperties(null);
    }

    private Properties createProperties(EnvVars envVars) {
        Properties props = new Properties();

        if (!StringUtils.isEmpty(this.fsTests)) {
            String expandedFsTests = envVars.expand(fsTests);
            String[] testsArr;
            if (isMtbxContent(expandedFsTests)) {
                testsArr = new String[]{expandedFsTests};
            } else {
                testsArr = expandedFsTests.replaceAll("\r", "").split("\n");
            }

            int i = 1;

            for (String test : testsArr) {
                test = test.trim();
                props.put("Test" + i, test);
                i++;
            }
        } else {
            props.put("fsTests", "");
        }


        if (StringUtils.isEmpty(fsTimeout)){
            props.put("fsTimeout", "-1");
        }
        else{
            props.put("fsTimeout", "" + envVars.expand(fsTimeout));
        }

        if (StringUtils.isEmpty(fsUftRunMode)){
            props.put("fsUftRunMode", "Fast");
        }
        else{
            props.put("fsUftRunMode", "" + fsUftRunMode);
        }

        if (StringUtils.isEmpty(controllerPollingInterval)){
            props.put("controllerPollingInterval", "30");
        }
        else{
            props.put("controllerPollingInterval", "" + controllerPollingInterval);
        }

        if (StringUtils.isEmpty(analysisTemplate)) {
            props.put("analysisTemplate", "");
        } else{
            props.put("analysisTemplate", analysisTemplate);
        }

       if (StringUtils.isEmpty(displayController) || displayController.equals("false")){
            props.put("displayController", "0");
        }
        else{
            props.put("displayController", "1");
        }

        if (StringUtils.isEmpty(perScenarioTimeOut)){
            props.put("PerScenarioTimeOut", "10");
        }
        else{
            props.put("PerScenarioTimeOut", ""+ envVars.expand(perScenarioTimeOut));
        }

        if (!StringUtils.isEmpty(ignoreErrorStrings.replaceAll("\\r|\\n", ""))){
            props.put("ignoreErrorStrings", ""+ignoreErrorStrings.replaceAll("\r", ""));
        }

        if (StringUtils.isNotBlank(fsUserName)){
            props.put("MobileUserName", fsUserName);
        }
        if (StringUtils.isNotBlank(mcTenantId)){
            props.put("MobileTenantId", mcTenantId);
        }

        if(isUseProxy()){
            props.put("MobileUseProxy", "1");
            props.put("MobileProxyType","2");
            props.put("MobileProxySetting_Address", proxySettings.getFsProxyAddress());

            if(isUseAuthentication()){
                props.put(MOBILE_PROXY_SETTING_AUTHENTICATION,"1");
                props.put(MOBILE_PROXY_SETTING_USER_NAME,proxySettings.getFsProxyUserName());
                String encryptedPassword;

                try {
                    encryptedPassword = EncryptionUtils.Encrypt(proxySettings.getFsProxyPassword(),
                            EncryptionUtils.getSecretKey());
                }catch (Exception ex) {
                    return null; // cannot continue without proper config
                }

                props.put(MOBILE_PROXY_SETTING_PASSWORD_FIELD, encryptedPassword);
            }else{
                props.put(MOBILE_PROXY_SETTING_AUTHENTICATION,"0");
                props.put(MOBILE_PROXY_SETTING_USER_NAME,"");
                props.put(MOBILE_PROXY_SETTING_PASSWORD_FIELD,"");
            }
        }else{
            props.put("MobileUseProxy", "0");
            props.put("MobileProxyType","0");
            props.put(MOBILE_PROXY_SETTING_AUTHENTICATION,"0");
            props.put("MobileProxySetting_Address", "");
            props.put(MOBILE_PROXY_SETTING_USER_NAME,"");
            props.put(MOBILE_PROXY_SETTING_PASSWORD_FIELD,"");
        }

        if(useSSL){
            props.put(MOBILE_USE_SSL,"1");
        }else{
            props.put(MOBILE_USE_SSL,"0");
        }

        return props;
    }

    public static boolean isMtbxContent(String testContent) {
        return testContent.toLowerCase().contains("<mtbx>");
    }

    /**
     * Get proxy details json object.
     *
     * @param mcUrl         the mc url
     * @param proxyAddress  the proxy address
     * @param proxyUserName the proxy user name
     * @param proxyPassword the proxy password
     * @return the json object
     */
    public JSONObject getJobDetails(String mcUrl, String proxyAddress, String proxyUserName, String proxyPassword){
        if(StringUtils.isBlank(fsUserName) || StringUtils.isBlank(fsPassword.getPlainText())){
            return null;
        }
        return JobConfigurationProxy
                .getInstance().getJobById(mcUrl, fsUserName, fsPassword.getPlainText(), mcTenantId, proxyAddress, proxyUserName, proxyPassword, fsJobId);
    }
}
