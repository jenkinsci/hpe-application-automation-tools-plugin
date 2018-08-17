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

package com.microfocus.application.automation.tools.results;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;
import com.microfocus.application.automation.tools.model.AlmServerSettingsModel;
import com.microfocus.application.automation.tools.model.EnumDescription;
import com.microfocus.application.automation.tools.results.service.AlmRestInfo;
import com.microfocus.application.automation.tools.results.service.AlmRestTool;
import com.microfocus.application.automation.tools.results.service.ExternalEntityUploadLogger;
import com.microfocus.application.automation.tools.results.service.IExternalEntityUploadService;
import com.microfocus.application.automation.tools.settings.AlmServerSettingsBuilder;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.tasks.test.TestResultAggregator;
import hudson.tasks.test.TestResultProjectAction;
import hudson.util.FormValidation;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import hudson.util.ListBoxModel;
import hudson.util.VariableResolver;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.microfocus.application.automation.tools.model.UploadTestResultToAlmModel;
import com.microfocus.application.automation.tools.results.service.DefaultExternalEntityUploadServiceImpl;

/**

 * 
 * @author Jacky Zhu
 */
public class TestResultToALMUploader extends Recorder implements Serializable, MatrixAggregatable {
    
    private static final long serialVersionUID = 1L;
    private UploadTestResultToAlmModel uploadTestResultToAlmModel;
    private String almServerName;
    private String credentialsId;
    private String almDomain;
    private String clientType;
    private String almProject;
    private String testingFramework;
    private String testingTool;
    private String almTestFolder;
    private String almTestSetFolder;
    private String almTimeout;
    private String testingResultFile;
    private String jenkinsServerUrl;

    // These getters setters work for reading config.xml.
    public UploadTestResultToAlmModel getUploadTestResultToAlmModel() {
        return uploadTestResultToAlmModel;
    }

    public void setUploadTestResultToAlmModel(UploadTestResultToAlmModel uploadTestResultToAlmModel) {
        this.uploadTestResultToAlmModel = uploadTestResultToAlmModel;
    }

    public String getAlmServerName() {
        return almServerName;
    }

