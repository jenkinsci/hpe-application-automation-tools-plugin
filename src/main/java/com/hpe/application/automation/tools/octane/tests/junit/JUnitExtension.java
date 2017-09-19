/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.hpe.application.automation.tools.octane.tests.junit;

import com.google.inject.Inject;
import com.hpe.application.automation.tools.octane.actions.cucumber.CucumberTestResultsAction;
import com.hpe.application.automation.tools.octane.executor.CheckOutSubDirEnvContributor;
import com.hpe.application.automation.tools.octane.tests.HPRunnerType;
import com.hpe.application.automation.tools.octane.tests.MqmTestsExtension;
import com.hpe.application.automation.tools.octane.tests.TestResultContainer;
import com.hpe.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import com.hpe.application.automation.tools.octane.tests.detection.ResultFields;
import com.hpe.application.automation.tools.octane.tests.detection.ResultFieldsDetectionService;
import com.hpe.application.automation.tools.octane.tests.impl.ObjectStreamIterator;
import com.hpe.application.automation.tools.octane.tests.testResult.TestResult;
import hudson.Extension;
import hudson.FilePath;
import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.Run;
import hudson.remoting.VirtualChannel;
import hudson.tasks.test.AbstractTestResultAction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.*;

/**
 * Converter of Jenkins test report to ALM Octane test report format(junitResult.xml->mqmTests.xml)
 */
@Extension
public class JUnitExtension extends MqmTestsExtension {
	private static Logger logger = LogManager.getLogger(JUnitExtension.class);

	public static final String STORM_RUNNER = "StormRunner";
	public static final String LOAD_RUNNER = "LoadRunner";
	public static final String PERFORMANCE_CENTER_RUNNER = "Performance Center";
	public static final String PERFORMANCE_TEST_TYPE = "Performance";

	private static final String JUNIT_RESULT_XML = "junitResult.xml"; // NON-NLS

	private static final String PREFORMANCE_REPORT = "PerformanceReport";
	private static final String TRANSACTION_SUMMARY = "TransactionSummary";
	@Inject
	ResultFieldsDetectionService resultFieldsDetectionService;

	public boolean supports(Run<?, ?> build) throws IOException, InterruptedException {
		if (build.getAction(CucumberTestResultsAction.class) != null) {
			logger.debug("CucumberTestResultsAction found. Will not process JUnit results.");
			return false;
		} else if (build.getAction(AbstractTestResultAction.class) != null) {
			logger.debug("AbstractTestResultAction found, JUnit results expected");
			return true;
		} else {
			logger.debug("AbstractTestResultAction not found, no JUnit results expected");
			return false;
		}
	}

	@Override
	public TestResultContainer getTestResults(Run<?, ?> build, HPRunnerType hpRunnerType, String jenkinsRootUrl) throws IOException, InterruptedException {
		logger.debug("Collecting JUnit results");

		boolean isLoadRunnerProject = isLoadRunnerProject(build);
		FilePath resultFile = new FilePath(build.getRootDir()).child(JUNIT_RESULT_XML);
		if (resultFile.exists()) {
			logger.debug("JUnit result report found");
			ResultFields detectedFields = getResultFields(build, hpRunnerType, isLoadRunnerProject);
			FilePath filePath= BuildHandlerUtils.getWorkspace(build).act(new GetJUnitTestResults(build, Arrays.asList(resultFile), shallStripPackageAndClass(detectedFields), hpRunnerType, jenkinsRootUrl));
			return new TestResultContainer(new ObjectStreamIterator<TestResult>(filePath, true), detectedFields);
		} else {
			//avoid java.lang.NoClassDefFoundError when maven plugin is not present
			if ("hudson.maven.MavenModuleSetBuild".equals(build.getClass().getName())) {
				logger.debug("MavenModuleSetBuild detected, looking for results in maven modules");

				List<FilePath> resultFiles = new LinkedList<FilePath>();
				Map<MavenModule, MavenBuild> moduleLastBuilds = ((MavenModuleSetBuild) build).getModuleLastBuilds();
				for (MavenBuild mavenBuild : moduleLastBuilds.values()) {
					AbstractTestResultAction action = mavenBuild.getAction(AbstractTestResultAction.class);
					if (action != null) {
						FilePath moduleResultFile = new FilePath(mavenBuild.getRootDir()).child(JUNIT_RESULT_XML);
						if (moduleResultFile.exists()) {
							logger.debug("Found results in " + mavenBuild.getFullDisplayName());
							resultFiles.add(moduleResultFile);
						}
					}
				}
				if (!resultFiles.isEmpty()) {
					ResultFields detectedFields = getResultFields(build, hpRunnerType, isLoadRunnerProject);
					FilePath filePath = BuildHandlerUtils.getWorkspace(build).act(new GetJUnitTestResults(build, resultFiles, shallStripPackageAndClass(detectedFields), hpRunnerType, jenkinsRootUrl));
					return new TestResultContainer(new ObjectStreamIterator<TestResult>(filePath, true), detectedFields);
				}
			}
			logger.debug("No JUnit result report found");
			return null;
		}
	}

