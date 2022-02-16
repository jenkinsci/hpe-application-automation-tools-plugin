package com.microfocus.application.automation.tools.model;

import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;

public class MCAuthModel {
    private String mcUserName;
    private Secret mcPassword;
    private String mcTenantId;
    private Secret mcExecToken;
    private String value;

    @DataBoundConstructor
    public MCAuthModel(String mcUserName, String mcPassword, String mcTenantId, String mcExecToken, String value) {
        this.mcUserName = mcUserName;
        this.mcPassword = Secret.fromString(mcPassword);
        this.mcTenantId = mcTenantId;
        this.mcExecToken = Secret.fromString(mcExecToken);
        this.value = value;
    }

    public String getMcUserName() {
        return mcUserName;
    }

    public String getMcPassword() {
        return mcPassword.getPlainText();
    }

    public String getMcTenantId() {
        return mcTenantId;
    }

    public String getMcExecToken() {
        return mcExecToken.getPlainText();
    }

    public void setMcUserName(String mcUserName) {
        this.mcUserName = mcUserName;
    }

    public void setMcPassword(String mcPassword) {
        this.mcPassword = Secret.fromString(mcPassword);
    }

    public void setMcTenantId(String mcTenantId) {
        this.mcTenantId = mcTenantId;
    }

    public void setMcExecToken(String mcExecToken) {
        this.mcExecToken = Secret.fromString(mcExecToken);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    public String getMcEncryptedExecToken() {
        return mcExecToken.getEncryptedValue();
    }
    public String getMcEncryptedPassword() {
        return mcPassword.getEncryptedValue();
    }
}
