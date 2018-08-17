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

package com.microfocus.application.automation.tools.octane.client;

import hudson.util.TimeUnit2;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("squid:S2699")
public class RetryModelTest {

    private RetryModel retryModel;
    private TestTimeProvider testTimeProvider;

    @Before
    public void init() {
        retryModel = new RetryModel();
        testTimeProvider = new TestTimeProvider();
        retryModel.setTimeProvider(testTimeProvider);
    }

    @Test
    public void testRetryModel() {
        Assert.assertFalse(retryModel.isQuietPeriod());

        retryModel.failure();
        // 3 seconds
        Assert.assertTrue(retryModel.isQuietPeriod());
        testTimeProvider.addOffset(TimeUnit2.SECONDS.toMillis(1));
        Assert.assertTrue(retryModel.isQuietPeriod());
        testTimeProvider.addOffset(TimeUnit2.SECONDS.toMillis(1));
        Assert.assertTrue(retryModel.isQuietPeriod());
        testTimeProvider.addOffset(TimeUnit2.SECONDS.toMillis(2));
        Assert.assertFalse(retryModel.isQuietPeriod());

        retryModel.failure();
        // 10 seconds
        Assert.assertTrue(retryModel.isQuietPeriod());
        testTimeProvider.addOffset(TimeUnit2.SECONDS.toMillis(4));
        Assert.assertTrue(retryModel.isQuietPeriod());
        testTimeProvider.addOffset(TimeUnit2.SECONDS.toMillis(4));
        Assert.assertTrue(retryModel.isQuietPeriod());
        testTimeProvider.addOffset(TimeUnit2.SECONDS.toMillis(4));
        Assert.assertFalse(retryModel.isQuietPeriod());

        retryModel.failure();
        // 60 seconds
        Assert.assertTrue(retryModel.isQuietPeriod());
        testTimeProvider.addOffset(TimeUnit2.SECONDS.toMillis(20));
        Assert.assertTrue(retryModel.isQuietPeriod());
        testTimeProvider.addOffset(TimeUnit2.SECONDS.toMillis(20));
        Assert.assertTrue(retryModel.isQuietPeriod());
        testTimeProvider.addOffset(TimeUnit2.SECONDS.toMillis(20));
        Assert.assertFalse(retryModel.isQuietPeriod());

        retryModel.failure();
        // 120 seconds
        Assert.assertTrue(retryModel.isQuietPeriod());
        testTimeProvider.addOffset(TimeUnit2.SECONDS.toMillis(40));
        Assert.assertTrue(retryModel.isQuietPeriod());
        testTimeProvider.addOffset(TimeUnit2.SECONDS.toMillis(40));
        Assert.assertTrue(retryModel.isQuietPeriod());
        testTimeProvider.addOffset(TimeUnit2.SECONDS.toMillis(40));
        Assert.assertFalse(retryModel.isQuietPeriod());
    }

    @Test
    public void testRetryModelSuccess() {
        Assert.assertFalse(retryModel.isQuietPeriod());

        retryModel.failure();
        // 3 seconds
        Assert.assertTrue(retryModel.isQuietPeriod());
        testTimeProvider.addOffset(TimeUnit2.SECONDS.toMillis(1));
        Assert.assertTrue(retryModel.isQuietPeriod());
        testTimeProvider.addOffset(TimeUnit2.SECONDS.toMillis(1));
        Assert.assertTrue(retryModel.isQuietPeriod());
        retryModel.success();
        Assert.assertFalse(retryModel.isQuietPeriod());
    }

    private static class TestTimeProvider implements RetryModel.TimeProvider {

        private long time = System.currentTimeMillis();

        public void addOffset(long time) {
            this.time += time;
        }

        @Override
        public long getTime() {
            return time;
        }
    }
}
