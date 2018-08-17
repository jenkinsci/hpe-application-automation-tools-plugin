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

package com.microfocus.application.automation.tools.sse.sdk;

import java.net.HttpURLConnection;
import java.util.Map;

import com.microfocus.application.automation.tools.sse.common.RestClient4Test;

public class MockRestClientBadDomain extends RestClient4Test {
    
    public MockRestClientBadDomain(String url, String domain, String project, String username) {
        
        super(url, domain, project, username);
    }
    
    @Override
    public Response httpGet(String url, String queryString, Map<String, String> headers, ResourceAccessLevel resourceAccessLevel) {
        
        Response ret = new Response();
        if (url.contains("rest/is-authenticated")) {
            ret = new Response(null, null, null, HttpURLConnection.HTTP_OK);
        } else if (url.contains("procedure-runs/")) {
            ret.setData("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Entity Type=\"procedure-run\"><Fields><Field Name=\"end-time\"><Value>2013-02-13 14:32:35</Value></Field><Field Name=\"topology-id\"><Value></Value></Field><Field Name=\"status\"><Value></Value></Field><Field Name=\"topology-name\"><Value></Value></Field><Field Name=\"run-type\"><Value>TestSet-Run</Value></Field><Field Name=\"state\"><Value>Finished</Value></Field><Field Name=\"start-time\"><Value>2013-02-13 14:31:57</Value></Field><Field Name=\"reservation-id\"><Value>1008</Value></Field><Field Name=\"is-closing\"><Value></Value></Field><Field Name=\"vts\"><Value>2013-02-13 14:32:35</Value></Field><Field Name=\"planner-time-lock\"><Value>2013-02-10 03:12:35</Value></Field><Field Name=\"user-name\"><Value>sa</Value></Field><Field Name=\"id\"><Value>1008</Value></Field><Field Name=\"test-set-ids\"><Value>1</Value></Field><Field Name=\"parent-id\"><Value>1008</Value></Field><Field Name=\"ver-stamp\"><Value>4</Value></Field><Field Name=\"completed-successfully\"><Value>Y</Value></Field><Field Name=\"name\"><Value>testset1_105bc9c9-466e-48c2-9282-3d17fa3cdf7e</Value></Field><Field Name=\"user-id\"><Value>251</Value></Field><Field Name=\"test-set-names\"><Value>testset1</Value></Field><Field Name=\"step\"><Value>Deprovisioning</Value></Field></Fields><RelatedEntities/></Entity>".getBytes());
        } else if (url.contains("event-log-reads")) {
            ret.setData("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Entities TotalResults=\"4\"><Entity Type=\"event-log-read\"><Fields><Field Name=\"id\"><Value>228</Value></Field><Field Name=\"description\"><Value>Timeslot ID '1008' was created successfully</Value></Field><Field Name=\"action\"><Value>Create Timeslot</Value></Field><Field Name=\"creation-time\"><Value>2013-02-13 14:31:55</Value></Field><Field Name=\"event-type\"><Value>Info</Value></Field></Fields><RelatedEntities/></Entity><Entity Type=\"event-log-read\"><Fields><Field Name=\"id\"><Value>230</Value></Field><Field Name=\"description\"><Value>Creating run-time data for run '1008' of 'Test Set' '1' (Timeslot ID '1008'; BVS ID '1008')</Value></Field><Field Name=\"action\"><Value>Create run-time data</Value></Field><Field Name=\"creation-time\"><Value>2013-02-13 14:31:58</Value></Field><Field Name=\"event-type\"><Value>Info</Value></Field></Fields><RelatedEntities/></Entity><Entity Type=\"event-log-read\"><Fields><Field Name=\"id\"><Value>231</Value></Field><Field Name=\"description\"><Value>TestSet ID: '1008' start time: '2013-02-13 14:31:59.022'</Value></Field><Field Name=\"action\"><Value>BVS Run</Value></Field><Field Name=\"creation-time\"><Value>2013-02-13 14:31:59</Value></Field><Field Name=\"event-type\"><Value>Info</Value></Field></Fields><RelatedEntities/></Entity><Entity Type=\"event-log-read\"><Fields><Field Name=\"id\"><Value>234</Value></Field><Field Name=\"description\"><Value>Timeslot ID '1008' was closed</Value></Field><Field Name=\"action\"><Value>Close Timeslot</Value></Field><Field Name=\"creation-time\"><Value>2013-02-13 14:32:36</Value></Field><Field Name=\"event-type\"><Value>Info</Value></Field></Fields><RelatedEntities/></Entity></Entities>".getBytes());
        } else if (url.contains("procedure-testset-instance-runs")) {
            ret.setData("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Entities TotalResults=\"1\"><Entity Type=\"procedure-testset-instance-run\"><Fields><Field Name=\"run-id\"><Value>7</Value></Field><Field Name=\"location\"><Value></Value></Field><Field Name=\"state\"><Value>Finished</Value></Field><Field Name=\"purpose\"><Value>VAPI-XP</Value></Field><Field Name=\"on-failure-settings\"><Value></Value></Field><Field Name=\"exec-progress-details\"><Value>Passed</Value></Field><Field Name=\"id\"><Value>1008</Value></Field><Field Name=\"start-exec-date\"><Value>2013-02-13</Value></Field><Field Name=\"ver-stamp\"/><Field Name=\"host-name\"><Value>Any</Value></Field><Field Name=\"order\"><Value>1</Value></Field><Field Name=\"test-config-name\"><Value>vapi1</Value></Field><Field Name=\"testset-name\"><Value>testset1</Value></Field><Field Name=\"testcycl-id\"><Value>1</Value></Field><Field Name=\"host\"><Value>schreida1</Value></Field><Field Name=\"status\"><Value>Passed</Value></Field><Field Name=\"testing-host\"><Value>Host with purpose(s): VAPI-XP</Value></Field><Field Name=\"host-demand\"><Value></Value></Field><Field Name=\"test-subtype\"><Value>hp.qc.test-instance.VAPI-XP-TEST</Value></Field><Field Name=\"test-name\"><Value>vapi1</Value></Field><Field Name=\"vts\"/><Field Name=\"test-run-try\"><Value></Value></Field><Field Name=\"start-exec-time\"><Value>14:32:28</Value></Field><Field Name=\"parent-id\"><Value>1008</Value></Field><Field Name=\"procedure-run\"><Value>1008</Value></Field><Field Name=\"duration\"><Value>0</Value></Field><Field Name=\"attributes\"><Value></Value></Field><Field Name=\"exec-progress\"><Value>Finished</Value></Field></Fields><RelatedEntities/></Entity></Entities>".getBytes());
        } else if (url.contains("test-sets/")) {
            ret.setData("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Entity Type=\"test-set\"><Fields><Field Name=\"close-date\"/><Field Name=\"request-id\"/><Field Name=\"mail-settings\"><Value></Value></Field><Field Name=\"pc-is-valid\"><Value>Y</Value></Field><Field Name=\"id\"><Value>1</Value></Field><Field Name=\"os-config\"><Value></Value></Field><Field Name=\"pc-validation-result-xml\"><Value>&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;&lt;reasons/&gt;</Value></Field><Field Name=\"description\"><Value></Value></Field><Field Name=\"name\"><Value>testset1</Value></Field><Field Name=\"has-linkage\"><Value>N</Value></Field><Field Name=\"cycle-ver-stamp\"><Value>2</Value></Field><Field Name=\"pinned-baseline\"><Value></Value></Field><Field Name=\"report-settings\"><Value></Value></Field><Field Name=\"last-modified\"><Value>2013-02-13 12:47:11</Value></Field><Field Name=\"assign-rcyc\"><Value></Value></Field><Field Name=\"status\"><Value>Open</Value></Field><Field Name=\"cycle-config\"><Value></Value></Field><Field Name=\"exec-event-handle\"><Value></Value></Field><Field Name=\"open-date\"><Value>2013-02-13</Value></Field><Field Name=\"attachment\"><Value></Value></Field><Field Name=\"subtype-id\"><Value>hp.sse.test-set.process</Value></Field><Field Name=\"pc-is-demand-customized\"><Value></Value></Field><Field Name=\"parent-id\"><Value>1</Value></Field><Field Name=\"comment\"><Value></Value></Field></Fields><RelatedEntities/></Entity>".getBytes());
        } else if (url.contains("procedures/")) {
            ret.setData("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Entity Type=\"Procedure\"><Fields><Field Name=\"is-valid\"><Value>Y</Value></Field><Field Name=\"creation-date\"><Value>2013-02-13</Value></Field><Field Name=\"created-by\"><Value>sa</Value></Field><Field Name=\"modified-by\"><Value>sa</Value></Field><Field Name=\"vts\"><Value>2013-02-13 14:49:51</Value></Field><Field Name=\"id\"><Value>1010</Value></Field><Field Name=\"parent-id\"><Value>1</Value></Field><Field Name=\"ver-stamp\"><Value>12</Value></Field><Field Name=\"is-autogenerated\"><Value>N</Value></Field><Field Name=\"description\"><Value></Value></Field><Field Name=\"name\"><Value>bvs1</Value></Field><Field Name=\"is-demand-customized\"><Value>N</Value></Field><Field Name=\"validation-result-xml\"><Value>&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;&lt;reasons/&gt;</Value></Field></Fields><RelatedEntities/></Entity>".getBytes());
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
