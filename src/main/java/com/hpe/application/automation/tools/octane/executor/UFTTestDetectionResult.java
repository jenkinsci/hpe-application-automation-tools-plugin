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

package com.hpe.application.automation.tools.octane.executor;

import com.hpe.application.automation.tools.octane.actions.dto.AutomatedTest;
import com.hpe.application.automation.tools.octane.actions.dto.ScmResourceFile;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This file represents result of UFT detection files (tests and data tables)
 */
@XmlRootElement(name = "detectionResult")
@XmlAccessorType(XmlAccessType.FIELD)
public class UFTTestDetectionResult {


    @XmlElementWrapper(name = "newTests")
    @XmlElement(name = "test")
    private List<AutomatedTest> newTests = new ArrayList<>();

    @XmlElementWrapper(name = "deletedTests")
    @XmlElement(name = "test")
    private List<AutomatedTest> deletedTests = new ArrayList<>();

    @XmlElementWrapper(name = "updatedTests")
    @XmlElement(name = "test")
    private List<AutomatedTest> updatedTests = new ArrayList<>();

    @XmlElementWrapper(name = "newDataTables")
    @XmlElement(name = "dataTable")
    private List<ScmResourceFile> newScmResourceFiles = new ArrayList<>();

    @XmlElementWrapper(name = "deletedDataTables")
    @XmlElement(name = "dataTable")
    private List<ScmResourceFile> deletedScmResourceFiles = new ArrayList<>();

    @XmlElementWrapper(name = "updatedDataTables")
    @XmlElement(name = "dataTable")
    private List<ScmResourceFile> updatedScmResourceFiles = new ArrayList<>();

    @XmlElementWrapper(name = "deletedFolders")
    @XmlElement(name = "folder")
    private List<String> deletedFolders = new ArrayList<>();

    @XmlAttribute
    private String scmRepositoryId;

    @XmlAttribute
    private String workspaceId;

    @XmlAttribute
    private boolean fullScan;

    @XmlAttribute
    private boolean hasQuotedPaths;

    public List<AutomatedTest> getNewTests() {
        return newTests;
    }

    public List<AutomatedTest> getDeletedTests() {
        return deletedTests;
    }

    public List<AutomatedTest> getUpdatedTests() {
        return updatedTests;
    }

    public String getScmRepositoryId() {
        return scmRepositoryId;
    }

    public void setScmRepositoryId(String scmRepositoryId) {
        this.scmRepositoryId = scmRepositoryId;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public boolean isFullScan() {
        return fullScan;
    }

    public void setFullScan(boolean fullScan) {
        this.fullScan = fullScan;
    }

    public boolean hasChanges() {
        return !getNewTests().isEmpty() || !getUpdatedTests().isEmpty() || !getDeletedTests().isEmpty()
                || !getNewScmResourceFiles().isEmpty() || !getDeletedScmResourceFiles().isEmpty();
    }

    public List<ScmResourceFile> getNewScmResourceFiles() {
        return newScmResourceFiles;
    }

    public List<ScmResourceFile> getDeletedScmResourceFiles() {
        return deletedScmResourceFiles;
    }

    public List<ScmResourceFile> getUpdatedScmResourceFiles() {
        return updatedScmResourceFiles;
    }

    public boolean isHasQuotedPaths() {
        return hasQuotedPaths;
    }

    public void setHasQuotedPaths(boolean hasQuotedPaths) {
        this.hasQuotedPaths = hasQuotedPaths;
    }

    public List<String> getDeletedFolders() {
        return deletedFolders;
    }

    public void setDeletedFolders(List<String> deletedFolders) {
        this.deletedFolders = deletedFolders;
    }
}