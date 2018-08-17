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

package com.microfocus.application.automation.tools.run;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microfocus.application.automation.tools.model.AbstractSvRunModel;
import com.microfocus.application.automation.tools.model.SvServerSettingsModel;
import com.microfocus.application.automation.tools.model.SvServiceSelectionModel;
import com.hp.sv.jsvconfigurator.build.ProjectBuilder;
import com.hp.sv.jsvconfigurator.core.IProject;
import com.hp.sv.jsvconfigurator.core.IService;
import com.hp.sv.jsvconfigurator.core.impl.exception.CommandExecutorException;
import com.hp.sv.jsvconfigurator.core.impl.exception.CommunicatorException;
import com.hp.sv.jsvconfigurator.core.impl.exception.ProjectBuilderException;
import com.hp.sv.jsvconfigurator.core.impl.jaxb.atom.ServiceListAtom;
import com.hp.sv.jsvconfigurator.serverclient.ICommandExecutor;
import com.hp.sv.jsvconfigurator.serverclient.impl.CommandExecutorFactory;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;

class ServiceInfo {
    private final String id;
    private final String name;

    public ServiceInfo(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}

class ConfigurationException extends Exception {
    public ConfigurationException(String message) {
        super(message);
    }
}

public abstract class AbstractSvRunBuilder<T extends AbstractSvRunModel> extends Builder implements SimpleBuildStep {
    private static final Logger LOG = Logger.getLogger(AbstractSvRunBuilder.class.getName());

    protected final T model;

    protected AbstractSvRunBuilder(T model) {
        this.model = model;
    }

    protected static void verifyNotNull(Object value, String errorMessage) throws ConfigurationException {
        if (value == null) {
            throw new ConfigurationException(errorMessage);
        }
    }

    public T getModel() {
        return model;
    }

    public SvServiceSelectionModel getServiceSelection() {
        return model.getServiceSelection();
    }

    protected SvServerSettingsModel getSelectedServerSettings() throws ConfigurationException {
        SvServerSettingsModel[] servers = ((AbstractSvRunDescriptor) getDescriptor()).getServers();
        if (servers != null) {
            for (SvServerSettingsModel serverSettings : servers) {
                if (model.getServerName() != null && model.getServerName().equals(serverSettings.getName())) {
                    return serverSettings;
                }
            }
        }
        throw new ConfigurationException("Selected server configuration '" + model.getServerName() + "' does not exist.");
    }

    protected abstract void performImpl(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, Launcher launcher, TaskListener listener) throws Exception;

    protected ICommandExecutor createCommandExecutor() throws Exception {
        SvServerSettingsModel serverModel = getSelectedServerSettings();
        return new CommandExecutorFactory().createCommandExecutor(serverModel.getUrlObject(), serverModel.getCredentials());
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        PrintStream logger = listener.getLogger();
        Date startDate = new Date();
        try {
            SvServerSettingsModel serverModel = getSelectedServerSettings();

            logger.printf("%nStarting %s for SV Server '%s' (%s as %s) on %s%n", getDescriptor().getDisplayName(),
                    serverModel.getName(), serverModel.getUrlObject(), serverModel.getUsername(), startDate);
            logConfig(logger, "    ");
            validateServiceSelection();
            performImpl(run, workspace, launcher, listener);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Build failed: " + e.getMessage(), e);
            throw new AbortException(e.getMessage());
        } finally {
            double duration = (new Date().getTime() - startDate.getTime()) / 1000.;
            logger.printf("Finished: %s in %.3f seconds%n%n", getDescriptor().getDisplayName(), duration);
        }
    }

    protected void logConfig(PrintStream logger, String prefix) {
        SvServiceSelectionModel ss = model.getServiceSelection();
        switch (ss.getSelectionType()) {
            case SERVICE:
                logger.println(prefix + "Service name or id: " + ss.getService());
                break;
            case PROJECT:
                logger.println(prefix + "Project path: " + ss.getProjectPath());
                logger.println(prefix + "Project password: " + ((StringUtils.isNotBlank(ss.getProjectPassword())) ? "*****" : null));
                break;
            case ALL_DEPLOYED:
                logger.println(prefix + "All deployed services");
                break;
            case DEPLOY:
                logger.println(prefix + "Project path: " + ss.getProjectPath());
                logger.println(prefix + "Project password: " + ((StringUtils.isNotBlank(ss.getProjectPassword())) ? "*****" : null));
                logger.println(prefix + "Service name or id: " + ss.getService());
                break;
        }
        logger.println(prefix + "Force: " + model.isForce());
    }

    protected List<ServiceInfo> getServiceList(boolean ignoreMissingServices, PrintStream logger, FilePath workspace) throws Exception {
        SvServiceSelectionModel s = getServiceSelection();
        ICommandExecutor exec = createCommandExecutor();

        ArrayList<ServiceInfo> res = new ArrayList<>();

        switch (s.getSelectionType()) {
            case SERVICE:
                addServiceIfDeployed(s.getService(), res, ignoreMissingServices, exec, logger);
                break;
            case PROJECT:
                IProject project = loadProject(workspace);
                for (IService svc : project.getServices()) {
                    addServiceIfDeployed(svc.getId(), res, ignoreMissingServices, exec, logger);
                }
                break;
            case ALL_DEPLOYED:
                for (ServiceListAtom.ServiceEntry entry : exec.getServiceList(null).getEntries()) {
                    res.add(new ServiceInfo(entry.getId(), entry.getTitle()));
                }
                break;
            case DEPLOY:
                break;
        }
        return res;
    }

    private void addServiceIfDeployed(String service, ArrayList<ServiceInfo> results, boolean ignoreMissingServices,
                                      ICommandExecutor exec, PrintStream logger) throws CommunicatorException, CommandExecutorException {
        try {
            IService svc = exec.findService(service, null);
            results.add(new ServiceInfo(svc.getId(), svc.getName()));
        } catch (CommandExecutorException e) {
            if (!ignoreMissingServices) {
                throw e;
            }
            logger.printf("Service '%s' is not deployed, ignoring%n", service);
        }
    }

    protected IProject loadProject(FilePath workspace) throws ProjectBuilderException {
        SvServiceSelectionModel s = getServiceSelection();
        FilePath projectPath = workspace.child(s.getProjectPath());
        return new ProjectBuilder().buildProject(new File(projectPath.getRemote()), s.getProjectPassword());
    }

    protected void validateServiceSelection() throws ConfigurationException {
        SvServiceSelectionModel s = getServiceSelection();
        switch (s.getSelectionType()) {
            case SERVICE:
                verifyNotNull(s.getService(), "Service name or id must not be empty if service selection by name or id set");
                break;
            case PROJECT:
                verifyNotNull(s.getProjectPath(), "Project path must not be empty if service selection by project is set");
                break;
            case ALL_DEPLOYED:
                break;
            case DEPLOY:
                verifyNotNull(s.getProjectPath(), "Project path must not be empty for deployment");
                break;
        }
    }
}
