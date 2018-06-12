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
import com.hpe.application.automation.tools.octane.actions.dto.OctaneStatus;
import com.hpe.application.automation.tools.octane.actions.dto.ScmResourceFile;
import com.hpe.application.automation.tools.octane.actions.dto.SupportsOctaneStatus;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This file represents result of UFT detection files (tests and data tables)
 */
@XmlRootElement(name = "detectionResult")
@XmlAccessorType(XmlAccessType.FIELD)
public class UFTTestDetectionResult {


    @XmlElementWrapper(name = "tests")
    @XmlElement(name = "test")
    private List<AutomatedTest> tests = new ArrayList<>();

    @XmlElementWrapper(name = "dataTables")
    @XmlElement(name = "dataTable")
    private List<ScmResourceFile> scmResourceFiles = new ArrayList<>();

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

    private List<AutomatedTest> getTestByOctaneStatus(OctaneStatus status) {
        List<AutomatedTest> filtered = new ArrayList<>();
        for (AutomatedTest test : tests) {
            if (test.getOctaneStatus().equals(status)) {
                filtered.add(test);
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    private List<ScmResourceFile> getResourceFilesByOctaneStatus(OctaneStatus status) {
        List<ScmResourceFile> filtered = new ArrayList<>();
        for (ScmResourceFile file : scmResourceFiles) {
            if (file.getOctaneStatus().equals(status)) {
                filtered.add(file);
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    public List<AutomatedTest> getNewTests() {
        return getTestByOctaneStatus(OctaneStatus.NEW);
    }

    public List<AutomatedTest> getDeletedTests() {
        return getTestByOctaneStatus(OctaneStatus.DELETED);
    }

    public List<AutomatedTest> getUpdatedTests() {
        return getTestByOctaneStatus(OctaneStatus.MODIFIED);
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
        return !getAllScmResourceFiles().isEmpty() || !getAllTests().isEmpty() || !getDeletedFolders().isEmpty();
    }

    public List<ScmResourceFile> getNewScmResourceFiles() {
        return getResourceFilesByOctaneStatus(OctaneStatus.NEW);
    }

    public List<ScmResourceFile> getDeletedScmResourceFiles() {
        return getResourceFilesByOctaneStatus(OctaneStatus.DELETED);
    }

    public List<ScmResourceFile> getUpdatedScmResourceFiles() {
        return getResourceFilesByOctaneStatus(OctaneStatus.MODIFIED);
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

    public List<AutomatedTest> getAllTests() {
        return tests;
    }

    public List<ScmResourceFile> getAllScmResourceFiles() {
        return scmResourceFiles;
    }

    public static int countItemsWithStatus(OctaneStatus status, List<? extends SupportsOctaneStatus> items) {

        int count = 0;
        for (SupportsOctaneStatus item : items) {
            if (item.getOctaneStatus().equals(status)) {
                count++;
            }
        }
        return count;
    }
}