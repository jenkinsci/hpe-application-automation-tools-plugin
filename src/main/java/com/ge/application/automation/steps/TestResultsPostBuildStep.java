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

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.hp.application.automation.tools.common.RuntimeUtils;
import com.hp.application.automation.tools.model.RunFromAlmModel;
import com.hp.application.automation.tools.rest.RestClient;
import com.hp.application.automation.tools.run.RunFromAlmBuilder;
import com.hp.application.automation.tools.sse.result.model.junit.Testsuites;
import com.hp.application.automation.tools.sse.sdk.Logger;
import com.hp.application.automation.tools.sse.sdk.RestAuthenticator;

/**
 * Post-build step for Jenkins ALM plugin.
 * This uses information from "Execute HP functional tests from HP ALM" build step
 * and makes REST calls to ALM to get detailed test results.
 */
public class TestResultsPostBuildStep extends Notifier {
	
	private static String TEST_RESULTS_FILE = "test_results.xml";
	
	private RunFromAlmBuilder runFromAlmBuilder;
	
    @DataBoundConstructor
    public TestResultsPostBuildStep() {
    	
    }
    
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        
    	PrintStreamLogger logger = new PrintStreamLogger(listener.getLogger());
    	
    	// get data from the previous build step (Execute HP functional tests from HP ALM)
    	loadDataFromAlmBuildStep(build);
    	
    	if (runFromAlmBuilder == null) {
    		listener.getLogger().println("No ALM build step found; "
        			+ getDescriptor().getDisplayName() + " may only be after "
        			+ "\"Execute HP functional tests from HP ALM\" "
        			+ "build step");
    	} else {
    		
    		listener.getLogger().println("Retrieving test set data...");
    		
    		// connect
    		RestAuthenticator authenticator = new RestAuthenticator();
    		RestClient restClient = makeRestClientAndLogin(authenticator, logger);
    		
    		// get test results
	        Testsuites results = null;
	        try {
	        	Date startTime = new Date(build.getStartTimeInMillis());
	        	results = new AlmTestStepRetriever(restClient, listener.getLogger(), startTime)
	        			.getTestSetResults(getTestSetPaths());
	        } catch (Exception e) {
	        	e.printStackTrace(listener.getLogger());
	        }
	        
	        // logout
	        logout(restClient, authenticator);
	        
	        // log results
	        if (results != null) {
	        	logResults(build, listener.getLogger(), results);
	        } else {
	        	listener.getLogger().println("Error: no test results found");
	        }
    	}
        
        return true;
    }
    
    private void loadDataFromAlmBuildStep(AbstractBuild<?, ?> build) {
    	Project<?, ?> project = RuntimeUtils.cast(build.getProject());
        List<Builder> builders = project.getBuilders();
    	for (Builder builder : builders) {
            if (builder instanceof RunFromAlmBuilder) {
            	runFromAlmBuilder = (RunFromAlmBuilder) builder;
            	break;
            }
    	}
    }
    
    /**
     * @return Returns array of test set paths used in previous build step
     */
    private String[] getTestSetPaths() {
		String almTestSetPathsString = runFromAlmBuilder.getRunFromAlmModel().getAlmTestSets();
        String[] testSetPaths = almTestSetPathsString.replaceAll("\r", "").split("\\n");
        for (int i = 0; i < testSetPaths.length; i++) {
        	testSetPaths[i] = testSetPaths[i].trim();
        }
        return testSetPaths;
    }
    
    /**
     * Generate a rest client for alm and login.
     * @param authenticator
     * @param logger
     * @return Returns the RestClient used to make requests
     */
    private RestClient makeRestClientAndLogin(RestAuthenticator authenticator, Logger logger) {
    	RunFromAlmModel runFromAlmModel = runFromAlmBuilder.getRunFromAlmModel();
    	
    	RestClient restClient = new RestClient(
    			runFromAlmBuilder.getAlmServerSettingsModel().getAlmServerUrl(), 
    			runFromAlmModel.getAlmDomain(), 
    			runFromAlmModel.getAlmProject(), 
    			runFromAlmModel.getAlmUserName());
    	
    	authenticator.login(restClient,
    			runFromAlmModel.getAlmUserName(), 
    			runFromAlmModel.getAlmPassword(),
    			logger);
    	
    	return restClient;
    }
    
    /**
     * Log out of the rest client
     * @param restClient
     * @param authenticator
     */
    private void logout(RestClient restClient, RestAuthenticator authenticator) {
    	RunFromAlmModel runFromAlmModel = runFromAlmBuilder.getRunFromAlmModel();
    	authenticator.logout(
    			restClient, 
    			runFromAlmModel.getAlmUserName());
    }
    
    /**
     * Write test results to xml file
     * @param build
     * @param logger
     * @param results
     */
    private void logResults(AbstractBuild<?, ?> build, PrintStream logger, Testsuites results) {
    	logger.println("Writing test results to file: " + TEST_RESULTS_FILE);
    	try {
    		String testResults = MarshallingUtility.marshal(/*Testsuites.class, */results);
    		FilePath resultsPath = new FilePath(build.getWorkspace(), TEST_RESULTS_FILE);
    		
    		PrintWriter writer = new PrintWriter(resultsPath.write());
    		writer.println(testResults);
    		writer.close();
		} catch (Exception e) {
			e.printStackTrace(logger);
		}
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

