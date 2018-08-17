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

package com.microfocus.application.automation.tools;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;

@SuppressWarnings("squid:S1160")
public class AlmToolsUtils {


	private AlmToolsUtils() {
	}

	public static void runOnBuildEnv(
            Run<?, ?> build,
            Launcher launcher,
            TaskListener listener,
            FilePath file,
            String paramFileName) throws IOException, InterruptedException {
        
        ArgumentListBuilder args = new ArgumentListBuilder();
        PrintStream out = listener.getLogger();
        
        // Use script to run the cmdLine and get the console output
        args.add(file);
        args.add("-paramfile");
        args.add(paramFileName);
        
        // Run the script on node
        // Execution result should be 0
        int returnCode = launcher.launch().cmds(args).stdout(out).pwd(file.getParent()).join();
        
        if (returnCode != 0) {
            if (returnCode == -1) {
                build.setResult(Result.FAILURE);
            } else if (returnCode == -2) {
                build.setResult(Result.UNSTABLE);
            } else if (returnCode == -3) {
                build.setResult(Result.ABORTED);
            }
        }
    }

    public static void runHpToolsAborterOnBuildEnv(
            AbstractBuild<?, ?> build,
            Launcher launcher,
            BuildListener listener,
            String paramFileName) throws IOException, InterruptedException {

            runHpToolsAborterOnBuildEnv(build, launcher, listener, paramFileName, build.getWorkspace());
    }

	@SuppressWarnings("squid:S2259")
	public static void runHpToolsAborterOnBuildEnv(
            Run<?, ?> build,
            Launcher launcher,
            TaskListener listener,
            String paramFileName, FilePath runWorkspace) throws IOException, InterruptedException {

        ArgumentListBuilder args = new ArgumentListBuilder();
        PrintStream out = listener.getLogger();

        String hpToolsAborter_exe = "HpToolsAborter.exe";

		URL hpToolsAborterUrl = Jenkins.getInstance().pluginManager.uberClassLoader.getResource("HpToolsAborter.exe");
        FilePath hpToolsAborterFile = runWorkspace.child(hpToolsAborter_exe);
        
        args.add(hpToolsAborterFile);
        args.add(paramFileName);
        
        hpToolsAborterFile.copyFrom(hpToolsAborterUrl);
        
        int returnCode = launcher.launch().cmds(args).stdout(out).pwd(hpToolsAborterFile.getParent()).join();
        
        try {
        	hpToolsAborterFile.delete();
		} catch (Exception e) {
			 listener.error("failed copying HpToolsAborter" + e);
		}
        
        
        if (returnCode != 0) {
            if (returnCode == 1) {
                build.setResult(Result.FAILURE);
            } else if (returnCode == 2) {
                build.setResult(Result.UNSTABLE);
            } else if (returnCode == 3) {
                build.setResult(Result.ABORTED);
            }
        }
    }
    
    
}
