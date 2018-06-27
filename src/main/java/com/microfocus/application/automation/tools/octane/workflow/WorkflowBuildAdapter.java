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
