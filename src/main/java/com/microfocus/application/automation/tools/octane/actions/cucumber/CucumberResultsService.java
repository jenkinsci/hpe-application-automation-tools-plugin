/*
 *  Certain versions of software accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 *  This software was acquired by Micro Focus on September 1, 2017, and is now
 *  offered by OpenText.
 *  Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical
 *  in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the
 *  property of their respective owners.
 *  OpenText is a trademark of Open Text.
 *  __________________________________________________________________
 *  MIT License
 *
 *  Copyright 2012-2025 Open Text.
 *
 *  The only warranties for products and services of Open Text and
 *  its affiliates and licensors ("Open Text") are as may be set forth
 *  in the express warranty statements accompanying such products and services.
 *  Nothing herein should be construed as constituting an additional warranty.
 *  Open Text shall not be liable for technical or editorial errors or
 *  omissions contained herein. The information contained herein is subject
 *  to change without notice.
 *
 *  Except as specifically indicated otherwise, this document contains
 *  confidential information and a valid license is required for possession,
 *  use or copying. If this work is provided to the U.S. Government,
 *  consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 *  Computer Software Documentation, and Technical Data for Commercial Items are
 *  licensed to the U.S. Government under vendor's standard commercial license.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ___________________________________________________________________
 */
package com.microfocus.application.automation.tools.octane.actions.cucumber;

import com.microfocus.application.automation.tools.octane.Messages;
import hudson.FilePath;
import hudson.Util;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Helper Service for Gherkin results
 */
public class CucumberResultsService {

    public static final String GHERKIN_NGA_RESULTS_XML = "OctaneGherkinResults.xml";
    public static final String GHERKIN_NGA_RESULTS = "OctaneGherkinResults";
    public static final String DEFAULT_GLOB = "**/*" + GHERKIN_NGA_RESULTS_XML;

    private static TaskListener listener;

    public static String getGherkinResultFileName(int index) {
        return GHERKIN_NGA_RESULTS + index + ".xml";
    }

    public static String[] getCucumberResultFiles(final FilePath workspace, String glob) throws IOException, InterruptedException {
        if (glob == null || glob.isEmpty()) {
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
        String contentStr = new String(content, 0, Math.min(content.length , 2000));
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

    public static void setListener(TaskListener l) {
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
