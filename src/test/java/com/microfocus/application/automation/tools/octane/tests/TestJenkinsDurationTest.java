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

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;

/**
 * Created by berkovir on 06/02/2017.
 */
@SuppressWarnings({"squid:S2698","squid:S2699","squid:S3658","squid:S2259","squid:S1872","squid:S2925"})
public class TestJenkinsDurationTest {

    @ClassRule
    public static final JenkinsRule rule = new JenkinsRule();


    @Test
    public void testDuration() throws Exception {

        FreeStyleProject p = rule.createFreeStyleProject("test-duration");

        assertEquals(0, p.getBuilds().toArray().length);
        long start = System.currentTimeMillis();
        p.scheduleBuild2(0);

        while (p.getLastBuild() == null || p.getLastBuild().isBuilding()) {
            Thread.sleep(100);
        }

        long end = System.currentTimeMillis();
        FreeStyleBuild run1 = p.getBuilds().getLastBuild();

        long buildDurationWithoutPostProcessTime = run1.getDuration();
        long buildDurationTotal = (end - start);
        long pluginPostProcessWorkTime = buildDurationTotal - buildDurationWithoutPostProcessTime;


        long buildDurationTotalExpected = 1750;
        long pluginPostProcessWorkTimeExpected = 140;

        System.out.println(String.format("buildDurationTotal=%d, expected=%d", buildDurationTotal, buildDurationTotalExpected));
        System.out.println(String.format("pluginPostProcessWorkTime=%d, expected=%d", pluginPostProcessWorkTime, pluginPostProcessWorkTimeExpected));
        Assert.assertTrue(buildDurationTotal < buildDurationTotalExpected);
        Assert.assertTrue(pluginPostProcessWorkTime < pluginPostProcessWorkTimeExpected);

        int t;

    }
}
