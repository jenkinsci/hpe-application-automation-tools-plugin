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

import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.extensions.impl.RelativeTargetDirectory;
import hudson.scm.SCM;

/**
 * Compute SharedCheckOutDirectory
 */

public class CheckOutSubDirEnvService {


    public static String getSharedCheckOutDirectory(Job j) {
        SCM scm = ((FreeStyleProject) j).getScm();
        if (scm != null && scm instanceof GitSCM) {
            GitSCM gitScm = (GitSCM) scm;
            RelativeTargetDirectory sharedCheckOutDirectory = gitScm.getExtensions().get(RelativeTargetDirectory.class);
            if (sharedCheckOutDirectory != null) {
                return sharedCheckOutDirectory.getRelativeTargetDir();
            }
        }

        return null;
    }

}

