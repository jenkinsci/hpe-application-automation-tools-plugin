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

package com.microfocus.application.automation.tools.octane.executor;

import com.google.inject.Inject;
import com.hp.octane.integrations.OctaneClient;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.executor.impl.TestingToolType;
import com.hp.octane.integrations.exceptions.OctaneRestException;
import com.hp.octane.integrations.services.entities.EntitiesService;
import com.hp.octane.integrations.uft.UftTestDispatchUtils;
import com.hp.octane.integrations.uft.items.JobRunContext;
import com.hp.octane.integrations.uft.items.UftTestDiscoveryResult;
import com.hp.octane.integrations.utils.SdkStringUtils;
import com.microfocus.application.automation.tools.octane.ResultQueue;
import com.microfocus.application.automation.tools.octane.actions.UFTActionDetectionBuildAction;
import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import com.microfocus.application.automation.tools.octane.tests.AbstractSafeLoggingAsyncPeriodWork;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Job;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * This class is responsible to send discovered uft tests to Octane.
 * Class uses file-based queue so if octane or jenkins will be down before sending,
 * after connection is up - this dispatcher will send tests to Octane.
 * <p>
 * Actually list of discovered tests are persisted in job run directory. Queue contains only reference to that job run.
 */
@Extension
public class UftTestDiscoveryDispatcher extends AbstractSafeLoggingAsyncPeriodWork {

    private static final Logger logger = SDKBasedLoggerProvider.getLogger(UftTestDiscoveryDispatcher.class);

    private static final int MAX_DISPATCH_TRIALS = 5;

    private UftTestDiscoveryQueue queue;

    private volatile boolean stopped = false;

    public UftTestDiscoveryDispatcher() {
        super("Uft Test Discovery Dispatcher");
    }

    private static void dispatchDetectionResults(ResultQueue.QueueItem item, EntitiesService entitiesService, UftTestDiscoveryResult result, AbstractBuild build) {
        //Check if there is diff in discovery and server status
        //for example : discovery found new test , but it already exist in server , instead of create new tests we will do update test
        UftTestDispatchUtils.prepareDiscoveryResultForDispatch(entitiesService, result);

        try {
            build.getWorkspace().act(new UFTTestDetectionFinalResultSaverCallable(result, build.getNumber()));
        } catch (Exception e) {
            logger.info("Failed to save final result : " + e.getMessage());
        }

        //dispatch
        JobRunContext jobRunContext = JobRunContext.create(item.getProjectName(), item.getBuildNumber());
        UftTestDispatchUtils.dispatchDiscoveryResult(entitiesService, result, jobRunContext, null);
        if (result.getTestingToolType().equals(TestingToolType.MBT)) {
            UFTActionDetectionBuildAction action = build.getAction(UFTActionDetectionBuildAction.class);
            action.setResults(result);
            try {
                build.save(); // save build in order to update the discovery report (build.xml in jenkins)
            } catch (IOException e) {
                logger.info("Failed to save build: " + e.getMessage());
            }
        }
    }

