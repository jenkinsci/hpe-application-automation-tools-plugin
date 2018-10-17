/*
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

package com.microfocus.application.automation.tools.octane.testrunner;

import com.hp.octane.integrations.executor.TestsToRunFramework;
import com.microfocus.application.automation.tools.model.EnumDescription;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/*
 * Test model for storing of available frameworks for converting
 */
public class TestsToRunConverterModel implements Serializable {

    private static final long serialVersionUID = 1L;

    public final static EnumDescription none = new EnumDescription("", "");

    public final static List<EnumDescription> Frameworks;

    private String framework;

    static {
        List<EnumDescription> temp = new ArrayList<>();
        temp.add(none);
        for (TestsToRunFramework fr : TestsToRunFramework.values()) {
            temp.add(new EnumDescription(fr.value(), fr.getDesc()));
        }
        Frameworks = temp;
    }

    @DataBoundConstructor
    public TestsToRunConverterModel(String framework) {

        this.framework = framework;
    }

    public String getFramework() {
        return framework;
    }

}



