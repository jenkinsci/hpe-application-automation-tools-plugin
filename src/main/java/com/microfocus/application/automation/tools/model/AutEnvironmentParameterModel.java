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
