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
package com.hpe.application.automation.tools.octane.executor.scmmanager;


import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.executor.TestConnectivityInfo;
import com.hp.octane.integrations.dto.scm.SCMRepository;
import com.hpe.application.automation.tools.common.HttpStatus;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Job;
import hudson.scm.SCM;
import hudson.scm.SubversionSCM;
import org.apache.commons.lang.StringUtils;
import org.tmatesoft.svn.core.SVNException;

import java.io.File;
import java.io.IOException;

public class SvnPluginHandler implements ScmPluginHandler {

    @Override
    public void setScmRepositoryInJob(SCMRepository scmRepository, String scmRepositoryCredentialsId, FreeStyleProject proj, boolean executorJob) throws IOException {

        String relativeCheckOut = null;
        if (executorJob) {
            String url = StringUtils.stripEnd(scmRepository.getUrl(), "/").replaceAll("[<>:\"/\\|?*]", "_");
            relativeCheckOut = "a\\..\\..\\..\\_test_sources\\" + url;
        }else{
            relativeCheckOut = "a" + File.separator + "..";//it doesn't has any affect on checkout folder. Without it, svn do checkout to folder of svn repository name
            //for example for URL https://myd-vm00812.hpeswlab.net/svn/uft_tests/ - tests will be cloned to "uft_tests" subfolder
            //adding a\.. will clone as is - Why we need it - subfolder is part of "package" reported to Octane
        }

        SubversionSCM svn = new SubversionSCM(scmRepository.getUrl(), scmRepositoryCredentialsId, relativeCheckOut);
        proj.setScm(svn);
    }

    @Override
    public String getSharedCheckOutDirectory(Job j) {
        SCM scm = ((FreeStyleProject) j).getScm();

        SubversionSCM svnScm = (SubversionSCM) scm;
        for (SubversionSCM.ModuleLocation location : svnScm.getLocations()) {
            if (StringUtils.isNotEmpty(location.getLocalDir())) {
                return location.getLocalDir();
            }
        }

        return null;
    }

    @Override
    public void checkRepositoryConnectivity(TestConnectivityInfo testConnectivityInfo, StandardCredentials credentials, OctaneResponse result) {

        String credentialsId = (credentials != null ? credentials.getId() : null);
        SubversionSCM svn = new SubversionSCM(testConnectivityInfo.getScmRepository().getUrl(), credentialsId);
        try {
            svn.getDescriptor().checkRepositoryPath((Item) null, svn.getLocations()[0].getSVNURL(), credentials);
            result.setStatus(HttpStatus.OK.getCode());
        } catch (SVNException e) {
            result.setStatus(HttpStatus.NOT_FOUND.getCode());
            result.setBody(e.getMessage());
        } catch (Exception e) {
            result.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
            result.setBody(e.getMessage());
        }
    }
}
