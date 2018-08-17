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

package com.microfocus.application.automation.tools.octane.tests.junit;

import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import hudson.FilePath;
import hudson.model.Run;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractMavenModuleDetection implements ModuleDetection {

    protected FilePath rootDir;
    protected List<FilePath> pomDirs;

    public AbstractMavenModuleDetection(Run build) {
        rootDir = BuildHandlerUtils.getWorkspace(build);
        pomDirs = new LinkedList<>();

        addPomDirectories(build);
    }

    protected abstract void addPomDirectories(Run build);

    @Override
    public String getModule(FilePath resultFile) throws IOException, InterruptedException {
        for (FilePath pomDir: pomDirs) {
            if (childOf(pomDir, resultFile)) {
                return normalize(locatePom(resultFile, pomDir));
            }
        }
        // unable to determine module
        return null;
    }

    protected void addPomDirectory(FilePath pomDir) {
        pomDirs.add(pomDir);
    }

    protected boolean childOf(FilePath parent, FilePath child) {
        while (child != null) {
            if (parent.equals(child)) {
                return true;
            }
            child = child.getParent();
        }
        return false;
    }


    private String locatePom(FilePath filePath, FilePath pomDir) throws IOException, InterruptedException {
        while (filePath != null) {
            FilePath parentPath = filePath.getParent();
            if (parentPath.equals(pomDir)) {
                // walk up as far as the enclosing pom directory
                break;
            }
            FilePath pomPath = new FilePath(parentPath, "pom.xml");
            if (pomPath.exists()) {
                // we found a nested pom directory
                return parentPath.getRemote().substring(rootDir.getRemote().length());
            }
            filePath = parentPath;
        }
        // no other pom found in nested directories
        return pomDir.getRemote().substring(rootDir.getRemote().length());
    }

    private String normalize(String path) {
        return path.replace("\\", "/").replaceFirst("^/", "");
    }
}
