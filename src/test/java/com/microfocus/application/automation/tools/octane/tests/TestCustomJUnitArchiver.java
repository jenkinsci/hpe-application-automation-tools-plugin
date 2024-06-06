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

package com.microfocus.application.automation.tools.octane.tests;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;
import hudson.tasks.test.AbstractTestResultAction;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class TestCustomJUnitArchiver extends Recorder {

	private String resultFile;

	public TestCustomJUnitArchiver(String resultFile) {
		this.resultFile = resultFile;
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		build.addAction(new TestResultAction());
		InputStream is = build.getWorkspace().child(resultFile).read();
		String junitResults = IOUtils.toString(is, "UTF-8");
		junitResults = junitResults
				.replaceAll("%%%WORKSPACE%%%", build.getWorkspace().getRemote())
				.replaceAll("%%%SEPARATOR%%%", File.separator.equals("\\") ? "\\\\" : File.separator);
		IOUtils.closeQuietly(is);
		new FilePath(build.getRootDir()).child("junitResult.xml").write(junitResults, "UTF-8");
		return true;
	}

	private static class TestResultAction extends AbstractTestResultAction {

		@Override
		public int getFailCount() {
			return 0;
		}

		@Override
		public int getTotalCount() {
			return 0;
		}

		@Override
		public Object getResult() {
			return new Object();
		}
	}
}
