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

package com.hpe.application.automation.tools.model;

import java.util.Arrays;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * 
 * @author Effi Bar-She'an
 * @author Dani Schreiber
 * 
 */
public class SseModel {
    
    public static final String TEST_SET = "TEST_SET";
    public static final String BVS = "BVS";
    public static final String PC = "PC";
    
    public static final String COLLATE = "Collate";
    public static final String COLLATE_ANALYZE = "CollateAndAnalyze";
    public static final String DO_NOTHING = "DoNothing";
    
    private final String _almServerName;
    private String _almServerUrl;
    private final String _almUserName;
    private final SecretContainer _almPassword;
    private final String _almDomain;
    private final String _clientType;
    private final String _almProject;
    private final String _timeslotDuration;
    private final String _description;
    private final String _runType;
    private final String _almEntityId;
    private final String _postRunAction;
    private final String _environmentConfigurationId;
    private final CdaDetails _cdaDetails;
    private final SseProxySettings _proxySettings;
    
    private final static EnumDescription _runTypeTestSet =
            new EnumDescription(TEST_SET, "Test Set");
    private final static EnumDescription _runTypeBVS = new EnumDescription(
            BVS,
            "Build Verification Suite");
    private final static List<EnumDescription> _runTypes = Arrays.asList(
            _runTypeTestSet,
            _runTypeBVS);
    
    private final static EnumDescription _postRunActionCollate = new EnumDescription(
            COLLATE,
            "Collate");
    private final static EnumDescription _postRunActionCollateAnalyze = new EnumDescription(
            COLLATE_ANALYZE,
            "CollateAndAnalyze");
    private final static EnumDescription _postRunActionDoNothing = new EnumDescription(
            DO_NOTHING,
            "DoNothing");
    private final static List<EnumDescription> _postRunActions = Arrays.asList(
            _postRunActionCollate,
            _postRunActionCollateAnalyze,
            _postRunActionDoNothing);
    
    @DataBoundConstructor
    public SseModel(
            String almServerName,
            String almUserName,
            String almPassword,
            String almDomain,
            String clientType,
            String almProject,
            String runType,
            String almEntityId,
            String timeslotDuration,
            String description,
            String postRunAction,
            String environmentConfigurationId,
            CdaDetails cdaDetails,
            SseProxySettings proxySettings) {
        
        _almServerName = almServerName;
        _almDomain = almDomain;
        _clientType = clientType;
        _almProject = almProject;
        _timeslotDuration = timeslotDuration;
        _almEntityId = almEntityId;
        _almUserName = almUserName;
        _almPassword = setPassword(almPassword);
        _runType = runType;
        _description = description;
        _postRunAction = postRunAction;
        _environmentConfigurationId = environmentConfigurationId;
        _cdaDetails = cdaDetails;
        _proxySettings = proxySettings;
        
    }
    
    protected SecretContainer setPassword(String almPassword) {
        
        SecretContainer secretContainer = new SecretContainerImpl();
        secretContainer.initialize(almPassword);
        
        return secretContainer;
    }
    
    public String getAlmServerName() {
        
        return _almServerName;
    }
    
    public String getAlmServerUrl() {
        
        return _almServerUrl;
    }
    
    public void setAlmServerUrl(String almServerUrl) {
        
        _almServerUrl = almServerUrl;
    }
    
    public String getAlmUserName() {
        
        return _almUserName;
    }
    
    public String getAlmPassword() {
        
        return _almPassword.toString();
    }
    
    public String getAlmDomain() {
        
        return _almDomain;
    }

    public String getClientType() {
        return _clientType;
    }
    
    public String getAlmProject() {
        
        return _almProject;
    }
    
    public String getTimeslotDuration() {
        
        return _timeslotDuration;
    }
    
    public String getAlmEntityId() {
        
        return _almEntityId;
    }
    
    public String getRunType() {
        return _runType;
    }
    
    public String getDescription() {
        
        return _description;
    }
    
    public String getEnvironmentConfigurationId() {
        
        return _environmentConfigurationId;
    }
    
    public static List<EnumDescription> getRunTypes() {
        
        return _runTypes;
    }
    
    public static List<EnumDescription> getPostRunActions() {
        
        return _postRunActions;
    }
    
    public CdaDetails getCdaDetails() {
        
        return _cdaDetails;
    }
    
    public String getPostRunAction() {
        
        return _postRunAction;
    }
    
    public SseProxySettings getProxySettings() {
        return _proxySettings;
    }
}
