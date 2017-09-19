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

package com.hpe.application.automation.tools.sse.common;

import com.hpe.application.automation.tools.rest.RestClient;

public class RestClient4Test extends RestClient {
	private final static String AUTH_INFO_FRONT = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><AuthenticationInfo><Username>";
	private final static String AUTH_INFO_BACK = "</Username></AuthenticationInfo>";

	public RestClient4Test(String url, String domain, String project, String username) {
		super(url, domain, project, username);
	}

    public byte[] getExpectAuthInfo() {
    	
    	String authInfo = AUTH_INFO_FRONT + getUsername() + AUTH_INFO_BACK;
		
    	return  authInfo.getBytes();
	}
}
