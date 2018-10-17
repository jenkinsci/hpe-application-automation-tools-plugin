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
 * © Copyright 2012-2018 Micro Focus or one of its affiliates.
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

package com.microfocus.application.automation.tools.pc;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;

import com.microfocus.adm.performancecenter.plugins.common.pcentities.*;

import com.microfocus.adm.performancecenter.plugins.common.rest.PcRestProxy;

import static com.microfocus.adm.performancecenter.plugins.common.pcentities.RunState.*;

public class MockPcRestProxyBadResponses extends PcRestProxy {
    
    private static Iterator<RunState> runState = initializeRunStateIterator();
    
    public MockPcRestProxyBadResponses(String webProtocol, String pcServerName, String almDomain, String almProject,PrintStream logger) throws PcException {
        super(webProtocol, pcServerName, almDomain, almProject,null,null,null);
    }

    @Override
    protected HttpResponse executeRequest(HttpRequestBase request) throws PcException, ClientProtocolException,
            IOException {
        HttpResponse response = null;
        String requestUrl = request.getURI().toString();
        if (requestUrl.equals(String.format(AUTHENTICATION_LOGIN_URL, PcTestBase.WEB_PROTOCOL, PcTestBase.PC_SERVER_NAME))) {
            throw new PcException(PcTestBase.pcAuthenticationFailureMessage);
        } else if (requestUrl.equals(String.format(getBaseURL() + "/%s", RUNS_RESOURCE_NAME))){
            throw new PcException(PcTestBase.pcNoTimeslotExceptionMessage);
        } else if (requestUrl.equals(String.format(getBaseURL() + "/%s/%s", RUNS_RESOURCE_NAME, PcTestBase.RUN_ID_WAIT))) {
            response = getOkResponse();
            response.setEntity(new StringEntity(PcTestBase.runResponseEntity.replace("*", runState.next().value())));
            if (!runState.hasNext())
                runState = initializeRunStateIterator();
        } else if (requestUrl.equals(String.format(getBaseURL() + "/%s/%s/%s", RUNS_RESOURCE_NAME, PcTestBase.RUN_ID,
            RESULTS_RESOURCE_NAME))) {
            response = getOkResponse();
            response.setEntity(new StringEntity(PcTestBase.emptyResultsEntity));
        } else if (requestUrl.equals(String.format(getBaseURL() + "/%s/%s/%s", RUNS_RESOURCE_NAME, PcTestBase.RUN_ID, PcTestBase.STOP_MODE))) {
            throw new PcException(PcTestBase.pcStopNonExistRunFailureMessage);
        }
        if (response == null)
            throw new PcException(String.format("%s %s is not recognized by PC Rest Proxy", request.getMethod(), requestUrl));
        return response;
    }
    
    private HttpResponse getOkResponse(){
        
        return new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK");
    }
    
    private static Iterator<RunState> initializeRunStateIterator() {

        return Arrays.asList(INITIALIZING, RUNNING, RUN_FAILURE).iterator();
    }
}
