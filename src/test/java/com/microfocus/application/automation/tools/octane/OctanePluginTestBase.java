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

package com.microfocus.application.automation.tools.octane;

import com.hp.octane.integrations.dto.DTOFactory;
import com.microfocus.application.automation.tools.model.OctaneServerSettingsModel;
import com.microfocus.application.automation.tools.octane.configuration.ConfigurationService;
import hudson.util.Secret;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.UUID;

public abstract class OctanePluginTestBase {
	protected static final DTOFactory dtoFactory = DTOFactory.getInstance();
	protected static String instanceId;
	protected static String ssp;

	@ClassRule
	public static final JenkinsRule rule = new JenkinsRule();
	public static final JenkinsRule.WebClient client = rule.createWebClient();

	@BeforeClass
	public static void init() {

		instanceId = UUID.randomUUID().toString();
		ssp = UUID.randomUUID().toString();

		OctaneServerSettingsModel model = new OctaneServerSettingsModel(
				"http://localhost:8008/ui/?p=" + ssp,
				"username",
				Secret.fromString("password"),
				"");
		model.setIdentity(instanceId);
		ConfigurationService.configurePlugin(model);
	}
}
