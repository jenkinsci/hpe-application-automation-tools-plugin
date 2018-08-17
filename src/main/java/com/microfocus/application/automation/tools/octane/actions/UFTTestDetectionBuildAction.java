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

package com.microfocus.application.automation.tools.octane.actions;

import com.hp.octane.integrations.uft.items.OctaneStatus;
import com.hp.octane.integrations.uft.items.SupportsOctaneStatus;
import com.hp.octane.integrations.uft.items.UftTestDiscoveryResult;
import com.microfocus.application.automation.tools.octane.executor.UFTTestDetectionService;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Run;

import java.util.List;

/**
 * Class responsible to show report of  {@link UFTTestDetectionService}
 */
public class UFTTestDetectionBuildAction implements Action {
    private AbstractBuild<?, ?> build;


    private UftTestDiscoveryResult results;

    @Override
    public String getIconFileName() {
        return "notepad.png";
    }

    @Override
    public String getDisplayName() {
        return "ALM Octane UFT Tests Discovery Report";
    }

    @Override
    public String getUrlName() {
        return "uft_report";
    }

    @SuppressWarnings("squid:S1452")
    public final Run<?, ?> getBuild() {
        return build;
    }

    public UFTTestDetectionBuildAction(final AbstractBuild<?, ?> build, UftTestDiscoveryResult results) {
        this.build = build;
        this.results = results == null ? new UftTestDiscoveryResult() : results;
    }

    public UftTestDiscoveryResult getResults() {
        return results;
    }

    /**
     * used by ~\src\main\resources\com\hp\application\automation\tools\octane\actions\UFTTestDetectionBuildAction\index.jelly
     *
     * @return
     */
    public boolean getHasNewTests() {
        return countItemsWithStatus(OctaneStatus.NEW, results.getAllTests()) > 0;
    }

    /**
     * used by ~\src\main\resources\com\hp\application\automation\tools\octane\actions\UFTTestDetectionBuildAction\index.jelly
     *
     * @return
     */
    public boolean getHasDeletedTests() {
        return countItemsWithStatus(OctaneStatus.DELETED, results.getAllTests()) > 0;
    }

    /**
     * used by ~\src\main\resources\com\hp\application\automation\tools\octane\actions\UFTTestDetectionBuildAction\index.jelly
     *
     * @return
     */
    public boolean getHasUpdatedTests() {
        return countItemsWithStatus(OctaneStatus.MODIFIED, results.getAllTests()) > 0;
    }

    public boolean getHasQuotedPaths() {
        return results.isHasQuotedPaths();
    }

    /**
     * used by ~\src\main\resources\com\hp\application\automation\tools\octane\actions\UFTTestDetectionBuildAction\index.jelly
     *
     * @return
     */
    public boolean getHasNewScmResources() {
        return countItemsWithStatus(OctaneStatus.NEW, results.getAllScmResourceFiles()) > 0;
    }

    /**
     * used by ~\src\main\resources\com\hp\application\automation\tools\octane\actions\UFTTestDetectionBuildAction\index.jelly
     *
     * @return
     */
    public boolean getHasDeletedScmResources() {
        return countItemsWithStatus(OctaneStatus.DELETED, results.getAllScmResourceFiles()) > 0;
    }

    /**
     * used by ~\src\main\resources\com\hp\application\automation\tools\octane\actions\UFTTestDetectionBuildAction\index.jelly
     *
     * @return
     */
    public boolean getHasUpdatedScmResources() {
        return countItemsWithStatus(OctaneStatus.MODIFIED, results.getAllScmResourceFiles()) > 0;
    }

    /**
     * used by ~\src\main\resources\com\hp\application\automation\tools\octane\actions\UFTTestDetectionBuildAction\index.jelly
     *
     * @return
     */
    public boolean getHasDeletedFolders() {
        return results.getDeletedFolders().size() > 0;
    }


    public void setResults(UftTestDiscoveryResult results) {
        this.results = results;
    }

    private static int countItemsWithStatus(OctaneStatus status, List<? extends SupportsOctaneStatus> items) {

        int count = 0;
        for (SupportsOctaneStatus item : items) {
            if (item.getOctaneStatus().equals(status)) {
                count++;
            }
        }
        return count;
    }
}
