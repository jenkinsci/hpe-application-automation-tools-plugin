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
