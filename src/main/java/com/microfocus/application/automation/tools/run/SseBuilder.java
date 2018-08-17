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

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import com.microfocus.application.automation.tools.model.AlmServerSettingsModel;
import com.microfocus.application.automation.tools.model.CdaDetails;
import com.microfocus.application.automation.tools.model.EnumDescription;
import com.microfocus.application.automation.tools.model.SseModel;
import com.microfocus.application.automation.tools.settings.AlmServerSettingsBuilder;
import com.microfocus.application.automation.tools.sse.result.model.junit.Testcase;
import com.microfocus.application.automation.tools.sse.result.model.junit.Testsuite;
import com.microfocus.application.automation.tools.sse.result.model.junit.Testsuites;
import com.microfocus.application.automation.tools.sse.sdk.Logger;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;
import com.microfocus.application.automation.tools.sse.SSEBuilderPerformer;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.*;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.VariableResolver;
import jenkins.tasks.SimpleBuildStep;

/***
 * This Jenkins plugin contains an unofficial implementation of some of the elements of the HPE ALM
 * Lab Management SDK. Users are free to use this plugin as they wish, but HPE does not take
 * responsibility for supporting or providing backwards compatibility for the functionality herein.
 * 
 * 
 * @author Effi Bar-She'an
 * @author Dani Schreiber
 * 
 */
public class SseBuilder extends Builder implements SimpleBuildStep {
    
    private SseModel _sseModel;
    private String _fileName;
    
    private String almServerName;
    private String credentialsId;
    private String clientType;
    private String almDomain;
    private String almProject;
    private String description;
    private String runType;
    private String almEntityId;
    private String timeslotDuration;
    private String postRunAction;
    private String environmentConfigurationId;
    private CdaDetails cdaDetails;
    
    //Databound setters and getters.
    public String getAlmServerName() { return almServerName; }
    public String getCredentialsId() { return credentialsId; }
    public String getClientType() { return clientType; }
    public String getAlmDomain() { return almDomain; }
    public String getAlmProject() { return almProject; }
    public String getDescription() { return description; }
    public String getRunType() { return runType; }
    public String getAlmEntityId() { return almEntityId; }
    public String getTimeslotDuration() { return timeslotDuration; }
    public String getPostRunAction() { return postRunAction; }
    public String getEnvironmentConfigurationId() { return environmentConfigurationId; }
    public CdaDetails getCdaDetails() { return cdaDetails; }

    public boolean isCdaDetailsChecked() {
        return cdaDetails != null;
    }
    
    @DataBoundSetter
    public void setDescription(String description) { this.description = description; }
    
    @DataBoundSetter
    public void setPostRunAction(String postRunAction) { this.postRunAction = postRunAction; }
    
    @DataBoundSetter
    public void setEnvironmentConfigurationId(String environmentConfigurationId) {
        this.environmentConfigurationId = environmentConfigurationId;
    }
    
    @DataBoundSetter
    public void setCdaDetails(CdaDetails cdaDetails) { this.cdaDetails = cdaDetails; }
    
    /**
     * Should only contains mandatory properties.
     */
    @DataBoundConstructor
    public SseBuilder(String almServerName,
    		String almProject,
    		String credentialsId,
    		String clientType,
    		String almDomain,
    		String runType,
    		String almEntityId,
            String timeslotDuration) {
    	
		this.almServerName = almServerName;
		this.credentialsId = credentialsId;
		this.almProject = almProject;
		this.almDomain = almDomain;
		this.timeslotDuration = timeslotDuration;
		this.runType = runType;
		this.almEntityId = almEntityId;
		this.clientType = clientType;
	}
    
