/*
 * © Copyright 2013 EntIT Software LLC
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright (c) 2018 Micro Focus Company, L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ___________________________________________________________________
 *
 */

package com.microfocus.application.automation.tools.octane.tests;

import com.google.inject.Inject;
import com.microfocus.application.automation.tools.octane.ResultQueue;
import com.microfocus.application.automation.tools.octane.model.processors.projects.JobProcessorFactory;
import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import com.microfocus.application.automation.tools.octane.tests.detection.UFTExtension;
import com.microfocus.application.automation.tools.octane.tests.xml.TestResultXmlWriter;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
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
@SuppressWarnings({"squid:S2699", "squid:S3658", "squid:S2259", "squid:S1872"})
public class TestListener {
	private static Logger logger = LogManager.getLogger(TestListener.class);

	private static final String JENKINS_STORMRUNNER_LOAD_TEST_RUNNER_CLASS = "com.hpe.sr.plugins.jenkins.StormTestRunner";
	private static final String JENKINS_STORMRUNNER_FUNCTIONAL_TEST_RUNNER_CLASS = "com.hpe.application.automation.tools.srf.run.RunFromSrfBuilder";
	private static final String JENKINS_PERFORMANCE_CENTER_TEST_RUNNER_CLASS = "com.hpe.application.automation.tools.run.PcBuilder";
	public static final String TEST_RESULT_FILE = "mqmTests.xml";

	private ResultQueue queue;

	public boolean processBuild(Run run) {

		FilePath resultPath = new FilePath(new FilePath(run.getRootDir()), TEST_RESULT_FILE);
		TestResultXmlWriter resultWriter = new TestResultXmlWriter(resultPath, run);
		boolean success = true;
		boolean hasTests = false;
		String jenkinsRootUrl = Jenkins.getInstance().getRootUrl();
		HPRunnerType hpRunnerType = HPRunnerType.NONE;
		List<Builder> builders = JobProcessorFactory.getFlowProcessor(run.getParent()).tryGetBuilders();
		if (builders != null) {
			for (Builder builder : builders) {
				if (builder.getClass().getName().equals(JENKINS_STORMRUNNER_LOAD_TEST_RUNNER_CLASS)) {
					hpRunnerType = HPRunnerType.StormRunnerLoad;
					break;
				}
				if (builder.getClass().getName().equals(JENKINS_STORMRUNNER_FUNCTIONAL_TEST_RUNNER_CLASS)) {
					hpRunnerType = HPRunnerType.StormRunnerFunctional;
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
			for (OctaneTestsExtension ext : OctaneTestsExtension.all()) {
				if (ext.supports(run)) {
					List<Run> buildsList = BuildHandlerUtils.getBuildPerWorkspaces(run);
					for (Run buildX : buildsList) {
						TestResultContainer testResultContainer = ext.getTestResults(buildX, hpRunnerType, jenkinsRootUrl);
						if (testResultContainer != null && testResultContainer.getIterator().hasNext()) {
							resultWriter.writeResults(testResultContainer);
							hasTests = true;
						}
					}
				}
			}
		} catch (Throwable t) {
			success = false;
			logger.error("failed to process test results", t);
		} finally {
			try {
				resultWriter.close();
				if (success && hasTests) {
					String projectFullName = BuildHandlerUtils.getProjectFullName(run);
					if (projectFullName != null) {
						queue.add(projectFullName, run.getNumber());
					}
				}
			} catch (XMLStreamException xmlse) {
				success = false;
				logger.error("failed to finalize test results processing", xmlse);
			}
		}
		return success && hasTests;//test results expected
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
