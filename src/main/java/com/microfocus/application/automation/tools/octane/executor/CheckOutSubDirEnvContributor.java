/*
 * © Copyright 2013 EntIT Software LLC
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright (c) 2018 Micro Focus Company, L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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

