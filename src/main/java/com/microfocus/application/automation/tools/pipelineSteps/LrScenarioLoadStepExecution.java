/*
 * © Copyright 2013 EntIT Software LLC
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright (c) 2018 Micro Focus Company, L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ___________________________________________________________________
 *
 */

package com.microfocus.application.automation.tools.pipelineSteps;

import com.microfocus.application.automation.tools.run.RunFromFileBuilder;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;


/**
 * The Load runner pipeline step execution.
 */
public class LrScenarioLoadStepExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {

    /**
     * Logger.
     */
    private static final Logger logger = Logger
            .getLogger(LrScenarioLoadStepExecution.class.getName());

  private static final long serialVersionUID = 1L;
  @Inject
  @SuppressWarnings("squid:S3306")
  private transient LoadRunnerTestStep step;
  @StepContextParameter
  private transient TaskListener listener;
  @StepContextParameter
  private transient FilePath ws;
  @StepContextParameter
  private transient Run build;
  @StepContextParameter
  private transient Launcher launcher;

    public LrScenarioLoadStepExecution() {
        //no need for actual construction
    }

    @Override
    protected Void run() throws InterruptedException {
        listener.getLogger().println("Running LoadRunner Scenario step");
         try {
                step.getRunFromFileBuilder().perform(build, ws, launcher, listener);
            } catch (IOException e) {
                listener.fatalError("LoadRunnner scenario run stage encountered an IOException " + e);
                build.setResult(Result.FAILURE);
                return null;
         }
            HashMap<String, String> resultFilename = new HashMap<String, String>(0);
            resultFilename.put(RunFromFileBuilder.class.getName(), step.getRunFromFileBuilder().getRunResultsFileName());

            try {
                step.getRunResultRecorder().pipelinePerform(build, ws, launcher, listener, resultFilename);
            } catch (IOException e) {
                listener.fatalError("LoadRunnner scenario run result recorder stage encountered an IOException " + e);
                build.setResult(Result.FAILURE);
                return null;
            }

        return null;
    }
}