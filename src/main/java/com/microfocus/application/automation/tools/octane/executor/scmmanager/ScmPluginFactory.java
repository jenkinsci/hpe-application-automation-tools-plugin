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
package com.microfocus.application.automation.tools.octane.executor.scmmanager;

import com.hp.octane.integrations.dto.scm.SCMType;
import hudson.PluginWrapper;
import jenkins.model.Jenkins;

public class ScmPluginFactory {

    private static ScmPluginHandler gitPluginHandler = null;
    private static ScmPluginHandler svnPluginHandler = null;


    public static ScmPluginHandler getScmHandler(SCMType scmType) {
        if (scmType.GIT.equals(scmType)) {
            if (gitPluginHandler == null) {
                /*try {
                    Class theClass = Class.forName("com.hpe.application.automation.tools.octane.executor.scmmanager.GitPluginHandler");
                    gitPluginHandler = (ScmPluginHandler)theClass.newInstance();
                } catch (ClassNotFoundException|IllegalAccessException|InstantiationException e) {
                    e.printStackTrace();
                } */
                gitPluginHandler = new GitPluginHandler();
            }
            return gitPluginHandler;
        } else if (scmType.SVN.equals(scmType)) {
            if (svnPluginHandler == null) {
                svnPluginHandler = new SvnPluginHandler();
            }
            return svnPluginHandler;
        }
        throw new IllegalArgumentException("SCM repository " + scmType + " isn't supported.");
    }

    public static ScmPluginHandler getScmHandlerByScmPluginName(String pluginName) {
        SCMType scmType = null;

        if ("hudson.plugins.git.GitSCM".equals(pluginName)) {
            scmType = scmType.GIT;
        } else if ("hudson.scm.SubversionSCM".equals(pluginName)) {
            scmType = scmType.SVN;
        } else {
            return null;
        }
        return getScmHandler(scmType);
    }

    public static ScmPluginHandler getScmHandlerByChangePathClass(String  changePathClass) {
        SCMType scmType = null;

        if ("hudson.plugins.git.GitChangeSet$Path".equals(changePathClass)) {
            scmType = scmType.GIT;
        } else {
            return null;
        }

        return getScmHandler(scmType);
    }



    public static boolean isPluginInstalled(SCMType scmType) {
        String shortName;
        if (scmType.GIT.equals(scmType)) {
            shortName = "git";
        } else if (scmType.SVN.equals(scmType)) {
            shortName = "subversion";
        } else {
            throw new IllegalArgumentException("SCM repository " + scmType + " isn't supported.");
        }

        PluginWrapper plugin = Jenkins.getInstance().pluginManager.getPlugin(shortName);
        return plugin != null;
    }
}
