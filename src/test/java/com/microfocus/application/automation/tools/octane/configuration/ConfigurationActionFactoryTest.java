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
