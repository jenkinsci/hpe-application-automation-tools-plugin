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