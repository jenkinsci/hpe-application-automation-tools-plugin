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

package com.microfocus.application.automation.tools.run;

import javax.annotation.Nonnull;
import java.io.PrintStream;
import java.util.ArrayList;

import com.microfocus.application.automation.tools.model.SvDeployModel;
import com.microfocus.application.automation.tools.model.SvServerSettingsModel;
import com.microfocus.application.automation.tools.sv.runner.AbstractSvRemoteRunner;
import com.microfocus.application.automation.tools.sv.runner.AbstractSvRunBuilder;
import com.microfocus.application.automation.tools.sv.runner.AbstractSvRunDescriptor;
import com.microfocus.sv.svconfigurator.core.IDataModel;
import com.microfocus.sv.svconfigurator.core.IPerfModel;
import com.microfocus.sv.svconfigurator.core.IProject;
import com.microfocus.sv.svconfigurator.core.IService;
import com.microfocus.sv.svconfigurator.processor.DeployProcessor;
import com.microfocus.sv.svconfigurator.processor.DeployProcessorInput;
import com.microfocus.sv.svconfigurator.processor.IDeployProcessor;
import com.microfocus.sv.svconfigurator.serverclient.ICommandExecutor;
import com.microfocus.sv.svconfigurator.util.ProjectUtils;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class SvDeployBuilder extends AbstractSvRunBuilder<SvDeployModel> {

    @DataBoundConstructor
    public SvDeployBuilder(String serverName, boolean force, String service, String projectPath, String projectPassword,
                           boolean firstAgentFallback) {
        super(new SvDeployModel(serverName, force, service, projectPath, projectPassword, firstAgentFallback));
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    protected RemoteRunner getRemoteRunner(@Nonnull FilePath workspace, TaskListener listener, SvServerSettingsModel server) {
        return new RemoteRunner(model, workspace, listener, server);
    }

    private static class RemoteRunner extends AbstractSvRemoteRunner<SvDeployModel> {

        private RemoteRunner(SvDeployModel model, FilePath workspace, TaskListener listener, SvServerSettingsModel server) {
            super(listener, model, workspace, server);
        }

        @Override
        public String call() throws Exception {
            PrintStream logger = listener.getLogger();

            IProject project = loadProject(workspace);
            printProjectContent(project, logger);
            deployServiceFromProject(project, logger);

            return null;
        }

        private void printProjectContent(IProject project, PrintStream logger) {
            logger.println("  Project content:");
            for (IService service : project.getServices()) {
                logger.println("    Service: " + service.getName() + " [" + service.getId() + "]");
                for (IDataModel dataModel : service.getDataModels()) {
                    logger.println("      DM: " + dataModel.getName() + " [" + dataModel.getId() + "]");
                }
                for (IPerfModel perfModel : service.getPerfModels()) {
                    logger.println("      PM: " + perfModel.getName() + " [" + perfModel.getId() + "]");
                }
            }
        }

        private Iterable<IService> getServiceList(IProject project) {
            if (model.getService() == null) {
                return project.getServices();
            } else {
                ArrayList<IService> list = new ArrayList<>();
                list.add(ProjectUtils.findProjElem(project.getServices(), model.getService()));
                return list;
            }
        }

        private void deployServiceFromProject(IProject project, PrintStream logger) throws Exception {
            IDeployProcessor processor = new DeployProcessor(null);
            ICommandExecutor commandExecutor = createCommandExecutor();

            for (IService service : getServiceList(project)) {
                logger.printf("  Deploying service '%s' [%s] %n", service.getName(), service.getId());
                DeployProcessorInput deployInput = new DeployProcessorInput(model.isForce(), false, project, model.getService(), null, false);
                deployInput.setFirstAgentFailover(model.isFirstAgentFallback());
                processor.process(deployInput, commandExecutor);
            }
        }
    }

    @Override
    protected void logConfig(PrintStream logger, String prefix) {
        super.logConfig(logger, prefix);
        logger.println(prefix + "First agent fallback: " + model.isFirstAgentFallback());
    }

    @Extension
    public static final class DescriptorImpl extends AbstractSvRunDescriptor {

        public DescriptorImpl() {
            super("SV: Deploy Virtual Service");
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckProjectPath(@QueryParameter String projectPath) {
            if (StringUtils.isBlank(projectPath)) {
                return FormValidation.error("Project path cannot be empty");
            }
            return FormValidation.ok();
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckService(@QueryParameter String service) {
            if (StringUtils.isBlank(service)) {
                return FormValidation.ok("All services from project will be deployed if no service is specified");
            }
            return FormValidation.ok();
        }

    }
}
