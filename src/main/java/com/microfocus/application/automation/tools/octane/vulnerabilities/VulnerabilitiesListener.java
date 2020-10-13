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
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.listeners.RunListener;
import org.apache.logging.log4j.Logger;

/**
 * Jenkins events life cycle listener for processing vulnerabilities scan results on build completed
 */

@Extension
@SuppressWarnings({"squid:S2699", "squid:S3658", "squid:S2259", "squid:S1872"})
public class VulnerabilitiesListener extends RunListener<AbstractBuild> {
    private static Logger logger = SDKBasedLoggerProvider.getLogger(VulnerabilitiesListener.class);

    @Override
    public void onFinalized(AbstractBuild build) {
        if (!OctaneSDK.hasClients()) {
            return;
        }

        SSCServerConfigUtil.SSCProjectVersionPair projectVersionPair = SSCServerConfigUtil.getProjectConfigurationFromBuild(build);
        if (!VulnerabilitiesUtils.insertQueueItem(build, projectVersionPair)) {
            return;
        }

        Long release = FodConfigUtil.getFODReleaseFromBuild(build);
        if (release != null) {
            logger.info("FOD configuration was found in " + build);
            VulnerabilitiesUtils.insertFODQueueItem(build, release);
        }
        if (projectVersionPair == null && release == null) {
            logger.debug("No Security Scan integration configuration was found " + build);
        }
    }
}
