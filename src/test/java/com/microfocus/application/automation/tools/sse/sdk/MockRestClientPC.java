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

package com.microfocus.application.automation.tools.sse.sdk;

import java.net.HttpURLConnection;
import java.util.Map;

import com.microfocus.application.automation.tools.sse.common.RestClient4Test;

public class MockRestClientPC extends RestClient4Test {
    
    public MockRestClientPC(String url, String domain, String project, String username) {
        
        super(url, domain, project, username);
    }
    
    @Override
    public Response httpGet(String url, String queryString, Map<String, String> headers, ResourceAccessLevel resourceAccessLevel) {
        
        Response ret = new Response();
        if (url.contains("rest/is-authenticated")) {
            ret = new Response(null, getExpectAuthInfo(), null, HttpURLConnection.HTTP_OK);
        } else if (url.contains("runs/")) {
            ret.setData("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Entity Type=\"run\"><Fields><Field Name=\"pc-vusers-involved\"/><Field Name=\"pc-state\"><Value></Value></Field><Field Name=\"test-instance\"><Value>1</Value></Field><Field Name=\"execution-date\"><Value>2013-04-09</Value></Field><Field Name=\"pc-end-time\"/><Field Name=\"pc-throughput-average\"/><Field Name=\"results-statistics\"><Value></Value></Field><Field Name=\"os-config\"><Value></Value></Field><Field Name=\"async-task-id\"><Value></Value></Field><Field Name=\"run-ver-stamp\"><Value>3</Value></Field><Field Name=\"pc-total-transact-passed\"/><Field Name=\"vc-version-number\"/><Field Name=\"pc-report-url\"><Value></Value></Field><Field Name=\"cycle-id\"><Value>1</Value></Field><Field Name=\"pc-actual-post-run-action\"><Value></Value></Field><Field Name=\"cycle\"><Value></Value></Field><Field Name=\"host\"><Value></Value></Field><Field Name=\"status\"><Value>N/A</Value></Field><Field Name=\"pc-total-transact-failed\"/><Field Name=\"iters-params-values\"><Value></Value></Field><Field Name=\"test-id\"><Value>9</Value></Field><Field Name=\"sla-calculated-data\"><Value></Value></Field><Field Name=\"sub-status-progress\"><Value></Value></Field><Field Name=\"pc-run-url\"><Value>RuntimeOperations/RunStart.aspx?pcRunID=363&amp;qcRunID=42</Value></Field><Field Name=\"owner\"><Value>sa</Value></Field><Field Name=\"bpta-change-detected\"/><Field Name=\"pc-testset-name\"/><Field Name=\"bpta-change-awareness\"><Value></Value></Field><Field Name=\"execution-time\"><Value>15:52:53</Value></Field><Field Name=\"vc-locked-by\"><Value></Value></Field><Field Name=\"vuds-mode\"><Value>N</Value></Field><Field Name=\"pc-is-copied\"><Value></Value></Field><Field Name=\"os-sp\"><Value></Value></Field><Field Name=\"pc-transact-sec-average\"/><Field Name=\"pc-vusers-average\"/><Field Name=\"sub-status\"><Value></Value></Field><Field Name=\"pc-load-generators\"><Value></Value></Field><Field Name=\"state\"><Value>Run Failure</Value></Field><Field Name=\"id\"><Value>42</Value></Field><Field Name=\"pc-procedure-id\"/><Field Name=\"test-config-id\"><Value>1009</Value></Field><Field Name=\"name\"><Value>AdhocRun_2013-04-09 15:52:51</Value></Field><Field Name=\"has-linkage\"><Value>N</Value></Field><Field Name=\"path\"><Value>1_42</Value></Field><Field Name=\"vc-status\"><Value></Value></Field><Field Name=\"pinned-baseline\"><Value></Value></Field><Field Name=\"pc-hits-sec-average\"/><Field Name=\"pc-vusers-max\"/><Field Name=\"os-build\"><Value></Value></Field><Field Name=\"testcycl-id\"><Value>5</Value></Field><Field Name=\"pc-start-time\"><Value>2013-04-09 15:52:51</Value></Field><Field Name=\"temp-results-dir-path\"><Value></Value></Field><Field Name=\"pc-procedure-name\"/><Field Name=\"pc-reservation-id\"><Value>4172</Value></Field><Field Name=\"assign-rcyc\"><Value></Value></Field><Field Name=\"last-modified\"><Value>2013-04-09 15:52:53</Value></Field><Field Name=\"attachment\"><Value></Value></Field><Field Name=\"os-name\"><Value></Value></Field><Field Name=\"pc-total-errors\"/><Field Name=\"subtype-id\"><Value>hp.pc.run.performance-test</Value></Field><Field Name=\"draft\"><Value>N</Value></Field><Field Name=\"iters-sum-status\"><Value></Value></Field><Field Name=\"duration\"><Value>0</Value></Field><Field Name=\"bpt-structure\"><Value></Value></Field><Field Name=\"text-sync\"><Value></Value></Field><Field Name=\"pc-controller-name\"><Value></Value></Field><Field Name=\"comments\"><Value></Value></Field></Fields><RelatedEntities/></Entity>".getBytes());
        }
        ret.setStatusCode(200);
        
        return ret;
    }
    
    @Override
    public Response httpPost(String url, byte[] data, Map<String, String> headers, ResourceAccessLevel resourceAccessLevel) {
        
        Response ret = new Response();
        if (url.contains("startrun")) {
            ret.setData("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Entity Type=\"StartRunTestSetResponse\"><Fields><Field Name=\"SuccessStaus\"><Value>1</Value></Field><Field Name=\"info\"><Value>1005</Value></Field></Fields></Entity>".getBytes());
        }
        ret.setStatusCode(201);
        
        return ret;
    }
}