    @Override
    public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher,
            TaskListener listener) throws InterruptedException, IOException {

        PrintStream logger = listener.getLogger();
    	
        UsernamePasswordCredentials credentials = getCredentialsById(credentialsId, build, logger);
    	
    	_sseModel = new SseModel(
                almServerName,
                credentials.getUsername(),
                credentials.getPassword().getPlainText(),
                almDomain,
                clientType,
                almProject,
                runType,
                almEntityId,
                timeslotDuration,
                description,
                postRunAction,
                environmentConfigurationId,
                cdaDetails);
    	
        _sseModel.setAlmServerUrl(getServerUrl(_sseModel.getAlmServerName()));
        
        VariableResolver<String> varResolver = new VariableResolver.ByMap<String>(build.getEnvironment(listener));
        Testsuites testsuites = execute(build, logger, varResolver);
        
        FilePath resultsFilePath = workspace.child(getFileName());
        Result resultStatus = createRunResults(resultsFilePath, testsuites, logger);
        provideStepResultStatus(resultStatus, build, logger);
    }
    
    /**
     * Get user name password credentials by id.
     */
    private UsernamePasswordCredentials getCredentialsById(String credentialsId, Run<?, ?> run, PrintStream logger) {
    	if (StringUtils.isBlank(credentialsId)) {
    		throw new NullPointerException("credentials is not configured.");
    	}
    	
    	UsernamePasswordCredentials credentials = CredentialsProvider.findCredentialById(credentialsId,
        		StandardUsernamePasswordCredentials.class,
        		run,
    			URIRequirementBuilder.create().build());
    	
    	if (credentials == null) {
    		logger.println("Can not find credentials with the credentialsId:" + credentialsId);
    	}
    	return credentials;
    }
        
    public AlmServerSettingsModel getAlmServerSettingsModel() {
        AlmServerSettingsModel ret = null;
        for (AlmServerSettingsModel almServer : getDescriptor().getAlmServers()) {
            if (this.almServerName.equals(almServer.getAlmServerName())) {
                ret = almServer;
                break;
            }
        }
        return ret;
    }
    
    private void provideStepResultStatus(
            Result resultStatus,
            Run<?, ?> build,
            PrintStream logger) {
        
        logger.println(String.format("Result Status: %s", resultStatus.toString()));
        build.setResult(resultStatus);
        
    }
    
    private Testsuites execute(
            Run<?, ?> build,
            PrintStream logger,
            VariableResolver<String> buildVariableResolver) throws InterruptedException {
        
        Testsuites ret = null;
        SSEBuilderPerformer performer = null;
        try {
            performer = new SSEBuilderPerformer();
            ret = execute(performer, logger, buildVariableResolver);
        } catch (InterruptedException e) {
            build.setResult(Result.ABORTED);
            stop(performer, logger);
            throw e;
        } catch (Exception cause) {
            build.setResult(Result.FAILURE);
            logger.print(String.format("Failed to execute test, Exception: %s", cause.getMessage()));
        }
        
        return ret;
    }
    
    private Result createRunResults(FilePath filePath, Testsuites testsuites, PrintStream logger) {
        
        Result ret = Result.SUCCESS;
        try {
            if (testsuites != null) {
                StringWriter writer = new StringWriter();
                JAXBContext context = JAXBContext.newInstance(Testsuites.class);
                Marshaller marshaller = context.createMarshaller();
                marshaller.marshal(testsuites, writer);
                filePath.write(writer.toString(), null);
                if (containsErrors(testsuites.getTestsuite())) {
                    ret = Result.UNSTABLE;
                }
            } else {
                logger.println("Empty Results");
                ret = Result.UNSTABLE;
            }
            
        } catch (Exception cause) {
            logger.print(String.format(
                    "Failed to create run results, Exception: %s",
                    cause.getMessage()));
            ret = Result.UNSTABLE;
        }
        
        return ret;
    }
    
    private boolean containsErrors(List<Testsuite> testsuites) {
        
        boolean ret = false;
        for (Testsuite testsuite : testsuites) {
            for (Testcase testcase : testsuite.getTestcase()) {
                if ("error".equals(testcase.getStatus())) {
                    ret = true;
                    break;
                }
            }
        }
        
        return ret;
    }
    
    private String getFileName() {
        
        Format formatter = new SimpleDateFormat("ddMMyyyyHHmmssSSS");
        String time = formatter.format(new Date());
        _fileName = String.format("Results%s.xml", time);
        return _fileName;
    }
    
    private void stop(SSEBuilderPerformer performer, PrintStream logger) {
        
        try {
            if (performer != null) {
                performer.stop();
            }
        } catch (Exception cause) {
            logger.println(String.format("Failed to stop BVS. Exception: %s", cause.getMessage()));
        }
    }
    
    private Testsuites execute(
            SSEBuilderPerformer performer,
            final PrintStream logger,
            VariableResolver<String> buildVariableResolver) throws InterruptedException,
            IOException {
        
        return performer.start(_sseModel, new Logger() {
            
            @Override
            public void log(String message) {
                
                logger.println(message);
            }
        }, buildVariableResolver);
    }
    
    public String getServerUrl(String almServerName) {
        
        String ret = "";
        AlmServerSettingsModel[] almServers = getDescriptor().getAlmServers();
        if (almServers != null && almServers.length > 0) {
            for (AlmServerSettingsModel almServer : almServers) {
                if (almServerName.equals(almServer.getAlmServerName())) {
                    ret = almServer.getAlmServerUrl();
                    break;
                }
            }
        }
        
        return ret;
    }
    
    public SseModel getSseModel() {
        
        return _sseModel;
    }
    
    public String getRunResultsFileName() {
        
        return _fileName;
    }
    
    @Override
    public DescriptorImpl getDescriptor() {
        
        return (DescriptorImpl) super.getDescriptor();
    }
    
    // This indicates to Jenkins that this is an implementation of an extension point
    @Extension
    // To expose this builder in the Snippet Generator.
    @Symbol("sseBuild")
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
            
            return "Execute tests using ALM Lab Management";
        }
        
        public boolean hasAlmServers() {
            
            return Hudson.getInstance().getDescriptorByType(
                    AlmServerSettingsBuilder.DescriptorImpl.class).hasAlmServers();
        }
        
        public AlmServerSettingsModel[] getAlmServers() {
            return Hudson.getInstance().getDescriptorByType(
                    AlmServerSettingsBuilder.DescriptorImpl.class).getInstallations();
        }
        
        public FormValidation doCheckTimeslotDuration(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
            	return FormValidation.error("Timeslot duration must be set");
            }
            
            String val1 = value.trim();
            
            if (!StringUtils.isNumeric(val1)) {
            	return FormValidation.error("Timeslot duration must be a number");
            }
            
            if (Integer.valueOf(val1) < 30) {
            	return FormValidation.error("Timeslot duration must be higher than 30");
            }
            
            return FormValidation.ok();
        }
        
        public FormValidation doCheckAlmDomain(@QueryParameter String value) {
            
            FormValidation ret = FormValidation.ok();
            if (StringUtils.isBlank(value)) {
                ret = FormValidation.error("Domain must be set");
            }
            
            return ret;
        }
        
        public FormValidation doCheckAlmProject(@QueryParameter String value) {
            
            FormValidation ret = FormValidation.ok();
            if (StringUtils.isBlank(value)) {
                ret = FormValidation.error("Project must be set");
            }
            
            return ret;
        }
        
        public FormValidation doCheckAlmEntityId(@QueryParameter String value) {
            
            FormValidation ret = FormValidation.ok();
            if (StringUtils.isBlank(value)) {
                ret = FormValidation.error("Entity ID must be set.");
            }
            
            return ret;
        }
        
        public List<EnumDescription> getRunTypes() {
            
            return SseModel.getRunTypes();
        }
        
        public List<EnumDescription> getPostRunActions() {
            
            return SseModel.getPostRunActions();
        }
        
        public List<EnumDescription> getDeploymentActions() {
            
            return CdaDetails.getDeploymentActions();
        }
        
        public static List<EnumDescription> getDeprovisioningActions() {
            
            return CdaDetails.getDeprovisioningActions();
        }
        
        /**
         * To fill in the credentials drop down list which's field is 'credentialsId'.
         * This method's name works with tag <c:select/>.
         */
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item project,
                @QueryParameter String credentialsId) {
        	
			if (project == null || !project.hasPermission(Item.CONFIGURE)) {
                return new StandardUsernameListBoxModel().includeCurrentValue(credentialsId);
            }
            return new StandardUsernameListBoxModel()
                    .includeEmptyValue()
                    .includeAs(
                    		project instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task) project) : ACL.SYSTEM,
                            project,
                            StandardUsernamePasswordCredentials.class,
                            URIRequirementBuilder.create().build())
                    .includeCurrentValue(credentialsId);
		}
        
        public FormValidation doCheckCredentialsId(@AncestorInPath Item project,
                @QueryParameter String url,
                @QueryParameter String value) {
			if (project == null || !project.hasPermission(Item.EXTENDED_READ)) {
				return FormValidation.ok();
			}
			
			value = Util.fixEmptyAndTrim(value);
			if (value == null) {
				return FormValidation.ok();
			}
			
			url = Util.fixEmptyAndTrim(url);
			if (url == null)
			// not set, can't check
			{
				return FormValidation.ok();
			}
			
			if (url.indexOf('$') >= 0)
			// set by variable, can't check
			{
				return FormValidation.ok();
			}
			
			for (ListBoxModel.Option o : CredentialsProvider.listCredentials(
						StandardUsernamePasswordCredentials.class, 
						project, 
						project instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task) project) : ACL.SYSTEM,
						URIRequirementBuilder.create().build(),
						new IdMatcher(value))) {
				
				if (StringUtils.equals(value, o.value)) {
					return FormValidation.ok();
				}
			}
			// no credentials available, can't check
			return FormValidation.warning("Cannot find any credentials with id " + value);
		}
    }
}
