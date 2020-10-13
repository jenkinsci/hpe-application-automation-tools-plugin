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

package com.microfocus.application.automation.tools.octane.executor;

import antlr.ANTLRException;
import com.cloudbees.hudson.plugins.folder.Folder;
import com.hp.octane.integrations.OctaneClient;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.executor.DiscoveryInfo;
import com.hp.octane.integrations.dto.scm.SCMRepository;
import com.hp.octane.integrations.executor.TestsToRunFramework;
import com.hp.octane.integrations.services.configurationparameters.UftTestRunnerFolderParameter;
import com.microfocus.application.automation.tools.model.ResultsPublisherModel;
import com.microfocus.application.automation.tools.octane.actions.UFTTestDetectionPublisher;
import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import com.microfocus.application.automation.tools.octane.executor.scmmanager.ScmPluginFactory;
import com.microfocus.application.automation.tools.octane.executor.scmmanager.ScmPluginHandler;
import com.microfocus.application.automation.tools.octane.model.processors.projects.JobProcessorFactory;
import com.microfocus.application.automation.tools.octane.testrunner.TestsToRunConverterBuilder;
import com.microfocus.application.automation.tools.results.RunResultRecorder;
import com.microfocus.application.automation.tools.run.RunFromFileBuilder;
import hudson.model.*;
import hudson.tasks.BuildWrapper;
import hudson.tasks.Builder;
import hudson.triggers.SCMTrigger;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This service is responsible to create jobs (discovery and execution) for execution process.
 */
public class TestExecutionJobCreatorService {
	private static final Logger logger = SDKBasedLoggerProvider.getLogger(TestExecutionJobCreatorService.class);

	private static void setScmRepository(SCMRepository scmRepository, String scmRepositoryCredentialsId, FreeStyleProject proj, boolean executorJob) {

		ScmPluginHandler scmPluginHandler = ScmPluginFactory.getScmHandler(scmRepository.getType());
		try {
			scmPluginHandler.setScmRepositoryInJob(scmRepository, scmRepositoryCredentialsId, proj, executorJob);
		} catch (IOException e) {
			throw new IllegalArgumentException("Failed to set SCM repository : " + e.getMessage());
		}
	}

	/**
	 * Create (if needed) and run test discovery
	 *
	 * @param discoveryInfo discovery info
	 */
	public static void runTestDiscovery(DiscoveryInfo discoveryInfo) {

        /*
        {
          "scmRepository": {
            "type": "git",
            "url": "git@github.com:radislavB/UftTests.git"
          },
          "executorId": "1",
          "executorLogicalName": "ABC",
          "workspaceId": "1002",
          "testingToolType": "uft",
          "forceFullDiscovery": true
        }
         */
		FreeStyleProject proj = createDiscoveryJob(discoveryInfo);

		//start job
		if (proj != null) {
			List<ParameterValue> paramList = new ArrayList<>();
			ParameterValue fullScanParam = new BooleanParameterValue(UftConstants.FULL_SCAN_PARAMETER_NAME, discoveryInfo.isForceFullDiscovery());
			paramList.add(fullScanParam);

			ParametersDefinitionProperty parameters = proj.getProperty(ParametersDefinitionProperty.class);
			if (parameters.getParameterDefinitionNames().contains(UftConstants.TEST_RUNNER_ID_PARAMETER_NAME)) {
				ParameterValue testRunnerIdParam = new StringParameterValue(UftConstants.TEST_RUNNER_ID_PARAMETER_NAME, discoveryInfo.getExecutorId());
				paramList.add(testRunnerIdParam);
			}

			ParametersAction paramAction = new ParametersAction(paramList);
			Cause cause = new Cause.UserIdCause();
			CauseAction causeAction = new CauseAction(cause);
			proj.scheduleBuild2(0, paramAction, causeAction);
		}
	}

