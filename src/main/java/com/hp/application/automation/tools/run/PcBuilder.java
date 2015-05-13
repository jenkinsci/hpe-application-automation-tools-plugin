package com.hp.application.automation.tools.run;

import static com.hp.application.automation.tools.pc.RunState.FINISHED;
import static com.hp.application.automation.tools.pc.RunState.RUN_FAILURE;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.hp.application.automation.tools.common.PcException;
import com.hp.application.automation.tools.model.PcModel;
import com.hp.application.automation.tools.model.PostRunAction;
import com.hp.application.automation.tools.model.TimeslotDuration;
import com.hp.application.automation.tools.pc.PcClient;
import com.hp.application.automation.tools.pc.PcRunEventLog;
import com.hp.application.automation.tools.pc.PcRunEventLogRecord;
import com.hp.application.automation.tools.pc.PcRunResponse;
import com.hp.application.automation.tools.pc.RunState;
import com.hp.application.automation.tools.sse.result.model.junit.Error;
import com.hp.application.automation.tools.sse.result.model.junit.Failure;
import com.hp.application.automation.tools.sse.result.model.junit.JUnitTestCaseStatus;
import com.hp.application.automation.tools.sse.result.model.junit.Testcase;
import com.hp.application.automation.tools.sse.result.model.junit.Testsuite;
import com.hp.application.automation.tools.sse.result.model.junit.Testsuites;

public class PcBuilder extends Builder {
    
    private static final String artifactsDirectoryName = "archive";
    public static final String artifactsResourceName = "artifact";
    public static final String runReportStructure = "%s/%s/performanceTestsReports/pcRun%s";
    public static final String pcReportArchiveName = "Reports.zip";
    public static final String pcReportFileName = "Report.html";
    
    private final PcModel pcModel;
    private final boolean statusBySLA;
    private int runId;
    private FilePath pcReportFile;
    private String junitResultsFileName;
    private PrintStream logger;
    private String status = null;
    
    @DataBoundConstructor
    public PcBuilder(
            String pcServerName,
            String almUserName,
            String almPassword,
            String almDomain,
            String almProject,
            String testId,
            String testInstanceId,
            String timeslotDurationHours,
            String timeslotDurationMinutes,
            PostRunAction postRunAction,
            boolean vudsMode,
            boolean statusBySLA,
            String description) {
        
        this.statusBySLA = statusBySLA;
        
        pcModel =
                new PcModel(
                        pcServerName.trim(),
                        almUserName.trim(),
                        almPassword,
                        almDomain.trim(),
                        almProject.trim(),
                        testId.trim(),
                        testInstanceId.trim(),
                        timeslotDurationHours.trim(),
                        timeslotDurationMinutes.trim(),
                        postRunAction,
                        vudsMode,
                        description);
    }
    
    @Override
    public DescriptorImpl getDescriptor() {
        
        return (DescriptorImpl) super.getDescriptor();
    }
    
    @Override
    public boolean perform(final AbstractBuild<?, ?> build, Launcher launcher, final BuildListener listener)
            throws InterruptedException {
        
    	Callable<String, IOException> task = new Callable<String, IOException>() {
			private static final long serialVersionUID = 1L;

			public String call() {
    			Result resultStatus = Result.FAILURE;
    			logger = listener.getLogger();
    			Testsuites testsuites;
				try {
					testsuites = execute(new PcClient(pcModel, logger), build);
				} catch (InterruptedException e) {
					testsuites = null;
					logger.println(e.toString());
				}
        
    			FilePath resultsFilePath = build.getWorkspace().child(getJunitResultsFileName());
    			resultStatus = createRunResults(resultsFilePath, testsuites);
    			provideStepResultStatus(resultStatus, build);
				return "Task complete";
    		}
    	};
        
    	VirtualChannel channel = launcher.getChannel();
		if (channel != null) {
			try {
				channel.call(task);
			} catch (IOException e) {
				logger.println(e.toString());
			}
		} else {
			try {
				status = task.call();
			} catch (IOException e) {
				logger.println(e.toString());
			}
		}
		logger.print("Final status: ");
		logger.println(status); 
    	
        return true;
    }
    
