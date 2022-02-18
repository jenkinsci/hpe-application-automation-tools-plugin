package com.microfocus.application.automation.tools.model;

import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;

public class AuthModel implements Serializable {
    private String mcUserName;
    private Secret mcPassword;
    private String mcTenantId;
    private Secret mcExecToken;
    private String value;

    @DataBoundConstructor
    public AuthModel(String mcUserName, String mcPassword, String mcTenantId, String mcExecToken, String value) {
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
        if (null != mcPassword) {
            return mcPassword.getPlainText();
        } else {
            return null;
        }
    }

    public String getMcTenantId() {
        return mcTenantId;
    }

    public String getMcExecToken() {
        if (null != mcExecToken) {
            return mcExecToken.getPlainText();
        } else {
            return null;
        }
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
        if (null != mcExecToken) {
            return mcExecToken.getEncryptedValue();
        } else {
            return null;
        }
    }

    public String getMcEncryptedPassword() {
        if (null != mcPassword) {
            return mcPassword.getEncryptedValue();
        } else {
            return null;
        }
    }
}
