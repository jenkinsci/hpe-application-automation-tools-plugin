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
