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
