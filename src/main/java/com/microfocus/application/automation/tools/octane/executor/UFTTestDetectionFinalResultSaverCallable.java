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
package com.microfocus.application.automation.tools.octane.executor;

import com.hp.octane.integrations.uft.items.UftTestDiscoveryResult;
import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.remoting.RoleChecker;

import java.io.File;
import java.io.IOException;

/**
 * used to save final UFTTestDetection results in slave machine workspace
 */
public class UFTTestDetectionFinalResultSaverCallable extends MasterToSlaveFileCallable<String> {
    private UftTestDiscoveryResult results;
    private int buildNumber;

    private static final Logger logger = SDKBasedLoggerProvider.getLogger(UFTTestDetectionFinalResultSaverCallable.class);

    public UFTTestDetectionFinalResultSaverCallable(UftTestDiscoveryResult results, int buildNumber) {
        this.results = results;
        this.buildNumber = buildNumber;
    }

    @Override
    public String invoke(File file, VirtualChannel virtualChannel) {
        //publish final results
        File subWorkspace = new File(file, "_Final_Detection_Results");
        try {
            if (!subWorkspace.exists()) {
                subWorkspace.mkdirs();
            }
            File reportXmlFile = new File(subWorkspace, "final_detection_result_build_" + buildNumber + ".json");
            results.writeToFile(reportXmlFile);
        } catch (IOException e) {
            logger.error("Failed to write final_detection_result file :" + e.getMessage());
        }

        return null;
    }

    @Override
    public void checkRoles(RoleChecker roleChecker) throws SecurityException {
        //no need to check roles as this can be run on master and on slave
    }
}
