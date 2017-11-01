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

package com.hpe.application.automation.tools.octane.tests;

import com.google.inject.Inject;
import com.hpe.application.automation.tools.octane.ResultQueue;
import com.hpe.application.automation.tools.octane.model.processors.projects.JobProcessorFactory;
import com.hpe.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import com.hpe.application.automation.tools.octane.tests.detection.UFTExtension;
import com.hpe.application.automation.tools.octane.tests.xml.TestResultXmlWriter;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.stream.XMLStreamException;
import java.util.List;

/**
 * Jenkins events life cycle listener for processing test results on build completed
 */
@Extension
@SuppressWarnings({"squid:S2699","squid:S3658","squid:S2259","squid:S1872"})
public class TestListener {
	private static Logger logger = LogManager.getLogger(TestListener.class);

	public static final String TEST_RESULT_FILE = "mqmTests.xml";
	public static final String JENKINS_STORM_TEST_RUNNER_CLASS = "com.hpe.sr.plugins.jenkins.StormTestRunner";
	public static final String JENKINS_PERFORMANCE_CENTER_TEST_RUNNER_CLASS = "com.hpe.application.automation.tools.run.PcBuilder";


	private ResultQueue queue;

	public void processBuild(Run build, TaskListener listener) {

		FilePath resultPath = new FilePath(new FilePath(build.getRootDir()), TEST_RESULT_FILE);
		TestResultXmlWriter resultWriter = new TestResultXmlWriter(resultPath, build);
		boolean success = false;
		boolean hasTests = false;
		String jenkinsRootUrl = Jenkins.getInstance().getRootUrl();
		HPRunnerType hpRunnerType = HPRunnerType.NONE;
		List<Builder> builders = JobProcessorFactory.getFlowProcessor(build.getParent()).tryGetBuilders();
		if (builders != null) {
			for (Builder builder : builders) {
				if (builder.getClass().getName().equals(JENKINS_STORM_TEST_RUNNER_CLASS)) {
					hpRunnerType = HPRunnerType.StormRunner;
					break;
				}
				if (builder.getClass().getName().equals(UFTExtension.RUN_FROM_FILE_BUILDER) || builder.getClass().getName().equals(UFTExtension.RUN_FROM_ALM_BUILDER)) {
					hpRunnerType = HPRunnerType.UFT;
					break;
				}
				if (builder.getClass().getName().equals(JENKINS_PERFORMANCE_CENTER_TEST_RUNNER_CLASS)) {
					hpRunnerType = HPRunnerType.PerformanceCenter;
					break;
				}
			}
		}

		try {
			for (MqmTestsExtension ext : MqmTestsExtension.all()) {
				try {
					if (ext.supports(build)) {

						List<Run> buildsList = BuildHandlerUtils.getBuildPerWorkspaces(build);
						for(Run buildX : buildsList){
							TestResultContainer testResultContainer = ext.getTestResults(buildX, hpRunnerType, jenkinsRootUrl);
							if (testResultContainer != null && testResultContainer.getIterator().hasNext()) {
								resultWriter.writeResults(testResultContainer);
								hasTests = true;
							}
						}
					}
				} catch (IllegalArgumentException e) {
					listener.error(e.getMessage());
					if (!build.getResult().isWorseOrEqualTo(Result.UNSTABLE)) {
						build.setResult(Result.UNSTABLE);
					}
					return;
				} catch (InterruptedException ie) {
					logger.error("Interrupted processing test results in " + ext.getClass().getName(), ie);
					Thread.currentThread().interrupt();
					return;
				} catch (Exception e) {
					// extensibility involved: catch both checked and RuntimeExceptions
					logger.error("Error processing test results in " + ext.getClass().getName(), e);
					return;
				}
			}
			success = true;
		} finally {
			try {
				resultWriter.close();
				if (success && hasTests) {
					String projectFullName = BuildHandlerUtils.getProjectFullName(build);
					if (projectFullName != null) {
						queue.add(projectFullName, build.getNumber());
					}
				}
			} catch (XMLStreamException xmlse) {
				logger.error("Error processing test results", xmlse);
			}
		}
	}

	@Inject
	public void setTestResultQueue(TestsResultQueue queue) {
		this.queue = queue;
	}

	/*
	 * To be used in tests only.
	 */
	public void _setTestResultQueue(ResultQueue queue) {
		this.queue = queue;
	}
}
