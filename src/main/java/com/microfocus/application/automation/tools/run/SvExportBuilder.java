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
import java.io.*;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;

import com.microfocus.sv.svconfigurator.build.ProjectBuilder;
import com.microfocus.sv.svconfigurator.core.IProject;
import com.microfocus.application.automation.tools.model.SvExportModel;
import com.microfocus.application.automation.tools.model.SvServiceSelectionModel;
import com.microfocus.sv.svconfigurator.core.IService;
import com.microfocus.sv.svconfigurator.core.impl.exception.CommandExecutorException;
import com.microfocus.sv.svconfigurator.core.impl.exception.CommunicatorException;
import com.microfocus.sv.svconfigurator.core.impl.exception.SVCParseException;
import com.microfocus.sv.svconfigurator.core.impl.jaxb.ServiceRuntimeConfiguration;
import com.microfocus.sv.svconfigurator.processor.ChmodeProcessor;
import com.microfocus.sv.svconfigurator.processor.ChmodeProcessorInput;
import com.microfocus.sv.svconfigurator.processor.ExportProcessor;
import com.microfocus.sv.svconfigurator.processor.IChmodeProcessor;
import com.microfocus.sv.svconfigurator.serverclient.ICommandExecutor;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.remoting.Channel;
import hudson.remoting.LocalChannel;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * Performs export of of virtual service
 */
public class SvExportBuilder extends AbstractSvRunBuilder<SvExportModel> {

    @DataBoundConstructor
    public SvExportBuilder(String serverName, boolean force, String targetDirectory, boolean cleanTargetDirectory,
                           SvServiceSelectionModel serviceSelection, boolean switchToStandByFirst, boolean archive) {
        super(new SvExportModel(serverName, force, targetDirectory, cleanTargetDirectory, serviceSelection, switchToStandByFirst, archive));
    }

