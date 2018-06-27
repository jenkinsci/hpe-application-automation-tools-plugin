/*
 * © Copyright 2013 EntIT Software LLC
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright (c) 2018 Micro Focus Company, L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