    public PcModel getPcModel() {
        
        return pcModel;
    }
    
    public String getRunResultsFileName() {
        
        return junitResultsFileName;
    }
    
    public static String getArtifactsDirectoryName() {
        
        return artifactsDirectoryName;
    }
    
    public static String getArtifactsResourceName() {
        
        return artifactsResourceName;
    }
    
    public static String getRunReportStructure() {
        
        return runReportStructure;
    }
    
    public static String getPcReportArchiveName() {
        
        return pcReportArchiveName;
    }
    
    public static String getPcreportFileName() {
        
        return pcReportFileName;
    }
    
    public boolean isStatusBySLA() {
        
        return statusBySLA;
    }
    
    private Testsuites execute(PcClient pcClient, AbstractBuild<?, ?> build)
            throws InterruptedException {
        try {
            if (!StringUtils.isBlank(pcModel.getDescription()))
                logger.println("- - -\nTest description: " + pcModel.getDescription());
            if (!beforeRun(pcClient))
                return null;
            
            return run(pcClient, build);
            
        } catch (InterruptedException e) {
            build.setResult(Result.ABORTED);
            pcClient.stopRun(runId);
            throw e;
        } catch (Exception e) {
            logger.println(e);
        } finally {
            pcClient.logout();
        }
        return null;
    }
    
    private Testsuites run(PcClient pcClient, AbstractBuild<?, ?> build)
            throws InterruptedException, NumberFormatException, ClientProtocolException,
            IOException {
        
        PcRunResponse response = null;
        String errorMessage = "";
        String eventLogString = "";
        try {
            runId = pcClient.startRun();
            response = pcClient.waitForRunCompletion(runId);
            if (response != null && RunState.get(response.getRunState()) == FINISHED) {
                pcReportFile = pcClient.publishRunReport(runId, getReportDirectory(build));
            } else  if (RunState.get(response.getRunState()).ordinal() > FINISHED.ordinal()) {
                PcRunEventLog eventLog = pcClient.getRunEventLog(runId);                
                eventLogString = buildEventLogString(eventLog);
            }           
        } catch (PcException e) {
            errorMessage = e.getMessage();
        }
        if (response == null)
            return null;
        return parsePcRunResponse(response, build, errorMessage, eventLogString);
    }
    
    private String buildEventLogString(PcRunEventLog eventLog) {
        
        String logFormat = "%-5s | %-7s | %-19s | %s\n";
        StringBuilder eventLogStr = new StringBuilder("Event Log:\n\n" + String.format(logFormat, "ID", "TYPE", "TIME","DESCRIPTION"));
        for (PcRunEventLogRecord record : eventLog.getRecordsList()) {
            eventLogStr.append(String.format(logFormat, record.getID(), record.getType(), record.getTime(), record.getDescription()));            
        }
        return eventLogStr.toString();
    }

    private boolean beforeRun(PcClient pcClient) {
        return validatePcForm() && pcClient.login();
    }
    
    private String getReportDirectory(AbstractBuild<?, ?> build) {
        return String.format(
                runReportStructure,
                build.getRootDir().getPath(),
                artifactsDirectoryName,
                runId);
    }
    
    private boolean validatePcForm() {
        
        logger.println("Validating parameters before run");
        String prefix = "doCheck";
        boolean ret = true;
        Method[] methods = getDescriptor().getClass().getMethods();
        Method[] modelMethods = pcModel.getClass().getMethods();
        for (Method method : methods) {
            String name = method.getName();
            if (name.startsWith(prefix)) {
                name = name.replace(prefix, "").toLowerCase();
                for (Method modelMethod : modelMethods) {
                    String modelMethodName = modelMethod.getName();
                    if (modelMethodName.toLowerCase().equals("get" + name)) {
                        try {
                            Object obj =
                                    method.invoke(getDescriptor(), modelMethod.invoke(getPcModel()));
                            if (!obj.equals(FormValidation.ok())) {
                                logger.println(obj);
                                ret = false;
                            }
                            break;
                        } catch (Exception e) {
                            logger.println(e);
                        }
                    }
                }
            }
        }
        return ret;
        
    }
    
