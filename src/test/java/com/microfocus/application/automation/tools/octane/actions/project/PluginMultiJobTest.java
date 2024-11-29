/*
 *  Certain versions of software accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 *  This software was acquired by Micro Focus on September 1, 2017, and is now
 *  offered by OpenText.
 *  Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical
 *  in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the
 *  property of their respective owners.
 *  OpenText is a trademark of Open Text.
 *  __________________________________________________________________
 *  MIT License
 *
 *  Copyright 2012-2025 Open Text.
 *
 *  The only warranties for products and services of Open Text and
 *  its affiliates and licensors ("Open Text") are as may be set forth
 *  in the express warranty statements accompanying such products and services.
 *  Nothing herein should be construed as constituting an additional warranty.
 *  Open Text shall not be liable for technical or editorial errors or
 *  omissions contained herein. The information contained herein is subject
 *  to change without notice.
 *
 *  Except as specifically indicated otherwise, this document contains
 *  confidential information and a valid license is required for possession,
 *  use or copying. If this work is provided to the U.S. Government,
 *  consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 *  Computer Software Documentation, and Technical Data for Commercial Items are
 *  licensed to the U.S. Government under vendor's standard commercial license.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ___________________________________________________________________
 */
package com.microfocus.application.automation.tools.octane.actions.project;

import com.hp.octane.integrations.dto.parameters.CIParameter;
import com.hp.octane.integrations.dto.parameters.CIParameterType;
import com.hp.octane.integrations.dto.pipelines.PipelineNode;
import com.hp.octane.integrations.dto.pipelines.PipelinePhase;
import com.microfocus.application.automation.tools.octane.OctanePluginTestBase;
import com.microfocus.application.automation.tools.octane.tests.TestUtils;
import com.tikal.jenkins.plugins.multijob.MultiJobBuilder;
import com.tikal.jenkins.plugins.multijob.MultiJobProject;
import com.tikal.jenkins.plugins.multijob.PhaseJobsConfig;
import hudson.matrix.MatrixProject;
import hudson.model.*;
import hudson.plugins.parameterizedtrigger.*;
import hudson.tasks.BuildTrigger;
import hudson.tasks.Fingerprinter;
import hudson.tasks.Shell;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: gullery
 * Date: 13/01/15
 * Time: 11:44
 * To change this template use File | Settings | File Templates.
 */

public class PluginMultiJobTest extends OctanePluginTestBase {
    //  Structure test: multi-job, no params, no children
    @Test
    public void testStructureMultiJobNoParamsNoChildren() throws IOException, SAXException {
        String projectName = "root-job-" + UUID.randomUUID().toString();
        rule.getInstance().createProject(MultiJobProject.class, projectName);

        String taskUrl = "nga/api/v1/jobs/" + projectName;
        PipelineNode pipeline = TestUtils.sendTask(taskUrl, PipelineNode.class);

        assertEquals(projectName, pipeline.getJobCiId());
        assertEquals(projectName, pipeline.getName());
        assertEquals(0, pipeline.getParameters().size());
        assertEquals(0, pipeline.getPhasesInternal().size());
        assertEquals(0, pipeline.getPhasesPostBuild().size());
    }

