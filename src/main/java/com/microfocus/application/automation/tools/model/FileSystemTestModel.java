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
package com.microfocus.application.automation.tools.model;

import com.microfocus.application.automation.tools.sse.common.StringUtils;
import com.microfocus.application.automation.tools.uft.utils.UftToolUtils;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents the tests that ParallelRunner should run and the environments on which to run them.
 */
public class FileSystemTestModel extends AbstractDescribableImpl<FileSystemTestModel> {
    private String tests; // tests to run
    private List<ParallelRunnerEnvironmentModel> parallelRunnerEnvironments; // the environments to run on

    /**
     * Constructs a new FileSystemTestModel
     * @param tests the tests to be run
     * @param parallelRunnerEnvironments the environments on which to run the tests on
     */
    @DataBoundConstructor
    public FileSystemTestModel(String tests,List<ParallelRunnerEnvironmentModel> parallelRunnerEnvironments) {
        this.tests = tests;
        this.parallelRunnerEnvironments = parallelRunnerEnvironments;
    }

    /**
     * Returns the tests.
     * @return the tests
     */
    public String getTests() {
        if(this.tests == null) {
            this.tests = "\n";
        }
        else if(!this.tests.contains("\n")) {
            this.tests+="\n";
        }

        return tests;
    }

    /**
     * Adds a new line to the tests string if not present (keep the expandableTextBox expanded).
     * @param tests the tests
     */
    private void setTestsWithNewLine(String tests) {
        this.tests = tests.trim();

        if (!this.tests.contains("\n")) {
            this.tests += "\n";
        }
    }

    /**
     * Returns the environments on which the tests will run.
     * @return the parallel runner environments
     */
    public List<ParallelRunnerEnvironmentModel> getParallelRunnerEnvironments() {
        return parallelRunnerEnvironments;
    }


    /**
     * Expand the tests based on the environment variables.
     * @param envVars the environment variables
     * @return the parsed list of tests
     */
    public List<String> parseTests(EnvVars envVars) {
        String expandedFsTests = envVars.expand(this.tests);

        List<String> result = new ArrayList<>();

        if(StringUtils.isNullOrEmpty(this.tests))
            return result;

        if (UftToolUtils.isMtbxContent(expandedFsTests)) {
            result.add(expandedFsTests);
        } else {
            result = Arrays.asList(expandedFsTests.replaceAll("\r", "").split("\n"));
        }

        return result;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<FileSystemTestModel> {
        @Nonnull
        public String getDisplayName() {return "File system test model";}
    }
}
