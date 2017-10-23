package com.hpe.application.automation.tools.run;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentContributor;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.model.TaskListener;

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
