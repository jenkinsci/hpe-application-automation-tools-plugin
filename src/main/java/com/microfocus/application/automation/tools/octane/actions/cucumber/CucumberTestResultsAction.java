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

package com.microfocus.application.automation.tools.octane.actions.cucumber;

import com.microfocus.application.automation.tools.octane.Messages;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;

import java.io.File;

/**
 * Created by franksha on 07/12/2016.
 */
public class CucumberTestResultsAction implements Action {
    private final String glob;
    private final AbstractBuild build;

    CucumberTestResultsAction(AbstractBuild build, String glob, BuildListener listener) {
        this.build = build;
        this.glob = glob;
        CucumberResultsService.setListener(listener);
    }

    public boolean copyResultsToBuildFolder() {
        try {
            CucumberResultsService.log(Messages.CucumberResultsActionCollecting());
            String[] files = CucumberResultsService.getCucumberResultFiles(build.getWorkspace(), glob);
            boolean found = files.length > 0;

            for (String fileName : files) {
                File resultFile = new File(build.getWorkspace().child(fileName).toURI());
                CucumberResultsService.copyResultFile(resultFile, build.getRootDir(), build.getWorkspace());
            }

            if (!found && build.getResult() != Result.FAILURE) {
                // most likely a configuration error in the job - e.g. false pattern to match the cucumber result files
                CucumberResultsService.log(Messages.CucumberResultsActionNotFound());
            }  // else , if results weren't found but build result is failure - most likely a build failed before us. don't report confusing error message.

            return found;

        } catch (Exception e) {
            CucumberResultsService.log(Messages.CucumberResultsActionError(), e.toString());
            return false;
        }
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return null;
    }
}
