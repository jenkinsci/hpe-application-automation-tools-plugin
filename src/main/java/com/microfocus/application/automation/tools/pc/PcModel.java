/*
 *  Certain versions of software accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 *  This software was acquired by Micro Focus on September 1, 2017, and is now
 *  offered by OpenText.
 *  Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical
 *  in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the
 *  property of their respective owners.
 *  OpenText is a trademark of Open Text.
 *  __________________________________________________________________
 *  MIT License
 *
 *  Copyright 2012-2025 Open Text.
 *
 *  The only warranties for products and services of Open Text and
 *  its affiliates and licensors ("Open Text") are as may be set forth
 *  in the express warranty statements accompanying such products and services.
 *  Nothing herein should be construed as constituting an additional warranty.
 *  Open Text shall not be liable for technical or editorial errors or
 *  omissions contained herein. The information contained herein is subject
 *  to change without notice.
 *
 *  Except as specifically indicated otherwise, this document contains
 *  confidential information and a valid license is required for possession,
 *  use or copying. If this work is provided to the U.S. Government,
 *  consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 *  Computer Software Documentation, and Technical Data for Commercial Items are
 *  licensed to the U.S. Government under vendor's standard commercial license.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ___________________________________________________________________
 */
/*
 * Takes all the parameter from the job in order to create a loadtest object
 * */
package com.microfocus.application.automation.tools.pc;

import com.microfocus.adm.performancecenter.plugins.common.pcentities.PostRunAction;
import com.microfocus.adm.performancecenter.plugins.common.pcentities.TimeslotDuration;
import com.microfocus.application.automation.tools.model.SecretContainer;
import com.microfocus.application.automation.tools.model.SecretContainerImpl;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Arrays;
import java.util.List;

public class PcModel {

    public static final String COLLATE = "Collate Results";
    public static final String COLLATE_ANALYZE = "Collate and Analyze";
    public static final String DO_NOTHING = "Do Not Collate";

    private final String serverAndPort;
    private final String pcServerName;
    private final String credentialsId;
    private final String almDomain;
    private final String almProject;
    private final String testId;
    private final String autoTestInstanceID;
    private final PostRunAction postRunAction;
    private final boolean vudsMode;
    private final String description;
    private final String addRunToTrendReport;
    private final boolean HTTPSProtocol;
    private final String proxyOutURL;
    private final String credentialsProxyId;
    private final boolean authenticateWithToken;
    private String testInstanceId;
    private String trendReportId;
    private String buildParameters;
    private String retry;
    private String retryDelay;
    private String retryOccurrences;
    private String timeslotDurationHours;
    private String timeslotDurationMinutes;

    @DataBoundConstructor
    public PcModel(String serverAndPort, String pcServerName, String credentialsId, String almDomain, String almProject,
                   String testId, String autoTestInstanceID, String testInstanceId, String timeslotDurationHours, String timeslotDurationMinutes,
                   PostRunAction postRunAction, boolean vudsMode, String description, String addRunToTrendReport, String trendReportId, boolean HTTPSProtocol,
                   String proxyOutURL, String credentialsProxyId, String retry, String retryDelay, String retryOccurrences, boolean authenticateWithToken) {

        this.serverAndPort = serverAndPort;
        this.pcServerName = pcServerName;
        this.credentialsId = credentialsId;
        this.almDomain = almDomain;
        this.almProject = almProject;
        this.testId = testId;
        this.autoTestInstanceID = autoTestInstanceID;
        this.testInstanceId = testInstanceId;
        this.timeslotDurationHours = timeslotDurationHours;
        this.timeslotDurationMinutes = timeslotDurationMinutes;
        this.postRunAction = postRunAction;
        this.vudsMode = vudsMode;
        this.description = description;
        this.addRunToTrendReport = addRunToTrendReport;
        this.HTTPSProtocol = HTTPSProtocol;
        this.trendReportId = trendReportId;
        this.proxyOutURL = proxyOutURL;
        this.credentialsProxyId = credentialsProxyId;
        this.buildParameters = "";
        this.retry = retry;
        this.retryDelay = verifyStringValueIsIntAndPositive(retryDelay, 5);
        this.retryOccurrences = verifyStringValueIsIntAndPositive(retryOccurrences, 3);
        this.authenticateWithToken = authenticateWithToken;
    }

