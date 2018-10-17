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

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;

import javax.annotation.CheckForNull;
import java.util.HashMap;
import java.util.Map;

/*
    Handle variable injection for tests of converter builder.
 */

public class VariableInjectionAction implements EnvironmentContributingAction {

    private Map<String, String> variables;

    public VariableInjectionAction(String key, String value) {
        variables = new HashMap<>();
        variables.put(key, value);
    }

    @Override
    public void buildEnvVars(AbstractBuild<?, ?> abstractBuild, EnvVars envVars) {
        if (envVars != null && variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                envVars.put(entry.getKey(), entry.getValue());
            }
        }
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return null;
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return "VariableInjectionAction";
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return null;
    }
}