	private static Folder getParentFolder(String configurationId) {
		OctaneClient octaneClient = OctaneSDK.getClientByInstanceId(configurationId);
		UftTestRunnerFolderParameter uftFolderParameter = (UftTestRunnerFolderParameter) octaneClient.getConfigurationService()
				.getConfiguration().getParameter(UftTestRunnerFolderParameter.KEY);

		if (uftFolderParameter != null) {
			Item item = Jenkins.getInstanceOrNull().getItem(uftFolderParameter.getFolder());
			String errorMsg = null;
			if (item == null) {
				errorMsg = UftTestRunnerFolderParameter.KEY + " parameter is defined with '" + uftFolderParameter.getFolder() + "', the folder is not found. Validate that folder exist and jenkins user has READ permission on the folder.";
			}
			if (item != null && !JobProcessorFactory.isFolder(item)) {
				errorMsg = UftTestRunnerFolderParameter.KEY + " parameter is defined with '" + uftFolderParameter.getFolder() + "', the item is " + item.getClass().getName() + " , but expected to be a folder.";
			}
			if (errorMsg != null) {
				logger.error(errorMsg);
				throw new IllegalArgumentException(errorMsg);
			} else {
				return (Folder) item;
			}
		} else {
			return null;
		}
	}

	private static FreeStyleProject createProject(String configurationId, String name) throws IOException {
		Folder folder = getParentFolder(configurationId);
		FreeStyleProject proj;
		if (folder == null) {
			proj = Jenkins.getInstanceOrNull().createProject(FreeStyleProject.class, name);
		} else {
			proj = folder.createProject(FreeStyleProject.class, name);
		}
		return proj;
	}

	private static FreeStyleProject createDiscoveryJob(DiscoveryInfo discoveryInfo) {
		try {
			String discoveryJobName = String.format("%s-%s-%s", UftConstants.DISCOVERY_JOB_MIDDLE_NAME_WITH_TEST_RUNNERS_NEW, discoveryInfo.getExecutorId(), discoveryInfo.getExecutorLogicalName().substring(0,5));
			FreeStyleProject proj = createProject(discoveryInfo.getConfigurationId(), discoveryJobName);

			proj.setDescription(String.format("This job was created by the Micro Focus Application Automation Tools plugin for discovery of %s tests. It is associated with ALM Octane test runner #%s.",
					discoveryInfo.getTestingToolType().toString(), discoveryInfo.getExecutorId()));

			setScmRepository(discoveryInfo.getScmRepository(), discoveryInfo.getScmRepositoryCredentialsId(), proj, false);
			addConstantParameter(proj, UftConstants.TEST_RUNNER_ID_PARAMETER_NAME, discoveryInfo.getExecutorId(), "ALM Octane test runner ID");
			addConstantParameter(proj, UftConstants.TEST_RUNNER_LOGICAL_NAME_PARAMETER_NAME, discoveryInfo.getExecutorLogicalName(), "ALM Octane test runner logical name");
			addBooleanParameter(proj, UftConstants.FULL_SCAN_PARAMETER_NAME, false, "Specify whether to synchronize the set of tests on ALM Octane with the whole SCM repository or to update the set of tests on ALM Octane based on the latest commits.");

			//set polling once in two minutes
			SCMTrigger scmTrigger = new SCMTrigger("H/2 * * * *");//H/2 * * * * : once in two minutes
			proj.addTrigger(scmTrigger);
			delayPollingStart(proj, scmTrigger);
			addDiscoveryAssignedNode(proj);
			addTimestamper(proj);

			//add post-build action - publisher
			UFTTestDetectionPublisher uftTestDetectionPublisher = null;
			List publishers = proj.getPublishersList();
			for (Object publisher : publishers) {
				if (publisher instanceof UFTTestDetectionPublisher) {
					uftTestDetectionPublisher = (UFTTestDetectionPublisher) publisher;
				}
			}

			if (uftTestDetectionPublisher == null) {
				uftTestDetectionPublisher = new UFTTestDetectionPublisher(discoveryInfo.getConfigurationId(), discoveryInfo.getWorkspaceId(), discoveryInfo.getScmRepositoryId());
				publishers.add(uftTestDetectionPublisher);
			}

			return proj;
		} catch (IOException | ANTLRException e) {
			logger.error("Failed to  create DiscoveryJob for test runner: " + e.getMessage());
			return null;
		}
	}