    private static boolean isInteger(String s, int radix) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (i == 0 && s.charAt(i) == '-') {
                if (s.length() == 1) return false;
                else continue;
            }
            if (Character.digit(s.charAt(i), radix) < 0) return false;
        }
        return true;
    }

    private static boolean isInteger(String s) {
        return isInteger(s, 10);
    }

    public static List<PostRunAction> getPostRunActions() {
        return Arrays.asList(PostRunAction.values());
    }

    private static String useParameterIfNeeded(String buildParameters, String attribute) {
        if (buildParameters != null && attribute != null) {
            if (attribute.startsWith("$")) {
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

    private String verifyStringValueIsIntAndPositive(String supplied, int defaultValue) {
        if (supplied != null && isInteger(supplied)) {
            int suppliedInt = Integer.parseInt(supplied);
            if (suppliedInt > 0)
                return Integer.toString(suppliedInt);
        }
        return Integer.toString(defaultValue);
    }

    public String getRetry() {

        return this.retry;
    }

    public String getRetryDelay() {
        return this.retryDelay;
    }

    public String getRetryOccurrences() {

        return this.retryOccurrences;
    }

    protected SecretContainer setPassword(String almPassword) {

        SecretContainer secretContainer = new SecretContainerImpl();
        secretContainer.initialize(almPassword);
        return secretContainer;
    }

    public String getserverAndPort() {
        return this.serverAndPort;
    }

    public String getPcServerName() {

        return this.pcServerName;
    }

    public String getPcServerName(boolean fromPcClient) {

        return fromPcClient ? useParameterIfNeeded(buildParameters, this.pcServerName) : getPcServerName();
    }

    public String getCredentialsId() {

        return this.credentialsId;
    }

    public String getCredentialsId(boolean fromPcClient) {

        return fromPcClient ? useParameterIfNeeded(buildParameters, this.credentialsId) : getCredentialsId();
    }

    public String getCredentialsProxyId() {

        return this.credentialsProxyId;
    }

    public String getCredentialsProxyId(boolean fromPcClient) {

        return fromPcClient ? useParameterIfNeeded(buildParameters, this.credentialsProxyId) : getCredentialsProxyId();
    }

    public String getAlmDomain() {

        return this.almDomain;
    }

    public String getAlmDomain(boolean fromPcClient) {

        return fromPcClient ? useParameterIfNeeded(buildParameters, this.almDomain) : getAlmDomain();
    }

    public String getAlmProject() {

        return this.almProject;
    }

    public String getAlmProject(boolean fromPcClient) {

        return fromPcClient ? useParameterIfNeeded(buildParameters, this.almProject) : getAlmProject();
    }

    public String getTestId() {

        return this.testId;
    }

    public String getTestId(boolean fromPcClient) {

        return fromPcClient ? useParameterIfNeeded(buildParameters, this.testId) : getTestId();
    }

    public String getTestInstanceId() {

        return this.testInstanceId;
    }

    public String getTestInstanceId(boolean fromPcClient) {

        return fromPcClient ? useParameterIfNeeded(buildParameters, this.testInstanceId) : getTestInstanceId();
    }

    public String getAutoTestInstanceID() {
        return this.autoTestInstanceID;
    }

    public String getTimeslotDurationHours() {

        return this.timeslotDurationHours;
    }

    public String getTimeslotDurationHours(boolean fromPcClient) {

        return fromPcClient ? useParameterIfNeeded(buildParameters, this.timeslotDurationHours) : getTimeslotDurationHours();
    }

    public String getTimeslotDurationMinutes() {

        return this.timeslotDurationMinutes;
    }

    public String getTimeslotDurationMinutes(boolean fromPcClient) {

        return fromPcClient ? useParameterIfNeeded(buildParameters, this.timeslotDurationMinutes) : getTimeslotDurationMinutes();
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

    public boolean httpsProtocol() {
        return this.HTTPSProtocol;
    }

    public String getProxyOutURL() {
        return this.proxyOutURL;
    }

    public String getProxyOutURL(boolean fromPcClient) {
        return fromPcClient ? useParameterIfNeeded(buildParameters, this.proxyOutURL) : getProxyOutURL();
    }

    public String getBuildParameters() {
        return this.buildParameters;
    }

    public void setBuildParameters(String buildParameters) {
        this.buildParameters = buildParameters;
    }

    @Override
    public String toString() {

        return String.format("%s", runParamsToString().substring(1));
    }

    public String runParamsToString() {

        String vudsModeString = (vudsMode) ? "true" : "false";
        String trendString = ("USE_ID").equals(addRunToTrendReport) ? String.format(", TrendReportID = '%s'", trendReportId) : "";

        return String.format("[PCServer='%s', CredentialsId='%s', Domain='%s', Project='%s', TestID='%s', " +
                        "TestInstanceID='%s', TimeslotDurationHours='%s', TimeslotDurationMinutes='%s', PostRunAction='%s', " +
                        "VUDsMode='%s, trending='%s', HTTPSProtocol='%s', authenticateWithToken='%s']",

                pcServerName, credentialsId, almDomain, almProject, testId,
                testInstanceId, timeslotDurationHours, timeslotDurationMinutes, postRunAction.getValue(),
                vudsModeString, trendString, HTTPSProtocol, authenticateWithToken);
    }

    public String getTrendReportId() {
        return trendReportId;
    }

    public void setTrendReportId(String trendReportId) {
        this.trendReportId = trendReportId;
    }

    public String getTrendReportId(boolean fromPcClient) {
        return fromPcClient ? useParameterIfNeeded(buildParameters, this.trendReportId) : getTrendReportId();
    }

    public String getAddRunToTrendReport() {
        return addRunToTrendReport;
    }

    public String isHTTPSProtocol() {
        if (!HTTPSProtocol)
            return "http";
        return "https";
    }

    public boolean isAuthenticateWithToken() {
        return this.authenticateWithToken;
    }


}
