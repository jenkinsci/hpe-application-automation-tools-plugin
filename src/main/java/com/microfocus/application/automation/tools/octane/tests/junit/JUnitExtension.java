/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2019 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors ("Micro Focus") are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.octane.tests.junit;

import com.google.inject.Inject;
import com.microfocus.application.automation.tools.octane.actions.cucumber.CucumberTestResultsAction;
import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import com.microfocus.application.automation.tools.octane.executor.CheckOutSubDirEnvContributor;
import com.microfocus.application.automation.tools.octane.model.processors.projects.JobProcessorFactory;
import com.microfocus.application.automation.tools.octane.tests.HPRunnerType;
import com.microfocus.application.automation.tools.octane.tests.OctaneTestsExtension;
import com.microfocus.application.automation.tools.octane.tests.TestResultContainer;
import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import com.microfocus.application.automation.tools.octane.tests.detection.MFToolsDetectionExtension;
import com.microfocus.application.automation.tools.octane.tests.detection.ResultFields;
import com.microfocus.application.automation.tools.octane.tests.detection.ResultFieldsDetectionService;
import com.microfocus.application.automation.tools.octane.tests.impl.ObjectStreamIterator;
import hudson.Extension;
import hudson.FilePath;
import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.Run;
import hudson.remoting.VirtualChannel;
import hudson.tasks.test.AbstractTestResultAction;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Converter of Jenkins test report to ALM Octane test report format(junitResult.xml->mqmTests.xml)
 */
@Extension
public class JUnitExtension extends OctaneTestsExtension {
	private static Logger logger = SDKBasedLoggerProvider.getLogger(JUnitExtension.class);

	private static final String JUNIT_RESULT_XML = "junitResult.xml"; // NON-NLS
	public static final String TEMP_TEST_RESULTS_FILE_NAME_PREFIX = "GetJUnitTestResults";

	@Inject
	private ResultFieldsDetectionService resultFieldsDetectionService;

	public boolean supports(Run<?, ?> build) {
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
	public TestResultContainer getTestResults(Run<?, ?> run, String jenkinsRootUrl) throws IOException, InterruptedException {
		logger.debug("Collecting JUnit results");

		FilePath resultFile = new FilePath(run.getRootDir()).child(JUNIT_RESULT_XML);
		if (resultFile.exists()) {
			logger.debug("JUnit result report found");
			FilePath workspace = BuildHandlerUtils.getWorkspace(run);
			if (workspace == null) {
				logger.error("Received null workspace : " + run);
				return null;
			}

			FilePath filePath = workspace.act(new GetJUnitTestResults(run, Collections.singletonList(resultFile), false, jenkinsRootUrl));
			ResultFields detectedFields = getResultFields(run);
			return new TestResultContainer(new ObjectStreamIterator<>(filePath), detectedFields);
		} else {
			//avoid java.lang.NoClassDefFoundError when maven plugin is not present
			if ("hudson.maven.MavenModuleSetBuild".equals(run.getClass().getName())) {
				logger.debug("MavenModuleSetBuild detected, looking for results in maven modules");

				List<FilePath> resultFiles = new LinkedList<>();
				Map<MavenModule, MavenBuild> moduleLastBuilds = ((MavenModuleSetBuild) run).getModuleLastBuilds();
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
					ResultFields detectedFields = getResultFields(run);
					FilePath filePath = BuildHandlerUtils.getWorkspace(run).act(new GetJUnitTestResults(run, resultFiles, false, jenkinsRootUrl));
					return new TestResultContainer(new ObjectStreamIterator<>(filePath), detectedFields);
				}
			}
			logger.debug("No JUnit result report found");
			return null;
		}
	}

	private ResultFields getResultFields(Run<?, ?> build) throws InterruptedException {
		return resultFieldsDetectionService.getDetectedFields(build);
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

		public GetJUnitTestResults(Run<?, ?> build, List<FilePath> reports, boolean stripPackageAndClass, String jenkinsRootUrl) throws IOException, InterruptedException {
			this.reports = reports;
			this.filePath = new FilePath(build.getRootDir()).createTempFile(TEMP_TEST_RESULTS_FILE_NAME_PREFIX, null);
			this.buildStarted = build.getStartTimeInMillis();
			this.workspace = BuildHandlerUtils.getWorkspace(build);
			this.stripPackageAndClass = stripPackageAndClass;
			this.hpRunnerType = MFToolsDetectionExtension.getRunnerType(build);
			this.jenkinsRootUrl = jenkinsRootUrl;
			String buildRootDir = build.getRootDir().getCanonicalPath();
			this.sharedCheckOutDirectory = CheckOutSubDirEnvContributor.getSharedCheckOutDirectory(build.getParent());

			this.jobName = JobProcessorFactory.getFlowProcessor(build.getParent()).getTranslatedJobName();
			this.buildId = build.getId();
			moduleDetection = Arrays.asList(
					new MavenBuilderModuleDetection(build),
					new MavenSetModuleDetection(build),
					new ModuleDetection.Default());


			if (HPRunnerType.UFT.equals(hpRunnerType)) {

				//extract folder names for created tests
				String reportFolder = buildRootDir + "/archive/UFTReport";
				List<String> testFolderNames = new ArrayList<>();
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
			if (HPRunnerType.StormRunnerLoad.equals(hpRunnerType)) {
				try {
					File file = new File(build.getRootDir(), "log");
					Path path = Paths.get(file.getPath());
					additionalContext = Files.readAllLines(path, StandardCharsets.UTF_8);
				} catch (Exception e) {
					logger.error("Failed to add log file for StormRunnerLoad :" + e.getMessage());
				}
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
