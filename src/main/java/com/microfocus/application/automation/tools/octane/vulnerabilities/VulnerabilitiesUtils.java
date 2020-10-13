/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2019 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors ("Micro Focus") are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.octane.vulnerabilities;

import com.hp.octane.integrations.OctaneClient;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.services.configurationparameters.FortifySSCFetchTimeout;
import com.hp.octane.integrations.services.vulnerabilities.ToolType;
import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import com.microfocus.application.automation.tools.octane.configuration.SSCServerConfigUtil;
import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import hudson.model.ParametersAction;
import hudson.model.Run;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class VulnerabilitiesUtils {
    private static Logger logger = SDKBasedLoggerProvider.getLogger(VulnerabilitiesUtils.class);
    private VulnerabilitiesUtils() {}

    public static void insertFODQueueItem(Run run, Long releaseId ) {
        HashMap<String,String> additionalProperties = new HashMap<>();
        additionalProperties.put("releaseId", releaseId.toString());
        insertQueueItem(run, ToolType.FOD, additionalProperties);
    }


    public static boolean insertQueueItem(Run run, SSCServerConfigUtil.SSCProjectVersionPair projectVersionPair) {
        if (projectVersionPair != null) {
            logger.warn("SSC configuration was found in " + run);
            String sscServerUrl = SSCServerConfigUtil.getSSCServer();
            if (sscServerUrl == null || sscServerUrl.isEmpty()) {
                logger.debug("SSC configuration not found in the whole CI Server");
                return false;
            }
            VulnerabilitiesUtils.insertQueueItem(run, ToolType.SSC, null);
        }
        return true;
    }

    private static void insertQueueItem(Run run, ToolType toolType, Map<String,String> props) {
        String jobCiId = BuildHandlerUtils.getJobCiId(run);
        String buildCiId = BuildHandlerUtils.getBuildCiId(run);

        final Long queueItemTimeoutHours = getQueueItemTimeoutHoursFromJob(run);
        OctaneSDK.getClients().forEach(octaneClient -> {
            octaneClient.getVulnerabilitiesService().enqueueRetrieveAndPushVulnerabilities(
                    jobCiId,
                    buildCiId, toolType,
                    run.getStartTimeInMillis(),
                    queueItemTimeoutHours == null ? getFortifyTimeoutHours(octaneClient.getInstanceId()) : queueItemTimeoutHours,
                    props);
        });
    }

    public static int getFortifyTimeoutHours(String instanceId){
        OctaneClient octaneClient = OctaneSDK.getClientByInstanceId(instanceId);
        FortifySSCFetchTimeout parameter = (FortifySSCFetchTimeout) octaneClient.getConfigurationService().getConfiguration().getParameter(FortifySSCFetchTimeout.KEY);
        if (parameter != null) {
            return parameter.getTimeout();
        }
        return FortifySSCFetchTimeout.DEFAULT_TIMEOUT;
    }

    private static Long getQueueItemTimeoutHoursFromJob(Run run) {
        Long queueItemTimeoutHours = null;
        String paramName = "fortify-maximum-analysis-timeout-hours";
        try {
            ParametersAction parameters = run.getAction(ParametersAction.class);
            if (parameters != null && parameters.getParameter(paramName) != null) {
                queueItemTimeoutHours = Long.parseLong((String) parameters.getParameter(paramName).getValue());
            }
        } catch (Exception e) {
            logger.warn("Failed to parse  " + paramName + " : " + e.getMessage());
            queueItemTimeoutHours = null;
        }
        return queueItemTimeoutHours;
    }

}
