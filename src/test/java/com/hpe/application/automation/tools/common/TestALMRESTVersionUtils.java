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

package com.hpe.application.automation.tools.common;

import com.hpe.application.automation.tools.model.ALMVersion;
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
