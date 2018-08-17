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

package com.microfocus.application.automation.tools.octane.configuration;

import com.microfocus.application.automation.tools.model.OctaneServerSettingsModel;
import com.microfocus.application.automation.tools.settings.OctaneServerSettingsBuilder;
import hudson.Plugin;
import jenkins.model.Jenkins;

/***
 * Octane plugin configuration service -
 * 1. helps to change Octane configuration
 * 2. helps to get Octane configuration and model
 * 3. helps to get RestClient based on some configuration
 */
public class ConfigurationService {

    /**
     * Get current {@see OctaneServerSettingsModel} model
     *
     * @return current configuration
     */
    public static OctaneServerSettingsModel getModel() {
        return getOctaneDescriptor().getModel();
    }

    /**
     * Get current Octane server configuration (that is based on model)
     *
     * @return current configuration
     */
    public static ServerConfiguration getServerConfiguration() {
        if (getOctaneDescriptor() != null) {
            return getOctaneDescriptor().getServerConfiguration();
        }
        return null;
    }

    /**
     * Change model (used by tests)
     *
     * @param newModel new configuration
     */
    public static void configurePlugin(OctaneServerSettingsModel newModel) {
        if (getOctaneDescriptor() != null) {
            getOctaneDescriptor().setModel(newModel);
        }
    }

    private static OctaneServerSettingsBuilder.OctaneDescriptorImpl getOctaneDescriptor() {
        OctaneServerSettingsBuilder.OctaneDescriptorImpl octaneDescriptor = getJenkinsInstance().getDescriptorByType(OctaneServerSettingsBuilder.OctaneDescriptorImpl.class);
        if (octaneDescriptor == null) {
            throw new IllegalArgumentException("failed to obtain Octane plugin descriptor");
        }

        return octaneDescriptor;
    }

    /**
     * Get plugin version
     *
     * @return plugin version
     */
    public static String getPluginVersion() {
        Plugin plugin = getJenkinsInstance().getPlugin("hp-application-automation-tools-plugin");
        return plugin.getWrapper().getVersion();
    }


    private static Jenkins getJenkinsInstance() {
        Jenkins result = Jenkins.getInstance();
        if (result == null) {
            throw new IllegalStateException("failed to obtain Jenkins instance");
        }
        return result;
    }
}
