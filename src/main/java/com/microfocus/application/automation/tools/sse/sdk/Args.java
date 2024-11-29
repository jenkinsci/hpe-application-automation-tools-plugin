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
package com.microfocus.application.automation.tools.sse.sdk;

import com.microfocus.application.automation.tools.model.CdaDetails;

/**
 * 
 * @author Effi Bar-She'an
 * @author Dani Schreiber
 * 
 */
public class Args {
    
    private final String _url;
    private final String _domain;
    private final String clientType;
    private final String _project;
    private final String _username;
    private final String _password;
    private final String _runType;
    private final String _entityId;
    private final String _duration;
    private final String _description;
    private final String _postRunAction;
    private final String _environmentConfigurationId;
    
    private final CdaDetails _cdaDetails;
    
    public Args(
            String url,
            String domain,
            String clientType,
            String project,
            String username,
            String password,
            String runType,
            String entityId,
            String duration,
            String description,
            String postRunAction,
            String environmentConfigurationId,
            CdaDetails cdaDetails) {
        
        _url = url;
        _domain = domain;
        this.clientType = clientType;
        _project = project;
        _username = username;
        _password = password;
        _entityId = entityId;
        _runType = runType;
        _duration = duration;
        _description = description;
        _postRunAction = postRunAction;
        _environmentConfigurationId = environmentConfigurationId;
        _cdaDetails = cdaDetails;
    }

    public String getClientType() { return clientType; }
    
    public String getUrl() {
        
        return _url;
    }
    
    public String getDomain() {
        
        return _domain;
    }
    
    public String getProject() {
        
        return _project;
    }
    
    public String getUsername() {
        
        return _username;
    }
    
    public String getPassword() {
        
        return _password;
    }
    
    public String getEntityId() {
        
        return _entityId;
    }
    
    public String getRunType() {
        return _runType;
    }
    
    public String getDuration() {
        
        return _duration;
    }
    
    public String getDescription() {
        
        return _description;
    }
    
    public String getPostRunAction() {
        
        return _postRunAction;
    }
    
    public String getEnvironmentConfigurationId() {
        
        return _environmentConfigurationId;
    }
    
    public CdaDetails getCdaDetails() {
        
        return _cdaDetails;
    }
}
