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

import com.microfocus.application.automation.tools.model.PcModel;
import com.microfocus.adm.performancecenter.plugins.common.pcEntities.*;
import com.microfocus.application.automation.tools.model.SecretContainer;
import com.microfocus.application.automation.tools.model.SecretContainerTest;

public class MockPcModel extends PcModel {


    public MockPcModel(String serverAndPort, String pcServerName, String almUserName, String almPassword, String almDomain,
                       String almProject, String testId, String autoTestInstanceID, String testInstanceId, String timeslotDurationHours,
                       String timeslotDurationMinutes, PostRunAction postRunAction, boolean vudsMode, String description, boolean webProtocol) {
        super(serverAndPort, pcServerName, almUserName, almPassword, almDomain, almProject, testId, autoTestInstanceID, testInstanceId, timeslotDurationHours,
            timeslotDurationMinutes, postRunAction, vudsMode, description, "NO_TREND", null,false,null,null,null
        );
    }

    @Override
    protected SecretContainer setPassword(String almPassword) {
        
        SecretContainer secretContainer = new SecretContainerTest();
        secretContainer.initialize(almPassword);
        
        return secretContainer;
    }
    
    

}
