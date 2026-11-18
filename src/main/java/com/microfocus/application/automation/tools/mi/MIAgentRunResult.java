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
import java.util.ArrayList;
import java.util.List;

/**
 * Parsed representation of a full MI Agent (Autonomous-Tester) run.
 * Populated from the AuTe manifest by {@link MIAgentResultPublisher}.
 */
public class MIAgentRunResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String manifestVersion;
    private String runId;
    private String executorId;
    private String executorLogicalName;
    private String suiteId;
    private String suiteRunId;
    private long startTimeMillis;
    private long durationMillis;
    private String status;
    private final List<MIAgentTestResult> tests = new ArrayList<>();

    public String getManifestVersion() { return manifestVersion; }
    public void setManifestVersion(String manifestVersion) { this.manifestVersion = manifestVersion; }

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }

    public String getExecutorId() { return executorId; }
    public void setExecutorId(String executorId) { this.executorId = executorId; }

    public String getExecutorLogicalName() { return executorLogicalName; }
    public void setExecutorLogicalName(String executorLogicalName) { this.executorLogicalName = executorLogicalName; }

    public String getSuiteId() { return suiteId; }
    public void setSuiteId(String suiteId) { this.suiteId = suiteId; }

    public String getSuiteRunId() { return suiteRunId; }
    public void setSuiteRunId(String suiteRunId) { this.suiteRunId = suiteRunId; }

    public long getStartTimeMillis() { return startTimeMillis; }
    public void setStartTimeMillis(long startTimeMillis) { this.startTimeMillis = startTimeMillis; }

    public long getDurationMillis() { return durationMillis; }
    public void setDurationMillis(long durationMillis) { this.durationMillis = durationMillis; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<MIAgentTestResult> getTests() { return tests; }
}
