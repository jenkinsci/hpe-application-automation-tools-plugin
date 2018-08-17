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

package com.microfocus.application.automation.tools.octane.tests;

import com.microfocus.application.automation.tools.octane.ResultQueue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

@SuppressWarnings("squid:S2699")
public class TestResultQueueTest {

    private TestsResultQueue queue;

    @Before
    public void init() throws IOException {
        File file = File.createTempFile("TestResultQueueTest", "");
        file.delete();
        queue = new TestsResultQueue(file);
    }

    @Test
    public void testQueue() {
        queue.add("foo", 1);
        ResultQueue.QueueItem item = queue.peekFirst();
        Assert.assertEquals("foo", item.getProjectName());
        Assert.assertEquals(1, item.getBuildNumber());
        Assert.assertEquals(0, item.getFailCount());
    }

    @Test
    public void testAddRemove() {
        Assert.assertNull(queue.peekFirst());
        queue.add("foo", 1);
        Assert.assertNotNull(queue.peekFirst());
        Assert.assertEquals(queue.peekFirst(), queue.peekFirst());
        queue.remove();
        Assert.assertNull(queue.peekFirst());
    }

    @Test
    public void testRetry() {
        Assert.assertNull(queue.peekFirst());
        queue.add("foo", 1);
        Assert.assertEquals(0, queue.peekFirst().getFailCount());
        Assert.assertTrue(queue.failed());
        Assert.assertEquals("foo", queue.peekFirst().getProjectName());
        Assert.assertEquals(1, queue.peekFirst().getBuildNumber());
        Assert.assertEquals(1, queue.peekFirst().getFailCount());
        Assert.assertTrue(queue.failed());
        Assert.assertEquals("foo", queue.peekFirst().getProjectName());
        Assert.assertEquals(1, queue.peekFirst().getBuildNumber());
        Assert.assertEquals(2, queue.peekFirst().getFailCount());
        Assert.assertTrue(queue.failed());
        Assert.assertEquals("foo", queue.peekFirst().getProjectName());
        Assert.assertEquals(1, queue.peekFirst().getBuildNumber());
        Assert.assertEquals(3, queue.peekFirst().getFailCount());
        Assert.assertFalse(queue.failed());
        Assert.assertNull(queue.peekFirst());
    }

    @Test
    public void testInvalidRemove() {
        Assert.assertNull(queue.peekFirst());
        queue.add("foo", 1);
        try {
            queue.remove();
            Assert.fail("should have failed");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    public void testInvalidFailed() {
        Assert.assertNull(queue.peekFirst());
        queue.add("foo", 1);
        try {
            queue.failed();
            Assert.fail("should have failed");
        } catch (IllegalStateException e) {
            // expected
        }
    }
}
