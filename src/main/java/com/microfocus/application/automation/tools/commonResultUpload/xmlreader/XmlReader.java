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
 * © Copyright 2012-2019 Micro Focus or one of its affiliates..
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

package com.microfocus.application.automation.tools.commonResultUpload.xmlreader;

import com.microfocus.application.automation.tools.commonResultUpload.CommonUploadLogger;
import com.microfocus.application.automation.tools.commonResultUpload.xmlreader.model.EntitiesFieldMap;
import com.microfocus.application.automation.tools.commonResultUpload.xmlreader.model.XmlResultEntity;
import hudson.FilePath;
import hudson.model.Result;
import hudson.model.Run;
import org.apache.tools.ant.DirectoryScanner;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class XmlReader {

    private CommonUploadLogger logger;
    private Run<?, ?> run;
    private FilePath workspace;

    public XmlReader(Run<?, ?> run, FilePath workspace, CommonUploadLogger logger) {
        this.run = run;
        this.logger = logger;
        this.workspace = workspace;
    }

    public List<XmlResultEntity> scan(String filePath, EntitiesFieldMap entitiesFieldMap) {
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(run.getRootDir());
        ds.setIncludes(new String[] {filePath});
        ds.scan();

        if (ds.getIncludedFilesCount() == 0) {
            logger.info("No Test Report found in job folder: " + run.getRootDir().getAbsolutePath());
            return scanInWorkspace(filePath, entitiesFieldMap);
        } else {
            logger.info(ds.getIncludedFilesCount() + " test result file found in job folder: " + run.getRootDir().getAbsolutePath());
            return readBuildPath(ds.getIncludedFiles(), entitiesFieldMap);
        }
    }

    private List<XmlResultEntity> scanInWorkspace(String filePath, EntitiesFieldMap entitiesFieldMap) {
        List<FilePath> files = new ArrayList<>();
        for (String include : new String[] {filePath}) {
            try {
                files.addAll(Arrays.asList(workspace.list(include)));
            } catch (Exception e) {
                logger.error(e.getMessage());
                run.setResult(Result.FAILURE);
            }
        }
        if (files.size() < 1) {
            logger.info("No Test Report found in workspace: " + workspace);
        }
        logger.info(files.size() + " test result file found in workspace: " + workspace);
        return readWorkspace(files, entitiesFieldMap);
    }

    private List<XmlResultEntity> readWorkspace(List<FilePath> files, EntitiesFieldMap entitiesFieldMap) {
        List<XmlResultEntity> xmlResultEntities = new ArrayList<>();
        for (FilePath file : files) {
            try {
                TestSetReader tr = new TestSetReader(file, entitiesFieldMap);
                xmlResultEntities.addAll(tr.readTestsets());
            } catch (Exception e) {
                logger.error(e.getMessage());
                e.printStackTrace();
                run.setResult(Result.FAILURE);
            }
        }
        return xmlResultEntities;
    }

    private List<XmlResultEntity> readBuildPath(String[] files, EntitiesFieldMap entitiesFieldMap) {
        List<XmlResultEntity> xmlResultEntities = new ArrayList<>();
        for (String fileName : files) {
            String fullpath = run.getRootDir().getAbsolutePath() + File.separator + fileName;
            try {
                TestSetReader tr = new TestSetReader(fullpath, entitiesFieldMap);
                xmlResultEntities.addAll(tr.readTestsets());
            } catch (Exception e) {
                logger.error(e.getMessage());
                e.printStackTrace();
                run.setResult(Result.FAILURE);
            }
        }
        return xmlResultEntities;
    }
}
