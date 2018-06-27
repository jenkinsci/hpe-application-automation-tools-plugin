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

package com.microfocus.application.automation.tools.pc;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;

import com.microfocus.adm.performancecenter.plugins.common.pcEntities.*;
import com.microfocus.application.automation.tools.run.PcBuilder;

import static com.microfocus.adm.performancecenter.plugins.common.pcEntities.RunState.*;
import com.microfocus.adm.performancecenter.plugins.common.rest.PcRestProxy;

public class MockPcRestProxy extends PcRestProxy {
    
    private static Iterator<RunState> runState = initializeRunStateIterator();
    
    public MockPcRestProxy(String webProtocol, String pcServerName, String almDomain, String almProject,PrintStream logger) throws PcException {
        super(webProtocol, pcServerName, almDomain, almProject,null,null,null);
    }

    @Override
    protected HttpResponse executeRequest(HttpRequestBase request) throws PcException, ClientProtocolException,
            IOException {
        HttpResponse response = null;
        String requestUrl = request.getURI().toString();
        if (requestUrl.equals(String.format(AUTHENTICATION_LOGIN_URL, PcTestBase.WEB_PROTOCOL, PcTestBase.PC_SERVER_NAME))
                || requestUrl.equals(String.format(AUTHENTICATION_LOGOUT_URL,
                PcTestBase.WEB_PROTOCOL, PcTestBase.PC_SERVER_NAME))
                || requestUrl.equals(String.format(getBaseURL() + "/%s/%s/%s", RUNS_RESOURCE_NAME, PcTestBase.RUN_ID, PcTestBase.STOP_MODE))) {
            response = getOkResponse();
        } else if (requestUrl.equals(String.format(getBaseURL() + "/%s", RUNS_RESOURCE_NAME))
                || requestUrl.equals(String.format(getBaseURL() + "/%s/%s", RUNS_RESOURCE_NAME, PcTestBase.RUN_ID))) {
            response = getOkResponse();
            response.setEntity(new StringEntity(PcTestBase.runResponseEntity));
        } else if(requestUrl.equals(String.format(getBaseURL() + "/%s", TESTS_RESOURCE_NAME))
                || requestUrl.equals(String.format(getBaseURL() + "/%s/%s", TESTS_RESOURCE_NAME, PcTestBase.TEST_ID))){
                response = getOkResponse();
                response.setEntity(new StringEntity(PcTestBase.testResponseEntity));
        } else if (requestUrl.equals(String.format(getBaseURL() + "/%s/%s", RUNS_RESOURCE_NAME, PcTestBase.RUN_ID_WAIT))) {
            response = getOkResponse();
            response.setEntity(new StringEntity(PcTestBase.runResponseEntity.replace("*", runState.next().value())));
            if (!runState.hasNext())
                runState = initializeRunStateIterator();
        } else if (requestUrl.equals(String.format(getBaseURL() + "/%s/%s/%s", RUNS_RESOURCE_NAME, PcTestBase.RUN_ID,
            RESULTS_RESOURCE_NAME))) {
            response = getOkResponse();
            response.setEntity(new StringEntity(PcTestBase.runResultsEntity));
        } else if (requestUrl.equals(String.format(getBaseURL() + "/%s/%s/%s/%s/data", RUNS_RESOURCE_NAME, PcTestBase.RUN_ID,
            RESULTS_RESOURCE_NAME, PcTestBase.REPORT_ID))) {
            response = getOkResponse();
            response.setEntity(new FileEntity(
                new File(getClass().getResource(PcBuilder.pcReportArchiveName).getPath()), ContentType.DEFAULT_BINARY));
        }
        if (response == null)
            throw new PcException(String.format("%s %s is not recognized by PC Rest Proxy", request.getMethod(), requestUrl));
        return response;
    }
    
    private HttpResponse getOkResponse(){
        
        return new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK");
    }
    
    private static Iterator<RunState> initializeRunStateIterator(){
        
        return Arrays.asList(INITIALIZING,RUNNING,COLLATING_RESULTS,CREATING_ANALYSIS_DATA,FINISHED).iterator();
    }
}
