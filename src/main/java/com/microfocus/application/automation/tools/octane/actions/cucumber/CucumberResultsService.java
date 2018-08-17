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

package com.microfocus.application.automation.tools.octane.actions.cucumber;

import com.microfocus.application.automation.tools.octane.Messages;
import hudson.FilePath;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

import java.io.*;
import java.nio.file.Files;

/**
 * Helper Service for Gherkin results
 */
public class CucumberResultsService {

    public static final String GHERKIN_NGA_RESULTS_XML = "OctaneGherkinResults.xml";
    public static final String GHERKIN_NGA_RESULTS = "OctaneGherkinResults";
    public static final String DEFAULT_GLOB = "**/*" + GHERKIN_NGA_RESULTS_XML;

    private static BuildListener listener;

    public static String getGherkinResultFileName(int index) {
        return GHERKIN_NGA_RESULTS + index + ".xml";
    }

    public static String[] getCucumberResultFiles(final FilePath workspace, String glob) throws IOException, InterruptedException {
        if (glob.isEmpty() || glob == null) {
            glob = DEFAULT_GLOB;
            log(Messages.CucumberResultsActionEmptyConfiguration(), glob);
        }

        log("Looking for files that match the pattern %s in root directory %s", glob, workspace.getName());
        return workspace.act(new ResultFilesCallable(glob));
    }

    public static void copyResultFile(File resultFile, File destinationFolder, final FilePath workspace) throws IOException, InterruptedException {
        File existingReportFile;
        int existingResultIndex = -1;

        log("Copying %s to %s", resultFile.getPath(), destinationFolder.getPath());

        do {
            existingReportFile = new File(destinationFolder, getGherkinResultFileName(++existingResultIndex));
        } while (existingReportFile.exists());
        log("New file name on destination will be %s", existingReportFile.getPath());

        byte[] content = workspace.act(new FileContentCallable(resultFile));
        log("Got result file content");

        validateContent(content);

        File target = existingReportFile;
        try (FileOutputStream os = new FileOutputStream(target)) {
            os.write(content);
        }
        log("Result file copied to %s", target.getPath());
    }

    private static void validateContent(byte[] content) {
        String contentStr = new String(content, 0, content.length > 2000 ? 2000 : content.length);
        //Heuristic validation. we don't check the whole file structure here - we should be quick.
        if(!contentStr.contains("<features")) {
            throw new IllegalArgumentException("The file is not Octane Gherkin results file");
        }
    }

    public static void log(final String message, final String... stringFormatArgs) {
        if(listener != null) {
            listener.getLogger().println(String.format("%s: %s",
                Messages.CucumberReporterName(), String.format(message, stringFormatArgs)));
        }
    }

    public static void setListener(BuildListener l) {
        listener = l;
    }

    private static final class ResultFilesCallable extends MasterToSlaveFileCallable<String[]> {
        private final String glob;

        private ResultFilesCallable(String glob) {
            this.glob = glob;
        }

        @Override
        public String[] invoke(File rootDir, VirtualChannel channel) throws IOException {
            FileSet fs = Util.createFileSet(rootDir, glob);
            DirectoryScanner ds = fs.getDirectoryScanner();
            String[] files = ds.getIncludedFiles();
            return files;
        }
    }

    private static final class FileContentCallable extends MasterToSlaveFileCallable<byte[]> {
        private final File file;

        private FileContentCallable(File file) {
            this.file = file;
        }

        @Override
        public byte[] invoke(File rootDir, VirtualChannel channel) throws IOException {
            return Files.readAllBytes(file.toPath());
        }
    }

}
