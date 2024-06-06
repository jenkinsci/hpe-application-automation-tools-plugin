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

package com.microfocus.application.automation.tools.model;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

import hudson.util.VariableResolver;

import java.util.Arrays;
import java.util.List;

import hudson.util.Secret;

import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;


import javax.annotation.Nonnull;

public class RunFromAlmModel extends AbstractDescribableImpl<RunFromAlmModel> {

    public final static EnumDescription runModeLocal = new EnumDescription(
            "RUN_LOCAL", "Run locally");
    public final static EnumDescription runModePlannedHost = new EnumDescription(
            "RUN_PLANNED_HOST", "Run on planned host");
    public final static EnumDescription runModeRemote = new EnumDescription(
            "RUN_REMOTE", "Run remotely");
    public final static List<EnumDescription> runModes = Arrays.asList(
            runModeLocal, runModePlannedHost, runModeRemote);

    public final static int DEFAULT_TIMEOUT = 36000; // 10 hrs
    public final static String ALM_PASSWORD_KEY = "almPassword";
    public final static String ALM_API_KEY_SECRET = "almApiKeySecret";

    private String almServerName;
    private String almUserName;
    private Secret almPassword;
    private String almDomain;
    private String almProject;
    private String almTestSets;
    private String almRunResultsMode;
    private String almTimeout;
    private String almRunMode;
    private String almRunHost;
    private Boolean isSSOEnabled;
    private String almClientID;
    private Secret almApiKey;
    private CredentialsScope credentialsScope;

    @DataBoundConstructor
    public RunFromAlmModel(String almServerName, String almUserName, String almPassword, String almDomain, String almProject,
                           String almTestSets, String almRunResultsMode, String almTimeout,
                           String almRunMode, String almRunHost, Boolean isSSOEnabled,
                           String almClientID, String almApiKey, CredentialsScope credentialsScope) {

        this.almServerName = almServerName;
        this.credentialsScope = credentialsScope;

        this.almUserName = StringUtils.defaultString(almUserName);
        this.almPassword = StringUtils.isBlank(almUserName) ? null : Secret.fromString(almPassword);
        this.almDomain = almDomain;
        this.almProject = almProject;
        this.almTestSets = almTestSets;

        if (!this.almTestSets.contains("\n")) {
            this.almTestSets += "\n";
        }

        this.almRunResultsMode = almRunResultsMode;
        this.almTimeout = almTimeout;
        this.almRunMode = almRunMode;
        this.almRunHost = almRunHost;

        this.isSSOEnabled = isSSOEnabled;
        this.almClientID = StringUtils.defaultString(almClientID);
        this.almApiKey = StringUtils.isBlank(almClientID) ? null : Secret.fromString(almApiKey);
    }

    public String getAlmUserName() {
        return almUserName;
    }

    public String getAlmDomain() {
        return almDomain;
    }

    public Secret getAlmPassword() {
        return almPassword;
    }

    public String getAlmProject() {
        return almProject;
    }

    public String getAlmTestSets() {
        return almTestSets;
    }

    public String getAlmRunResultsMode() {
        return almRunResultsMode;
    }

    public String getAlmTimeout() {
        return almTimeout;
    }

    public String getAlmRunHost() {
        return almRunHost;
    }

    public String getAlmRunMode() {
        return almRunMode;
    }

    public String getAlmServerName() {
        return almServerName;
    }

    public CredentialsScope getCredentialsScope() {
        return credentialsScope;
    }

    public Boolean isSSOEnabled() {
        return isSSOEnabled;
    }

    public void setIsSSOEnabled(Boolean isSSOEnabled){
        this.isSSOEnabled = isSSOEnabled;
    }

    public String getAlmClientID() { return almClientID; }

    public Secret getAlmApiKey() { return almApiKey; }

    public Properties getProperties(EnvVars envVars, VariableResolver<String> varResolver) {
        return CreateProperties(envVars, varResolver);
    }

    public String getCredentialsScopeValue() { return credentialsScope == null ? "" : credentialsScope.getValue(); }
    public String getPasswordEncryptedValue() { return almPassword == null ? "" : almPassword.getEncryptedValue(); }
    public String getApiKeyEncryptedValue() { return almApiKey == null || StringUtils.isBlank(almApiKey.getPlainText()) ? "" : almApiKey.getEncryptedValue(); }
    public String getPasswordPlainText() { return almPassword == null ? "" : almPassword.getPlainText(); }
    public String getApiKeyPlainText() { return almApiKey == null ? "" : almApiKey.getPlainText(); }

    public Properties getProperties() {
        return CreateProperties(null, null);
    }

    private Properties CreateProperties(EnvVars envVars, VariableResolver<String> varResolver) {
        Properties props = new Properties();

        if(isSSOEnabled != null){
            props.put("SSOEnabled", Boolean.toString(isSSOEnabled));
        }else{
            props.put("SSOEnabled", Boolean.toString(false));
        }

        if (envVars == null) {
            props.put("almUsername", almUserName);
            props.put("almDomain", almDomain);
            props.put("almProject", almProject);
        } else {
            props.put("almUsername",
                    Util.replaceMacro(envVars.expand(almUserName), varResolver));
            props.put("almDomain",
                    Util.replaceMacro(envVars.expand(almDomain), varResolver));
            props.put("almProject",
                    Util.replaceMacro(envVars.expand(almProject), varResolver));
        }

        if (!StringUtils.isEmpty(this.almTestSets)) {

            String[] testSetsArr = this.almTestSets.replaceAll("\r", "").split(
                    "\n");

            int i = 1;

            for (String testSet : testSetsArr) {
                if (!StringUtils.isBlank(testSet)) {
                    props.put("TestSet" + i,
                            Util.replaceMacro(envVars.expand(testSet), varResolver));
                    i++;
                }
            }

            props.put("numOfTests", String.valueOf(i - 1));
        } else {
            props.put("almTestSets", "");
        }

        if (StringUtils.isEmpty(almTimeout)) {
            props.put("almTimeout", "-1");
        } else {
            props.put("almTimeout", almTimeout);
        }

        props.put("almRunMode", almRunMode);
        props.put("almRunHost", almRunHost);

        return props;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<RunFromAlmModel> {
        @Nonnull
        @Override
        public String getDisplayName() {
            return "UFT ALM Model";
        }

        public List<EnumDescription> getAlmRunModes() {
            return runModes;
        }
    }
}
