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

package com.microfocus.application.automation.tools.octane.tests;

import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public abstract class AbstractSafeLoggingAsyncPeriodWork extends AsyncPeriodicWork {

    private static Logger logger = Logger.getLogger(AbstractSafeLoggingAsyncPeriodWork.class.getName());

    protected AbstractSafeLoggingAsyncPeriodWork(String name) {
        super(name);
    }

    @Override
    protected void execute(TaskListener listener) throws IOException, InterruptedException {
        try {
            doExecute(listener);
        } catch (IOException|InterruptedException e) {
            // by default this ends up in log file and is rewritten on each execution
            // we want this in the regular Jenkins log in order to be able to troubleshoot
            logError(e);
        } catch (Throwable t) {
            // by default this ends up on the console as uncaught exception
            // we want this in the regular Jenkins log in order to be able to troubleshoot
            logError(t);
        }
    }

    protected abstract void doExecute(TaskListener listener) throws IOException, InterruptedException;

    private void logError(Throwable t) {
        LogRecord lr = new LogRecord(this.getErrorLoggingLevel(), "{0} thread failed with error");
        lr.setThrown(t);
        lr.setParameters(new Object[] { name });
        logger.log(lr);
    }

    @Override
    protected Level getNormalLoggingLevel() {
        return Level.CONFIG;
    }
}
