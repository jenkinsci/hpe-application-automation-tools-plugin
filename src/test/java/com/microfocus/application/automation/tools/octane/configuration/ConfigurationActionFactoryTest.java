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
