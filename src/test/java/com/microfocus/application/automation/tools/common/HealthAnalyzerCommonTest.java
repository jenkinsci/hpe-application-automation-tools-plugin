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

package com.microfocus.application.automation.tools.common;

import com.microfocus.application.automation.tools.common.utils.HealthAnalyzerCommon;
import com.microfocus.application.automation.tools.common.utils.OperatingSystem;
import hudson.AbortException;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.microfocus.application.automation.tools.common.OperatingSystemTest.initializeOperatingSystemOs;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;

public class HealthAnalyzerCommonTest {
    private final static String DUMMY_PRODUCT_NAME = "productName";
    private final static String NON_EXISTING_REGISTRY = "non\\existing\\registry\\value";
    private static HealthAnalyzerCommon healthAnalyzerCommon;
    private static String os;

    @BeforeClass
    public static void setup() {
        healthAnalyzerCommon = new HealthAnalyzerCommon(DUMMY_PRODUCT_NAME);
        os = System.getProperty("os.name");
    }

    @After
    public void tearDown() throws Exception {
        initializeOperatingSystemOs(os);
    }

    @Test
    public void isCheckedPerformWindowsInstallationCheck_throwsException_ifValueDoesNotExistsAndToCheckIsTrue()
            throws Exception {
        boolean returnValue = healthAnalyzerCommon.isRegistryExist(NON_EXISTING_REGISTRY);
        assertEquals("Operating system mismatch", false, returnValue);
    }

    @Test
    public void runningMethodOnNonWindows_throwsException() throws Exception {
        initializeOperatingSystemOs("Linux");
        boolean returnValue = healthAnalyzerCommon.isRegistryExist(NON_EXISTING_REGISTRY);
        assertEquals("Operating system mismatch", false, returnValue);
        initializeOperatingSystemOs(System.getProperty("os.name"));
    }

    @Test
    public void runningMethodOnWindowsWhenRegistryNotExists_throwsException() throws Exception {
        if (OperatingSystem.isWindows()) {
            boolean returnValue = healthAnalyzerCommon.isRegistryExist(NON_EXISTING_REGISTRY);
            assertEquals("Operating system mismatch", false, returnValue);
        }
    }

    @Test
    public void isRegistryExists_shouldReturnTrue_ifValueExists() throws Exception {
        if (OperatingSystem.isWindows()) {
            String existingRegistryValue = "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\";
            try {
                healthAnalyzerCommon.isRegistryExist(existingRegistryValue);
            } catch (AbortException e) {
                fail("Should not have thrown AbortException");
            }
        }
    }

    @Test
    public void ifCheckedPerformFilesExistenceCheck_throwsException_ifFileDoesNotExist() throws Exception {
        String file = "C:\\non\\existing\\jenkins\\plugin\\path";
        boolean returnValue = healthAnalyzerCommon.isFileExist(file);
        assertEquals("Operating system mismatch", false, returnValue);
    }

    @Test(expected = AbortException.class)
    public void ifCheckedPerformFilesExistenceCheck_throwsException_ifDirectory() throws Exception {
        if (OperatingSystem.isWindows()) {
            String file = "C:\\Users";
            healthAnalyzerCommon.isFileExist(file);
        }
    }

    @Test
    public void ifCheckedPerformFilesExistenceCheck_notThrowing_ifFileExist() {
        String file = null;

        if (OperatingSystem.isWindows()) {
            file = "C:\\Windows\\regedit.exe";
        } else if (OperatingSystem.isMac()) {
            file = "//bin";
        } else if (OperatingSystem.isLinux()) {
            file = "//proc";
        }

        try {
            healthAnalyzerCommon.isFileExist(file);
        } catch (AbortException e) {
            fail("Should not have thrown AbortException: The file doesn't exist");
        }
    }

    @Test
    public void ifCheckedPerformFilesExistenceCheck_shouldReturnTrue_ifNoFilesExist() {
        try {
            healthAnalyzerCommon.isFileExist(null);
            String file = "";
            healthAnalyzerCommon.isFileExist(file);
        } catch (AbortException e) {
            fail("Should not have thrown AbortException");
        }
    }
}