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

package com.microfocus.application.automation.tools.run;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentContributor;
import hudson.model.ParameterValue;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.ParametersAction;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Fix for supporting SECURITY-170 changes
 * https://jenkins.io/blog/2016/05/11/security-update/
 * https://issues.jenkins-ci.org/browse/JENKINS-39654
 *
 */
public class AdditionalParametersAction  extends ParametersAction{

    private List<ParameterValue> parameters;

    public AdditionalParametersAction(List<ParameterValue> cparameters){
        this.parameters = Collections.unmodifiableList(cparameters);
    }



    @Override
    public List<ParameterValue> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    @Override
    public ParameterValue getParameter(String name){
        for (ParameterValue p : parameters) {
            if (p == null)
                continue;
            if (p.getName().equals(name))
                return p;
        }
        return null;
    }

    @Extension
    public static final class AdditionalParametersActionEnvironmentContributor extends EnvironmentContributor {
        @Override
        public void buildEnvironmentFor(Run r, EnvVars envs, TaskListener listener)
            throws IOException, InterruptedException {
            AdditionalParametersAction action = r.getAction(AdditionalParametersAction.class);
            if (action != null) {
                for (ParameterValue p : action.getParameters()) {
                    envs.putIfNotNull(p.getName(), String.valueOf(p.getValue()));
                }
            }
        }
    }
}
