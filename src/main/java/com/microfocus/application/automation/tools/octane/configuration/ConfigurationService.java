/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2019 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors ("Micro Focus") are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.octane.configuration;

import com.microfocus.application.automation.tools.model.OctaneServerSettingsModel;
import com.microfocus.application.automation.tools.settings.OctaneServerSettingsBuilder;
import hudson.Plugin;
import jenkins.model.Jenkins;

import java.util.Arrays;
import java.util.List;

/***
 * Octane plugin configuration service -
 * 1. helps to change Octane configuration
 * 2. helps to get Octane configuration and model
 */
public class ConfigurationService {

	/**
	 * provides all available configurations
	 *
	 * @return list of all available configurations
	 */
	public static List<OctaneServerSettingsModel> getAllSettings() {
		return Arrays.asList(OctaneServerSettingsBuilder.getOctaneSettingsManager().getServers());
	}

	/**
	 * Get current {@see OctaneServerSettingsModel} model
	 *
	 * @return current configuration
	 */
	public static OctaneServerSettingsModel getSettings(String instanceId) {
		return OctaneServerSettingsBuilder.getOctaneSettingsManager().getSettings(instanceId);
	}

	/**
	 * Change model (used by tests)
	 *
	 * @param newModel new configuration
	 */
	public static void configurePlugin(OctaneServerSettingsModel newModel) {
		OctaneServerSettingsBuilder.getOctaneSettingsManager().setModel(newModel);
	}

	/**
	 * Get plugin version
	 *
	 * @return plugin version
	 */
	public static String getPluginVersion() {
		Plugin plugin = Jenkins.getInstanceOrNull().getPlugin("hp-application-automation-tools-plugin");
		if(plugin == null){
			return "na";
		}
		return plugin.getWrapper().getVersion();
	}
}
