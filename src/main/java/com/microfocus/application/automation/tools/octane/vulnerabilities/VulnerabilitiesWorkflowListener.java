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

import com.hp.octane.integrations.OctaneSDK;
import com.microfocus.application.automation.tools.octane.configuration.FodConfigUtil;
import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import com.microfocus.application.automation.tools.octane.configuration.SSCServerConfigUtil;
import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import hudson.Extension;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.plugins.workflow.flow.GraphListener;
import org.jenkinsci.plugins.workflow.graph.FlowEndNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.lang.reflect.InvocationTargetException;

/**
 * Jenkins events life cycle listener for processing vulnerabilities scan results on build completed
 */

@Extension
public class VulnerabilitiesWorkflowListener implements GraphListener {
    private static final Logger logger = SDKBasedLoggerProvider.getLogger(VulnerabilitiesWorkflowListener.class);

    @Override
    public void onNewHead(FlowNode flowNode) {
        if (!OctaneSDK.hasClients()) {
            return;
        }
        try {
            if (BuildHandlerUtils.isWorkflowEndNode(flowNode)) {
                sendPipelineFinishedEvent((FlowEndNode) flowNode);
            }
        } catch (Exception e) {
            logger.error("failed to build and/or dispatch STARTED/FINISHED event for " + flowNode, e);
        }
    }

    protected void sendPipelineFinishedEvent(FlowEndNode flowEndNode) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        WorkflowRun parentRun = BuildHandlerUtils.extractParentRun(flowEndNode);

        if (!OctaneSDK.hasClients()) {
            return;
        }

        SSCServerConfigUtil.SSCProjectVersionPair projectVersionPair = SSCServerConfigUtil.getProjectConfigurationFromWorkflowRun(parentRun);
        if (!VulnerabilitiesUtils.insertQueueItem(parentRun, projectVersionPair)) return;

        Long release = FodConfigUtil.getFODReleaseFromRun(parentRun);
        if (release != null) {
            logger.info("FOD configuration was found in " + parentRun);
            VulnerabilitiesUtils.insertFODQueueItem(parentRun, release);
        }
        if (projectVersionPair == null && release == null) {
            logger.debug("No Security Scan integration configuration was found " + parentRun);
        }
    }
}
