/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2019 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors ("Micro Focus") are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.octane.model;

import com.microfocus.application.automation.tools.model.OctaneServerSettingsModel;
import com.microfocus.application.automation.tools.octane.configuration.ConfigurationValidator;
import com.microfocus.application.automation.tools.octane.exceptions.AggregatedMessagesException;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class WorkspaceJenkinsUserTest {


    @Test
    public void testParseWorkspace2ImpersonatedUserConf() {
        String payload = "1001:user1" + System.lineSeparator() + //legal line
                "#1002:user2" + System.lineSeparator() + //comment line
                "1003:user1003:user1004" + System.lineSeparator() + //Expected error : Workspace configuration is not valid, valid format is 'Workspace ID:jenkins user': 1003:user1003:user1004
                "user1004:1004" + System.lineSeparator() + //Expected error : Workspace configuration is not valid, workspace ID must be numeric: user1004:1004
                "abc:abc" + System.lineSeparator() + //Expected error : Workspace configuration is not valid, workspace ID must be numeric: abc:abc
                "1005:" + System.lineSeparator() + //Expected error : Workspace configuration is not valid, user value is empty: 1005:
                "1001:user1a" + System.lineSeparator() + //Expected error : Duplicated workspace configuration: 1001:user1a

                "1010:user10" + System.lineSeparator(); //legal line

        //iteration 1 - ignore errors
        Map<Long, String> output = OctaneServerSettingsModel.parseWorkspace2ImpersonatedUserConf(payload, true);
        Assert.assertEquals(3, output.size());
        Assert.assertTrue(output.containsKey(1001L));
        Assert.assertEquals("user1", output.get(1001L));

        Assert.assertTrue(output.containsKey(1010L));
        Assert.assertEquals("user10", output.get(1010L));

        //iteration 1 - get errors
        try {
            OctaneServerSettingsModel.parseWorkspace2ImpersonatedUserConf(payload, false);
            Assert.fail("Exception expected");
        } catch (Exception e) {
            if (e instanceof AggregatedMessagesException) {
                List<String> messages = ((AggregatedMessagesException) e).getMessages();
                Assert.assertEquals(4, messages.size());
                //Assert.assertTrue(messages.contains("Workspace configuration is not valid, valid format is 'Workspace ID:jenkins user': 1003:user1003:user1004"));
                Assert.assertTrue(messages.contains("Workspace configuration is not valid, workspace ID must be numeric: user1004:1004"));
                Assert.assertTrue(messages.contains("Workspace configuration is not valid, workspace ID must be numeric: abc:abc"));
                Assert.assertTrue(messages.contains("Workspace configuration is not valid, user value is empty: 1005:"));
                Assert.assertTrue(messages.contains("Duplicated workspace configuration: 1001:user1a"));
            } else {
                Assert.fail("Wrong type of exception : " + e.getClass().getName());
            }
        }
    }
}
