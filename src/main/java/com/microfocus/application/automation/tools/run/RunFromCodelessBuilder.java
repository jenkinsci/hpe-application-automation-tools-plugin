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
package com.microfocus.application.automation.tools.run;

import com.microfocus.application.automation.tools.Messages;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import jenkins.tasks.SimpleBuildStep;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.PrintStream;

import static com.microfocus.application.automation.tools.octane.executor.UftConstants.CODELESS_FOLDER_TEMPLATE;

/**
 * @author Itay Karo on 11/11/2021
 */
public class RunFromCodelessBuilder extends Builder implements SimpleBuildStep {

    private static final String RUN_MSG_TEMPLATE = "Running codeless for test: %s, path: %s%n";

    private static final String RESULT_MSG_TEMPLATE = "Codeless returned code: %d%n";

    private static final String CODELESS_BATCH_FILE = "CodelessExecuter.bat";

    @DataBoundConstructor
    public RunFromCodelessBuilder() {
        //for codeclimate
    }

    @Override
    public void perform(@NonNull Run<?, ?> build, @NonNull FilePath workspace, @NonNull Launcher launcher, @NonNull TaskListener taskListener) throws InterruptedException, IOException {
        FilePath parentFolder = workspace.child(String.format(CODELESS_FOLDER_TEMPLATE, build.getNumber()));
        if (!parentFolder.exists()) { // no codeless tests to run
            taskListener.getLogger().println(RunFromCodelessBuilder.class.getSimpleName() + " : No codeless tests were found");
            return;
        }

        FilePath[] list = parentFolder.list("mbt.json");
        if (list.length == 0) {
            taskListener.getLogger().println(RunFromCodelessBuilder.class.getSimpleName() + " : mbt.json file was not found");
            return;
        }
        FilePath mbtJsonFile = list[0];
        JSONArray mbtJsonArr = (JSONArray) JSONValue.parse(mbtJsonFile.read());
        PrintStream out = taskListener.getLogger();
        Result currentResult = Result.NOT_BUILT;

        for (Object o : mbtJsonArr) {
            JSONObject jsonObject = (JSONObject) o;
            String testName = jsonObject.getAsString("testName");
            String filePath = jsonObject.getAsString("path");

            ArgumentListBuilder args = new ArgumentListBuilder();
            args.add(CODELESS_BATCH_FILE);
            args.add(filePath);

            // Run the script on node
            // Execution result should be 0
            try {
                out.printf(RUN_MSG_TEMPLATE, testName, filePath);
                int returnCode = launcher.launch().cmds(args).stdout(out).pwd(parentFolder).join();
                out.printf(RESULT_MSG_TEMPLATE, returnCode);
                currentResult = getResultFromCodeless(returnCode, currentResult);
            } catch (IOException e) {
                Util.displayIOException(e, taskListener);
                build.setResult(Result.FAILURE);
                taskListener.error("Failed running {} with exception {}", CODELESS_BATCH_FILE, e);
            } catch (InterruptedException e) {
                build.setResult(Result.ABORTED);
                taskListener.error("Failed running {} - build aborted {}", CODELESS_BATCH_FILE, e);
                Thread.currentThread().interrupt();//for codeclimate
            }
        }

        build.setResult(currentResult);
    }

    @Override
    public RunFromCodelessBuilder.DescriptorImpl getDescriptor() {
        return (RunFromCodelessBuilder.DescriptorImpl) super.getDescriptor();
    }

    private Result getResultFromCodeless(int reportCode, Result currentResult) {
        Result codelessResult = convertReportCode(reportCode);

        if (currentResult.equals(Result.NOT_BUILT)) { // initial status
            return codelessResult;
        } else if ((currentResult.equals(Result.SUCCESS) && codelessResult.equals(Result.FAILURE)) || (currentResult.equals(Result.FAILURE) && codelessResult.equals(Result.SUCCESS))) {
            return Result.UNSTABLE;
        } else {
            return codelessResult;
        }
    }

    private Result convertReportCode(int reportCode) {
        switch (reportCode) {
            case 0:
                return Result.SUCCESS;
            case 1:
                return Result.FAILURE;
            case -1:
                return Result.ABORTED;
            default:
                return Result.SUCCESS;
        }
    }

    /**
     * The type Descriptor.
     */
    @Symbol("runFromCodelessBuilder")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        /**
         * Instantiates a new Descriptor.
         */
        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return com.microfocus.application.automation.tools.Messages.RunFromCodelessBuilderStepName(Messages.CompanyName());
        }

    }

    ;
}
