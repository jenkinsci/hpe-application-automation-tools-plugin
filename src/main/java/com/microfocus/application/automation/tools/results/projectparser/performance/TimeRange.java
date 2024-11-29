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
 *  Copyright 2012-2025 Open Text.
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
package com.microfocus.application.automation.tools.results.projectparser.performance;

/**
 * The type Time range.
 */
public class TimeRange {

    private LrTest.SLA_STATUS slaStatus = LrTest.SLA_STATUS.bad;
    private double _actualValue;
    private double _goalValue;
    private int loadAmount;
    private double startTime;
    private double endTime;

    /**
     * Instantiates a new Time range.
     *
     * @param _actualValue the actual value
     * @param _goalValue   the goal value
     * @param slaStatus    the sla status
     * @param loadAmount   the load amount
     * @param startTime    the start time
     * @param endTime      the end time
     */
    public TimeRange(double _actualValue, double _goalValue, LrTest.SLA_STATUS slaStatus, int loadAmount,
                     double startTime, double endTime) {
        this._actualValue = _actualValue;
        this._goalValue = _goalValue;
        this.slaStatus = slaStatus;
        this.loadAmount = 0;
        this.loadAmount = loadAmount;
        this.startTime = 0;
        this.startTime = startTime;
        this.endTime = 0;
        this.endTime = endTime;
    }

    /**
     * Gets sla status.
     *
     * @return the sla status
     */
    public LrTest.SLA_STATUS getSlaStatus() {
        return slaStatus;
    }

    /**
     * Sets sla status.
     *
     * @param slaStatus the sla status
     */
    public void setSlaStatus(LrTest.SLA_STATUS slaStatus) {
        this.slaStatus = slaStatus;
    }

    /**
     * Gets actual value.
     *
     * @return the actual value
     */
    public double getActualValue() {
        return _actualValue;
    }

    /**
     * Sets actual value.
     *
     * @param actualValue the actual value
     */
    public void setActualValue(double actualValue) {
        this._actualValue = actualValue;
    }

    /**
     * Gets goal value.
     *
     * @return the goal value
     */
    public double getGoalValue() {
        return _goalValue;
    }

    /**
     * Sets goal value.
     *
     * @param goalValue the goal value
     */
    public void setGoalValue(double goalValue) {
        this._goalValue = goalValue;
    }

    /**
     * Gets load amount.
     *
     * @return the load amount
     */
    public int getLoadAmount() {
        return loadAmount;
    }

    /**
     * Sets load amount.
     *
     * @param loadAmount the load amount
     */
    public void setLoadAmount(int loadAmount) {
        this.loadAmount = loadAmount;
    }

    /**
     * Gets start time.
     *
     * @return the start time
     */
    public double getStartTime() {
        return startTime;
    }

    /**
     * Sets start time.
     *
     * @param startTime the start time
     */
    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    /**
     * Gets end time.
     *
     * @return the end time
     */
    public double getEndTime() {
        return endTime;
    }

    /**
     * Sets end time.
     *
     * @param endTime the end time
     */
    public void setEndTime(double endTime) {
        this.endTime = endTime;
    }


}
