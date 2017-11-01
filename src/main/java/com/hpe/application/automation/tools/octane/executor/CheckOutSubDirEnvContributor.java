/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.hpe.application.automation.tools.octane.executor;

import com.hpe.application.automation.tools.octane.configuration.ConfigurationService;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentContributor;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.TaskListener;

import java.io.IOException;

/**
 * Add job environment value for CHECKOUT_SUBDIR
 */
@Extension
public class CheckOutSubDirEnvContributor extends EnvironmentContributor {

    public static final String CHECKOUT_SUBDIR_ENV_NAME = "CHECKOUT_SUBDIR";

    @Override
    public void buildEnvironmentFor(Job j, EnvVars envs, TaskListener listener) throws IOException, InterruptedException {
        String dir = getSharedCheckOutDirectory(j);
        if (dir != null) {
            envs.put(CHECKOUT_SUBDIR_ENV_NAME, dir);
        }
    }

    public static String getSharedCheckOutDirectory(Job j) {
        if (j instanceof FreeStyleProject && UftJobRecognizer.isExecutorJob((FreeStyleProject) j) && ConfigurationService.getServerConfiguration().isValid()) {
            return CheckOutSubDirEnvService.getSharedCheckOutDirectory(j);
        }

        return null;
    }

}

