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

package com.microfocus.application.automation.tools.pipelineSteps;

import com.microfocus.application.automation.tools.octane.tests.HPRunnerType;
import com.microfocus.application.automation.tools.run.RunFromFileBuilder;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * The UFT pipeline step execution.
 */
public class UftScenarioLoadStepExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {

    private static final long serialVersionUID = 1L;
    @Inject

    private transient UftScenarioLoadStep step;

    @StepContextParameter
    private transient TaskListener listener;

    @StepContextParameter
    private transient FilePath ws;

    @StepContextParameter
    private transient Run build;

    @StepContextParameter
    private transient Launcher launcher;

    public UftScenarioLoadStepExecution() {
        //no need for actual construction
    }

    @Override
    protected Void run() throws Exception {
        listener.getLogger().println("Running UftScenarioLoadStepExecution");

        setRunnerTypeAsParameter();

        step.getRunFromFileBuilder().perform(build, ws, launcher, listener);

        HashMap<String, String> resultFilename = new HashMap<String, String>(0);
        resultFilename.put(RunFromFileBuilder.class.getName(), step.getRunFromFileBuilder().getRunResultsFileName());
        step.getRunResultRecorder().pipelinePerform(build, ws, launcher, listener, resultFilename);

        return null;
    }

    private void setRunnerTypeAsParameter() {
        ParametersAction parameterAction = build.getAction(ParametersAction.class);
        List<ParameterValue> newParams = (parameterAction != null) ? new ArrayList<>(parameterAction.getAllParameters()) : new ArrayList<>();
        newParams.add(new StringParameterValue(HPRunnerType.class.getSimpleName(), HPRunnerType.UFT.name()));
        ParametersAction newParametersAction = new ParametersAction(newParams);
        build.addOrReplaceAction(newParametersAction);
    }
}
