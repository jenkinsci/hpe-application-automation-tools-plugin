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

package com.hpe.application.automation.tools.nv.plugin;

import hudson.AbortException;
import hudson.Extension;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import com.hpe.application.automation.tools.nv.common.NvTestUtils;
import com.hpe.application.automation.tools.nv.model.NvDataHolder;

import javax.annotation.Nonnull;

@Extension
public class NvEmulationRunListener<R extends Run> extends RunListener<R> {
    @Override
    public void onCompleted(R r, @Nonnull TaskListener listener) {
        super.onCompleted(r, listener);

        try {
            // stop emulation in case of failure
            if (r.getResult().isWorseOrEqualTo(Result.FAILURE)) {
                if (null != NvDataHolder.getInstance().get(NvTestUtils.getBuildKey(r))) {
                    listener.getLogger().println("Build was not completed. Stopping Network Virtualization emulation.");

                    NvTestUtils.stopTestEmulation(r, listener);
                }
            }
        } catch (AbortException e) {
            // do nothing
        }
    }
}
