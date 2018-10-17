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

package com.microfocus.application.automation.tools.octane.executor;

import antlr.ANTLRException;
import com.hp.octane.integrations.dto.executor.DiscoveryInfo;
import com.hp.octane.integrations.dto.executor.TestExecutionInfo;
import com.hp.octane.integrations.dto.executor.TestSuiteExecutionInfo;
import com.hp.octane.integrations.dto.executor.impl.TestingToolType;
import com.hp.octane.integrations.dto.scm.SCMRepository;
import com.hp.octane.integrations.util.SdkConstants;
import com.microfocus.application.automation.tools.model.ResultsPublisherModel;
import com.microfocus.application.automation.tools.octane.actions.UFTTestDetectionPublisher;
import com.microfocus.application.automation.tools.octane.executor.scmmanager.ScmPluginFactory;
import com.microfocus.application.automation.tools.octane.executor.scmmanager.ScmPluginHandler;
import com.microfocus.application.automation.tools.results.RunResultRecorder;
import com.microfocus.application.automation.tools.run.RunFromFileBuilder;
import hudson.model.*;
import hudson.tasks.BuildWrapper;
import hudson.tasks.LogRotator;
import hudson.triggers.SCMTrigger;
import jenkins.model.BuildDiscarder;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/**
 * This service is responsible to create jobs (discovery and execution) for execution process.
 */
public class TestExecutionJobCreatorService {

    private static final Logger logger = LogManager.getLogger(TestExecutionJobCreatorService.class);


    /**
     * Create (if needed) and run test execution
     *
     * @param suiteExecutionInfo
     */
    public static void runTestSuiteExecution(TestSuiteExecutionInfo suiteExecutionInfo) {

        /*
        {
                "tests": [{
                        "testName": "GUITest2",
                        "packageName": "GUITests"
                    }, {
                        "testName": "GUITest3",
                        "packageName": "GUITests"
                    }
                ],
                "scmRepository": {
                    "type": "git",
                                            "url": "git@github.com:radislavB/UftTests.git"
                },
                "executorId": "1",
                "workspaceId": "1002",
                "suiteId": "6",
                "testingToolType": "uft"
            }
         */
        FreeStyleProject proj = getExecutionJob(suiteExecutionInfo);

        //start job
        if (proj != null) {
            ParameterValue suiteRunIdParam = new StringParameterValue(UftConstants.SUITE_RUN_ID_PARAMETER_NAME, suiteExecutionInfo.getSuiteRunId());
            ParameterValue suiteIdParam = new StringParameterValue(UftConstants.SUITE_ID_PARAMETER_NAME, suiteExecutionInfo.getSuiteId());
            ParametersAction parameters = new ParametersAction(suiteRunIdParam, suiteIdParam);

            Cause cause = StringUtils.isNotEmpty(suiteExecutionInfo.getSuiteRunId()) ? TriggeredBySuiteRunCause.create(suiteExecutionInfo.getSuiteRunId()) : new Cause.UserIdCause();
            CauseAction causeAction = new CauseAction(cause);
            proj.scheduleBuild2(0, parameters, causeAction);
        }
    }

    private static FreeStyleProject getExecutionJob(TestSuiteExecutionInfo suiteExecutionInfo) {

        try {
            String projectName = String.format("%s %s %s",
                    suiteExecutionInfo.getTestingToolType().toString(),
                    UftConstants.EXECUTION_JOB_MIDDLE_NAME,
                    suiteExecutionInfo.getSuiteId());

            //validate creation of job
            FreeStyleProject proj = (FreeStyleProject) Jenkins.getInstance().getItem(projectName);
            if (proj == null) {
                proj = Jenkins.getInstance().createProject(FreeStyleProject.class, projectName);
                proj.setDescription(String.format("This job was created by the Micro Focus Application Automation Tools plugin for running %s tests. It is associated with ALM Octane test suite #%s.",
                        suiteExecutionInfo.getTestingToolType().toString(), suiteExecutionInfo.getSuiteId()));
            }

            setScmRepository(suiteExecutionInfo.getScmRepository(), suiteExecutionInfo.getScmRepositoryCredentialsId(), proj, true);
            setBuildDiscarder(proj, 40);
            addConstantParameter(proj, UftConstants.SUITE_ID_PARAMETER_NAME, suiteExecutionInfo.getSuiteId(), "ALM Octane test suite ID");
            addStringParameter(proj, UftConstants.SUITE_RUN_ID_PARAMETER_NAME, "", "The ID of the ALM Octane test suite run to associate with the test run results. Provided by ALM Octane when running a planned suite run.\nOtherwise, leave this parameter empty. ALM Octane creates a new  test suite run for the new results.");
            addExecutionAssignedNode(proj);
            addTimestamper(proj);

            //add build action
            String fsTestsData = prepareMtbxData(suiteExecutionInfo.getTests());
            List<RunFromFileBuilder> builders = proj.getBuildersList().getAll(RunFromFileBuilder.class);
            if (builders != null && !builders.isEmpty()) {
                builders.get(0).setFsTests(fsTestsData);
            } else {
                proj.getBuildersList().add(new RunFromFileBuilder(fsTestsData));
            }

            //add post-build action - publisher
            RunResultRecorder runResultRecorder = null;
            List publishers = proj.getPublishersList();//.add(new RunResultRecorder(ResultsPublisherModel.alwaysArchiveResults.getValue()));
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
            logger.error("Failed to create ExecutionJob : " + e.getMessage());
            return null;
        }
    }

