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
        SCMUtils.persistSCMData(run, jobCiId, buildCiId, scmData);

        if (scmData != null) {
            OctaneSDK.getClients().forEach(octaneClient ->
                    octaneClient.getSCMDataService().enqueueSCMData(jobCiId, buildCiId, scmData));
        }
    }
}
