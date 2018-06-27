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

import com.microfocus.application.automation.tools.run.SvDeployBuilder;
import hudson.Extension;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class SvDeployStep extends AbstractSvStep {
    private final String service;
    private final String projectPath;
    private final String projectPassword;
    private final boolean firstAgentFallback;

    @DataBoundConstructor
    public SvDeployStep(String serverName, boolean force, String service, String projectPath, String projectPassword, boolean firstAgentFallback) {
        super(serverName, force);
        this.service = service;
        this.projectPath = projectPath;
        this.projectPassword = projectPassword;
        this.firstAgentFallback = firstAgentFallback;
    }

    public String getService() {
        return service;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public String getProjectPassword() {
        return projectPassword;
    }

    public boolean isFirstAgentFallback() {
        return firstAgentFallback;
    }

    @Override
    protected SimpleBuildStep getBuilder() {
        return new SvDeployBuilder(serverName, force, service, projectPath, projectPassword, firstAgentFallback);
    }

    @Extension
    public static class DescriptorImpl extends AbstractSvStepDescriptor<SvDeployBuilder.DescriptorImpl> {
        public DescriptorImpl() {
            super(SvExecution.class, "svDeployStep", new SvDeployBuilder.DescriptorImpl());
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckProjectPath(@QueryParameter String projectPath) {
            return builderDescriptor.doCheckProjectPath(projectPath);
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckService(@QueryParameter String service) {
            return builderDescriptor.doCheckService(service);
        }
    }
}
