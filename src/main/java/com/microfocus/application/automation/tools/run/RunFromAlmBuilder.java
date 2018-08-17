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

package com.microfocus.application.automation.tools.run;

import com.microfocus.application.automation.tools.settings.AlmServerSettingsBuilder;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;

import hudson.model.Result;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Run;
import hudson.model.TaskListener;

import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.IOUtils;
import hudson.util.VariableResolver;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.microfocus.application.automation.tools.AlmToolsUtils;
import com.microfocus.application.automation.tools.EncryptionUtils;
import com.microfocus.application.automation.tools.model.AlmServerSettingsModel;
import com.microfocus.application.automation.tools.model.EnumDescription;
import com.microfocus.application.automation.tools.model.RunFromAlmModel;
import com.microfocus.application.automation.tools.run.AlmRunTypes.RunType;

public class RunFromAlmBuilder extends Builder implements SimpleBuildStep {
    
    private final RunFromAlmModel runFromAlmModel;
    private final static String HpToolsLauncher_SCRIPT_NAME = "HpToolsLauncher.exe";
    private String ResultFilename = "ApiResults.xml";
    private String ParamFileName = "ApiRun.txt";
    //private String KillFileName = "";
    
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
            String almRunHost) {
        
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
                        almRunHost);
    }

    public String getAlmServerName(){
        return runFromAlmModel.getAlmServerName();
    }

    public String getAlmUserName(){
        return runFromAlmModel.getAlmUserName();
    }

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
            
            encAlmPass =
                    EncryptionUtils.Encrypt(
                            runFromAlmModel.getAlmPassword(),
                            EncryptionUtils.getSecretKey());
            
            mergedProperties.remove(RunFromAlmModel.ALM_PASSWORD_KEY);
            mergedProperties.put(RunFromAlmModel.ALM_PASSWORD_KEY, encAlmPass);
            
        } catch (Exception e) {
            build.setResult(Result.FAILURE);
            listener.fatalError("problem in qcPassword encription");
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
            if (this.runFromAlmModel != null
                && runFromAlmModel.getAlmServerName().equals(almServer.getAlmServerName())) {
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
            return "Execute functional tests from Micro Focus ALM";
        }
        
        public boolean hasAlmServers() {
            return Hudson.getInstance().getDescriptorByType(
                    AlmServerSettingsBuilder.DescriptorImpl.class).hasAlmServers();
        }
        
        public AlmServerSettingsModel[] getAlmServers() {
            return Hudson.getInstance().getDescriptorByType(
                    AlmServerSettingsBuilder.DescriptorImpl.class).getInstallations();
        }
        
        public FormValidation doCheckAlmUserName(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("User name must be set");
            }
            
            return FormValidation.ok();
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
            // if (StringUtils.isBlank(value)) {
            // return FormValidation.error("Password must be set");
            // }
            
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
                return FormValidation.error("Testsets are missing");
            }
            
            String[] testSetsArr = value.replaceAll("\r", "").split("\n");

			for (int i=0; i < testSetsArr.length; i++) {
				if (StringUtils.isBlank(testSetsArr[i])) {
					return FormValidation.error("Testsets should not contains empty lines");
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