	private static void addTimestamper(FreeStyleProject proj) {
		try {
			Descriptor<BuildWrapper> wrapperDescriptor = Jenkins.getInstanceOrNull().getBuildWrapper("TimestamperBuildWrapper");
			if (wrapperDescriptor != null) {
				BuildWrapper wrapper = proj.getBuildWrappersList().get(wrapperDescriptor);
				if (wrapper == null) {
					wrapper = wrapperDescriptor.newInstance(null, null);
					proj.getBuildWrappersList().add(wrapper);
				}

			}
		} catch (Descriptor.FormException e) {
			logger.error("Failed to  addTimestamper : " + e.getMessage());
		}
	}

	/**
	 * Delay starting of polling by 5 minutes to allow original clone
	 *
	 * @param proj
	 * @param scmTrigger
	 */
	private static void delayPollingStart(final FreeStyleProject proj, final SCMTrigger scmTrigger) {
		long delayStartPolling = 1000L * 60 * 5;//5 minute
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				scmTrigger.start(proj, false);
			}
		}, delayStartPolling);
	}

	private static ParametersDefinitionProperty getParametersDefinitions(FreeStyleProject proj) throws IOException {
		ParametersDefinitionProperty parameters = proj.getProperty(ParametersDefinitionProperty.class);
		if (parameters == null) {
			parameters = new ParametersDefinitionProperty(new ArrayList<>());
			proj.addProperty(parameters);
		}
		return parameters;
	}

	private static void addConstantParameter(FreeStyleProject proj, String parameterName, String parameterValue, String desc) throws IOException {
		ParametersDefinitionProperty parameters = getParametersDefinitions(proj);
		if (parameters.getParameterDefinition(parameterName) == null) {
			ParameterDefinition param = new ChoiceParameterDefinition(parameterName, new String[]{parameterValue}, desc);
			parameters.getParameterDefinitions().add(param);
		}
	}

	private static void addStringParameter(FreeStyleProject proj, String parameterName, String defaultValue, String desc) throws IOException {
		ParametersDefinitionProperty parameters = getParametersDefinitions(proj);
		if (parameters.getParameterDefinition(parameterName) == null) {
			ParameterDefinition param = new StringParameterDefinition(parameterName, defaultValue, desc);
			parameters.getParameterDefinitions().add(param);
		}
	}

	private static void addBooleanParameter(FreeStyleProject proj, String parameterName, Boolean defaultValue, String desc) throws IOException {
		ParametersDefinitionProperty parameters = getParametersDefinitions(proj);
		if (parameters.getParameterDefinition(parameterName) == null) {
			ParameterDefinition param = new BooleanParameterDefinition(parameterName, defaultValue, desc);
			parameters.getParameterDefinitions().add(param);
		}
	}

	private static void addDiscoveryAssignedNode(FreeStyleProject proj) {
		try {
			Label joinedLabel = Label.parseExpression(Jenkins.getInstanceOrNull().getSelfLabel() + "||" + Jenkins.getInstanceOrNull().getSelfLabel());
			//why twice Jenkins.getInstance().getSelfLabel()==master? because only one master is not saved in method proj.setAssignedLabel as it is label of Jenkins.getInstance().getSelfLabel()
			proj.setAssignedLabel(joinedLabel);
		} catch (ANTLRException | IOException e) {
			logger.error("Failed to  set add DiscoveryAssignedNode : " + e.getMessage());
		}
	}

	private static void addExecutionAssignedNode(FreeStyleProject proj) {
		Computer[] computers = Jenkins.getInstanceOrNull().getComputers();
		Set<String> labels = new HashSet();

		//add existing
		String assigned = proj.getAssignedLabelString();
		if (assigned != null) {
			String[] assignedArr = StringUtils.split(assigned, "||");
			for (String item : assignedArr) {
				labels.add(item.trim());
			}
		}

		//try to add new
		try {
			for (Computer computer : computers) {
				if (computer instanceof Jenkins.MasterComputer) {
					continue;
				}

				String label = "" + computer.getNode().getSelfLabel();
				if (label.toLowerCase().contains("uft")) {
					label = label.trim();
					Pattern p = Pattern.compile("[^\\w]");
					Matcher m = p.matcher(label);
					if (m.find()) {
						//if contain non-letter/digit character, wrap with "
						label = "\"" + label + "\"";
					}
					labels.add(label);
				}
			}

			if (!labels.isEmpty()) {
				String joined = StringUtils.join(labels, "||");
				//if there are more than 1 wrapped label (for example : "label 1"), need to wrap it with parentheses
				boolean parenthesesRequired = labels.stream().filter(l -> l.startsWith("\"")).count() > 1;
				if (parenthesesRequired) {
					joined = "(" + joined + ")";
				}

				proj.setAssignedLabel(Label.parseExpression(joined));
			}

		} catch (IOException | ANTLRException e) {
			logger.error("Failed to  set addExecutionAssignedNode : " + e.getMessage());
		}
	}

	public static FreeStyleProject createExecutor(DiscoveryInfo discoveryInfo) {
		try {
			String projectName = String.format("%s-%s-%s", UftConstants.EXECUTION_JOB_MIDDLE_NAME_WITH_TEST_RUNNERS_NEW, discoveryInfo.getExecutorId(), discoveryInfo.getExecutorLogicalName().substring(0,5));
			FreeStyleProject proj = createProject(discoveryInfo.getConfigurationId(), projectName);

			proj.setDescription(String.format("This job was created by the Micro Focus Application Automation Tools plugin for running UFT tests. It is associated with ALM Octane test runner #%s.",
					discoveryInfo.getExecutorId()));

			setScmRepository(discoveryInfo.getScmRepository(), discoveryInfo.getScmRepositoryCredentialsId(), proj, true);
			addStringParameter(proj, UftConstants.TESTS_TO_RUN_PARAMETER_NAME, "", "Tests to run");
			addStringParameter(proj, UftConstants.CHECKOUT_DIR_PARAMETER_NAME, "${WORKSPACE}\\${CHECKOUT_SUBDIR}", "Shared UFT directory");
			addConstantParameter(proj, UftConstants.TEST_RUNNER_ID_PARAMETER_NAME, discoveryInfo.getExecutorId(), "ALM Octane test runner ID");
			addConstantParameter(proj, UftConstants.TEST_RUNNER_LOGICAL_NAME_PARAMETER_NAME, discoveryInfo.getExecutorLogicalName(), "ALM Octane test runner logical name");
			addStringParameter(proj, UftConstants.SUITE_ID_PARAMETER_NAME, "", "ALM Octane test suite ID");
			addStringParameter(proj, UftConstants.SUITE_RUN_ID_PARAMETER_NAME, "", "The ID of the ALM Octane test suite run to associate with the test run results.");

			addExecutionAssignedNode(proj);
			addTimestamper(proj);

			//add build action
			Builder convertedBuilder = new TestsToRunConverterBuilder(TestsToRunFramework.MF_UFT.value());
			Builder uftRunner = new RunFromFileBuilder("${testsToRunConverted}");
			proj.getBuildersList().add(convertedBuilder);
			proj.getBuildersList().add(uftRunner);


			//add post-build action - publisher
			RunResultRecorder runResultRecorder = null;
			List publishers = proj.getPublishersList();
			for (Object publisher : publishers) {
				if (publisher instanceof RunResultRecorder) {
					runResultRecorder = (RunResultRecorder) publisher;
				}
			}
			if (runResultRecorder == null) {
				runResultRecorder = new RunResultRecorder(ResultsPublisherModel.alwaysArchiveResults.getValue());
				publishers.add(runResultRecorder);
			}
			return proj;
		} catch (IOException e) {
			logger.error("Failed to create executor job : " + e.getMessage());
			return null;
		}
	}
}
