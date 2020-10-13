package com.microfocus.application.automation.tools.model;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.Serializable;


public class CredentialsModel extends AbstractDescribableImpl<CredentialsModel> implements Serializable {
    private String almUsername;
    private Secret almPassword;

    @DataBoundConstructor
    public CredentialsModel(String almUsername, String almPassword) {
        this.almUsername = almUsername;
        this.almPassword = Secret.fromString(almPassword);
    }

    public String getAlmUsername(){
        return almUsername;
    }

    public String getAlmPassword(){
        return almPassword.getPlainText();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<CredentialsModel> {
        @Nonnull
        @Override
        public String getDisplayName() {
            return "Credentials Model";
        }
    }
}
