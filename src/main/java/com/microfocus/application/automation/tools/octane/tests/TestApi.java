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

package com.microfocus.application.automation.tools.octane.tests;

import com.hp.mqm.client.exception.RequestException;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Item;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Flavor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@SuppressWarnings({"squid:S2699", "squid:S3658", "squid:S2259", "squid:S1872", "squid:S2925", "squid:S109", "squid:S1607"})
public class TestApi {
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();
	private AbstractBuild build;

	public TestApi(AbstractBuild build) {
		this.build = build;
	}

	public void doAudit(StaplerRequest req, StaplerResponse res) throws IOException, InterruptedException {
		// audit log contains possibly sensitive information (location, domain and project): require configure permission
		build.getProject().getACL().checkPermission(Item.CONFIGURE);
		serveFile(res, TestDispatcher.TEST_AUDIT_FILE, Flavor.JSON);
	}

	public void doXml(StaplerRequest req, StaplerResponse res) throws IOException, InterruptedException {
		build.getACL().checkPermission(Item.READ);
		serveFile(res, TestListener.TEST_RESULT_FILE, Flavor.XML);
	}

	public void doLog(StaplerRequest req, final StaplerResponse res) throws IOException, InterruptedException {
		build.getACL().checkPermission(Item.READ);
		FilePath auditFile = new FilePath(new File(build.getRootDir(), TestDispatcher.TEST_AUDIT_FILE));
		if (!auditFile.exists()) {
			res.sendError(404, "Audit file is not present, log information is not available");
			return;
		}
		JSONArray audit = JSONArray.fromObject(auditFile.readToString());
		JSONObject lastAudit = audit.getJSONObject(audit.size() - 1);
		if (!lastAudit.getBoolean("pushed")) {
			res.sendError(404, "Last audited push didn't succeed, log information is not available");
			return;
		}
		long pushTestResultRequestId = lastAudit.getLong("id");

		res.setStatus(200);
		try {
			OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class)
					.setMethod(HttpMethod.GET)
					.setUrl(OctaneSDK.getInstance().getPluginServices().getOctaneConfiguration().getUrl() +
							"/internal-api/shared_spaces/" + OctaneSDK.getInstance().getPluginServices().getOctaneConfiguration().getSharedSpace() +
							"/analytics/ci/test-results/" + pushTestResultRequestId + "/log");
			OctaneResponse response = OctaneSDK.getInstance().getRestService().obtainClient().execute(request);
			if (response.getStatus() == 200) {
				res.setStatus(200);
				res.setHeader("content-type", "text/plain");
				res.getWriter().write(response.getBody());
			} else {
				res.sendError(500, "failed to retrieve log for tests result push request " + pushTestResultRequestId + "; " + response.getStatus() + " " + response.getBody());
			}
		} catch (RequestException e) {
			if ("testbox.not_found".equals(e.getErrorCode())) {
				res.sendError(404, "Log information is not available. It either expired or shared space was re-created on the server.");
			} else {
				throw e;
			}
		}
	}

	private void serveFile(StaplerResponse res, String relativePath, Flavor flavor) throws IOException, InterruptedException {
		FilePath file = new FilePath(new File(build.getRootDir(), relativePath));
		if (!file.exists()) {
			res.sendError(404, "Information not available");
			return;
		}
		res.setStatus(200);
		res.setContentType(flavor.contentType);
		InputStream is = file.read();
		IOUtils.copy(is, res.getOutputStream());
		IOUtils.closeQuietly(is);
	}
}
