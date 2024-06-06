/*
 * Certain versions of software accessible here may contain branding from Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 * This software was acquired by Micro Focus on September 1, 2017, and is now offered by OpenText.
 * Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright 2012-2024 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.lr.run;

import com.microfocus.application.automation.tools.common.utils.HealthAnalyzerCommon;
import com.microfocus.application.automation.tools.common.utils.OperatingSystem;
import com.microfocus.application.automation.tools.common.model.HealthAnalyzerModel;
import com.microfocus.application.automation.tools.common.model.VariableWrapper;
import com.microfocus.application.automation.tools.common.model.VariableListWrapper;
import com.microfocus.application.automation.tools.lr.Messages;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

public class HealthAnalyzerLrStep extends HealthAnalyzerModel {
    private static final String LR_REGISTRY_PATH =
            "HKEY_LOCAL_MACHINE\\SOFTWARE\\Wow6432Node\\Mercury Interactive\\LoadRunner\\CurrentVersion";
    private static final transient HealthAnalyzerCommon healthAnalyzerCommon =
            new HealthAnalyzerCommon(Messages.ProductName());
    private final boolean checkLrInstallation;
    private final boolean checkOsVersion;
    private final VariableListWrapper checkFiles;


    @DataBoundConstructor
    public HealthAnalyzerLrStep(boolean checkLrInstallation, boolean checkOsVersion, VariableListWrapper checkFiles) {
        this.checkLrInstallation = checkLrInstallation;
        this.checkOsVersion = checkOsVersion;
        this.checkFiles = checkFiles;
    }

    public boolean isCheckOsVersion() {
        return checkOsVersion;
    }

    public VariableListWrapper getCheckFiles() {
        return checkFiles;
    }

    public boolean isFilesExist() {
        return checkFiles != null;
    }

    public List<VariableWrapper> getFilesList() {
        return checkFiles != null ? checkFiles.getFilesList() : null;
    }

    public boolean isCheckLrInstallation() {
        return checkLrInstallation;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher,
                        @Nonnull TaskListener listener) throws InterruptedException, IOException {
        healthAnalyzerCommon.ifCheckedPerformWindowsInstallationCheck(LR_REGISTRY_PATH, checkLrInstallation, workspace);
        healthAnalyzerCommon.ifCheckedPerformFilesExistenceCheck(getFilesList(), isFilesExist(), workspace);
        healthAnalyzerCommon.ifCheckedPerformOsCheck(OperatingSystem.WINDOWS, checkOsVersion, workspace);
    }

    @Extension
    public static class DescriptorImpl extends HealthAnalyzerModelDescriptor {
        @Override
        public String toString() {
            return "Info in DescriptorImpl at HealthAnalyzerLrStep";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.ProductName();
        }
    }
}