    //  Structure test: multi-job, with params, no children
    //
    @Test
    public void testStructureMultiJobWithParamsNoChildren() throws IOException, SAXException {
        String projectName = "root-job-" + UUID.randomUUID().toString();
        MultiJobProject p = rule.getInstance().createProject(MultiJobProject.class, projectName);
        ParametersDefinitionProperty params = new ParametersDefinitionProperty(Arrays.asList(
                (ParameterDefinition) new BooleanParameterDefinition("ParamA", true, "bool"),
                (ParameterDefinition) new StringParameterDefinition("ParamB", "str", "string"),
                (ParameterDefinition) new TextParameterDefinition("ParamC", "txt", "text"),
                (ParameterDefinition) new ChoiceParameterDefinition("ParamD", new String[]{"A", "B", "C"}, "choice"),
                (ParameterDefinition) new FileParameterDefinition("ParamE", "file param")
        ));
        p.addProperty(params);

        CIParameter tmpParam;

        String taskUrl = "nga/api/v1/jobs/" + projectName;
        PipelineNode pipeline = TestUtils.sendTask(taskUrl, PipelineNode.class);
        assertEquals(projectName, pipeline.getJobCiId());
        assertEquals(projectName, pipeline.getName());
        assertEquals(5, pipeline.getParameters().size());
        assertEquals(0, pipeline.getPhasesInternal().size());
        assertEquals(0, pipeline.getPhasesPostBuild().size());

        tmpParam = pipeline.getParameters().get(0);
        assertEquals("ParamA", tmpParam.getName());
        assertEquals(CIParameterType.BOOLEAN, tmpParam.getType());
        assertEquals("bool", tmpParam.getDescription());
        assertEquals(true, tmpParam.getDefaultValue());
        assertNull(tmpParam.getChoices());

        tmpParam = pipeline.getParameters().get(1);
        assertEquals("ParamB", tmpParam.getName());
        assertEquals(CIParameterType.STRING, tmpParam.getType());
        assertEquals("string", tmpParam.getDescription());
        assertEquals("str", tmpParam.getDefaultValue());
        assertNull(tmpParam.getChoices());

        tmpParam = pipeline.getParameters().get(2);
        assertEquals("ParamC", tmpParam.getName());
        assertEquals(CIParameterType.STRING, tmpParam.getType());
        assertEquals("text", tmpParam.getDescription());
        assertEquals("txt", tmpParam.getDefaultValue());
        assertNull(tmpParam.getChoices());

        tmpParam = pipeline.getParameters().get(3);
        assertEquals("ParamD", tmpParam.getName());
        assertEquals(CIParameterType.STRING, tmpParam.getType());
        assertEquals("choice", tmpParam.getDescription());
        assertEquals("A", tmpParam.getDefaultValue());
        assertNotNull(tmpParam.getChoices());
        assertEquals(3, tmpParam.getChoices().length);
        assertEquals("A", tmpParam.getChoices()[0]);
        assertEquals("B", tmpParam.getChoices()[1]);
        assertEquals("C", tmpParam.getChoices()[2]);

        tmpParam = pipeline.getParameters().get(4);
        assertEquals("ParamE", tmpParam.getName());
        assertEquals(CIParameterType.FILE, tmpParam.getType());
        assertEquals("file param", tmpParam.getDescription());
        assertEquals("", tmpParam.getDefaultValue());
        assertNull(tmpParam.getChoices());
    }

