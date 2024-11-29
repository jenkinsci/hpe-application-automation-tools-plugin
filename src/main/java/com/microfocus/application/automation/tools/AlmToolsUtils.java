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
package com.microfocus.application.automation.tools;

import com.microfocus.application.automation.tools.settings.UFTEncryptionGlobalConfiguration;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.security.ACL;
import hudson.security.AuthorizationStrategy;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import hudson.util.ArgumentListBuilder;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SuppressWarnings("squid:S1160")
public final class AlmToolsUtils {

	private AlmToolsUtils() {
        // no meaning instantiating
	}
    public static void runOnBuildEnv(
            Run<?, ?> build,
            Launcher launcher,
            TaskListener listener,
            FilePath file,
            String paramFileName,
            Node node) throws IOException, InterruptedException {
        runOnBuildEnv(build, launcher, listener, file, paramFileName, node, "UTF-8");
    }
	public static void runOnBuildEnv(
            Run<?, ?> build,
            Launcher launcher,
            TaskListener listener,
            FilePath file,
            String paramFileName,
            Node node,
            String encoding) throws IOException, InterruptedException {

            ArgumentListBuilder args = new ArgumentListBuilder();
            PrintStream out = listener.getLogger();

            // Use script to run the cmdLine and get the console output
            args.add(file);
            args.add("-paramfile");
            args.add(paramFileName);
            if (StringUtils.isNotBlank(encoding)) {
                args.add("-encoding");
                args.add(encoding);
            }

            // for encryption
            Map<String, String> envs = new HashMap<>();

            try {
                UFTEncryptionGlobalConfiguration config = UFTEncryptionGlobalConfiguration.getInstance();
                envs.put("hptoolslauncher.key", Secret.fromString(config.getEncKey()).getPlainText());
            } catch (NullPointerException ignored) {
                throw new IOException("Failed to access encryption key, the module UFTEncryption is unavailable.");
            }

            if (node == null) {
                node = JenkinsUtils.getCurrentNode(file);

                if (node == null) {
                    throw new IOException("Failed to access current executor node.");
                }
            }

            try {
                envs.put("hptoolslauncher.rootpath", Objects.requireNonNull(node.getRootPath()).getRemote());
            } catch (NullPointerException e) {
                throw new IOException(e.getMessage());
            }

            // Run the script on node
            // Execution result should be 0
            int returnCode = launcher.launch().cmds(args).stdout(out).pwd(file.getParent()).envs(envs).join();

            if (returnCode != 0) {
                if (returnCode == -1) {
                    build.setResult(Result.FAILURE);
                } else if (returnCode == -2) {
                    build.setResult(Result.UNSTABLE);
                } else if (returnCode == -3) {
                    build.setResult(Result.ABORTED);
                    // throwing this exception ensures we enter into the respective catch branch in the callstack
                    throw new InterruptedException();
                } else {
                    listener.getLogger().println("Launch return code " + returnCode);
                    build.setResult(Result.FAILURE);
                    if(returnCode == -2146232576 && file.getRemote().toLowerCase(Locale.ROOT).contains("\\system32\\config")) {
                        listener.getLogger().println("!!! Move 'JENKINS_HOME' out of the 'windows\\\\system32' folder because the plugin may not have permissions to launch tests under this folder.");
                    }
                }
            }
    }

    public static void runHpToolsAborterOnBuildEnv(
            AbstractBuild<?, ?> build,
            Launcher launcher,
            BuildListener listener,
            String paramFileName) throws IOException, InterruptedException {

            runHpToolsAborterOnBuildEnv(build, launcher, listener, paramFileName, build.getWorkspace());
    }

	@SuppressWarnings("squid:S2259")
	public static void runHpToolsAborterOnBuildEnv(
            Run<?, ?> build,
            Launcher launcher,
            TaskListener listener,
            String paramFileName, FilePath runWorkspace) throws IOException, InterruptedException {

        ArgumentListBuilder args = new ArgumentListBuilder();
        PrintStream out = listener.getLogger();

        String hpToolsAborter_exe = "HpToolsAborter.exe";

		URL hpToolsAborterUrl = Jenkins.get().pluginManager.uberClassLoader.getResource("HpToolsAborter.exe");
        FilePath hpToolsAborterFile = runWorkspace.child(hpToolsAborter_exe);

        args.add(hpToolsAborterFile);
        args.add(paramFileName);

        hpToolsAborterFile.copyFrom(hpToolsAborterUrl);

        int returnCode = launcher.launch().cmds(args).stdout(out).pwd(hpToolsAborterFile.getParent()).join();

        try {
        	hpToolsAborterFile.delete();
		} catch (Exception e) {
			 listener.error("failed copying HpToolsAborter: " + e);
		}


        if (returnCode != 0) {
            if (returnCode == 1) {
                build.setResult(Result.FAILURE);
            } else if (returnCode == 2) {
                build.setResult(Result.UNSTABLE);
            } else if (returnCode == 3) {
                build.setResult(Result.ABORTED);
            }
        }
    }

    public static boolean tryCreatePropsFile(TaskListener listener, String props, FilePath fileProps) throws InterruptedException, IOException {

        if (StringUtils.isBlank(props)) {
            listener.fatalError("Missing properties text content."); //should never happen
            return false;
        }
        if (fileProps.exists() && fileProps.length() > 0) { // this should never happen
            listener.getLogger().println(String.format("NOTE: The file [%s] already exists.", fileProps.getRemote()));
        }

        String msg = String.format("Trying to create or replace the file [%s] ...", fileProps.getRemote());
        listener.getLogger().println(msg);
        return trySaveAndCheckPropsFile(listener, props, fileProps, 0);
    }

    private static boolean trySaveAndCheckPropsFile(TaskListener listener, String props, FilePath fileProps, int idxOfRetry) throws InterruptedException {
        boolean ok = false;
        try {
            try (InputStream in = IOUtils.toInputStream(props, StandardCharsets.UTF_8)) {
                fileProps.copyFrom(in);
            }
            Thread.sleep(1500);
            if (fileProps.exists() && fileProps.length() > 0) {
                String msg = "Successfully created the file";
                if (idxOfRetry == 0) {
                    listener.getLogger().println(String.format("%s [%s].", msg, fileProps.getName()));
                } else {
                    listener.getLogger().println(String.format("%s after %d %s.", msg, idxOfRetry, (idxOfRetry == 1 ? "retry" : "retries")));
                }
                ok = true;
            } else if (idxOfRetry > 5) {
                listener.fatalError("Failed to save the file " + fileProps.getName() + " after 5 retries.");
            } else {
                ok = trySaveAndCheckPropsFile(listener, props, fileProps, ++idxOfRetry);
            }
        } catch (IOException ioe) {
            if (idxOfRetry > 5) {
                listener.fatalError("Failed to save the file " + fileProps.getName() + " after 5 retries: " + ioe.getMessage());
            } else {
                Thread.sleep(1500);
                ok = trySaveAndCheckPropsFile(listener, props, fileProps, ++idxOfRetry);
            }
        }
        return ok;
    }

    public static String getPropsAsString(Properties props) throws IOException {
        try (OutputStream stream = new ByteArrayOutputStream()) {
            props.store(stream, "");
            return stream.toString();
        }
    }
}
