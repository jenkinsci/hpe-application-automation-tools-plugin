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
