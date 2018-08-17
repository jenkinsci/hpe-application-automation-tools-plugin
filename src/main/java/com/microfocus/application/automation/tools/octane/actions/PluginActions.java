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

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.api.TasksProcessor;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneResultAbridged;
import com.hp.octane.integrations.dto.connectivity.OctaneTaskAbridged;
import com.microfocus.application.automation.tools.octane.configuration.ConfigApi;
import hudson.Extension;
import hudson.model.RootAction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: gullery
 * Date: 8/10/14
 * Time: 12:47 PM
 * To change this template use File | Settings | File Templates.
 */

@Extension
public class PluginActions implements RootAction {
	private static final Logger logger = LogManager.getLogger(PluginActions.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	public String getIconFileName() {
		return null;
	}

	public String getDisplayName() {
		return null;
	}

	public String getUrlName() {
		return "nga";
	}

	public ConfigApi getConfiguration() {
		return new ConfigApi();
	}

	public void doDynamic(StaplerRequest req, StaplerResponse res) throws IOException, ServletException {
		HttpMethod method = null;
		if ("post".equals(req.getMethod().toLowerCase())) {
			method = HttpMethod.POST;
		} else if ("get".equals(req.getMethod().toLowerCase())) {
			method = HttpMethod.GET;
		} else if ("put".equals(req.getMethod().toLowerCase())) {
			method = HttpMethod.PUT;
		} else if ("delete".equals(req.getMethod().toLowerCase())) {
			method = HttpMethod.DELETE;
		}
		if (method != null) {
			OctaneTaskAbridged octaneTaskAbridged = dtoFactory.newDTO(OctaneTaskAbridged.class);
			octaneTaskAbridged.setId(UUID.randomUUID().toString());
			octaneTaskAbridged.setMethod(method);
			octaneTaskAbridged.setUrl(req.getRequestURIWithQueryString());
			octaneTaskAbridged.setBody(getBody(req.getReader()));
			TasksProcessor taskProcessor = OctaneSDK.getInstance().getTasksProcessor();
			OctaneResultAbridged result = taskProcessor.execute(octaneTaskAbridged);

			res.setStatus(result.getStatus());
			if (result.getBody() != null) {
				res.getWriter().write(result.getBody());
			}
			if (result.getHeaders() != null) {
				for (Map.Entry<String, String> header : result.getHeaders().entrySet()) {
					res.setHeader(header.getKey(), header.getValue());
				}
			}
		} else {
			res.setStatus(501);
		}
	}

	public static String getBody(BufferedReader reader) throws IOException {

		StringBuilder buffer = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			buffer.append(line);
		}
		String body = buffer.toString();
		return body;
	}
}
