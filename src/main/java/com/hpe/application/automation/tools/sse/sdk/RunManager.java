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

package com.hpe.application.automation.tools.sse.sdk;

import com.hpe.application.automation.tools.common.SSEException;
import com.hpe.application.automation.tools.rest.RestClient;
import com.hpe.application.automation.tools.sse.common.StringUtils;
import com.hpe.application.automation.tools.sse.result.PublisherFactory;
import com.hpe.application.automation.tools.sse.result.model.junit.Testsuites;
import com.hpe.application.automation.tools.sse.sdk.authenticator.AuthenticationTool;
import com.hpe.application.automation.tools.sse.sdk.handler.PollHandler;
import com.hpe.application.automation.tools.sse.sdk.handler.PollHandlerFactory;
import com.hpe.application.automation.tools.sse.sdk.handler.RunHandler;
import com.hpe.application.automation.tools.sse.sdk.handler.RunHandlerFactory;

/**
 * @author Effi Bar-She'an
 * @author Dani Schreiber
 */
public class RunManager {

    private RunHandler _runHandler;
    private PollHandler _pollHandler;
    private Logger _logger;
    private boolean _running = false;
    private boolean _polling = false;

    /**
     * Execute
     */
    public Testsuites execute(RestClient client, Args args, Logger logger)
            throws InterruptedException {
        Testsuites ret = null;
        _logger = logger;
        _running = true;
        if (AuthenticationTool.authenticate(client, args.getUsername(), args.getPassword(), args.getUrl(), args.getClientType(), logger)) {
            initialize(args, client);
            if (start(args)) {
                _polling = true;
                if (poll()) {
                    ret =
                            new PublisherFactory().create(
                                    client,
                                    args.getRunType(),
                                    args.getEntityId(),
                                    _runHandler.getRunId()).publish(
                                    _runHandler.getNameSuffix(),
                                    args.getUrl(),
                                    args.getDomain(),
                                    args.getProject(),
                                    logger);
                }
                _polling = false;
            }
        }
        return ret;
    }

    /**
     * Initialize
     */
    private void initialize(Args args, RestClient client) {
        String entityId = args.getEntityId();
        _runHandler = new RunHandlerFactory().create(client, args.getRunType(), entityId);
        _pollHandler = new PollHandlerFactory().create(client, args.getRunType(), entityId);
    }

    /**
     * Poll
     */
    private boolean poll() throws InterruptedException {
        return _pollHandler.poll(_logger);
    }

    /**
     * Stop
     */
    public void stop() {
        _logger.log("Stopping run...");
        if (_runHandler != null) {
            _runHandler.stop();
            _running = false;
        }
        if (_pollHandler != null) {
            _polling = false;
        }
    }

    /**
     * Start
     */
    private boolean start(Args args) {
        boolean ret = false;
        Response response =
                _runHandler.start(
                        args.getDuration(),
                        args.getPostRunAction(),
                        args.getEnvironmentConfigurationId(),
                        args.getCdaDetails());
        if (isOk(response, args)) {
            RunResponse runResponse = getRunResponse(response);
            setRunId(runResponse);
            if (runResponse.isSucceeded()) {
                ret = true;
            }
        }
        logReportUrl(ret, args);
        return ret;
    }

    /**
     * Set Run id
     */
    private void setRunId(RunResponse runResponse) {
        String runId = runResponse.getRunId();
        if (StringUtils.isNullOrEmpty(runId)) {
            _logger.log("No run ID");
            throw new SSEException("No run ID");
        } else {
            _runHandler.setRunId(runId);
            _pollHandler.setRunId(runId);
        }
    }

    /**
     * Log report url
     */
    private void logReportUrl(boolean isSucceeded, Args args) {
        if (isSucceeded) {
            _logger.log(String.format(
                    "%s run report for run id %s is at: %s",
                    args.getRunType(),
                    _runHandler.getRunId(),
                    _runHandler.getReportUrl(args)));
        } else {
            _logger.log(String.format(
                    "Failed to start %s ID:%s, run id: %s "
                            + "\nNote: You can run only functional test sets and build verification suites using this plugin. "
                            + "Check to make sure that the configured ID is valid "
                            + "(and that it is not a performance test ID).",
                    args.getRunType(),
                    args.getEntityId(),
                    _runHandler.getRunId()));
        }
    }

    /**
     * Get run response
     */
    private RunResponse getRunResponse(Response response) {
        return _runHandler.getRunResponse(response);
    }

    /**
     * Is response ok
     */
    private boolean isOk(Response response, Args args) {
        boolean ret = false;
        if (response.isOk()) {
            _logger.log(String.format(
                    "Executing %s ID: %s in %s/%s %sDescription: %s",
                    args.getRunType(),
                    args.getEntityId(),
                    args.getDomain(),
                    args.getProject(),
                    StringUtils.NEW_LINE,
                    args.getDescription()));
            ret = true;
        } else {
            Throwable cause = response.getFailure();
            if (cause != null) {
                _logger.log(String.format(
                        "Failed to start %s ID: %s, ALM Server URL: %s (Exception: %s)",
                        args.getRunType(),
                        args.getEntityId(),
                        args.getUrl(),
                        cause.getMessage()));
            } else {
                _logger.log(String.format(
                        "Failed to execute %s ID: %s, ALM Server URL: %s (Response: %s)",
                        args.getRunType(),
                        args.getEntityId(),
                        args.getUrl(),
                        response.getStatusCode()));
            }
        }
        return ret;
    }

    /**
     * Get running
     */
    public boolean getRunning() {
        return _running;
    }

    /**
     * Get polling
     */
    public boolean getPolling() {
        return _polling;
    }

}