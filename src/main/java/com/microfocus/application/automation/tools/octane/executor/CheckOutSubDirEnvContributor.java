/*
 *
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * © Copyright 2012-2018 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors (“Micro Focus”) are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 *
 */

package com.microfocus.application.automation.tools.octane.executor;

import com.microfocus.application.automation.tools.octane.executor.scmmanager.ScmPluginFactory;
import com.microfocus.application.automation.tools.octane.executor.scmmanager.ScmPluginHandler;
import com.microfocus.application.automation.tools.run.RunFromFileBuilder;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentContributor;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.TaskListener;
import hudson.scm.NullSCM;
import hudson.scm.SCM;
import hudson.tasks.Builder;

import java.util.List;

/**
 * Add job environment value for CHECKOUT_SUBDIR
 */
@Extension
public class CheckOutSubDirEnvContributor extends EnvironmentContributor {

    public static final String CHECKOUT_SUBDIR_ENV_NAME = "CHECKOUT_SUBDIR";

    @Override
    public void buildEnvironmentFor(Job j, EnvVars envs, TaskListener listener) {
        String dir = getSharedCheckOutDirectory(j);
        if (dir != null) {
            envs.put(CHECKOUT_SUBDIR_ENV_NAME, dir);
        }
    }

    public static String getSharedCheckOutDirectory(Job j) {
        if (j instanceof FreeStyleProject) {
            FreeStyleProject proj = (FreeStyleProject) j;
            SCM scm = proj.getScm();
            List<Builder> builders = proj.getBuilders();
            if (scm != null && !(scm instanceof NullSCM) && builders != null) {
                for (Builder builder : builders) {
                    if (builder instanceof RunFromFileBuilder) {
                        ScmPluginHandler scmPluginHandler = ScmPluginFactory.getScmHandlerByScmPluginName(scm.getClass().getName());
                        if (scmPluginHandler != null) {
                            return scmPluginHandler.getSharedCheckOutDirectory(j);
                        }
                    }
                }

            }
        }

        return null;
    }

}

