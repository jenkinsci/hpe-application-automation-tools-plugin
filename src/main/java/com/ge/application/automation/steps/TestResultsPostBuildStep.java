package com.ge.application.automation.steps;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Project;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import qc.rest.examples.infrastructure.EntityMarshallingUtils;
import qc.rest.examples.infrastructure.RestConnector;

import com.hp.application.automation.tools.common.RuntimeUtils;
import com.hp.application.automation.tools.model.RunFromAlmModel;
import com.hp.application.automation.tools.run.RunFromAlmBuilder;
import com.hp.application.automation.tools.sse.result.model.junit.Testsuites;

/**
 * Post-build step for Jenkins ALM plugin
 * 
 * @author Tyler Hoffman
 */
public class TestResultsPostBuildStep extends Notifier {
	
    @DataBoundConstructor
    public TestResultsPostBuildStep() {
    	
    }
    
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        
    	Project<?, ?> project = RuntimeUtils.cast(build.getProject());
        List<Builder> builders = project.getBuilders();
    	String serverAddress = null;
    	String almUsername = null;
    	String almPassword = null;
    	String almDomain = null;
    	String almProject = null;
    	String almTestSetPathsString = null;
    	boolean foundBuilder = false;
        for (Builder builder : builders) {
            if (builder instanceof RunFromAlmBuilder) {
                foundBuilder = true;
            	serverAddress = ((RunFromAlmBuilder) builder).getAlmServerSettingsModel().getAlmServerUrl();
                
                RunFromAlmModel runFromAlmModel = ((RunFromAlmBuilder) builder).getRunFromAlmModel();
                almUsername = runFromAlmModel.getAlmUserName();
                almPassword = runFromAlmModel.getAlmPassword();
                almDomain = runFromAlmModel.getAlmDomain();
                almProject = runFromAlmModel.getAlmProject();
                almTestSetPathsString = runFromAlmModel.getAlmTestSets();
                
                break;
            }
        }
        
        // check that ALM build step was actually used
        if (!foundBuilder) {
        	listener.getLogger().println("No ALM build step found; "
        			+ getDescriptor().getDisplayName() + " may only be after "
        			+ "\"Execute HP functional tests from HP ALM\" "
        			+ "build step");
        } else {
	        
	        String[] testSetPaths = almTestSetPathsString.replaceAll("\r", "").split("\\n");
	        for (int i = 0; i < testSetPaths.length; i++) {
	        	testSetPaths[i] = testSetPaths[i].trim();
	        }
	        
	        listener.getLogger().println("Retrieving test set data...");
	        
	        // connect
	        RestAuthenticationHandler handler = new RestAuthenticationHandler(listener.getLogger());
	        RestConnector connector = handler.connect(almUsername, almPassword, 
	        		serverAddress, almDomain, almProject);
	        
	        // get test results
	        Testsuites results = null;
	        try {
	        	Date startTime = new Date(build.getStartTimeInMillis());
	        	results = new AlmTestStepRetriever(connector, listener.getLogger(), startTime)
	        			.getTestSetResults(testSetPaths);
	        } catch (Exception e) {
	        	e.printStackTrace(listener.getLogger());
	        }
	        
	        // logout
	        try {
				handler.logout();
			} catch (Exception e) {
				e.printStackTrace(listener.getLogger());
			}
	        
	        // log results
	        if (results != null) {
	        	listener.getLogger().println("Writing test results to file");
	        	try {
	        		String testResults = EntityMarshallingUtils.unmarshal(
							Testsuites.class, 
							results);
	        		FilePath resultsPath = new FilePath(build.getWorkspace(), "test_results.xml");
	        		
	        		PrintWriter writer = new PrintWriter(resultsPath.write());
	        		writer.println(testResults);
	        		writer.close();
	        		
				} catch (Exception e) {
					e.printStackTrace(listener.getLogger());
				}
	        } else {
	        	listener.getLogger().println("Error: no test results found");
	        }
        }
        
        return true;
    }
    
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link TestResultsPostBuildStep}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/ALMPlugin/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // This builder can be used with all kinds of project types 
            return true;
        }

		/**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Retrieve ALM Run Results";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
        	save();
            return super.configure(req,formData);
        }
    }

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}
	
}