    private Testsuites parsePcRunResponse(
            PcRunResponse runResponse,
            AbstractBuild<?, ?> build,
            String errorMessage, String eventLogString) throws IOException, InterruptedException {
        
        Testsuites ret = new Testsuites();
        List<Testsuite> testSuites = ret.getTestsuite();
        Testsuite testSuite = new Testsuite();
        Testcase testCase = new Testcase();
        testCase.setClassname("Performance Tests.Test ID: " + runResponse.getTestID());
        testCase.setName("Run ID: " + runResponse.getID());
        testCase.setTime(String.valueOf(runResponse.getDuration() * 60));
        if (pcReportFile != null && pcReportFile.exists())
            testCase.getSystemOut().add(getOutputForReportLinks(build));
        updateTestStatus(testCase, runResponse, errorMessage, eventLogString);
        testSuite.getTestcase().add(testCase);
        testSuites.add(testSuite);
        
        return ret;
    }
    
    private void updateTestStatus(Testcase testCase, PcRunResponse response, String errorMessage, String eventLog) {
        RunState runState = RunState.get(response.getRunState());
        if (runState == RUN_FAILURE) {
            setError(testCase, String.format("%s. %s", runState, errorMessage), eventLog);
        } else if (statusBySLA && runState == FINISHED && !(response.getRunSLAStatus().equalsIgnoreCase("passed"))) {
            setFailure(testCase, "Run measurements did not reach SLA criteria. Run SLA Status: "
                                 + response.getRunSLAStatus(), eventLog);
        } else if (runState.hasFailure()) {          
            setFailure(testCase, String.format("%s. %s", runState, errorMessage), eventLog);
        } else
            testCase.setStatus(JUnitTestCaseStatus.PASS);
    }
    
    private void setError(Testcase testCase, String message, String eventLog) {
        Error error = new Error();
        error.setMessage(message);
        if (!(eventLog == null || eventLog.isEmpty()))
            testCase.getSystemErr().add(eventLog);
        testCase.getError().add(error);
        testCase.setStatus(JUnitTestCaseStatus.ERROR);
        logger.println(String.format("%s %s", message ,eventLog));
    }
    
    private void setFailure(Testcase testCase, String message, String eventLog) {
        Failure failure = new Failure();
        failure.setMessage(message);
        if (!(eventLog == null || eventLog.isEmpty()))
            testCase.getSystemErr().add(eventLog);
        testCase.getFailure().add(failure);
        testCase.setStatus(JUnitTestCaseStatus.FAILURE);
        logger.println(String.format("Failure: %s %s", message ,eventLog));
    }
    
    private String getOutputForReportLinks(AbstractBuild<?, ?> build) {
        String urlPattern = getArtifactsUrlPattern(build);
        String viewUrl = String.format(urlPattern, pcReportFileName);
        String downloadUrl = String.format(urlPattern, "*zip*/pcRun" + runId);
        logger.println(String.format("View report: %s", viewUrl));
        return String.format("View Report:\n%s\n\n\nDownload Report:\n%s", viewUrl, downloadUrl);
    }
    
    private String getArtifactsUrlPattern(AbstractBuild<?, ?> build) {
        return String.format(
                runReportStructure,
                Hudson.getInstance().getRootUrl() + StringUtils.chop(build.getUrl()).replace("%", "%%"),
                artifactsResourceName,
                runId + "/%s");
    }
    
    private void provideStepResultStatus(Result resultStatus, AbstractBuild<?, ?> build) {
        String runIdStr =
                (runId > 0) ? String.format(" (PC RunID: %s)", String.valueOf(runId)) : "";
        logger.println(String.format(
                "Result Status%s: %s\n- - -",
                runIdStr,
                resultStatus.toString()));
        build.setResult(resultStatus);
        
    }
    
