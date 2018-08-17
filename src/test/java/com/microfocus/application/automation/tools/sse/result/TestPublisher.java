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

package com.microfocus.application.automation.tools.sse.result;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

import com.microfocus.application.automation.tools.sse.common.TestCase;
import com.microfocus.application.automation.tools.sse.result.model.junit.JUnitTestCaseStatus;
import com.microfocus.application.automation.tools.sse.result.model.junit.Testcase;
import com.microfocus.application.automation.tools.sse.result.model.junit.Testsuite;
import com.microfocus.application.automation.tools.sse.result.model.junit.Testsuites;
import com.microfocus.application.automation.tools.sse.sdk.Client;
import com.microfocus.application.automation.tools.sse.sdk.ResourceAccessLevel;
import com.microfocus.application.automation.tools.sse.sdk.Response;
import org.junit.Assert;
import org.junit.Test;

import com.microfocus.application.automation.tools.sse.common.ConsoleLogger;
import com.microfocus.application.automation.tools.sse.common.RestClient4Test;

@SuppressWarnings("squid:S2699")
public class TestPublisher extends TestCase {

    @Test
    public void testSomeErrorsPublish() {

        byte[] bytes =
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Entities TotalResults=\"6\"><Entity Type=\"procedure-testset-instance-run\"><Fields><Field Name=\"run-id\"/><Field Name=\"location\"><Value></Value></Field><Field Name=\"state\"><Value>No Run</Value></Field><Field Name=\"purpose\"><Value>System Test</Value></Field><Field Name=\"on-failure-settings\"><Value></Value></Field><Field Name=\"exec-progress-details\"><Value></Value></Field><Field Name=\"id\"><Value>2647</Value></Field><Field Name=\"start-exec-date\"/><Field Name=\"ver-stamp\"/><Field Name=\"host-name\"><Value>Any</Value></Field><Field Name=\"order\"><Value>3</Value></Field><Field Name=\"test-config-name\"><Value>log in to dev-petclinic.com</Value></Field><Field Name=\"testset-name\"><Value>PetClinic Dev Nightly Build</Value></Field><Field Name=\"testcycl-id\"><Value>1</Value></Field><Field Name=\"host\"><Value></Value></Field><Field Name=\"status\"><Value>Passed</Value></Field><Field Name=\"testing-host\"><Value>Host with purpose(s): System Test</Value></Field><Field Name=\"host-demand\"><Value></Value></Field><Field Name=\"test-subtype\"><Value>hp.qc.test-instance.SYSTEM-TEST</Value></Field><Field Name=\"test-name\"><Value>log in to dev-petclinic.com</Value></Field><Field Name=\"vts\"/><Field Name=\"test-run-try\"><Value></Value></Field><Field Name=\"start-exec-time\"><Value></Value></Field><Field Name=\"parent-id\"><Value>1523</Value></Field><Field Name=\"procedure-run\"><Value>1310</Value></Field><Field Name=\"duration\"><Value>3.5</Value></Field><Field Name=\"attributes\"><Value></Value></Field><Field Name=\"exec-progress\"><Value>No Run</Value></Field></Fields><RelatedEntities/></Entity><Entity Type=\"procedure-testset-instance-run\"><Fields><Field Name=\"run-id\"/><Field Name=\"location\"><Value></Value></Field><Field Name=\"state\"><Value>No Run</Value></Field><Field Name=\"purpose\"><Value>System Test</Value></Field><Field Name=\"on-failure-settings\"><Value></Value></Field><Field Name=\"exec-progress-details\"><Value></Value></Field><Field Name=\"id\"><Value>2648</Value></Field><Field Name=\"start-exec-date\"/><Field Name=\"ver-stamp\"/><Field Name=\"host-name\"><Value>Any</Value></Field><Field Name=\"order\"><Value>4</Value></Field><Field Name=\"test-config-name\"><Value>place dog collars in shopping cart</Value></Field><Field Name=\"testset-name\"><Value>PetClinic Dev Nightly Build</Value></Field><Field Name=\"testcycl-id\"><Value>2</Value></Field><Field Name=\"host\"><Value></Value></Field><Field Name=\"status\"><Value>No Run</Value></Field><Field Name=\"testing-host\"><Value>Host with purpose(s): System Test</Value></Field><Field Name=\"host-demand\"><Value></Value></Field><Field Name=\"test-subtype\"><Value>hp.qc.test-instance.SYSTEM-TEST</Value></Field><Field Name=\"test-name\"><Value>place dog collars in shopping cart</Value></Field><Field Name=\"vts\"/><Field Name=\"test-run-try\"><Value></Value></Field><Field Name=\"start-exec-time\"><Value></Value></Field><Field Name=\"parent-id\"><Value>1523</Value></Field><Field Name=\"procedure-run\"><Value>1310</Value></Field><Field Name=\"duration\"/><Field Name=\"attributes\"><Value></Value></Field><Field Name=\"exec-progress\"><Value>No Run</Value></Field></Fields><RelatedEntities/></Entity><Entity Type=\"procedure-testset-instance-run\"><Fields><Field Name=\"run-id\"/><Field Name=\"location\"><Value></Value></Field><Field Name=\"state\"><Value>No Run</Value></Field><Field Name=\"purpose\"><Value>System Test</Value></Field><Field Name=\"on-failure-settings\"><Value></Value></Field><Field Name=\"exec-progress-details\"><Value></Value></Field><Field Name=\"id\"><Value>2649</Value></Field><Field Name=\"start-exec-date\"/><Field Name=\"ver-stamp\"/><Field Name=\"host-name\"><Value>Any</Value></Field><Field Name=\"order\"><Value>1</Value></Field><Field Name=\"test-config-name\"><Value>checkout</Value></Field><Field Name=\"testset-name\"><Value>PetClinic Dev Nightly Build</Value></Field><Field Name=\"testcycl-id\"><Value>4</Value></Field><Field Name=\"host\"><Value></Value></Field><Field Name=\"status\"><Value>No Run</Value></Field><Field Name=\"testing-host\"><Value>Host with purpose(s): System Test</Value></Field><Field Name=\"host-demand\"><Value></Value></Field><Field Name=\"test-subtype\"><Value>hp.qc.test-instance.SYSTEM-TEST</Value></Field><Field Name=\"test-name\"><Value>checkout</Value></Field><Field Name=\"vts\"/><Field Name=\"test-run-try\"><Value></Value></Field><Field Name=\"start-exec-time\"><Value></Value></Field><Field Name=\"parent-id\"><Value>1523</Value></Field><Field Name=\"procedure-run\"><Value>1310</Value></Field><Field Name=\"duration\"/><Field Name=\"attributes\"><Value></Value></Field><Field Name=\"exec-progress\"><Value>No Run</Value></Field></Fields><RelatedEntities/></Entity><Entity Type=\"procedure-testset-instance-run\"><Fields><Field Name=\"run-id\"/><Field Name=\"location\"><Value></Value></Field><Field Name=\"state\"><Value>No Run</Value></Field><Field Name=\"purpose\"><Value>System Test</Value></Field><Field Name=\"on-failure-settings\"><Value></Value></Field><Field Name=\"exec-progress-details\"><Value></Value></Field><Field Name=\"id\"><Value>2650</Value></Field><Field Name=\"start-exec-date\"/><Field Name=\"ver-stamp\"/><Field Name=\"host-name\"><Value>Any</Value></Field><Field Name=\"order\"><Value>1</Value></Field><Field Name=\"test-config-name\"><Value>checkout</Value></Field><Field Name=\"testset-name\"><Value>PetClinic Dev Daily Build</Value></Field><Field Name=\"testcycl-id\"><Value>6</Value></Field><Field Name=\"host\"><Value></Value></Field><Field Name=\"status\"><Value>No Run</Value></Field><Field Name=\"testing-host\"><Value>Host with purpose(s): System Test</Value></Field><Field Name=\"host-demand\"><Value></Value></Field><Field Name=\"test-subtype\"><Value>hp.qc.test-instance.SYSTEM-TEST</Value></Field><Field Name=\"test-name\"><Value>checkout</Value></Field><Field Name=\"vts\"/><Field Name=\"test-run-try\"><Value></Value></Field><Field Name=\"start-exec-time\"><Value></Value></Field><Field Name=\"parent-id\"><Value>1522</Value></Field><Field Name=\"procedure-run\"><Value>1310</Value></Field><Field Name=\"duration\"/><Field Name=\"attributes\"><Value></Value></Field><Field Name=\"exec-progress\"><Value>No Run</Value></Field></Fields><RelatedEntities/></Entity><Entity Type=\"procedure-testset-instance-run\"><Fields><Field Name=\"run-id\"/><Field Name=\"location\"><Value></Value></Field><Field Name=\"state\"><Value>No Run</Value></Field><Field Name=\"purpose\"><Value>System Test</Value></Field><Field Name=\"on-failure-settings\"><Value></Value></Field><Field Name=\"exec-progress-details\"><Value></Value></Field><Field Name=\"id\"><Value>2651</Value></Field><Field Name=\"start-exec-date\"/><Field Name=\"ver-stamp\"/><Field Name=\"host-name\"><Value>Any</Value></Field><Field Name=\"order\"><Value>2</Value></Field><Field Name=\"test-config-name\"><Value>go to wish list</Value></Field><Field Name=\"testset-name\"><Value>PetClinic Dev Daily Build</Value></Field><Field Name=\"testcycl-id\"><Value>5</Value></Field><Field Name=\"host\"><Value></Value></Field><Field Name=\"status\"><Value>No Run</Value></Field><Field Name=\"testing-host\"><Value>Host with purpose(s): System Test</Value></Field><Field Name=\"host-demand\"><Value></Value></Field><Field Name=\"test-subtype\"><Value>hp.qc.test-instance.SYSTEM-TEST</Value></Field><Field Name=\"test-name\"><Value>go to wish list</Value></Field><Field Name=\"vts\"/><Field Name=\"test-run-try\"><Value></Value></Field><Field Name=\"start-exec-time\"><Value></Value></Field><Field Name=\"parent-id\"><Value>1522</Value></Field><Field Name=\"procedure-run\"><Value>1310</Value></Field><Field Name=\"duration\"/><Field Name=\"attributes\"><Value></Value></Field><Field Name=\"exec-progress\"><Value>No Run</Value></Field></Fields><RelatedEntities/></Entity><Entity Type=\"procedure-testset-instance-run\"><Fields><Field Name=\"run-id\"/><Field Name=\"location\"><Value></Value></Field><Field Name=\"state\"><Value>No Run</Value></Field><Field Name=\"purpose\"><Value>System Test</Value></Field><Field Name=\"on-failure-settings\"><Value></Value></Field><Field Name=\"exec-progress-details\"><Value></Value></Field><Field Name=\"id\"><Value>2652</Value></Field><Field Name=\"start-exec-date\"/><Field Name=\"ver-stamp\"/><Field Name=\"host-name\"><Value>Any</Value></Field><Field Name=\"order\"><Value>2</Value></Field><Field Name=\"test-config-name\"><Value>go to wish list</Value></Field><Field Name=\"testset-name\"><Value>PetClinic Dev Nightly Build</Value></Field><Field Name=\"testcycl-id\"><Value>3</Value></Field><Field Name=\"host\"><Value></Value></Field><Field Name=\"status\"><Value>No Run</Value></Field><Field Name=\"testing-host\"><Value>Host with purpose(s): System Test</Value></Field><Field Name=\"host-demand\"><Value></Value></Field><Field Name=\"test-subtype\"><Value>hp.qc.test-instance.SYSTEM-TEST</Value></Field><Field Name=\"test-name\"><Value>go to wish list</Value></Field><Field Name=\"vts\"/><Field Name=\"test-run-try\"><Value></Value></Field><Field Name=\"start-exec-time\"><Value></Value></Field><Field Name=\"parent-id\"><Value>1523</Value></Field><Field Name=\"procedure-run\"><Value>1310</Value></Field><Field Name=\"duration\"/><Field Name=\"attributes\"><Value></Value></Field><Field Name=\"exec-progress\"><Value>No Run</Value></Field></Fields><RelatedEntities/></Entity></Entities>".getBytes();
        Client client = new MockClient(URL, DOMAIN, PROJECT, bytes, USER);
        Testsuites testsuites =
                new PublisherFactory().create(client, "BVS", "1310", "1001").publish(
                        "",
                        URL,
                        DOMAIN,
                        PROJECT,
                        new ConsoleLogger());

        Assert.assertTrue(containsStatus(
                testsuites.getTestsuite(),
                JUnitTestCaseStatus.ERROR));
        Assert.assertNotNull(testsuites);
    }

