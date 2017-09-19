/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.hpe.application.automation.tools.pipelineSteps;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/** 
* LoadRunnerTestStep Tester.
* 
* @author <Authors name> 
* @since <pre>??? 28, 2016</pre> 
* @version 1.0 
*/
@SuppressWarnings({"squid:S2699","squid:S3658","squid:S2259","squid:S1872","squid:S2925","squid:S109","squid:S1607","squid:S2701"})
public class LrScenarioLoadStepTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();


    @Before
    public void before() throws Exception {
    }

    @After
    public void after() throws Exception {
    }

    /**
     * Method: runLoadRunnerScenario
     */
    @Test
    public void testLrScenarioRun() throws Exception {
        // Setup the job
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "foo");
        job.setDefinition(new CpsFlowDefinition("node {}")); // insert here the pipeline code to run

        //run the build nad wait for successful result
        WorkflowRun workflowRun = jenkinsRule.assertBuildStatusSuccess(job.scheduleBuild2(0).get());

        //now we need to check the results.

    }
}