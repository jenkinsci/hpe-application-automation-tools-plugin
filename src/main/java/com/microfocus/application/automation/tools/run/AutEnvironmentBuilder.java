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

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.microfocus.application.automation.tools.model.AUTEnvironmentResolvedModel;
import com.microfocus.application.automation.tools.model.AlmServerSettingsModel;
import com.microfocus.application.automation.tools.model.AutEnvironmentModel;
import com.microfocus.application.automation.tools.settings.AlmServerSettingsBuilder;
import com.microfocus.application.automation.tools.sse.autenvironment.AUTEnvironmentBuilderPerformer;
import com.microfocus.application.automation.tools.sse.common.StringUtils;
import com.microfocus.application.automation.tools.sse.sdk.Logger;
import org.kohsuke.stapler.DataBoundConstructor;

import com.microfocus.application.automation.tools.model.AUTEnvironmentModelResolver;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.VariableResolver;

/**
 * Created by barush on 21/10/2014.
 */
public class AutEnvironmentBuilder extends Builder {
    
    private final AutEnvironmentModel autEnvironmentModel;
    
    @DataBoundConstructor
    public AutEnvironmentBuilder(AutEnvironmentModel autEnvironmentModel) {
        
        this.autEnvironmentModel = autEnvironmentModel;
        
    }
    
    public AutEnvironmentModel getAutEnvironmentModel() {
        return autEnvironmentModel;
    }
    
    @Override
    public DescriptorImpl getDescriptor() {
        
        return (DescriptorImpl) super.getDescriptor();
    }
    
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        
        autEnvironmentModel.setAlmServerUrl(getServerUrl(autEnvironmentModel.getAlmServerName()));
        PrintStream logger = listener.getLogger();
        EnvVars envVars = build.getEnvironment(listener);
        execute(build, envVars, autEnvironmentModel, logger);
        
        return true;
    }
    
    public String getServerUrl(String almServerName) {
        
        String ret = "";
        AlmServerSettingsModel[] almServers = getDescriptor().getAlmServers();
        if (almServers != null && almServers.length > 0) {
            for (AlmServerSettingsModel almServer : almServers) {
                if (almServerName.equals(almServer.getAlmServerName())) {
                    ret = almServer.getAlmServerUrl();
                    break;
                }
            }
        }
        
        return ret;
    }
    
    private void execute(
            AbstractBuild<?, ?> build,
            EnvVars envVars,
            AutEnvironmentModel autEnvironmentModel,
            final PrintStream printStreamLogger) throws InterruptedException {
        
        AUTEnvironmentBuilderPerformer performer;
        try {
            Logger logger = new Logger() {
                
                public void log(String message) {
                    printStreamLogger.println(message);
                }
            };
            VariableResolver.ByMap<String> variableResolver =
                    new VariableResolver.ByMap<String>(envVars);
            
            AUTEnvironmentResolvedModel autEnvModel =
                    AUTEnvironmentModelResolver.resolveModel(autEnvironmentModel, variableResolver);
            performer = new AUTEnvironmentBuilderPerformer(autEnvModel, variableResolver, logger);
            performer.start();
            assignOutputValue(build, performer, autEnvModel.getOutputParameter(), logger);
        } catch (InterruptedException e) {
            build.setResult(Result.ABORTED);
            throw e;
        } catch (Throwable cause) {
            build.setResult(Result.FAILURE);
        }
        
    }
    
    private void assignOutputValue(
            AbstractBuild<?, ?> build,
            AUTEnvironmentBuilderPerformer performer,
            String outputParameterName,
            Logger logger) {
        
        if (StringUtils.isNullOrEmpty(outputParameterName)) {
            logger.log("No environment variable was specified for getting the AUT Environment Configuration ID");
            return;
        }
        
        ParametersAction oldParametersAction = build.getAction(ParametersAction.class);
        if (oldParametersAction != null
            && oldParametersAction.getParameter(outputParameterName) != null) {
            
            List<ParameterValue> parametersList =
                    new ArrayList<ParameterValue>(oldParametersAction.getParameters());
            Iterator<ParameterValue> iterator = parametersList.iterator();
            while (iterator.hasNext()) {
                ParameterValue nextValue = iterator.next();
                if (nextValue.getName().equals(outputParameterName)) {
                    if (!(nextValue instanceof StringParameterValue)) {
                        logger.log(String.format(
                                "Can't assign value to %s because it's type is not 'String Parameter'",
                                outputParameterName));
                        return;
                    }
                    parametersList.remove(nextValue);
                    parametersList.add(new StringParameterValue(
                            nextValue.getName(),
                            performer.getAutEnvironmentConfigurationIdToReturn(),
                            nextValue.getDescription()));
                    break;
                }
            }
            
            build.getActions().remove(oldParametersAction);
            build.addAction(new ParametersAction(parametersList));
            
        } else {
            logger.log(String.format(
                    "Can't assign created AUT Environment Configuration ID to: [%s] because there's no such parameter for this build",
                    outputParameterName));
        }
        
    }
    
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        
        public DescriptorImpl() {
            
            load();
        }
        
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }
        
        @Override
        public String getDisplayName() {
            
            return "Execute AUT Environment preparation using HPE ALM Lab Management";
        }
        
        public AlmServerSettingsModel[] getAlmServers() {
            
            return Hudson.getInstance().getDescriptorByType(
                    AlmServerSettingsBuilder.DescriptorImpl.class).getInstallations();
        }
        
    }
}
