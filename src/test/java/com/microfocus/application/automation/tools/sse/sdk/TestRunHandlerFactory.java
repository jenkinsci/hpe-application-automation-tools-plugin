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

import com.microfocus.application.automation.tools.common.SSEException;
import com.microfocus.application.automation.tools.model.SseModel;
import com.microfocus.application.automation.tools.sse.common.TestCase;
import com.microfocus.application.automation.tools.sse.sdk.handler.RunHandlerFactory;
import com.microfocus.application.automation.tools.sse.sdk.handler.TestSetRunHandler;
import org.junit.Assert;
import org.junit.Test;

import com.microfocus.application.automation.tools.sse.sdk.handler.BvsRunHandler;
import com.microfocus.application.automation.tools.sse.sdk.handler.RunHandler;

@SuppressWarnings("squid:S2699")
public class TestRunHandlerFactory extends TestCase {
    
    @Test
    public void testRunHandlerBVS() {
        
        RunHandler runHandler =
                new RunHandlerFactory().create(new MockRestClientFunctional(
                        URL,
                        DOMAIN,
                        PROJECT,
                        USER), SseModel.BVS, "1001");
        Assert.assertNotNull(runHandler);
        Assert.assertTrue(runHandler.getClass() == BvsRunHandler.class);
    }
    
    @Test
    public void testRunHandlerTestSet() {
        
        RunHandler runHandler =
                new RunHandlerFactory().create(new MockRestClientFunctional(
                        URL,
                        DOMAIN,
                        PROJECT,
                        USER), SseModel.TEST_SET, "1001");
        Assert.assertNotNull(runHandler);
        Assert.assertTrue(runHandler.getClass() == TestSetRunHandler.class);
    }
    
    @Test(expected = SSEException.class)
    public void testRunHandlerUnrecognized() {
        
        new RunHandlerFactory().create(
                new MockRestClientFunctional(URL, DOMAIN, PROJECT, USER),
                "Custom",
                "1001");
    }
}
