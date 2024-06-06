/*
 * Certain versions of software accessible here may contain branding from Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 * This software was acquired by Micro Focus on September 1, 2017, and is now offered by OpenText.
 * Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright 2012-2024 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ___________________________________________________________________
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
import com.microfocus.application.automation.tools.settings.AlmServerSettingsGlobalConfiguration;
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

import static com.microfocus.application.automation.tools.Messages.AutEnvironmentBuilderStepName;
import static com.microfocus.application.automation.tools.Messages.CompanyName;

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
            final PrintStream printStreamLogger) {

        AUTEnvironmentBuilderPerformer performer;

        Logger logger = new Logger() {
            @Override
            public void log(String message) {
                printStreamLogger.println(message);
            }
            @Override
            public void error(String message) {
                log(message);
            }
        };

        try {
            VariableResolver.ByMap<String> variableResolver =
                    new VariableResolver.ByMap<String>(envVars);

            AUTEnvironmentResolvedModel autEnvModel =
                    AUTEnvironmentModelResolver.resolveModel(autEnvironmentModel, variableResolver);
            performer = new AUTEnvironmentBuilderPerformer(autEnvModel, variableResolver, logger);
            performer.start(envVars);
            assignOutputValue(build, performer, autEnvModel.getOutputParameter(), logger);

        } catch (Exception e) {
            logger.log(String.format("Build failed: %s", e.getMessage()));
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
            return AutEnvironmentBuilderStepName(CompanyName());
        }

        public AlmServerSettingsModel[] getAlmServers() {
            return AlmServerSettingsGlobalConfiguration.getInstance().getInstallations();
        }

    }
}