	private ResultFields getResultFields(Run<?, ?> build, HPRunnerType hpRunnerType, boolean isLoadRunnerProject) throws InterruptedException {
		ResultFields detectedFields;
		if (hpRunnerType.equals(HPRunnerType.StormRunner)) {
			detectedFields = new ResultFields(null, STORM_RUNNER, null);
		} else if (isLoadRunnerProject) {
			detectedFields = new ResultFields(null, LOAD_RUNNER, null);
		} else if (hpRunnerType.equals(HPRunnerType.PerformanceCenter)) {
			detectedFields = new ResultFields(null, PERFORMANCE_CENTER_RUNNER, null, PERFORMANCE_TEST_TYPE);
		} else {
			detectedFields = resultFieldsDetectionService.getDetectedFields(build);
		}

		return detectedFields;
	}

	private boolean shallStripPackageAndClass(ResultFields resultFields) {
		/*if (resultFields == null) {
			return false;
		}*/
		return false; //resultFields.equals(new ResultFields("UFT", "UFT", null));
	}

	private boolean isLoadRunnerProject(Run build) throws IOException, InterruptedException {
		FilePath preformanceReportFolder = new FilePath(build.getRootDir()).child(PREFORMANCE_REPORT);
		FilePath transactionSummaryFolder = new FilePath(build.getRootDir()).child(TRANSACTION_SUMMARY);
		if ((preformanceReportFolder.exists() && preformanceReportFolder.isDirectory()) && (transactionSummaryFolder.exists() && transactionSummaryFolder.isDirectory())) {
			return true;
		}
		return false;
	}

	private static class GetJUnitTestResults implements FilePath.FileCallable<FilePath> {

		private final List<FilePath> reports;
		private final String jobName;
		private final String buildId;
		private final String jenkinsRootUrl;
		private final HPRunnerType hpRunnerType;
		private FilePath filePath;
		private List<ModuleDetection> moduleDetection;
		private long buildStarted;
		private FilePath workspace;
		private boolean stripPackageAndClass;
		private String sharedCheckOutDirectory;

		//this class is run on master and JUnitXmlIterator is runnning on slave.
		//this object pass some master2slave data
		private Object additionalContext;
		private String buildRootDir;

		public GetJUnitTestResults( Run<?, ?> build, List<FilePath> reports, boolean stripPackageAndClass, HPRunnerType hpRunnerType, String jenkinsRootUrl) throws IOException, InterruptedException {
			this.reports = reports;
			this.filePath = new FilePath(build.getRootDir()).createTempFile(getClass().getSimpleName(), null);
			this.buildStarted = build.getStartTimeInMillis();
			this.workspace = BuildHandlerUtils.getWorkspace(build);//build.getExecutor().getCurrentWorkspace();//build.getWorkspace();
			this.stripPackageAndClass = stripPackageAndClass;
			this.hpRunnerType = hpRunnerType;
			this.jenkinsRootUrl = jenkinsRootUrl;
			this.buildRootDir = build.getRootDir().getCanonicalPath();
			this.sharedCheckOutDirectory = CheckOutSubDirEnvContributor.getSharedCheckOutDirectory(build.getParent());

			//AbstractProject project = (AbstractProject)build.getParent();/*build.getProject()*/;
			this.jobName =build.getParent().getName();// project.getName();
			this.buildId = BuildHandlerUtils.getBuildId(build);///*build.getProject()*/((AbstractProject)build.getParent()).getBuilds().getLastBuild().getId();
			moduleDetection =Arrays.asList(
					new MavenBuilderModuleDetection(build),
					new MavenSetModuleDetection(build),
					new ModuleDetection.Default());


			if(HPRunnerType.UFT.equals(hpRunnerType)){

				//extract folder names for created tests
				String reportFolder = buildRootDir + "/archive/UFTReport";
				Set<String> testFolderNames = new HashSet<>();
				File reportFolderFile = new File(reportFolder);
				if (reportFolderFile.exists()) {
					File[] children = reportFolderFile.listFiles();
					if (children != null) {
						for (File child : children) {
							testFolderNames.add(child.getName());
						}
					}
				}
				additionalContext = testFolderNames;
			}
		}

		@Override
		public FilePath invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
			OutputStream os = filePath.write();
			BufferedOutputStream bos = new BufferedOutputStream(os);
			ObjectOutputStream oos = new ObjectOutputStream(bos);

			try {
				for (FilePath report : reports) {
					JUnitXmlIterator iterator = new JUnitXmlIterator(report.read(), moduleDetection, workspace, sharedCheckOutDirectory, jobName, buildId, buildStarted, stripPackageAndClass, hpRunnerType, jenkinsRootUrl, additionalContext);
					while (iterator.hasNext()) {
						oos.writeObject(iterator.next());
					}
				}
			} catch (XMLStreamException e) {
				throw new IOException(e);
			}
			os.flush();

			oos.close();
			return filePath;
		}

		@Override
		public void checkRoles(RoleChecker roleChecker) throws SecurityException {
			roleChecker.check(this, Role.UNKNOWN);
		}
	}

	/*
	 * To be used in tests only.
	 */
	public void _setResultFieldsDetectionService(ResultFieldsDetectionService detectionService) {
		this.resultFieldsDetectionService = detectionService;
	}
}
