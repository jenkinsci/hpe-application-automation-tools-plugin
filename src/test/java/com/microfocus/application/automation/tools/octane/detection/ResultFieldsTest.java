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

package com.microfocus.application.automation.tools.octane.detection;

import com.microfocus.application.automation.tools.octane.tests.detection.ResultFields;
import org.junit.Assert;
import org.junit.Test;

public class ResultFieldsTest {

    @Test
    public void testEquals() {
        ResultFields x = new ResultFields("foo", "bar", null);
        Assert.assertTrue(x.equals(x));
        Assert.assertTrue(!x.equals(null));

        ResultFields y = new ResultFields("foo", "bar", null);
        Assert.assertTrue(x.equals(y) && y.equals(x));
        Assert.assertTrue(x.hashCode() == y.hashCode());

        ResultFields z = new ResultFields("foo", "bar", "");
        Assert.assertTrue(!x.equals(z) && !z.equals(x));
    }
}
