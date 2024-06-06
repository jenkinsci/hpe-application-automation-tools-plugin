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

package com.microfocus.application.automation.tools.octane.tests.detection;

import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.remoting.VirtualChannel;
import hudson.tasks.junit.JUnitResultArchiver;
import hudson.tasks.test.AbstractTestResultAction;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Extension(optional = true)
public class TestNGExtension extends ResultFieldsDetectionExtension {
	private static final String TESTNG = "TestNG";
	private static final String TESTNG_RESULT_FILE = "testng-results.xml";
	private static final List<String> supportedReportFileLocations = Arrays.asList(
			"target/surefire-reports/" + TESTNG_RESULT_FILE,
			"target/failsafe-reports/" + TESTNG_RESULT_FILE
	);

	@Override
	public ResultFields detect(final Run build) throws IOException, InterruptedException {
		if (!(build instanceof AbstractBuild)) {
			return null;
		}

		final List<Object> publishers = ((AbstractBuild) build).getProject().getPublishersList().toList();
		for (Object publisher : publishers) {
			if ("hudson.tasks.junit.JUnitResultArchiver".equals(publisher.getClass().getName())) {
				JUnitResultArchiver junit = (JUnitResultArchiver) publisher;
				String testResultsPattern = junit.getTestResults();
				if (BuildHandlerUtils.getWorkspace(build).act(new TestNgResultsFileFinder(testResultsPattern))) {
					return new ResultFields(TESTNG, null, null);
				}
			}
		}

		if ("hudson.maven.MavenBuild".equals(build.getClass().getName())) {
			MavenBuild mavenBuild = (MavenBuild) build;
			if (findTestNgResultsFile(mavenBuild)) {
				return new ResultFields(TESTNG, null, null);
			}
		}

		if ("hudson.maven.MavenModuleSetBuild".equals(build.getClass().getName())) {
			Map<MavenModule, MavenBuild> moduleLastBuilds = ((MavenModuleSetBuild) build).getModuleLastBuilds();
			for (MavenBuild mavenBuild : moduleLastBuilds.values()) {
				if (findTestNgResultsFile(mavenBuild)) {
					return new ResultFields(TESTNG, null, null);
				}
			}
		}
		return null;
	}

	boolean findTestNgResultsFile(MavenBuild mavenBuild) throws IOException, InterruptedException {
		AbstractTestResultAction action = mavenBuild.getAction(AbstractTestResultAction.class);
		//try finding only if the maven build includes tests
		return action != null && mavenBuild.getWorkspace().act(new TestNgResultsFileMavenFinder());
	}

	public static class TestNgResultsFileFinder implements FilePath.FileCallable<Boolean> {

		private String testResultsPattern;

		public TestNgResultsFileFinder(String testResultsPattern) {
			this.testResultsPattern = testResultsPattern;
		}

		@Override
		public Boolean invoke(File workspace, VirtualChannel virtualChannel) throws IOException, InterruptedException {
			FileSet fs = Util.createFileSet(workspace, testResultsPattern);
			DirectoryScanner ds = fs.getDirectoryScanner();
			String[] includedFiles = ds.getIncludedFiles();
			File baseDir = ds.getBasedir();

			if (includedFiles.length > 0 && findTestNgResultsFile(baseDir, includedFiles)) {
				return true;
			}
			return false;
		}

		@Override
		public void checkRoles(RoleChecker roleChecker) throws SecurityException {
			roleChecker.check(this, Role.UNKNOWN);
		}

		boolean findTestNgResultsFile(File baseDir, String[] includedFiles) throws IOException, InterruptedException {
			Set<FilePath> directoryCache = new LinkedHashSet<>();

			for (String path : includedFiles) {
				FilePath file = new FilePath(baseDir).child(path);
				if (file.exists() && !directoryCache.contains(file.getParent())) {
					directoryCache.add(file.getParent());
					FilePath testNgResulsFile = new FilePath(file.getParent(), TESTNG_RESULT_FILE);
					if (testNgResulsFile.exists()) {
						return true;
					}
				}
			}
			return false;
		}
	}

	public static class TestNgResultsFileMavenFinder implements FilePath.FileCallable<Boolean> {

		@Override
		public Boolean invoke(File workspace, VirtualChannel virtualChannel) {
			for (String locationInWorkspace : supportedReportFileLocations) {
				File reportFile = new File(workspace, locationInWorkspace);
				if (reportFile.exists()) {
					return true;
				}
			}
			return false;
		}

		@Override
		public void checkRoles(RoleChecker roleChecker) throws SecurityException {
			roleChecker.check(this, Role.UNKNOWN);
		}
	}
}