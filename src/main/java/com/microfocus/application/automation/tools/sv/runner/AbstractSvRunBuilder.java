/*
 *  Certain versions of software accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 *  This software was acquired by Micro Focus on September 1, 2017, and is now
 *  offered by OpenText.
 *  Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical
 *  in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the
 *  property of their respective owners.
 *  OpenText is a trademark of Open Text.
 *  __________________________________________________________________
 *  MIT License
 *
 *  Copyright 2012-2025 Open Text.
 *
 *  The only warranties for products and services of Open Text and
 *  its affiliates and licensors ("Open Text") are as may be set forth
 *  in the express warranty statements accompanying such products and services.
 *  Nothing herein should be construed as constituting an additional warranty.
 *  Open Text shall not be liable for technical or editorial errors or
 *  omissions contained herein. The information contained herein is subject
 *  to change without notice.
 *
 *  Except as specifically indicated otherwise, this document contains
 *  confidential information and a valid license is required for possession,
 *  use or copying. If this work is provided to the U.S. Government,
 *  consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 *  Computer Software Documentation, and Technical Data for Commercial Items are
 *  licensed to the U.S. Government under vendor's standard commercial license.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ___________________________________________________________________
 */
package com.microfocus.application.automation.tools.sv.runner;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microfocus.application.automation.tools.sv.model.AbstractSvRunModel;
import com.microfocus.application.automation.tools.model.SvServerSettingsModel;
import com.microfocus.application.automation.tools.model.SvServiceSelectionModel;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;

public abstract class AbstractSvRunBuilder<T extends AbstractSvRunModel> extends Builder implements SimpleBuildStep {
    private static final Logger LOG = Logger.getLogger(AbstractSvRunBuilder.class.getName());

    protected final T model;

    protected AbstractSvRunBuilder(T model) {
        this.model = model;
    }

    protected static void verifyNotNull(Object value, String errorMessage) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public T getModel() {
        return model;
    }

    public SvServiceSelectionModel getServiceSelection() {
        return model.getServiceSelection();
    }

    protected SvServerSettingsModel getSelectedServerSettings() throws IllegalArgumentException {
        SvServerSettingsModel[] servers = ((AbstractSvRunDescriptor) getDescriptor()).getServers();
        if (servers != null) {
            for (SvServerSettingsModel serverSettings : servers) {
                if (model.getServerName() != null && model.getServerName().equals(serverSettings.getName())) {
                    return serverSettings;
                }
            }
        }
        throw new IllegalArgumentException("Selected server configuration '" + model.getServerName() + "' does not exist.");
    }

    protected abstract AbstractSvRemoteRunner<T> getRemoteRunner(@Nonnull FilePath workspace, TaskListener listener, SvServerSettingsModel server);

        @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        PrintStream logger = listener.getLogger();
        Date startDate = new Date();
        try {
            SvServerSettingsModel serverModel = getSelectedServerSettings();

            logger.printf("%nStarting %s for SV Server '%s' (%s as %s, ignoreSslErrors=%s) on %s%n", getDescriptor().getDisplayName(),
                    serverModel.getName(), serverModel.getUrlObject(), serverModel.getUsername(), serverModel.isTrustEveryone(), startDate);
            logConfig(logger, "    ");
            validateServiceSelection();

            SvServerSettingsModel server = getSelectedServerSettings();
            AbstractSvRemoteRunner<T> runner = getRemoteRunner(workspace, listener, server);
            launcher.getChannel().call(runner);

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Build failed: " + e.getMessage(), e);
            throw new AbortException(e.getMessage());
        } finally {
            double duration = (new Date().getTime() - startDate.getTime()) / 1000.;
            logger.printf("Finished: %s in %.3f seconds%n%n", getDescriptor().getDisplayName(), duration);
        }
    }

    protected void logConfig(PrintStream logger, String prefix) {
        SvServiceSelectionModel ss = model.getServiceSelection();
        switch (ss.getSelectionType()) {
            case SERVICE:
                logger.println(prefix + "Service name or id: " + ss.getService());
                break;
            case PROJECT:
                logger.println(prefix + "Project path: " + ss.getProjectPath());
                logger.println(prefix + "Project password: " + ((StringUtils.isNotBlank(ss.getProjectPassword())) ? "*****" : null));
                break;
            case ALL_DEPLOYED:
                logger.println(prefix + "All deployed services");
                break;
            case DEPLOY:
                logger.println(prefix + "Project path: " + ss.getProjectPath());
                logger.println(prefix + "Project password: " + ((StringUtils.isNotBlank(ss.getProjectPassword())) ? "*****" : null));
                logger.println(prefix + "Service name or id: " + ss.getService());
                break;
        }
        logger.println(prefix + "Force: " + model.isForce());
    }

    protected void validateServiceSelection() throws IllegalArgumentException {
        SvServiceSelectionModel s = getServiceSelection();
        switch (s.getSelectionType()) {
            case SERVICE:
                verifyNotNull(s.getService(), "Service name or id must not be empty if service selection by name or id set");
                break;
            case PROJECT:
                verifyNotNull(s.getProjectPath(), "Project path must not be empty if service selection by project is set");
                break;
            case ALL_DEPLOYED:
                break;
            case DEPLOY:
                verifyNotNull(s.getProjectPath(), "Project path must not be empty for deployment");
                break;
        }
    }
}
