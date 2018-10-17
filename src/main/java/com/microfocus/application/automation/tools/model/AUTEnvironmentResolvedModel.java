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

/**
 * Created by barush
 */
public class AUTEnvironmentResolvedModel {
    
    private String almServerName;
    private String almServerUrl;
    private String almUserName;
    private String almPassword;
    private String almDomain;
    private String almProject;
    private String clientType;
    private String autEnvironmentId;
    private boolean useExistingAutEnvConf;
    private String existingAutEnvConfId;
    private boolean createNewAutEnvConf;
    private String newAutEnvConfName;
    
    private List<AutEnvironmentParameterModel> autEnvironmentParameters;
    
    private String pathToJsonFile;
    private String outputParameter;
    
    public AUTEnvironmentResolvedModel(
            String almServerName,
            String almServerUrl,
            String almUserName,
            String almPassword,
            String almDomain,
            String almProject,
            String clientType,
            String autEnvironmentId,
            boolean useExistingAutEnvConf,
            String existingAutEnvConfId,
            boolean createNewAutEnvConf,
            String newAutEnvConfName,
            List<AutEnvironmentParameterModel> autEnvironmentParameters,
            String pathToJsonFile,
            String outputParameter) {
        
        this.almServerName = almServerName;
        this.almServerUrl = almServerUrl;
        this.almUserName = almUserName;
        this.almPassword = almPassword;
        this.almDomain = almDomain;
        this.almProject = almProject;
        this.clientType = clientType;
        this.autEnvironmentId = autEnvironmentId;
        this.useExistingAutEnvConf = useExistingAutEnvConf;
        this.existingAutEnvConfId = existingAutEnvConfId;
        this.createNewAutEnvConf = createNewAutEnvConf;
        this.newAutEnvConfName = newAutEnvConfName;
        this.autEnvironmentParameters = autEnvironmentParameters;
        this.pathToJsonFile = pathToJsonFile;
        this.outputParameter = outputParameter;
    }
    
    public String getAlmServerName() {
        return almServerName;
    }
    
    public String getAlmServerUrl() {
        return almServerUrl;
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

    public String getClientType() {
        return clientType;
    }

    public String getAutEnvironmentId() {
        return autEnvironmentId;
    }
    
    public boolean isUseExistingAutEnvConf() {
        return useExistingAutEnvConf;
    }
    
    public String getExistingAutEnvConfId() {
        return existingAutEnvConfId;
    }
    
    public boolean isCreateNewAutEnvConf() {
        return createNewAutEnvConf;
    }
    
    public String getNewAutEnvConfName() {
        return newAutEnvConfName;
    }
    
    public List<AutEnvironmentParameterModel> getAutEnvironmentParameters() {
        return autEnvironmentParameters;
    }
    
    public String getPathToJsonFile() {
        return pathToJsonFile;
    }
    
    public String getOutputParameter() {
        return outputParameter;
    }
}
