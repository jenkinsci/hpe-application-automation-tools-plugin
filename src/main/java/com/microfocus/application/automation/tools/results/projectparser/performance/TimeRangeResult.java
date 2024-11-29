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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kazaky on 07/07/2016.
 */
public class TimeRangeResult extends GoalResult implements LrTest {


    /**
     * The Time ranges.
     */
    private ArrayList<TimeRange> timeRanges;
    private double _avgActualValue;
    private double _actualValueSum;
    private double _goalValue;
    private String LoadThrashold;
    /**
     * Instantiates a new Time range result.
     */
    public TimeRangeResult() {
        _actualValueSum = 0;
        _goalValue = 0;
        _avgActualValue = 0;
        timeRanges = new ArrayList<TimeRange>(0);
    }

    public List<TimeRange> getTimeRanges() {
        return timeRanges;
    }

    /**
     * Gets actual value avg.
     *
     * @return the actual value avg
     */
    public double getActualValueAvg() {
        _avgActualValue = _actualValueSum / timeRanges.size();
        return _avgActualValue;
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
     * Inc actual value.
     *
     * @param actualValue the actual value
     */
    public void incActualValue(double actualValue) {
        this._actualValueSum += actualValue;
    }

    /**
     * Gets load thrashold.
     *
     * @return the load thrashold
     */
    public String getLoadThrashold() {
        return LoadThrashold;
    }

    /**
     * Sets load thrashold.
     *
     * @param loadThrashold the load thrashold
     * @return the load thrashold
     */
    public TimeRangeResult setLoadThrashold(String loadThrashold) {
        LoadThrashold = loadThrashold;
        return this;
    }

}
