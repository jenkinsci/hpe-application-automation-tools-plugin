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

package com.microfocus.application.automation.tools.octane.actions;

/**
 * Created with IntelliJ IDEA.
 * User: gullery
 * Date: 13/01/15
 * Time: 12:05
 * To change this template use File | Settings | File Templates.
 */

public class Configuration {
	public static enum OS {
		WINDOWS("windows"),
		LINUX("linux");

		private String value;
		private static OS current;

		private OS(String v) {
			value = v;
		}

		public static OS getCurrent() {
			String tmpVal;
			if (current == null) {
				tmpVal = System.getProperty("os.name").toLowerCase();
				if (tmpVal.indexOf("windows") == 0) current = WINDOWS;
				else if (tmpVal.indexOf("linux") == 0) current = LINUX;
				else current = LINUX;
			}
			return current;
		}
	}
}