    @Override
    protected void logConfig(PrintStream logger, String prefix) {
        logger.println(prefix + "Target Directory: " + model.getTargetDirectory());
        logger.println(prefix + "Switch to Stand-By: " + model.isSwitchToStandByFirst());
        super.logConfig(logger, prefix);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    protected void performImpl(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, Launcher launcher, TaskListener listener) throws Exception {
        boolean isMaster = !workspace.isRemote();

        PrintStream logger = listener.getLogger();

        ExportProcessor exportProcessor = new ExportProcessor(null);
        IChmodeProcessor chmodeProcessor = new ChmodeProcessor(null);

        ICommandExecutor exec = createCommandExecutor();
        IProject project = null;

        verifyNotNull(model.getTargetDirectory(), "Target directory must be set");

        String targetDirectory = "";
        if (isMaster) {
            targetDirectory = workspace.child(model.getTargetDirectory()).getRemote();
        } else {
            targetDirectory = Files.createTempDirectory("svExport").toString();
        }

        if (model.isCleanTargetDirectory()) {
            if (isMaster) {
                cleanTargetDirectory(logger, targetDirectory);
            } else {
                cleanSlaveTargetDirectory(logger, new FilePath(launcher.getChannel(), workspace.child(model.getTargetDirectory()).getRemote()));
            }
        }

        if (model.getServiceSelection().getSelectionType().equals(SvServiceSelectionModel.SelectionType.PROJECT)) {
            project = new ProjectBuilder().buildProject(new File(model.getServiceSelection().getProjectPath()), model.getServiceSelection().getProjectPassword());
        }

        for (ServiceInfo serviceInfo : getServiceList(false, logger, workspace)) {
            if (model.isSwitchToStandByFirst()) {
                switchToStandBy(serviceInfo, chmodeProcessor, exec, logger);
            }

            logger.printf("  Exporting service '%s' [%s] to %s %n", serviceInfo.getName(), serviceInfo.getId(), targetDirectory);
            verifyNotLearningBeforeExport(logger, exec, serviceInfo);
            if (!model.getServiceSelection().getSelectionType().equals(SvServiceSelectionModel.SelectionType.PROJECT)) {
                exportProcessor.process(exec, targetDirectory, serviceInfo.getId(), project, false, model.isArchive());
            }
        }
        if (model.getServiceSelection().getSelectionType().equals(SvServiceSelectionModel.SelectionType.PROJECT)) {
            exportProcessor.process(exec, targetDirectory, null, project, false, model.isArchive());
        }

        if (!isMaster) {
            String slaveDirectory = workspace.child(model.getTargetDirectory()).getRemote();
            FilePath localpath = new FilePath(new File(targetDirectory));
            FilePath slavepath = new FilePath(launcher.getChannel(), slaveDirectory);

            if (!slavepath.exists()) {
                try {
                    slavepath.mkdirs();
                } catch (IOException exc) {
                    throw new CommandExecutorException(String.format("Cannot create output directory %s:", slavepath));
                }
            } else if (!slavepath.isDirectory()) {
                throw new IllegalArgumentException("Specified path is not a directory: " + slavepath);
            }

            localpath.copyRecursiveTo(slavepath);
            localpath.deleteRecursive();
        }
    }

    private void verifyNotLearningBeforeExport(PrintStream logger, ICommandExecutor exec, ServiceInfo serviceInfo)
            throws CommunicatorException, CommandExecutorException {

        IService service = exec.findService(serviceInfo.getId(), null);
        ServiceRuntimeConfiguration info = exec.getServiceRuntimeInfo(service);
        if (info.getRuntimeMode() == ServiceRuntimeConfiguration.RuntimeMode.LEARNING) {
            logger.printf("    WARNING: Service '%s' [%s] is in Learning mode. Exported model need not be complete!",
                    serviceInfo.getName(), serviceInfo.getId());
        }
    }

    private void switchToStandBy(ServiceInfo service, IChmodeProcessor chmodeProcessor, ICommandExecutor exec, PrintStream logger)
            throws CommandExecutorException, SVCParseException, CommunicatorException {

        logger.printf("  Switching service '%s' [%s] to Stand-By mode before export%n", service.getName(), service.getId());
        ChmodeProcessorInput chmodeInput = new ChmodeProcessorInput(model.isForce(), null, service.getId(), null, null,
                ServiceRuntimeConfiguration.RuntimeMode.STAND_BY, false, false);
        chmodeProcessor.process(chmodeInput, exec);
    }

    /**
     * Cleans all sub-folders containing *.vproj file.
     */
    private void cleanTargetDirectory(PrintStream logger, String targetDirectory) throws IOException {
        File target = new File(targetDirectory);
        if (target.exists()) {
            File[] subfolders = target.listFiles((FilenameFilter) DirectoryFileFilter.INSTANCE);
            File[] files = target.listFiles((FilenameFilter) new SuffixFileFilter(".vproja"));
            if (subfolders.length > 0 || files.length > 0) {
                logger.println("  Cleaning target directory...");
            }
            for(File file : files) {
                FileUtils.forceDelete(file);
            }
            for (File subfolder : subfolders) {
                if (subfolder.listFiles((FilenameFilter) new SuffixFileFilter(".vproj")).length > 0) {
                    logger.println("    Deleting subfolder of target directory: " + subfolder.getAbsolutePath());
                    FileUtils.deleteDirectory(subfolder);
                } else {
                    logger.println("    Skipping delete of directory '" + subfolder.getAbsolutePath() + "' because it does not contain any *.vproj file.");
                }
            }
        }
    }

    private void cleanSlaveTargetDirectory(PrintStream logger, FilePath targetDirectory) throws IOException, InterruptedException {
        if(targetDirectory.exists()) {
            List<FilePath> subfolders = targetDirectory.listDirectories();
            List<FilePath> files = targetDirectory.list(new SuffixFileFilter(".vproj"));
            if (subfolders.size() > 0 || files.size() > 0) {
                logger.println("  Cleaning target directory...");
            }
            for(FilePath file : files) {
                file.delete();
            }
            for (FilePath subfolder : subfolders) {
                if (subfolder.list(new SuffixFileFilter(".vproj")).size() > 0) {
                    logger.println("    Deleting subfolder of target directory: " + subfolder.absolutize());
                    subfolder.deleteRecursive();
                } else {
                    logger.println("    Skipping delete of directory '" + subfolder.absolutize() + "' because it does not contain any *.vproj file.");
                }
            }
        }
    }


    @Extension
    public static final class DescriptorImpl extends AbstractSvRunDescriptor {

        public DescriptorImpl() {
            super("SV: Export Virtual Service");
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckTargetDirectory(@QueryParameter String targetDirectory) {
            if (StringUtils.isBlank(targetDirectory)) {
                return FormValidation.error("Target directory cannot be empty");
            }
            return FormValidation.ok();
        }
    }
}
