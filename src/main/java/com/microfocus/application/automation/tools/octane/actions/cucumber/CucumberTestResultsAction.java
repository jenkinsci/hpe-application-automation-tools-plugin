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