    private static void setScmRepository(SCMRepository scmRepository, String scmRepositoryCredentialsId, FreeStyleProject proj, boolean executorJob) {

        ScmPluginHandler scmPluginHandler = ScmPluginFactory.getScmHandler(scmRepository.getType());
        try {
            scmPluginHandler.setScmRepositoryInJob(scmRepository, scmRepositoryCredentialsId, proj, executorJob);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to set SCM repository : " + e.getMessage());
        }
    }

    private static String prepareMtbxData(List<TestExecutionInfo> tests) throws IOException {
        /*<Mtbx>
            <Test name="test1" path="${WORKSPACE}\${CHECKOUT_SUBDIR}\APITest1">
			<Parameter name="A" value="abc" type="string"/>
			<DataTable path="${WORKSPACE}\aa\bbb.xslx"/>
			 ….
			</Test>
			<Test name="test2" path="${WORKSPACE}\${CHECKOUT_SUBDIR}\test2">
				<Parameter name="p1" value="123" type="int"/>
				<Parameter name="p4" value="123.4" type="float"/>
			….
			</Test>
		</Mtbx>*/

        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("Mtbx");
            doc.appendChild(rootElement);

            for (TestExecutionInfo test : tests) {
                Element testElement = doc.createElement("Test");
                String packageAndTestName = (StringUtils.isNotEmpty(test.getPackageName()) ? test.getPackageName() + "\\" : "") + test.getTestName();
                testElement.setAttribute("name", packageAndTestName);
                String path = "${WORKSPACE}\\${CHECKOUT_SUBDIR}" + (StringUtils.isEmpty(test.getPackageName()) ? "" : SdkConstants.FileSystem.WINDOWS_PATH_SPLITTER + test.getPackageName()) +
                        SdkConstants.FileSystem.WINDOWS_PATH_SPLITTER + test.getTestName();
                testElement.setAttribute("path", path);

                if (StringUtils.isNotEmpty(test.getDataTable())) {
                    Element dataTableElement = doc.createElement("DataTable");
                    dataTableElement.setAttribute("path", "${WORKSPACE}\\${CHECKOUT_SUBDIR}" + SdkConstants.FileSystem.WINDOWS_PATH_SPLITTER + test.getDataTable());
                    testElement.appendChild(dataTableElement);
                }

                rootElement.appendChild(testElement);
            }

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));

            return writer.toString();
        } catch (Exception e) {
            throw new IOException("Failed to build MTBX content : " + e.getMessage());
        }

    }

    /**
     * Create (if needed) and run test discovery
     *
     * @param discoveryInfo
     */
    public static void runTestDiscovery(DiscoveryInfo discoveryInfo) {

        /*
        {
          "scmRepository": {
            "type": "git",
            "url": "git@github.com:radislavB/UftTests.git"
          },
          "executorId": "1",
          "executorLogialName": "ABC",
          "workspaceId": "1002",
          "testingToolType": "uft",
          "forceFullDiscovery": true
        }
         */
        FreeStyleProject proj = getDiscoveryJob(discoveryInfo);

        //start job
        if (proj != null) {
            ParameterValue executorIdParam = new StringParameterValue(UftConstants.EXECUTOR_ID_PARAMETER_NAME, discoveryInfo.getExecutorId());
            ParameterValue fullScanParam = new BooleanParameterValue(UftConstants.FULL_SCAN_PARAMETER_NAME, discoveryInfo.isForceFullDiscovery());
            ParametersAction parameters = new ParametersAction(executorIdParam, fullScanParam);

            Cause cause = new Cause.UserIdCause();
            CauseAction causeAction = new CauseAction(cause);
            proj.scheduleBuild2(0, parameters, causeAction);
        }
    }

    private static FreeStyleProject getDiscoveryJob(DiscoveryInfo discoveryInfo) {

        try {
            String discoveryJobName = buildDiscoveryJobName(discoveryInfo.getTestingToolType(), discoveryInfo.getExecutorId(), discoveryInfo.getExecutorLogicalName());
            //validate creation of job
            FreeStyleProject proj = (FreeStyleProject) Jenkins.getInstance().getItem(discoveryJobName);
            if (proj == null) {

                proj = Jenkins.getInstance().createProject(FreeStyleProject.class, discoveryJobName);
                proj.setDescription(String.format("This job was created by the Micro Focus Application Automation Tools plugin for discovery of %s tests. It is associated with ALM Octane testing tool connection #%s.",
                        discoveryInfo.getTestingToolType().toString(), discoveryInfo.getExecutorId()));
            }

            setScmRepository(discoveryInfo.getScmRepository(), discoveryInfo.getScmRepositoryCredentialsId(), proj, false);
            setBuildDiscarder(proj, 20);
            addConstantParameter(proj, UftConstants.EXECUTOR_ID_PARAMETER_NAME, discoveryInfo.getExecutorId(), "ALM Octane testing tool connection ID");
            addConstantParameter(proj, UftConstants.EXECUTOR_LOGICAL_NAME_PARAMETER_NAME, discoveryInfo.getExecutorLogicalName(), "ALM Octane testing tool connection logical name");
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
                uftTestDetectionPublisher = new UFTTestDetectionPublisher(discoveryInfo.getWorkspaceId(), discoveryInfo.getScmRepositoryId());
                publishers.add(uftTestDetectionPublisher);
            }

            return proj;
        } catch (IOException | ANTLRException e) {
            logger.error("Failed to  create DiscoveryJob : " + e.getMessage());
            return null;
        }
    }

    private static void addTimestamper(FreeStyleProject proj) {
        try {
            Descriptor<BuildWrapper> wrapperDescriptor = Jenkins.getInstance().getBuildWrapper("TimestamperBuildWrapper");
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

    private static String buildDiscoveryJobName(TestingToolType testingToolType, String executorId, String executorLogicalName) {
        String name = String.format("%s %s %s (%s)", testingToolType.toString(), UftConstants.DISCOVERY_JOB_MIDDLE_NAME, executorId, executorLogicalName);
        return name;
    }

    private static void setBuildDiscarder(FreeStyleProject proj, int numBuildsToKeep) throws IOException {
        int irrelevant = -1;
        BuildDiscarder bd = new LogRotator(irrelevant, numBuildsToKeep, irrelevant, irrelevant);
        proj.setBuildDiscarder(bd);
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
            parameters = new ParametersDefinitionProperty(new ArrayList<ParameterDefinition>());
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

    private static void addDiscoveryAssignedNode(FreeStyleProject proj)  {
        try {
            Label joinedLabel = Label.parseExpression(Jenkins.getInstance().getSelfLabel() +"||" + Jenkins.getInstance().getSelfLabel());
            //why twice Jenkins.getInstance().getSelfLabel()==master? because only one master is not saved in method proj.setAssignedLabel as it is label of Jenkins.getInstance().getSelfLabel()
            proj.setAssignedLabel(joinedLabel);
        } catch (ANTLRException|IOException e) {
            logger.error("Failed to  set add DiscoveryAssignedNode : " + e.getMessage());
        }
    }

    private static void addExecutionAssignedNode(FreeStyleProject proj) {
        Computer[] computers = Jenkins.getInstance().getComputers();
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
                    if (label.contains(" ")) {
                        label = "\"" + label + "\"";
                    }
                    labels.add(label);
                }
            }

            if (!labels.isEmpty()) {
                Label joinedLabel = Label.parseExpression(StringUtils.join(labels, "||"));
                proj.setAssignedLabel(joinedLabel);
            }

        } catch (IOException | ANTLRException e) {
            logger.error("Failed to  set addExecutionAssignedNode : " + e.getMessage());
        }
    }
}
