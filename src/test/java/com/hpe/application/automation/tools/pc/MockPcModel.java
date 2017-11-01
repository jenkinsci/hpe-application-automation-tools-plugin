package com.hpe.application.automation.tools.pc;

import com.hpe.application.automation.tools.model.PcModel;
import com.hpe.application.automation.tools.model.PostRunAction;
import com.hpe.application.automation.tools.model.SecretContainer;
import com.hpe.application.automation.tools.model.SecretContainerTest;

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
