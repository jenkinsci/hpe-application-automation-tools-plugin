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

import java.util.List;

import com.microfocus.application.automation.tools.settings.AlmServerSettingsBuilder;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.util.FormValidation;

/**
 * Created by barush on 21/10/2014.
 */
public class AutEnvironmentModel extends AbstractDescribableImpl<AutEnvironmentModel> {
    
    private final String almServerName;
    private String almServerUrl;
    private final String almUserName;
    private final SecretContainer almPassword;
    private final String almDomain;
    private final String almProject;
    
    private final String autEnvironmentId;
    private final boolean useExistingAutEnvConf;
    private final String existingAutEnvConfId;
    private final boolean createNewAutEnvConf;
    private final String newAutEnvConfName;
    
    private List<AutEnvironmentParameterModel> autEnvironmentParameters;
    
    private final String pathToJsonFile;
    private final String outputParameter;
    
    @DataBoundConstructor
    public AutEnvironmentModel(
            String almServerName,
            String almUserName,
            String almPassword,
            String almDomain,
            String almProject,
            String autEnvironmentId,
            boolean useExistingAutEnvConf,
            String existingAutEnvConfId,
            boolean createNewAutEnvConf,
            String newAutEnvConfName,
            List<AutEnvironmentParameterModel> autEnvironmentParameters,
            String pathToJsonFile,
            String outputParameter) {
        
        this.almServerName = almServerName;
        this.almUserName = almUserName;
        this.almPassword = setPassword(almPassword);
        this.almDomain = almDomain;
        this.almProject = almProject;
        this.autEnvironmentId = autEnvironmentId;
        this.useExistingAutEnvConf = useExistingAutEnvConf;
        this.existingAutEnvConfId = existingAutEnvConfId;
        this.createNewAutEnvConf = createNewAutEnvConf;
        this.newAutEnvConfName = newAutEnvConfName;
        this.autEnvironmentParameters = autEnvironmentParameters;
        this.pathToJsonFile = pathToJsonFile;
        this.outputParameter = outputParameter;
        
    }
    
    protected SecretContainer setPassword(String almPassword) {
        
        SecretContainer secretContainer = new SecretContainerImpl();
        secretContainer.initialize(almPassword);
        
        return secretContainer;
    }
    
    public String getPathToJsonFile() {
        return pathToJsonFile;
    }
    
    public boolean isCreateNewAutEnvConf() {
        return createNewAutEnvConf;
    }
    
    public boolean isUseExistingAutEnvConf() {
        return useExistingAutEnvConf;
    }
    
    public String getAlmServerName() {
        
        return almServerName;
    }
    
    public String getAlmServerUrl() {
        
        return almServerUrl;
    }
    
    public void setAlmServerUrl(String almServerUrl) {
        
        this.almServerUrl = almServerUrl;
    }
    
    public String getAlmUserName() {
        
        return almUserName;
    }
    
    public String getAlmPassword() {
        
        return almPassword.toString();
    }
    
    public String getAlmDomain() {
        
        return almDomain;
    }
    
    public String getAlmProject() {
        
        return almProject;
    }
    
    public String getOutputParameter() {
        return outputParameter;
    }
    
    public String getAutEnvironmentId() {
        return autEnvironmentId;
    }
    
    public String getExistingAutEnvConfId() {
        return existingAutEnvConfId;
    }
    
    public String getNewAutEnvConfName() {
        return newAutEnvConfName;
    }
    
    public List<AutEnvironmentParameterModel> getAutEnvironmentParameters() {
        return autEnvironmentParameters;
    }
    
    @Extension
    public static class DescriptorImpl extends Descriptor<AutEnvironmentModel> {
        
        public String getDisplayName() {
            return "AUT Env";
        }
        
        public AlmServerSettingsModel[] getAlmServers() {
            
            return Hudson.getInstance().getDescriptorByType(
                    AlmServerSettingsBuilder.DescriptorImpl.class).getInstallations();
        }
        
        public FormValidation doCheckAlmUserName(@QueryParameter String value) {
            
            return generalCheckWithError(value, "User name must be set");
        }
        
        public FormValidation doCheckAlmDomain(@QueryParameter String value) {
            
            return generalCheckWithError(value, "Domain must be set");
        }
        
        public FormValidation doCheckAlmProject(@QueryParameter String value) {
            
            return generalCheckWithError(value, "Project must be set");
        }
        
        public FormValidation doCheckAutEnvironmentId(@QueryParameter String value) {
            
            return generalCheckWithError(value, "AUT Environment ID must be set");
        }
        
        public FormValidation doCheckAlmPassword(@QueryParameter String value) {
            
            FormValidation ret = FormValidation.ok();
            if (StringUtils.isBlank(value)) {
                ret = FormValidation.warning("Password for ALM server is empty");
            }
            
            return ret;
        }
        
        public FormValidation doCheckOutputParameter(@QueryParameter String value) {
            
            FormValidation ret = FormValidation.ok();
            if (StringUtils.isBlank(value)) {
                ret =
                        FormValidation.warning("AUT Environment Configuration ID isn't assigned to any environment variable");
            }
            
            return ret;
        }
        
        private FormValidation generalCheckWithError(String value, String errorMessage) {
            
            FormValidation ret = FormValidation.ok();
            if (StringUtils.isBlank(value)) {
                ret = FormValidation.error(errorMessage);
            }
            
            return ret;
        }
        
    }
    
}
