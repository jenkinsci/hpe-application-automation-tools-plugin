package com.microfocus.application.automation.tools.model;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.Serializable;

public class SSOCredentialsModel extends AbstractDescribableImpl<SSOCredentialsModel> implements Serializable {
    private String almClientID;
    private Secret almApiKeySecret;

    @DataBoundConstructor
    public SSOCredentialsModel(String almClientID, String almApiKeySecret) {
        this.almClientID = almClientID;
        this.almApiKeySecret = Secret.fromString(almApiKeySecret);
    }

    public String getAlmClientID(){
        return almClientID;
    }

    public String getAlmApiKeySecret(){
        return almApiKeySecret.getPlainText();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<SSOCredentialsModel> {
        @Nonnull
        @Override
        public String getDisplayName() {
            return "SSO Credentials Model";
        }
    }
}
