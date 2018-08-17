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

package com.microfocus.application.automation.tools.octane.model.processors.scm;

import com.microfocus.application.automation.tools.settings.OctaneServerSettingsBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by gullery on 31/03/2015.
 */

public enum SCMProcessors {
	NONE("hudson.scm.NullSCM", null),
	GIT("hudson.plugins.git.GitSCM", GitSCMProcessor.class),
	SVN("hudson.scm.SubversionSCM", SvnSCMProcessor.class);

	private static Logger logger = LogManager.getLogger(OctaneServerSettingsBuilder.class);
	private String targetSCMPluginClassName;
	private Class<? extends SCMProcessor> processorClass;

	SCMProcessors(String targetSCMPluginClassName, Class<? extends SCMProcessor> processorClass) {
		this.targetSCMPluginClassName = targetSCMPluginClassName;
		this.processorClass = processorClass;
	}

	public static SCMProcessor getAppropriate(String className) {
		SCMProcessor result = null;

		//  skip any processing if NULL SCM declared
		if (!className.startsWith(NONE.targetSCMPluginClassName)) {
			for (SCMProcessors p : values()) {
				if (className.startsWith(p.targetSCMPluginClassName))
					try {
						result = p.processorClass.newInstance();
						break;
					} catch (InstantiationException | IllegalAccessException e) {
						logger.error("failed to instantiate SCM processor of type '" + p.targetSCMPluginClassName, e);
					}
			}
			if (result == null) {
				result = getGenericSCMProcessor(className);
			}
		}

		return result;
	}

	private static SCMProcessor getGenericSCMProcessor(String className) {
		SCMProcessor genericSCMProcessor = null;
		try {
			genericSCMProcessor = (GenericSCMProcessor.class).newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			logger.error("failed to instantiate SCM processor of type '" + className, e);
		}
		return genericSCMProcessor;
	}
}
