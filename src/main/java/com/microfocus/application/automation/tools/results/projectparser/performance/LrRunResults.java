/*
 * Certain versions of software accessible here may contain branding from Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 * This software was acquired by Micro Focus on September 1, 2017, and is now offered by OpenText.
 * Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright 2012-2024 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.results.projectparser.performance;

public class LrRunResults {
    protected int _totalFailures;
    protected int _totalErrors;
    protected double _time;
    private int TotalNoData;
    private int TotalPassed;
    private int TestCount;

    public LrRunResults() {
        _totalFailures = 0;
        _totalErrors = 0;
        _time = 0;
        TotalNoData = 0;
        TotalPassed = 0;
        TestCount = 0;
    }

    public int getTotalFailures() {
        return _totalFailures;
    }

    public void setTotalFailures(int totalFailures) {
        this._totalFailures = totalFailures;
    }

    public void incTotalErrors() {
        _totalErrors++;
    }

    public void incTotalNoData() {
        TotalNoData++;
    }

    public int getTotalErrors() {
        return _totalErrors;
    }

    public void setTotalErrors(int totalErrors) {
        this._totalErrors = totalErrors;
    }

    public void updateStatus(LrTest.SLA_STATUS slaStatus) {
        switch (slaStatus) {
            case Failed:
                incTotalFailures();
                incTotalTests();
                break;
            case Passed:
                incTotalPassed();
                incTotalTests();
                break;
            default:
                break;
        }
    }

    public void incTotalFailures() {
        _totalFailures++;
    }

    public void incTotalPassed() {
        TotalPassed++;
    }

    public void incTotalTests() {
        TestCount++;
    }

    public int getTotalNoData() {
        return TotalNoData;
    }

    public int getTestCount() {
        return TestCount;
    }

    public double getTime() {
        return _time;
    }

    public void setTime(double time) {
        this._time = time;
    }
}
