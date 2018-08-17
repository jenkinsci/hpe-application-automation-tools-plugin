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

import hudson.Extension;
import jenkins.model.Jenkins;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.List;

@Extension
public final class PredefinedConfigurationUnmarshaller {
	private final static Logger logger = LogManager.getLogger(PredefinedConfigurationUnmarshaller.class);

	private static PredefinedConfigurationUnmarshaller extensionInstance;
	private static Unmarshaller jaxbUnmarshaller;

	public static synchronized PredefinedConfigurationUnmarshaller getExtensionInstance() {
		List<PredefinedConfigurationUnmarshaller> extensions;
		if (extensionInstance == null) {
			extensions = Jenkins.getInstance().getExtensionList(PredefinedConfigurationUnmarshaller.class);
			if (extensions.isEmpty()) {
				logger.warn("PredefinedConfigurationUnmarshaller was not initialized properly");
				return null;
			} else {
				extensionInstance = extensions.get(0);
			}
		}

		if (jaxbUnmarshaller == null) {
			try {
				jaxbUnmarshaller = JAXBContext.newInstance(PredefinedConfiguration.class).createUnmarshaller();
			} catch (JAXBException e) {
				logger.warn("Unable to create JAXB unmarshaller for predefined server configuration", e);
				return null;
			}
		}
		return extensionInstance;
	}

	public PredefinedConfiguration unmarshall(File configurationFile) {
		if (!configurationFile.canRead()) {
			logger.warn("Unable to read predefined server configuration file");
			return null;
		}
		try {
			return (PredefinedConfiguration) jaxbUnmarshaller.unmarshal(configurationFile);
		} catch (JAXBException e) {
			logger.warn("Unable to unmarshall predefined server configuration", e);
			return null;
		}
	}
}
