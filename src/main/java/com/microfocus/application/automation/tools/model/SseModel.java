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
            CdaDetails cdaDetails) {
        
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
}
