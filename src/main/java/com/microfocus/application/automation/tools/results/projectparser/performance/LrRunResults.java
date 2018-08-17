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
