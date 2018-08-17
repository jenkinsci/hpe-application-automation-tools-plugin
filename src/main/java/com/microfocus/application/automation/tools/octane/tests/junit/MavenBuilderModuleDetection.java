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

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Project;
import hudson.model.Run;
import hudson.tasks.Builder;
import hudson.tasks.Maven;

import java.io.File;

public class MavenBuilderModuleDetection extends AbstractMavenModuleDetection {

    public MavenBuilderModuleDetection(Run build) {
        super(build);
    }

    protected void addPomDirectories(Run build) {
        if (build instanceof AbstractBuild) {

            if (((AbstractBuild)build).getProject() instanceof FreeStyleProject ||
                    "hudson.matrix.MatrixConfiguration".equals(((AbstractBuild)build).getProject().getClass().getName())) {
                boolean unknownBuilder = false;
                for (Builder builder : ((Project<?, ?>) ((AbstractBuild)build).getProject()).getBuilders()) {
                    if (builder instanceof Maven) {
                        Maven maven = (Maven) builder;
                        if (maven.pom != null) {
                            if (maven.pom.endsWith("/pom.xml") || maven.pom.endsWith("\\pom.xml")) {
                                addPomDirectory(new FilePath(rootDir, maven.pom.substring(0, maven.pom.length() - 8)));
                                continue;
                            } else {
                                int p = maven.pom.lastIndexOf(File.separatorChar);
                                if (p > 0) {
                                    addPomDirectory(new FilePath(rootDir, maven.pom.substring(0, p)));
                                    continue;
                                }
                            }
                        }
                        addPomDirectory(rootDir);
                    } else {
                        unknownBuilder = true;
                    }
                }
                if (unknownBuilder && !pomDirs.contains(rootDir)) {
                    // attempt to support shell and batch executions too
                    // simply assume there is top-level pom file for any non-maven builder
                    addPomDirectory(rootDir);
                }
            }
        }
    }
}
