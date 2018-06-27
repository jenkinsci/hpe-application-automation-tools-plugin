/*
 * © Copyright 2013 EntIT Software LLC
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright (c) 2018 Micro Focus Company, L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
