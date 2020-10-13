/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2019 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors ("Micro Focus") are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.run;

import com.cloudbees.groovy.cps.impl.ListBlock;
import com.microfocus.application.automation.tools.model.*;
import com.microfocus.application.automation.tools.octane.JellyUtils;
import com.microfocus.application.automation.tools.octane.executor.UftConstants;
import com.microfocus.application.automation.tools.uft.model.FilterTestsModel;
import com.microfocus.application.automation.tools.settings.AlmServerSettingsBuilder;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;

import hudson.model.*;

import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.IOUtils;
import hudson.util.ListBoxModel;
import hudson.util.VariableResolver;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;

import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.microfocus.application.automation.tools.AlmToolsUtils;
import com.microfocus.application.automation.tools.EncryptionUtils;
import com.microfocus.application.automation.tools.run.AlmRunTypes.RunType;

import static com.microfocus.application.automation.tools.Messages.CompanyName;
import static com.microfocus.application.automation.tools.Messages.RunFromAlmBuilderStepName;

public class RunFromAlmBuilder extends Builder implements SimpleBuildStep {

    public RunFromAlmModel runFromAlmModel;
    private boolean isFilterTestsEnabled;
    private FilterTestsModel filterTestsModel;
    private final static String HpToolsLauncher_SCRIPT_NAME = "HpToolsLauncher.exe";
    private String ResultFilename = "ApiResults.xml";
    private String ParamFileName = "ApiRun.txt";
    private AlmServerSettingsModel almServerSettingsModel;

    @DataBoundConstructor
    public RunFromAlmBuilder(
            String almServerName,
            String almUserName,
            String almPassword,
            String almDomain,
            String almProject,
            String almTestSets,
            String almRunResultsMode,
            String almTimeout,
            String almRunMode,
            String almRunHost,
            String almClientID,
            String almApiKey,
            boolean isSSOEnabled,
            boolean isFilterTestsEnabled,
            FilterTestsModel filterTestsModel,
            AlmServerSettingsModel almServerSettingsModel){

        this.isFilterTestsEnabled = isFilterTestsEnabled;
        this.filterTestsModel = filterTestsModel;
        this.almServerSettingsModel = almServerSettingsModel;

        runFromAlmModel =
                new RunFromAlmModel(
                        almServerName,
                        almUserName,
                        almPassword,
                        almDomain,
                        almProject,
                        almTestSets,
                        almRunResultsMode,
                        almTimeout,
                        almRunMode,
                        almRunHost,
                        isSSOEnabled,
                        almClientID,
                        almApiKey);
    }

    public String getAlmServerName(){
        return runFromAlmModel.getAlmServerName();
    }

    public boolean getIsSSOEnabled() { return runFromAlmModel.isSSOEnabled(); }

    public void setIsSSOEnabled(Boolean isSSOEnabled) { runFromAlmModel.setIsSSOEnabled(isSSOEnabled);}

    @DataBoundSetter
    public void setRunFromAlmModel(RunFromAlmModel runFromAlmModel){
        this.runFromAlmModel = runFromAlmModel;
    }

    @DataBoundSetter
    public  void setAlmServerSettingsModel(AlmServerSettingsModel almServerSettingsModel) { this.almServerSettingsModel = almServerSettingsModel; }

    public String getAlmUserName(){ return runFromAlmModel.getAlmUserName(); }

    public String getAlmPassword(){
        return runFromAlmModel.getAlmPassword();
    }

    public String getAlmDomain(){
        return runFromAlmModel.getAlmDomain();
    }

    public String getAlmProject(){
        return runFromAlmModel.getAlmProject();
    }

    public String getAlmTestSets(){
        return runFromAlmModel.getAlmTestSets();
    }

    public String getAlmRunResultsMode(){
        return runFromAlmModel.getAlmRunResultsMode();
    }

    public String getAlmTimeout(){
        return runFromAlmModel.getAlmTimeout();
    }

    public String getAlmRunMode(){
        return runFromAlmModel.getAlmRunMode();
    }

    public String getAlmRunHost(){
        return runFromAlmModel.getAlmRunHost();
    }

    public boolean getIsFilterTestsEnabled() {
        return isFilterTestsEnabled;
    }

    public String getAlmClientID() { return runFromAlmModel.getAlmClientID(); }

    public String getAlmApiKey() { return runFromAlmModel.getAlmApiKey(); }


   @DataBoundSetter
    public void setIsFilterTestsEnabled(boolean isFilterTestsEnabled) {
        this.isFilterTestsEnabled = isFilterTestsEnabled;
    }

