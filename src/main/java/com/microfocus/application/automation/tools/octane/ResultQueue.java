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

package com.microfocus.application.automation.tools.octane;

import java.io.Serializable;

@SuppressWarnings("squid:S2039")
public interface ResultQueue {

    ResultQueue.QueueItem peekFirst();

    boolean failed();

    void remove();

    void add(String projectName, int buildNumber);

    void add(String projectName, String type, int buildNumber);

    void add(String projectName, int buildNumber, String workspace);

    void clear();

    class QueueItem implements Serializable {
        private static final long serialVersionUID = 1;
        public String type;
        String projectName;
        int buildNumber;
        String workspace;
        int failCount;


        public void setType(String type) {
            this.type = type;
        }


        public QueueItem(String projectName, int buildNumber) {
            this(projectName, buildNumber, 0);
        }

        public QueueItem(String projectName, String type, int buildNumber) {
            this(projectName, buildNumber, 0);
            this.type = type;
        }

        public QueueItem(String projectName, int buildNumber, String workspace) {
            this(projectName, buildNumber, 0);
            this.workspace = workspace;
        }

        QueueItem(String projectName, int buildNumber, int failCount) {
            this.projectName = projectName;
            this.buildNumber = buildNumber;
            this.failCount = failCount;
        }

        QueueItem(String projectName, int buildNumber, int failCount, String workspace) {
            this.projectName = projectName;
            this.buildNumber = buildNumber;
            this.failCount = failCount;
            this.workspace = workspace;
        }

        public String getType() {
            return type;
        }

        public int incrementFailCount() {
            return this.failCount++;
        }

        public int getFailCount() {
            return failCount;
        }

        public String getProjectName() {
            return projectName;
        }

        public int getBuildNumber(){
            return buildNumber;
        }

        public String getWorkspace() {
            return workspace;
        }
    }
}
