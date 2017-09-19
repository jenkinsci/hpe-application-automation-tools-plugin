/*
    (c) Copyright [2016] Hewlett Packard Enterprise Development LP

    Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
    documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
    rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
    persons to whom the Software is furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
    Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
    WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
    COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
    OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.hpe.application.automation.tools.nv.plugin.results;

import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestResult;
import com.hpe.application.automation.tools.nv.model.NvNetworkProfile;

import java.util.Map;

public class NvResultsBuilder {
    private static final String ERROR_MESSAGE = "Failed by HPE Network Virtualization. Reason: duration (%s) > threshold (%s)";
    private static final String ASSERTION_EXCEPTION_CLASS_NAME = AssertionError.class.getName();

    private Map<String, Float> thresholdsMap;
    private NvJUnitResult result = new NvJUnitResult();
    private boolean finalized;

    public NvResultsBuilder(Map<String, Float> thresholdsMap) {
        this.thresholdsMap = thresholdsMap;
    }

    public void aggregate(TestResult testResult, NvNetworkProfile profile) {
        result.getProfiles().add(profile.toDTO());

        Float threshold;
        for (SuiteResult suiteResult : testResult.getSuites()) {
            for (CaseResult caseResult : suiteResult.getCases()) {
                NvTestCaseResult testCaseResult = new NvTestCaseResult(caseResult.getName());
                testCaseResult.setDuration(caseResult.getDuration());
                threshold = getThreshold(caseResult.getClassName(), caseResult.getName());
                if (null != threshold && caseResult.isPassed()) {
                    testCaseResult.setThreshold(threshold);
                    if (caseResult.getDuration() > threshold) {
                        testCaseResult.setFailed();
                        testCaseResult.setErrorMessage(String.format(ERROR_MESSAGE, caseResult.getDuration(), threshold));
                        testCaseResult.setErrorStackTrace("");
                    } else {
                        testCaseResult.setPassed();
                    }
                } else {
                    if (caseResult.isFailed()) {
                        if (checkFailedException(caseResult.getErrorStackTrace())) {
                            testCaseResult.setFailed();
                        } else {
                            testCaseResult.setError();
                        }
                        testCaseResult.setErrorMessage(caseResult.getErrorDetails());
                        testCaseResult.setErrorStackTrace(caseResult.getErrorStackTrace());
                    } else if (caseResult.isSkipped()) {
                        testCaseResult.setSkipped();
                    } else {
                        testCaseResult.setPassed();
                    }
                }

                NvProfileResult profileResult = getTestCaseParent(suiteResult.getName(), caseResult.getClassName(), profile.getProfileName());
                profileResult.addResult(testCaseResult);
            }
        }
    }

    public NvJUnitResult finalizeResults() {
        if (!finalized) {
            result.tally();
            finalized = true;
        }

        return result;
    }

    private NvProfileResult getTestCaseParent(String testSuiteName, String className, String profileName) {
        NvTestSuiteResult nvTestSuiteResult = result.getNvTestSuiteResult(testSuiteName);
        if (null == nvTestSuiteResult) {
            nvTestSuiteResult = new NvTestSuiteResult(testSuiteName);
            result.addNvTestSuiteResult(nvTestSuiteResult);
            NvClassResult nvClassResult = new NvClassResult(className);
            nvTestSuiteResult.addResult(nvClassResult);
            NvProfileResult nvProfileResult = new NvProfileResult(profileName);
            nvClassResult.addResult(nvProfileResult);
            return nvProfileResult;
        }

        NvClassResult nvClassResult = nvTestSuiteResult.getResult(className);
        if (null == nvClassResult) {
            nvClassResult = new NvClassResult(className);
            nvTestSuiteResult.addResult(nvClassResult);
            NvProfileResult nvProfileResult = new NvProfileResult(profileName);
            nvClassResult.addResult(nvProfileResult);
            return nvProfileResult;
        }

        NvProfileResult nvProfileResult = nvClassResult.getResult(profileName);
        if (null == nvProfileResult) {
            nvProfileResult = new NvProfileResult(profileName);
            nvClassResult.addResult(nvProfileResult);
            return nvProfileResult;
        }

        return nvProfileResult;
    }

    private Float getThreshold(String className, String testCaseName) {
        if (null == thresholdsMap) {
            return null;
        } else {
            Float threshold = thresholdsMap.get(className + "." + testCaseName);
            if(null == threshold) { // fetch default threshold if exists, null otherwise
                threshold = thresholdsMap.get("default");
            }
            return threshold;
        }
    }

    private boolean checkFailedException(String stackTrace) {
        return stackTrace.startsWith(ASSERTION_EXCEPTION_CLASS_NAME);
    }
}