    //  Structure test: multi-job, with params, with children
    //
    @Test
    public void testStructureMultiJobWithParamsWithChildren() throws IOException, SAXException {
        String projectName = "root-job-" + UUID.randomUUID().toString();
        MultiJobProject p = rule.getInstance().createProject(MultiJobProject.class, projectName);
        FreeStyleProject p1 = rule.createFreeStyleProject("jobA");
        MatrixProject p2 = rule.createProject(MatrixProject.class, "jobB");
        MultiJobProject p3 = rule.getInstance().createProject(MultiJobProject.class, "jobC");
        MatrixProject p4 = rule.createProject(MatrixProject.class, "jobD");
        CustomProject p5 = rule.getInstance().createProject(CustomProject.class, "jobE");
        ParametersDefinitionProperty params = new ParametersDefinitionProperty(Arrays.asList(
                (ParameterDefinition) new BooleanParameterDefinition("ParamA", true, "bool"),
                (ParameterDefinition) new StringParameterDefinition("ParamB", "str", "string")
        ));
        p.addProperty(params);
        p.getBuildersList().add(new TriggerBuilder(Arrays.asList(
                new BlockableBuildTriggerConfig("jobA, jobB", new BlockingBehaviour(
                        Result.FAILURE,
                        Result.UNSTABLE,
                        Result.FAILURE
                ), Arrays.asList(new AbstractBuildParameters[0])),
                new BlockableBuildTriggerConfig("jobC,jobD", null, Arrays.asList(new AbstractBuildParameters[0]))
        )));
        p.getBuildersList().add(new MultiJobBuilder(
                "Build",
                //PhaseJobsConfig(String jobName, String jobAlias, String jobProperties, boolean currParams, List<AbstractBuildParameters> configs,
                // PhaseJobsConfig.KillPhaseOnJobResultCondition killPhaseOnJobResultCondition, boolean disableJob, boolean enableRetryStrategy,
                // String parsingRulesPath, int maxRetries, boolean enableCondition, boolean abortAllJob, String condition, boolean buildOnlyIfSCMChanges,
                // boolean applyConditionOnlyIfNoSCMChanges) {
                Arrays.asList(
                        createPhaseJobsConfig("jobA"),
                        createPhaseJobsConfig("jobB"),
                        createPhaseJobsConfig("jobE")
                ),
                MultiJobBuilder.ContinuationCondition.SUCCESSFUL,
                MultiJobBuilder.ExecutionType.SEQUENTIALLY,
                null
        ));
        p.getBuildersList().add(new Shell(""));
        p.getBuildersList().add(new MultiJobBuilder(
                "Test",
                Arrays.asList(
                        createPhaseJobsConfig("jobC"),
                        createPhaseJobsConfig("jobD")
                ),
                MultiJobBuilder.ContinuationCondition.SUCCESSFUL,
                MultiJobBuilder.ExecutionType.SEQUENTIALLY,
                null
        ));
        p.getPublishersList().add(new BuildTrigger("jobA, jobB", Result.SUCCESS));
        p.getPublishersList().add(new hudson.plugins.parameterizedtrigger.BuildTrigger(Collections.singletonList(
                new BuildTriggerConfig("jobC,jobD", ResultCondition.ALWAYS, false, null)
        )));
        p.getPublishersList().add(new Fingerprinter(""));

        List<PipelinePhase> tmpPhases;
        PipelineNode tmpNode;
        CIParameter tmpParam;

        String taskUrl = "nga/api/v1/jobs/" + projectName;
        PipelineNode pipeline = TestUtils.sendTask(taskUrl, PipelineNode.class);
        assertEquals(projectName, pipeline.getJobCiId());
        assertEquals(projectName, pipeline.getName());
        assertEquals(2, pipeline.getParameters().size());

        tmpParam = pipeline.getParameters().get(0);
        assertEquals("ParamA", tmpParam.getName());
        assertEquals(CIParameterType.BOOLEAN, tmpParam.getType());
        assertEquals("bool", tmpParam.getDescription());
        assertEquals(true, tmpParam.getDefaultValue());
        assertNull(tmpParam.getChoices());

        tmpParam = pipeline.getParameters().get(1);
        assertEquals("ParamB", tmpParam.getName());
        assertEquals(CIParameterType.STRING, tmpParam.getType());
        assertEquals("string", tmpParam.getDescription());
        assertEquals("str", tmpParam.getDefaultValue());
        assertNull(tmpParam.getChoices());

        //  Phases Internal
        //
        tmpPhases = pipeline.getPhasesInternal();
        assertEquals(4, tmpPhases.size());

        //  Phase 0
        assertEquals("", tmpPhases.get(0).getName());
        assertTrue(tmpPhases.get(0).isBlocking());
        assertEquals(2, tmpPhases.get(0).getJobs().size());

        tmpNode = tmpPhases.get(0).getJobs().get(0);
        assertEquals("jobA", tmpNode.getJobCiId());
        assertEquals("jobA", tmpNode.getName());
        assertEquals(0, tmpNode.getParameters().size());
        assertEquals(0, tmpNode.getPhasesInternal().size());
        assertEquals(0, tmpNode.getPhasesPostBuild().size());
        tmpNode = tmpPhases.get(0).getJobs().get(1);
        assertEquals("jobB", tmpNode.getJobCiId());
        assertEquals("jobB", tmpNode.getName());
        assertEquals(0, tmpNode.getParameters().size());
        assertEquals(0, tmpNode.getPhasesInternal().size());
        assertEquals(0, tmpNode.getPhasesPostBuild().size());

        //  Phase 1
        assertEquals("", tmpPhases.get(1).getName());
        assertFalse(tmpPhases.get(1).isBlocking());
        assertEquals(2, tmpPhases.get(1).getJobs().size());

        tmpNode = tmpPhases.get(1).getJobs().get(0);
        assertEquals("jobC", tmpNode.getJobCiId());
        assertEquals("jobC", tmpNode.getName());
        assertEquals(0, tmpNode.getParameters().size());
        assertEquals(0, tmpNode.getPhasesInternal().size());
        assertEquals(0, tmpNode.getPhasesPostBuild().size());
        tmpNode = tmpPhases.get(1).getJobs().get(1);
        assertEquals("jobD", tmpNode.getJobCiId());
        assertEquals("jobD", tmpNode.getName());
        assertEquals(0, tmpNode.getParameters().size());
        assertEquals(0, tmpNode.getPhasesInternal().size());
        assertEquals(0, tmpNode.getPhasesPostBuild().size());

        //  Phase 2
        assertEquals("Build", tmpPhases.get(2).getName());
        assertTrue(tmpPhases.get(2).isBlocking());
        assertEquals(3, tmpPhases.get(2).getJobs().size());

        tmpNode = tmpPhases.get(2).getJobs().get(0);
        assertEquals("jobA", tmpNode.getJobCiId());
        assertEquals("jobA", tmpNode.getName());
        assertEquals(0, tmpNode.getParameters().size());
        assertEquals(0, tmpNode.getPhasesInternal().size());
        assertEquals(0, tmpNode.getPhasesPostBuild().size());
        tmpNode = tmpPhases.get(2).getJobs().get(1);
        assertEquals("jobB", tmpNode.getJobCiId());
        assertEquals("jobB", tmpNode.getName());
        assertEquals(0, tmpNode.getParameters().size());
        assertEquals(0, tmpNode.getPhasesInternal().size());
        assertEquals(0, tmpNode.getPhasesPostBuild().size());
        tmpNode = tmpPhases.get(2).getJobs().get(2);
        assertEquals("jobE", tmpNode.getJobCiId());
        assertEquals("jobE", tmpNode.getName());
        assertEquals(0, tmpNode.getParameters().size());
        assertEquals(0, tmpNode.getPhasesInternal().size());
        assertEquals(0, tmpNode.getPhasesPostBuild().size());

        //  Phase 3
        assertEquals("Test", tmpPhases.get(3).getName());
        assertTrue(tmpPhases.get(3).isBlocking());
        assertEquals(2, tmpPhases.get(3).getJobs().size());

        tmpNode = tmpPhases.get(3).getJobs().get(0);
        assertEquals("jobC", tmpNode.getJobCiId());
        assertEquals("jobC", tmpNode.getName());
        assertEquals(0, tmpNode.getParameters().size());
        assertEquals(0, tmpNode.getPhasesInternal().size());
        assertEquals(0, tmpNode.getPhasesPostBuild().size());
        tmpNode = tmpPhases.get(3).getJobs().get(1);
        assertEquals("jobD", tmpNode.getJobCiId());
        assertEquals("jobD", tmpNode.getName());
        assertEquals(0, tmpNode.getParameters().size());
        assertEquals(0, tmpNode.getPhasesInternal().size());
        assertEquals(0, tmpNode.getPhasesPostBuild().size());

        //  Phases Post build
        //
        tmpPhases = pipeline.getPhasesPostBuild();
        assertEquals(2, tmpPhases.size());

        //  Phase 0
        assertEquals("downstream", tmpPhases.get(0).getName());
        assertFalse(tmpPhases.get(0).isBlocking());
        assertEquals(2, tmpPhases.get(0).getJobs().size());

        tmpNode = tmpPhases.get(0).getJobs().get(0);
        assertEquals("jobA", tmpNode.getJobCiId());
        assertEquals("jobA", tmpNode.getName());
        assertEquals(0, tmpNode.getParameters().size());
        assertEquals(0, tmpNode.getPhasesInternal().size());
        assertEquals(0, tmpNode.getPhasesPostBuild().size());
        tmpNode = tmpPhases.get(0).getJobs().get(1);
        assertEquals("jobB", tmpNode.getJobCiId());
        assertEquals("jobB", tmpNode.getName());
        assertEquals(0, tmpNode.getParameters().size());
        assertEquals(0, tmpNode.getPhasesInternal().size());
        assertEquals(0, tmpNode.getPhasesPostBuild().size());

        //  Phase 1
        assertEquals("", tmpPhases.get(1).getName());
        assertFalse(tmpPhases.get(1).isBlocking());
        assertEquals(2, tmpPhases.get(1).getJobs().size());

        tmpNode = tmpPhases.get(1).getJobs().get(0);
        assertEquals("jobC", tmpNode.getJobCiId());
        assertEquals("jobC", tmpNode.getName());
        assertEquals(0, tmpNode.getParameters().size());
        assertEquals(0, tmpNode.getPhasesInternal().size());
        assertEquals(0, tmpNode.getPhasesPostBuild().size());
        tmpNode = tmpPhases.get(1).getJobs().get(1);
        assertEquals("jobD", tmpNode.getJobCiId());
        assertEquals("jobD", tmpNode.getName());
        assertEquals(0, tmpNode.getParameters().size());
        assertEquals(0, tmpNode.getPhasesInternal().size());
        assertEquals(0, tmpNode.getPhasesPostBuild().size());
    }

    private PhaseJobsConfig createPhaseJobsConfig(String jobName){
        return new PhaseJobsConfig(jobName, "", null, false, null,
                PhaseJobsConfig.KillPhaseOnJobResultCondition.NEVER, false, false, null, 1,
                false, false, null, false, false);
    }
}
