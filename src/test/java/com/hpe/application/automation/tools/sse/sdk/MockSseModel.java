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

import com.hpe.application.automation.tools.model.CdaDetails;
import com.hpe.application.automation.tools.model.ProxySettings;
import com.hpe.application.automation.tools.model.SecretContainer;
import com.hpe.application.automation.tools.model.SecretContainerTest;
import com.hpe.application.automation.tools.model.SseModel;

public class MockSseModel extends SseModel {
    
    public MockSseModel(
            String almServerName,
            String almUserName,
            String almPassword,
            String almDomain,
            String almProject,
            String runType,
            String almEntityId,
            String timeslotDuration,
            String description,
            String postRunAction,
            String environmentConfigurationId,
            CdaDetails cdaDetails,
            ProxySettings proxySettings) {
        
        super(
                almServerName,
                almUserName,
                almPassword,
                almDomain,
                almProject,
                runType,
                almEntityId,
                timeslotDuration,
                description,
                postRunAction,
                environmentConfigurationId,
                cdaDetails,
                null);
    }
    
    @Override
    protected SecretContainer setPassword(String almPassword) {
        
        SecretContainer secretContainer = new SecretContainerTest();
        secretContainer.initialize(almPassword);
        
        return secretContainer;
    }
}
