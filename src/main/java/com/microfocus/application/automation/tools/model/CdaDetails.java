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
