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

package com.microfocus.application.automation.tools.octane.tests.gherkin;

import com.microfocus.application.automation.tools.octane.actions.cucumber.CucumberTestResultsAction;
import com.microfocus.application.automation.tools.octane.tests.HPRunnerType;
import com.microfocus.application.automation.tools.octane.tests.OctaneTestsExtension;
import com.microfocus.application.automation.tools.octane.tests.TestProcessingException;
import com.microfocus.application.automation.tools.octane.tests.TestResultContainer;
import com.microfocus.application.automation.tools.octane.tests.testResult.TestResult;
import hudson.Extension;
import hudson.model.Run;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;

@Extension
public class GherkinTestExtention extends OctaneTestsExtension {
    private static Logger logger = LogManager.getLogger(GherkinTestExtention.class);

    @Override
    public boolean supports(Run<?, ?> build) throws IOException, InterruptedException {
        if (build.getAction(CucumberTestResultsAction.class) != null) {
            logger.debug("CucumberTestResultsAction found, gherkin results expected");
            return true;
        } else {
            logger.debug("CucumberTestResultsAction not found, no gherkin results expected");
            return false;
        }
    }

    @Override
    public TestResultContainer getTestResults(Run<?, ?> build, HPRunnerType hpRunnerType, String jenkinsRootUrl) throws
            TestProcessingException, IOException, InterruptedException {
        try {
            List<TestResult> testResults = GherkinTestResultsCollector.collectGherkinTestsResults(build.getRootDir());
            return new TestResultContainer(testResults.iterator(), null);
        } catch (IOException e) {
            throw e;
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new TestProcessingException("Error while processing gherkin test results", e);
        }
    }
}
