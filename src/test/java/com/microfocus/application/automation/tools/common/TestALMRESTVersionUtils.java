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

package com.microfocus.application.automation.tools.common;

import com.microfocus.application.automation.tools.model.ALMVersion;
import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

/**
 * @author Effi Bar-She'an
 */
@SuppressWarnings("squid:S2699")
public class TestALMRESTVersionUtils {

    @Test
    public void testToModel() throws UnsupportedEncodingException {

        final String MAJOR = "12";
        final String MINOR = "5";
        final String _dataFormat = "<SiteVersions>\n" +
                "<SiteVersion>12.50.39 (Patch 1)</SiteVersion>\n" +
                "<MajorVersion>%s</MajorVersion>\n" +
                "<MinorVersion>%s</MinorVersion>\n" +
                "<MinorMinorVersion>0</MinorMinorVersion>\n" +
                "<PatchLevel>1</PatchLevel>\n" +
                "<FormatVersion>1</FormatVersion>\n" +
                "</SiteVersions>";
        ALMVersion version = ALMRESTVersionUtils.toModel(String.format(_dataFormat, MAJOR, MINOR).getBytes("UTF-8"));
        Assert.assertEquals(MAJOR, version.getMajorVersion());
        Assert.assertEquals(MINOR, version.getMinorVersion());
    }
}
