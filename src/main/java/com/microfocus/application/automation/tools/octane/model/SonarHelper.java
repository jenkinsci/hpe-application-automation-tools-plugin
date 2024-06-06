/*
 * Certain versions of software accessible here may contain branding from Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 * This software was acquired by Micro Focus on September 1, 2017, and is now offered by OpenText.
 * Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright 2012-2024 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.octane.model;

import com.microfocus.application.automation.tools.sse.common.StringUtils;
import hudson.EnvVars;
import hudson.maven.MavenModuleSet;
import hudson.model.*;
import hudson.plugins.sonar.SonarGlobalConfiguration;
import hudson.plugins.sonar.SonarInstallation;
import hudson.plugins.sonar.SonarRunnerBuilder;
import hudson.tasks.Builder;
import hudson.util.DescribableList;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

/**
 * the adapter enables usage of sonar classes and interfaces without adding a full dependency on
 * sonar plugin.
 * this class is only dependent on sonar plugin for compile time.
 */

public class SonarHelper {

    public enum DataType {VULNERABILITIES, COVERAGE}

    public static final String SONAR_GLOBAL_CONFIG = "hudson.plugins.sonar.SonarGlobalConfiguration";
    private static final String SONAR_ACTION_ID = "hudson.plugins.sonar.SonarRunnerBuilder";
    private static final String SONAR_SERVER_HOST_VARIABLE = "SONAR_HOST_URL";
    private static final String SONAR_SERVER_TOKEN_VARIABLE = "SONAR_AUTH_TOKEN";

    private String serverUrl;
    private String serverToken;

    public SonarHelper(Run<?, ?> run, TaskListener listener) {
        DescribableList<Builder, Descriptor<Builder>> postbuilders = null;
        if (run instanceof AbstractBuild) {
            AbstractBuild abstractBuild = (AbstractBuild) run;
            setSonarDetailsFromMavenEnvironment(abstractBuild, listener);
            AbstractProject project = abstractBuild.getProject();

            // for jobs that does not use new sonar approach for maven jobs,
            // extract data for old approach which used post build step
            if (StringUtils.isNullOrEmpty(this.getServerUrl()) || StringUtils.isNullOrEmpty(this.getServerToken())) {
                if (project instanceof MavenModuleSet) {
                    postbuilders = ((MavenModuleSet) project).getPostbuilders();
                } else if (project instanceof Project) {
                    postbuilders = ((Project) project).getBuildersList();
                }
                if (postbuilders != null){
                    setDataFromSonarBuilder(postbuilders, run);
                }
            }
        }
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getServerToken() {
        return serverToken;
    }

    // used by the web hook
    public static String getSonarInstallationTokenByUrl(GlobalConfiguration sonarConfiguration, String sonarUrl, Run run) {
        if (sonarConfiguration instanceof SonarGlobalConfiguration) {
            SonarGlobalConfiguration sonar = (SonarGlobalConfiguration) sonarConfiguration;
            Optional<SonarInstallation> installation = Arrays.stream(sonar.getInstallations())
                    .filter(sonarInstallation -> sonarInstallation.getServerUrl().equals(sonarUrl))
                    .findFirst();
            if (installation.isPresent()) {
                return extractAuthenticationToken(installation.get(), run);
            }
        }
        return "";
    }

    private static String extractAuthenticationToken(SonarInstallation sonarInstallation, Run run) {
        return sonarInstallation.getServerAuthenticationToken(run);
    }

    private void setSonarDetailsFromMavenEnvironment(AbstractBuild build, TaskListener listener) {
        EnvVars environment;
        try {
            environment = build.getEnvironment(listener);
            if (environment != null) {
                this.serverUrl = environment.get(SONAR_SERVER_HOST_VARIABLE, "");
                this.serverToken = environment.get(SONAR_SERVER_TOKEN_VARIABLE, "");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void setDataFromSonarBuilder(DescribableList<Builder, Descriptor<Builder>> postBuilders, Run run) {
        Builder sonarBuilder = postBuilders.getDynamic(SONAR_ACTION_ID);
        if (sonarBuilder != null) {
            SonarRunnerBuilder builder = (SonarRunnerBuilder) sonarBuilder;
            this.serverUrl = extractSonarUrl(builder);
            this.serverToken = extractSonarToken(builder, run);
        }
    }

    /**
     * get sonar URL address
     *
     * @return Sonar's URL
     */
    private String extractSonarUrl(SonarRunnerBuilder builder) {
        return builder != null ? builder.getSonarInstallation().getServerUrl() : "";
    }

    /**
     * get sonar server token
     *
     * @return Sonar's auth token
     */
    private String extractSonarToken(SonarRunnerBuilder builder, Run run) {
        String result  = builder != null  ? extractAuthenticationToken(builder.getSonarInstallation(), run) : "";
        return result;
    }
}
