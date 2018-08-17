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

/*
* Takes all the parameter from the job in order to create a loadtest object
* */
package com.microfocus.application.automation.tools.model;

import java.util.Arrays;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

import com.microfocus.adm.performancecenter.plugins.common.pcEntities.*;

public class PcModel {

    public static final String    COLLATE         = "Collate Results";
    public static final String    COLLATE_ANALYZE = "Collate and Analyze";
    public static final String    DO_NOTHING      = "Do Not Collate";
    private static final String EXPECTED_ALMPASSWORD_PARAMETER_NAME = "PCPASSWORD";

    private final String           serverAndPort;
    private final String                 pcServerName;
    private final String                 almUserName;
    private final SecretContainer        almPassword;
    private final String                 almDomain;
    private final String                 almProject;
    private final String                 testId;
    private String                 testInstanceId;
    private final String           autoTestInstanceID;
    private final TimeslotDuration timeslotDuration;
    private final PostRunAction    postRunAction;
    private final boolean          vudsMode;
    private final String           description;
    private final String          addRunToTrendReport;
    private String trendReportId;
    private final boolean HTTPSProtocol;
    private final String proxyOutURL;
    private final String proxyOutUser;
    private final String proxyOutPassword;
    private String buildParameters;


    @DataBoundConstructor
    public PcModel(String serverAndPort, String pcServerName, String almUserName, String almPassword, String almDomain, String almProject,
                   String testId,String autoTestInstanceID, String testInstanceId, String timeslotDurationHours, String timeslotDurationMinutes,
                   PostRunAction postRunAction, boolean vudsMode, String description, String addRunToTrendReport, String trendReportId, boolean HTTPSProtocol, String proxyOutURL, String proxyOutUser, String proxyOutPassword) {

        this.serverAndPort = serverAndPort;
        this.pcServerName = pcServerName;
        this.almUserName = almUserName;
        this.almPassword = setPassword(almPassword);
        this.almDomain = almDomain;
        this.almProject = almProject;
        this.testId = testId;
        this.autoTestInstanceID = autoTestInstanceID;
        this.testInstanceId = testInstanceId;
        this.timeslotDuration = new TimeslotDuration(timeslotDurationHours, timeslotDurationMinutes);
        this.postRunAction = postRunAction;
        this.vudsMode = vudsMode;
        this.description = description;
        this.addRunToTrendReport = addRunToTrendReport;
        this.HTTPSProtocol = HTTPSProtocol;
        this.trendReportId = trendReportId;
        this.proxyOutURL = proxyOutURL;
        this.proxyOutUser = proxyOutUser;
        this.proxyOutPassword = proxyOutPassword;
        this.buildParameters="";

    }

    protected SecretContainer setPassword(String almPassword) {

        SecretContainer secretContainer = new SecretContainerImpl();
        secretContainer.initialize(almPassword);
        return secretContainer;
    }

    public String getserverAndPort(){
        return this.serverAndPort;
    }

    public String getPcServerName() {

        return this.pcServerName;
    }

    public String getPcServerName(boolean fromPcClient) {

        return fromPcClient?useParameterIfNeeded(buildParameters,this.pcServerName):getPcServerName();
    }

    public String getAlmUserName() {

        return this.almUserName;
    }

    public String getAlmUserName(boolean fromPcClient) {

        return fromPcClient?useParameterIfNeeded(buildParameters,this.almUserName):getAlmUserName();
    }

    public SecretContainer getAlmPassword() {

        return this.almPassword;
    }

    public SecretContainer getAlmPassword(boolean fromPcClient) {

        return fromPcClient?useParameterForAlmPasswordIfNeeded(buildParameters,this.almPassword, EXPECTED_ALMPASSWORD_PARAMETER_NAME):getAlmPassword();
    }

    public String getAlmDomain() {

        return this.almDomain;
    }

    public String getAlmDomain(boolean fromPcClient) {

        return fromPcClient?useParameterIfNeeded(buildParameters,this.almDomain):getAlmDomain();
    }

    public String getAlmProject() {

        return this.almProject;
    }

    public String getAlmProject(boolean fromPcClient) {

        return fromPcClient?useParameterIfNeeded(buildParameters,this.almProject):getAlmProject();
    }

    public String getTestId() {

        return this.testId;
    }

    public String getTestId(boolean fromPcClient) {

        return fromPcClient?useParameterIfNeeded(buildParameters,this.testId):getTestId();
    }

