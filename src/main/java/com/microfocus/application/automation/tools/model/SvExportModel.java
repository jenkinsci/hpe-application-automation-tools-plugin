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

package com.microfocus.application.automation.tools.model;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

public class SvExportModel extends AbstractSvRunModel {

    private final String targetDirectory;
    private final boolean cleanTargetDirectory;
    private final boolean switchToStandByFirst;
    private final boolean archive;

    @DataBoundConstructor
    public SvExportModel(String serverName, boolean force, String targetDirectory, boolean cleanTargetDirectory,
                         SvServiceSelectionModel serviceSelection, boolean switchToStandByFirst, boolean archive) {
        super(serverName, force, serviceSelection);
        this.targetDirectory = StringUtils.trim(targetDirectory);
        this.cleanTargetDirectory = cleanTargetDirectory;
        this.switchToStandByFirst = switchToStandByFirst;
        this.archive = archive;
    }

    public String getTargetDirectory() {
        return (StringUtils.isNotBlank(targetDirectory)) ? targetDirectory : null;
    }

    public boolean isCleanTargetDirectory() {
        return cleanTargetDirectory;
    }

    public boolean isSwitchToStandByFirst() {
        return switchToStandByFirst;
    }

    public boolean isArchive() {
        return archive;
    }
}