    @Override
    protected void doExecute(TaskListener listener) {
        if (stopped) {
            return;
        }

        if (queue.peekFirst() == null) {
            return;
        }

        if (OctaneSDK.getClients().isEmpty()) {
            logger.warn("There are pending discovered UFT tests, but no Octane configuration is found, results can't be submitted");
            return;
        }

        ResultQueue.QueueItem item = null;
        try {
            while ((item = queue.peekFirst()) != null) {
                if (queueContainsPostponedItems(item)) {
                    logger.warn("Project [" + item.getProjectName() + "] has postpone items");
                    //all postponed items are in the end of queue, so it we encountered one postponed item, other postponed items will come after it, so we do break
                    break;
                }

                Job project = (Job) Jenkins.get().getItemByFullName(item.getProjectName());
                if (project == null) {
                    logger.warn("Project [" + item.getProjectName() + "] no longer exists, pending discovered tests can't be submitted");
                    queue.remove();
                    continue;
                }

                AbstractBuild build = (AbstractBuild) project.getBuildByNumber(item.getBuildNumber());
                if (build == null) {
                    logger.warn("Build [" + item.getProjectName() + "#" + item.getBuildNumber() + "] no longer exists, pending discovered tests can't be submitted");
                    queue.remove();
                    continue;
                }

                UftTestDiscoveryResult result = UFTTestDetectionService.readDetectionResults(build);
                if (result == null) {
                    logger.warn("Build [" + item.getProjectName() + "#" + item.getBuildNumber() + "] no longer contains valid detection result file");
                    queue.remove();
                    continue;
                }

                OctaneClient client;
                try {
                    client = OctaneSDK.getClientByInstanceId(result.getConfigurationId());
                } catch (Exception e) {
                    logger.error("Build [" + item.getProjectName() + "#" + item.getBuildNumber() + "] does not have valid configuration " + result.getConfigurationId() + " : " + e.getMessage());
                    queue.remove();
                    continue;
                }

                if (!client.getConfigurationService().isConnected()) {
                    logger.info(client.getConfigurationService().getConfiguration().getLocationForLog() +
                            " - Build [" + item.getProjectName() + "#" + item.getBuildNumber() + "] - octane is down , postponing sending UFT tests ");
                    //if octane is down - put current item to the end of queue and try wth other items (that might be from another octane)
                    item.setSendAfter(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));
                    queue.remove();
                    queue.add(item);
                    continue;
                }

                logger.warn("Persistence [" + item.getProjectName() + "#" + item.getBuildNumber() + "]");
                dispatchDetectionResults(item, client.getEntitiesService(), result, build);
                queue.remove();
            }
        } catch (OctaneRestException e) {
            String reasonDesc = StringUtils.isNotEmpty(e.getData().getDescriptionTranslated()) ? e.getData().getDescriptionTranslated() : e.getData().getDescription();
            if (e.getResponseStatus() == HttpStatus.SC_FORBIDDEN) {
                logger.error("Failed to  persist discovery of [" + item.getProjectName() + "#" + item.getBuildNumber() + "]  because of lacking Octane permission : " + reasonDesc);
            } else {
                logger.error("Failed to  persist discovery of [" + item.getProjectName() + "#" + item.getBuildNumber() + "]  : " + reasonDesc);
            }
            queue.remove();
        } catch (Exception e) {
            if (item != null) {
                item.incrementFailCount();
                if (item.incrementFailCount() > MAX_DISPATCH_TRIALS) {
                    queue.remove();
                    logger.error("Failed to  persist discovery of [" + item.getProjectName() + "#" + item.getBuildNumber() + "]  after " + MAX_DISPATCH_TRIALS + " trials");
                }
            }
        }
    }

    private boolean queueContainsPostponedItems(ResultQueue.QueueItem queueItem) {
        if (queueItem.getSendAfter() > 0 && queueItem.getSendAfter() > System.currentTimeMillis()) {
            //all postponed items are in the end of queue, so it we encountered one postponed item, other postponed items will come after it, so we do break
            return true;
        }
        return false;
    }

    public void close() {
        logger.info("stopping the UFT dispatcher and closing its queue");
        stopped = true;
        queue.close();
    }

    @Override
    public long getRecurrencePeriod() {
        String value = System.getProperty("UftTestDiscoveryDispatcher.Period"); // let's us config the recurrence period. default is 30 seconds.
        if (!SdkStringUtils.isEmpty(value)) {
            return Long.parseLong(value);
        }
        return TimeUnit.SECONDS.toMillis(30);
    }

    @Inject
    public void setTestResultQueue(UftTestDiscoveryQueue queue) {
        this.queue = queue;
    }

    /**
     * Queue that current run contains discovered tests
     *
     * @param projectName jobs name
     * @param buildNumber build number
     */
    public void enqueueResult(String instanceId, String projectName, int buildNumber, String workspace) {
        queue.add(instanceId, projectName, buildNumber, workspace);
    }

}
