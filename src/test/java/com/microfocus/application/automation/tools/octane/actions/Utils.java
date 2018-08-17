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

import hudson.model.AbstractProject;
import hudson.tasks.BatchFile;
import hudson.tasks.CommandInterpreter;
import hudson.tasks.Shell;
import org.jvnet.hudson.test.JenkinsRule;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: gullery
 * Date: 13/01/15
 * Time: 13:55
 * To change this template use File | Settings | File Templates.
 */

public class Utils {
	public static CommandInterpreter getSleepScript(int seconds) {
		if (Configuration.OS.getCurrent() == Configuration.OS.WINDOWS) {
			return new BatchFile("ping -n " + seconds + " 127.0.0.1 >nul");
		} else if (Configuration.OS.getCurrent() == Configuration.OS.LINUX) {
			return new Shell("sleep " + seconds);
		} else {
			return null;
		}
	}

	public static void buildProject(JenkinsRule.WebClient client, AbstractProject project) throws IOException, SAXException {
		client.goTo("job/" + project.getName() + "/build", "");
	}

	public static void buildProjectWithParams(JenkinsRule.WebClient client, AbstractProject project, String params) throws IOException, SAXException {
		client.goTo("job/" + project.getName() + "/buildWithParameters?" + params, "");
	}
}
