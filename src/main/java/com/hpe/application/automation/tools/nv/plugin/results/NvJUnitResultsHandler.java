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

import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.junit.JUnitParser;
import hudson.tasks.junit.TestResult;
import com.hpe.application.automation.tools.nv.model.NvNetworkProfile;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class NvJUnitResultsHandler {
    private final String JSON_FILE_NAME = "nvResult.json";

    private String reportFilesPattern;

    private JUnitParser parser;
    private NvResultsBuilder aggregator;
    private ObjectMapper objectMapper;

    public NvJUnitResultsHandler(Map<String, Float> thresholdsMap, String reportFilesPattern) {
        this.reportFilesPattern = reportFilesPattern;

        parser = new JUnitParser(true, true);
        aggregator = new NvResultsBuilder(thresholdsMap);
        objectMapper = new ObjectMapper();
    }

    public void handle(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, NvNetworkProfile profile) throws IOException, InterruptedException {
        TestResult testResult = parser.parseResult(reportFilesPattern, build, build.getWorkspace(), launcher, listener);
        aggregator.aggregate(testResult, profile);
    }

    public void finalizeResults(AbstractBuild<?, ?> build) throws IOException, InterruptedException {
        NvJUnitResult result = aggregator.finalizeResults();
        writeResultToFile(build.getRootDir(), result);
        mergeResults(build, result);
    }

    private void writeResultToFile(File rootDir, NvJUnitResult result) throws IOException {
        File resultFile = new File(rootDir, JSON_FILE_NAME);
        objectMapper.writeValue(resultFile, result);
    }

    private void mergeResults(AbstractBuild<?, ?> build, NvJUnitResult result) throws IOException, InterruptedException {
        NvResultsMerger.mergeResults(build, reportFilesPattern, result);
    }
}
