/*
 *  Certain versions of software accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 *  This software was acquired by Micro Focus on September 1, 2017, and is now
 *  offered by OpenText.
 *  Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical
 *  in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the
 *  property of their respective owners.
 *  OpenText is a trademark of Open Text.
 *  __________________________________________________________________
 *  MIT License
 *
 *  Copyright 2012-2025 Open Text.
 *
 *  The only warranties for products and services of Open Text and
 *  its affiliates and licensors ("Open Text") are as may be set forth
 *  in the express warranty statements accompanying such products and services.
 *  Nothing herein should be construed as constituting an additional warranty.
 *  Open Text shall not be liable for technical or editorial errors or
 *  omissions contained herein. The information contained herein is subject
 *  to change without notice.
 *
 *  Except as specifically indicated otherwise, this document contains
 *  confidential information and a valid license is required for possession,
 *  use or copying. If this work is provided to the U.S. Government,
 *  consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 *  Computer Software Documentation, and Technical Data for Commercial Items are
 *  licensed to the U.S. Government under vendor's standard commercial license.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ___________________________________________________________________
 */
package com.microfocus.application.automation.tools.sse.sdk.handler;

import com.microfocus.application.automation.tools.sse.sdk.Client;
import com.microfocus.application.automation.tools.sse.sdk.Logger;
import com.microfocus.application.automation.tools.sse.sdk.Response;

public abstract class PollHandler extends Handler {
    
    private int _interval = 5000; // millisecond
    
    public PollHandler(Client client, String entityId) {
        
        super(client, entityId);
    }
    
    public PollHandler(Client client, String entityId, int interval) {
        
        super(client, entityId);
        _interval = interval;
    }
    
    public PollHandler(Client client, String entityId, String runId) {
        
        super(client, entityId, runId);
    }

    public boolean poll(Logger logger) throws InterruptedException {

        logger.log(String.format("Polling... Run ID: %s", _runId));

        return doPoll(logger);
    }
    
    protected boolean doPoll(Logger logger) throws InterruptedException {
        boolean ret = false;
        int failures = 0;

        while (failures < 3) {
            Response response = getResponse();
            if (isOk(response, logger)) {
                log(logger);
                if (isFinished(response, logger)) {
                    ret = true;
                    logRunEntityResults(getRunEntityResultsResponse(), logger);
                    break;
                }
            } else {
                ++failures;
            }
            if (sleep(logger)) { // interrupted
                break;
            }
        }
        
        return ret;
    }
    
    protected abstract Response getRunEntityResultsResponse();
    
    protected abstract boolean logRunEntityResults(Response response, Logger logger);
    
    protected abstract boolean isFinished(Response response, Logger logger);
    
    protected abstract Response getResponse();
    
    protected boolean isOk(Response response, Logger logger) {
        
        boolean ret = false;
        if (!response.isOk()) {
            Throwable cause = response.getFailure();
            logger.log(String.format(
                    "Polling try failed. Status code: %s, Exception: %s",
                    response.getStatusCode(),
                    cause != null ? cause.getMessage() : "Not Available"));
        } else {
            ret = true;
        }
        
        return ret;
    }
    
    protected boolean sleep(Logger logger) throws InterruptedException {
        
        boolean ret = false;
        try {
            Thread.sleep(_interval);
        } catch (InterruptedException ex) {
            logger.log("Interrupted while polling");
            throw ex;
        }
        
        return ret;
    }
    
    protected void log(Logger logger) {}
}
