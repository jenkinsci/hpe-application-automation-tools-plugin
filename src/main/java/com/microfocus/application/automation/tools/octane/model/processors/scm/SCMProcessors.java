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
