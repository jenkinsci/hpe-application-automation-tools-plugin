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
