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
 *  Copyright 2012-2026 Open Text.
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
package com.microfocus.application.automation.tools.pipelineSteps;

import com.microfocus.adm.performancecenter.plugins.common.pcentities.PostRunAction;
import com.microfocus.application.automation.tools.run.PcBuilder;
import hudson.Extension;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.Set;

public class PcBuildStep extends Step {

    private String serverAndPort;
    private String pcServerName;
    private String credentialsId;
    private String almDomain;
    private String almProject;
    private String testId;
    private String testInstanceId;
    private String autoTestInstanceID;
    private String timeslotDurationHours;
    private String timeslotDurationMinutes;
    private PostRunAction postRunAction;
    private boolean vudsMode;
    private boolean statusBySLA;
    private String description;
    private String addRunToTrendReport;
    private String trendReportId;
    private boolean HTTPSProtocol;
    private String proxyOutURL;
    private String credentialsProxyId;
    private String retry;
    private String retryDelay;
    private String retryOccurrences;
    private boolean authenticateWithToken;

    @DataBoundConstructor
    public PcBuildStep(
            String serverAndPort,
            String pcServerName,
            String credentialsId,
            String almDomain,
            String almProject,
            String testId,
            String testInstanceId,
            String autoTestInstanceID,
            String timeslotDurationHours,
            String timeslotDurationMinutes,
            PostRunAction postRunAction,
            boolean vudsMode,
            boolean statusBySLA,
            String description,
            String addRunToTrendReport,
            String trendReportId,
            boolean HTTPSProtocol,
            String proxyOutURL,
            String credentialsProxyId,
            String retry,
            String retryDelay,
            String retryOccurrences,
            boolean authenticateWithToken) {

        this.serverAndPort = serverAndPort;
        this.pcServerName = pcServerName;
        this.credentialsId = credentialsId;
        this.almDomain = almDomain;
        this.almProject = almProject;
        this.testId = testId;
        this.testInstanceId = testInstanceId;
        this.autoTestInstanceID = autoTestInstanceID;
        this.timeslotDurationHours = timeslotDurationHours;
        this.timeslotDurationMinutes = timeslotDurationMinutes;
        this.postRunAction = postRunAction;
        this.vudsMode = vudsMode;
        this.statusBySLA = statusBySLA;
        this.description = description;
        this.addRunToTrendReport = addRunToTrendReport;
        this.trendReportId = trendReportId;
        this.HTTPSProtocol = HTTPSProtocol;
        this.proxyOutURL = proxyOutURL;
        this.credentialsProxyId = credentialsProxyId;
        this.retry = retry;
        this.retryDelay = retryDelay;
        this.retryOccurrences = retryOccurrences;
        this.authenticateWithToken = authenticateWithToken;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        PcBuilder builder = new PcBuilder(
                serverAndPort,
                pcServerName,
                credentialsId,
                almDomain,
                almProject,
                testId,
                testInstanceId,
                autoTestInstanceID,
                timeslotDurationHours,
                timeslotDurationMinutes,
                postRunAction,
                vudsMode,
                statusBySLA,
                description,
                addRunToTrendReport,
                trendReportId,
                HTTPSProtocol,
                proxyOutURL,
                credentialsProxyId,
                retry,
                retryDelay,
                retryOccurrences,
                authenticateWithToken
        );
        return new PcBuildStepExecution(builder, context);
    }

    @Extension
    @Symbol("pcBuild")
    public static class DescriptorImpl extends StepDescriptor {
        @Override
        public Set<Class<?>> getRequiredContext() {
            return java.util.Collections.emptySet();
        }

        @Override
        public String getDisplayName() {
            return "Performance Center Load Test";
        }

        @Override
        public String getFunctionName() {
            return "pcBuild";
        }
    }

    // Getters for configuration (for Jenkins form rendering)
    public String getServerAndPort() { return serverAndPort; }
    public String getPcServerName() { return pcServerName; }
    public String getCredentialsId() { return credentialsId; }
    public String getAlmDomain() { return almDomain; }
    public String getAlmProject() { return almProject; }
    public String getTestId() { return testId; }
    public String getTestInstanceId() { return testInstanceId; }
    public String getAutoTestInstanceID() { return autoTestInstanceID; }
    public String getTimeslotDurationHours() { return timeslotDurationHours; }
    public String getTimeslotDurationMinutes() { return timeslotDurationMinutes; }
    public PostRunAction getPostRunAction() { return postRunAction; }
    public boolean isVudsMode() { return vudsMode; }
    public boolean isStatusBySLA() { return statusBySLA; }
    public String getDescription() { return description; }
    public String getAddRunToTrendReport() { return addRunToTrendReport; }
    public String getTrendReportId() { return trendReportId; }
    public boolean isHTTPSProtocol() { return HTTPSProtocol; }
    public String getProxyOutURL() { return proxyOutURL; }
    public String getCredentialsProxyId() { return credentialsProxyId; }
    public String getRetry() { return retry; }
    public String getRetryDelay() { return retryDelay; }
    public String getRetryOccurrences() { return retryOccurrences; }
    public boolean isAuthenticateWithToken() { return authenticateWithToken; }
}

