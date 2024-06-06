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

package com.microfocus.application.automation.tools.octane.events;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.scm.SCMData;
import com.microfocus.application.automation.tools.octane.model.processors.scm.SCMProcessors;
import com.microfocus.application.automation.tools.octane.model.processors.scm.SCMUtils;
import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.SCMListener;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCM;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Run Listener that handles SCM CI events and dispatches notifications to the Octane server
 * Created by gullery on 10/07/2016.
 */

@Extension
public class SCMListenerOctaneImpl extends SCMListener {

    @Override
    public void onChangeLogParsed(Run<?, ?> run, SCM scm, TaskListener listener, ChangeLogSet<?> changelog) throws Exception {
        if (!OctaneSDK.hasClients()) {
            return;
        }
        super.onChangeLogParsed(run, scm, listener, changelog);

        String jobCiId = BuildHandlerUtils.getJobCiId(run);
        String buildCiId = BuildHandlerUtils.getBuildCiId(run);

        SCMData scmData = SCMUtils.extractSCMData(run, scm, SCMProcessors.getAppropriate(scm.getClass().getName()));


        if (scmData != null) {
            String parents = BuildHandlerUtils.getRootJobCiIds(run);
            SCMUtils.persistSCMData(run, jobCiId, buildCiId, scmData);
            OctaneSDK.getClients().forEach(octaneClient ->
                    octaneClient.getSCMDataService().enqueueSCMData(jobCiId, buildCiId, scmData, parents));
        }
    }
}
