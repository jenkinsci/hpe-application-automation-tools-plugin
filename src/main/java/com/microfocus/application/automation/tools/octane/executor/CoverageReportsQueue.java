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

package com.microfocus.application.automation.tools.octane.executor;

import com.microfocus.application.automation.tools.octane.AbstractResultQueueImpl;
import jenkins.model.Jenkins;

import java.io.File;
import java.io.IOException;

/**
 * Queue for jenkins coverage reports before dispatching
 */
public class CoverageReportsQueue extends AbstractResultQueueImpl {

    public CoverageReportsQueue(int maxRetries) throws IOException {
        super(maxRetries);
        Jenkins jenkinsContainer = Jenkins.getInstance();
        if (jenkinsContainer != null) {
            File queueFile = new File(jenkinsContainer.getRootDir(), "coverage-reports-queue.dat");
            init(queueFile);
        } else {
            throw new IllegalStateException("Jenkins container not initialized properly");
        }
    }

}