    private Result createRunResults(FilePath filePath, Testsuites testsuites) {
        Result ret = Result.SUCCESS;
        try {
            if (testsuites != null) {
                StringWriter writer = new StringWriter();
                JAXBContext context = JAXBContext.newInstance(Testsuites.class);
                Marshaller marshaller = context.createMarshaller();
                marshaller.marshal(testsuites, writer);
                filePath.write(writer.toString(), null);
                if (containsErrorsOrFailures(testsuites.getTestsuite())) {
                    ret = Result.UNSTABLE;
                }
            } else {
                logger.println("Empty Results");
                ret = Result.UNSTABLE;
            }
            
        } catch (Throwable cause) {
            logger.print(String.format(
                    "Failed to create run results, Exception: %s",
                    cause.getMessage()));
            ret = Result.UNSTABLE;
        }
        return ret;
    }
    
    private boolean containsErrorsOrFailures(List<Testsuite> testsuites) {
        boolean ret = false;
        for (Testsuite testsuite : testsuites) {
            for (Testcase testcase : testsuite.getTestcase()) {
                String status = testcase.getStatus();
                if (status.equals(JUnitTestCaseStatus.ERROR)
                    || status.equals(JUnitTestCaseStatus.FAILURE)) {
                    ret = true;
                    break;
                }
            }
        }
        return ret;
    }
    
    private String getJunitResultsFileName() {
        Format formatter = new SimpleDateFormat("ddMMyyyyHHmmssSSS");
        String time = formatter.format(new Date());
        junitResultsFileName = String.format("Results%s.xml", time);
        return junitResultsFileName;
    }
    
    // This indicates to Jenkins that this is an implementation of an extension
    // point
    @Extension
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
            
            return "Execute HP tests using HP Performance Center";
        }
        
        public FormValidation doCheckPcServerName(@QueryParameter String value) {
            
            return validateString(value, "PC Server");
        }
        
        public FormValidation doCheckAlmUserName(@QueryParameter String value) {
            
            return validateString(value, "User name");
        }
        
        public FormValidation doCheckAlmDomain(@QueryParameter String value) {
            
            return validateString(value, "Domain");
        }
        
        public FormValidation doCheckAlmProject(@QueryParameter String value) {
            
            return validateString(value, "Project");
        }
        
        public FormValidation doCheckTestId(@QueryParameter String value) {
            
            return validateHigherThanInt(value, "Test ID", 0, true);
        }
        
        public FormValidation doCheckTestInstanceId(@QueryParameter String value) {
            
            return validateHigherThanInt(value, "Test Instance ID", 0, true);
        }
        
        public FormValidation doCheckTimeslotDuration(@QueryParameter TimeslotDuration value) {
            
            return validateHigherThanInt(
                    String.valueOf(value.toMinutes()),
                    "Timeslot Duration (in minutes)",
                    30,
                    false);
        }
        
        public FormValidation doCheckTimeslotId(@QueryParameter String value) {
            
            return validateHigherThanInt(value, "Timeslot ID", 0, true);
        }
        
        /**
         * @param limitIncluded
         *            if true, value must be higher than limit. if false, value must be equal to or
         *            higher than limit.
         */
        private FormValidation validateHigherThanInt(
                String value,
                String field,
                int limit,
                boolean limitIncluded) {
            FormValidation ret = FormValidation.ok();
            value = value.trim();
            String messagePrefix = field + " must be ";
            if (StringUtils.isBlank(value)) {
                ret = FormValidation.error(messagePrefix + "set");
            } else {
                try {
                    if (limitIncluded && Integer.parseInt(value) <= limit)
                        ret = FormValidation.error(messagePrefix + "higher than " + limit);
                    else if (Integer.parseInt(value) < limit)
                        ret = FormValidation.error(messagePrefix + "at least " + limit);
                } catch (Exception e) {
                    ret = FormValidation.error(messagePrefix + "a whole number");
                }
            }
            
            return ret;
            
        }
        
        private FormValidation validateString(String value, String field) {
            FormValidation ret = FormValidation.ok();
            if (StringUtils.isBlank(value.trim())) {
                ret = FormValidation.error(field + " must be set");
            }
            
            return ret;
        }
        
        public List<PostRunAction> getPostRunActions() {
            
            return PcModel.getPostRunActions();
        }
        
    }
    
}
