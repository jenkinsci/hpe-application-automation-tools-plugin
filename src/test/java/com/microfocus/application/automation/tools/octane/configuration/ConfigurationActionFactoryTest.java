/*
 * Certain versions of software accessible here may contain branding from Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 * This software was acquired by Micro Focus on September 1, 2017, and is now offered by OpenText.
 * Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright 2012-2024 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.octane.configuration;

import com.microfocus.application.automation.tools.octane.tests.ExtensionUtil;
import hudson.matrix.Axis;
import hudson.matrix.AxisList;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixProject;
import hudson.model.FreeStyleProject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;

public class ConfigurationActionFactoryTest {

    @ClassRule
    static final public JenkinsRule jenkins = new JenkinsRule();

    private ConfigurationActionFactory configurationActionFactory;

    @Before
    public void initialize() {
        configurationActionFactory = ExtensionUtil.getInstance(jenkins, ConfigurationActionFactory .class);
    }

    @Test
    public void testMatrixJob() throws IOException {

        MatrixProject matrixProject = jenkins.createProject(MatrixProject.class, "ConfigurationActionFactoryTest.testMatrixJob");
        //jenkins.createMatrixProject("ConfigurationActionFactoryTest.testMatrixJob");
        matrixProject.setAxes(new AxisList(new Axis("OS", "Linux", "Windows")));

        Assert.assertEquals(1, configurationActionFactory.createFor(matrixProject).size());
        for (MatrixConfiguration configuration: matrixProject.getItems()) {
            Assert.assertEquals(0, configurationActionFactory.createFor(configuration).size());
        }
    }

    @Test
    public void testFreeStyleJob() throws IOException {
        FreeStyleProject project = jenkins.createFreeStyleProject("ConfigurationActionFactoryTest.testFreeStyleJob");
        Assert.assertEquals(1, configurationActionFactory.createFor(project).size());
    }
}
