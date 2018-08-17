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

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.configuration.OctaneConfiguration;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.microfocus.application.automation.tools.octane.Messages;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.Secret;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

@Extension
public class ConfigurationParser {
	private final static Logger logger = LogManager.getLogger(ConfigurationParser.class);
	private final static DTOFactory dtoFactory = DTOFactory.getInstance();

	private static final String PARAM_SHARED_SPACE = "p"; // NON-NLS

	public static MqmProject parseUiLocation(String uiLocation) throws FormValidation {
		try {
			URL url = new URL(uiLocation);
			String location;
			int contextPos = uiLocation.indexOf("/ui");
			if (contextPos < 0) {
				throw FormValidation.errorWithMarkup(markup("red", Messages.ApplicationContextNotFound()));
			} else {
				location = uiLocation.substring(0, contextPos);
			}
			List<NameValuePair> params = URLEncodedUtils.parse(url.toURI(), "UTF-8");
			for (NameValuePair param : params) {
				if (param.getName().equals(PARAM_SHARED_SPACE)) {
					String[] sharedSpaceAndWorkspace = param.getValue().split("/");
					// we are relaxed and allow parameter without workspace in order not to force user to makeup
					// workspace value when configuring manually or via config API and not via copy & paste
					if (sharedSpaceAndWorkspace.length < 1 || StringUtils.isEmpty(sharedSpaceAndWorkspace[0])) {
						throw FormValidation.errorWithMarkup(markup("red", Messages.UnexpectedSharedSpace()));
					}
					return new MqmProject(location, sharedSpaceAndWorkspace[0]);
				}
			}
			throw FormValidation.errorWithMarkup(markup("red", Messages.MissingSharedSpace()));
		} catch (MalformedURLException e) {
			throw FormValidation.errorWithMarkup(markup("red", Messages.ConfigurationUrInvalid()));
		} catch (URISyntaxException e) {
			throw FormValidation.errorWithMarkup(markup("red", Messages.ConfigurationUrInvalid()));
		}
	}

	public FormValidation checkConfiguration(String location, String sharedSpace, String username, Secret password) {
		OctaneConfiguration configuration = dtoFactory.newDTO(OctaneConfiguration.class)
				.setUrl(location)
				.setSharedSpace(sharedSpace)
				.setApiKey(username)
				.setSecret(password.getPlainText());
		OctaneResponse checkResponse;
		try {
			checkResponse = OctaneSDK.getInstance().getConfigurationService().validateConfiguration(configuration);
		} catch (IOException ioe) {
			logger.warn("Connection check failed due to communication problem", ioe);
			return FormValidation.errorWithMarkup(markup("red", Messages.ConnectionFailure()));
		}
		if (checkResponse.getStatus() == 200) {
			return FormValidation.okWithMarkup(markup("green", Messages.ConnectionSuccess()));
		} else if (checkResponse.getStatus() == 401) {
			return FormValidation.errorWithMarkup(markup("red", Messages.AuthenticationFailure()));
		} else if (checkResponse.getStatus() == 403) {
			return FormValidation.errorWithMarkup(markup("red", Messages.AuthorizationFailure()));
		} else if (checkResponse.getStatus() == 404) {
			return FormValidation.errorWithMarkup(markup("red", Messages.ConnectionSharedSpaceInvalid()));
		} else {
			return FormValidation.errorWithMarkup(markup("red", Messages.UnexpectedFailure() + ": " + checkResponse.getStatus()));
		}
	}

	public static String markup(String color, String message) {
		return "<font color=\"" + color + "\"><b>" + message + "</b></font>";
	}
}