    public FilterTestsModel getFilterTestsModel() {
        return filterTestsModel;
    }

    @DataBoundSetter
    public void setFilterTestsModel(FilterTestsModel filterTestsModel) {
        this.filterTestsModel = filterTestsModel;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher,
                        TaskListener listener) throws InterruptedException, IOException {

        // get the alm server settings
        AlmServerSettingsModel almServerSettingsModel = getAlmServerSettingsModel();
        
        if (almServerSettingsModel == null) {
            listener.fatalError("An ALM server is not defined. Go to Manage Jenkins->Configure System and define your ALM server under Application Lifecycle Management");
            
	    // set pipeline stage as failure in case if ALM server was not configured
	    build.setResult(Result.FAILURE);
		
            return;
        }

        EnvVars env = null;
        try {
            env = build.getEnvironment(listener);
        } catch (IOException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
        VariableResolver<String> varResolver = new VariableResolver.ByMap<String>(build.getEnvironment(listener));
        
        // now merge them into one list
        Properties mergedProperties = new Properties();

        mergedProperties.putAll(almServerSettingsModel.getProperties());
        mergedProperties.putAll(runFromAlmModel.getProperties(env, varResolver));

        String encAlmPass = "";
        try {
            String almPassword = "";
            List<CredentialsModel> credentials = almServerSettingsModel.getAlmCredentials();
            for(CredentialsModel model : credentials){
                if(model.getAlmUsername().equals(runFromAlmModel.getAlmUserName())){
                    almPassword = model.getAlmPassword();
                    break;
                }
            }
            encAlmPass =
                    EncryptionUtils.Encrypt(
                            almPassword,
                            EncryptionUtils.getSecretKey());
            
            mergedProperties.remove(RunFromAlmModel.ALM_PASSWORD_KEY);
            mergedProperties.put(RunFromAlmModel.ALM_PASSWORD_KEY, encAlmPass);
            
        } catch (Exception e) {
            build.setResult(Result.FAILURE);
            listener.fatalError("problem with qcPassword encryption");
        }

        String encAlmApiKey = "";
        try{
            String almApiKeySecret = "";
            List<SSOCredentialsModel> ssoCredentials = almServerSettingsModel.getAlmSSOCredentials();
            for(SSOCredentialsModel model : ssoCredentials){
                if(model.getAlmClientID().equals(runFromAlmModel.getAlmClientID())){
                    almApiKeySecret = model.getAlmApiKeySecret();
                    break;
                }
            }

            encAlmApiKey =
                    EncryptionUtils.Encrypt(
                            almApiKeySecret,
                            EncryptionUtils.getSecretKey());
            mergedProperties.remove(RunFromAlmModel.ALM_API_KEY_SECRET);
            mergedProperties.put(RunFromAlmModel.ALM_API_KEY_SECRET, encAlmApiKey);
        }catch (Exception e) {
            build.setResult(Result.FAILURE);
            listener.fatalError("problem with apiKey encryption");
        }

        if(isFilterTestsEnabled){
            filterTestsModel.addProperties(mergedProperties);
        } else {
            mergedProperties.put("FilterTests", "false");
        }
        
        Date now = new Date();
        Format formatter = new SimpleDateFormat("ddMMyyyyHHmmssSSS");
        String time = formatter.format(now);
        
        // get a unique filename for the params file
        ParamFileName = "props" + time + ".txt";
        ResultFilename = "Results" + time + ".xml";
        //KillFileName = "stop" + time + ".txt";
        
        mergedProperties.put("runType", RunType.Alm.toString());
        mergedProperties.put("resultsFilename", ResultFilename);
        
        // get properties serialized into a stream
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            mergedProperties.store(stream, "");
        } catch (IOException e) {
            build.setResult(Result.FAILURE);
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String propsSerialization = stream.toString();
        InputStream propsStream = IOUtils.toInputStream(propsSerialization);
        
        // get the remote workspace filesys
        FilePath projectWS = workspace;
        
        // Get the URL to the Script used to run the test, which is bundled
        // in the plugin

        URL cmdExeUrl =
                Hudson.getInstance().pluginManager.uberClassLoader.getResource(HpToolsLauncher_SCRIPT_NAME);
        if (cmdExeUrl == null) {
            listener.fatalError(HpToolsLauncher_SCRIPT_NAME + " not found in resources");
            return;
        }

        FilePath propsFileName = projectWS.child(ParamFileName);
        FilePath CmdLineExe = projectWS.child(HpToolsLauncher_SCRIPT_NAME);

        try {
            // create a file for the properties file, and save the properties
            propsFileName.copyFrom(propsStream);

            // Copy the script to the project workspace
            CmdLineExe.copyFrom(cmdExeUrl);
        } catch (IOException e1) {
            build.setResult(Result.FAILURE);
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            // Run the HpToolsLauncher.exe
            AlmToolsUtils.runOnBuildEnv(build, launcher, listener, CmdLineExe, ParamFileName);
        } catch (IOException ioe) {
            Util.displayIOException(ioe, listener);
            build.setResult(Result.FAILURE);
            return;
        } catch (InterruptedException e) {
            build.setResult(Result.ABORTED);
            PrintStream out = listener.getLogger();
            // kill processes
            //FilePath killFile = projectWS.child(KillFileName);
            /* try {
                out.println("Sending abort command");
                killFile.write("\n", "UTF-8");
                while (!killFile.exists())
                    Thread.sleep(1000);
                Thread.sleep(1500);
                
            } catch (IOException e1) {
                //build.setResult(Result.FAILURE);
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (InterruptedException e1) {
                //build.setResult(Result.FAILURE);
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }*/
            
    			try {
    				AlmToolsUtils.runHpToolsAborterOnBuildEnv(build, launcher, listener, ParamFileName, workspace);
    			} catch (IOException e1) {
    				Util.displayIOException(e1, listener);
    				build.setResult(Result.FAILURE);
    				return;
    		} catch (InterruptedException e1) {
    				// TODO Auto-generated catch block
    				e1.printStackTrace();
    			}            	
            	
            out.println("Operation was aborted by user.");
            //build.setResult(Result.FAILURE);
        }
        return;
        
    }
    
    public AlmServerSettingsModel getAlmServerSettingsModel() {
        for (AlmServerSettingsModel almServer : getDescriptor().getAlmServers()) {
            if (runFromAlmModel != null && runFromAlmModel.getAlmServerName().equals(almServer.getAlmServerName())) {
                    return almServer;
                }
        }

        return null;
    }

    public RunFromAlmModel getRunFromAlmModel() {
        return runFromAlmModel;
    }


    // This indicates to Jenkins that this is an implementation of an extension
    // point.
    @Extension
    // To expose this builder in the Snippet Generator.
    @Symbol("runFromAlmBuilder")
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        
        public DescriptorImpl() {
            load();
        }
        
        @Override
        public boolean isApplicable(
                @SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {
            return true;
        }
        
        @Override
        public String getDisplayName() {
            return RunFromAlmBuilderStepName(CompanyName());
        }


        public boolean hasAlmServers() {
            return Hudson.getInstance().getDescriptorByType(
                    AlmServerSettingsBuilder.DescriptorImpl.class).hasAlmServers();
        }
        
        public Set<AlmServerSettingsModel> getAlmServers() {
            Set<AlmServerSettingsModel> almServers = new HashSet<>();
            for (AlmServerSettingsModel almServer : Jenkins.getInstanceOrNull().getDescriptorByType(AlmServerSettingsBuilder.DescriptorImpl.class).getInstallations())
            {
                almServers.add(almServer);
            }
            return almServers;
        }

        public Set<String> getAlmServerNames(){
            Set<String> almServers = new HashSet<>();
            for (AlmServerSettingsModel almServer : Hudson.getInstance().getDescriptorByType(AlmServerSettingsBuilder.DescriptorImpl.class).getInstallations())
            {
                almServers.add(almServer.getAlmServerName());
            }
            return almServers;
        }

        public List<String> getAlmUsernames(String almServerName) {
            List<String> usernames = new ArrayList<>();
            Set<AlmServerSettingsModel> serverList = getAlmServers();
            for (AlmServerSettingsModel model: serverList) {
                    if (model.getAlmCredentials().get(0).getAlmUsername() != null && model.getAlmCredentials().get(0).getAlmUsername() != "" && model.getAlmServerName().equals(almServerName)) {
                        usernames.add(model.getAlmCredentials().get(0).getAlmUsername());
                    }
            }

            return usernames;
        }


       public List<String> getAlmClientIds(String almServerName) {
            List<String> clientIDList = new ArrayList<>();
            Set<AlmServerSettingsModel> serverList = getAlmServers();
            for (AlmServerSettingsModel model: serverList) {
                if(!model.getAlmCredentials().isEmpty()  && model.getAlmServerName().equals(almServerName)){
                    for(SSOCredentialsModel ssoCredentialsModel : model.getAlmSSOCredentials()) {
                        clientIDList.add(ssoCredentialsModel.getAlmClientID());
                    }
                }
            }

            return clientIDList;
        }

        public ListBoxModel doFillAlmServerNameItems() {
            ListBoxModel m = new ListBoxModel();
            Set<String> serverList = getAlmServerNames();
            for(String server: serverList){
                    m.add(server);
            }
            return m;
        }




        public ListBoxModel doFillAlmUserNameItems(@QueryParameter String almServerName) {
            ListBoxModel m = new ListBoxModel();
            Set<AlmServerSettingsModel> serverList = getAlmServers();
            for (AlmServerSettingsModel model: serverList) {
                if (model.getAlmServerName().equals(almServerName)) {
                    if(!model.getAlmCredentials().isEmpty()) {
                        for (CredentialsModel credentialsModel : model.getAlmCredentials()) {
                            m.add(credentialsModel.getAlmUsername());
                        }
                    }
                }
            }
            if(m.size() == 0 && !StringUtils.isEmpty(almServerName)){
               m.add(UftConstants.NO_USERNAME_DEFINED);
            }


            if (m.size() == 0 && StringUtils.isEmpty(almServerName)) {//new job
                Set<String> serverNames = getAlmServerNames();
                for (AlmServerSettingsModel model: serverList) {
                    for (String serverName : serverNames) {
                        if (model.getAlmServerName().equals(serverName)) {
                            for (CredentialsModel credentialsModel : model.getAlmCredentials()) {
                                m.add(credentialsModel.getAlmUsername());
                            }
                        }
                    }
                }
               if (m.size() == 0) {
                    m.add(UftConstants.NO_USERNAME_DEFINED);
               }
            }

            return m;
        }

        public ListBoxModel doFillAlmClientIDItems(@QueryParameter String almServerName){
            ListBoxModel m = new ListBoxModel();
            Set<AlmServerSettingsModel> serverList = getAlmServers();
            for (AlmServerSettingsModel model: serverList) {
                if(model.getAlmServerName().equals(almServerName)){
                    if(!model.getAlmSSOCredentials().isEmpty()) {
                        for (SSOCredentialsModel ssoCredentialsModel : model.getAlmSSOCredentials()) {
                            m.add(ssoCredentialsModel.getAlmClientID());
                        }
                    }
                }

            }

            if(m.size() == 0 && !StringUtils.isEmpty(almServerName)){
                m.add(UftConstants.NO_CLIENT_ID_DEFINED);
            }

            if (m.size() == 0 && StringUtils.isEmpty(almServerName)) {//new job
                Set<String> serverNames = getAlmServerNames();
                for (AlmServerSettingsModel model: serverList) {
                    for (String serverName : serverNames) {
                        if (model.getAlmServerName().equals(serverName)) {
                            for (SSOCredentialsModel ssoCredentialsModel : model.getAlmSSOCredentials()) {
                                m.add(ssoCredentialsModel.getAlmClientID());
                            }
                        }
                    }
                }
                if(m.size() == 0){
                        m.add(UftConstants.NO_CLIENT_ID_DEFINED);
                }
            }

            return m;
        }


        public FormValidation doCheckAlmTimeout(@QueryParameter String value) {

            if (StringUtils.isEmpty(value)) {
                return FormValidation.ok();
            }
            
            String val1 = value.trim();
            
            if (val1.length() > 0 && val1.charAt(0) == '-')
                val1 = val1.substring(1);
            
            if (!StringUtils.isNumeric(val1) && val1 != "") {
                return FormValidation.error("Timeout name must be a number");
            }
            return FormValidation.ok();
        }
        
        public FormValidation doCheckAlmPassword(@QueryParameter String value) {
            return FormValidation.ok();
        }
        
        public FormValidation doCheckAlmDomain(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Domain must be set");
            }
            
            return FormValidation.ok();
        }
        
        public FormValidation doCheckAlmProject(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Project must be set");
            }
            
            return FormValidation.ok();
        }
        
        public FormValidation doCheckAlmTestSets(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Test sets are missing");
            }
            
            String[] testSetsArr = value.replaceAll("\r", "").split("\n");

			for (int i=0; i < testSetsArr.length; i++) {
				if (StringUtils.isBlank(testSetsArr[i])) {
					return FormValidation.error("Test sets should not contains empty lines");
				}
			}
            return FormValidation.ok();
        }
        
        public List<EnumDescription> getAlmRunModes() {
            return RunFromAlmModel.runModes;
        }

    }
    
    public String getRunResultsFileName() {
        return ResultFilename;
    }
}