    @Test
    public void testEmptyResults() {

        byte[] bytes = "".getBytes();
        Client client = new MockClient(URL, DOMAIN, PROJECT, bytes, USER);
        Testsuites testsuites = publish(client);

        Assert.assertNull(testsuites);
    }

    @Test
    public void testNonEmptyInvalidResults() {

        byte[] bytes = "garbage".getBytes();
        Client client = new MockClient(URL, DOMAIN, PROJECT, bytes, USER);

        Testsuites testsuites =
                new PublisherFactory().create(client, "BVS", "1310", "1001").publish(
                        "",
                        URL,
                        DOMAIN,
                        PROJECT,
                        new ConsoleLogger());
        Assert.assertNull(testsuites);
    }

    @Test
    public void testAllPassedPublish() {

        byte[] bytes =
                "<Entities TotalResults=\"1\"><Entity Type=\"procedure-testset-instance-run\"><Fields><Field Name=\"run-id\"><Value>31</Value></Field><Field Name=\"location\"><Value/></Field><Field Name=\"state\"><Value>Finished</Value></Field><Field Name=\"purpose\"><Value>VAPI-XP</Value></Field><Field Name=\"on-failure-settings\"><Value/></Field><Field Name=\"exec-progress-details\"><Value>Passed</Value></Field><Field Name=\"id\"><Value>3238</Value></Field><Field Name=\"start-exec-date\"><Value>2013-01-31</Value></Field><Field Name=\"ver-stamp\"/><Field Name=\"host-name\"><Value>Any</Value></Field><Field Name=\"order\"><Value>1</Value></Field><Field Name=\"test-config-name\"><Value>vapi2</Value></Field><Field Name=\"testset-name\"><Value>testsetworking</Value></Field><Field Name=\"testcycl-id\"><Value>10</Value></Field><Field Name=\"host\"><Value>schreida1</Value></Field><Field Name=\"status\"><Value>Passed</Value></Field><Field Name=\"testing-host\"><Value>Host with purpose(s): VAPI-XP</Value></Field><Field Name=\"host-demand\"><Value/></Field><Field Name=\"test-subtype\"><Value>hp.qc.test-instance.VAPI-XP-TEST</Value></Field><Field Name=\"test-name\"><Value>vapi2</Value></Field><Field Name=\"vts\"/><Field Name=\"test-run-try\"><Value/></Field><Field Name=\"start-exec-time\"><Value>18:09:07</Value></Field><Field Name=\"parent-id\"><Value>1752</Value></Field><Field Name=\"procedure-run\"><Value>1459</Value></Field><Field Name=\"duration\"><Value>0</Value></Field><Field Name=\"attributes\"><Value/></Field><Field Name=\"exec-progress\"><Value>Finished</Value></Field></Fields><RelatedEntities/></Entity></Entities>".getBytes();
        Client client = new MockClient(URL, DOMAIN, PROJECT, bytes, USER);
        Testsuites testsuites = publish(client);

        Assert.assertFalse(containsStatus(
                testsuites.getTestsuite(),
                JUnitTestCaseStatus.ERROR));
        Assert.assertNotNull(testsuites);
    }

    private boolean containsStatus(List<Testsuite> testsuites, String status) {

        boolean ret = false;
        for (Testsuite testsuite : testsuites) {
            for (Testcase testcase : testsuite.getTestcase()) {
                if (testcase.getStatus().equals(status)) {
                    ret = true;
                }
            }
        }

        return ret;
    }

    private Testsuites publish(Client client) {

        return new PublisherFactory().create(client, "BVS", "1310", "1001").publish(
                "",
                URL,
                DOMAIN,
                PROJECT,
                new ConsoleLogger());
    }

    private class MockClient extends RestClient4Test {

        private final byte[] _bytes;

        public MockClient(String url, String domain, String project, byte[] bytes, String username) {

            super(url, domain, project, username);
            _bytes = bytes;
        }

        @Override
        public Response httpGet(
                String url,
                String queryString,
                Map<String, String> headers,
                ResourceAccessLevel resourceAccessLevel) {

            return new Response(null, _bytes, null, HttpURLConnection.HTTP_OK);
        }
    }
}
