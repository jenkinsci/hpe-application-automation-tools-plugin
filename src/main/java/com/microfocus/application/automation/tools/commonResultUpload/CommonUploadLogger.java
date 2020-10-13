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
 * © Copyright 2012-2019 Micro Focus or one of its affiliates..
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

package com.microfocus.application.automation.tools.commonResultUpload;

import com.microfocus.application.automation.tools.sse.sdk.Logger;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class CommonUploadLogger implements Logger {

    private static final String ERR_PREFIX = "ERR: ";
    private static final String INFO_PREFIX = "INFO: ";
    private static final String WARN_PREFIX = "WARN: ";

    private int errorCount;
    private List<String> failedEntityNames;
    private PrintStream printStream;

    public CommonUploadLogger(PrintStream printStream) {
        this.printStream = printStream;
        failedEntityNames = new ArrayList<>();
    }

    public void error(String message) {
        errorCount++;
        storeError(message);
        message = ERR_PREFIX + message;
        log(message);
    }

    public void info(String message) {
        message = INFO_PREFIX + message;
        log(message);
    }

    public void warn(String message) {
        message = WARN_PREFIX + message;
        log(message);
    }

    @Override
    public void log(String message) {
        if (printStream != null) {
            printStream.println(message);
        }
    }

    public List<String> getFailedEntityNames() {
        return failedEntityNames;
    }

    public int getErrorCount() {
        return errorCount;
    }

    private void storeError(String message) {
        if (message.indexOf('(') > -1 && message.indexOf(')') > message.indexOf('(')) {
            failedEntityNames.add(
                    message.substring(message.indexOf('(') + 1, message.indexOf(')'))
            );
        }
    }
}
