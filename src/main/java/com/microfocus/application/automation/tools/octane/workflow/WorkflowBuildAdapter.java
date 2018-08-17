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

package com.microfocus.application.automation.tools.octane.workflow;

import hudson.*;
import hudson.model.*;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import javax.annotation.Nonnull;
import hudson.model.Job;

import java.io.File;
import java.io.IOException;


/**
 * Created by lazara on 31/01/2017.
 */
public class WorkflowBuildAdapter extends Run {

    private WorkflowRun run;
    private FilePath workspace;
    protected WorkflowBuildAdapter(@Nonnull Job job, WorkflowRun run,FilePath workspace) throws IOException {
        super(job, run.getTimestamp().getTimeInMillis());
        this.run = run;
        this.workspace =workspace;
    }

    @Override
    public Job getParent() {
        return run.getParent();
    }

    @Override
    public String getId(){
        return run.getId();
    }

    @Override
    public int getNumber() {
        return run.getNumber();
    }

    @Override
    public Action getAction(Class type){
        return run.getAction(type);
    }
    @Nonnull
    @Override
    public File getRootDir() {
        return run.getRootDir();
    }

    @Override
    public Executor getExecutor() {
        return run.getExecutor();
    }

    public FilePath getWorkspace(){
        return workspace;
    }
//    build.getRootDir()
//            build.getParent()
//            build.getResult()
//            build.getNumber()
//            BuildHandlerUtils.getProjectFullName(build)
//    build.setResult
}
