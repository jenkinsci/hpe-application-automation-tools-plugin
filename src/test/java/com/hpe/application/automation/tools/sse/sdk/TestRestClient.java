/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.hpe.application.automation.tools.sse.sdk;

import com.hpe.application.automation.tools.sse.common.TestCase;
import org.junit.Assert;
import org.junit.Test;

import com.hpe.application.automation.tools.rest.RestClient;

@SuppressWarnings("squid:S2699")
public class TestRestClient extends TestCase {
    
    @Test
    public void testBuild() {
        
        final String SUFFIX = "mysuffix";
        String url = new RestClient(URL, null, null, USER).build(SUFFIX);
        
        Assert.assertEquals(String.format("%s/%s", URL, SUFFIX), url);
    }
    
    @Test
    public void testBuildRequest() {
        
        final String SUFFIX = "mysuffix";
        String url = new RestClient(URL, DOMAIN, PROJECT, USER).buildRestRequest(SUFFIX);
        
        Assert.assertEquals(
                String.format("%s/rest/domains/%s/projects/%s/%s", URL, DOMAIN, PROJECT, SUFFIX),
                url);
    }
    
    @Test
    public void testBuildRequestWithSlash() {
        final String URL = "http://16.55.245.168:8081/qcbin/";
        final String SUFFIX = "mysuffix";
        String url = new RestClient(URL, DOMAIN, PROJECT, USER).buildRestRequest(SUFFIX);
        
        Assert.assertEquals(
                String.format("%srest/domains/%s/projects/%s/%s", URL, DOMAIN, PROJECT, SUFFIX),
                url);
    }

}
