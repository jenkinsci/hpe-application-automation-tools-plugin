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

package com.microfocus.application.automation.tools.uft.model;

import com.microfocus.application.automation.tools.model.EnumDescription;
import com.microfocus.application.automation.tools.uft.utils.UftToolUtils;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class UftSettingsModel extends AbstractDescribableImpl<UftSettingsModel> {
    public static final String ENTIRE_TEST_SET = "Rerun the entire set of tests";
    public static final String SPECIFIC_TESTS = "Rerun specific tests in the build";
    public static final String ONLY_FAILED_TESTS = "Rerun only failed tests";
    public static final EnumDescription ANY_BUILD_TEST = new EnumDescription( ENTIRE_TEST_SET, ENTIRE_TEST_SET);
    public static final EnumDescription SPECIFIC_BUILD_TEST = new EnumDescription(SPECIFIC_TESTS, SPECIFIC_TESTS);
    public static final EnumDescription FAILED_BUILD_TEST = new EnumDescription(ONLY_FAILED_TESTS, ONLY_FAILED_TESTS);
    public static final List<EnumDescription> fsTestTypes = Arrays.asList(ANY_BUILD_TEST, SPECIFIC_BUILD_TEST, FAILED_BUILD_TEST);

    private String selectedNode;
    private String fsTestPath;
    private String numberOfReruns;
    private String cleanupTest;
    private String onCheckFailedTest;
    private String fsTestType;
    private List<RerunSettingsModel> rerunSettingsModels;

    @DataBoundConstructor
    public UftSettingsModel(String selectedNode, String numberOfReruns, String cleanupTest, String onCheckFailedTest,
                            String fsTestType, List<RerunSettingsModel> rerunSettingsModels) {
        this.selectedNode = selectedNode;
        this.numberOfReruns = numberOfReruns;
        this.cleanupTest = cleanupTest;
        this.onCheckFailedTest = onCheckFailedTest;
        this.fsTestType = fsTestType;
        this.setRerunSettingsModels(UftToolUtils.updateRerunSettings(getSelectedNode(), getFsTestPath(),
              rerunSettingsModels));
    }

    public List<String> getNodes() {
        return UftToolUtils.getNodesList();
    }

    public String getSelectedNode() {
        return selectedNode;
    }

    @DataBoundSetter
    public void setSelectedNode(String selectedNode) {
        this.selectedNode = selectedNode;
    }

    public String getFsTestPath() {
        return fsTestPath;
    }

    public void setFsTestPath(String fsTestPath) {
        this.fsTestPath = fsTestPath;
    }

    public String getNumberOfReruns() {
        return numberOfReruns;
    }

    @DataBoundSetter
    public void setNumberOfReruns(String numberOfReruns) {
        this.numberOfReruns = numberOfReruns;
    }

    public String getCleanupTest() {
        return cleanupTest;
    }

    @DataBoundSetter
    public void setCleanupTest(String cleanupTest) {
        this.cleanupTest = cleanupTest;
    }

    public String getOnCheckFailedTest() {
        return onCheckFailedTest;
    }

    @DataBoundSetter
    public void setOnCheckFailedTest(String onCheckFailedTest) {
        this.onCheckFailedTest = onCheckFailedTest;
    }

    public String getFsTestType() {
        return fsTestType;
    }

    @DataBoundSetter
    public void setFsTestType(String fsTestType) {
        this.fsTestType = fsTestType;
    }

    /**
     * Gets the rerun settings
     *
     * @return the rerun settings
     */
    public List<RerunSettingsModel> getRerunSettingsModels() {
        return UftToolUtils.updateRerunSettings(selectedNode, getFsTestPath(), rerunSettingsModels);
    }

    @DataBoundSetter
    public void setRerunSettingsModels(List<RerunSettingsModel> rerunSettingsModels) {
        this.rerunSettingsModels = rerunSettingsModels;
    }

    public List<EnumDescription> getFsTestTypes() {
        return fsTestTypes;
    }

    /**
     * Add properties (failed tests, cleanup tests, number of reruns) to properties file
     *
     * @param props
     */
    public void addToProperties(Properties props) {
        if (!StringUtils.isEmpty(this.selectedNode)) {
            props.put("Selected node", this.selectedNode);
        }

        String onCheckFailedTestVal = StringUtils.isEmpty(this.onCheckFailedTest) ? "false" : this.onCheckFailedTest;
        props.put("onCheckFailedTest", onCheckFailedTestVal);

        props.put("testType", this.fsTestType);

        switch(this.fsTestType){
            case ENTIRE_TEST_SET :
                addPropertiesForEntireSet(props);
                break;

            case SPECIFIC_TESTS:
                addPropertiesForSpecificTests(props);
                break;

            case ONLY_FAILED_TESTS:
                addPropertiesForFailedTests(props);
                break;

            default: break;
        }
    }


    /**
     * Add failed tests, number of reruns and cleanup to the set of properties in case of on failure scenario (rerun the entire test set)
     * @param props task properties
     */
    private void addPropertiesForEntireSet(Properties props) {
        int i = 1;
        int index = 1;
        while (props.getProperty("Test" + index) != null) {
            props.put("FailedTest" + index, props.getProperty("Test" + index));
            index++;
        }

        //add number of reruns
        if (!StringUtils.isEmpty(this.numberOfReruns)) {
            props.put("Reruns" + i, this.numberOfReruns);
        }

        //add cleanup test
        if (!StringUtils.isEmpty(this.cleanupTest)) {
            props.put("CleanupTest" + i, this.cleanupTest);
        }
    }

    /**
     * dd failed tests, number of reruns and cleanup to the set of properties in case of on failure scenario (rerun specific tests)
     * @param props task properties
     */
    private void addPropertiesForSpecificTests(Properties props){
        int j = 1;
        if(this.rerunSettingsModels != null && !this.rerunSettingsModels.isEmpty()) {
            for (RerunSettingsModel settings : this.rerunSettingsModels) {
                if (settings.getChecked()) {//test is selected
                    props.put("FailedTest" + j, settings.getTest());
                    props.put("Reruns" + j, String.valueOf(settings.getNumberOfReruns()));
                    if (StringUtils.isEmpty(settings.getCleanupTest())){
                        j++; continue;
                    }
                    props.put("CleanupTest" + j, settings.getCleanupTest());
                    j++;
                }
            }
        }
    }

    /**
     * Add failed tests, number of reruns and cleanup to the set of properties in case of on failure scenario (rerun only failed tests)
     * @param props task properties
     */
    private void addPropertiesForFailedTests(Properties props) {
        //add number of reruns
        if (!StringUtils.isEmpty(this.numberOfReruns)) {
            props.put("Reruns1", this.numberOfReruns);
        }
        //add cleanup test
        if (!StringUtils.isEmpty(this.cleanupTest)) {
            props.put("CleanupTest1", this.cleanupTest);
        }
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<UftSettingsModel> {
        @Nonnull
        @Override
        public String getDisplayName() {
            return "UFT Settings Model";
        }

        public FormValidation doCheckNumberOfReruns(@QueryParameter String value) {
            return UftToolUtils.doCheckNumberOfReruns(value);
        }

        public List<EnumDescription> getFsTestTypes() {
            return fsTestTypes;
        }

        public List<String> getNodes() {
            return UftToolUtils.getNodesList();
        }
    }
}
