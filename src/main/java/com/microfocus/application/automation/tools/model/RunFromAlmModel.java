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

import com.sun.org.apache.xpath.internal.operations.Bool;
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
    private String almApiKey;

    @DataBoundConstructor
    public RunFromAlmModel(String almServerName, String almUserName, String almPassword, String almDomain, String almProject,
                           String almTestSets, String almRunResultsMode, String almTimeout,
                           String almRunMode, String almRunHost, Boolean isSSOEnabled,
                           String almClientID, String almApiKey) {

        this.almServerName = almServerName;
        this.almUserName = almUserName;
        this.almPassword = Secret.fromString(almPassword);
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
        this.almClientID = almClientID;
        this.almApiKey = almApiKey;
    }

    public String getAlmUserName() {
        return almUserName;
    }

    public String getAlmDomain() {
        return almDomain;
    }

    public String getAlmPassword() {
        return almPassword.getPlainText();
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

    public Boolean isSSOEnabled() {
        return isSSOEnabled;
    }

    public void setIsSSOEnabled(Boolean isSSOEnabled){
        this.isSSOEnabled = isSSOEnabled;
    }

    public String getAlmClientID() { return almClientID; }

    public String getAlmApiKey() { return almApiKey; }

    public Properties getProperties(EnvVars envVars,
                                    VariableResolver<String> varResolver) {
        return CreateProperties(envVars, varResolver);
    }

    public Properties getProperties() {
        return CreateProperties(null, null);
    }


    private Properties CreateProperties(EnvVars envVars,
                                        VariableResolver<String> varResolver) {
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
