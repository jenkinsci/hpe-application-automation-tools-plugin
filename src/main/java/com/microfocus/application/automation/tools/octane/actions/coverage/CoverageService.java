/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.microfocus.application.automation.tools.octane.actions.coverage;

import hudson.FilePath;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Helper Service for coverage publisher
 */
public class CoverageService {
    private static final String COVERAGE_REPORT_FILE_NAME_PREFIX = "coverage_report";
    private static BuildListener listener;

    public static class Jacoco {
        public static final String JACOCO_TYPE = "JACOCOXML";
        public static final String JACOCO_FILE_EXTENSION = ".xml";
        public static final String JACOCO_DEFAULT_FILE_NAME = "jacoco" + JACOCO_FILE_EXTENSION;
        public static final String JACOCO_DEFAULT_PATH = "**/target/site/*/" + JACOCO_DEFAULT_FILE_NAME;

    }
    public static class Lcov {
        public static final String LCOV_TYPE = "LCOV";
        public static final String LCOV_FILE_EXTENSION = ".info";
        public static final String LCOV_DEFAULT_FILE_NAME = "lcov" + LCOV_FILE_EXTENSION;
        public static final String LCOV_DEFAULT_PATH = "**/coverage/" + LCOV_DEFAULT_FILE_NAME;

    }

    public static String getCoverageReportFileName(int index, String fileSuffix) {
        return COVERAGE_REPORT_FILE_NAME_PREFIX + index + "-" + fileSuffix;
    }

    public static String[] getCoverageFiles(final FilePath workspace, String glob) throws IOException, InterruptedException {
        log(String.format("Looking for files that match the pattern %s in root directory %s", glob, workspace.getName()));
        return workspace.act(new ResultFilesCallable(glob));
    }

    public static void copyCoverageFile(File resultFile, File targetReportFile, final FilePath workspace) throws IOException, InterruptedException {
        log(String.format("Copying %s to %s", resultFile.getPath(), targetReportFile));

        byte[] content = workspace.act(new FileContentCallable(resultFile));

        if (validateContent(content)) {
            log("Got coverage file content");

            try (FileOutputStream os = new FileOutputStream(targetReportFile)) {
                os.write(content);
            }
            log(String.format("coverage file copied successfully to %s", targetReportFile.getPath()));
        } else {
            log("coverage file content corrupted, failed to copy the file to target destination");
        }
    }

    /**
     * most of the validations will be done in octane side
     * this is a place holder to do more validations if needed
     * @param content of the file
     * @return status
     */
    private static boolean validateContent(byte[] content) {
        return content.length > 0;
    }

    public static void log(final String message) {
        if(listener != null) {
            listener.getLogger().println(message);
        }
    }

    public static void setListener(BuildListener l) {
        listener = l;
    }

    /**
     * this class searched for files that match specific pattern
     */
    private static final class ResultFilesCallable extends MasterToSlaveFileCallable<String[]> {
        private final String glob;

        private ResultFilesCallable(String glob) {
            this.glob = glob;
        }

        @Override
        public String[] invoke(File rootDir, VirtualChannel channel) throws IOException {
            FileSet fs = Util.createFileSet(rootDir, glob);
            DirectoryScanner ds = fs.getDirectoryScanner();
            return ds.getIncludedFiles();
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