    public void setAlmServerName(String almServerName) {
        this.almServerName = almServerName;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public String getAlmDomain() {
        return almDomain;
    }

    public void setAlmDomain(String almDomain) {
        this.almDomain = almDomain;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public String getAlmProject() {
        return almProject;
    }

    public void setAlmProject(String almProject) {
        this.almProject = almProject;
    }

    public String getTestingFramework() {
        return testingFramework;
    }

    public void setTestingFramework(String testingFramework) {
        this.testingFramework = testingFramework;
    }

    public String getTestingTool() {
        return testingTool;
    }

    public void setTestingTool(String testingTool) {
        this.testingTool = testingTool;
    }

    public String getAlmTestFolder() {
        return almTestFolder;
    }

    public void setAlmTestFolder(String almTestFolder) {
        this.almTestFolder = almTestFolder;
    }

    public String getAlmTestSetFolder() {
        return almTestSetFolder;
    }

    public void setAlmTestSetFolder(String almTestSetFolder) {
        this.almTestSetFolder = almTestSetFolder;
    }

    public String getAlmTimeout() {
        return almTimeout;
    }

    public void setAlmTimeout(String almTimeout) {
        this.almTimeout = almTimeout;
    }

    public String getTestingResultFile() {
        return testingResultFile;
    }

    public void setTestingResultFile(String testingResultFile) {
        this.testingResultFile = testingResultFile;
    }

    public String getJenkinsServerUrl() {
        return jenkinsServerUrl;
    }

    public void setJenkinsServerUrl(String jenkinsServerUrl) {
        this.jenkinsServerUrl = jenkinsServerUrl;
    }

    @DataBoundConstructor
    public TestResultToALMUploader(
            String almServerName,
            String credentialsId,
            String almDomain,
            String clientType,
            String almProject,
            String testingFramework,
            String testingTool,
            String almTestFolder,
            String almTestSetFolder,
            String almTimeout,
            String testingResultFile,
            String jenkinsServerUrl) {

        this.almServerName = almServerName;
        this.credentialsId = credentialsId;
        this.almDomain = almDomain;
        this.clientType = clientType;
        this.almProject = almProject;
        this.testingFramework = testingFramework;
        this.testingTool = testingTool;
        this.almTestFolder = almTestFolder;
        this.almTestSetFolder = almTestSetFolder;
        this.almTimeout = almTimeout;
        this.testingResultFile = testingResultFile;
        this.jenkinsServerUrl = jenkinsServerUrl;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
    	ExternalEntityUploadLogger logger = new ExternalEntityUploadLogger(listener.getLogger());

    	// Credentials id maybe can't be blank
        if (StringUtils.isBlank(credentialsId)) {
            logger.log("INFO: credentials is not configured.");
            build.setResult(Result.UNSTABLE);
            return true;
        }
        UsernamePasswordCredentials credentials = getCredentialsById(credentialsId, build, logger);


    	logger.log(String.format("INFO: 'Upload test result to ALM' Post Build Step is being invoked by %s.",
                credentials.getUsername()));

        uploadTestResultToAlmModel = new UploadTestResultToAlmModel(
                almServerName,
                credentials.getUsername(),
                credentials.getPassword().getPlainText(),
                almDomain,
                clientType,
                almProject,
                testingFramework,
                testingTool,
                almTestFolder,
                almTestSetFolder,
                almTimeout,
                testingResultFile,
                jenkinsServerUrl);

        VariableResolver<String> varResolver = new VariableResolver.ByMap<String>(build.getEnvironment(listener));

        String serverUrl = getAlmServerUrl(uploadTestResultToAlmModel.getAlmServerName());
        String runUrl = "";
        String tempUrl = Util.replaceMacro(uploadTestResultToAlmModel.getJenkinsServerUrl(), varResolver);
        if(tempUrl != null && tempUrl.length() >0 ) {
        	if(tempUrl.charAt(tempUrl.length() -1) != '/') {
        		runUrl= tempUrl + "/" + build.getUrl();
        	} else  {
        		runUrl = tempUrl + build.getUrl();
        	}
        }

        File root = build.getRootDir();
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(root);
        ds.setIncludes( new String[] {uploadTestResultToAlmModel.getTestingResultFile()});
        ds.scan();
        if (ds.getIncludedFilesCount() == 0) {
        	logger.log("INFO: No Test Report found.");
            build.setResult(Result.UNSTABLE);
        } else {
        	logger.log("INFO: "+ ds.getIncludedFilesCount() +" test result file found.");
        	String[] files = ds.getIncludedFiles();
        	for(String fileName : files) {
        		String fullpath = root.getAbsolutePath() + File.separator + fileName;
    			AlmRestInfo loginInfo = new AlmRestInfo(
    					serverUrl,
    					Util.replaceMacro(uploadTestResultToAlmModel.getAlmDomain(), varResolver),
                        clientType,
    					Util.replaceMacro(uploadTestResultToAlmModel.getAlmProject(), varResolver),
    					uploadTestResultToAlmModel.getAlmUserName(),
    					uploadTestResultToAlmModel.getAlmPassword(),
                        Util.replaceMacro(uploadTestResultToAlmModel.getAlmTestSetFolder(), varResolver)
                );
    			AlmRestTool u = new AlmRestTool(loginInfo, logger);
    			logger.log("INFO: Start to upload "+fullpath);
    			IExternalEntityUploadService service = new DefaultExternalEntityUploadServiceImpl(u, build.getWorkspace(), logger);
    			try {
	    			service.UploadExternalTestSet(loginInfo,
	    					fullpath,
                            Util.replaceMacro(uploadTestResultToAlmModel.getAlmTestSetFolder(), varResolver),
                            Util.replaceMacro(uploadTestResultToAlmModel.getAlmTestFolder(), varResolver),
                            uploadTestResultToAlmModel.getTestingFramework(),
                            uploadTestResultToAlmModel.getTestingTool(),
                            String.valueOf(build.getNumber()),
                            build.getParent().getDisplayName(),
                            runUrl
                    );
	    			logger.log("INFO: Uploaded "+fullpath + ".");
    			} catch (Exception e) {
    				logger.log("WARN: there's exception while uploading "+fullpath + ".");
    				build.setResult(Result.UNSTABLE);
    			}
        	}
        }
        logger.log("INFO: 'Upload test result to ALM' Completed.");
        return true;
    }

    /**
     * Get user name password credentials by id.
     */
    private UsernamePasswordCredentials getCredentialsById(String credentialsId, Run<?, ?> run, ExternalEntityUploadLogger logger) {
        UsernamePasswordCredentials credentials = CredentialsProvider.findCredentialById(credentialsId,
                StandardUsernamePasswordCredentials.class,
                run,
                URIRequirementBuilder.create().build());

        if (credentials == null) {
            logger.log("Can not find credentials with the credentialsId:" + credentialsId);
        }
        return credentials;
    }

    private String getAlmServerUrl(String almServerName) {
        AlmServerSettingsModel[] almServers = Hudson.getInstance().getDescriptorByType(
                AlmServerSettingsBuilder.DescriptorImpl.class).getInstallations();
        if(almServers != null && almServers.length >0) {
            for(AlmServerSettingsModel almServerModel: almServers) {
                if(almServerName.equalsIgnoreCase(almServerModel.getAlmServerName())) {
                    return almServerModel.getAlmServerUrl();
                }
            }
        }
        return "";
    }

    /**
     * This works for jelly
     * <f:option selected="${almServer.almServerName==instance.almServerSettingsModel.almServerName}" value="${almServer.almServerName}">
     * @return
     */
    public AlmServerSettingsModel getAlmServerSettingsModel() {
        for (AlmServerSettingsModel almServer : getDescriptor().getAlmServers()) {
            if (this.almServerName.equals(almServer.getAlmServerName())) {
                return almServer;
            }
        }
        return null;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }
    
    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        
        return new TestResultProjectAction(project);
    }
    
    @Override
    public MatrixAggregator createAggregator(
            MatrixBuild build,
            Launcher launcher,
            BuildListener listener) {
        
        return new TestResultAggregator(build, launcher, listener);
    }
    
    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        
        return BuildStepMonitor.BUILD;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        
        public DescriptorImpl() {
            
            load();
        }
        
        @Override
        public String getDisplayName() {
            
            return "Upload test result to ALM";
        }
        
        @Override
        public boolean isApplicable(
                @SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {
            
            return true;
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
        
        public FormValidation doCheckAlmTestFolder(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("TestFolder are missing");
            }
            
            return FormValidation.ok();
        }  
        
        public FormValidation doCheckAlmTestSetFolder(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("TestSetFolder are missing");
            }
            
            return FormValidation.ok();
        }        
        
        public FormValidation doCheckTestingResultFile(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Testing result file must be set");
            }
            
            return FormValidation.ok();
        }  
        
        public List<EnumDescription> getTestingFrameworks() {
            return UploadTestResultToAlmModel.testingFrameworks;
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

        public FormValidation doCheckCredentialsId(@AncestorInPath Item project, @QueryParameter String value) {
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
