/*
 * Certain versions of software accessible here may contain branding from Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 * This software was acquired by Micro Focus on September 1, 2017, and is now offered by OpenText.
 * Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright 2012-2024 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.octane.executor.scmmanager;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.executor.TestConnectivityInfo;
import com.hp.octane.integrations.dto.scm.SCMRepository;
import com.hp.octane.integrations.dto.scm.SCMType;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Job;
import hudson.scm.SCM;
import hudson.scm.SubversionSCM;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.tmatesoft.svn.core.SVNException;

import java.io.File;
import java.io.IOException;

public class SvnPluginHandler implements ScmPluginHandler {

	@Override
	public void setScmRepositoryInJob(SCMRepository scmRepository, String scmRepositoryCredentialsId, FreeStyleProject proj, boolean executorJob) throws IOException {

		String relativeCheckOut;
		if (executorJob) {
			String url = StringUtils.stripEnd(scmRepository.getUrl(), "/").replaceAll("[<>:\"/\\|?*]", "_");
			relativeCheckOut = "a\\..\\..\\..\\_test_sources\\" + url;
		} else {
			relativeCheckOut = "a" + File.separator + "..";//it doesn't has any affect on checkout folder. Without it, svn do checkout to folder of svn repository name
			//for example for URL https://myd-vm00812.hpeswlab.net/svn/uft_tests/ - tests will be cloned to "uft_tests" subfolder
			//adding a\.. will clone as is - Why we need it - subfolder is part of "package" reported to Octane
		}

		SubversionSCM svn = new SubversionSCM(scmRepository.getUrl(), scmRepositoryCredentialsId, relativeCheckOut);
		proj.setScm(svn);
	}

	@Override
	public String getSharedCheckOutDirectory(Job j) {
		SCM scm = ((FreeStyleProject) j).getScm();

		SubversionSCM svnScm = (SubversionSCM) scm;
		for (SubversionSCM.ModuleLocation location : svnScm.getLocations()) {
			if (StringUtils.isNotEmpty(location.getLocalDir())) {
				return location.getLocalDir();
			}
		}

		return null;
	}

	@Override
	public void checkRepositoryConnectivity(TestConnectivityInfo testConnectivityInfo, StandardCredentials credentials, OctaneResponse result) {

		String credentialsId = (credentials != null ? credentials.getId() : null);
		SubversionSCM svn = new SubversionSCM(testConnectivityInfo.getScmRepository().getUrl(), credentialsId);
		try {
			svn.getDescriptor().checkRepositoryPath((Item) null, svn.getLocations()[0].getSVNURL(), credentials);
			result.setStatus(HttpStatus.SC_OK);
		} catch (Exception e) {
			result.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			result.setBody("Failed to connect to SVN : " + e.getMessage());
		}
	}

	@Override
	public String getScmRepositoryUrl(SCM scm) {
		return ((SubversionSCM) scm).getLocations()[0].remote;
	}

	@Override
	public String getScmRepositoryCredentialsId(SCM scm) {
		return ((SubversionSCM) scm).getLocations()[0].credentialsId;
	}

	@Override
	public SCMType getScmType() {
		return SCMType.SVN;
	}

	@Override
	public String tryExtractUrlShortName(String url) {
		return url;
	}
}
