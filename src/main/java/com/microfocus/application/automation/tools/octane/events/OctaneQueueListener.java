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
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.events.CIEvent;
import com.hp.octane.integrations.dto.events.CIEventType;
import com.microfocus.application.automation.tools.octane.CIJenkinsServicesImpl;
import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import com.microfocus.application.automation.tools.octane.model.processors.parameters.ParameterProcessors;
import com.microfocus.application.automation.tools.octane.model.processors.projects.JobProcessorFactory;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.ParametersAction;
import hudson.model.Queue;
import hudson.model.queue.QueueListener;
import org.apache.logging.log4j.Logger;

@Extension
public class OctaneQueueListener extends QueueListener {
    private static final Logger logger = SDKBasedLoggerProvider.getLogger(OctaneQueueListener.class);

    @Override
    public void onLeft(Queue.LeftItem li) {
        if (!OctaneSDK.hasClients()) {
            return;
        }

        if (li.isCancelled()) {
            if (li.task instanceof AbstractProject) {
                try {
                    ParametersAction paramActions = li.getAction(ParametersAction.class);
                    AbstractProject project = (AbstractProject) li.task;
                    CIEvent event = DTOFactory.getInstance().newDTO(CIEvent.class)
                            .setEventType(CIEventType.REMOVED_FROM_QUEUE)
                            .setProject(JobProcessorFactory.getFlowProcessor(project).getTranslatedJobName())
                            .setBuildCiId("-1")
                            .setParameters(ParameterProcessors.getInstances(paramActions));
                    CIJenkinsServicesImpl.publishEventToRelevantClients(event);
                } catch (Exception e) {
                    logger.error("Failed to set REMOVED_FROM_QUEUE event :" + e.getMessage() + "(" + li.task.getName() + ")");
                }
            } else {
                logger.error("Job is cancelled in queue but it isn't AbstractProject :" + li.task.getFullDisplayName() + "(" + li.task.getClass().getName() + ")");
            }
        }
    }
}
