/*
 *  Certain versions of software accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 *  This software was acquired by Micro Focus on September 1, 2017, and is now
 *  offered by OpenText.
 *  Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical
 *  in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the
 *  property of their respective owners.
 *  OpenText is a trademark of Open Text.
 *  __________________________________________________________________
 *  MIT License
 *
 *  Copyright 2012-2026 Open Text.
 *
 *  The only warranties for products and services of Open Text and
 *  its affiliates and licensors ("Open Text") are as may be set forth
 *  in the express warranty statements accompanying such products and services.
 *  Nothing herein should be construed as constituting an additional warranty.
 *  Open Text shall not be liable for technical or editorial errors or
 *  omissions contained herein. The information contained herein is subject
 *  to change without notice.
 *
 *  Except as specifically indicated otherwise, this document contains
 *  confidential information and a valid license is required for possession,
 *  use or copying. If this work is provided to the U.S. Government,
 *  consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 *  Computer Software Documentation, and Technical Data for Commercial Items are
 *  licensed to the U.S. Government under vendor's standard commercial license.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ___________________________________________________________________
 */
package com.microfocus.application.automation.tools.mi;

import java.io.Serializable;

/**
 * Parsed representation of a single MI Agent (Autonomous-Tester) test result.
 */
public class MIAgentTestResult implements Serializable {

    public enum Status {
        PASSED, FAILED, SKIPPED, ERROR, UNKNOWN
    }

    private static final long serialVersionUID = 1L;

    private String testId;
    private String testName;
    private String packageName;
    private String className;
    private long durationMillis;
    private Status status = Status.UNKNOWN;
    private String errorMessage;
    private String stackTrace;
    private String reportUrl;

    public String getTestId() { return testId; }
    public void setTestId(String testId) { this.testId = testId; }

    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public long getDurationMillis() { return durationMillis; }
    public void setDurationMillis(long durationMillis) { this.durationMillis = durationMillis; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }

    public String getReportUrl() { return reportUrl; }
    public void setReportUrl(String reportUrl) { this.reportUrl = reportUrl; }
}
