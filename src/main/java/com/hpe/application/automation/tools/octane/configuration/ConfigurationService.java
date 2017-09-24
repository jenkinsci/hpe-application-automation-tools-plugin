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

package com.hpe.application.automation.tools.octane.configuration;

import com.hpe.application.automation.tools.model.OctaneServerSettingsModel;
import com.hpe.application.automation.tools.octane.client.JenkinsMqmRestClientFactoryImpl;
import com.hpe.application.automation.tools.settings.OctaneServerSettingsBuilder;
import com.hp.mqm.client.MqmRestClient;
import com.hp.mqm.client.exception.LoginException;
import com.hp.mqm.client.exception.RequestException;
import com.hp.mqm.client.exception.SharedSpaceNotExistException;
import hudson.Plugin;
import jenkins.model.Jenkins;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/***
 * Octane plugin configuration service -
 * 1. helps to change Octane configuration
 * 2. helps to get Octane configuration and model
 * 3. helps to get RestClient based on some configuration
 */
public class ConfigurationService {

    private static JenkinsMqmRestClientFactoryImpl clientFactory;

    private static Logger logger = LogManager.getLogger(ConfigurationService.class);

    /**
     * Get current {@see OctaneServerSettingsModel} model
     *
     * @return
     */
    public static OctaneServerSettingsModel getModel() {
        return getOctaneDescriptor().getModel();
    }

    /**
     * Get current Octane server configuration (that is based on model)
     *
     * @return
     */
    public static ServerConfiguration getServerConfiguration() {
        return getOctaneDescriptor().getServerConfiguration();
    }

    /**
     * Change model (used by tests)
     *
     * @param newModel
     */
    public static void configurePlugin(OctaneServerSettingsModel newModel) {
        getOctaneDescriptor().setModel(newModel);
    }

    private static OctaneServerSettingsBuilder.OctaneDescriptorImpl getOctaneDescriptor() {
        return Jenkins.getInstance().getDescriptorByType(OctaneServerSettingsBuilder.OctaneDescriptorImpl.class);
    }

    /**
     * Get plugin version
     *
     * @return
     */
    public static String getPluginVersion() {
        Plugin plugin = Jenkins.getInstance().getPlugin("hp-application-automation-tools-plugin");
        return plugin.getWrapper().getVersion();
    }

    /**
     * Create restClient based on some server configuration
     *
     * @param serverConfiguration
     * @return
     */
    public static MqmRestClient createClient(ServerConfiguration serverConfiguration) {

        if (!serverConfiguration.isValid()) {
            logger.warn("MQM server configuration is not valid");
            return null;
        }

        MqmRestClient client = getMqmRestClientFactory().obtain(
                serverConfiguration.location,
                serverConfiguration.sharedSpace,
                serverConfiguration.username,
                serverConfiguration.password);

        try {
            client.validateConfigurationWithoutLogin();
            return client;
        } catch (SharedSpaceNotExistException e) {
            logger.warn("Invalid shared space");
        } catch (LoginException e) {
            logger.warn("Login failed : " + e.getMessage());
        } catch (RequestException e) {
            logger.warn("Problem with communication with MQM server : " + e.getMessage());
        }

        return null;
    }

    private static JenkinsMqmRestClientFactoryImpl getMqmRestClientFactory() {
        if (clientFactory == null) {
            clientFactory = Jenkins.getInstance().getExtensionList(JenkinsMqmRestClientFactoryImpl.class).get(0);
        }
        return clientFactory;
    }

}
