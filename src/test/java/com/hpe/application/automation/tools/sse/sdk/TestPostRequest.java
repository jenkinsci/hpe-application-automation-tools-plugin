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

import java.util.Map;

import com.hpe.application.automation.tools.sse.common.StringUtils;
import com.hpe.application.automation.tools.sse.common.TestCase;
import com.hpe.application.automation.tools.sse.sdk.request.PostRequest;
import org.junit.Assert;
import org.junit.Test;

import com.hpe.application.automation.tools.sse.common.RestClient4Test;

@SuppressWarnings("squid:S2699")
public class TestPostRequest extends TestCase {
    
    @Test
    public void testPostRequestException() {
        
        Response response =
                new MockPostRequest(new MockRestClientPostRequestException(
                        URL,
                        DOMAIN,
                        PROJECT,
                        USER), RUN_ID).execute();
        Assert.assertTrue(PostRequestException.class.equals(response.getFailure().getClass()));
    }
    
    private class MockRestClientPostRequestException extends RestClient4Test {
        
        public MockRestClientPostRequestException(
                String url,
                String domain,
                String project,
                String username) {
            
            super(url, domain, project, username);
        }
        
        @Override
        public Response httpPost(
                String url,
                byte[] data,
                Map<String, String> headers,
                ResourceAccessLevel resourceAccessLevel) {
            
            throw new PostRequestException();
        }
    }
    
    private class PostRequestException extends NullPointerException {
        
        private static final long serialVersionUID = 1L;
        
    }
    
    private class MockPostRequest extends PostRequest {
        
        protected MockPostRequest(Client client, String runId) {

            super(client, runId);
        }
        
        @Override
        protected String getSuffix() {
            
            return StringUtils.EMPTY_STRING;
        }
    }
}
