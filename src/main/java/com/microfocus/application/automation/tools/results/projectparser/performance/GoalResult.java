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

/**
 * Created by kazaky on 07/07/2016.
 */


public abstract class GoalResult implements LrTest{

    public SLA_STATUS getStatus() {
        return _status;
    }

    public void setStatus(SLA_STATUS _status) {
        this._status = _status;
    }

    public SLA_GOAL getSlaGoal() {
        return _slaGoal;
    }

    public void setSlaGoal(SLA_GOAL _slaGoal) {
        this._slaGoal = _slaGoal;
    }

    public double getDuration() {
        return _duration;
}

    public void setDuration(double _duration) {
        this._duration = _duration;
    }

    private SLA_GOAL _slaGoal;
    private SLA_STATUS _status;

    public String getFullName() {
        return _fullName;
    }

    public void setFullName(String _fullName) {
        this._fullName = _fullName;
    }

    private String _fullName;
    private double _duration;


}
