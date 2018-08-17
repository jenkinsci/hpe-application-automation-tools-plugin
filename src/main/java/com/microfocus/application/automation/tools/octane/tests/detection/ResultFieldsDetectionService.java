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

package com.microfocus.application.automation.tools.octane.tests.detection;

import hudson.model.Run;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Service used for auto detection of test results global parameters like test-framework, testing-tool, etc.
 */
public class ResultFieldsDetectionService {
	private static Logger logger = LogManager.getLogger(ResultFieldsDetectionService.class);

	public ResultFields getDetectedFields(Run<?,?> build) throws InterruptedException {
		for (ResultFieldsDetectionExtension ext : ResultFieldsDetectionExtension.all()) {
			try {
				ResultFields fields = ext.detect(build);
				if (fields != null) {
					return fields;
				}
			} catch (InterruptedException e) {
				logger.error("Interrupted during running of detection service: " + ext.getClass().getName(), e);
				throw e;
			} catch (Exception e) {
				logger.error("Error during running of detection service: " + ext.getClass().getName(), e);
			}
		}
		return null;
	}
}
