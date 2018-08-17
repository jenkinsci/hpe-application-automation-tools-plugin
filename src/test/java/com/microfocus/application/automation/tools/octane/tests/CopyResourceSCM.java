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

package com.microfocus.application.automation.tools.octane.tests;// (C) Copyright 2003-2015 Hewlett-Packard Development Company, L.P.

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.scm.NullSCM;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.IOException;

public class CopyResourceSCM extends NullSCM {

    private String path;
    private String targetPath;

    public CopyResourceSCM(String path) {
        this(path, "");
    }

    public CopyResourceSCM(String path, String targetPath) {
        this.path = path;
        this.targetPath = targetPath;
    }

    @Override
    public boolean checkout(AbstractBuild<?,?> build, Launcher launcher, FilePath workspace, BuildListener listener, File changeLogFile) throws IOException, InterruptedException {
        if (workspace.exists()) {
            listener.getLogger().println("Deleting existing workspace " + workspace.getRemote());
            workspace.deleteRecursive();
        }
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath*:" + path + "/**");
        for (Resource resource : resources) {
            if (resource.exists() && resource.isReadable()) {
                String urlString = resource.getURL().toExternalForm();
                String targetName = urlString.substring(urlString.indexOf(path) + path.length());
                byte[] fileContent = IOUtils.toByteArray(resource.getInputStream());
                FileUtils.writeByteArrayToFile(new File(new File(workspace.getRemote(), targetPath), targetName), fileContent);
            }
        }
        return true;
    }
}