    public String getTestInstanceId() {

        return this.testInstanceId;
    }

    public String getTestInstanceId(boolean fromPcClient) {

        return fromPcClient?useParameterIfNeeded(buildParameters,this.testInstanceId):getTestInstanceId();
    }

    public String getAutoTestInstanceID(){
        return this.autoTestInstanceID;
    }

    public TimeslotDuration getTimeslotDuration() {

        return this.timeslotDuration;
    }

    public boolean isVudsMode() {

        return this.vudsMode;
    }

    public PostRunAction getPostRunAction() {

        return this.postRunAction;
    }

    public String getDescription() {

        return this.description;
    }

    public boolean httpsProtocol(){
        return this.HTTPSProtocol;
    }

    public String getProxyOutURL(){
        return this.proxyOutURL;
    }

    public String getProxyOutURL(boolean fromPcClient){
        return fromPcClient?useParameterIfNeeded(buildParameters,this.proxyOutURL):getProxyOutURL();
    }

    public String getProxyOutUser(){
        return this.proxyOutUser;
    }

    public String getProxyOutUser(boolean fromPcClient){
        return fromPcClient?useParameterIfNeeded(buildParameters,this.proxyOutUser):getProxyOutUser();
    }

    public String getProxyOutPassword(){
        return this.proxyOutPassword;
    }

    public String getProxyOutPassword(boolean fromPcClient){
        return fromPcClient?useParameterIfNeeded(buildParameters,this.proxyOutPassword):getProxyOutPassword();
    }

    public static List<PostRunAction> getPostRunActions() {
        return Arrays.asList(PostRunAction.values());
    }

    public String getBuildParameters() {
        return this.buildParameters;
    }


    @Override
    public String toString() {

        return String.format("%s", runParamsToString().substring(1));
    }

    public String runParamsToString() {

        String vudsModeString = (vudsMode) ? "true" : "false";
        String trendString = ("USE_ID").equals(addRunToTrendReport) ? String.format(", TrendReportID = '%s'",trendReportId) : "";

        return String.format("[PCServer='%s', User='%s', Domain='%s', Project='%s', TestID='%s', " +
                        "TestInstanceID='%s', TimeslotDuration='%s', PostRunAction='%s', " +
                        "VUDsMode='%s'%s, HTTPSProtocol='%s']",

                pcServerName, almUserName, almDomain, almProject, testId,
                testInstanceId, timeslotDuration, postRunAction.getValue(),
                vudsModeString, trendString, HTTPSProtocol);
    }


    public String getTrendReportId() {
        return trendReportId;
    }

    public String getTrendReportId(boolean fromPcClient) {
        return fromPcClient?useParameterIfNeeded(buildParameters,this.trendReportId):getTrendReportId();
    }

    public void setTrendReportId(String trendReportId){
        this.trendReportId = trendReportId;
    }

    public String getAddRunToTrendReport() {
        return addRunToTrendReport;
    }

    public String isHTTPSProtocol(){
        if (!HTTPSProtocol)
            return "http";
        return "https";
    }

    public void setBuildParameters(String buildParameters){
        this.buildParameters = buildParameters;
    }

    private static String useParameterIfNeeded (String buildParameters,String attribute){
        if (buildParameters!=null && attribute!=null) {
            if(attribute.startsWith("$")) {
                String attributeParameter = attribute.replace("$", "").replace("{", "").replace("}", "");
                String[] buildParametersArray = buildParameters.replace("{", "").replace("}", "").split(",");
                for (String buildParameter : buildParametersArray) {
                    if (buildParameter.trim().startsWith(attributeParameter + "=")) {
                        return buildParameter.trim().replace(attributeParameter + "=", "");
                    }
                }
            }
        }
        return attribute;
    }

    private SecretContainer useParameterForAlmPasswordIfNeeded (String buildParameters, SecretContainer almPassword, String expectedAlmPasswordParameterName ){
        if (buildParameters!=null) {
            String[] buildParametersArray = buildParameters.replace("{", "").replace("}", "").split(",");
            for (String buildParameter : buildParametersArray) {
                if (buildParameter.trim().startsWith(expectedAlmPasswordParameterName + "=")) {
                    return setPassword(buildParameter.trim().replace(expectedAlmPasswordParameterName + "=", ""));
                }
            }
        }
        return almPassword;
    }
}
