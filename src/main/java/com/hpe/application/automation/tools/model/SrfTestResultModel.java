package com.hpe.application.automation.tools.model;

/**
 * Created by shepshel on 29/09/2016.
 */



import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

/**
 * Created by shepshel on 27/07/2016.
 */
public class SrfTestResultModel extends AbstractDescribableImpl<SrfTestResultModel> {

    @DataBoundConstructor
    public SrfTestResultModel(){

    }
    @Extension
    public static class DescriptorImpl extends Descriptor<SrfTestResultModel> {
        public String getDisplayName() {
            return "SrfTestResult";
        }


    }
}

