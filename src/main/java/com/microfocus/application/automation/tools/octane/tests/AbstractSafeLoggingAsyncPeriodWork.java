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
