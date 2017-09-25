package com.hpe.application.automation.tools.model;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.util.UUID;
/**
 * Created by shepshel on 27/07/2016.
 */
public class SrfTestParamsModel extends AbstractDescribableImpl<SrfTestParamsModel> {
    private final String name;
    private final String value;
    private String resolvedValue;
    private  Boolean shouldGetOnlyFirstValueFromJson;
    @DataBoundConstructor
    public SrfTestParamsModel(
            String name,
            String value,
			boolean shouldGetOnlyFirstValueFromJson) {
        this.name = name;
        this.value = value;
 
        this.shouldGetOnlyFirstValueFromJson = shouldGetOnlyFirstValueFromJson;
    }
    public boolean isShouldGetOnlyFirstValueFromJson() {
        return shouldGetOnlyFirstValueFromJson;
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
    @Extension
    public static class DescriptorImpl extends Descriptor<SrfTestParamsModel> {
        public String getDisplayName() {
            return "";
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
