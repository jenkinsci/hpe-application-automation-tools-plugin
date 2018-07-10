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
        if(getOctaneDescriptor() != null) {
            return getOctaneDescriptor().getServerConfiguration();
        }
        return null;
    }

    /**
     * Change model (used by tests)
     *
     * @param newModel
     */
    public static void configurePlugin(OctaneServerSettingsModel newModel) {
        if(getOctaneDescriptor() != null) {
            getOctaneDescriptor().setModel(newModel);
        }
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
