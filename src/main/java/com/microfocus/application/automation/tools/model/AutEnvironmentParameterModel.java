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
package com.microfocus.application.automation.tools.model;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;

/**
 * Created by barush on 21/10/2014.
 */
public class AutEnvironmentParameterModel extends
        AbstractDescribableImpl<AutEnvironmentParameterModel> {
    
    private final String name;
    private final String value;
    private final String paramType;
    private final boolean shouldGetOnlyFirstValueFromJson;
    private String resolvedValue;
    
    public static final List<String> parametersTypes = Arrays.asList(
            AutEnvironmentParameterType.USER_DEFINED.value(),
            AutEnvironmentParameterType.ENVIRONMENT.value(),
            AutEnvironmentParameterType.EXTERNAL.value());
    
    @DataBoundConstructor
    public AutEnvironmentParameterModel(
            String name,
            String value,
            String paramType,
            boolean shouldGetOnlyFirstValueFromJson) {
        
        this.name = name;
        this.value = value;
        this.paramType = paramType;
        this.shouldGetOnlyFirstValueFromJson = shouldGetOnlyFirstValueFromJson;
    }
    
    public boolean isShouldGetOnlyFirstValueFromJson() {
        return shouldGetOnlyFirstValueFromJson;
    }
    
    public String getParamType() {
        return paramType;
    }
    
    public String getValue() {
        return value;
    }
    
    public String getName() {
        return name;
    }
    
    public String getResolvedValue() {
        return resolvedValue;
    }
    
    public void setResolvedValue(String resolvedValue) {
        this.resolvedValue = resolvedValue;
    }
    
    public enum AutEnvironmentParameterType {
        
        UNDEFINED(""), ENVIRONMENT("Environment"), EXTERNAL("From JSON"), USER_DEFINED("Manual");
        
        private String value;
        
        private AutEnvironmentParameterType(String value) {
            
            this.value = value;
        }
        
        public String value() {
            
            return value;
        }
        
        public static AutEnvironmentParameterType get(String val) {
            for (AutEnvironmentParameterType parameterType : AutEnvironmentParameterType.values()) {
                if (val.equals(parameterType.value()))
                    return parameterType;
            }
            return UNDEFINED;
        }
    }
    
    @Extension
    public static class DescriptorImpl extends Descriptor<AutEnvironmentParameterModel> {
        
        public String getDisplayName() {
            return "AUT Parameter";
        }
        
        public List<String> getParametersTypes() {
            return AutEnvironmentParameterModel.parametersTypes;
        }
        
        public FormValidation doCheckName(@QueryParameter String value) {
            
            FormValidation ret = FormValidation.ok();
            if (StringUtils.isBlank(value)) {
                ret = FormValidation.error("Parameter name must be set");
            }
            
            return ret;
        }
        
        public FormValidation doCheckValue(@QueryParameter String value) {
            
            FormValidation ret = FormValidation.ok();
            if (StringUtils.isBlank(value)) {
                ret = FormValidation.warning("You didn't assign any value to this parameter");
            }
            
            return ret;
        }
        
        public String getRandomName(@QueryParameter String prefix) {
            return prefix + UUID.randomUUID();
        }
        
    }
    
}
