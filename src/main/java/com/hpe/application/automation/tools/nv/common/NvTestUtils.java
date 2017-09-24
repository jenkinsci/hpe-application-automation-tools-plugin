/*
    (c) Copyright [2016] Hewlett Packard Enterprise Development LP

    Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
    documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
    rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
    persons to whom the Software is furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
    Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
    WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
    COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
    OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.hpe.application.automation.tools.nv.common;

import com.hpe.nv.api.NVExceptions;
import com.hpe.nv.api.Test;
import com.hpe.nv.api.Transaction;
import hudson.AbortException;
import hudson.model.Run;
import hudson.model.TaskListener;
import com.hpe.application.automation.tools.nv.model.NvContext;
import com.hpe.application.automation.tools.nv.model.NvDataHolder;

import java.io.IOException;

public class NvTestUtils {
    private static final String JOB_NAME = "JOB_NAME";

    public static String getBuildKey(Run<?, ?> build) {
        return build.getCharacteristicEnvVars().get(JOB_NAME) + "_" + build.getId();
    }

    public static String getNvTestId(Run<?, ?> build) {
        return getBuildKey(build) + "_Test";
    }

    public static void stopTestEmulation(Run<?, ?> run, TaskListener listener) throws AbortException {
        NvContext nvContext = NvDataHolder.getInstance().get(getBuildKey(run));
        if (null != nvContext) {
            boolean txStopped = true;
            boolean testStopped = true;
            String errorMessage = "";

            // stop the current transaction
            Transaction transaction = nvContext.getTransaction();
            if (null != transaction) {
                try {
                    transaction.stop();
                } catch (IOException | NVExceptions.ServerErrorException e) {
                    txStopped = false;
                    e.printStackTrace(listener.getLogger());
                    errorMessage = "Failed to stop transaction. Error: " + e.getMessage() + "\n";
                }
                if (txStopped) {
                    listener.getLogger().println("Successfully stopped transaction.");
                }
            }

            // stop the current test
            Test test = nvContext.getTest();
            if (null != test) {
                try {
                    test.stop();
                    NvDataHolder.getInstance().clear(NvTestUtils.getBuildKey(run));
                } catch (IOException | NVExceptions.ServerErrorException e) {
                    testStopped = false;
                    e.printStackTrace(listener.getLogger());
                    errorMessage += "Failed to stop Network Virtualization emulation. Error: " + e.getMessage();
                }

                if (testStopped) {
                    listener.getLogger().println("Network Virtualization emulation was stopped successfully.");
                }
            }

            if (!txStopped || !testStopped) {
                throw new AbortException(errorMessage);
            }
        }
    }
}
