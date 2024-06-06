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

import com.microfocus.application.automation.tools.sse.common.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

public class CdaDetails {
    
    private final String _deploymentAction;
    private final String _deployedEnvironmentName;
    private final String _deprovisioningAction;
    
    private final static EnumDescription _deploymentActionUseDeployed = new EnumDescription(
            "",
            "Use Deployed");
    private final static EnumDescription _deploymentActionProvisionDeploy = new EnumDescription(
            "Provision;Deploy",
            "Provision and Deploy");
    private final static EnumDescription _deploymentActionRedeploy = new EnumDescription(
            "Deploy",
            "Redeploy");
    private final static List<EnumDescription> _deploymentActions = Arrays.asList(
            _deploymentActionUseDeployed,
            _deploymentActionProvisionDeploy,
            _deploymentActionRedeploy);
    
    private final static EnumDescription _deprovisioningActionLeaveDeployed = new EnumDescription(
            "",
            "Leave environment deployed");
    private final static EnumDescription _deprovisioningActionDeprovision = new EnumDescription(
            "Deprovision",
            "Deprovision at end");
    private final static List<EnumDescription> _deprovisioningActions = Arrays.asList(
            _deprovisioningActionLeaveDeployed,
            _deprovisioningActionDeprovision);
    
    @DataBoundConstructor
    public CdaDetails(
            String deploymentAction,
            String deployedEnvironmentName,
            String deprovisioningAction) {
        
        _deploymentAction = deploymentAction;
        _deployedEnvironmentName = deployedEnvironmentName;
        _deprovisioningAction = deprovisioningAction;
    }
    
    public static List<EnumDescription> getDeploymentActions() {
        
        return _deploymentActions;
    }
    
    public static List<EnumDescription> getDeprovisioningActions() {
        
        return _deprovisioningActions;
    }
    
    public String getTopologyAction() {
        
        String ret = _deploymentAction;
        if (!StringUtils.isNullOrEmpty(_deprovisioningAction)) {
            ret = String.format("%s;%s", ret, _deprovisioningAction);
        }
        
        return ret;
    }
    
    public String getDeploymentAction() {
        
        return _deploymentAction;
    }
    
    public String getDeployedEnvironmentName() {
        
        return _deployedEnvironmentName;
    }
    
    public String getDeprovisioningAction() {
        
        return _deprovisioningAction;
    }
}
